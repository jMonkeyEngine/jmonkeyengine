package com.jme3.vulkan.material.exp2;

import com.jme3.backend.Engine;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.material.experimental.SetBindCommand;
import com.jme3.vulkan.material.experimental.ShadingGlobals;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.pipeline.state.GraphicsState;
import com.jme3.vulkan.render.bucket.RenderElement;

import java.util.Collection;
import java.util.Queue;
import java.util.function.Supplier;

public interface RenderSession <T extends Engine> extends AutoCloseable {

    @Override
    void close();

    void render(Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques);

    RenderElement createRenderElement(ViewPort vp, Geometry g, GraphicsState state);

    void stageShaderSet(int location, SetBindCommand set);

    void bindShaderSets();

    <G extends ShadingGlobals> G acquireGlobals(Class<G> type, Supplier<G> factory);

    T getEngine();

}
