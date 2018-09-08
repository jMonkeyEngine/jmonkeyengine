/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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
package com.jme3.shadow.next.array;

import com.jme3.light.Light;
import com.jme3.light.Light.Type;
import com.jme3.light.PointLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.texture.TextureArray;

/**
 * @author Kirill Vainer
 */
public class PointArrayShadowMap extends BaseArrayShadowMap<PointArrayShadowMapSlice> {

    private final PointLight light;
    
    private static final Quaternion[] ROTATIONS = new Quaternion[6];
    
    static {
        for (int i = 0; i < ROTATIONS.length; i++) {
            ROTATIONS[i] = new Quaternion();
        }

        // left
        ROTATIONS[0].fromAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, Vector3f.UNIT_X.mult(-1f));

        // right
        ROTATIONS[1].fromAxes(Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_X);

        // bottom
        ROTATIONS[2].fromAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));

        // top
        ROTATIONS[3].fromAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z, Vector3f.UNIT_Y);

        // forward
        ROTATIONS[4].fromAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_Z.mult(-1f));

        // backward
        ROTATIONS[5].fromAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);
    }
    
    public PointArrayShadowMap(PointLight light, TextureArray array, int firstArraySlice, int textureSize) {
        super(array, firstArraySlice);
        this.light = light;
        this.slices = new PointArrayShadowMapSlice[6];
        for (int i = 0; i < slices.length; i++) {
            this.slices[i] = new PointArrayShadowMapSlice(array, firstArraySlice + i, textureSize, ROTATIONS[i]);
        }
    }

    public void renderShadowMap(RenderManager renderManager, ViewPort viewPort, GeometryList shadowCasters) {
        for (int i = 0; i < slices.length; i++) {
            shadowCasters.clear();
            slices[i].updateShadowCamera(viewPort, light, shadowCasters);
            slices[i].renderShadowMap(renderManager, light, viewPort, shadowCasters);
        }
    }
    
    @Override
    public Light.Type getLightType() {
        return Type.Point;
    }
    
}
