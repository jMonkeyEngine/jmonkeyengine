/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.queue.RenderQueue.Bucket;

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
    }
    
    
    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {
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
    public boolean isReferenced() {
        return true;
    }
    
}
