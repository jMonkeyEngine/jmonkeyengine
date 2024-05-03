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
public class TestConstructor implements GraphConstructor {
    
    private BucketPass opaque, sky;
    private ResultPass result;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        opaque = frameGraph.add(new BucketPass(Bucket.Opaque));
        sky = frameGraph.add(new BucketPass(Bucket.Sky, DepthRange.REAR));
        result = frameGraph.add(new ResultPass());
    }
    @Override
    public void preparePasses(FGRenderContext context) {
        //opaque.setInput(sky);
        opaque.prepareRender(context);
        sky.setInput(opaque);
        sky.prepareRender(context);
        //result.setInColor(opaque.getOutColor());
        //result.setInDepth(opaque.getOutDepth());
        result.setInColor(sky.getOutColor());
        result.setInDepth(sky.getOutDepth());
        result.prepareRender(context);
    }
    
}
