package com.jme3.vulkan.memory;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum MemoryFlag implements Flag<MemoryFlag> {

    HostVisible(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
    HostCoherent(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
    HostCached(VK_MEMORY_PROPERTY_HOST_CACHED_BIT),
    DeviceLocal(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    LazilyAllocated(VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT);

    private final int vkEnum;

    MemoryFlag(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int bits() {
        return vkEnum;
    }

}
