package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum IndexType implements IntEnum<IndexType> {

    UInt32(VK_INDEX_TYPE_UINT32, 2),
    UInt16(VK_INDEX_TYPE_UINT16, 4);

    private final int vkEnum;
    private final int bytes;

    IndexType(int vkEnum, int bytes) {
        this.vkEnum = vkEnum;
        this.bytes = bytes;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public int getBytes() {
        return bytes;
    }

    public static IndexType of(MappableBuffer buffer) {
        if (buffer.size().getBytesPerElement() <= UInt16.bytes) {
            return UInt16;
        } else {
            return UInt32;
        }
    }

}
