package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.render.bucket.RenderElement;
import com.jme3.vulkan.render.bucket.GeometryBucket;

import java.util.function.Function;

public interface ShadingTechnique {

    void render(RenderSession session, ViewPort vp, GeometryBucket bucket, Function<Geometry, RenderElement> elementFactory);

    ShaderProgram getProgram();

}
