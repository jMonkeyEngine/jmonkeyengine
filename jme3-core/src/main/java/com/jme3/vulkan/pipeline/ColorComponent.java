package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum ColorComponent implements Flag<ColorComponent> {

    R(VK_COLOR_COMPONENT_R_BIT),
    G(VK_COLOR_COMPONENT_G_BIT),
    B(VK_COLOR_COMPONENT_B_BIT),
    A(VK_COLOR_COMPONENT_A_BIT),
    All(VK_COLOR_COMPONENT_R_BIT
        | VK_COLOR_COMPONENT_G_BIT
        | VK_COLOR_COMPONENT_B_BIT
        | VK_COLOR_COMPONENT_A_BIT);

    private final int vk;

    ColorComponent(int vk) {
        this.vk = vk;
    }

    @Override
    public int bits() {
        return vk;
    }

}
