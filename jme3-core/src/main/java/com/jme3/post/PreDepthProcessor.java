/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

/**
 * Processor that lays depth first, this can improve performance in complex
 * scenes.
 */
public class PreDepthProcessor implements SceneProcessor {

    private RenderManager rm;
    private ViewPort vp;
    private AssetManager assetManager;
    private Material preDepth;
    private RenderState forcedRS;
    private AppProfiler prof;

    public PreDepthProcessor(AssetManager assetManager){
        this.assetManager = assetManager;
        preDepth = new Material(assetManager, "Common/MatDefs/Shadow/PreShadow.j3md");
        preDepth.getAdditionalRenderState().setPolyOffset(0, 0);
        preDepth.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Back);

        forcedRS = new RenderState();
        forcedRS.setDepthTest(true);
        forcedRS.setDepthWrite(false);
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        this.vp = vp;
    }

    public void reshape(ViewPort vp, int w, int h) {
        this.vp = vp;
    }

    public boolean isInitialized() {
        return vp != null;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
        // lay depth first
        rm.setForcedMaterial(preDepth);
        rq.renderQueue(RenderQueue.Bucket.Opaque, rm, vp.getCamera(), false);
        rm.setForcedMaterial(null);

        rm.setForcedRenderState(forcedRS);
    }

    public void postFrame(FrameBuffer out) {
        rm.setForcedRenderState(null);
    }

    public void cleanup() {
        vp = null;
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

}
