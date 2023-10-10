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
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class DeferredSinglePassLightingLogic extends DefaultTechniqueDefLogic {
    private final static String _S_LIGHT_CULL_DRAW_STAGE = "Light_Cull_Draw_Stage";
    private static final String DEFINE_DEFERRED_SINGLE_PASS_LIGHTING = "DEFERRED_SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_USE_TEXTURE_PACK_MODE = "USE_TEXTURE_PACK_MODE";
    private static final String DEFINE_PACK_NB_LIGHTS = "PACK_NB_LIGHTS";
    private static final String DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES = "NB_SKY_LIGHT_AND_REFLECTION_PROBES";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();
    private boolean bUseTexturePackMode = true;
    // 避免过多的光源
    private static final int MAX_LIGHT_NUM = 9046;
    // 使用texture来一次性存储大量光源数据,避免多次绘制
    private Texture2D lightData1;
    private Texture2D lightData2;
    private Texture2D lightData3;
    private ImageRaster lightDataUpdateIO1;
    private ImageRaster lightDataUpdateIO2;
    private ImageRaster lightDataUpdateIO3;
    private int lightNum;

    private final ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);
    final private List<LightProbe> skyLightAndReflectionProbes = new ArrayList<>(3);

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private int nbLightsDefineId;
    private int packNbLightsDefineId;
    private int packTextureModeDefineId;
    private final int nbSkyLightAndReflectionProbesDefineId;

    public DeferredSinglePassLightingLogic(TechniqueDef techniqueDef) {
        super(techniqueDef);
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_DEFERRED_SINGLE_PASS_LIGHTING, VarType.Boolean);
        if(bUseTexturePackMode){
            packNbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_PACK_NB_LIGHTS, VarType.Int);
            packTextureModeDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_TEXTURE_PACK_MODE, VarType.Boolean);
            prepaLightData(1024);
        }
        else{
            nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
        }
        nbSkyLightAndReflectionProbesDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES, VarType.Int);
    }

    private void cleanupLightData(){
        if(this.lightData1 != null){
            this.lightData1.getImage().dispose();
        }
        if(this.lightData2 != null){
            this.lightData2.getImage().dispose();
        }
        if(this.lightData3 != null){
            this.lightData3.getImage().dispose();
        }
    }

    private void prepaLightData(int lightNum){
        this.lightNum = lightNum;
        // 1d texture
        lightData1 = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightData1.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightData1.setMagFilter(Texture.MagFilter.Nearest);
        lightData1.setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage = new Image(Image.Format.RGBA32F, lightNum, 1, data, null, ColorSpace.Linear);
        lightData1.setImage(convertedImage);
        lightData1.getImage().setMipmapsGenerated(false);
        lightDataUpdateIO1 = ImageRaster.create(lightData1.getImage());

        lightData2 = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightData2.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightData2.setMagFilter(Texture.MagFilter.Nearest);
        lightData2.setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data2 = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage2 = new Image(Image.Format.RGBA32F, lightNum, 1, data2, null, ColorSpace.Linear);
        lightData2.setImage(convertedImage2);
        lightData2.getImage().setMipmapsGenerated(false);
        lightDataUpdateIO2 = ImageRaster.create(lightData2.getImage());

        lightData3 = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightData3.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightData3.setMagFilter(Texture.MagFilter.Nearest);
        lightData3.setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data3 = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage3 = new Image(Image.Format.RGBA32F, lightNum, 1, data3, null, ColorSpace.Linear);
        lightData3.setImage(convertedImage3);
        lightData3.getImage().setMipmapsGenerated(false);
        lightDataUpdateIO3 = ImageRaster.create(lightData3.getImage());
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        if(bUseTexturePackMode){
            defines.set(packNbLightsDefineId, this.lightNum);
            defines.set(packTextureModeDefineId, true);
        }
        else{
            defines.set(nbLightsDefineId, renderManager.getSinglePassLightBatchSize() * 3);
        }
        defines.set(singlePassLightingDefineId, true);
        //TODO here we have a problem, this is called once before render, so the define will be set for all passes (in case we have more than NB_LIGHTS lights)
        //Though the second pass should not render IBL as it is taken care of on first pass like ambient light in phong lighting.
        //We cannot change the define between passes and the old technique, and for some reason the code fails on mac (renders nothing).
        if(lights != null) {
            SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientLightColor, skyLightAndReflectionProbes, false);
            defines.set(nbSkyLightAndReflectionProbesDefineId, skyLightAndReflectionProbes.size());
        }
        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
    }

    protected int updateLightListPackToTexture(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex, boolean isLightCullStageDraw, int lastTexUnit) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");

