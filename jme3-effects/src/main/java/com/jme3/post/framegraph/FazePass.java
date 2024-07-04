/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.post.framegraph;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 * Similar to fog, but does not affect sky visibility.
 * 
 * @author codex
 */
public class FazePass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor, inDepth, result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private ColorRGBA color;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        inDepth = addInput("Depth");
        result = addOutput("Result");
        texDef.setMagFilter(Texture.MagFilter.Bilinear);
        texDef.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/Faze.j3md");
        material.setColor("FazeColor", color);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        Texture2D colorTex = resources.acquire(inColor);
        Texture2D depthTex = resources.acquire(inDepth);
        material.setTexture("ColorMap", colorTex);
        material.setTexture("DepthMap", depthTex);
        
        int w = colorTex.getImage().getWidth();
        int h = colorTex.getImage().getHeight();
        texDef.setSize(w, h);
        texDef.setFormat(colorTex.getImage().getFormat());
        
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.resizeCamera(w, h, false, false, false);
        
        context.getScreen().render(context.getRenderManager(), material);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
