package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.HostVisibleBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Streams changes from
 */
public class BufferStream {

    private final LogicalDevice<?> device;
    private final Collection<StreamingPage> pages = new ConcurrentLinkedQueue<>();
    private final Queue<CopyCmd> copyCommands = new ConcurrentLinkedQueue<>();
    private final Collection<WeakReference<Streamable>> buffers = new ConcurrentLinkedQueue<>();
    private final int pageByteSize;
    private int regionMergeGap = -1;
    private int maxRegions = -1;

    public BufferStream(LogicalDevice<?> device, int pageByteSize) {
        this.device = device;
        this.pageByteSize = pageByteSize;
    }

    /**
     * Stages changes of all streamable objects registered with this buffer stream.
     *
     * @see #add(Streamable)
     * @see #stage(Streamable)
     */
    public void stage() {
        for (Iterator<WeakReference<Streamable>> it = buffers.iterator(); it.hasNext();) {
            Streamable b = it.next().get();
            if (b != null) stage(b);
            else it.remove();
        }
    }

    /**
     * Registers a {@link WeakReference} of {@code object} with this buffer stream.
     * On each call of {@link #stage()}, if {@code object} is reachable, object's
     * changes will be staged.
     *
     * @param object object to register
     */
    public <T extends Streamable> T add(T object) {
        buffers.add(new WeakReference<>(object));
        return object;
    }

    /**
     * Removes {@code object} from being automatically staged on each call of {@link #stage()}.
     *
     * @param object object to remove
     */
    public void remove(Streamable object) {
        buffers.removeIf(ref -> ref.get() == object);
    }

    /**
     * Stages changes from the streamable object to an intermediate streaming page and
     * submits a copy command to upload the changes from the streaming page to the streamable
     * object's destination buffer. If the streamable object has no changes, this does nothing.
     *
     * @param object streamable object to stage changes from
     * @see #upload(CommandBuffer)
     */
    public <T extends Streamable> T stage(T object) {
        if (object.getUpdateRegions().getCoverage() == 0) {
            return object;
        }
        VulkanBuffer dst = object.getDstBuffer();
        if (!dst.getUsage().contains(BufferUsage.TransferDst)) {
            throw new IllegalArgumentException("Cannot stream to " + dst + ": not a transfer destination.");
        }
        if (regionMergeGap >= 0) {
            object.getUpdateRegions().optimizeGaps(regionMergeGap);
        }
        if (maxRegions >= 0) {
            object.getUpdateRegions().optimizeNumRegions(maxRegions);
        }
        BufferPartition<StreamingPage> partition = allocatePartition(object.getUpdateRegions().getCoverage());
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        ByteBuffer srcBytes = object.mapBytes();
        ByteBuffer partitionBytes = partition.mapBytes();
        StreamCopy copy = new StreamCopy(partition, dst, object.getUpdateRegions().getNumRegions());
        int partitionOffset = 0;
        for (DirtyRegions.Region r : object.getUpdateRegions()) {
            if (r.getEnd() > srcBytes.limit()) {
                throw new IllegalStateException("Buffer region extends outside source buffer.");
            }
            // copy src to intermediate
            MemoryUtil.memCopy(MemoryUtil.memAddress(srcBytes, r.getOffset()),
                    MemoryUtil.memAddress(partitionBytes, partitionOffset), r.getSize());
            copy.data.get().set(partitionOffset, r.getOffset(), r.getSize());
            partitionOffset += r.getSize();
        }
        object.getUpdateRegions().clear();
        copy.data.flip();
        copyCommands.add(copy);
        return object;
    }

    /**
     * Stages a direct copy between two vulkan buffers, bypassing the need to allocate
     * an intermediate staging buffer. The region copied starts at the first byte in both
     * buffers and runs to end of the smallest buffer.
     *
     * <p>Although technically more efficient, this can result in concurrent modification
     * with multiple frames-in-flight that {@link #stage(Streamable)} usually guards against.</p>
     *
     * @param src source buffer
     * @param dst destination buffer
     */
    public void stageDirect(VulkanBuffer src, VulkanBuffer dst) {
        if (!src.getUsage().contains(BufferUsage.TransferSrc)) {
            throw new IllegalArgumentException("Source buffer must have the TransferSrc flag set.");
        }
        if (!dst.getUsage().contains(BufferUsage.TransferDst)) {
            throw new IllegalArgumentException("Destination buffer must have the TransferDst flag set.");
        }
        DirectCopy copy = new DirectCopy(src, dst, 1);
        copy.data.get().set(0, 0, Math.min(src.size().getBytes(), dst.size().getBytes()));
        copyCommands.add(copy);
    }

