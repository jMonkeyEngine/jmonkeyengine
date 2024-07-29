/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer;

/**
 * Handles objects globally for a single type of RenderPipeline.
 * 
 * @author codex
 */
public interface PipelineContext {
    
    public boolean addPipeline(RenderManager rm, RenderPipeline pipeline);
    
    public void flushPipelineStack(RenderManager rm);
    
}
