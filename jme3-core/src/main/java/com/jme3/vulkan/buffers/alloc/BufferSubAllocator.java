package com.jme3.vulkan.buffers.alloc;

import com.jme3.export.*;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.SharingMode;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.buffers.saving.BufferAllocator;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class BufferSubAllocator implements BufferAllocator<VulkanBuffer>, Savable {

    private final Map<BufferAllocRequest, Collection<Page>> pages = new HashMap<>();
    @Final private long pageSize;

    @SerializationOnly
    protected BufferSubAllocator() {}

    public BufferSubAllocator(long pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public VulkanBuffer allocate(long bytes, BufferAllocRequest<VulkanBuffer> alloc) {
        return new Handle(allocateSub(bytes, alloc));
    }

    private SubBuffer allocateSub(long bytes, BufferAllocRequest<VulkanBuffer> alloc) {
        Collection<Page> type = pages.computeIfAbsent(alloc, k -> new ArrayList<>());
        for (Page p : type) {
            SubBuffer sub = p.allocate(bytes);
            if (sub != null) {
                return sub;
            }
        }
        Page p = new Page(Math.max(bytes, pageSize), alloc);
        type.add(p);
        return p.allocate(bytes);
    }

    public void flush() {
        for (Collection<Page> type : pages.values()) {
            type.removeIf(Page::isEmpty);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(pageSize, "pageSize", 0);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        pageSize = in.readInt("pageSize", 0);
    }

    private class Page {

        private final BufferAllocRequest<VulkanBuffer> allocReq;
        private final VulkanBuffer buffer;
        private SubBuffer head;

        public Page(long bytes, BufferAllocRequest<VulkanBuffer> allocReq) {
            this.allocReq = allocReq;
            this.buffer = allocReq.create(bytes);
        }

        public SubBuffer allocate(long bytes) {
            if (head == null && bytes <= buffer.size().getBytes()) {
                return head = new SubBuffer(this, new MemorySize(0, bytes));
            }
            for (SubBuffer region = head; region != null; region = region.next) {
                long space = region.getBytesAfter();
                if (bytes <= space) {
                    SubBuffer sub = new SubBuffer(this, new MemorySize(region.size.getEnd(), bytes));
                    region.insertAfter(sub);
                    return sub;
                }
            }
            return null;
        }

        public void upload(CommandBuffer cmd, BufferStream stream) {
            for (SubBuffer sub = head; sub != null; sub = sub.next) {
                sub.performCopy(cmd);
            }
            buffer.upload(cmd, stream);
        }

        public boolean isEmpty() {
            return head == null;
        }

    }

    private class SubBuffer {

        private final Page page;
        private MemorySize size;
        private SubBuffer prev, next;
        private SubBuffer copyDst, copySrc;

        public SubBuffer(Page page, MemorySize size) {
            this.page = page;
            this.size = size;
        }

        public long getBytesAfter() {
            if (next != null) return next.size.getOffset() - size.getEnd();
            else return page.buffer.size().getEnd() - size.getEnd();
        }

        public long getAvailableBytes() {
            if (next != null) return next.size.getOffset() - size.getOffset();
            else return page.buffer.size().getEnd() - size.getOffset();
        }

        public SubBuffer resize(long bytes) {
            if (bytes <= getAvailableBytes()) {
                size = size.setBytes(bytes);
                return this;
            } else if (copySrc != null) {
                // this subbuffer is already staged to copy from a source
                // so cut this buffer out and copy directly to the new target
                remove();
                copySrc.copyDst = allocateSub(bytes, page.allocReq);
                copySrc.copyDst.copySrc = copySrc;
                copySrc = copyDst = null;
                return copySrc.copyDst;
            } else {
                copyDst = allocateSub(bytes, page.allocReq);
                copyDst.copySrc = this;
                return copyDst;
            }
        }

        public void insertAfter(SubBuffer sub) {
            sub.prev = this;
            sub.next = next;
            next.prev = sub;
            next = sub;
        }

        public void remove() {
            if (prev != null) prev.next = next;
            if (next != null) next.prev = prev;
            if (page.head == this) page.head = next;
        }

        public void performCopy(CommandBuffer cmd) {
            if (copyDst != null) try (MemoryStack stack = MemoryStack.stackPush()) {
                VkBufferCopy.Buffer copy = VkBufferCopy.malloc(1, stack);
                copy.get().set(size.getOffset(), copyDst.size.getOffset(), size.getBytes());
                LogicalDevice<?> device = cmd.getPool().getDevice();
                VK10.vkCmdCopyBuffer(cmd.getBuffer(), page.buffer.getBufferId(device), copyDst.page.buffer.getBufferId(device), copy);
                cmd.onExecutionComplete(this::remove);
                copyDst.copySrc = null;
                copyDst = null;
            }
        }

        public BufferSubAllocator getAllocator() {
            return BufferSubAllocator.this;
        }

    }

    private static class Handle implements VulkanBuffer, Disposable {

        private SubBuffer sub;
        @Final private DisposableReference ref;

        @SerializationOnly
        protected Handle() {}

        public Handle(SubBuffer sub) {
            this.sub = sub;
            ref = DisposableManager.reference(this);
        }

        @Override
        public Runnable createDestroyer() {
            return sub::remove;
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        @Override
        public void upload(CommandBuffer cmd, BufferStream stream) {
            sub.page.upload(cmd, stream);
        }

        @Override
        public long getBufferId(LogicalDevice<?> device) {
            return sub.page.buffer.getBufferId(device);
        }

        @Override
        public Flag<BufferUsage> getUsage() {
            return sub.page.buffer.getUsage();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return sub.page.buffer.getMemoryProperties();
        }

        @Override
        public IntEnum<SharingMode> getSharingMode() {
            return sub.page.buffer.getSharingMode();
        }

        @Override
        public BufferMapping map(long offset, long size) {
            return sub.page.buffer.map(sub.size.getOffset() + offset, size);
        }

        @Override
        public void stage(long offset, long size) {
            sub.page.buffer.stage(sub.size.getOffset() + offset, size);
        }

        @Override
        public void resize(long bytes) {
            sub = sub.resize(bytes);
            ref.refresh();
        }

        @Override
        public MemorySize size() {
            return sub.size;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(sub.getAllocator(), "allocator", null);
            out.write(sub.page.allocReq, "request", null);
            if (!sub.page.buffer.isMemoryMappable()) {
                out.write(sub.size.getBytes(), "size", 0L);
            } else try (BufferMapping m = map()) {
                out.write(m.getBytes(), "bytes", null);
            }
        }

        @Override
        @FinalWriter
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            BufferSubAllocator allocator = (BufferSubAllocator)in.readSavable("allocator", null);
            BufferAllocRequest req = (BufferAllocRequest)in.readSavable("request", null);
            ByteBuffer bytes = in.readByteBuffer("bytes", null);
            if (bytes != null) {
                sub = allocator.allocateSub(bytes.limit(), req);
                if (sub.page.buffer.isMemoryMappable()) try (BufferMapping m = map()) {
                    MemoryUtil.memCopy(bytes, m.getBytes());
                }
            } else {
                sub = allocator.allocateSub(in.readLong("size", 0L), req);
            }
            ref = DisposableManager.reference(this);
        }

    }

}
