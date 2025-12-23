package com.jme3.vulkan.surface;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.KHRSurface.*;

public enum CompositeAlpha implements Flag<CompositeAlpha> {

    Opaque(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR),
    Inherit(VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR),
    PostMultiplied(VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR),
    PreMultiplied(VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR);

    private final int vk;

    CompositeAlpha(int vk) {
        this.vk = vk;
    }

    @Override
    public int bits() {
        return vk;
    }

}
