package com.jme3.vulkan.render;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.render.batching.GeometryBatch;

public interface BatchRenderer {

    void render(GeometryBatch batch);

    void setViewPortArea(ViewPortArea area);

    void setScissorArea(ScissorArea area);

}
