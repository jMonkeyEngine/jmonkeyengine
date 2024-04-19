/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.pass.RenderGeometry;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;

/**
 *
 * @author codex
 */
public abstract class RenderQueueModule extends AbstractModule implements RenderGeometry {
    
    protected ViewPort forcedViewPort;
    protected GeometryList drawCommands;
    protected boolean canExecute = false;
    
    public abstract void dispatchPassSetup(RenderQueue queue);
    
    public abstract void executeDrawCommands(RenderContext context);
    
    @Override
    public void execute(RenderContext context) {
        context.getRenderManager().setRenderGeometryHandler(this);
        dispatchPassSetup(context.getRenderQueue());
        if (canExecute) {
            executeDrawCommands(context);
        }
        context.getRenderManager().setRenderGeometryHandler(null);
    }
    
    @Override
    public void reset() {
        if (drawCommands != null && drawCommands.size() > 0) {
            drawCommands.clear();
        }
    }
    
    public void setForcedViewPort(ViewPort vp) {
        this.forcedViewPort = vp;
    }
    
    public ViewPort getForcedViewPort() {
        return forcedViewPort;
    }
    
}
