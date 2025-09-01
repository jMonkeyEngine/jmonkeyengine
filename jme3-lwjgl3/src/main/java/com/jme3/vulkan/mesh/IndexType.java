package com.jme3.vulkan.mesh;

import com.jme3.vulkan.util.LibEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum IndexType implements LibEnum<IndexType> {

    UInt32(VK_INDEX_TYPE_UINT32),
    UInt16(VK_INDEX_TYPE_UINT16);

    private final int vkEnum;

    IndexType(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
