package com.jme3.vulkan.pipeline;

import com.jme3.material.RenderState;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import com.jme3.vulkan.util.Flag;

public interface PipelineBuilder {

    void applyMesh(Mesh mesh);

    void applyRenderState(RenderState state);

    void createLayoutFromBindingSets(ShaderBindingSet... sets);

    void setTopology(Topology topology);

    void setDepthTest(boolean depthTest);

    void setStencilTest(boolean stencilTest);

    void setPolygonMode(PolygonMode polygonMode);

    void setCullMode(Flag<CullMode> cullMode);

    void setLineWidth(float lineWidth);



}
