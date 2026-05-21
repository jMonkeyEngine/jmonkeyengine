package com.jme3.vulkan.mesh;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.pipeline.VertexPipeline;

public interface VulkanMesh extends Mesh {

    void render(CommandBuffer cmd, VertexPipeline pipeline);

}
