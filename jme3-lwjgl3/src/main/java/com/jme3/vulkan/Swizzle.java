package com.jme3.vulkan;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum Swizzle implements IntEnum<Swizzle> {

    Identity(VK_COMPONENT_SWIZZLE_IDENTITY),
    R(VK_COMPONENT_SWIZZLE_R),
    G(VK_COMPONENT_SWIZZLE_G),
    B(VK_COMPONENT_SWIZZLE_B),
    A(VK_COMPONENT_SWIZZLE_A),
    One(VK_COMPONENT_SWIZZLE_ONE),
    Zero(VK_COMPONENT_SWIZZLE_ZERO);

    private final int vkEnum;

    Swizzle(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
