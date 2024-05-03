/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.material.RenderState;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class ResultPass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor, inDepth;
    private Texture2D forcedTex;
    
    @Override
    public void initialize(FrameGraph frameGraph) {
        forcedTex = (Texture2D)frameGraph.getAssetManager().loadTexture("Common/Textures/MissingTexture.png");
    }
    @Override
    public void prepare(FGRenderContext context) {
        referenceOptional(inColor, inDepth);
    }
    @Override
    public void execute(FGRenderContext context) {
        Texture2D color = resources.acquire(inColor, null);
        Texture2D depth = resources.acquire(inDepth, null);
        context.transferTextures(color, depth, RenderState.BlendMode.Alpha);
    }
    @Override
    public void reset(FGRenderContext context) {}
    @Override
    public void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isReferenced() {
        return true;
    }

    public void setInColor(ResourceTicket<Texture2D> inColor) {
        this.inColor = inColor;
    }
    public void setInDepth(ResourceTicket<Texture2D> inDepth) {
        this.inDepth = inDepth;
    }
    
    @Override
    public String toString() {
        return "ResultPass[]";
    }
    
}
