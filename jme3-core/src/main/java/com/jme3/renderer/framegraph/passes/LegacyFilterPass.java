/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.client.GraphSource;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * FrameGraph adapter for traditional filters.
 * 
 * @author codex
 */
public class LegacyFilterPass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor, inDepth, outColor;
    private TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private final HashMap<ViewPort, PassFilter> filters = new HashMap<>();
    private GraphSource<Filter> source;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        inDepth = addInput("Depth");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, outColor);
        reference(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        PassFilter f = filters.get(context.getViewPort());
        if (f == null) {
            f = new PassFilter(source.getGraphValue(frameGraph, context.getViewPort()));
            f.filter.init(frameGraph.getAssetManager(), context.getRenderManager(), context.getViewPort(), 0, 0);
            filters.put(context.getViewPort(), f);
        }
        f.used = true;
        Texture2D colorTex = resources.acquire(inColor);
        Texture2D depthTex = resources.acquire(inDepth);
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, outColor);
        if (f.filter.isEnabled()) {
            f.filter.filterPreFrame(context.getTpf());
            List<Filter.Pass> passes = f.filter.getPostRenderPasses();
            if (passes != null) for (Filter.Pass p : passes) {
                p.beforeRender();
                Material mat = p.getPassMaterial();
                if (p.requiresSceneAsTexture()) {
                    applyTexture(mat, colorTex, "Texture", "NumSamples");
                }
                if (p.requiresDepthAsTexture()) {
                    applyTexture(mat, depthTex, "DepthTexture", "NumSamplesDepth");
                }
                context.getRenderer().setFrameBuffer(p.getRenderFrameBuffer());
                context.getRenderer().clearBuffers(true, true, true);
                context.renderFullscreen(p.getPassMaterial());
            }
            Material mat = f.filter.getPassMaterial();
            if (f.filter.isReqSceneTex()) {
                applyTexture(mat, colorTex, "Texture", "NumSamples");
            }
            if (f.filter.isReqDepthTex()) {
                applyTexture(mat, depthTex, "DepthTexture", "NumSamplesDepth");
            }
            context.getRenderer().setFrameBuffer(fb);
            context.getRenderer().clearBuffers(true, true, true);
            context.renderFullscreen(mat);
            f.filter.filterPostRender(context.getRenderer(), fb);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void renderingComplete() {
        super.renderingComplete();
        for (Iterator<PassFilter> it = filters.values().iterator(); it.hasNext();) {
            PassFilter f = it.next();
            if (!f.used) {
                f.filter.cleanup(frameGraph.getRenderManager().getRenderer());
                it.remove();
            } else {
                f.used = false;
            }
        }
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
    }
    
    private void applyTexture(Material mat, Texture tex, String texParam, String msParam) {
        mat.setTexture(texParam, tex);
        if (tex.getImage().getMultiSamples() > 1) {
            mat.setInt(msParam, tex.getImage().getMultiSamples());
        } else {
            mat.clearParam(msParam);
        }
    }
    
    public Filter getFilter(ViewPort vp) {
        PassFilter f = filters.get(vp);
        if (f != null) {
            return f.filter;
        } else {
            return null;
        }
    }
    
    private static class PassFilter {
        
        public final Filter filter;
        public int width = -1;
        public int height = -1;
        public boolean used = true;
        
        public PassFilter(Filter filter) {
            this.filter = filter;
        }
        
    }
    
}
