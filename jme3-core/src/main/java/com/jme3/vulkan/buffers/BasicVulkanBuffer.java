package com.jme3.vulkan.buffers;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.BufferOverflowException;
import java.nio.LongBuffer;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class BasicVulkanBuffer extends AbstractNative<Long> implements VulkanBuffer {

    private final LogicalDevice<?> device;
    private MemorySize size;
    private MemoryRegion memory;
    protected Flag<BufferUsage> usage = BufferUsage.Storage;
    protected boolean concurrent = false;
    protected long padding;

    protected BasicVulkanBuffer(LogicalDevice<?> device, MemorySize size) {
        this.device = device;
        this.size = size;
    }

    @Override
    public Long getGpuObject() {
        return object;
    }

    @Override
    public void push(int offset, int size) {}

    @Override
    public Runnable createDestroyer() {
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
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (memory != null && size.getBytes() > memory.getSize()) {
            Builder b = new Builder();
            b.setMemFlags(getMemoryProperties());
            b.build();
            return true;
        }
        return false;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return memory.getFlags();
    }

    protected MemoryRegion getMemory() {
        return memory;
    }

    public void setPadding(long padding) {
        assert padding >= 0 : "Padding cannot be negative.";
        this.padding = padding;
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return usage;
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    public long getPadding() {
        return padding;
    }

    public static BasicVulkanBuffer build(LogicalDevice<?> device, MemorySize size, Consumer<Builder> config) {
        Builder b = new BasicVulkanBuffer(device, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<BasicVulkanBuffer> {

        protected Flag<MemoryProp> memFlags = MemoryProp.DeviceLocal;

        @Override
        protected BasicVulkanBuffer construct() {
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
            memory = MemoryRegion.build(device, bufferMem.size(), m -> {
                m.setFlags(memFlags);
                m.setUsableMemoryTypes(bufferMem.memoryTypeBits());
            });
            memory.bind(BasicVulkanBuffer.this, 0);
            ref = DisposableManager.reference(BasicVulkanBuffer.this);
            device.getReference().addDependent(ref);
            memory.getReference().addDependent(ref);
            return BasicVulkanBuffer.this;
        }

        public Builder setMemFlags(Flag<MemoryProp> memFlags) {
            this.memFlags = memFlags;
            return this;
        }

        public Builder setUsage(Flag<BufferUsage> usage) {
            BasicVulkanBuffer.this.usage = usage;
            return this;
        }

        public Builder setSize(MemorySize size) {
            BasicVulkanBuffer.this.size = size;
            return this;
        }

        public Builder setConcurrent(boolean concurrent) {
            BasicVulkanBuffer.this.concurrent = concurrent;
            return this;
        }

    }

}
