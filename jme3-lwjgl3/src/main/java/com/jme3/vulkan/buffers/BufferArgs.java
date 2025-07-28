package com.jme3.vulkan.buffers;

import static org.lwjgl.vulkan.VK10.*;

public class BufferArgs {

    private int usage;
    private int memFlags = 0;
    private boolean concurrent = false;

    public BufferArgs vertexBuffer() {
        usage |= VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        return this;
    }

    public BufferArgs indexBuffer() {
        usage |= VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        return this;
    }

    public BufferArgs transferSrc() {
        usage |= VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        return this;
    }

    public BufferArgs transferDst() {
        usage |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        return this;
    }

    public BufferArgs hostVisible() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
        return this;
    }

    public BufferArgs hostCoherent() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
        return this;
    }

    public BufferArgs hostCached() {
        memFlags |= VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
        return this;
    }

    public BufferArgs deviceLocal() {
        memFlags |= VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
        return this;
    }

    public BufferArgs lazilyAllocated() {
        memFlags |= VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
        return this;
    }

    public BufferArgs setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
        return this;
    }

    public int getUsage() {
        return usage;
    }

    public int getMemoryFlags() {
        return memFlags;
    }

    public boolean isConcurrent() {
        return concurrent;
    }

    public int getSharingMode() {
        return concurrent ? VK_SHARING_MODE_CONCURRENT : VK_SHARING_MODE_EXCLUSIVE;
    }

}
