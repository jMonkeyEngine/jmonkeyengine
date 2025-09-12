package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.shader.ShaderStage;

public class TestMaterial extends Material {

    private final BufferUniform matrices = new BufferUniform(
            "Matrices", Descriptor.UniformBuffer, 0, ShaderStage.Vertex);
    private final TextureUniform baseColorMap = new TextureUniform(
            "BaseColorMap", VulkanImage.Layout.ShaderReadOnlyOptimal, 1, ShaderStage.Fragment);

    public TestMaterial(DescriptorPool pool) {
        super(pool);
        addSet(matrices, baseColorMap);
    }

    public BufferUniform getMatrices() {
        return matrices;
    }

    public TextureUniform getBaseColorMap() {
        return baseColorMap;
    }

}
