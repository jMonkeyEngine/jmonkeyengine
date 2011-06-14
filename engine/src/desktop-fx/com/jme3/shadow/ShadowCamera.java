/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Creates a camera according to a light
 * Handy to compute projection matrix of a light
 * @author Kirill Vainer
 */
public class ShadowCamera {

    private Vector3f[] points = new Vector3f[8];
    private Light target;

    public ShadowCamera(Light target) {
        this.target = target;
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    /**
     * Updates the camera view direction and position based on the light
     */
    public void updateLightCamera(Camera lightCam) {
        if (target.getType() == Light.Type.Directional) {
            DirectionalLight dl = (DirectionalLight) target;
            lightCam.setParallelProjection(true);
            lightCam.setLocation(Vector3f.ZERO);
            lightCam.lookAtDirection(dl.getDirection(), Vector3f.UNIT_Y);
            lightCam.setFrustum(-1, 1, -1, 1, 1, -1);
        } else {
            PointLight pl = (PointLight) target;
            lightCam.setParallelProjection(false);
            lightCam.setLocation(pl.getPosition());
            // direction will have to be calculated automatically
            lightCam.setFrustumPerspective(45, 1, 1, 300);
        }
    }
}
