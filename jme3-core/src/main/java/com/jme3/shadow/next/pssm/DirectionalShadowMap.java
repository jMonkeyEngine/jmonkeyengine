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
package com.jme3.shadow.next.pssm;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light.Type;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.shadow.next.ShadowMapSlice;
import com.jme3.shadow.next.ShadowMap;

/**
 * @author Kirill Vainer
 */
public class DirectionalShadowMap implements ShadowMap<DirectionalLight> {

    private final DirectionalLight light;
    private final DirectionalShadowMapSlice[] splits;
    private final Vector3f projectionSplitPositions = new Vector3f();

    public DirectionalShadowMap(DirectionalLight light, int textureSize, int numSplits, Vector3f[] points) {
        this.light = light;
        this.splits = new DirectionalShadowMapSlice[numSplits];
        for (int i = 0; i < splits.length; i++) {
            this.splits[i] = new DirectionalShadowMapSlice(textureSize, points);
        }
    }

    public void renderShadowMap(RenderManager renderManager, ViewPort viewPort, DirectionalShadowParameters params, GeometryList shadowCasters) {
        projectionSplitPositions.set(params.getProjectionSplitPositions());
        float[] splitPositionsViewSpace = params.getSplitPositions();
        for (int i = 0; i < splits.length; i++) {
            float near = splitPositionsViewSpace[i];
            float far = splitPositionsViewSpace[i + 1];
            shadowCasters.clear();
            splits[i].updateShadowCamera(viewPort, light, shadowCasters, near, far);
            splits[i].renderShadowMap(renderManager, light, viewPort, shadowCasters);
        }
    }

    public Vector3f getProjectionSplitPositions() {
        return projectionSplitPositions;
    }

    @Override
    public int getNumSlices() {
        return splits.length;
    }

    @Override
    public ShadowMapSlice getSlice(int index) {
        return splits[index];
    }

    @Override
    public Type getLightType() {
        return Type.Directional;
    }

}
