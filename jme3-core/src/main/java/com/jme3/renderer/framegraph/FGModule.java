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
    
    public void initialize(T frameGraph);
    
    public void prepare(RenderContext context);
    
    public void execute(RenderContext context);
    
    public void reset();
    
}
