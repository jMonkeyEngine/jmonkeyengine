package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.formats.EnumInterpreter;

public enum Topology {

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

    public int getEnum(EnumInterpreter interpreter) {
        return interpreter.getTopologyEnum(this);
    }

}
