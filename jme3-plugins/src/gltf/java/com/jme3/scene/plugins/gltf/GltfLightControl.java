/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.control.LightControl;
import com.jme3.util.TempVars;

/**
 * This Control maintains a reference to a Camera,
 * which will be synced with the position (worldTranslation)
 * of the current spatial. Rotations are relative to the
 * negative Z direction.
 *
 * @author Trevor Flynn
 */
public class GltfLightControl extends LightControl {

    public GltfLightControl() {

    }

    public GltfLightControl(Light light) {
        this.setLight(light);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial != null && this.getLight() != null) {
            switch (this.getControlDir()) {
                case SpatialToLight:
                    spatialToLight(this.getLight());
                    break;
                case LightToSpatial:
                    lightToSpatial(this.getLight());
                    break;
            }
        }
    }

    private void spatialToLight(Light light) {

        final Vector3f worldTranslation = spatial.getWorldTranslation();

        if (light instanceof PointLight) {
            ((PointLight) light).setPosition(worldTranslation);
        } else if (light instanceof DirectionalLight) {
            ((DirectionalLight) light).setDirection(spatial.getLocalRotation().multLocal(new Vector3f(1f, -1f, 0f)));
        } else if (light instanceof SpotLight) {
            final SpotLight spotLight = (SpotLight) light;
            spotLight.setPosition(worldTranslation);
            spotLight.setDirection(spatial.getLocalRotation().multLocal(new Vector3f(1f, -1f, 0f)));
        }
    }

    private void lightToSpatial(Light light) {
        TempVars vars = TempVars.get();
        if (light instanceof PointLight) {

            PointLight pLight = (PointLight) light;

            Vector3f vecDiff = vars.vect1.set(pLight.getPosition()).subtractLocal(spatial.getWorldTranslation());
            spatial.setLocalTranslation(vecDiff.addLocal(spatial.getLocalTranslation()));
        }

        if (light instanceof DirectionalLight) {
            DirectionalLight dLight = (DirectionalLight) light;
            vars.vect1.set(dLight.getDirection()).multLocal(-1.0f);
            Vector3f vecDiff = vars.vect1.subtractLocal(spatial.getWorldTranslation());
            spatial.setLocalTranslation(vecDiff.addLocal(spatial.getLocalTranslation()));
        }
        vars.release();
        //TODO add code for Spot light here when it's done
    }
}
