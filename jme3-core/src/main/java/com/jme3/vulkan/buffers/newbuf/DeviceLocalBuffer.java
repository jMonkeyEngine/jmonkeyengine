package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.function.Consumer;

public class DeviceLocalBuffer extends AbstractVulkanBuffer {

    private BufferStream stream;

    protected DeviceLocalBuffer(LogicalDevice<?> device, MemorySize size) {
        super(device, size);
    }

    private DeviceLocalBuffer(DeviceLocalBuffer alias) {
        super(alias.getDevice(), alias.size());
        object = alias.object;
        ref = alias.ref;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped.");
    }

    @Override
    public void unmap() {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped.");
    }

    @Override
    public void push(long offset, long size) {}

    @Override
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (size.getEnd() > getMemory().getSize()) {
            new Builder().build();
            return stream != null ? ResizeResult.Realloc : ResizeResult.DataLost;
        }
        return ResizeResult.Success;
    }

    public void setStream(BufferStream stream) {
        this.stream = stream;
    }

    public BufferStream getStream() {
        return stream;
    }

    public static DeviceLocalBuffer build(LogicalDevice<?> device, MemorySize size, Consumer<Builder> config) {
        Builder b = new DeviceLocalBuffer(device, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractVulkanBuffer.Builder<DeviceLocalBuffer> {

        private boolean lazilyAllocated = getMemory() != null && getMemory().getFlags().contains(MemoryProp.LazilyAllocated);

        @Override
        protected DeviceLocalBuffer construct() {
            DeviceLocalBuffer prev = null;
            if (stream != null && ref != null) {
                prev = new DeviceLocalBuffer(DeviceLocalBuffer.this);
                ref = null;
            }
            construct(MemoryProp.DeviceLocal.addIf(lazilyAllocated, MemoryProp.LazilyAllocated));
            if (prev != null) {
                stream.stageDirect(prev, DeviceLocalBuffer.this);
            }
            return DeviceLocalBuffer.this;
        }

        public void setLazilyAllocated(boolean lazilyAllocated) {
            this.lazilyAllocated = lazilyAllocated;
        }

    }

}
