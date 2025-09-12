package com.jme3.vulkan.buffers;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum BufferUsage implements Flag<BufferUsage> {

    Uniform(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
    Index(VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
    Storage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT),
    StorageTexel(VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT),
    Indirect(VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT),
    TransferDst(VK_BUFFER_USAGE_TRANSFER_DST_BIT),
    TransferSrc(VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
    UniformTexel(VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT),
    Vertex(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);

    private final int vkEnum;

    BufferUsage(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int bits() {
        return vkEnum;
    }

}
