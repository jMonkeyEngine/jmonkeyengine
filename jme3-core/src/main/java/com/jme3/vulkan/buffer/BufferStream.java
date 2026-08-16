package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandCycleListener;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Streams changes from
 */
public class BufferStream {

    private final MemoryAllocator allocator;
    private final Collection<StreamingPage> pages = new ConcurrentLinkedQueue<>();
    private final int pageByteSize;

    public BufferStream(MemoryAllocator allocator, int pageByteSize) {
        this.allocator = allocator;
        this.pageByteSize = pageByteSize;
    }

    public void streamToRemote(CommandBuffer cmd, DataBuffer local, EngineBuffer remote) {
        streamToRemote(cmd, local.getBytes(), remote, local.getTracker());
    }

    /**
     * Streams the specified regions from {@code local} to {@code remote} through an intermediate
     * streaming page partition.
     *
     * @param local buffer to stream from
     * @param remote vulkan buffer to stream to (must be created with the {@link EngineBuffer.Role#TransferDst} flag set)
     * @throws IllegalArgumentException if {@code remote} to not a transfer destination
     * @throws IllegalStateException if {@code regions} gives a region not fully within {@code src}
     */
    public void streamToRemote(CommandBuffer cmd, ByteBuffer local, EngineBuffer remote, BufferTracker regions) {
        if (!remote.getRoles().contains(EngineBuffer.Role.TransferDst)) {
            throw new IllegalArgumentException("Cannot stream to " + remote + ": not a transfer destination.");
        }
        if (regions.isEmpty()) return;
        Partition partition = allocatePartition(regions.getNumCovered());
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        long partitionOffset = 0;
        long localAddress = MemoryUtil.memAddress(local);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(regions.getNumIslands(), stack);
            for (BufferTracker.Island i : regions) {
                MemoryUtil.memCopy(localAddress + i.getStart(),
                        partition.getCache().getAddress() + partitionOffset, i.getSize());
                copy.get().set(partitionOffset, i.getStart(), i.getSize());
                partitionOffset += i.getSize();
            }
        }
        cmd.cmdFlatCopy(partition.page, partition.start, remote, 0, regions, false, OpLocation.PreferDevice);
        cmd.addListener(partition);
    }

    public void streamFromRemote(CommandBuffer cmd, EngineBuffer remote, ByteBuffer local) {
        streamFromRemote(cmd, remote, local, null);
    }

    public void streamFromRemote(CommandBuffer cmd, EngineBuffer remote, ByteBuffer local, Runnable callback) {
        if (!remote.getRoles().contains(EngineBuffer.Role.TransferSrc)) {
            throw new IllegalArgumentException("Cannot stream from " + remote + ": not a transfer source.");
        }
        Partition partition = allocatePartition(Math.min(remote.capacity(), local.remaining()));
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        cmd.cmdCopy(remote, 0, partition, 0, Integer.MAX_VALUE, OpLocation.PreferDevice);
        ByteBuffer localDup = local.duplicate();
        cmd.addListener(new CommandCycleListener() {
            @Override
            public void onCmdSubmit() {}
            @Override
            public void onCmdComplete() {
                MemoryUtil.memCopy(partition.getCache().getBytes(), localDup);
                partition.onCmdComplete();
                if (callback != null) callback.run();
            }
        });
    }

    /**
     * Allocates {@code bytes} of free space in a streaming page. If not enough
     * consecutive space is found in an existing page, a new page is created that
     * is guaranteed to contain at least enough space for the allocation.
     *
     * @param bytes consecutive bytes to allocate
     * @return allocated page partition
     */
    private Partition allocatePartition(int bytes) {
        for (StreamingPage s : pages) {
            Partition p = s.allocatePartition(bytes);
            if (p != null) return p;
        }
        StreamingPage s = new StreamingPage(allocator.createStreamingBuffer(
                Math.max(bytes, pageByteSize),
                Flag.of(EngineBuffer.Role.TransferSrc, EngineBuffer.Role.TransferDst)));
        pages.add(s);
        return s.allocatePartition(bytes);
    }

    private static class StreamingPage implements EngineBuffer {

        private final EngineBuffer buffer;
        private final BufferTracker allocated = new ExactBufferTracker();

        public StreamingPage(EngineBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public long getHandle() {
            return buffer.getHandle();
        }

        @Override
        public void flushCache() {
            buffer.flushCache();
        }

        @Override
        public void invalidateCache() {
            buffer.invalidateCache();
        }

        @Override
        public int getBufferLocalOffset() {
            return buffer.getBufferLocalOffset();
        }

        @Override
        public long getDeviceAddress() {
            return buffer.getDeviceAddress() + getBufferLocalOffset();
        }

        @Override
        public Flag<Role> getRoles() {
            return buffer.getRoles();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return buffer.getMemoryProperties();
        }

        @Override
        public DataBuffer cache() {
            return buffer.cache();
        }

        @Override
        public int capacity() {
            return buffer.capacity();
        }

        @Override
        public void update(CommandBuffer cmd) {
            buffer.update(cmd);
        }

        @Override
        public boolean isDeviceAccessible() {
            return buffer.isDeviceAccessible();
        }

        @Override
        public void acquireControl() {
            buffer.acquireControl();
        }

        @Override
        public void releaseControl() {
            buffer.releaseControl();
        }

        public Partition allocatePartition(int bytes) {
            if (bytes > buffer.capacity()) {
                return null;
            }
            if (allocated.isEmpty()) {
                return createPartition(0, bytes);
            }
            for (BufferTracker.Island i : allocated) {
                if (i.getAvailableAfter(buffer.capacity()) > bytes) {
                    return createPartition(i.getStart(), bytes);
                }
            }
            return null;
        }

        private Partition createPartition(int start, int size) {
            Partition p = new Partition(this, start, size);
            allocated.add(start, size);
            return p;
        }

        public void releasePartition(Partition partition) {
            allocated.remove(partition.getStart(), partition.getSize());
        }

    }

    private static class Partition implements EngineBuffer, CommandCycleListener {

        private final StreamingPage page;
        private final DataBuffer cache;
        private final int start;

        public Partition(StreamingPage page, int start, int size) {
            this.page = page;
            this.start = start;
            this.cache = page.cache().slice(start, start + size);
        }

        @Override
        public void update(CommandBuffer cmd) {

        }

        @Override
        public boolean isDeviceAccessible() {
            return page.isDeviceAccessible();
        }

        @Override
        public void onCmdSubmit() {}

        @Override
        public void onCmdComplete() {
            page.releasePartition(this);
        }

        @Override
        public void acquireControl() {
            page.acquireControl();
        }

        @Override
        public void releaseControl() {
            page.releaseControl();
        }

        @Override
        public long getHandle() {
            return page.getHandle();
        }

        @Override
        public void flushCache() {
            page.flushCache();
        }

        @Override
        public void invalidateCache() {
            page.invalidateCache();
        }

        @Override
        public long getDeviceAddress() {
            return page.getDeviceAddress() + getBufferLocalOffset();
        }

        @Override
        public Flag<Role> getRoles() {
            return page.getRoles();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return page.getMemoryProperties();
        }

        @Override
        public DataBuffer cache() {
            return cache;
        }

        @Override
        public int capacity() {
            return cache.capacity();
        }

        @Override
        public int getBufferLocalOffset() {
            return start;
        }

        public DataBuffer getCache() {
            return cache;
        }

        public int getStart() {
            return start;
        }

        public int getSize() {
            return cache.capacity();
        }

    }

}