    /**
     * Uploads changes staged to this buffer stream to the destination buffers.
     * If no changes have been staged, this does nothing.
     *
     * @param cmd command buffer to submit the necessary commands to
     * @see #stage(Streamable)
     */
    public void upload(CommandBuffer cmd) {
        if (copyCommands.isEmpty()) {
            return;
        }
        Collection<CopyCmd> toRelease = new ArrayList<>(copyCommands.size());
        for (CopyCmd c; (c = copyCommands.poll()) != null;) {
            vkCmdCopyBuffer(cmd.getBuffer(), c.getSrc().getGpuObject(), c.getDst().getGpuObject(), c.getParameters());
            toRelease.add(c);
        }
        cmd.onExecutionComplete(() -> {
            for (CopyCmd c : toRelease) {
                c.release();
            }
        });
    }

    /**
     * {@link #stage() Stages} registered streamable objects and
     * {@link #upload(CommandBuffer) uploads}.
     *
     * @param cmd command buffer
     */
    public void stream(CommandBuffer cmd) {
        stage();
        upload(cmd);
    }

    /**
     * Returns true if changes are staged to this buffer stream that require
     * {@link #upload(CommandBuffer) uploading}.
     *
     * @return true if changes are staged
     */
    public boolean hasChanges() {
        return !copyCommands.isEmpty();
    }

    public void setRegionMergeGap(int regionMergeGap) {
        this.regionMergeGap = regionMergeGap;
    }

    public void setMaxRegions(int maxRegions) {
        this.maxRegions = maxRegions;
    }

    public int getRegionMergeGap() {
        return regionMergeGap;
    }

    public int getMaxRegions() {
        return maxRegions;
    }

    /**
     * Allocates {@code bytes} of free space in a streaming page. If not enough
     * consecutive space is found in an existing page, a new page is created that
     * is guaranteed to contain at least enough space for the allocation.
     *
     * @param bytes consecutive bytes to allocate
     * @return allocated page partition
     */
    private BufferPartition<StreamingPage> allocatePartition(int bytes) {
        for (StreamingPage s : pages) {
            BufferPartition<StreamingPage> p = s.allocatePartition(bytes);
            if (p != null) return p;
        }
        StreamingPage s = new StreamingPage(device, MemorySize.bytes(Math.max(bytes, pageByteSize)));
        pages.add(s);
        return s.allocatePartition(bytes);
    }

    private static class StreamingPage extends PersistentBuffer<VulkanBuffer> {

        private final AllocatedRegion head = new AllocatedRegion(this, 0, 0);

        public StreamingPage(LogicalDevice<?> device, MemorySize size) {
            super(create(device, size));
        }

        private static VulkanBuffer create(LogicalDevice<?> device, MemorySize size) {
            return HostVisibleBuffer.build(device, size, b -> {
                b.setUsage(BufferUsage.TransferSrc);
                b.setConcurrent(true);
            });
        }

        public BufferPartition<StreamingPage> allocatePartition(int bytes) {
            for (AllocatedRegion r = head, prev = null; r != null; r = r.next) {
                BufferPartition<StreamingPage> p = r.allocateBytesAfter(bytes);
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

        public boolean releasePartition(BufferPartition<StreamingPage> partition) {
            for (AllocatedRegion r = head; r != null; r = r.next) {
                if (r.release(partition)) {
                    return true;
                }
            }
            return false;
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

        public BufferPartition<StreamingPage> allocateBytesAfter(int bytes) {
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
                    return new BufferPartition<>(stream, MemorySize.bytes(pStart, bytes));
                }
            }
            return null;
        }

        public boolean release(BufferPartition partition) {
            if (start == end) {
                return false;
            }
            long pEnd = partition.size().getEnd();
            if (pEnd <= start || partition.getOffset() >= end) {
                return false;
            }
            synchronized (this) {
                if (partition.getOffset() > start && pEnd < end) {
                    AllocatedRegion r = new AllocatedRegion(stream, pEnd, end);
                    r.next = next;
                    next = r;
                    end = partition.getOffset();
                } else if (partition.getOffset() <= start) {
                    start = pEnd;
                } else {
                    end = partition.getOffset();
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

        public final BufferPartition<StreamingPage> src;
        public final VulkanBuffer dst;
        public final VkBufferCopy.Buffer data;

        public StreamCopy(BufferPartition<StreamingPage> src, VulkanBuffer dst, int regions) {
            this.src = src;
            this.dst = dst;
            this.data = VkBufferCopy.calloc(regions);
        }

        @Override
        public VulkanBuffer getSrc() {
            return src.getBuffer().getBuffer();
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
            src.getBuffer().releasePartition(src);
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
