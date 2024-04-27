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

import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

/**
 * In order to be compatible with existing logic, FGRenderContext is currently just a local proxy, and may gradually replace the existing state machine manager in the future.
 * @author JohnKkk
 */
public class RenderContext {

    private final RenderManager renderManager;
    private ViewPort viewPort;
    private AppProfiler profiler;
    private final DepthRange depth = new DepthRange();
    private float tpf;
    private int width, height;
    private boolean sizeChanged = false;

    public RenderContext(RenderManager renderManager, ViewPort viewPort) {
        this.renderManager = renderManager;
        this.viewPort = viewPort;
        width = height = -1;
    }
    public RenderContext(RenderManager renderManager) {
        this(renderManager, null);
    }
    
    public void update(ViewPort vp, AppProfiler profiler, float tpf) {
        this.viewPort = vp;
        this.profiler = profiler;
        this.tpf = tpf;
        if (viewPort == null) {
            throw new NullPointerException("ViewPort cannot be null.");
        }
        Camera cam = viewPort.getCamera();
        sizeChanged = width != cam.getWidth() || height != cam.getHeight();
        //System.out.println("size changed? "+sizeChanged);
        width = cam.getWidth();
        height = cam.getHeight();
    }
    
    public void setDepthRange(float start, float end) {
        if (!depth.equals(start, end)) {
            depth.set(start, end);
            renderManager.getRenderer().setDepthRange(depth);
        }
    }
    
    public void setDepthRange(DepthRange depth) {
        if (!this.depth.equals(depth)) {
            renderManager.getRenderer().setDepthRange(this.depth.set(depth));
        }
    }
    
    public DepthRange getDepthRange() {
        return depth;
    }
    
    public RenderManager getRenderManager() {
        return renderManager;
    }
    
    public ViewPort getViewPort() {
        return viewPort;
    }
    
    public AppProfiler getProfiler() {
        return profiler;
    }
    
    public float getTpf() {
        return tpf;
    }
    
    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }
    
    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }
    
    public boolean isProfilerAvailable() {
        return profiler != null;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean isSizeChanged() {
        return sizeChanged;
    }
    
}
