package com.jme3.vulkan.render.batching;

import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.pipeline.VertexPipeline;

public interface VulkanBatchElement extends BatchElement {

    @Override
    VulkanMaterial getMaterial();

    @Override
    VulkanMesh getMesh();

    VulkanTechnique getTechnique();

    VertexPipeline getPipeline();

}
