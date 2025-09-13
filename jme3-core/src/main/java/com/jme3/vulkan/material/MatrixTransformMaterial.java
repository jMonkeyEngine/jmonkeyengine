package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.shader.ShaderStage;

/**
 * Material specifically for storing matrix transforms for a geometry.
 */
public class MatrixTransformMaterial extends NewMaterial {

    private final BufferUniform transforms = new BufferUniform("Transforms",
            Descriptor.UniformBuffer, 0, ShaderStage.Vertex);

    public MatrixTransformMaterial(DescriptorPool pool) {
        super(pool);
        addSet(transforms);
    }

    public BufferUniform getTransforms() {
        return transforms;
    }

}
