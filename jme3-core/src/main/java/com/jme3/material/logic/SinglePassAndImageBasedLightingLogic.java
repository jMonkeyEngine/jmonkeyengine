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
package com.jme3.material.logic;

import com.jme3.asset.AssetManager;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.TempVars;

import java.util.*;

public final class SinglePassAndImageBasedLightingLogic extends DefaultTechniqueDefLogic {

    private static final String DEFINE_SINGLE_PASS_LIGHTING = "SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_NB_PROBES = "NB_PROBES";
    private static final String DEFINE_USE_AMBIENT_LIGHT = "USE_AMBIENT_LIGHT";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();

    private boolean useAmbientLight;
    private final ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);
    final private List<LightProbe> lightProbes = new ArrayList<>(3);

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private final int nbLightsDefineId;
    private final int nbProbesDefineId;
    private final int useAmbientLightDefineId;

    public SinglePassAndImageBasedLightingLogic(TechniqueDef techniqueDef) {
        super(techniqueDef);
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_SINGLE_PASS_LIGHTING, VarType.Boolean);
        nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
        nbProbesDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_PROBES, VarType.Int);
        useAmbientLightDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_AMBIENT_LIGHT, VarType.Boolean);
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        defines.set(nbLightsDefineId, renderManager.getSinglePassLightBatchSize() * 3);
        defines.set(singlePassLightingDefineId, true);


        //TODO here we have a problem, this is called once before render, so the define will be set for all passes (in case we have more than NB_LIGHTS lights)
        //Though the second pass should not render IBL as it is taken care of on first pass like ambient light in phong lighting.
        //We cannot change the define between passes and the old technique, and for some reason the code fails on mac (renders nothing).
        if(lights != null) {
            lightProbes.clear();
            extractIndirectLights(lights, false);
            defines.set(nbProbesDefineId, lightProbes.size());
            defines.set(useAmbientLightDefineId, useAmbientLight);
        }

        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
    }

    /**
     * Uploads the lights in the light list as two uniform arrays.
     * <p>
     * <code>uniform vec4 g_LightColor[numLights];</code> //
     * g_LightColor.rgb is the diffuse/specular color of the light. //
     * g_LightColor.a is the type of light, 0 = Directional, 1 = Point, 2 = Spot.
     * <code>uniform vec4 g_LightPosition[numLights];</code> //
     * g_LightPosition.xyz is the position of the light (for point lights)
     * // or the direction of the light (for directional lights). //
     * g_LightPosition.w is the inverse radius (1/r) of the light (for
     * attenuation) </p>
     *
     * @param shader the Shader being used
     * @param g the Geometry being rendered
     * @param lightList the list of lights
     * @param numLights the number of lights to upload
     * @param rm to manage rendering
     * @param startIndex the starting index in the LightList
     * @param lastTexUnit the index of the most recently-used texture unit
     * @return the next starting index in the LightList
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

        lightProbes.clear();
        if (startIndex != 0) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            extractIndirectLights(lightList,true);
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

        int lightDataIndex = 0;
        TempVars vars = TempVars.get();
        Vector4f tmpVec = vars.vect4f1;
        int curIndex;
        int endIndex = numLights + startIndex;
        for (curIndex = startIndex; curIndex < endIndex && curIndex < lightList.size(); curIndex++) {
            Light l = lightList.get(curIndex);
            if(l.getType() == Light.Type.Ambient){
                endIndex++;
                continue;
            }
            ColorRGBA color = l.getColor();
            //Color

            if(l.getType() != Light.Type.Probe){
                lightData.setVector4InArray(color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        l.getType().getId(),
                        lightDataIndex);
                lightDataIndex++;
            }

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    //Data directly sent in view space to avoid a matrix mult for each pixel
                    tmpVec.set(dir.getX(), dir.getY(), dir.getZ(), 0.0f);
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), -1, lightDataIndex);
                    lightDataIndex++;
                    //PADDING
                    lightData.setVector4InArray(0,0,0,0, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    tmpVec.set(pos.getX(), pos.getY(), pos.getZ(), 1.0f);

                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), invRadius, lightDataIndex);
                    lightDataIndex++;
                    //PADDING
                    lightData.setVector4InArray(0,0,0,0, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();
                    tmpVec.set(pos2.getX(), pos2.getY(), pos2.getZ(),  1.0f);

                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), invRange, lightDataIndex);
                    lightDataIndex++;

                    tmpVec.set(dir2.getX(), dir2.getY(), dir2.getZ(),  0.0f);
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), spotAngleCos, lightDataIndex);
                    lightDataIndex++;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        vars.release();

        // pad unused buffer space
        while (lightDataIndex < numLights * 3) {
            lightData.setVector4InArray(0f, 0f, 0f, 0f, lightDataIndex);
            lightDataIndex++;
        }
        return curIndex;
    }

    private int setProbeData(RenderManager rm, int lastTexUnit, Uniform lightProbeData, Uniform shCoeffs,
            Uniform lightProbePemMap, LightProbe lightProbe) {
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
            String message = "Can't assign texture unit for LightProbe."
                    + " lastTexUnit=" + lastTexUnit;
            throw new IllegalArgumentException(message);
        }
        lightProbePemMap.setValue(VarType.Int, pemUnit);
        return lastTexUnit;
    }

    @Override
    public void render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, int lastTexUnit) {
        int nbRenderedLights = 0;
        Renderer renderer = renderManager.getRenderer();
        int batchSize = renderManager.getSinglePassLightBatchSize();
        if (lights.size() == 0) {
            updateLightListUniforms(shader, geometry, lights,batchSize, renderManager, 0, lastTexUnit);
            renderer.setShader(shader);
            renderMeshFromGeometry(renderer, geometry);
        } else {
            while (nbRenderedLights < lights.size()) {
                nbRenderedLights = updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, nbRenderedLights, lastTexUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
        return;
    }

    protected void extractIndirectLights(LightList lightList, boolean removeLights) {
        ambientLightColor.set(0, 0, 0, 1);
        useAmbientLight = false;
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof AmbientLight) {
                useAmbientLight = true;
                ambientLightColor.addLocal(l.getColor());
                if(removeLights){
                    lightList.remove(l);
                    j--;
                }
            }
            if (l instanceof LightProbe) {
                lightProbes.add((LightProbe) l);
                if(removeLights){
                    lightList.remove(l);
                    j--;
                }
            }
        }
        ambientLightColor.a = 1.0f;
    }
}
