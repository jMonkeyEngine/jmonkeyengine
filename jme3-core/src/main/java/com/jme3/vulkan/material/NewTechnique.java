package com.jme3.vulkan.material;

import com.jme3.material.RenderState;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages rendering parameters and shaders as well as mapping
 * uniform names to descriptor bindings.
 */
public class NewTechnique {

    private final PipelineLayout layout;
    private final Map<ShaderStage, ShaderModule> shaders = new HashMap<>();
    private final Map<String, String> uniformDefines = new HashMap<>();
    private final Map<String, String> defines = new HashMap<>();
    private final RenderState state;

    public PipelineLayout generateLayout(NewMaterial material) {

    }

}
