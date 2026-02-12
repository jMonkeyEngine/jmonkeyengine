package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.material.technique.PushConstantRange;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;

public interface VulkanUniform <T> extends Uniform<T> {

    /**
     * Creates a new {@link DescriptorSetWriter} that can be used to write
     * a snapshot of the uniform's current value into a
     * {@link com.jme3.vulkan.descriptors.DescriptorSet DescriptorSet}.
     *
     * <p>Writers are compared using {@code equals} and {@code hashCode} in
     * order to determine if a new writer outdates an old cached writer and
     * thus updates should be rewritten to the descriptor set.</p>
     *
     * @return writer, or null if this uniform's value cannot be written to a descriptor set
     */
    DescriptorSetWriter createWriter(SetLayoutBinding binding);

    /**
     * Creates a binding from the parameters that can be used to bind this uniform
     *
     * @param binding binding index
     * @param scope shader stages the binding is accessible from
     * @return created binding
     */
    SetLayoutBinding createBinding(int binding, Flag<ShaderStage> scope);

    void fillPushConstantsBuffer(PushConstantRange constant, BufferMapping mapping);

}
