package com.jme3.vulkan.material.uniforms;

public class ShaderParam <T> {

    private T value;

    /**
     * Sets the value of this uniform.
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Returns the value of this uniform.
     */
    public T get() {
        return value;
    }

}
