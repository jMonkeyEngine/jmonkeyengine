/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.shadow.next;

import com.jme3.shadow.next.pssm.DirectionalShadowParameters;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shadow.next.pssm.DirectionalShadowMap;
import com.jme3.texture.FrameBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The 4th generation of shadow mapping in jME3.
 * <p>
 * This version is primarily focused on rendering in-pass shadows, so pre-pass
 * and subsequent stages are separated.
 *
 * @author Kirill Vainer
 */
public class PreShadowRenderer implements SceneProcessor {

    private static final String PRE_SHADOW_TECHNIQUE_NAME = "PreShadow";

    private RenderManager renderManager;
    private ViewPort viewPort;
    private final Vector3f[] points = new Vector3f[8];
    private final GeometryList shadowCasters = new GeometryList(new OpaqueComparator());
    private final List<ShadowMap> shadowMaps = new ArrayList<>();
    private final RenderState prePassRenderState = RenderState.ADDITIONAL.clone();

    private int textureSize = 1024;
    
    // parameters for directional lights
    private final DirectionalShadowParameters directionalParams = new DirectionalShadowParameters();

    public PreShadowRenderer() {
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

        prePassRenderState.setFaceCullMode(RenderState.FaceCullMode.Off);
        prePassRenderState.setColorWrite(false);
        prePassRenderState.setDepthWrite(true);
        prePassRenderState.setDepthTest(true);
        prePassRenderState.setPolyOffset(1.2f, 0);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
        this.viewPort = vp;
    }

    public DirectionalShadowParameters directional() {
        return directionalParams;
    }

    public void setPolyOffset(float factor, float units) {
        // TODO: might want to set this separately per model
        prePassRenderState.setPolyOffset(factor, units);
    }

    public int getTextureSize() {
        return textureSize;
    }

    public void setTextureSize(int textureSize) {
        // TODO: support changing texture size after shadow maps are created
        this.textureSize = textureSize;
    }
    
    public void addLight(Light light) {
        ShadowMap shadowMap;
        switch (light.getType()) {
            case Directional:
                shadowMap = new DirectionalShadowMap(
                        (DirectionalLight) light,
                        textureSize,
                        directionalParams.getNumSplits(),
                        points);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        
        light.setShadowMap(shadowMap);
        shadowMaps.add(shadowMap);
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
    }

    @Override
    public boolean isInitialized() {
        return this.viewPort != null;
    }

    @Override
    public void preFrame(float tpf) {
    }

    private void renderShadowMaps() {
        renderManager.setForcedRenderState(prePassRenderState);
        renderManager.setForcedTechnique(PRE_SHADOW_TECHNIQUE_NAME);

        for (ShadowMap shadowMap : shadowMaps) {
            switch (shadowMap.getLightType()) {
                case Directional:
                    DirectionalShadowMap directionalShadow = (DirectionalShadowMap) shadowMap;
                    directionalShadow.renderShadowMap(renderManager, viewPort, directionalParams, shadowCasters);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        Renderer renderer = renderManager.getRenderer();
        renderer.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.setForcedRenderState(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewPort.getCamera(), false);
    }

    @Override
    public void postQueue(RenderQueue rq) {
        directionalParams.updateSplitPositions(viewPort.getCamera());
        renderShadowMaps();
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

}
