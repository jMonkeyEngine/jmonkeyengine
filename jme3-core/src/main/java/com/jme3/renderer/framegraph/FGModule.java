/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public interface FGModule <T extends MyFrameGraph> extends RenderParameterGroup {
    
    /**
     * Initializes the pass to the framegraph.
     * <p>
     * This is called when the pass is first added to the framegraph.
     * 
     * @param frameGraph 
     */
    public void initialize(T frameGraph);
    
    /**
     * Called before the render queue is rendered.
     * 
     * @param context 
     */
    public void preFrame(RenderContext context);
    
    /**
     * Called after the render queue is rendered.
     * 
     * @param context 
     */
    public void postQueue(RenderContext context);
    
    /**
     * Prepares the pass for execution.
     * 
     * @param context 
     */
    public void prepare(RenderContext context);
    
    /**
     * Executes this pass.
     * 
     * @param context 
     */
    public void execute(RenderContext context);
    
    /**
     * Resets the pass after execution.
     */
    public void reset();
    
}
