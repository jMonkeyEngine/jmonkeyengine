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

    public void streamToRemote(CommandBuffer cmd, DataBuffer local, GpuBuffer remote) {
        streamToRemote(cmd, local.getBytes(), remote, local.getTracker());
    }

    /**
     * Streams the specified regions from {@code local} to {@code remote} through an intermediate
     * streaming page partition.
     *
     * @param local buffer to stream from
     * @param remote vulkan buffer to stream to (must be created with the {@link GpuBuffer.Role#TransferDst} flag set)
     * @throws IllegalArgumentException if {@code remote} to not a transfer destination
     * @throws IllegalStateException if {@code regions} gives a region not fully within {@code src}
     */
    public void streamToRemote(CommandBuffer cmd, ByteBuffer local, GpuBuffer remote, BufferTracker regions) {
        if (!remote.getRoles().contains(GpuBuffer.Role.TransferDst)) {
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
        cmd.cmdCopy(partition, remote, new BufferCopy().addExpand(0, 0, regions), OpLocation.Device);
        cmd.addListener(partition);
    }

    public void streamFromRemoteAsync(CommandBuffer cmd, GpuBuffer remote, ByteBuffer local) {
        streamFromRemoteAsync(cmd, remote, local, null);
    }

    public void streamFromRemoteAsync(CommandBuffer cmd, GpuBuffer remote, ByteBuffer local, Runnable callback) {
        if (!remote.getRoles().contains(GpuBuffer.Role.TransferSrc)) {
            throw new IllegalArgumentException("Cannot stream from " + remote + ": not a transfer source.");
        }
        Partition partition = allocatePartition(Math.min(remote.capacity(), local.remaining()));
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        cmd.cmdCopy(remote, partition, new BufferCopy().add(0, 0, partition.capacity()), OpLocation.Device);
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
                Flag.of(GpuBuffer.Role.TransferSrc, GpuBuffer.Role.TransferDst)));
        pages.add(s);
        return s.allocatePartition(bytes);
    }

    private static class StreamingPage implements GpuBuffer {

        private final GpuBuffer buffer;
        private final BufferTracker allocated = new ExactBufferTracker();

        public StreamingPage(GpuBuffer buffer) {
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
        public int offset() {
            return buffer.offset();
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

    private static class Partition implements GpuBuffer, CommandCycleListener {

        private final StreamingPage page;
        private final DataBuffer cache;
        private final int start;

        public Partition(StreamingPage page, int start, int size) {
            this.page = page;
            this.start = start;
            this.cache = page.cache().slice(start, start + size);
        }

        @Override
        public void onCmdSubmit() {
            page.flushCache();
        }

        @Override
        public void onCmdComplete() {
            page.releasePartition(this);
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
        public int offset() {
            return page.offset() + start;
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
