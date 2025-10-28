package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum PipelineBindPoint implements IntEnum<PipelineBindPoint> {

    Graphics(VK_PIPELINE_BIND_POINT_GRAPHICS),
    Compute(VK_PIPELINE_BIND_POINT_COMPUTE);

    private final int vkEnum;

    PipelineBindPoint(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
