package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum CompareOp implements IntEnum<CompareOp> {

    LessOrEqual(VK_COMPARE_OP_LESS_OR_EQUAL),
    Always(VK_COMPARE_OP_ALWAYS),
    Equal(VK_COMPARE_OP_EQUAL),
    Greater(VK_COMPARE_OP_GREATER),
    Less(VK_COMPARE_OP_LESS),
    GreaterOrEqual(VK_COMPARE_OP_GREATER_OR_EQUAL),
    Never(VK_COMPARE_OP_NEVER),
    NotEqual(VK_COMPARE_OP_NOT_EQUAL);

    private final int vkEnum;

    CompareOp(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
