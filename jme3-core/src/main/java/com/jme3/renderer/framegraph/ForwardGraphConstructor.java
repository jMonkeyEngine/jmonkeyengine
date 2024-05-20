/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.Attribute;
import com.jme3.renderer.framegraph.passes.BucketPass;
import com.jme3.renderer.framegraph.passes.OutputBucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class ForwardGraphConstructor implements GraphConstructor {
    
    private BucketPass opaque;
    private OutputPass opaqueOut;
    private Attribute<Texture2D> opaqueAttr;
    private OutputBucketPass sky, transparent, gui, translucent;
    
    @Override
    public void addPasses(FrameGraph frameGraph) {
        
        opaque = frameGraph.add(new BucketPass(Bucket.Opaque));
        opaqueOut = frameGraph.add(new OutputPass());
        sky = frameGraph.add(new OutputBucketPass(Bucket.Sky, DepthRange.REAR));
        transparent = frameGraph.add(new OutputBucketPass(Bucket.Transparent));
        gui = frameGraph.add(new OutputBucketPass(Bucket.Gui, DepthRange.FRONT));
        translucent = frameGraph.add(new OutputBucketPass(Bucket.Translucent));
        opaqueAttr = frameGraph.add(new Attribute<>());
        
        opaqueOut.makeInput(opaque, "Color", "Color");
        opaqueOut.makeInput(opaque, "Depth", "Depth");
        
        opaqueAttr.makeInput(opaque, "Color", "Value");
        opaqueAttr.setName("OpaqueColor");
        
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
