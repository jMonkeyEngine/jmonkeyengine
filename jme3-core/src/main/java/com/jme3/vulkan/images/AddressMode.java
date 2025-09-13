package com.jme3.vulkan.images;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum AddressMode implements IntEnum<AddressMode> {

    ClampToBorder(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER),
    ClampToEdge(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE),
    Repeat(VK_SAMPLER_ADDRESS_MODE_REPEAT),
    MirroredRepeat(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT);

    private final int vkEnum;

    AddressMode(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
