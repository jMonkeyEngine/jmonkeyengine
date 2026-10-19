package com.jme3.vulkan.buffer;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.ImmediateCommands;
import com.jme3.vulkan.concurrent.CommandDataAccess;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;

/**
 * A {@link GpuBuffer} for data that is rewritten periodically, every several frames,
 * rather than continuously ({@link BufferType#Dynamic}, kept persistently mapped) or
 * never again after its initial upload ({@link BufferType#Local}).
 * <p>
 * The buffer itself is allocated in device-local memory so the GPU always reads it at
 * full bandwidth. Writes go through a private host-visible staging buffer whose dirty
 * byte ranges are tracked as they're written; {@link #flushCache()} copies only those
 * ranges into the device-local buffer, using its own {@link ImmediateCommands} submission
 * so callers never have to manage a {@link CommandBuffer} themselves. Access to the
 * device-local buffer is ordered against earlier accesses with a {@link CommandDataAccess},
 * so neither {@link #flushCache()} nor this buffer's use elsewhere has to block the CPU
 * to stay correct.
 */
public class StaticBuffer implements GpuBuffer {

    private final ImmediateCommands commands;
    private final BufferStream stream;
    private final GpuBuffer remote;
    private final DataBuffer cache;

    public StaticBuffer(MemoryAllocator allocator, ImmediateCommands commands, BufferStream stream, int capacity, Flag<Role> roles) {
        this(commands, stream, allocator.createBuffer(BufferType.Local, capacity, roles.add(Role.TransferDst)));
    }

    public StaticBuffer(ImmediateCommands commands, BufferStream stream, GpuBuffer remote) {
        this.commands = commands;
        this.stream = stream;
        this.remote = remote;
        this.cache = new DataBuffer(ByteBuffer.wrap(new byte[remote.capacity()]));
    }

    @Override
    public DataBuffer cache() {
        return cache;
    }

    /**
     * Copies the byte ranges written since the last flush from the staging buffer into
     * device-local memory. The underlying copy is ordered against earlier accesses to
     * the device-local buffer with a semaphore rather than a CPU-side wait, so this does
     * not block. Does nothing if nothing has been written through {@link #cache()} since
     * the last flush.
     */
    @Override
    public void flushCache() {
        if (cache.getTracker().isEmpty()) {
            return;
        }
        CommandBuffer cmd = commands.acquire();
        cmd.beginRecording();
        stream.streamToRemote(cmd, cache, remote);
        cmd.endRecording();
        cmd.submit();
        cache.getTracker().clear();
    }

    @Override
    public void invalidateCache() {
        CommandBuffer cmd = commands.acquire();
        cmd.beginRecording();
        stream.streamFromRemoteAsync(cmd, remote, cache.getBytes());
        cmd.endRecording();
        TimelineSemaphore.SignalEvent event = cmd.getCompletionSignal().createEvent();
        cmd.submit();
        event.awaitSignal(2000);
    }

    @Override
    public int capacity() {
        return remote.capacity();
    }

    @Override
    public int offset() {
        return 0;
    }

    @Override
    public long getHandle() {
        return remote.getHandle();
    }

    @Override
    public Flag<Role> getRoles() {
        return remote.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return remote.getMemoryProperties();
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return remote.getLifecycle();
    }

}
