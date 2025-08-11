package com.jme3.vulkan.flags;

import static org.lwjgl.vulkan.VK10.*;

public class MemoryFlags {

    private int memFlags = 0;

    public MemoryFlags hostVisible() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
        return this;
    }

    public MemoryFlags hostCoherent() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
        return this;
    }

    public MemoryFlags hostCached() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
        return this;
    }

    public MemoryFlags deviceLocal() {
        memFlags |= VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
        return this;
    }

    public MemoryFlags lazilyAllocated() {
        memFlags |= VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
        return this;
    }

    public int getMemoryFlags() {
        return memFlags;
    }

}
