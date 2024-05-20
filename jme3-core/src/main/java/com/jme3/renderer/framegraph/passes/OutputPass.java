/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.texture.Texture2D;
import java.util.Objects;

/**
 *
 * @author codex
 */
public class OutputPass extends RenderPass {
    
    private ResourceTicket<Texture2D> color, depth;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        depth = addInput("Depth");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(color, depth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        context.popFrameBuffer();
        Texture2D colorTex = resources.acquireOrElse(color, null);
        Texture2D depthTex = resources.acquireOrElse(depth, null);
        context.transferTextures(colorTex, depthTex);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    
}
