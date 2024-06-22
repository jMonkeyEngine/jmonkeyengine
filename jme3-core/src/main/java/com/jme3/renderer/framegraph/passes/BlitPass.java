/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.material.Material;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class BlitPass extends RenderPass {

    private ResourceTicket<FrameBuffer> source;
    private ResourceTicket<Texture2D> color, depth;
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D();
    private Material nullMat;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        source = addInput("Source");
        color = addOutput("Color");
        depth = addOutput("Depth");
        colorDef.setFormatFlexible(true);
        depthDef.setFormat(Image.Format.Depth);
        depthDef.setFormatFlexible(true);
        nullMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Misc/Null.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(colorDef, color);
        declare(depthDef, depth);
        reserve(color, depth);
        referenceOptional(source);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer sourceBuf = resources.acquireOrElse(source, null);
        FrameBuffer targetBuf;
        if (sourceBuf != null) {
            targetBuf = getFrameBuffer(sourceBuf.getWidth(), sourceBuf.getHeight(), sourceBuf.getSamples());
            colorDef.setSize(sourceBuf.getWidth(), sourceBuf.getHeight());
            depthDef.setSize(sourceBuf.getWidth(), sourceBuf.getHeight());
        } else {
            targetBuf = getFrameBuffer(context, 1);
            colorDef.setSize(context.getWidth(), context.getHeight());
            depthDef.setSize(context.getWidth(), context.getHeight());
        }
        context.getRenderer().copyFrameBuffer(sourceBuf, targetBuf, true, true);
        resources.acquireColorTarget(targetBuf, color);
        resources.acquireDepthTarget(targetBuf, depth);
        context.getRenderer().setFrameBuffer(targetBuf);
        context.renderFullscreen(nullMat);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
