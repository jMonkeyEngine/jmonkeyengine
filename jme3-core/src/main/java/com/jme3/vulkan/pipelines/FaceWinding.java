package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum FaceWinding implements IntEnum<FaceWinding> {

    Clockwise(VK_FRONT_FACE_CLOCKWISE),
    CounterClockwise(VK_FRONT_FACE_COUNTER_CLOCKWISE);

    private final int vkEnum;

    FaceWinding(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
