/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RenderManager.RenderPath;
import com.jme3.renderer.pass.DeferredShadingModule;
import com.jme3.renderer.pass.ForwardModule;
import com.jme3.renderer.pass.GBufferModule;
import com.jme3.renderer.pass.GuiModule;
import com.jme3.renderer.pass.OpaqueModule;
import com.jme3.renderer.pass.PostProcessingModule;
import com.jme3.renderer.pass.SkyModule;
import com.jme3.renderer.pass.TileDeferredShadingModule;

/**
 * Constructs basic framegraphs.
 * 
 * @author codex
 */
public class RenderPipelineFactory {
    
    public static MyFrameGraph create(Application app, RenderPath path) {
        RenderManager rm = app.getRenderManager();
        switch (path) {
            case Forward: return createForwardPipeline(rm);
            case ForwardPlus: return createForwardPlusPipeline(rm);
            case Deferred: return createDeferredPipeline(app.getAssetManager(), rm);
            case TiledDeferred: return createTileDeferredPipeline(app.getAssetManager(), rm);
            default: return null;
        }
    }
    
    private static MyFrameGraph addBasicPasses(MyFrameGraph g) {
        g.add(ForwardModule.opaque());
        g.add(ForwardModule.sky());
        g.add(ForwardModule.transparent());
        g.add(new GuiModule());
        g.add(new PostProcessingModule());
        g.add(ForwardModule.translucent());
        return g;
    }
    
    public static MyFrameGraph createForwardPipeline(RenderManager rm) {
        return addBasicPasses(new MyFrameGraph(rm));
    }
    
    public static MyFrameGraph createForwardPlusPipeline(RenderManager rm) {
        throw new UnsupportedOperationException("ForwardPlus render pipeline is currently unsupported.");
    }
    
    public static MyFrameGraph createDeferredPipeline(AssetManager am, RenderManager rm) {
        MyFrameGraph g = new MyFrameGraph(rm);
        g.add(new GBufferModule());
        g.add(new DeferredShadingModule(am));
        //g.add(ForwardModule.opaque());
        g.add(ForwardModule.sky());
        g.add(ForwardModule.transparent());
        g.add(new GuiModule());
        //g.add(new PostProcessingModule());
        //g.add(ForwardModule.translucent());
        return g;
    }
    
    public static MyFrameGraph createTileDeferredPipeline(AssetManager am, RenderManager rm) {
        MyFrameGraph g = new MyFrameGraph(rm);
        g.add(new GBufferModule());
        g.add(new TileDeferredShadingModule(am));
        return addBasicPasses(g);
    }
    
}
