package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum LogicOp implements IntEnum<LogicOp> {

    Copy(VK_LOGIC_OP_COPY),
    And(VK_LOGIC_OP_AND),
    Or(VK_LOGIC_OP_OR),
    None(VK_LOGIC_OP_NO_OP),
    Clear(VK_LOGIC_OP_CLEAR),
    AndInverted(VK_LOGIC_OP_AND_INVERTED),
    AndReverse(VK_LOGIC_OP_AND_REVERSE),
    CopyInverted(VK_LOGIC_OP_COPY_INVERTED),
    Equivalent(VK_LOGIC_OP_EQUIVALENT),
    Invert(VK_LOGIC_OP_INVERT),
    Nand(VK_LOGIC_OP_NAND),
    Nor(VK_LOGIC_OP_NOR),
    OrInverted(VK_LOGIC_OP_OR_INVERTED),
    OrReverse(VK_LOGIC_OP_OR_REVERSE),
    Set(VK_LOGIC_OP_SET),
    Xor(VK_LOGIC_OP_XOR);

    private final int vkEnum;

    LogicOp(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
