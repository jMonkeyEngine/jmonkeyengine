/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.light;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.util.TempVars;
import java.util.HashSet;

public final class DefaultLightFilter implements LightFilter {

    private Camera camera;
    private final HashSet<Light> processedLights = new HashSet<Light>();
    private final LightProbeBlendingStrategy probeBlendStrat;

    public DefaultLightFilter() {
        probeBlendStrat = new BasicProbeBlendingStrategy();
    }

    public DefaultLightFilter(LightProbeBlendingStrategy probeBlendStrat) {
        this.probeBlendStrat = probeBlendStrat;
    }
    
    @Override
    public void setCamera(Camera camera) {
        this.camera = camera;
        for (Light light : processedLights) {
            light.frustumCheckNeeded = true;
        }
    }

    @Override
    public void filterLights(Geometry geometry, LightList filteredLightList) {
        TempVars vars = TempVars.get();
        try {
            LightList worldLights = geometry.getWorldLightList();
           
            for (int i = 0; i < worldLights.size(); i++) {
                Light light = worldLights.get(i);

                // If this light is not enabled it will be ignored.
                if (!light.isEnabled()) {
                    continue;
                }

                if (light.frustumCheckNeeded) {
                    processedLights.add(light);
                    light.frustumCheckNeeded = false;
                    light.intersectsFrustum = light.intersectsFrustum(camera, vars);
                }

                if (!light.intersectsFrustum) {
                    continue;
                }

                BoundingVolume bv = geometry.getWorldBound();
                
                if (bv instanceof BoundingBox) {
                    if (!light.intersectsBox((BoundingBox)bv, vars)) {
                        continue;
                    }
                } else if (bv instanceof BoundingSphere) {
                    if (!Float.isInfinite( ((BoundingSphere)bv).getRadius() )) {
                        if (!light.intersectsSphere((BoundingSphere)bv, vars)) {
                            continue;
                        }
                    }
                }
                
                if (light.getType() == Light.Type.Probe) {
                    probeBlendStrat.registerProbe((LightProbe) light);
                } else {
                    filteredLightList.add(light);
                }
                
            }
            
            probeBlendStrat.populateProbes(geometry, filteredLightList);

        } finally {
            vars.release();
        }
    }
    
}
