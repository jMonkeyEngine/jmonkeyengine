package com.jme3.vulkan.flags;

import static org.lwjgl.vulkan.VK10.*;

public class BufferUsageFlags {

    private int usageFlags;

    public BufferUsageFlags vertexBuffer() {
        usageFlags |= VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        return this;
    }

    public BufferUsageFlags indexBuffer() {
        usageFlags |= VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        return this;
    }

    public BufferUsageFlags transferSrc() {
        usageFlags |= VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        return this;
    }

    public BufferUsageFlags transferDst() {
        usageFlags |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        return this;
    }

    public BufferUsageFlags uniformBuffer() {
        usageFlags |= VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
        return this;
    }

    public int getUsageFlags() {
        return usageFlags;
    }

}
