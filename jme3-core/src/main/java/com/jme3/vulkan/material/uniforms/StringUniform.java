package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;

public class StringUniform implements Uniform<String> {

    private String value;

    public StringUniform() {}

    public StringUniform(String value) {
        this.value = value;
    }

    @Override
    public void set(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public String getDefineValue() {
        return value;
    }

}
