/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.RenderManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph implements Savable {
    
    private static int nextId = 0;
    
    private final int id;
    private final AssetManager assetManager;
    private final ResourceList resources;
    private final FGRenderContext context;
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private GraphConstructor constructor;
    private boolean debug = false;

    public FrameGraph(AssetManager assetManager, RenderManager renderManager) {
        this.id = nextId++;
        this.assetManager = assetManager;
        this.resources = new ResourceList(renderManager.getRenderObjectsMap());
        this.context = new FGRenderContext(this, renderManager);
    }
    
    public void execute() {
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
            if (p.isUsed()) {
                p.executeRender(context);
                context.popRenderSettings();
            }
        }
        context.popFrameBuffer();
        // reset passes
        for (RenderPass p : passes) {
            p.resetRender(context);
        }
        // cleanup resources
        resources.clear();
    }
    
    public <T extends RenderPass> T add(T pass) {
        passes.addLast(pass);
        pass.initializePass(this, passes.size()-1);
        return pass;
    }
    public <T extends RenderPass> T add(T pass, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative.");
        }
        if (index >= passes.size()) {
            return add(pass);
        }
        passes.add(index, pass);
        pass.initializePass(this, index);
        for (RenderPass p : passes) {
            p.shiftExecutionIndex(index, 1);
        }
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
    public <T extends RenderPass> T get(Class<T> type, String name) {
        for (RenderPass p : passes) {
            if (name.equals(p.getName()) && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    public <T extends RenderPass> T get(Class<T> type, int id) {
        for (RenderPass p : passes) {
            if (id == p.getId() && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    public RenderPass remove(int i) {
        RenderPass pass = passes.remove(i);
        pass.cleanupPass(this);
        for (RenderPass p : passes) {
            p.disconnectFrom(pass);
            p.shiftExecutionIndex(i, -1);
        }
        return pass;
    }
    public boolean remove(RenderPass pass) {
        int i = 0;
        for (Iterator<RenderPass> it = passes.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (p == pass) {
                it.remove();
                break;
            }
            i++;
        }
        if (i < passes.size()) {
            pass.cleanupPass(this);
            for (RenderPass p : passes) {
                p.disconnectFrom(pass);
                p.shiftExecutionIndex(i, -1);
            }
            return true;
        }
        return false;
    }
    public void clear() {
        for (RenderPass p : passes) {
            p.cleanupPass(this);
        }
        passes.clear();
    }
    
    public void setConstructor(GraphConstructor constructor) {
        if (this.constructor != null || constructor == null) {
            throw new IllegalStateException();
        }
        this.constructor = constructor;
        this.constructor.addPasses(this);
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public int getId() {
        return id;
    }
    public AssetManager getAssetManager() {
        return assetManager;
    }
    public ResourceList getResources() {
        return resources;
    }
    public RenderObjectMap getRecycler() {
        return context.getRenderManager().getRenderObjectsMap();
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(passes.toArray(RenderPass[]::new), "passes", new RenderPass[0]);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        RenderPass[] array = (RenderPass[])in.readSavableArray("passes", new RenderPass[0]);
        passes.addAll(Arrays.asList(array));
    }
    
}
