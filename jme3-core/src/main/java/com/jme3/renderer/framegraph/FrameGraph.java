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
import com.jme3.profile.AppProfiler;
import com.jme3.profile.FgStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph implements Savable {
    
    private final AssetManager assetManager;
    private final ResourceList resources;
    private final FGRenderContext context;
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private GraphConstructor constructor;
    private boolean debug = false;
    private boolean rendered = false;

    public FrameGraph(AssetManager assetManager, RenderManager renderManager) {
        this.assetManager = assetManager;
        this.resources = new ResourceList(renderManager.getRenderObjectMap());
        this.context = new FGRenderContext(this, renderManager);
    }
    
    public void configure(ViewPort vp, AppProfiler prof, float tpf) {
        context.target(vp, prof, tpf);
    }
    public void preFrame() {
        for (RenderPass p : passes) {
            p.preFrame(context);
        }
    }
    public void postQueue() {
        for (RenderPass p : passes) {
            p.postQueue(context);
        }
    }
    public boolean execute() {
        // prepare
        ViewPort vp = context.getViewPort();
        AppProfiler prof = context.getProfiler();
        if (prof != null) prof.vpStep(VpStep.FrameGraphSetup, vp, null);
        if (!rendered) {
            resources.beginRenderingSession();
        }
        for (RenderPass p : passes) {
            if (prof != null) {
                prof.fgStep(FgStep.Prepare, p.getProfilerName());
            }
            p.prepareRender(context);
        }
        // cull resources
        if (prof != null) prof.vpStep(VpStep.FrameGraphCull, vp, null);
        for (RenderPass p : passes) {
            p.countReferences();
        }
        resources.cullUnreferenced();
        // execute
        if (prof != null) prof.vpStep(VpStep.FrameGraphExecute, vp, null);
        context.pushRenderSettings();
        for (RenderPass p : passes) {
            if (p.isUsed()) {
                if (prof != null) {
                    prof.fgStep(FgStep.Execute, p.getProfilerName());
                }
                p.executeRender(context);
                context.popRenderSettings();
            }
        }
        context.popFrameBuffer();
        // reset
        if (prof != null) prof.vpStep(VpStep.FrameGraphReset, vp, null);
        for (RenderPass p : passes) {
            if (prof != null) {
                prof.fgStep(FgStep.Reset, p.getProfilerName());
            }
            p.resetRender(context);
        }
        // cleanup resources
        resources.clear();
        if (rendered) return false;
        else return (rendered = true);
    }
    public void renderingComplete() {
        // notify passes
        for (RenderPass p : passes) {
            p.renderingComplete();
        }
        // reset flags
        rendered = false;
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
            p.shiftExecutionIndex(index, true);
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
        int j = 0;
        RenderPass removed = null;
        for (Iterator<RenderPass> it = passes.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (removed != null) {
                p.disconnectFrom(removed);
                p.shiftExecutionIndex(i, false);
            } else if (j++ == i) {
                removed = p;
                it.remove();
            }
        }
        if (removed != null) {
            removed.cleanupPass(this);
        }
        return removed;
    }
    public boolean remove(RenderPass pass) {
        int i = 0;
        boolean found = false;
        for (Iterator<RenderPass> it = passes.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (found) {
                // shift execution indices down
                p.disconnectFrom(pass);
                p.shiftExecutionIndex(i, false);
                continue;
            }
            if (p == pass) {
                it.remove();
                found = true;
            }
            i++;
        }
        if (found) {
            pass.cleanupPass(this);
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
    
    public AssetManager getAssetManager() {
        return assetManager;
    }
    public ResourceList getResources() {
        return resources;
    }
    public RenderObjectMap getRecycler() {
        return context.getRenderManager().getRenderObjectMap();
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
