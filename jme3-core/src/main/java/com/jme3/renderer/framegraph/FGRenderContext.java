/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.util.function.Predicate;

/**
 * Contains necessary context for framegraph rendering.
 * <p>
 * Also manages renderer states, to ensure settings do no leak between passes.
 * 
 * @author codex
 */
public class FGRenderContext {
    
    private final FrameGraph frameGraph;
    private final RenderManager renderManager;
    private ViewPort viewPort;
    private AppProfiler profiler;
    private float tpf;
    private final FullScreenQuad screen;
    
    private String forcedTechnique;
    private Material forcedMat;
    private FrameBuffer frameBuffer;
    private GeometryRenderHandler geomRender;
    private Predicate<Geometry> geomFilter;
    private RenderState renderState;

    public FGRenderContext(FrameGraph frameGraph, RenderManager renderManager) {
        this.frameGraph = frameGraph;
        this.renderManager = renderManager;
        this.screen = new FullScreenQuad(this.frameGraph.getAssetManager());
    }
    
    /**
     * Targets this context to the viewport.
     * 
     * @param vp
     * @param profiler
     * @param tpf 
     */
    public void target(ViewPort vp, AppProfiler profiler, float tpf) {
        this.viewPort = vp;
        this.profiler = profiler;
        this.tpf = tpf;
        if (viewPort == null) {
            throw new NullPointerException("ViewPort cannot be null.");
        }
    }
    
    /**
     * Saves the current render settings.
     */
    public void pushRenderSettings() {
        forcedTechnique = renderManager.getForcedTechnique();
        forcedMat = renderManager.getForcedMaterial();
        frameBuffer = renderManager.getRenderer().getCurrentFrameBuffer();
        geomRender = renderManager.getGeometryRenderHandler();
        geomFilter = renderManager.getRenderFilter();
        renderState = renderManager.getForcedRenderState();
    }
    /**
     * Applies saved render settings, except the framebuffer.
     */
    public void popRenderSettings() {
        renderManager.setForcedTechnique(forcedTechnique);
        renderManager.setForcedMaterial(forcedMat);
        renderManager.getRenderer().setFrameBuffer(frameBuffer);
        renderManager.setGeometryRenderHandler(geomRender);
        renderManager.setRenderFilter(geomFilter);
        renderManager.setForcedRenderState(renderState);
        renderManager.getRenderer().setDepthRange(0, 1);
        if (viewPort.isClearColor()) {
            renderManager.getRenderer().setBackgroundColor(viewPort.getBackgroundColor());
        }
    }
    /**
     * Applies the saved framebuffer.
     */
    public void popFrameBuffer() {
        renderManager.getRenderer().setFrameBuffer(frameBuffer);
    }
    
    /**
     * Renders the specified queue bucket.
     * 
     * @param bucket queue bucket to render (not null and not {@link RenderQueue.Bucket#Inherit})
     * @param clear true to flush the bucket
     */
    public void renderViewPortQueue(RenderQueue.Bucket bucket, boolean clear) {
        viewPort.getQueue().renderQueue(bucket, renderManager, viewPort.getCamera(), clear);
    }
    /**
     * Renders the material on a fullscreen quad.
     * 
     * @param mat 
     */
    public void renderFullscreen(Material mat) {
        screen.render(renderManager, mat);
    }
    /**
     * Renders the color and depth textures on a fullscreen quad, where
     * the color texture informs the color, and the depth texture informs
     * the depth.
     * <p>
     * If both color and depth are null, no rendering will be performed
     * 
     * @param color color texture, or null
     * @param depth depth texture, or null
     */
    public void transferTextures(Texture2D color, Texture2D depth) {
        screen.render(renderManager, color, depth);
    }
    
    /**
     * Gets the render manager.
     * 
     * @return 
     */
    public RenderManager getRenderManager() {
        return renderManager;
    }
    /**
     * Gets the viewport currently being rendered.
     * 
     * @return 
     */
    public ViewPort getViewPort() {
        return viewPort;
    }
    /**
     * Gets the profiler.
     * 
     * @return app profiler, or null
     */
    public AppProfiler getProfiler() {
        return profiler;
    }
    /**
     * Gets the renderer held by the render manager.
     * 
     * @return 
     */
    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }
    /**
     * Gets the render queue held by the viewport.
     * 
     * @return 
     */
    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }
    /**
     * Gets the fullscreen quad used for fullscreen renders.
     * 
     * @return 
     */
    public FullScreenQuad getScreen() {
        return screen;
    }
    /**
     * Gets the time per frame.
     * 
     * @return 
     */
    public float getTpf() {
        return tpf;
    }
    /**
     * Gets the camera width.
     * 
     * @return 
     */
    public int getWidth() {
        return viewPort.getCamera().getWidth();
    }
    /**
     * Gets the camera height.
     * 
     * @return 
     */
    public int getHeight() {
        return viewPort.getCamera().getHeight();
    }
    
    /**
     * Returns true if the app profiler is not null.
     * 
     * @return 
     */
    public boolean isProfilerAvailable() {
        return profiler != null;
    }
    
}
