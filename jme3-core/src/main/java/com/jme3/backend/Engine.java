
package com.jme3.backend;

import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.FrameRenderer;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import com.jme3.vulkan.material.experimental.ShaderSetBuilder;

import java.util.function.Consumer;

public interface Engine {

    /**
     * Creates a {@link FrameRenderer} for performing rendering tasks for the current rendering frame.
     *
     * @param tpf time per frame
     * @return renderer
     */
    RenderSession createRenderSession(float tpf);

    ShadingLayout createShadingLayout()

    ShaderBindingSet createShaderBindings(Consumer<ShaderSetBuilder> builder);

}
