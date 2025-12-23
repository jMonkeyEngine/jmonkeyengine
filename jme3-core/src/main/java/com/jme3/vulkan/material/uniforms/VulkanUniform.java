package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

public interface VulkanUniform <T> extends Uniform<T> {

    /**
     * Creates a {@link DescriptorSetWriter} that can be used to write
     * a snapshot of the uniform's current value into the
     * {@link com.jme3.vulkan.descriptors.DescriptorSet DescriptorSet}.
     *
     * <p>The returned writer is expected to implement {@link Object#equals(Object)
     * equals} and {@link Object#hashCode() hashCode} so that writers can be compared
     * for DescriptorSet caching.</p>
     *
     * @return writer, or null if this uniform's value cannot be written to a descriptor set
     */
    DescriptorSetWriter createWriter(SetLayoutBinding binding);

    /**
     * Creates a binding from the parameters that can be used for this uniform.
     *
     * @param type binding type
     * @param binding binding index
     * @param stages shader stages the binding is accessible from
     * @return created binding
     */
    SetLayoutBinding createBinding(IntEnum<Descriptor> type, int binding, Flag<ShaderStage> stages);

}
