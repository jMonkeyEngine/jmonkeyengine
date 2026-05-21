package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.render.bucket.GeometryBucket;

public interface ShadingTechnique {

    void render(RenderSession session, ViewPort vp, GeometryBucket bucket);

}
