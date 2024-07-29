/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.AbstractPipelineContext;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.debug.GraphEventCapture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages global pipeline context for rendering with FrameGraphs.
 * 
 * @author codex
 */
public class FGPipelineContext extends AbstractPipelineContext {
    
    private static final Logger LOG = Logger.getLogger(FGPipelineContext.class.getName());
    
    private final RenderObjectMap renderObjects;
    private GraphEventCapture eventCapture;
    
    public FGPipelineContext(RenderManager rm) {
        renderObjects = new RenderObjectMap(this, true);
    }

    @Override
    public void beginRenderFrame(RenderManager rm) {
        if (eventCapture != null) {
            eventCapture.beginRenderFrame();
        }
        renderObjects.newFrame();
    }
    @Override
    public void endRenderFrame(RenderManager rm) {
        if (eventCapture != null) {
            eventCapture.endRenderFrame();
        }
        renderObjects.flushMap();
        if (eventCapture != null && eventCapture.isComplete()) {
            try {
                eventCapture.export();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error exporting captured event data.", ex);
            }
            eventCapture = null;
        }
    }

    public void setEventCapture(GraphEventCapture eventCapture) {
        this.eventCapture = eventCapture;
    }
    
    public RenderObjectMap getRenderObjects() {
        return renderObjects;
    }
    public GraphEventCapture getEventCapture() {
        return eventCapture;
    }
    
}
