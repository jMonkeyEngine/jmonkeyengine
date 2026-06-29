
package com.jme3.backend;

import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.slang.ComponentType;
import com.jme3.vulkan.slang.Session;
import com.jme3.vulkan.util.Flag;

import java.util.Map;

public interface Engine {

    RenderSession createRenderSession(float tpf);

    MappableBuffer createBuffer(long bytes, Flag<BufferUsage> usage, UpdateHint update);

    ShaderBindingSet createShaderSet(Map<Integer, UniformBinding> bindings);

    UniformBinding createUniformBufferBinding(Flag<ShaderStage> scope);

    UniformBinding createStorageBufferBinding(Flag<ShaderStage> scope);

    UniformBinding createTextureBinding(Flag<ShaderStage> scope);

    ShaderModule createShader(ComponentType component);

    Session getSlangSession();

}
