package com.jme3.vulkan;

import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.KHRSurface;

public enum ColorSpace implements IntEnum<ColorSpace> {

    KhrSrgbNonlinear(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);

    private final int vkEnum;

    ColorSpace(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
