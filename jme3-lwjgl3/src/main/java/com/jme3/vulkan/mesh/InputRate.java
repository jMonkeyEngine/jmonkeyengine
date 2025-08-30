package com.jme3.vulkan.mesh;

import static org.lwjgl.vulkan.VK10.*;

public enum InputRate {

    Vertex(VK_VERTEX_INPUT_RATE_VERTEX),
    Instance(VK_VERTEX_INPUT_RATE_INSTANCE);

    private final int vkEnum;

    InputRate(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int getVkEnum() {
        return vkEnum;
    }

}
