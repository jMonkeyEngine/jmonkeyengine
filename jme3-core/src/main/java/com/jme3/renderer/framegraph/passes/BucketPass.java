/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class BucketPass extends RenderPass {
    
    private Bucket bucket;
    private final DepthRange depth = new DepthRange();
    private int samples = 1;
    private boolean flush = true;
    private ResourceTicket<Texture2D> inColor, inDepth, outColor, outDepth;
    private TextureDef<Texture2D> colorDef, depthDef;
    
    public BucketPass() {
        this(Bucket.Opaque, DepthRange.IDENTITY);
    }
    public BucketPass(Bucket bucket) {
        this(bucket, DepthRange.IDENTITY);
    }
    public BucketPass(Bucket bucket, DepthRange depth) {
        this.bucket = bucket;
        this.depth.set(depth);
        if (this.bucket == Bucket.Inherit) {
            throw new IllegalArgumentException("Rendered bucket cannot be Inherit.");
        }
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        inDepth = addInput("Depth");
        outColor = addOutput("Color");
        outDepth = addOutput("Depth");
        colorDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        depthDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img), Image.Format.Depth);
        colorDef.setFormatFlexible(true);
        depthDef.setFormatFlexible(true);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        colorDef.setSize(w, h);
        depthDef.setSize(w, h);
        declare(colorDef, outColor);
        declare(depthDef, outDepth);
        reserve(outColor, outDepth);
        referenceOptional(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTargets(fb, outColor);
        resources.acquireDepthTarget(fb, inDepth);
        context.getRenderer().setFrameBuffer(fb);
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
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(bucket, "bucket", Bucket.Opaque);
        out.write(depth, "depth", DepthRange.IDENTITY);
        out.write(samples, "samples", 1);
        out.write(flush, "flush", true);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        bucket = in.readEnum("bucket", Bucket.class, Bucket.Opaque);
        depth.set(in.readSavable("depth", DepthRange.class, DepthRange.IDENTITY));
        samples = in.readInt("samples", 1);
        flush = in.readBoolean("flush", true);
        if (samples <= 0) {
            throw new IllegalArgumentException("Samples must be greater than zero.");
        }
    }
    
    public void setDepthRange(DepthRange depth) {
        this.depth.set(depth);
    }
    
    public DepthRange getDepthRange() {
        return depth;
    }
    
    @Override
    public String toString() {
        return "BucketPass["+bucket+"]";
    }
    
}
