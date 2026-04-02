package com.jme3.vulkan.buffers.newbuf;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public abstract class AbstractVulkanBuffer implements VulkanBuffer {

    protected MemorySize size;
    protected Flag<BufferUsage> usage = BufferUsage.Storage;
    protected Flag<MemoryProp> memProps = Flag.empty();
    protected IntEnum<SharingMode> sharing = SharingMode.Exclusive;
    protected final Flag<MemoryProp> implicitMemProps;
    private BufferHandle handle;
    private TempBuffer temp;

    @SerializationOnly
    protected AbstractVulkanBuffer(Flag<MemoryProp> implicitMemProps) {
        this.implicitMemProps = implicitMemProps;
    }

    protected AbstractVulkanBuffer(long bytes, Flag<MemoryProp> implicitMemProps) {
        this.size = new MemorySize(bytes);
        this.implicitMemProps = implicitMemProps;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(size.getBytes(), "size", 0L);
        out.write(usage.bits(), "usage", BufferUsage.Vertex.bits());
        out.write(memProps.bits(), "memoryProps", implicitMemProps.bits());
        out.write(sharing.getEnum(), "sharingMode", SharingMode.Exclusive.getEnum());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        size = new MemorySize(in.readLong("size", 0L));
        Builder b = new Builder();
        b.setUsage(Flag.of(in.readInt("usage", BufferUsage.Vertex.bits())));
        b.setMemoryProps(Flag.of(in.readInt("memoryProps", implicitMemProps.bits())));
        b.setSharingMode(IntEnum.of(in.readInt("sharingMode", SharingMode.Exclusive.getEnum())));
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (!isMemoryMappable()) {
            throw new UnsupportedOperationException("Device local memory cannot be mapped.");
        }
        if (handle != null) {
            return mapNative(handle, offset, size);
        } else {
            if (temp == null) {
                temp = new TempBuffer(this.size.getBytes());
            }
            return new DirectBufferMapping(temp.getBuffer().position((int)offset).limit((int)(offset + size)).slice());
        }
    }

    @Override
    public void resize(long bytes) {
        size = size.setBytes(bytes);
        if (handle != null && size.getEnd() > handle.memory.getSize()) {
            initialize(null);
        } else if (temp != null && size.getEnd() > temp.getBuffer().capacity()) {
            temp.buffer = MemoryUtil.memRealloc(temp.getBuffer(), (int)size.getEnd());
        }
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return usage;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return handle.memory.getFlags();
    }

    @Override
    public IntEnum<SharingMode> getSharingMode() {
        return sharing;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public long getBufferId(LogicalDevice<?> device) {
        if (handle == null) {
            initialize(device);
        }
        return handle.getNativeObject();
    }

    @Override
    public void upload(CommandBuffer cmd, BufferStream stream) {

    }

    @Override
    public void stage(long offset, long size) {

    }

    protected void initialize(LogicalDevice<?> device) {
        if (device == null && (handle == null || (device = handle.device) == null)) {
            throw new IllegalStateException("Logical device not specified.");
        }
        memProps = memProps.add(implicitMemProps);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes())
                    .usage(usage.bits())
                    .sharingMode(sharing.getEnum());
            BufferHandle handle = new BufferHandle(stack, device, this, create, m -> m.setFlags(memProps));
            if (this.handle != null) {
                moveToNewBuffer(this.handle, handle);
            } else if (temp != null) {
                if (!isMemoryMappable()) {
                    throw new UnsupportedOperationException("Unable to copy from temporary buffer: device local memory cannot be mapped.");
                }
                long size = Math.min(temp.getBuffer().capacity(), handle.getMemory().getSize());
                temp.getBuffer().position(0).limit((int)size);
                try (BufferMapping m = mapNative(handle, 0, size)) {
                    MemoryUtil.memCopy(temp.getBuffer(), m.getBytes());
                }
            }
            temp = null;
            this.handle = handle;
        }
    }

    protected abstract BufferMapping mapNative(BufferHandle handle, long offset, long size);

    protected abstract void moveToNewBuffer(BufferHandle oldHandle, BufferHandle newHandle);

    protected void unmap() {
        handle.memory.unmap();
    }

    protected BufferHandle getHandle() {
        return handle;
    }

    protected static class BufferHandle extends AbstractNative<Long> {

        private final LogicalDevice<?> device;
        private final MemoryRegion memory;

        protected BufferHandle(MemoryStack stack, LogicalDevice<?> device, VulkanBuffer buffer,
                               VkBufferCreateInfo create, Consumer<MemoryRegion.Builder> memConfig) {
            this.device = device;
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, handle), "Failed to create buffer.");
            memory = MemoryRegion.buildBufferMemory(stack, device, buffer, memConfig);
            ref = DisposableManager.reference(this);
            device.getReference().addDependent(ref);
            memory.getReference().addDependent(ref);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> vkDestroyBuffer(device.getNativeObject(), object, null);
        }

        public LogicalDevice<?> getDevice() {
            return device;
        }

        public MemoryRegion getMemory() {
            return memory;
        }

    }

    protected static class TempBuffer implements Disposable {

        private ByteBuffer buffer;
        private final DisposableReference ref;

        public TempBuffer(long bytes) {
            buffer = MemoryUtil.memCalloc((int)bytes);
            ref = DisposableManager.reference(this);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> MemoryUtil.memFree(buffer);
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

    }

    public class Builder {

        protected Builder() {}

        public void setUsage(Flag<BufferUsage> usage) {
            AbstractVulkanBuffer.this.usage = usage;
        }

        public void setSharingMode(IntEnum<SharingMode> mode) {
            sharing = mode;
        }

        public void setMemoryProps(Flag<MemoryProp> props) {
            memProps = props;
        }

        public Flag<BufferUsage> getUsage() {
            return usage;
        }

        public IntEnum<SharingMode> getSharingMode() {
            return sharing;
        }

        public Flag<MemoryProp> getMemoryProps() {
            return memProps;
        }

    }

}
