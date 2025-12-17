package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
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

public class BufferStream {

    private final LogicalDevice<?> device;
    private final Collection<StreamingPage> pages = new ConcurrentLinkedQueue<>();
    private final Queue<CopyInfo> copyCommands = new ConcurrentLinkedQueue<>();
    private final Collection<WeakReference<Streamable>> buffers = new ConcurrentLinkedQueue<>();
    private final int pageByteSize;

    public BufferStream(LogicalDevice<?> device, int pageByteSize) {
        this.device = device;
        this.pageByteSize = pageByteSize;
    }

    public void stream() {
        for (Iterator<WeakReference<Streamable>> it = buffers.iterator(); it.hasNext();) {
            Streamable b = it.next().get();
            if (b != null) stream(b);
            else it.remove();
        }
    }

    public void add(Streamable buffer) {
        buffers.add(new WeakReference<>(buffer));
    }

    /**
     * Streams changes from a source buffer to a destination buffer.
     *
     * @param object
     */
    public void stream(Streamable object) {
        VulkanBuffer dst = object.getDstBuffer();
        if (!dst.getUsage().contains(BufferUsage.TransferDst)) {
            throw new IllegalArgumentException("Cannot stream to " + dst + ": not a transfer destination.");
        }
        if (object.getUpdateRegions().getCoverage() == 0) {
            return;
        }
        BufferPartition<StreamingPage> partition = allocatePartition(object.getUpdateRegions().getCoverage());
        if (partition == null) {
            throw new NullPointerException("Failed to allocate streaming page partition.");
        }
        ByteBuffer srcBytes = object.mapBytes();
        ByteBuffer partitionBytes = partition.mapBytes();
        CopyInfo copy = new CopyInfo(partition, dst, object.getUpdateRegions().getNumRegions());
        int partitionOffset = 0;
        for (DirtyRegions.Region r : object.getUpdateRegions()) {
            if (r.getEnd() > srcBytes.limit()) {
                throw new IllegalStateException("Buffer region extends outside source buffer.");
            }
            MemoryUtil.memCopy(MemoryUtil.memAddress(srcBytes, r.getOffset()), MemoryUtil.memAddress(partitionBytes, partitionOffset), r.getSize());
            copy.data.get().set(partitionOffset, r.getOffset(), r.getSize());
            partitionOffset += r.getSize();
        }
        object.getUpdateRegions().clear();
        copy.data.flip();
        copyCommands.add(copy);
    }

    public void upload(CommandBuffer cmd) {
        Collection<CopyInfo> toRelease = new ArrayList<>(copyCommands.size());
        for (CopyInfo c; (c = copyCommands.poll()) != null;) {
            vkCmdCopyBuffer(cmd.getBuffer(), c.src.getId(), c.dst.getId(), c.data);
            toRelease.add(c);
        }
        cmd.onExecutionComplete(() -> {
            for (CopyInfo c : toRelease) {
                c.freeData();
                c.releasePartition();
            }
        });
    }

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
            return BasicVulkanBuffer.build(device, size, b -> {
                b.setUsage(BufferUsage.TransferSrc);
                b.setMemFlags(MemoryProp.HostVisibleAndCoherent);
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
        private int start, end;
        private AllocatedRegion next;

        public AllocatedRegion(StreamingPage stream, int start, int end) {
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
                int a = availableBytesAfter();
                if (a >= bytes) {
                    int pStart = end;
                    if (a == bytes) {
                        next.start = start;
                        end = start; // collapse island
                    } else {
                        end += bytes;
                    }
                    return new BufferPartition<>(stream, pStart, MemorySize.bytes(bytes));
                }
            }
            return null;
        }

        public boolean release(BufferPartition partition) {
            if (start == end) {
                return false;
            }
            int pEnd = partition.getOffset() + partition.size().getBytes();
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

        public int availableBytesAfter() {
            if (next == null) return stream.size().getBytes() - end;
            else return next.start - end;
        }

    }

    private static class CopyInfo {

        public final BufferPartition<StreamingPage> src;
        public final VulkanBuffer dst;
        public final VkBufferCopy.Buffer data;

        public CopyInfo(BufferPartition<StreamingPage> src, VulkanBuffer dst, int regions) {
            this.src = src;
            this.dst = dst;
            this.data = VkBufferCopy.calloc(regions);
        }

        public void freeData() {
            MemoryUtil.memFree(data);
        }

        public void releasePartition() {
            src.getBuffer().releasePartition(src);
        }

    }

}
