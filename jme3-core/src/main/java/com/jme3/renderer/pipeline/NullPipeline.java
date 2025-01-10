/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pipeline;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * Render pipeline that performs no rendering.
 * 
 * @author codex
 */
public class NullPipeline implements RenderPipeline {
    
    private boolean rendered = false;
    
    @Override
    public PipelineContext fetchPipelineContext(RenderManager rm) {
        return rm.getDefaultContext();
    }

    @Override
    public boolean hasRenderedThisFrame() {
        return rendered;
    }

    @Override
    public void startRenderFrame(RenderManager rm) {}

    @Override
    public void pipelineRender(RenderManager rm, PipelineContext context, ViewPort vp, float tpf) {
        rendered = true;
    }

    @Override
    public void endRenderFrame(RenderManager rm) {
        rendered = false;
    }
    
}
