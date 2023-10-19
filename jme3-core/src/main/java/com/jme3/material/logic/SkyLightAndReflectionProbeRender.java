/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.material.logic;

import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.TextureUnitException;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.TextureCubeMap;

import java.util.List;

/**
 * Rendering logic for handling SkyLight and ReflectionProbe.<br/>
 * todo:The functionality of this tool class is not yet complete, because LightProbe has not yet been split into SkyLight and ReflectionProbe. So the code here is just to be compatible with the extraction of LightProbe and lay the groundwork for subsequent work.
 * @author JohnKkk
 */
public class SkyLightAndReflectionProbeRender {

    /**
     * Update lightProbe data to current shader (currently data comes from lightProbe, later this method will be used for skyLight and reflectionProbe)
     * @param rm
     * @param lastTexUnit
     * @param lightProbeData
     * @param shCoeffs
     * @param lightProbePemMap
     * @param lightProbe
     * @return
     */
    public static int setSkyLightAndReflectionProbeData(RenderManager rm, int lastTexUnit, Uniform lightProbeData, Uniform shCoeffs, Uniform lightProbePemMap, LightProbe lightProbe) {

        lightProbeData.setValue(VarType.Matrix4, lightProbe.getUniformMatrix());
        //setVector4InArray(lightProbe.getPosition().x, lightProbe.getPosition().y, lightProbe.getPosition().z, 1f / area.getRadius() + lightProbe.getNbMipMaps(), 0);
        shCoeffs.setValue(VarType.Vector3Array, lightProbe.getShCoeffs());
        /*
         * Assign the prefiltered env map to the next available texture unit.
         */
        int pemUnit = lastTexUnit++;
        Renderer renderer = rm.getRenderer();
        TextureCubeMap pemTexture = lightProbe.getPrefilteredEnvMap();
        try {
            renderer.setTexture(pemUnit, pemTexture);
        } catch (TextureUnitException exception) {
            String message = "Can't assign texture unit for SkyLightAndReflectionProbe."
                    + " lastTexUnit=" + lastTexUnit;
            throw new IllegalArgumentException(message);
        }
        lightProbePemMap.setValue(VarType.Int, pemUnit);
        return lastTexUnit;
    }

    /**
     * Extract which lightProbes should affect the currently rendered object. Currently, this method is only used in deferredPath and only works for the first three collected lightProbes, so it is problematic, but I put it here to prepare for future functionality (and compatibilty with current lightProbes).
     * @param lightList
     * @param ambientLightColor
     * @param skyLightAndReflectionProbes
     * @param removeLights
     */
    public static void extractSkyLightAndReflectionProbes(LightList lightList, ColorRGBA ambientLightColor, List<LightProbe> skyLightAndReflectionProbes, boolean removeLights) {
        ambientLightColor.set(0, 0, 0, 1);
        skyLightAndReflectionProbes.clear();
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof AmbientLight) {
                ambientLightColor.addLocal(l.getColor());
                if(removeLights){
                    lightList.remove(j);
                    j--;
                }
            }
            if (l instanceof LightProbe) {
                skyLightAndReflectionProbes.add((LightProbe) l);
                if(removeLights){
                    lightList.remove(j);
                    j--;
                }
            }
        }
        // todo:For reflection probes, only top three in view frustum are processed per frame (but scene can contain large amount of reflection probes)
        if(skyLightAndReflectionProbes.size() > 3){

        }
        ambientLightColor.a = 1.0f;
    }

}
