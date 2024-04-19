/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

/**
 *
 * @author codex
 */
public class SkyModule extends ForwardModule {
    
    public SkyModule() {
        super(RenderQueue.Bucket.Sky, new DepthRange(1, 1));
    }
    
    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        rm.renderGeometry(geom);
        return true;
    }
    
}
