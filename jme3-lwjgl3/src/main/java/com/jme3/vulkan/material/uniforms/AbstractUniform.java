package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;

public abstract class AbstractUniform <T> implements Uniform<T> {

    protected final String name;
    protected final Descriptor type;
    protected final int bindingIndex;

    public AbstractUniform(String name, Descriptor type, int bindingIndex) {
        this.name = name;
        this.type = type;
        this.bindingIndex = bindingIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBindingIndex() {
        return bindingIndex;
    }

    public Descriptor getType() {
        return type;
    }

}
