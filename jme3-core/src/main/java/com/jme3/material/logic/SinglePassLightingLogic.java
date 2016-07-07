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
package com.jme3.material.logic;

import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.util.TempVars;
import java.util.EnumSet;

public final class SinglePassLightingLogic extends DefaultTechniqueDefLogic {

    private static final String DEFINE_SINGLE_PASS_LIGHTING = "SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();

    private final ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private final int nbLightsDefineId;

    public SinglePassLightingLogic(TechniqueDef techniqueDef) {
        super(techniqueDef);
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_SINGLE_PASS_LIGHTING, VarType.Boolean);
        nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        defines.set(nbLightsDefineId, renderManager.getSinglePassLightBatchSize() * 3);
        defines.set(singlePassLightingDefineId, true);
        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
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
    protected int updateLightListUniforms(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform lightData = shader.getUniform("g_LightData");
        lightData.setVector4Length(numLights * 3);//8 lights * max 3
        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");


        if (startIndex != 0) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList, true, ambientLightColor));
        }

        int lightDataIndex = 0;
        TempVars vars = TempVars.get();
        Vector4f tmpVec = vars.vect4f1;
        int curIndex;
        int endIndex = numLights + startIndex;
        for (curIndex = startIndex; curIndex < endIndex && curIndex < lightList.size(); curIndex++) {

            Light l = lightList.get(curIndex);
            if (l.getType() == Light.Type.Ambient) {
                endIndex++;
                continue;
            }
            ColorRGBA color = l.getColor();
            //Color
            lightData.setVector4InArray(color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    l.getType().getId(),
                    lightDataIndex);
            lightDataIndex++;

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    //Data directly sent in view space to avoid a matrix mult for each pixel
                    tmpVec.set(dir.getX(), dir.getY(), dir.getZ(), 0.0f);
                    rm.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
//                        tmpVec.divideLocal(tmpVec.w);
//                        tmpVec.normalizeLocal();
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), -1, lightDataIndex);
                    lightDataIndex++;
                    //PADDING
                    lightData.setVector4InArray(0, 0, 0, 0, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    tmpVec.set(pos.getX(), pos.getY(), pos.getZ(), 1.0f);
                    rm.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
                    //tmpVec.divideLocal(tmpVec.w);
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), invRadius, lightDataIndex);
                    lightDataIndex++;
                    //PADDING
                    lightData.setVector4InArray(0, 0, 0, 0, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();
                    tmpVec.set(pos2.getX(), pos2.getY(), pos2.getZ(), 1.0f);
                    rm.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
                    // tmpVec.divideLocal(tmpVec.w);
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), invRange, lightDataIndex);
                    lightDataIndex++;

                    //We transform the spot direction in view space here to save 5 varying later in the lighting shader
                    //one vec4 less and a vec4 that becomes a vec3
                    //the downside is that spotAngleCos decoding happens now in the frag shader.
                    tmpVec.set(dir2.getX(), dir2.getY(), dir2.getZ(), 0.0f);
                    rm.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
                    tmpVec.normalizeLocal();
                    lightData.setVector4InArray(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), spotAngleCos, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Probe:
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        vars.release();
        //Padding of unsued buffer space
        while(lightDataIndex < numLights * 3) {
            lightData.setVector4InArray(0f, 0f, 0f, 0f, lightDataIndex);
            lightDataIndex++;
        }
        return curIndex;
    }

    @Override
    public void render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, int lastTexUnit) {
        int nbRenderedLights = 0;
        Renderer renderer = renderManager.getRenderer();
        int batchSize = renderManager.getSinglePassLightBatchSize();
        if (lights.size() == 0) {
            updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, 0);
            renderer.setShader(shader);
            renderMeshFromGeometry(renderer, geometry);
        } else {
            while (nbRenderedLights < lights.size()) {
                nbRenderedLights = updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, nbRenderedLights);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
    }
}
