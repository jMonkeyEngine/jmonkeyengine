/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer;

/**
 * Renders a ViewPort.
 * 
 * @author codex
 * @param <T>
 */
public interface RenderPipeline <T extends PipelineContext> {
    
    /**
     * Fetches the PipelineContext this pipeline requires for rendering
     * from the RenderManager.
     * 
     * @param rm
     * @return pipeline context (not null)
     */
    public T fetchPipelineContext(RenderManager rm);
    
    /**
     * Returns true if this pipeline has rendered a viewport this render frame.
     * 
     * @return 
     */
    public boolean hasRenderedThisFrame();
    
    /**
     * Called before this pipeline is rendered for the first time this frame.
     * <p>
     * Only called if the pipeline will actually be rendered.
     * 
     * @param rm 
     */
    public void beginRenderFrame(RenderManager rm);
    
    /**
     * Renders the pipeline.
     * 
     * @param rm
     * @param context
     * @param vp
     * @param tpf 
     */
    public void pipelineRender(RenderManager rm, T context, ViewPort vp, float tpf);
    
    /**
     * Called after all rendering is complete in a rendering frame this
     * pipeline participated in.
     * 
     * @param rm 
     */
    public void endRenderFrame(RenderManager rm);
    
}
