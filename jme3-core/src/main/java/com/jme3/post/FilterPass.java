/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.passes.RenderPass;

/**
 *
 * @author codex
 */
public class FilterPass extends RenderPass {

    private Filter filter;
    private AssetManager assetManager;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        assetManager = frameGraph.getAssetManager();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        filter.init(assetManager, context.getRenderManager(), context.getViewPort(), context.getWidth(), context.getHeight());
    }
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
