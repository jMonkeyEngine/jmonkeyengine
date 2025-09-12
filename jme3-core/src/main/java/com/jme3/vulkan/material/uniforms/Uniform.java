package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;

public interface Uniform <T> extends DescriptorSetWriter {

    /**
     * Gets the name by which this uniform is identified.
     */
    String getName();

    /**
     * Updates this uniform and extracts the uniform value from the
     * {@link #setResource(VersionedResource) data pipe}.
     *
     * @param cmd command buffer to submit commands to
     */
    void update(CommandBuffer cmd);

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
    void setResource(VersionedResource<? extends T> resource);

    /**
     * Returns the {@link VersionedResource} supplying the uniform value.
     */
    VersionedResource<? extends T> getResource();

    /**
     * The binding this uniform is targeting. Should be unique among all
     * uniforms within a single {@link com.jme3.vulkan.material.UniformSet UniformSet}.
     *
     * @return the index of the target binding
     */
    int getBindingIndex();

}
