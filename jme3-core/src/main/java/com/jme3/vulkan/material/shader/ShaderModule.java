package com.jme3.vulkan.material.shader;

import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.spvc.CompiledShaderCode;

public interface ShaderModule {

    CompiledShaderCode getSource();

    ShaderType getType();

    String getEntryPoint();

}
