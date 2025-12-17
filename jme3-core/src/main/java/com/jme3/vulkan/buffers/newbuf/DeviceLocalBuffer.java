package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.function.Consumer;

public class DeviceLocalBuffer extends AbstractVulkanBuffer implements VulkanBuffer {

    protected DeviceLocalBuffer(LogicalDevice<?> device, MemorySize size) {
        super(device, size);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped.");
    }

    @Override
    public void unmap() {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped.");
    }

    public static DeviceLocalBuffer build(LogicalDevice<?> device, MemorySize size, Consumer<Builder> config) {
        Builder b = new DeviceLocalBuffer(device, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractVulkanBuffer.Builder<DeviceLocalBuffer> {

        private boolean lazilyAllocated = false;

        @Override
        protected DeviceLocalBuffer construct() {
            construct(MemoryProp.DeviceLocal.addIf(lazilyAllocated, MemoryProp.LazilyAllocated));
            return DeviceLocalBuffer.this;
        }

        public void setLazilyAllocated(boolean lazilyAllocated) {
            this.lazilyAllocated = lazilyAllocated;
        }

    }

}
