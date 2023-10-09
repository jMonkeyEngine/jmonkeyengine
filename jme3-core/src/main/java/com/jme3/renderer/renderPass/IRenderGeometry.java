package com.jme3.renderer.renderPass;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

/**
 * @author JohnKkk
 */
public interface IRenderGeometry {
    /**
     * Submit the given Geometry to the Pipeline for rendering.
     * @param rm
     * @param geom
     * @return If true is returned, the geometry will be removed from the render Bucket after being rendered.
     */
    public boolean drawGeometry(RenderManager rm, Geometry geom);
}