//        skyLightAndReflectionProbes.clear();
        if (startIndex != 0 || isLightCullStageDraw) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
//            extractSkyLightAndReflectionProbes(lightList,true);
            ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList, true, ambientLightColor));
        }

        // render skyLights and reflectionProbes
        if(!skyLightAndReflectionProbes.isEmpty()){
            // Matrix4f
            Uniform skyLightData = shader.getUniform("g_SkyLightData");
            Uniform skyLightData2 = shader.getUniform("g_SkyLightData2");
            Uniform skyLightData3 = shader.getUniform("g_SkyLightData3");

            Uniform shCoeffs = shader.getUniform("g_ShCoeffs");
            Uniform reflectionProbePemMap = shader.getUniform("g_ReflectionEnvMap");
            Uniform shCoeffs2 = shader.getUniform("g_ShCoeffs2");
            Uniform reflectionProbePemMap2 = shader.getUniform("g_ReflectionEnvMap2");
            Uniform shCoeffs3 = shader.getUniform("g_ShCoeffs3");
            Uniform reflectionProbePemMap3 = shader.getUniform("g_ReflectionEnvMap3");

            LightProbe skyLight = skyLightAndReflectionProbes.get(0);
            lastTexUnit = SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(rm, lastTexUnit, skyLightData, shCoeffs, reflectionProbePemMap, skyLight);
            if (skyLightAndReflectionProbes.size() > 1) {
                skyLight = skyLightAndReflectionProbes.get(1);
                lastTexUnit = SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(rm, lastTexUnit, skyLightData2, shCoeffs2, reflectionProbePemMap2, skyLight);
            }
            if (skyLightAndReflectionProbes.size() > 2) {
                skyLight = skyLightAndReflectionProbes.get(2);
                SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(rm, lastTexUnit, skyLightData3, shCoeffs3, reflectionProbePemMap3, skyLight);
            }
        } else {
            Uniform skyLightData = shader.getUniform("g_SkyLightData");
            //Disable IBL for this pass
            skyLightData.setValue(VarType.Matrix4, LightProbe.FALLBACK_MATRIX);
        }

        TempVars vars = TempVars.get();
        int curIndex;
        int endIndex = numLights + startIndex;
        ColorRGBA temp = vars.color;
        for (curIndex = startIndex; curIndex < endIndex && curIndex < lightList.size(); curIndex++) {

            Light l = lightList.get(curIndex);
            if (l.getType() == Light.Type.Ambient || l.getType() == Light.Type.Probe) {
                endIndex++;
                continue;
            }
            ColorRGBA color = l.getColor();
            //Color
            temp.r = color.getRed();
            temp.g = color.getGreen();
            temp.b = color.getBlue();
            temp.a = l.getType().getId();
            lightDataUpdateIO1.setPixel(curIndex, 0, temp);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    temp.r = dir.getX();
                    temp.g = dir.getY();
                    temp.b = dir.getZ();
                    temp.a = -1;
                    lightDataUpdateIO2.setPixel(curIndex, 0, temp);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    temp.r = pos.getX();
                    temp.g = pos.getY();
                    temp.b = pos.getZ();
                    temp.a = invRadius;
                    lightDataUpdateIO2.setPixel(curIndex, 0, temp);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();
                    temp.r = pos2.getX();
                    temp.g = pos2.getY();
                    temp.b = pos2.getZ();
                    temp.a = invRange;
                    lightDataUpdateIO2.setPixel(curIndex, 0, temp);

                    //We transform the spot direction in view space here to save 5 varying later in the lighting shader
                    //one vec4 less and a vec4 that becomes a vec3
                    //the downside is that spotAngleCos decoding happens now in the frag shader.
                    temp.r = dir2.getX();
                    temp.g = dir2.getY();
                    temp.b = dir2.getZ();
                    temp.a = spotAngleCos;
                    lightDataUpdateIO3.setPixel(curIndex, 0, temp);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        temp.r = temp.g = temp.b = temp.a = 0;
        // Since the drawing is sent within the loop branch, and actually before the actual glSwapBuffers, the gl commands actually reside at the graphics driver level. So in order to correctly branch within the loop, the size must be fixed here (while filling the number of light sources).
        ColorRGBA temp2 = vars.color2;
        for(;curIndex < this.lightNum;curIndex++){
            temp2 = lightDataUpdateIO1.getPixel(curIndex, 0);
            if(temp2.r == 0 && temp2.g == 0 && temp2.b == 0 && temp2.a == 0)break;
            lightDataUpdateIO1.setPixel(curIndex, 0, temp);
            lightDataUpdateIO2.setPixel(curIndex, 0, temp);
            lightDataUpdateIO3.setPixel(curIndex, 0, temp);
        }
        vars.release();
        lightData1.getImage().setUpdateNeeded();
        lightData2.getImage().setUpdateNeeded();
        lightData3.getImage().setUpdateNeeded();
//        Uniform g_LightPackData1 = shader.getUniform("g_LightPackData1");
//        g_LightPackData1.setValue(VarType.Texture2D, lightData1);
//        Uniform g_LightPackData2 = shader.getUniform("g_LightPackData2");
//        g_LightPackData2.setValue(VarType.Texture2D, lightData2);
//        Uniform g_LightPackData3 = shader.getUniform("g_LightPackData3");
//        g_LightPackData3.setValue(VarType.Texture2D, lightData3);
        g.getMaterial().setTexture("LightPackData1", lightData1);
        g.getMaterial().setTexture("LightPackData2", lightData2);
        g.getMaterial().setTexture("LightPackData3", lightData3);
        return curIndex;
    }

    /**
     * Uploads the lights in the light list as two uniform arrays.
     * <p>
     * <code>uniform vec4 g_LightColor[numLights];</code><br> //
     * g_LightColor.rgb is the diffuse/specular color of the light.<br> //
     * g_Lightcolor.a is the type of light, 0 = Directional, 1 = Point, <br> //
     * 2 = Spot. <br> <br>
     * <code>uniform vec4 g_LightPosition[numLights];</code><br> //
     * g_LightPosition.xyz is the position of the light (for point lights)<br>
     * // or the direction of the light (for directional lights).<br> //
     * g_LightPosition.w is the inverse radius (1/r) of the light (for
     * attenuation)</p>
     *
     * @param shader the Shader being used
     * @param g the Geometry being rendered
     * @param lightList the list of lights
     * @param numLights the number of lights to upload
     * @param rm to manage rendering
     * @param startIndex the starting index in the LightList
     * @param isLightCullStageDraw isLightCullStageDraw
     * @return the next starting index in the LightList
     */
    protected int updateLightListUniforms(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex, boolean isLightCullStageDraw) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform lightData = shader.getUniform("g_LightData");
        lightData.setVector4Length(numLights * 3);//8 lights * max 3
        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");

        if (startIndex != 0 || isLightCullStageDraw) {
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
            if (l.getType() == Light.Type.Ambient || l.getType() == Light.Type.Probe) {
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
                    lightData.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, lightDataIndex);
                    lightDataIndex++;
                    //PADDING
                    lightData.setVector4InArray(0, 0, 0, 0, lightDataIndex);
                    lightDataIndex++;
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    lightData.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, lightDataIndex);
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
                    lightData.setVector4InArray(pos2.getX(), pos2.getY(), pos2.getZ(), invRange, lightDataIndex);
                    lightDataIndex++;

                    //We transform the spot direction in view space here to save 5 varying later in the lighting shader
                    //one vec4 less and a vec4 that becomes a vec3
                    //the downside is that spotAngleCos decoding happens now in the frag shader.
                    lightData.setVector4InArray(dir2.getX(), dir2.getY(), dir2.getZ(), spotAngleCos, lightDataIndex);
                    lightDataIndex++;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        vars.release();
        // pad unused buffer space
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
        boolean isLightCullStageDraw = false;
        if(geometry.getUserData(_S_LIGHT_CULL_DRAW_STAGE) != null){
            isLightCullStageDraw = geometry.getUserData(_S_LIGHT_CULL_DRAW_STAGE);
        }
        // todo: One optimization approach here is:
        // todo: Use uniform array storage scheme:
        // todo: First divide lights into DirectionalLights, PointLights, SpotLights three lists
        // todo: Then first draw DirectionalLights and SpotLights, these two are drawn with Rect, submitting the number of lights specified by SingleBatch at a time
        // todo: Then use Sphere (turn on FrontFace culling to avoid camera entering sphere and light source shading disappears) to draw point lights, drawing groups of Sphere instances based on SingleBatch at a time

        // todo: Another approach is to divide lights into full-screen and non full-screen, handle DirectionalLight and SpotLight as full-screen, then traverse all PointLights with invalid radius also categorized as full-screen
        // todo: Then take the remaining PointLights as non full-screen lights, then store all light source information in the texture at once, with full-screen in front and non full-screen behind in memory
        // todo: Then initiate a RectPass, draw all full-screen lights in one DC, update light_offset, then use precreated SphereInstance with SingleBatch to draw the remaining non full-screen lights
        // todo: The second method will be adopted here

        // todo: For light probes (temporarily implemented based on preCompute light probe), get light probe grid based on current view frustum visible range, execute multi pass according to light probe grid
        // todo: For reflection probes, use textureArray (cubemap projection, with mipmap), collect reflection probes visible to current camera view frustum, and limit the number of reflection probes allowed in the current view frustum
        if(bUseTexturePackMode){
            if(this.lightNum != renderManager.getCurMaxDeferredShadingLightNum()){
                cleanupLightData();
                prepaLightData(renderManager.getCurMaxDeferredShadingLightNum());
            }
            // todo:Currently, this texturePackMode is only suitable for scenes where there are a large number of light sources per frame. The number of light sources is submitted to the texture all at once, so lightNum can be pre-allocated, but light source information can also be submitted to the texture all at once here, and then drawn in multiple passes (drawing each time by the specified singlePassLightBatchSize)
            Uniform lightCount = shader.getUniform("g_LightCount");
            SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientLightColor, skyLightAndReflectionProbes, true);
            int count = lights.size();
//            lightCount.setValue(VarType.Int, count);
            lightCount.setValue(VarType.Int, this.lightNum);
            while (nbRenderedLights < count) {
                // todo:采用第二种方法优化deferred,则这里使用当前类的geometrys(rect,sphere)进行绘制,而不使用这个传递进来的geometry(或者在外部传递两个geometry,一个rect一个sphereinstance)
                nbRenderedLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, nbRenderedLights, isLightCullStageDraw, lastTexUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
        else{
            int batchSize = renderManager.getSinglePassLightBatchSize();
            if (lights.size() == 0) {
                updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, 0, isLightCullStageDraw);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            } else {
                while (nbRenderedLights < lights.size()) {
                    nbRenderedLights = updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, nbRenderedLights, isLightCullStageDraw);
                    renderer.setShader(shader);
                    renderMeshFromGeometry(renderer, geometry);
                }
            }
        }
    }
}
