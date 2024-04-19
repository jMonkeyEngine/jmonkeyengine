/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 *
 * @author codex
 */
public class OpaqueModule extends ForwardModule {
    
    public OpaqueModule() {
        super(RenderQueue.Bucket.Opaque);
    }
    
    @Override
    public void executeDrawCommands(RenderContext renderContext) {
        if (canExecute) {
            renderContext.setDepthRange(0, 1);
        }
        super.executeDrawCommands(renderContext);
    }
    
}
