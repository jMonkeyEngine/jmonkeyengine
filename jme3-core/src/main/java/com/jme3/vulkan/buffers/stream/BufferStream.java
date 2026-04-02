package com.jme3.vulkan.buffers.stream;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.AbstractVulkanBuffer;
import com.jme3.vulkan.buffers.newbuf.HostVisibleBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;

import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Streams changes from
 */
public class BufferStream {

    private final LogicalDevice<?> device;
    private final Collection<StreamingPage> pages = new ConcurrentLinkedQueue<>();
    private final int pageByteSize;

    public BufferStream(LogicalDevice<?> device, int pageByteSize) {
        this.device = device;
        this.pageByteSize = pageByteSize;
    }

    /**
     * Streams the specified regions from {@code src} to {@code dst} through an intermediate
     * streaming page partition.
     *
     * @param src buffer to stream from
     * @param dst vulkan buffer to stream to (must be created with the {@link BufferUsage#TransferDst} flag set)
     * @param tracker regions that should be streamed between the buffers
     * @throws IllegalArgumentException if {@code dst} to not a transfer destination
     * @throws IllegalStateException if {@code regions} gives a region not fully within {@code src}
     */
    public void stream(CommandBuffer cmd, MappableBuffer src, VulkanBuffer dst, BufferTracker tracker) {
        if (tracker.isEmpty()) {
            return;
        }
        if (!dst.getUsage().contains(BufferUsage.TransferDst)) {
            throw new IllegalArgumentException("Cannot stream to " + dst + ": not a transfer destination.");
        }
        long coverage = 0;
        int count = 0;
        for (BufferTracker.Island i : tracker) {
            coverage += i.getSize();
            count++;
        }
        if (coverage == 0 || count == 0) {
            return;
        }
        PagePartition partition = allocatePartition(coverage);
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        long partitionOffset = 0;
        try (MemoryStack stack = MemoryStack.stackPush(); BufferMapping srcMap = src.map(); BufferMapping pMap = partition.map()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(count, stack);
            for (BufferTracker.Island i : tracker) {
                if (i.getEnd() > srcMap.getBytes().capacity()) {
                    throw new IllegalStateException("Buffer region extends outside source buffer.");
                }
                MemoryUtil.memCopy(srcMap.getAddress() + i.getStart(), pMap.getAddress() + partitionOffset, i.getSize());
                copy.get().set(partitionOffset, i.getStart(), i.getSize());
                partitionOffset += i.getSize();
            }
            long pId = partition.page.getBuffer().getBufferId(device);
            vkCmdCopyBuffer(cmd.getBuffer(), pId, dst.getBufferId(device), copy.flip());
        }
        cmd.onExecutionComplete(partition);
        tracker.clear();
    }

    /**
     * Allocates {@code bytes} of free space in a streaming page. If not enough
     * consecutive space is found in an existing page, a new page is created that
     * is guaranteed to contain at least enough space for the allocation.
     *
     * @param bytes consecutive bytes to allocate
     * @return allocated page partition
     */
    private PagePartition allocatePartition(long bytes) {
        for (StreamingPage s : pages) {
            PagePartition p = s.allocatePartition(bytes);
            if (p != null) return p;
        }
        StreamingPage s = new StreamingPage(Math.max(bytes, pageByteSize));
        pages.add(s);
        return s.allocatePartition(bytes);
    }

    private static class StreamingPage extends PersistentBuffer<VulkanBuffer> {

        private final AllocatedRegion head = new AllocatedRegion(this, 0, 0);

        public StreamingPage(long bytes) {
            super(create(bytes));
        }

        private static VulkanBuffer create(long bytes) {
            return HostVisibleBuffer.build(bytes, (AbstractVulkanBuffer.Builder b) -> {
                b.setUsage(BufferUsage.TransferSrc);
                b.setSharingMode(SharingMode.Concurrent);
            });
        }

        public PagePartition allocatePartition(long bytes) {
            for (AllocatedRegion r = head, prev = null; r != null; r = r.next) {
                PagePartition p = r.allocateBytesAfter(bytes);
                if (p != null) {
                    return p;
                }
                if (prev != null && r.start == r.end) {
                    prev.next = r.next;
                    continue;
                }
                prev = r;
            }
            return null;
        }

