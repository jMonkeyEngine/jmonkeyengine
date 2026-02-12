package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.AdaptiveEnum;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum Topology implements AdaptiveEnum<Topology> {

    LineList,
    LineStrip,
    LineLoop,
    TriangleList,
    PatchList,
    PointList,
    LineListAdjacency,
    LineStripAdjacency,
    TriangleFan,
    TriangleListAdjacency,
    TriangleStrip,
    TriangleStripAdjacency;

    private int vkEnum = -1;

    @Override
    public int getEnum() {
        return vkEnum;
    }

    @Override
    public Topology set(int enumVal) {
        this.vkEnum = enumVal;
        return this;
    }

}
