package com.jme3.vulkan.mesh;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.pipeline.PolygonMode;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.util.IntEnum;

public interface VulkanMesh extends Mesh {

    void render(CommandBuffer cmd, VertexPipeline pipeline);

    GpuBuffer getVertexBuffer(VertexBinding binding);

    IntEnum<Topology> getTopology();

    IntEnum<PolygonMode> getPolygonMode();

}
