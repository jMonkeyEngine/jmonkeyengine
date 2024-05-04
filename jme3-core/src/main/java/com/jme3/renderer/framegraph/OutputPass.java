/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class OutputPass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor, inDepth;
    private Texture2D fTex;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        fTex = (Texture2D)frameGraph.getAssetManager().loadTexture("Common/Textures/MissingTexture.png");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        context.transferTextures(resources.acquire(inColor, null), resources.acquire(inDepth, null));
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return new FrameBuffer(context.getWidth(), context.getHeight(), 1);
    }
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
    
}
