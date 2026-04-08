package com.jme3.vulkan.material.technique;

import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

public interface NewTechnique extends Cloneable {

    void setBinding(int set, String name, UniformBinding binding);

    void setShaderSource(ShaderStage stage, String assetName);

    void linkDefine(String defineName, String uniformName, Flag<ShaderStage> scope);

    NewTechnique clone();

}
