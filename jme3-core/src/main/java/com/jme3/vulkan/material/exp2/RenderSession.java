package com.jme3.vulkan.material.exp2;

import com.jme3.backend.Engine;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.util.functional.Function;
import com.jme3.vulkan.material.experimental.SetBind;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import com.jme3.vulkan.material.experimental.ShaderProgram;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.pipeline.PipelineBuilder;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.render.bucket.RenderElement;

import java.util.Collection;
import java.util.Comparator;
import java.util.Queue;
import java.util.function.Consumer;

public interface RenderSession extends AutoCloseable {

    @Override
    void close();

    void render(Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques);

    Function<Geometry, RenderElement> createElementMapper(ViewPort vp, Consumer<PipelineBuilder>)

    void stageShaderSets(int location, SetBind... sets);

    void bindShaderSets();

    Engine getEngine();

}
