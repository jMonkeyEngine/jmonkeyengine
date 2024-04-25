/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.RenderParameterGroup;

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
     * Called before the render buckets are queued.
     * 
     * @param context 
     */
    public void preFrame(RenderContext context);
    
    /**
     * Called after the render buckets are queued.
     * 
     * @param context 
     */
    public void postQueue(RenderContext context);
    
    /**
     * Prepares the pass for execution and determines if execution should occur.
     * <p>
     * Execution should be vetoed on this step if possible, because the framegraph
     * does not need to pull or push parameters from the parameter space for this pass
     * if execution is vetoed on this step.
     * 
     * @param context 
     * @return true if execution should occur
     */
    public boolean prepare(RenderContext context);
    
    /**
     * Returns true if this pass is ready for execution.
     * <p>
     * If this pass is not ready for execution, the framegraph will not execute
     * this pass and will not push parameters from this pass to bound parameters.
     * 
     * @param context
     * @return 
     */
    public boolean readyForExecution(RenderContext context);
    
    /**
     * Executes this pass.
     * 
     * @param context 
     */
    public void execute(RenderContext context);
    
    /**
     * Resets this pass after execution.
     */
    public void reset();
    
}
