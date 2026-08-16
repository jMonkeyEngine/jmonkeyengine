package com.jme3.vulkan.images;

import com.jme3.texture.GlTexture;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum FilterMode implements IntEnum<FilterMode> {

    Linear(VK_FILTER_LINEAR),
    Nearest(VK_FILTER_NEAREST);

    private final int vkEnum;

    FilterMode(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public static FilterMode of(GlTexture.MinFilter min) {
        switch (min) {
            case BilinearNearestMipMap:
            case BilinearNoMipMaps:
            case Trilinear: return Linear;
            default: return Nearest;
        }
    }

    public static FilterMode of(GlTexture.MagFilter mag) {
        if (mag == GlTexture.MagFilter.Bilinear) return Linear;
        else return Nearest;
    }

}
