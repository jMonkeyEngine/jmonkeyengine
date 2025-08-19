package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

public abstract class AbstractUniform <T> implements Uniform<T> {

    protected final String name;
    protected final Descriptor type;
    protected final int bindingIndex;
    protected final Flag<ShaderStage> stages;

    public AbstractUniform(String name, Descriptor type, int bindingIndex, Flag<ShaderStage> stages) {
        this.name = name;
        this.type = type;
        this.bindingIndex = bindingIndex;
        this.stages = stages;
    }

    @Override
    public SetLayoutBinding createBinding() {
        return new SetLayoutBinding(type, bindingIndex, 1, stages);
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
