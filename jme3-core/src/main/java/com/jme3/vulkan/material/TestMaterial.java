package com.jme3.vulkan.material;

import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;

public class TestMaterial extends NewMaterial {

    private final BufferUniform matrices = new BufferUniform();
    private final TextureUniform baseColorMap = new TextureUniform(VulkanImage.Layout.ShaderReadOnlyOptimal);

    public TestMaterial() {
        addUniform("Matrices", matrices);
        addUniform("BaseColorMap", baseColorMap);
    }

    public BufferUniform getMatrices() {
        return matrices;
    }

    public TextureUniform getBaseColorMap() {
        return baseColorMap;
    }

}
