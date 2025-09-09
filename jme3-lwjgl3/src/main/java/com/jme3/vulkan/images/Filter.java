package com.jme3.vulkan.images;

import com.jme3.vulkan.util.IntEnum;

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

}
