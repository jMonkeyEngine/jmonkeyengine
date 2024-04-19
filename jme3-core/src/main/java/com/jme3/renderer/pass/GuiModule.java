/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.renderer.Camera;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 *
 * @author codex
 */
public class GuiModule extends ForwardModule {
    
    public GuiModule() {
        super(RenderQueue.Bucket.Gui);
    }
    
    @Override
    public void executeDrawCommands(RenderContext context) {
        Camera cam;
        if (forcedViewPort != null) {
            cam = forcedViewPort.getCamera();
        } else {
            cam = context.getViewPort().getCamera();
        }
        if (canExecute) {
            context.setDepthRange(0, 0);
            context.getRenderManager().setCamera(cam, true);
        }
        super.executeDrawCommands(context);
        if (canExecute) {
            context.getRenderManager().setCamera(cam, false);
        }
    }
    
}
