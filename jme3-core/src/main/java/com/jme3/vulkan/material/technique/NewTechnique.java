package com.jme3.vulkan.material.technique;

import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

public interface NewTechnique extends Cloneable {

    void setBinding(int set, String name, SetLayoutBinding binding);

    void setShaderSource(ShaderStage stage, String assetName);

    void removeShader(ShaderStage stage);

    void linkDefine(String defineName, String uniformName, Flag<ShaderStage> scope);

    void unlinkDefine(String defineName);

    NewTechnique clone();

}
