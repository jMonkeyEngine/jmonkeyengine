package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.AgnosticEnum;

public enum Topology implements AgnosticEnum<Topology> {

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
