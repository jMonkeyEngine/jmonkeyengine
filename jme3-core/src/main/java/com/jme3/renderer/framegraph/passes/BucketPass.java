/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.RenderState;
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
 * Renders a queue bucket to a set of color and depth textures.
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
    private Texture2D depthTex;
    
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
        depthTex = resources.acquireDepthTarget(fb, outDepth);
        System.out.println(fb.getDepthTarget());
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        context.renderTextures(resources.acquireOrElse(inColor, null), resources.acquireOrElse(inDepth, null));
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
    public String getProfilerName() {
        return super.getProfilerName()+"["+bucket+"]";
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(bucket, "bucket", Bucket.Opaque);
        out.write(depth, "depth", DepthRange.IDENTITY);
        out.write(samples, "samples", 1);
        out.write(flush, "flush", true);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        bucket = in.readEnum("bucket", Bucket.class, Bucket.Opaque);
        depth.set(in.readSavable("depth", DepthRange.class, DepthRange.IDENTITY));
        samples = in.readInt("samples", 1);
        flush = in.readBoolean("flush", true);
        if (samples <= 0) {
            throw new IllegalArgumentException("Samples must be greater than zero.");
        }
    }
    
    /**
     * Sets the depth range objects are rendered within.
     * 
     * @param depth depth range (not null, unaffected)
     */
    public void setDepthRange(DepthRange depth) {
        this.depth.set(depth);
    }
    
    /**
     * Gets the depth range objects are rendered within.
     * 
     * @return 
     */
    public DepthRange getDepthRange() {
        return depth;
    }
    
    @Override
    public String toString() {
        return "BucketPass["+bucket+"]";
    }
    
}
