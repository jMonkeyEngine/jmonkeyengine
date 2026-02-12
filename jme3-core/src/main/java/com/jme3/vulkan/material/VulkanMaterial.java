package com.jme3.vulkan.material;

import com.jme3.material.Material;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.pipeline.Pipeline;

public interface VulkanMaterial extends Material {

    void bind(CommandBuffer cmd, Pipeline pipeline, DescriptorPool pool);

}
