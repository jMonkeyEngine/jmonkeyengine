package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;

public interface Uniform <T> {

    /**
     * Creates a {@link DescriptorSetWriter} that can be used to write
     * a snapshot of the uniform's current value into the
     * {@link com.jme3.vulkan.descriptors.DescriptorSet DescriptorSet}.
     *
     * <p>The returned writer is expected to implement
     * {@link Object#equals(Object) equals} so that writers can be compared
     * for DescriptorSet caching.</p>
     *
     * @return writer
     */
    DescriptorSetWriter createWriter(SetLayoutBinding binding);

    /**
     * Sets the {@link VersionedResource} that will provide the uniform value.
     */
    void set(T value);

    /**
     * Returns the {@link VersionedResource} supplying the uniform value.
     */
    T get();

}
