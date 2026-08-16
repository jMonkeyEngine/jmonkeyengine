package com.jme3.vulkan.material

import com.jme3.backend.Engine
import com.jme3.backend.SimpleVulkanEngine.LightData


import com.jme3.vulkan.buffer.EngineBuffer
import com.jme3.vulkan.buffers.stream.StreamingBuffer
import com.jme3.vulkan.descriptors.DescriptorType
import com.jme3.vulkan.descriptors.UniformBinding
import com.jme3.vulkan.devices.LogicalDevice
import com.jme3.vulkan.images.newimage.EngineImage
import com.jme3.vulkan.material.shader.ShaderStage
import com.jme3.vulkan.material.technique.VulkanTechnique
import com.jme3.vulkan.material.uniforms.TextureUniform
import com.jme3.vulkan.material.uniforms.BufferUniform
import com.jme3.vulkan.memory.MemorySize

Engine engine = null
LogicalDevice device = null

mat = engine.createMaterial()
mat.setUniform "PBR", new BufferUniform<>(StructLayout.std140, new LightData(), (MemorySize size) -> {
    return new StreamingBuffer(device, size, EngineBuffer.Role.Uniform)
})
mat.setUniform "ColorMap", new TextureUniform(EngineImage.Layout.ShaderReadOnlyOptimal)

technique = new VulkanTechnique()
technique.setShaderSource ShaderStage.Vertex, "Test/VulkanTest.vert"
technique.setShaderSource ShaderStage.Fragment, "Test/VulkanTest.frag"
technique.setBinding 0, "PBR", new UniformBinding(DescriptorType.UniformBuffer, 0, ShaderStage.Fragment)
technique.setBinding 0, "ColorMap", new UniformBinding(DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment)
mat.setTechnique "main", technique
