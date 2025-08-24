package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.VersionedResource;

public interface Uniform <T> extends DescriptorSetWriter {

    /**
     * Gets the name by which this uniform is identified.
     */
    String getName();

    /**
     * Updates this uniform.
     *
     * @param device the current logical device
     */
    void update(LogicalDevice<?> device);

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
     * Sets the {@link VersionedResource} containing the resources this
     * uniform is to represent.
     *
     * <p>Changing the value will typically result in the {@link #getVariant()
     * variant index} being increment, requiring {@link com.jme3.vulkan.descriptors.DescriptorSet
     * DescriptorSets} to be partially rewritten. Changing the value itself will
     * rarely ever require rewriting DescriptorSets.</p>
     */
    void setValue(VersionedResource<T> value);

    /**
     * Returns the {@link VersionedResource} containing the resources this
     * uniform represents.
     */
    VersionedResource<T> getValue();

    /**
     * The binding this uniform is targeting. Should be unique among all
     * uniforms within a single {@link com.jme3.vulkan.material.UniformSet UniformSet}.
     *
     * @return the index of the target binding
     */
    int getBindingIndex();

    /**
     * Gets the variant index of this uniform. The variant index starts at {@code 0},
     * and be incremented each time the uniform is changed enough to require a
     * rewrite of {@link com.jme3.vulkan.descriptors.DescriptorSet DescriptorSets}.
     *
     * @return the variant index
     */
    long getVariant();

    /**
     * Returns the {@link VersionedResource VersionedResource's} version for the current frame.
     *
     * <p>Is functionally identical to</p>
     * <pre><code>
     * Uniform&lt;T&gt; uniform = ...
     * T version = uniform.getValue().getVersion();
     * </code></pre>
     */
    default T getVersion() {
        return getValue().getVersion();
    }

}
