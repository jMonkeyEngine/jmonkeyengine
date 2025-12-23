package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;

public interface Uniform <T> {

    /**
     * Basic value that can be assigned to a define to simply enable it.
     */
    String ENABLED_DEFINE = "1";

    /**
     * Sets the {@link VersionedResource} that will provide the uniform value.
     */
    void set(T value);

    /**
     * Returns the {@link VersionedResource} supplying the uniform value.
     */
    T get();

    /**
     * Gets this uniform's value as a string assignable to a shader define.
     *
     * @return define value, or null to not generate a define for this uniform
     */
    String getDefineValue();

}
