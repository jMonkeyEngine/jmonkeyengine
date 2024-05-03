/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph {
    
    private final AssetManager assetManager;
    private final ResourceList resources;
    private final FGRenderContext context;
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private GraphConstructor constructor;
    private boolean debug = false;

    public FrameGraph(AssetManager assetManager, RenderManager renderManager) {
        this.assetManager = assetManager;
        this.resources = new ResourceList(renderManager.getResourceRecycler());
        this.context = new FGRenderContext(this.assetManager, renderManager);
    }
    
    public void execute() {
        System.out.println("render viewport: "+context.getViewPort().getName());
        // prepare passes
        if (constructor != null) {
            constructor.preparePasses(context);
        } else for (RenderPass p : passes) {
            p.prepareRender(context);
        }
        for (RenderPass p : passes) {
            p.countReferences();
        }
        // cull resources
        resources.cullUnreferenced();
        // execute passes
        context.pushRenderSettings();
        for (RenderPass p : passes) {
            if (p.isReferenced()) {
                System.out.println("  execute pass: "+p);
                p.executeRender(context);
            }
        }
        // reset passes
        for (RenderPass p : passes) {
            p.resetRender(context);
        }
        // cleanup resources
        resources.clear();
    }
    
    public void setConstructor(GraphConstructor constructor) {
        if (this.constructor != null || constructor == null) {
            throw new IllegalStateException();
        }
        this.constructor = constructor;
        this.constructor.addPasses(this);
    }
    public <T extends RenderPass> T add(T pass) {
        passes.addLast(pass);
        pass.initializePass(this);
        return pass;
    }
    public <T extends RenderPass> T get(Class<T> type) {
        for (RenderPass p : passes) {
            if (type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public AssetManager getAssetManager() {
        return assetManager;
    }
    public ResourceList getResources() {
        return resources;
    }
    public ResourceRecycler getRecycler() {
        return context.getRenderManager().getResourceRecycler();
    }
    public FGRenderContext getContext() {
        return context;
    }
    public RenderManager getRenderManager() {
        return context.getRenderManager();
    }
    public boolean isDebug() {
        return debug;
    }
    
}
