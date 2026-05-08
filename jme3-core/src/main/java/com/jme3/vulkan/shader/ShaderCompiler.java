package com.jme3.vulkan.shader;

public interface ShaderCompiler <IN, OUT> {

    OUT compile(IN shader);

}
