package com.jme3.vulkan.images;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum BorderColor implements IntEnum<BorderColor> {

    FloatOpaqueBlack(VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK),
    FloatOpaqueWhite(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE),
    FloatTransparentBlack(VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK),
    IntOpaqueBlack(VK_BORDER_COLOR_INT_OPAQUE_BLACK),
    IntOpaqueWhite(VK_BORDER_COLOR_INT_OPAQUE_WHITE),
    IntTransparentBlack(VK_BORDER_COLOR_INT_TRANSPARENT_BLACK);

    private final int vkEnum;

    BorderColor(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
