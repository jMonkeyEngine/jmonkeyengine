package com.jme3.vulkan.material.uniforms.def;

import com.jme3.backend.Engine;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.uniforms.ShaderParam;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

public interface UniformDef <T extends ShaderParam> {

    T createUniform(Engine engine);

    UniformBinding createBinding(IntEnum<Descriptor> type, int binding, Flag<ShaderStage> stages);

}
