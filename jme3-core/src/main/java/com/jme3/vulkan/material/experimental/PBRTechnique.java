package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.render.bucket.GeometryBucket;

import java.util.Collection;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBRTechnique implements ShadingTechnique {

    @Override
    public void render(RenderSession session, ViewPort vp, GeometryBucket bucket) {

        Collection<Geometry> selected = bucket.selectGeometries(g -> g.getMaterial().containsInterface(PBR.class));
        if (selected.isEmpty()) {
            return;
        }



    }

}
