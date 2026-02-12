package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.SourceBufferMapping;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class HostVisibleBuffer extends AbstractVulkanBuffer {

    protected HostVisibleBuffer(LogicalDevice<?> device, MemorySize size) {
        super(device, size);
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return new SourceBufferMapping(this, getMemory().map(this.size.getOffset() + offset, size), size);
    }

    @Override
    public void push(long offset, long size) {}

    @Override
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (size.getEnd() > getMemory().getSize()) {
            new Builder().build();
            return ResizeResult.Realloc;
        }
        return ResizeResult.Success;
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

        private boolean lazilyAllocated = getMemory() != null && getMemory().getFlags().contains(MemoryProp.LazilyAllocated);

        @Override
        protected HostVisibleBuffer construct() {
            ByteBuffer prev = ref != null ? mapBytes() : null;
            construct(MemoryProp.HostVisibleAndCoherent.addIf(lazilyAllocated, MemoryProp.LazilyAllocated));
            if (prev != null) {
                MemoryUtil.memCopy(prev, mapBytes());
            }
            return HostVisibleBuffer.this;
        }

        public void setLazilyAllocated(boolean lazilyAllocated) {
            this.lazilyAllocated = lazilyAllocated;
        }

    }

}
