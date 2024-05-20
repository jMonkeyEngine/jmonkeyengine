/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.Attribute;
import com.jme3.renderer.framegraph.passes.DeferredPass;
import com.jme3.renderer.framegraph.passes.GBufferPass;
import com.jme3.renderer.framegraph.passes.OutputBucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.PostProcessingPass;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DeferredGraphConstructor implements GraphConstructor {
    
    private GBufferPass gbuf;
    private DeferredPass deferred;
    private OutputPass defOut;
    private Attribute<Texture2D> depthAttr;
    private OutputBucketPass sky, transparent, gui, translucent;
    private PostProcessingPass post;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        
        gbuf = frameGraph.add(new GBufferPass());
        deferred = frameGraph.add(new DeferredPass());
        defOut = frameGraph.add(new OutputPass());
        depthAttr = frameGraph.add(new Attribute<>());
        sky = frameGraph.add(new OutputBucketPass(Bucket.Sky, DepthRange.REAR));
        transparent = frameGraph.add(new OutputBucketPass(Bucket.Transparent));
        gui = frameGraph.add(new OutputBucketPass(Bucket.Gui, DepthRange.FRONT));
        post = frameGraph.add(new PostProcessingPass());
        translucent = frameGraph.add(new OutputBucketPass(Bucket.Translucent));
        
        deferred.makeInput(gbuf, "Diffuse", "Diffuse");
        deferred.makeInput(gbuf, "Specular", "Specular");
        deferred.makeInput(gbuf, "Emissive", "Emissive");
        deferred.makeInput(gbuf, "Normal", "Normal");
        deferred.makeInput(gbuf, "Depth", "Depth");
        deferred.makeInput(gbuf, "Lights", "Lights");
        
        defOut.makeInput(deferred, "Color", "Color");
        defOut.makeInput(gbuf, "Depth", "Depth");
        
        depthAttr.makeInput(gbuf, "Depth", "Value");
        depthAttr.setName("DepthDebug");
        
    }
    @Override
    public void preparePasses(FGRenderContext context) {
    
        gbuf.prepareRender(context);
        deferred.prepareRender(context);
        defOut.prepareRender(context);
        
        sky.prepareRender(context);
        transparent.prepareRender(context);
        gui.prepareRender(context);
        post.prepareRender(context);
        translucent.prepareRender(context);
        
    }
    
}
