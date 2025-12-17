package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.function.Consumer;

public class HostVisibleBuffer extends AbstractVulkanBuffer {

    protected HostVisibleBuffer(LogicalDevice<?> device, MemorySize size) {
        super(device, size);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return getMemory().map(offset, size);
    }

    @Override
    public void unmap() {
        getMemory().unmap();
    }

    public static HostVisibleBuffer build(LogicalDevice<?> device, MemorySize size, Consumer<Builder> config) {
        Builder b = new HostVisibleBuffer(device, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractVulkanBuffer.Builder<HostVisibleBuffer> {

        private boolean lazilyAllocated = false;

        @Override
        protected HostVisibleBuffer construct() {
            construct(MemoryProp.HostVisibleAndCoherent.addIf(lazilyAllocated, MemoryProp.LazilyAllocated));
            return HostVisibleBuffer.this;
        }

        public void setLazilyAllocated(boolean lazilyAllocated) {
            this.lazilyAllocated = lazilyAllocated;
        }

    }

}
