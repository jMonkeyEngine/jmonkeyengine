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
public class DeferredGraphConstructor implements GraphConstructor {
    
    private GBufferPass gbuf;
    private TileDeferredPass deferred;
    private OutputPass defOut;
    private OutputBucketPass sky, transparent, gui, translucent;
    private PostProcessingPass post;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        gbuf = frameGraph.add(new GBufferPass());
        deferred = frameGraph.add(new TileDeferredPass());
        defOut = frameGraph.add(new OutputPass());
        sky = frameGraph.add(new OutputBucketPass(Bucket.Sky, DepthRange.REAR));
        transparent = frameGraph.add(new OutputBucketPass(Bucket.Transparent));
        gui = frameGraph.add(new OutputBucketPass(Bucket.Gui, DepthRange.FRONT));
        post = frameGraph.add(new PostProcessingPass());
        translucent = frameGraph.add(new OutputBucketPass(Bucket.Translucent));
    }
    @Override
    public void preparePasses(FGRenderContext context) {
    
        gbuf.prepareRender(context);
        
        deferred.setDepth(gbuf.getDepth());
        deferred.setDiffuse(gbuf.getDiffuse());
        deferred.setEmissive(gbuf.getEmissive());
        deferred.setNormal(gbuf.getNormal());
        deferred.setLights(gbuf.getLights());
        deferred.setSpecular(gbuf.getSpecular());
        deferred.prepareRender(context);
        
        defOut.setInColor(deferred.getOutColor());
        defOut.setInDepth(gbuf.getDepth());
        defOut.prepareRender(context);
        
        sky.prepareRender(context);
        transparent.prepareRender(context);
        gui.prepareRender(context);
        post.prepareRender(context);
        translucent.prepareRender(context);
        
    }
    
}
