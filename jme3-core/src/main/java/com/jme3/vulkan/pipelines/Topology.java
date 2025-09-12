package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum Topology implements IntEnum<Topology> {

    LineList(VK_PRIMITIVE_TOPOLOGY_LINE_LIST),
    LineStrip(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP),
    TriangleList(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST),
    PatchList(VK_PRIMITIVE_TOPOLOGY_PATCH_LIST),
    PointList(VK_PRIMITIVE_TOPOLOGY_POINT_LIST),
    LineListAdjacency(VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY),
    LineStripAdjacency(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY),
    TriangleFan(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN),
    TriangleListAdjacency(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY),
    TriangleStrip(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP),
    TriangleStripAdjacency(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY);

    private final int vkEnum;

    Topology(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
