/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.renderer.Camera;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 *
 * @author codex
 */
public class GuiModule extends ForwardModule {
    
    public GuiModule() {
        super(RenderQueue.Bucket.Gui, DepthRange.IDENTITY);
    }
    
    @Override
    public void execute(RenderContext context) {
        Camera cam = context.getViewPort().getCamera();
        context.getRenderManager().setCamera(cam, true);
        super.execute(context);
        context.getRenderManager().setCamera(cam, false);
    }
    
}
