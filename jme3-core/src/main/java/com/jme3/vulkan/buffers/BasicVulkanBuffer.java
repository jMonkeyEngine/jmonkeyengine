package com.jme3.vulkan.buffers;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.BufferOverflowException;
import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class BasicVulkanBuffer extends AbstractNative<Long> implements VulkanBuffer {

    private final LogicalDevice<?> device;
    private MemorySize size;
    private MemoryRegion memory;
    protected Flag<BufferUsage> usage = BufferUsage.Storage;
    protected boolean concurrent = false;
    protected long padding;

    public BasicVulkanBuffer(LogicalDevice<?> device, MemorySize size) {
        this(device, size, 0);
    }

    public BasicVulkanBuffer(LogicalDevice<?> device, MemorySize size, long padding) {
        this.device = device;
        this.size = size;
        this.padding = padding;
    }

    @Override
    public long getId() {
        return object;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyBuffer(device.getNativeObject(), object, null);
    }

    @Override
    public void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size.getBytes()) {
            throw new BufferOverflowException();
        }
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return memory.map(offset, size);
    }

    @Override
    public void unmap() {
        memory.unmap();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return device;
    }

    @Override
    public boolean resize(int elements) {
        if (elements < 0) {
            throw new IllegalArgumentException("Buffer size cannot be negative.");
        }
        if (elements != size.getElements()) {
            size = new MemorySize(elements, size.getBytesPerElement());
            if (memory != null && size.getBytes() > memory.getSize()) {
                build().close();
                return true;
            }
        }
        return false;
    }

    protected MemoryRegion getMemory() {
        return memory;
    }

    public void setPadding(long padding) {
        assert padding >= 0 : "Padding cannot be negative.";
        this.padding = padding;
    }

    public Flag<BufferUsage> getUsage() {
        return usage;
    }

    public boolean isConcurrent() {
        return concurrent;
    }

    public long getPadding() {
        return padding;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        protected Flag<MemoryProp> memFlags = MemoryProp.DeviceLocal;

        @Override
        protected void build() {
            if (ref != null) {
                ref.destroy();
                ref = null;
            }
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes() + padding * size.getBytesPerElement())
                    .usage(usage.bits())
                    .sharingMode(VulkanUtils.sharingMode(concurrent));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, idBuf),
                    "Failed to create buffer.");
            object = idBuf.get(0);
            VkMemoryRequirements bufferMem = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getNativeObject(), object, bufferMem);
            memory = new MemoryRegion(device, bufferMem.size());
            try (MemoryRegion.Builder m = memory.build()) {
                m.setFlags(memFlags);
                m.setUsableMemoryTypes(bufferMem.memoryTypeBits());
            }
            memory.bind(BasicVulkanBuffer.this, 0);
            ref = Native.get().register(BasicVulkanBuffer.this);
            device.getNativeReference().addDependent(ref);
            memory.getNativeReference().addDependent(ref);
        }

        public void setMemFlags(Flag<MemoryProp> memFlags) {
            this.memFlags = memFlags;
        }

        public void setUsage(Flag<BufferUsage> usage) {
            BasicVulkanBuffer.this.usage = usage;
        }

        public void setSize(MemorySize size) {
            BasicVulkanBuffer.this.size = size;
        }

        public void setConcurrent(boolean concurrent) {
            BasicVulkanBuffer.this.concurrent = concurrent;
        }

    }

}
