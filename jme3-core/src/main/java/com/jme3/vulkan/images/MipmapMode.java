package com.jme3.vulkan.images;

import com.jme3.texture.GlTexture;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum MipmapMode implements IntEnum<MipmapMode> {

    Linear(VK_SAMPLER_MIPMAP_MODE_LINEAR),
    Nearest(VK_SAMPLER_MIPMAP_MODE_NEAREST);

    private final int vkEnum;

    MipmapMode(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public static MipmapMode of(GlTexture.MinFilter min) {
        switch (min) {
            case NearestLinearMipMap:
            case Trilinear: return Linear;
            default: return Nearest;
        }
    }

}
