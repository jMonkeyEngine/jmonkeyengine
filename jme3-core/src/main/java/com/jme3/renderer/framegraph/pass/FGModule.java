/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.SocketGroup;

/**
 *
 * @author codex
 * @param <T>
 */
public interface FGModule <T extends FrameGraph> extends SocketGroup {
    
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
     * <p>
     * This method is called before parameters are pulled.
     * 
     * @param context 
     */
    public void preFrame(RenderContext context);
    
    /**
     * Called after the render buckets are queued.
     * <p>
     * This method is called before parameters are pulled.
     * 
     * @param context 
     */
    public void postQueue(RenderContext context);
    
    /**
     * Prepares the pass for execution and determines if execution should occur.
     * <p>
     * If execution is vetoed on this step, parameter pulling, execution, parameter
     * pushing, and render state reset will not occur.
     * <p>
     * This method is called before parameters are pulled.
     * 
     * @param context 
     * @return true if execution should occur
     */
    public boolean prepare(RenderContext context);
    
    /**
     * Executes this pass.
     * 
     * @param context 
     */
    public void execute(RenderContext context);
    
    /**
     * Resets this pass after all passes have been executed.
     */
    public void reset();
    
}
