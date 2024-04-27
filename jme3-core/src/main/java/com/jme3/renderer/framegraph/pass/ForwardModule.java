/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 *
 * @author codex
 */
public class ForwardModule extends AbstractModule {

    private final RenderQueue.Bucket bucket;
    protected DepthRange depth;

    public ForwardModule(RenderQueue.Bucket bucket) {
        this(bucket, DepthRange.IDENTITY);
    }
    public ForwardModule(RenderQueue.Bucket bucket, DepthRange depth) {
        this.bucket = bucket;
        this.depth = depth;
    }
    
    @Override
    public void initialize(FrameGraph frameGraph) {}
    @Override
    public boolean prepare(RenderContext context) {
        return !context.getRenderQueue().isQueueEmpty(bucket);
    }
    @Override
    public void execute(RenderContext context) {
        if (depth != null) {
            context.setDepthRange(depth);
        }
        context.getRenderQueue().renderQueue(bucket, context.getRenderManager(), context.getViewPort().getCamera(), true);
    }
    @Override
    public void reset() {}
    
    public static ForwardModule opaque() {
        return new ForwardModule(RenderQueue.Bucket.Opaque, DepthRange.IDENTITY);
    }
    public static ForwardModule sky() {
        return new ForwardModule(RenderQueue.Bucket.Sky, new DepthRange(1, 1));
    }
    public static ForwardModule transparent() {
        return new ForwardModule(RenderQueue.Bucket.Transparent, DepthRange.IDENTITY);
    }
    public static ForwardModule translucent() {
        return new ForwardModule(RenderQueue.Bucket.Translucent, DepthRange.IDENTITY);
    }
    
}
