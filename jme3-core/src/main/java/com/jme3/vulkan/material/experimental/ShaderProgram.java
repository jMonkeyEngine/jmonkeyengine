package com.jme3.vulkan.material.experimental;

import com.jme3.material.RenderState;
import com.jme3.vulkan.shaderc.ShaderType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShaderProgram {

    private final Map<ShaderType, String> sources = new HashMap<>();
    private final RenderState state = new RenderState();

    protected ShaderProgram() {}

    public Map<ShaderType, String> getSources() {
        return Collections.unmodifiableMap(sources);
    }

    public RenderState getState() {
        return state;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {

        private final ShaderProgram program = new ShaderProgram();

        private Builder() {}

        public Builder addSource(ShaderType type, String source) {
            program.sources.put(type, source);
            return this;
        }

        public ShaderProgram build() {
            return program;
        }

    }

}
