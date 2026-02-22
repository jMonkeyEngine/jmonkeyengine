package com.jme3.vulkan.buffers.newbuf;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;

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
    private BufferHandle handle;
    private ByteBuffer tempBuffer;

    protected AbstractVulkanBuffer(MemorySize size) {
        this.size = size;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (handle != null) {
            return mapNative(handle, offset, size);
        } else {
            if (tempBuffer == null) {
                tempBuffer = MemoryUtil.memAlloc((int)this.size.getBytes());
            }
            return new VirtualBufferMapping(MemoryUtil.memByteBuffer(MemoryUtil.memAddress(tempBuffer, (int)offset), (int)size));
        }
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (handle != null && size.getEnd() > handle.memory.getSize()) {
            initialize(null);
            return ResizeResult.Realloc;
        } else if (tempBuffer != null && size.getEnd() > tempBuffer.capacity()) {
            tempBuffer = MemoryUtil.memRealloc(tempBuffer, (int)size.getEnd());
            return ResizeResult.Realloc;
        }
        return ResizeResult.Success;
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

    protected void initialize(LogicalDevice<?> device) {
        if (device == null && (handle == null || (device = handle.device) == null)) {
            throw new IllegalStateException("Logical device not specified.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes())
                    .usage(usage.bits())
                    .sharingMode(sharing.getEnum());
            BufferHandle handle = new BufferHandle(stack, device, this, create, m -> m.setFlags(memProps));
            if (this.handle != null) {
                moveToNewBuffer(this.handle, handle);
            } else if (tempBuffer != null) {
                long size = Math.min(tempBuffer.capacity(), handle.getMemory().getSize());
                tempBuffer.position(0).limit((int)size);
                try (BufferMapping m = mapNative(handle, 0, size)) {
                    MemoryUtil.memCopy(tempBuffer, m.getBytes());
                }
            }
            if (tempBuffer != null) {
                MemoryUtil.memFree(tempBuffer);
                tempBuffer = null;
            }
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
