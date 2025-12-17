package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum BlendOp implements IntEnum<BlendOp> {

    Add(VK_BLEND_OP_ADD),
    Min(VK_BLEND_OP_MIN),
    Max(VK_BLEND_OP_MAX),
    Subtract(VK_BLEND_OP_SUBTRACT),
    ReverseSubtract(VK_BLEND_OP_REVERSE_SUBTRACT);

    private final int vk;

    BlendOp(int vk) {
        this.vk = vk;
    }

    @Override
    public int getEnum() {
        return vk;
    }

}
