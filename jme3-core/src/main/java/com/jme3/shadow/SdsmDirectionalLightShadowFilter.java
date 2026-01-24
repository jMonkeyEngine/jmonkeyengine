/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

import java.io.IOException;

/**
 * SDSM (Sample Distribution Shadow Mapping) filter for directional lights.
 * <p>
 * This filter uses GPU compute shaders to analyze the depth buffer and compute
 * optimal cascade split positions for rendering shadow maps.
 * <p>
 * Key benefits over {@link DirectionalLightShadowFilter}:
 * <ul>
 *   <li>Better shadow map utilization through sample-based fitting</li>
 *   <li>Dynamic cascade adaptation to scene geometry</li>
 *   <li>Reduced shadow pop-in artifacts</li>
 * </ul>
 * <p>
 * Requires OpenGL 4.3+ for compute shader support. Only works for filter-based shadow mapping.
 */
public class SdsmDirectionalLightShadowFilter extends AbstractShadowFilter<SdsmDirectionalLightShadowRenderer> {
    /**
     * For serialization only. Do not use.
     *
     * @see #SdsmDirectionalLightShadowFilter(AssetManager, int, int)
     */
    public SdsmDirectionalLightShadowFilter() {
        super();
    }

    /**
     * Creates an SDSM directional light shadow filter.
     * @param assetManager the application's asset manager
     * @param shadowMapSize the size of the rendered shadow maps (512, 1024, 2048, etc.)
     * @param splitCount the number of shadow map splits (1-4)
     * @throws IllegalArgumentException if splitCount is not between 1 and 4
     */
    public SdsmDirectionalLightShadowFilter(AssetManager assetManager, int shadowMapSize, int splitCount) {
        super(assetManager, shadowMapSize, new SdsmDirectionalLightShadowRenderer(assetManager, shadowMapSize, splitCount));
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        shadowRenderer.needsfallBackMaterial = true;
        material = new Material(manager, "Common/MatDefs/Shadow/Sdsm/SdsmPostShadow.j3md");
        shadowRenderer.setPostShadowMaterial(material);
        shadowRenderer.initialize(renderManager, vp);
        this.viewPort = vp;
    }

    /**
     * Returns the light used to cast shadows.
     *
     * @return the DirectionalLight
     */
    public DirectionalLight getLight() {
        return shadowRenderer.getLight();
    }

    /**
     * Sets the light to use for casting shadows.
     *
     * @param light a DirectionalLight
     */
    public void setLight(DirectionalLight light) {
        shadowRenderer.setLight(light);
    }

    /**
     * Gets the fit expansion factor.
     *
     * @return the expansion factor
     * @see SdsmDirectionalLightShadowRenderer#getFitExpansionFactor()
     */
    public float getFitExpansionFactor() {
        return shadowRenderer.getFitExpansionFactor();
    }

    /**
     * Sets the expansion factor for fitted shadow frustums.
     *
     * @param factor the expansion factor (default 1.0)
     * @see SdsmDirectionalLightShadowRenderer#setFitExpansionFactor(float)
     */
    public void setFitExpansionFactor(float factor) {
        shadowRenderer.setFitExpansionFactor(factor);
    }

    /**
     * Gets the frame delay tolerance.
     *
     * @return the tolerance value
     * @see SdsmDirectionalLightShadowRenderer#getFitFrameDelayTolerance()
     */
    public float getFitFrameDelayTolerance() {
        return shadowRenderer.getFitFrameDelayTolerance();
    }

    /**
     * Sets the frame delay tolerance.
     *
     * @param tolerance the tolerance (default 0.05)
     * @see SdsmDirectionalLightShadowRenderer#setFitFrameDelayTolerance(float)
     */
    public void setFitFrameDelayTolerance(float tolerance) {
        shadowRenderer.setFitFrameDelayTolerance(tolerance);
    }

    @Override
    public void setDepthTexture(Texture depthTexture) {
        super.setDepthTexture(depthTexture);
        shadowRenderer.setDepthTexture(depthTexture);
    }

    @Override
    protected void postQueue(RenderQueue queue) {
        // We need the depth texture from the previous pass, so we defer
        // shadow processing to postFrame
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort,
                             FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        super.postQueue(null);
        super.postFrame(renderManager, viewPort, prevFilterBuffer, sceneBuffer);
    }

    @Override
    protected void cleanUpFilter(com.jme3.renderer.Renderer r) {
        super.cleanUpFilter(r);
        if (shadowRenderer != null) {
            shadowRenderer.cleanup();
        }
    }

    public void displayAllFrustums(){
        shadowRenderer.displayAllDebugFrustums();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(shadowRenderer, "shadowRenderer", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        shadowRenderer = (SdsmDirectionalLightShadowRenderer) ic.readSavable("shadowRenderer", null);
    }

}