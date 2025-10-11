package com.jme3.vulkan.material;

import com.jme3.material.Material;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pipelines.Pipeline;
import com.jme3.vulkan.pipelines.PipelineCache;
import com.jme3.vulkan.pipelines.newstate.PipelineState;

public interface VulkanMaterial extends Material {

    boolean bind(CommandBuffer cmd, Pipeline pipeline);

    Pipeline selectPipeline(PipelineCache cache, MeshDescription description,
                            String forcedTechnique, PipelineState overrideState);

}
