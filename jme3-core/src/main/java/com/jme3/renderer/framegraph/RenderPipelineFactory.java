/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RenderManager.RenderPath;
import com.jme3.renderer.framegraph.pass.BackgroundScreenTestModule;
import com.jme3.renderer.framegraph.pass.DeferredShadingModule;
import com.jme3.renderer.framegraph.pass.ForwardModule;
import com.jme3.renderer.framegraph.pass.GBufferModule;
import com.jme3.renderer.pass.GuiModule;
import com.jme3.renderer.framegraph.pass.PostProcessingModule;
import com.jme3.renderer.framegraph.pass.TileDeferredShadingModule;

/**
 * Constructs basic framegraphs.
 * 
 * @author codex
 */
public class RenderPipelineFactory {
    
    public static FrameGraph create(Application app, RenderPath path) {
        RenderManager rm = app.getRenderManager();
        switch (path) {
            case Forward: return createForwardPipeline(rm);
            case ForwardPlus: return createForwardPlusPipeline(rm);
            case Deferred: return createDeferredPipeline(app.getAssetManager(), rm);
            case TiledDeferred: return createTileDeferredPipeline(app.getAssetManager(), rm);
            default: return null;
        }
    }
    
    private static FrameGraph addBasicPasses(FrameGraph g) {
        g.add(ForwardModule.opaque());
        g.add(ForwardModule.sky());
        g.add(ForwardModule.transparent());
        g.add(new GuiModule());
        g.add(new PostProcessingModule());
        g.add(ForwardModule.translucent());
        return g;
    }
    
    public static FrameGraph createForwardPipeline(RenderManager rm) {
        return addBasicPasses(new FrameGraph(rm));
    }
    
    public static FrameGraph createForwardPlusPipeline(RenderManager rm) {
        throw new UnsupportedOperationException("ForwardPlus render pipeline is currently unsupported.");
    }
    
    public static FrameGraph createDeferredPipeline(AssetManager am, RenderManager rm) {
        FrameGraph g = new FrameGraph(rm);
        g.add(new GBufferModule());
        //g.add(new DeferredShadingModule(am));
        g.add(ForwardModule.sky());
        g.add(ForwardModule.transparent());
        g.add(new GuiModule());
        g.add(new PostProcessingModule());
        g.add(ForwardModule.translucent());
        return g;
    }
    
    public static FrameGraph createTileDeferredPipeline(AssetManager am, RenderManager rm) {
        FrameGraph g = new FrameGraph(rm);
        g.add(new GBufferModule());
        g.add(new TileDeferredShadingModule(am));
        return addBasicPasses(g);
    }
    
    public static FrameGraph createBackroundScreenTest(AssetManager am, RenderManager rm) {
        FrameGraph g = new FrameGraph(rm);
        g.add(new BackgroundScreenTestModule(am));
        g.add(ForwardModule.opaque());
        g.add(ForwardModule.sky());
        g.add(ForwardModule.transparent());
        g.add(new GuiModule());
        g.add(new PostProcessingModule());
        g.add(ForwardModule.translucent());
        return g;
    }
    
}
