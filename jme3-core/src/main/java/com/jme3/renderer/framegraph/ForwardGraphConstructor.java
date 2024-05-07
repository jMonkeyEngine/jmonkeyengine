/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.OutputBucketPass;
import com.jme3.renderer.queue.RenderQueue.Bucket;

/**
 *
 * @author codex
 */
public class ForwardGraphConstructor implements GraphConstructor {
    
    private OutputBucketPass opaque, sky, transparent, gui, translucent;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        opaque = frameGraph.add(new OutputBucketPass(Bucket.Opaque));
        sky = frameGraph.add(new OutputBucketPass(Bucket.Sky, DepthRange.REAR));
        transparent = frameGraph.add(new OutputBucketPass(Bucket.Transparent));
        gui = frameGraph.add(new OutputBucketPass(Bucket.Gui, DepthRange.FRONT));
        translucent = frameGraph.add(new OutputBucketPass(Bucket.Translucent));
    }
    @Override
    public void preparePasses(FGRenderContext context) {
        
        opaque.prepareRender(context);
        sky.prepareRender(context);
        transparent.prepareRender(context);
        gui.prepareRender(context);
        translucent.prepareRender(context);
        
    }
    
}
