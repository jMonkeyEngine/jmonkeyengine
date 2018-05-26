/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
import com.jme3.bounding.BoundingSphere;
import com.jme3.light.*;
import static com.jme3.light.Light.Type.Directional;
import static com.jme3.light.Light.Type.Spot;
import com.jme3.material.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import com.jme3.shadow.next.array.ArrayShadowMap;
import com.jme3.shadow.next.array.ArrayShadowMapSlice;
import com.jme3.shadow.next.array.DirectionalArrayShadowMap;
import com.jme3.texture.TextureArray;
import java.util.Comparator;

import java.util.*;

public final class SinglePassAndImageBasedLightingLogic extends DefaultTechniqueDefLogic {

    private static final String DEFINE_SINGLE_PASS_LIGHTING = "SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_INDIRECT_LIGHTING = "INDIRECT_LIGHTING";
    private static final String DEFINE_IN_PASS_SHADOWS = "IN_PASS_SHADOWS";
    private static final String DEFINE_NUM_PSSM_SPLITS = "NUM_PSSM_SPLITS";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();

    private final ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);
    private TextureArray shadowMapArray;
    private Vector3f pssmSplitsPositions;
    private int numPssmSplits;
    private static final String DEFINE_NB_PROBES = "NB_PROBES";
    private List<LightProbe> lightProbes = new ArrayList<>(3);

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private final int inPassShadowsDefineId;
    private final int nbLightsDefineId;
    private final int numPssmSplitsDefineId;
    private final int nbProbesDefineId;

    public SinglePassAndImageBasedLightingLogic(TechniqueDef techniqueDef) {
        super(techniqueDef);
        numPssmSplitsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NUM_PSSM_SPLITS, VarType.Int);
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_SINGLE_PASS_LIGHTING, VarType.Boolean);
        nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
        inPassShadowsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_IN_PASS_SHADOWS, VarType.Boolean);
        nbProbesDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_PROBES, VarType.Int);
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, Geometry geometry, DefineList defines) {
        
        defines.set(singlePassLightingDefineId, true);

        // TODO: here we have a problem, this is called once before render,
        // so the define will be set for all passes (in case we have more than NB_LIGHTS lights)
        // Though the second pass should not render IBL as it is taken care of on 
        // first pass like ambient light in phong lighting.
        // We cannot change the define between passes and the old technique, and 
        // for some reason the code fails on mac (renders nothing).
        getFilteredLightList(renderManager, geometry);
       
        ambientLightColor.set(0, 0, 0, 1);
        lightProbes.clear();
        pssmSplitsPositions = null;
        numPssmSplits = 0;
        
        for (int i = 0; i < filteredLightList.size(); i++) {
            Light light = filteredLightList.get(i);
            if (light instanceof AmbientLight) {
                ambientLightColor.addLocal(light.getColor());
                filteredLightList.remove(i--);
            } else if (light instanceof LightProbe) {
                lightProbes.add((LightProbe) light);
                filteredLightList.remove(i--);
            } else if (light.getShadowMap() != null) {
                ArrayShadowMap shadowMap = (ArrayShadowMap) light.getShadowMap();
                shadowMapArray = shadowMap.getArray();
                if (light.getType() == Light.Type.Directional) {
                    numPssmSplits = shadowMap.getNumSlices();
                    pssmSplitsPositions = ((DirectionalArrayShadowMap) shadowMap).getProjectionSplitPositions();
                }
            }
        }
        defines.set(nbProbesDefineId, lightProbes.size());
        ambientLightColor.a = 1.0f;
        
        filteredLightList.sort(new Comparator<Light>() {
            @Override
            public int compare(Light a, Light b) {
                boolean shadA = a.getShadowMap() != null;
                boolean shadB = b.getShadowMap() != null;
                if (shadA != shadB) {
                    return shadA ? -1 : 1;
                } else {
                    int ordA = a.getType().ordinal();
                    int ordB = b.getType().ordinal();
                    return ordB - ordA;
                }
            }
        });
        
        defines.set(nbLightsDefineId, renderManager.getSinglePassLightBatchSize() * 3);
        defines.set(inPassShadowsDefineId, shadowMapArray != null);
        defines.set(numPssmSplitsDefineId, numPssmSplits);
        
        return super.makeCurrent(assetManager, renderManager, rendererCaps, geometry, defines);
    }

    /**
     * Uploads the lights in the light list as two uniform arrays.<br/><br/> *
     * <p>
     * <code>uniform vec4 g_LightColor[numLights];</code><br/> //
     * g_LightColor.rgb is the diffuse/specular color of the light.<br/> //
     * g_Lightcolor.a is the type of light, 0 = Directional, 1 = Point, <br/> //
     * 2 = Spot. <br/> <br/>
     * <code>uniform vec4 g_LightPosition[numLights];</code><br/> //
     * g_LightPosition.xyz is the position of the light (for point lights)<br/>
     * // or the direction of the light (for directional lights).<br/> //
     * g_LightPosition.w is the inverse radius (1/r) of the light (for
     * attenuation) <br/> </p>
     */
    protected int updateLightListUniforms(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex, int lastTexUnit) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform lightData = shader.getUniform("g_LightData");
        lightData.setVector4Length(numLights * 3);//8 lights * max 3
        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");

        // Matrix4f
        Uniform lightProbeData = shader.getUniform("g_LightProbeData");
        Uniform lightProbeData2 = shader.getUniform("g_LightProbeData2");
        Uniform lightProbeData3 = shader.getUniform("g_LightProbeData3");

        Uniform shCoeffs = shader.getUniform("g_ShCoeffs");
        Uniform lightProbePemMap = shader.getUniform("g_PrefEnvMap");
        Uniform shCoeffs2 = shader.getUniform("g_ShCoeffs2");
        Uniform lightProbePemMap2 = shader.getUniform("g_PrefEnvMap2");
        Uniform shCoeffs3 = shader.getUniform("g_ShCoeffs3");
        Uniform lightProbePemMap3 = shader.getUniform("g_PrefEnvMap3");

        if (startIndex != 0) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            ambientColor.setValue(VarType.Vector4, ambientLightColor);
        }

        //If there is a lightProbe in the list we force its render on the first pass
        if (!lightProbes.isEmpty()) {
            LightProbe lightProbe = lightProbes.get(0);
            lastTexUnit = setProbeData(rm, lastTexUnit, lightProbeData, shCoeffs, lightProbePemMap, lightProbe);
            if (lightProbes.size() > 1) {
                lightProbe = lightProbes.get(1);
                lastTexUnit = setProbeData(rm, lastTexUnit, lightProbeData2, shCoeffs2, lightProbePemMap2, lightProbe);
            }
            if (lightProbes.size() > 2) {
                lightProbe = lightProbes.get(2);
                setProbeData(rm, lastTexUnit, lightProbeData3, shCoeffs3, lightProbePemMap3, lightProbe);
            }
        } else {
            //Disable IBL for this pass
            lightProbeData.setValue(VarType.Matrix4, LightProbe.FALLBACK_MATRIX);
        }

        Uniform shadowMatricesUniform = shader.getUniform("g_ShadowMatrices");
        shadowMatricesUniform.setMatrix4Length(numLights + numPssmSplits);
        int shadowMatrixIndex = numPssmSplits;
        int lightDataIndex = 0;
        int curIndex;
        int endIndex = Math.min(startIndex + numLights, lightList.size());
        
        ArrayShadowMap directionalShadowMap = null;
        
        for (curIndex = startIndex; curIndex < endIndex; curIndex++) {
            Light light = lightList.get(curIndex);
            
            if (light.getType() == Light.Type.Ambient || light.getType() == Light.Type.Probe) {
                throw new AssertionError();
            }
            
            if (light.getShadowMap() != null) {
                ArrayShadowMap shadowMap = (ArrayShadowMap) light.getShadowMap();
                if (light.getType() == Directional) {
                    directionalShadowMap = shadowMap;
                } else if (light.getType() == Spot) {
                    for (int j = 0; j < shadowMap.getNumSlices(); j++) {
                        ArrayShadowMapSlice slice = (ArrayShadowMapSlice) shadowMap.getSlice(j);
                        shadowMatricesUniform.setMatrix4InArray(
                                slice.getBiasedViewProjectionMatrix(),
                                shadowMatrixIndex);
                        shadowMatrixIndex++;
                    }
                }
            }
            
            ColorRGBA color = light.getColor();
            lightData.setVector4InArray(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    encodeLightTypeAndShadowMapIndex(light),
                    lightDataIndex++);

            switch (light.getType()) {
                case Directional: {
                    DirectionalLight dl = (DirectionalLight) light;
                    Vector3f dir = dl.getDirection();
                    lightData.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, lightDataIndex++);
                    lightData.setVector4InArray(0, 0, 0, 0, lightDataIndex++);
                    break;
                }
                case Point: {
                    PointLight pl = (PointLight) light;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    lightData.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, lightDataIndex++);
                    lightData.setVector4InArray(0, 0, 0, 0, lightDataIndex++);
                    break;
                }
                case Spot: {
                    SpotLight sl = (SpotLight) light;
                    Vector3f pos = sl.getPosition();
                    Vector3f dir = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();
                    lightData.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRange, lightDataIndex++);
                    lightData.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), spotAngleCos, lightDataIndex++);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + light.getType());
            }
        }

        // Padding of unsued buffer space
        while (lightDataIndex < numLights * 3) {
            lightData.setVector4InArray(0f, 0f, 0f, 0f, lightDataIndex++);
        }
        
        if (directionalShadowMap != null) {
            for (int i = 0; i < numPssmSplits; i++) {
                ArrayShadowMapSlice slice = (ArrayShadowMapSlice) directionalShadowMap.getSlice(i);
                shadowMatricesUniform.setMatrix4InArray(slice.getBiasedViewProjectionMatrix(), i);
            }
        }

        if (shadowMapArray != null) {
            rm.getRenderer().setTexture(lastTexUnit, shadowMapArray);
            shader.getUniform("g_ShadowMapArray").setValue(VarType.Int, lastTexUnit);
        }
        
        if (pssmSplitsPositions != null) {
            shader.getUniform("g_PssmSplits").setValue(VarType.Vector3, pssmSplitsPositions);
        }

        return curIndex;
    }

    private int setProbeData(RenderManager rm, int lastTexUnit, Uniform lightProbeData, Uniform shCoeffs, Uniform lightProbePemMap, LightProbe lightProbe) {

        lightProbeData.setValue(VarType.Matrix4, lightProbe.getUniformMatrix());
        shCoeffs.setValue(VarType.Vector3Array, lightProbe.getShCoeffs());
        //assigning new texture indexes
        int pemUnit = lastTexUnit++;
        rm.getRenderer().setTexture(pemUnit, lightProbe.getPrefilteredEnvMap());
        lightProbePemMap.setValue(VarType.Int, pemUnit);
        return lastTexUnit;
    }

    @Override
    public void render(RenderManager renderManager, Shader shader, Geometry geometry, int lastTexUnit) {
        int nbRenderedLights = 0;
        Renderer renderer = renderManager.getRenderer();
        int batchSize = renderManager.getSinglePassLightBatchSize();
        if (filteredLightList.size() == 0) {
            updateLightListUniforms(shader, geometry, filteredLightList,batchSize, renderManager, 0, lastTexUnit);
            renderer.setShader(shader);
            renderMeshFromGeometry(renderer, geometry);
        } else {
            while (nbRenderedLights < filteredLightList.size()) {
                nbRenderedLights = updateLightListUniforms(shader, geometry, filteredLightList, batchSize, renderManager, nbRenderedLights, lastTexUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
    }
}
