package com.jme3.vulkan.material.experimental;

import com.jme3.material.RenderState;
import com.jme3.vulkan.material.shader.ShaderStage;

import java.util.*;

public class ShadingTechnique {

    // uniform binding, attribute location, and push constant info is extracted from the shaders themselves
    // the techniques do not need to manually specify this information

    private final Class<? extends ShaderInterface> dependency;
    private final Map<ShaderStage, String> shaderSources = new HashMap<>();
    private final RenderState state = new RenderState();

    public ShadingTechnique(Class<? extends ShaderInterface> dependency) {
        this.dependency = dependency;
    }

    /**
     * Specifies the shader interface this technique depends on. Materials that do
     * not support the interface will not be rendered by this technique.
     *
     * @return dependency shader interface type
     */
    public Class<? extends ShaderInterface> getDependency() {
        return dependency;
    }

    /**
     * Specifies the shaders by asset name that will be used to render this technique.
     *
     * @return shader sources
     */
    public Map<ShaderStage, String> getShaderSources() {
        return Collections.unmodifiableMap(shaderSources);
    }

    /**
     * Gets the render state used to render this technique.
     *
     * @return render state
     */
    public RenderState getState() {
        return state;
    }

}
