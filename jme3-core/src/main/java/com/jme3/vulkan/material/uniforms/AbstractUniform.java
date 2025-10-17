package com.jme3.vulkan.material.uniforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.FlagParser;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.ReflectionArgs;

import java.util.Objects;

public abstract class AbstractUniform <T> implements Uniform<T> {

    protected final String name;
    protected final IntEnum<Descriptor> type;
    protected final int bindingIndex;
    protected final Flag<ShaderStage> stages;
    protected T value;

    public AbstractUniform(String name, IntEnum<Descriptor> type, int bindingIndex, Flag<ShaderStage> stages) {
        this.name = name;
        this.type = type;
        this.bindingIndex = bindingIndex;
        this.stages = stages;
    }

    public AbstractUniform(IntEnum<Descriptor> type, ReflectionArgs args) {
        this.name = args.getName();
        this.type = type;
        this.bindingIndex = Objects.requireNonNull(args.getProperties().get("binding"),
                "Binding index is not specified for \"" + name + "\"").asInt();
        this.stages = FlagParser.parseFlag(ShaderStage.class,
                args.getProperties().get("stages"), ShaderStage.All);
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
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    public IntEnum<Descriptor> getType() {
        return type;
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

}
