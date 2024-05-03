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
public class ForwardGraphConstructor implements GraphConstructor {

    private BucketPass opaque, sky, transparent, gui, translucent;
    private ResultPass result;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        opaque = frameGraph.add(new BucketPass(Bucket.Opaque));
        sky = frameGraph.add(new BucketPass(Bucket.Sky, DepthRange.REAR));
        transparent = frameGraph.add(new BucketPass(Bucket.Transparent));
        gui = frameGraph.add(new BucketPass(Bucket.Gui, DepthRange.FRONT));
        translucent = frameGraph.add(new BucketPass(Bucket.Translucent));
        result = frameGraph.add(new ResultPass());
    }
    @Override
    public void preparePasses(FGRenderContext context) {
        
        System.out.println("  prepare opaque");
        opaque.prepareRender(context);
        
        System.out.println("  prepare sky");
        sky.setInput(opaque);
        sky.prepareRender(context);
        
        System.out.println("  prepare transparent");
        transparent.setInput(sky);
        transparent.prepareRender(context);
        
        System.out.println("  prepare gui");
        gui.setInput(transparent);
        gui.prepareRender(context);
        
        System.out.println("  prepare translucent");
        translucent.setInput(gui);
        translucent.prepareRender(context);
        
        System.out.println("  prepare result");
        BucketPass out = sky;
        result.setInColor(out.getOutColor());
        result.setInDepth(out.getOutDepth());
        result.prepareRender(context);
    }
    
}
