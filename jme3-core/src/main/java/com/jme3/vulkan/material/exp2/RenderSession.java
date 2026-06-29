package com.jme3.vulkan.material.exp2;

import com.jme3.backend.Engine;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.material.experimental.SetBindCommand;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.pipeline.state.GraphicsState;
import com.jme3.vulkan.render.bucket.GraphicsElement;

import java.util.Collection;
import java.util.Queue;

public interface RenderSession <T extends Engine> extends AutoCloseable {

    @Override
    void close();

    void render(Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques);

    GraphicsElement createRenderElement(ViewPort vp, Geometry g, GraphicsState state);

    void stageShaderSet(int location, SetBindCommand set);

    void bindShaderSets();

    T getEngine();

}
