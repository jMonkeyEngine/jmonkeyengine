package com.jme3.vulkan.shader;

import com.jme3.util.Versionable;

public interface Define extends Versionable {

    /**
     * Gets the define as named in the shader.
     *
     * @return define name
     */
    String getDefineName();

    /**
     * Gets the value assigned to the define in the shader.
     *
     * @return define value, as a string
     */
    String getDefineValue();

    /**
     * Returns true if this define is active and should be
     * included in the shader.
     *
     * @return true if active
     */
    boolean isDefineActive();

}
