/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class BucketPass extends RenderPass implements GeometryRenderHandler {
    
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
        outColor = register(new TextureDef2D(w, h, samples, Image.Format.RGBA8), outColor);
        outDepth = register(new TextureDef2D(w, h, samples, Image.Format.Depth), outDepth);
        referenceOptional(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        frameBuffer.addColorTarget(context.createTextureTarget(resources.acquire(outColor)));
        frameBuffer.setDepthTarget(context.createTextureTarget(resources.acquire(outDepth)));
        context.setFrameBuffer(frameBuffer, true, true, true);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        //context.getRenderManager().setGeometryRenderHandler(this);
        context.transferTextures(resources.acquire(inColor, null), resources.acquire(inDepth, null));
        context.getRenderer().setDepthRange(depth);
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), true);
        }
        //context.getRenderer().setDepthRange(0, 0);
        context.renderViewPortQueue(bucket, flush);
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), false);
        }
        //context.transferTextures(fTex, null, false);
        frameBuffer.clearColorTargets();
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return new FrameBuffer(context.getWidth(), context.getHeight(), samples);
        //return null;
    }
    @Override
    public boolean isReferenced() {
        return super.isReferenced();
        //return true;
    }
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry geom) {
        rm.renderGeometry(geom);
        System.out.println("RENDER GEOMETRY: "+geom);
        return true;
    }
    
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
