/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.definitions.TextureDef2D;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class BucketPass extends RenderPass {
    
    private Bucket bucket;
    private DepthRange depth;
    private int samples = 1;
    private boolean flush = true;
    private ResourceTicket<Texture2D> inColor, inDepth, outColor, outDepth;
    private Texture2D fTex;
    
    public BucketPass(Bucket bucket) {
        this(bucket, DepthRange.IDENTITY);
    }
    public BucketPass(Bucket bucket, DepthRange depth) {
        this.bucket = bucket;
        this.depth = depth;
        if (this.bucket == Bucket.Inherit) {
            throw new IllegalArgumentException("Rendered bucket cannot be Inherit.");
        }
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        fTex = (Texture2D)frameGraph.getAssetManager().loadTexture("Common/Textures/MissingTexture.png");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        outColor = declare(new TextureDef2D(w, h, samples, Image.Format.RGBA8), outColor);
        outDepth = declare(new TextureDef2D(w, h, samples, Image.Format.Depth), outDepth);
        referenceOptional(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        resources.acquireColorTargets(frameBuffer, outColor);
        resources.acquireDepthTarget(frameBuffer, inDepth);
        context.getRenderer().setFrameBuffer(frameBuffer);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        context.transferTextures(resources.acquireOrElse(inColor, null), resources.acquireOrElse(inDepth, null));
        context.getRenderer().setDepthRange(depth);
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), true);
        }
        context.renderViewPortQueue(bucket, flush);
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), false);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    public void setDepthRange(DepthRange depth) {
        this.depth = depth;
    }
    public void setInColor(ResourceTicket<Texture2D> inColor) {
        this.inColor = inColor;
    }
    public void setInDepth(ResourceTicket<Texture2D> inDepth) {
        this.inDepth = inDepth;
    }
    public void setInput(BucketPass pass) {
        inColor = pass.outColor.copyIndexTo(null);
        inDepth = pass.outDepth.copyIndexTo(null);
    }
    
    public DepthRange getDepthRange() {
        return depth;
    }
    public ResourceTicket<Texture2D> getOutColor() {
        return outColor;
    }
    public ResourceTicket<Texture2D> getOutDepth() {
        return outDepth;
    }
    
    @Override
    public String toString() {
        return "BucketPass["+bucket+"]";
    }
    
}
