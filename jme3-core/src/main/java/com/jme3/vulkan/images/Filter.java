package com.jme3.vulkan.images;

import com.jme3.texture.GlTexture;
import com.jme3.vulkan.util.IntEnum;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public enum Filter implements IntEnum<Filter> {

    Linear(VK_FILTER_LINEAR),
    Nearest(VK_FILTER_NEAREST);

    private final int vkEnum;

    Filter(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public static Filter of(GlTexture.MinFilter min) {
        switch (min) {
            case BilinearNearestMipMap:
            case BilinearNoMipMaps:
            case Trilinear: return Linear;
            default: return Nearest;
        }
    }

    public static Filter of(GlTexture.MagFilter mag) {
        if (mag == GlTexture.MagFilter.Bilinear) return Linear;
        else return Nearest;
    }

}
