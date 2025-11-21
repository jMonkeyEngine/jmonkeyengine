package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public class PersistentVulkanBuffer <T extends VulkanBuffer> extends PersistentBuffer<T> implements VulkanBuffer {

    public PersistentVulkanBuffer(T buffer) {
        super(buffer);
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return getBuffer().getDevice();
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return getBuffer().getUsage();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return getBuffer().getMemoryProperties();
    }

    @Override
    public boolean isConcurrent() {
        return getBuffer().isConcurrent();
    }

}
