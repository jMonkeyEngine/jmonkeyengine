package com.jme3.vulkan.material.technique;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class PushConstantRange {

    private final String name;
    private final Flag<ShaderStage> scope;
    private final MemorySize size;

    public PushConstantRange(String name, Flag<ShaderStage> scope, MemorySize size) {
        this.name = name;
        this.scope = scope;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public Flag<ShaderStage> getScope() {
        return scope;
    }

    public MemorySize getSize() {
        return size;
    }

}
