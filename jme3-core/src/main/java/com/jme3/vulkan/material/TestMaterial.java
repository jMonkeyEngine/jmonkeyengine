package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.shader.ShaderStage;

public class TestMaterial extends NewMaterial {

    private final TextureUniform baseColorMap = new TextureUniform(
            "BaseColorMap", VulkanImage.Layout.ShaderReadOnlyOptimal, 1, ShaderStage.Fragment);

    public TestMaterial(DescriptorPool pool) {
        super(pool);
        addSet(0, baseColorMap);
    }

    public TextureUniform getBaseColorMap() {
        return baseColorMap;
    }

}
