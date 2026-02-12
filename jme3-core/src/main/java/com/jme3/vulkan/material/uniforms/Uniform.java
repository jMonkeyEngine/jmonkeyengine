package com.jme3.vulkan.material.uniforms;

import com.jme3.backend.Engine;
import com.jme3.vulkan.struct.StdLayoutType;

import java.nio.ByteBuffer;

public interface Uniform <T> {

    /**
     * Basic value that can be assigned to a shader define to enable it.
     */
    String ENABLED_DEFINE = "1";

    /**
     * Sets the value of this uniform.
     */
    void set(T value);

    /**
     * Returns the value of this uniform.
     */
    T get();

    /**
     * Gets this uniform's value as a string assignable to a shader define.
     * The define value can be used directly by this uniform if {@link #getDefineName()}
     * does not return null. Techniques may also specify links between
     *
     * @return define value, or null to not generate a shader define for this uniform
     */
    String getDefineValue();

}
