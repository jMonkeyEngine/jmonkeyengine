package com.jme3.vulkan.material.uniforms.def;

import com.jme3.backend.Engine;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

public interface UniformDef <T extends Uniform> {

    T createUniform(Engine engine);

    SetLayoutBinding createBinding(IntEnum<Descriptor> type, int binding, Flag<ShaderStage> stages);

}
