/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.passes.DeferredPass;
import com.jme3.renderer.framegraph.passes.GBufferPass;
import com.jme3.renderer.framegraph.passes.OutputBucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.PostProcessingPass;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.renderer.framegraph.passes.TileDeferredPass;
import com.jme3.renderer.queue.RenderQueue;

/**
 * Utility class for constructing common framegraphs.
 * 
 * @author codex
 */
public class FrameGraphFactory {
    
    /**
     * Constructs a standard forward framegraph, with no controllable features.
     * 
     * @param assetManager
     * @param renderManager
     * @return forward framegraph
     */
    public static FrameGraph forward(AssetManager assetManager, RenderManager renderManager) {
        FrameGraph fg = new FrameGraph(assetManager, renderManager);
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Opaque));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Sky, DepthRange.REAR));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Transparent));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Gui, DepthRange.FRONT));
        fg.add(new PostProcessingPass());
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Translucent));
        return fg;
    }
    
    /**
     * Constructs a deferred or tiled deferred framegraph.
     * 
     * @param assetManager
     * @param renderManager
     * @param tiled true to construct advanced tiled deferred
     * @return deferred framegraph
     */
    public static FrameGraph deferred(AssetManager assetManager, RenderManager renderManager, boolean tiled) {
        FrameGraph fg = new FrameGraph(assetManager, renderManager);
        GBufferPass gbuf = fg.add(new GBufferPass());
        RenderPass deferred;
        if (!tiled) {
            deferred = fg.add(new DeferredPass());
        } else {
            deferred = fg.add(new TileDeferredPass());
        }
        OutputPass defOut = fg.add(new OutputPass());
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Sky, DepthRange.REAR));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Transparent));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Gui, DepthRange.FRONT));
        fg.add(new PostProcessingPass());
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Translucent));
        
        deferred.makeInput(gbuf, "Diffuse", "Diffuse");
        deferred.makeInput(gbuf, "Specular", "Specular");
        deferred.makeInput(gbuf, "Emissive", "Emissive");
        deferred.makeInput(gbuf, "Normal", "Normal");
        deferred.makeInput(gbuf, "Depth", "Depth");
        deferred.makeInput(gbuf, "Lights", "Lights");
        
        defOut.makeInput(deferred, "Color", "Color");
        defOut.makeInput(gbuf, "Depth", "Depth");
        
        return fg;
        
    }
    
}
