package com.jme3.vulkan.material.experimental;

import com.jme3.material.RenderState;
import com.jme3.vulkan.material.shader.ShaderStage;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public abstract class AbstractTechnique implements ShadingTechnique {

    protected final Map<ShaderStage, String> shaderSources = new HashMap<>();
    protected final RenderState state = new RenderState();

}
