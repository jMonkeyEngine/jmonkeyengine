/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class OutputBucketPass extends RenderPass {
    
    private Bucket bucket;
    private DepthRange depth;

    public OutputBucketPass(Bucket bucket) {
        this(bucket, DepthRange.IDENTITY);
    }
    public OutputBucketPass(Bucket bucket, DepthRange depth) {
        this.bucket = bucket;
        this.depth = depth;
        if (this.bucket == Bucket.Inherit) {
            throw new IllegalArgumentException("Rendered bucket cannot be Inherit.");
        }
    }
    
    
    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {
        context.popFrameBuffer();
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), true);
        }
        context.getRenderer().setDepthRange(depth);
        context.renderViewPortQueue(bucket, true);
        if (bucket == Bucket.Gui) {
            context.getRenderManager().setCamera(context.getViewPort().getCamera(), false);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    @Override
    public String getProfilerName() {
        return super.getProfilerName()+"["+bucket+"]";
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(depth, "depth", DepthRange.IDENTITY);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        depth = in.readSavable("depth", DepthRange.class, DepthRange.IDENTITY);
    }
    
}
