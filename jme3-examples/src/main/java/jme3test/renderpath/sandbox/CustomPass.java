/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.renderpath.sandbox;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FGRenderQueuePass;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

/**
 *
 * @author codex
 */
public class CustomPass extends FGRenderQueuePass {

    public CustomPass() {
        super("MyCustomPass");
    }

    @Override
    public void dispatchPassSetup(RenderQueue renderQueue) {
        
    }
    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {}
    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {}
    
}
