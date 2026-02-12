package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.uniforms.StructUniform;
import com.jme3.vulkan.material.shader.ShaderStage;

/**
 * Material specifically for storing matrix transforms for a geometry.
 */
public class MatrixTransformMaterial extends NewMaterial {

    private final StructUniform transforms = new StructUniform("Transforms",
            Descriptor.UniformBuffer, 0, ShaderStage.Vertex);

    public MatrixTransformMaterial(DescriptorPool pool) {
        super(pool);
        addSet(0, transforms);
    }

    public StructUniform getTransforms() {
        return transforms;
    }

}
