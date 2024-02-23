/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
import java.io.IOException;

/**
 *
 * This Filter does basically the same as a DirectionalLightShadowRenderer
 * except it renders the post shadow pass as a fullscreen quad pass instead of a
 * geometry pass. It's mostly faster than PssmShadowRenderer as long as you have
 * more than about ten shadow receiving objects. The expense is the drawback
 * that the shadow Receive mode set on spatial is ignored. So basically all and
 * only objects that render depth in the scene receive shadows.
 *
 * API is basically the same as the PssmShadowRenderer;
 *
 * @author Rémy Bouquet aka Nehon
 */
public class DirectionalLightShadowFilter extends AbstractShadowFilter<DirectionalLightShadowRenderer> {

    /**
     * For serialization only. Do not use.
     * 
     * @see #DirectionalLightShadowFilter(AssetManager assetManager, int shadowMapSize, int nbSplits)
     */
    public DirectionalLightShadowFilter() {
        super();
    }
    
    /**
     * Creates a new instance of {@code DirectionalLightShadowFilter} with the
     * specified parameters.
     * 
     * @param assetManager  the application's asset manager
     * @param shadowMapSize the size of the rendered shadow maps (512, 1024, 2048, etc...)
     * @param nbSplits      the number of shadow maps rendered (more shadow maps = better quality, but slower)
     */
    public DirectionalLightShadowFilter(AssetManager assetManager, int shadowMapSize, int nbSplits) {
        super(assetManager, shadowMapSize, new DirectionalLightShadowRenderer(assetManager, shadowMapSize, nbSplits));
    }

    /**
     * Return the light used to cast shadows.
     *
     * @return the DirectionalLight
     */
    public DirectionalLight getLight() {
        return shadowRenderer.getLight();
    }

    /**
     * Sets the light to use to cast shadows.
     *
     * @param light a DirectionalLight
     */
    public void setLight(DirectionalLight light) {
        shadowRenderer.setLight(light);
    }

    /**
     * Returns the lambda parameter.
     *
     * @see #setLambda(float lambda)
     * @return lambda
     */
    public float getLambda() {
        return shadowRenderer.getLambda();
    }

    /**
     * Adjusts the partition of the shadow extend into shadow maps. Lambda is
     * usually between 0 and 1. A low value gives a more linear partition, resulting
     * in consistent shadow quality over the extend, but near shadows could look
     * very jagged. A high value gives a more logarithmic partition, resulting in
     * high quality for near shadows, but quality decreases rapidly with distance.
     * The default value is 0.65 (the theoretical optimum).
     *
     * @param lambda the lambda value
     */
    public void setLambda(float lambda) {
        shadowRenderer.setLambda(lambda);
    }

    /**
     * Returns true if stabilization is enabled.
     * 
     * @return true if stabilization is enabled
     */
    public boolean isEnabledStabilization() {
        return shadowRenderer.isEnabledStabilization();
    }
    
    /**
     * Enables the stabilization of the shadow's edges. (default is true)
     * This prevents shadow edges from flickering when the camera moves.
     * However, it can lead to some loss of shadow quality in particular scenes.
     *
     * @param stabilize true to stabilize, false to disable stabilization
     */
    public void setEnabledStabilization(boolean stabilize) {
        shadowRenderer.setEnabledStabilization(stabilize);        
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
        shadowRenderer = (DirectionalLightShadowRenderer) ic.readSavable("shadowRenderer", null);
    }
    
}