        public boolean releasePartition(PagePartition partition) {
            for (AllocatedRegion r = head; r != null; r = r.next) {
                if (r.release(partition)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static class PagePartition implements MappableBuffer, Runnable {

        public final StreamingPage page;
        private final MemorySize size;

        public PagePartition(StreamingPage page, MemorySize size) {
            this.page = page;
            this.size = size;
        }

        @Override
        public void run() {
            page.releasePartition(this);
        }

        @Override
        public BufferMapping map(long offset, long size) {
            return page.map(this.size.getOffset() + offset, size);
        }

        @Override
        public void stage(long offset, long size) {
            page.stage(this.size.getOffset() + offset, size);
        }

        @Override
        public void resize(long bytes) {}

        @Override
        public MemorySize size() {
            return size;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            throw new UnsupportedOperationException("Cannot be written.");
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            throw new UnsupportedOperationException("Cannot be read.");
        }

    }

    private static class AllocatedRegion {

        private final StreamingPage stream;
        private long start, end;
        private AllocatedRegion next;

        public AllocatedRegion(StreamingPage stream, long start, long end) {
            this.stream = stream;
            this.start = Math.min(start, stream.size().getBytes());
            this.end = Math.min(end, stream.size().getBytes());
        }

        public PagePartition allocateBytesAfter(long bytes) {
            if (availableBytesAfter() >= bytes) synchronized (this) {
                if (next == null) {
                    // create next island so other concurrent allocation requests won't all pile
                    // up trying to allocate after this island
                    next = new AllocatedRegion(stream, end + bytes, end + bytes);
                }
                long a = availableBytesAfter();
                if (a >= bytes) {
                    long pStart = end;
                    if (a == bytes) {
                        next.start = start;
                        end = start; // collapse island
                    } else {
                        end += bytes;
                    }
                    return new PagePartition(stream, MemorySize.bytes(pStart, bytes));
                }
            }
            return null;
        }

        public boolean release(PagePartition partition) {
            if (start == end) {
                return false;
            }
            long pEnd = partition.size().getEnd();
            if (pEnd <= start || partition.size().getOffset() >= end) {
                return false;
            }
            synchronized (this) {
                if (partition.size().getOffset() > start && pEnd < end) {
                    AllocatedRegion r = new AllocatedRegion(stream, pEnd, end);
                    r.next = next;
                    next = r;
                    end = partition.size().getOffset();
                } else if (partition.size().getOffset() <= start) {
                    start = pEnd;
                } else {
                    end = partition.size().getOffset();
                }
            }
            return true;
        }

        public long availableBytesAfter() {
            if (next == null) return stream.size().getBytes() - end;
            else return next.start - end;
        }

    }

    private interface CopyCmd {

        VulkanBuffer getSrc();

        VulkanBuffer getDst();

        VkBufferCopy.Buffer getParameters();

        void release();

    }

    private static class StreamCopy implements CopyCmd {

        public final PagePartition src;
        public final VulkanBuffer dst;
        public final VkBufferCopy.Buffer data;

        public StreamCopy(PagePartition src, VulkanBuffer dst, int regions) {
            this.src = src;
            this.dst = dst;
            this.data = VkBufferCopy.calloc(regions);
        }

        @Override
        public VulkanBuffer getSrc() {
            return src.page.getBuffer();
        }

        @Override
        public VulkanBuffer getDst() {
            return dst;
        }

        @Override
        public VkBufferCopy.Buffer getParameters() {
            return data;
        }

        @Override
        public void release() {
            MemoryUtil.memFree(data);
            src.page.releasePartition(src);
        }

    }

    private static class DirectCopy implements CopyCmd {

        private final VulkanBuffer src, dst;
        private final VkBufferCopy.Buffer data;

        public DirectCopy(VulkanBuffer src, VulkanBuffer dst, int regions) {
            this.src = src;
            this.dst = dst;
            this.data = VkBufferCopy.calloc(regions);
        }

        @Override
        public VulkanBuffer getSrc() {
            return src;
        }

        @Override
        public VulkanBuffer getDst() {
            return dst;
        }

        @Override
        public VkBufferCopy.Buffer getParameters() {
            return data;
        }

        @Override
        public void release() {
            MemoryUtil.memFree(data);
        }

    }

}
