package com.jme3.vulkan.material

import com.jme3.backend.Engine
import com.jme3.backend.SimpleVulkanEngine.LightData
import com.jme3.util.struct.StructLayout
import com.jme3.vulkan.buffers.BufferUsage
import com.jme3.vulkan.buffers.stream.StreamingBuffer
import com.jme3.vulkan.descriptors.Descriptor
import com.jme3.vulkan.descriptors.SetLayoutBinding
import com.jme3.vulkan.devices.LogicalDevice
import com.jme3.vulkan.images.VulkanImage
import com.jme3.vulkan.material.shader.ShaderStage
import com.jme3.vulkan.material.technique.VulkanTechnique
import com.jme3.vulkan.material.uniforms.TextureUniform
import com.jme3.vulkan.material.uniforms.StructUniform
import com.jme3.vulkan.memory.MemorySize

Engine engine = null
LogicalDevice device = null

mat = engine.createMaterial()
mat.setUniform "PBR", new StructUniform<>(StructLayout.std140, new LightData(), (MemorySize size) -> {
    return new StreamingBuffer(device, size, BufferUsage.Uniform)
})
mat.setUniform "ColorMap", new TextureUniform(VulkanImage.Layout.ShaderReadOnlyOptimal)

technique = new VulkanTechnique()
technique.setShaderSource ShaderStage.Vertex, "Test/VulkanTest.vert"
technique.setShaderSource ShaderStage.Fragment, "Test/VulkanTest.frag"
technique.setBinding 0, "PBR", new SetLayoutBinding(Descriptor.UniformBuffer, 0, ShaderStage.Fragment)
technique.setBinding 0, "ColorMap", new SetLayoutBinding(Descriptor.CombinedImageSampler, 1, ShaderStage.Fragment)
mat.setTechnique "main", technique
