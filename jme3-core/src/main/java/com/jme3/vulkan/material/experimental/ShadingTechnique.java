package com.jme3.vulkan.material.experimental;

import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.RenderCommands;

public interface ShadingTechnique {

    /**
     * Called in parallel for each geometry to prepare corresponding
     * resources
     *
     * @param cmd
     * @param g
     */
    void update(RenderCommands cmd, Geometry g);

}
