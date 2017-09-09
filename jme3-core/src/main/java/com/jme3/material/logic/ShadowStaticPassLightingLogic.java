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
package com.jme3.material.logic;

import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.TechniqueDef;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.shadow.next.array.ArrayShadowMap;
import com.jme3.shadow.next.array.ArrayShadowMapSlice;
import com.jme3.shadow.next.array.DirectionalArrayShadowMap;
import com.jme3.shadow.next.array.SpotArrayShadowMap;
import com.jme3.shadow.next.array.SpotArrayShadowMapSlice;
import com.jme3.texture.TextureArray;
import java.util.EnumSet;

public class ShadowStaticPassLightingLogic extends StaticPassLightingLogic {

    private static final String DEFINE_NUM_PSSM_SPLITS = "NUM_PSSM_SPLITS";
    private static final String DEFINE_NUM_SHADOW_DIR_LIGHTS = "NUM_SHADOW_DIR_LIGHTS";
    private static final String DEFINE_NUM_SHADOW_POINT_LIGHTS = "NUM_SHADOW_POINT_LIGHTS";
    private static final String DEFINE_NUM_SHADOW_SPOT_LIGHTS = "NUM_SHADOW_SPOT_LIGHTS";

    private final int numPssmSplitsDefineId;
    private final int numShadowDirLightsDefineId;
    private final int numShadowPointLightsDefineId;
    private final int numShadowSpotLightsDefineId;
    private int numShadowDirLights = 0;
    private int numShadowPointLights = 0;
    private int numShadowSpotLights = 0;
    private final Matrix4f[] shadowMatrices = new Matrix4f[5];

    public ShadowStaticPassLightingLogic(TechniqueDef techniqueDef) {
        super(techniqueDef);
        numPssmSplitsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NUM_PSSM_SPLITS, VarType.Int);
        numShadowDirLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NUM_SHADOW_DIR_LIGHTS, VarType.Int);
        numShadowPointLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NUM_SHADOW_POINT_LIGHTS, VarType.Int);
        numShadowSpotLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NUM_SHADOW_SPOT_LIGHTS, VarType.Int);

        for (int i = 0; i < shadowMatrices.length; i++) {
            shadowMatrices[i] = new Matrix4f();
        }
    }

    @Override
    protected void makeCurrentBase(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {

        tempDirLights.clear();
        tempPointLights.clear();
        tempSpotLights.clear();
        ambientLightColor.set(0, 0, 0, 1);
        numShadowDirLights = 0;
        numShadowPointLights = 0;
        numShadowSpotLights = 0;

        int pssmSplits = 0;

        for (Light light : lights) {
            switch (light.getType()) {
                case Directional:
                    if (light.getShadowMap() != null) {
                        pssmSplits = light.getShadowMap().getNumSlices();
                        tempDirLights.add(numShadowDirLights, (DirectionalLight) light);
                        numShadowDirLights++;
                    } else {
                        tempDirLights.add((DirectionalLight) light);
                    }
                    break;
                case Point:
                    if (light.getShadowMap() != null) {
                        tempPointLights.add(numShadowPointLights, (PointLight) light);
                        numShadowPointLights++;
                    } else {
                        tempPointLights.add((PointLight) light);
                    }
                    break;
                case Spot:
                    if (light.getShadowMap() != null) {
                        tempSpotLights.add(numShadowSpotLights, (SpotLight) light);
                        numShadowSpotLights++;
                    } else {
                        tempSpotLights.add((SpotLight) light);
                    }
                    break;
                case Ambient:
                    ambientLightColor.addLocal(light.getColor());
                    break;
            }
        }
        ambientLightColor.a = 1.0f;

        defines.set(numDirLightsDefineId, tempDirLights.size());
        defines.set(numPointLightsDefineId, tempPointLights.size());
        defines.set(numSpotLightsDefineId, tempSpotLights.size());

        defines.set(numShadowDirLightsDefineId, numShadowDirLights);
        defines.set(numShadowPointLightsDefineId, numShadowPointLights);
        defines.set(numShadowSpotLightsDefineId, numShadowSpotLights);

        defines.set(numPssmSplitsDefineId, pssmSplits);
    }

    @Override
    protected float getShadowMapIndex(Light light) {
        if (light.getShadowMap() == null) {
            return -1.0f;
        }
        ArrayShadowMap map = (ArrayShadowMap) light.getShadowMap();
        return (float) map.getFirstArraySlice();
    }

    @Override
    protected void updateShadowUniforms(Renderer renderer, Shader shader, int nextTextureUnit) {
        TextureArray array = null;
        Vector4f pssmSplits = null;

        Uniform shadowMatricesUniform = shader.getUniform("g_ShadowMatrices");
        int shadowMatrixIndex = 0;
        for (int i = 0; i < numShadowDirLights; i++) {
            DirectionalArrayShadowMap map = (DirectionalArrayShadowMap) tempDirLights.get(i).getShadowMap();
            array = map.getArray();
            pssmSplits = map.getProjectionSplitPositions();
            for (int j = 0; j < map.getNumSlices(); j++) {
                ArrayShadowMapSlice slice = (ArrayShadowMapSlice) map.getSlice(j);
                BIAS_MATRIX.mult(slice.getViewProjectionMatrix(), shadowMatrices[shadowMatrixIndex]);
                shadowMatrixIndex++;
            }
        }

        for (int i = 0; i < numShadowSpotLights; i++) {
            SpotArrayShadowMap map = (SpotArrayShadowMap) tempSpotLights.get(i).getShadowMap();
            array = map.getArray();
            SpotArrayShadowMapSlice slice = map.getSlice(0);
            BIAS_MATRIX.mult(slice.getViewProjectionMatrix(), shadowMatrices[shadowMatrixIndex]);
            shadowMatrixIndex++;
        }

        shadowMatricesUniform.setValue(VarType.Matrix4Array, shadowMatrices);
        if (array != null) {
            renderer.setTexture(nextTextureUnit, array);
            Uniform shadowMapArrayUniform = shader.getUniform("g_ShadowMapArray");
            shadowMapArrayUniform.setValue(VarType.Int, nextTextureUnit);
        }
        if (pssmSplits != null) {
            Uniform pssmSplitsUniform = shader.getUniform("g_PssmSplits");
            pssmSplitsUniform.setValue(VarType.Vector4, pssmSplits);
        }
    }
}
