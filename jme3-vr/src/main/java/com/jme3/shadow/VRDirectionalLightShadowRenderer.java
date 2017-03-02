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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.shadow.DirectionalLightShadowRenderer;

/**
 * DirectionalLightShadowRenderer renderer use Parrallel Split Shadow Mapping
 * technique (pssm)<br> It splits the view frustum in several parts and compute
 * a shadow map for each one.<br> splits are distributed so that the closer they
 * are from the camera, the smaller they are to maximize the resolution used of
 * the shadow map.<br> This result in a better quality shadow than standard
 * shadow mapping.<br> for more informations on this read this <a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a><br>
 * <p/>
 * @author Rémy Bouquet aka Nehon
 */
public class VRDirectionalLightShadowRenderer extends DirectionalLightShadowRenderer {

    /**
     * Create a OculusDirectionalLightShadowRenderer More info on the technique at <a
     * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     */
    public VRDirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbSplits) {
        super(assetManager, shadowMapSize, nbSplits);
    }
    
    @Override
    public VRDirectionalLightShadowRenderer clone() {
        VRDirectionalLightShadowRenderer clone = new VRDirectionalLightShadowRenderer(assetManager, (int)shadowMapSize, nbShadowMaps);
        clone.setEdgeFilteringMode(getEdgeFilteringMode());
        clone.setEdgesThickness(getEdgesThickness());
        clone.setEnabledStabilization(isEnabledStabilization());
        clone.setLambda(getLambda());
        clone.setLight(getLight());
        clone.setShadowCompareMode(getShadowCompareMode());
        clone.setShadowIntensity(getShadowIntensity());
        clone.setShadowZExtend(getShadowZExtend());
        clone.setShadowZFadeLength(getShadowZFadeLength());
        return clone;
    }
}
