package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

public abstract class AbstractUniform <T> implements Uniform<T> {

    protected final String name;
    protected final IntEnum<Descriptor> type;
    protected final int bindingIndex;
    protected final Flag<ShaderStage> stages;
    protected VersionedResource<? extends T> resource;

    public AbstractUniform(String name, IntEnum<Descriptor> type, int bindingIndex, Flag<ShaderStage> stages) {
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

    @Override
    public void setResource(VersionedResource<? extends T> resource) {
        this.resource = resource;
    }

    @Override
    public VersionedResource<? extends T> getResource() {
        return resource;
    }

    public IntEnum<Descriptor> getType() {
        return type;
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

}
