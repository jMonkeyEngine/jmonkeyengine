/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer;

import java.util.LinkedList;

/**
 *
 * @author codex
 */
public abstract class AbstractPipelineContext implements PipelineContext {
    
    private final LinkedList<RenderPipeline> usedPipelines = new LinkedList<>();
    
    @Override
    public boolean addPipeline(RenderManager rm, RenderPipeline pipeline) {
        boolean firstCall = false;
        if (!pipeline.hasRenderedThisFrame()) {
            if (firstCall = usedPipelines.isEmpty()) {
                beginRenderFrame(rm);
            }
            pipeline.beginRenderFrame(rm);
            usedPipelines.add(pipeline);
        } else if (usedPipelines.isEmpty()) {
            throw new IllegalStateException(
                    "Pipeline cannot have rendered at this point, but claims it did.");
        }
        return firstCall;
    }
    @Override
    public void flushPipelineStack(RenderManager rm) {
        for (RenderPipeline p : usedPipelines) {
            p.endRenderFrame(rm);
        }
        usedPipelines.clear();
        endRenderFrame(rm);
    }
    
    protected abstract void beginRenderFrame(RenderManager rm);
    
    protected abstract void endRenderFrame(RenderManager rm);
    
}
