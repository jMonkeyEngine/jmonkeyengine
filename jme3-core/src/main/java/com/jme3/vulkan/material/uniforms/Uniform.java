package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;

public interface Uniform <T> {

    /**
     * Gets the name by which this uniform is identified.
     */
    String getName();

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
    DescriptorSetWriter createWriter();

    /**
     * Tests if the {@link SetLayoutBinding} is compatible with this uniform,
     * indicating that this uniform may be bound to the binding which it represents.
     * Bindings that previously tested as compatible should always be compatible with
     * this uniform.
     *
     * @param binding binding to test
     * @return true if the binding is compatible
     */
    boolean isBindingCompatible(SetLayoutBinding binding);

    /**
     * Creates a new {@link SetLayoutBinding} that is completely (and always will be)
     * {@link #isBindingCompatible(SetLayoutBinding) compatible} with this uniform.
     *
     * @return new set layout binding that is compatible with this uniform
     */
    SetLayoutBinding createBinding();

    /**
     * Sets the {@link VersionedResource} that will provide the uniform value.
     */
    void set(T value);

    /**
     * Returns the {@link VersionedResource} supplying the uniform value.
     */
    T get();

    /**
     * The binding this uniform is targeting. Should be unique among all
     * uniforms within a single {@link com.jme3.vulkan.material.UniformSet UniformSet}.
     *
     * @return the index of the target binding
     */
    int getBindingIndex();

}
