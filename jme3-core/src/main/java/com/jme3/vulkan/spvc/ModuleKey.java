package com.jme3.vulkan.spvc;

import com.jme3.vulkan.material.shader.VulkanShaderModule;

public interface ModuleKey <T extends VulkanShaderModule> {

    T createModule(SpvcCompiler compiler);

}
