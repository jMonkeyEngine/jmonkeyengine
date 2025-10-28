package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum PolygonMode implements IntEnum<PolygonMode> {

    Fill(VK_POLYGON_MODE_FILL),
    Line(VK_POLYGON_MODE_LINE),
    Point(VK_POLYGON_MODE_POINT);

    private final int vkEnum;

    PolygonMode(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

}
