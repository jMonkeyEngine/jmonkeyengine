package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.NioBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.buffers.stream.DirtyRegions;
import com.jme3.vulkan.buffers.stream.Streamable;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class StreamingBuffer extends NioBuffer implements VulkanBuffer, Streamable {

    private final DeviceLocalBuffer deviceBuffer;
    private final DirtyRegions regions = new DirtyRegions();

    public StreamingBuffer(LogicalDevice<?> device, MemorySize size, Flag<BufferUsage> usage) {
        super(size);
        deviceBuffer = DeviceLocalBuffer.build(device, size, b -> b.setUsage(usage.add(BufferUsage.TransferDst)));
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return deviceBuffer.getDevice();
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return deviceBuffer.getUsage();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return deviceBuffer.getMemoryProperties();
    }

    @Override
    public boolean isConcurrent() {
        return deviceBuffer.isConcurrent();
    }

    @Override
    public void push(int offset, int size) {
        regions.add(offset, size);
    }

    @Override
    public long getId() {
        return deviceBuffer.getId();
    }

    @Override
    public VulkanBuffer getDstBuffer() {
        return deviceBuffer;
    }

    @Override
    public DirtyRegions getUpdateRegions() {
        return regions;
    }

}
