package com.jme3.vulkan.material;

import com.jme3.material.Material;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.pipeline.states.PipelineState;

public interface VkMaterial extends Material {

    boolean bind(CommandBuffer cmd, Pipeline pipeline);

    Pipeline selectPipeline(PipelineCache cache, MeshDescription mesh,
                            String forcedTechnique, PipelineState overrideState);

}
