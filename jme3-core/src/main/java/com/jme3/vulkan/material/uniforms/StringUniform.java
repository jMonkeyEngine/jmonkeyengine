package com.jme3.vulkan.material.uniforms;

public class StringUniform implements ShaderParam<String> {

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
