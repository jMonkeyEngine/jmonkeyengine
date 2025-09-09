package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum CullMode implements Flag<CullMode> {

    None(VK_CULL_MODE_NONE),
    Back(VK_CULL_MODE_BACK_BIT),
    Front(VK_CULL_MODE_FRONT_BIT),
    FrontAndBack(VK_CULL_MODE_FRONT_AND_BACK);

    private final int bits;

    CullMode(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }

}
