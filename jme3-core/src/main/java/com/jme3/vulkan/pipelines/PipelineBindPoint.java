package com.jme3.vulkan.pipelines;

import static org.lwjgl.vulkan.VK10.*;

public enum PipelineBindPoint {

    Graphics(VK_PIPELINE_BIND_POINT_GRAPHICS),
    Compute(VK_PIPELINE_BIND_POINT_COMPUTE);

    private final int vkEnum;

    PipelineBindPoint(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int getVkEnum() {
        return vkEnum;
    }

}
