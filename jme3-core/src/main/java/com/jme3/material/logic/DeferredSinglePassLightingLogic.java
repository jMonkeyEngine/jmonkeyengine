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

import com.jme3.asset.AssetManager;
import com.jme3.light.*;
import com.jme3.material.Material.BindUnits;
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

/**
 * DeferredShading.
 * <p>
 * This is a standard DeferredShading, without any optimizations, but it should be faster than Forward when there are complex scenes and lots of lights. It is suitable for deferred rendering scenarios with simple requirements. There are some ways to optimize it:<br/>
 * A few involved drawing proxy geometry that approximated the bounds of each type of light and evaluating lighting by sampling from the G buffer for each fragment that the geometry touched. This can be implemented with varying complexity of proxy geometry. <br/>
 * Some implementations just used billboarded quads with enough width and height in world space to approximate the bounds of the area that the light influences. For instance a point light would just have a quad with a width and height the same as the lights radius of influence. <br/>
 * Other implementations actually draw 3D proxy geometry like spheres for point lights and cones for spotlights.<br/>
 *
 * These implementations have the issue that they require many additional samples of the G buffer. Each light still needs to sample the G buffer for each texture that it has; in my case 4 textures. So each fragment of the G buffer gets sampled 4 * the number of lights affecting that fragment. <br/>
 * Additionally these techniques incur a lot of overdraw since many of the proxy geometry objects will overlap and cannot be culled most of the time.
 * @author JohnKkk
 */
public final class DeferredSinglePassLightingLogic extends DefaultTechniqueDefLogic {
    
    private final static String LIGHT_CULL_DRAW_STAGE = "Light_Cull_Draw_Stage";
    private static final String DEFINE_DEFERRED_SINGLE_PASS_LIGHTING = "DEFERRED_SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_USE_TEXTURE_PACK_MODE = "USE_TEXTURE_PACK_MODE";
    private static final String DEFINE_USE_AMBIENT_LIGHT = "USE_AMBIENT_LIGHT";
    private static final String DEFINE_PACK_NB_LIGHTS = "PACK_NB_LIGHTS";
    private static final String DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES = "NB_SKY_LIGHT_AND_REFLECTION_PROBES";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();
    
    private final Texture2D[] lightTextures = new Texture2D[3];
    private final ImageRaster[] lightTexRasters = new ImageRaster[3];
    private final boolean useLightTextures;
    private int lightNum;
    private boolean useAmbientLight;
    private final ColorRGBA ambientColor = new ColorRGBA(0, 0, 0, 1);
    private final List<LightProbe> probes = new ArrayList<>(3);

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private int nbLightsDefineId;
    private int packNbLightsDefineId;
    private int textureModeDefine;
    private final int probeDefine;
    private final int ambientDefines;
    
    public DeferredSinglePassLightingLogic(TechniqueDef techniqueDef) {
        this(techniqueDef, true);
    }
    public DeferredSinglePassLightingLogic(TechniqueDef techniqueDef, boolean useLightTextures) {
        super(techniqueDef);
        this.useLightTextures = useLightTextures;
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_DEFERRED_SINGLE_PASS_LIGHTING, VarType.Boolean);
        if (this.useLightTextures) {
            packNbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_PACK_NB_LIGHTS, VarType.Int);
            textureModeDefine = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_TEXTURE_PACK_MODE, VarType.Boolean);
            prepareLightData(1024);
        } else {
            nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
        }
        probeDefine = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES, VarType.Int);
        ambientDefines = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_AMBIENT_LIGHT, VarType.Boolean);
    }

    private void cleanupLightData() {
        if (lightTextures[0] != null) {
            lightTextures[0].getImage().dispose();
        }
        if (lightTextures[1] != null) {
            lightTextures[1].getImage().dispose();
        }
        if (lightTextures[2] != null) {
            lightTextures[2].getImage().dispose();
        }
    }

    /**
     * Creates textures to accomodate light data.
     * <p>
     * Currently, a large amount of light information is stored in textures, divided into three texture1d,
     * lightData[0] stores lightColor (rgb stores lightColor, a stores lightType), lightData[1] stores lightPosition +
     * invRange/lightDir, lightData[2] stores dir and spotAngleCos about SpotLight.
     * 
     * @param lightNum By preallocating texture memory for the known number of lights, dynamic reallocation at runtime can be prevented.
     */
    private void prepareLightData(int lightNum) {
        this.lightNum = lightNum;
        // 1d texture
        lightTextures[0] = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightTextures[0].setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightTextures[0].setMagFilter(Texture.MagFilter.Nearest);
        lightTextures[0].setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage = new Image(Image.Format.RGBA32F, lightNum, 1, data, null, ColorSpace.Linear);
        lightTextures[0].setImage(convertedImage);
        lightTextures[0].getImage().setMipmapsGenerated(false);
        lightTexRasters[0] = ImageRaster.create(lightTextures[0].getImage());

        lightTextures[1] = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightTextures[1].setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightTextures[1].setMagFilter(Texture.MagFilter.Nearest);
        lightTextures[1].setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data2 = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage2 = new Image(Image.Format.RGBA32F, lightNum, 1, data2, null, ColorSpace.Linear);
        lightTextures[1].setImage(convertedImage2);
        lightTextures[1].getImage().setMipmapsGenerated(false);
        lightTexRasters[1] = ImageRaster.create(lightTextures[1].getImage());

        lightTextures[2] = new Texture2D(lightNum, 1, Image.Format.RGBA32F);
        lightTextures[2].setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightTextures[2].setMagFilter(Texture.MagFilter.Nearest);
        lightTextures[2].setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer data3 = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightNum);
        Image convertedImage3 = new Image(Image.Format.RGBA32F, lightNum, 1, data3, null, ColorSpace.Linear);
        lightTextures[2].setImage(convertedImage3);
        lightTextures[2].getImage().setMipmapsGenerated(false);
        lightTexRasters[2] = ImageRaster.create(lightTextures[2].getImage());
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        if(useLightTextures){
            defines.set(packNbLightsDefineId, this.lightNum);
            defines.set(textureModeDefine, true);
        }
        else{
            defines.set(nbLightsDefineId, renderManager.getSinglePassLightBatchSize() * 3);
        }
        defines.set(singlePassLightingDefineId, true);
        //TODO here we have a problem, this is called once before render, so the define will be set for all passes (in case we have more than NB_LIGHTS lights)
        //Though the second pass should not render IBL as it is taken care of on first pass like ambient light in phong lighting.
        //We cannot change the define between passes and the old technique, and for some reason the code fails on mac (renders nothing).
        if(lights != null) {
            useAmbientLight = SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientColor, probes, false);
            defines.set(probeDefine, probes.size());
            defines.set(ambientDefines, useAmbientLight);
        }
        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
    }

    /**
     * Packs light data into textures.
     * <p>
     * lightData[0]:<br>
     * - rgb stores lightColor<br>
     * - a stores lightTypeId<br>
     * lightData[1]:<br>
     * - directionalLightDirection<br>
     * - pointLightPosition + invRadius<br>
     * - spotLightPosition + invRadius<br>
     * lightData[2]:<br>
     * - spotLightDirection<br>
     * 
     * @param shader               Current shader used for rendering (a global shader)
     * @param g                    Current geometry used for rendering (a rect)
     * @param lightList            Information about all visible lights this frame
     * @param numLights            numLights
     * @param rm                   renderManager
     * @param startIndex           first light start offset
     * @param isLightCullStageDraw cullMode
     * @param lastTexUnit          lastTexUnit the index of the most recently-used texture unit
     * @return the next starting index in the LightList
     */
    private int updateLightListPackToTexture(Shader shader, Geometry g, LightList lightList, int numLights,
            RenderManager rm, int startIndex, boolean isLightCullStageDraw, int lastTexUnit) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform ambClr = shader.getUniform("g_AmbientLightColor");

        //skyLightAndReflectionProbes.clear();
        if (startIndex != 0 || isLightCullStageDraw) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambClr.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            //extractSkyLightAndReflectionProbes(lightList,true);
            ambClr.setValue(VarType.Vector4, ambientColor);
        }

        // render skyLights and reflectionProbes
        if(!probes.isEmpty()){
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

            LightProbe skyLight = probes.get(0);
            lastTexUnit = SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(
                    rm, lastTexUnit, skyLightData, shCoeffs, reflectionProbePemMap, skyLight);
            if (probes.size() > 1) {
                skyLight = probes.get(1);
                lastTexUnit = SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(
                        rm, lastTexUnit, skyLightData2, shCoeffs2, reflectionProbePemMap2, skyLight);
            }
            if (probes.size() > 2) {
                skyLight = probes.get(2);
                SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(
                        rm, lastTexUnit, skyLightData3, shCoeffs3, reflectionProbePemMap3, skyLight);
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
            lightTexRasters[0].setPixel(curIndex, 0, temp);
            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    temp.r = dir.getX();
                    temp.g = dir.getY();
                    temp.b = dir.getZ();
                    temp.a = -1;
                    lightTexRasters[1].setPixel(curIndex, 0, temp);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    temp.r = pos.getX();
                    temp.g = pos.getY();
                    temp.b = pos.getZ();
                    temp.a = invRadius;
                    lightTexRasters[1].setPixel(curIndex, 0, temp);
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
                    lightTexRasters[1].setPixel(curIndex, 0, temp);

                    //We transform the spot direction in view space here to save 5 varying later in the lighting shader
                    //one vec4 less and a vec4 that becomes a vec3
                    //the downside is that spotAngleCos decoding happens now in the frag shader.
                    temp.r = dir2.getX();
                    temp.g = dir2.getY();
                    temp.b = dir2.getZ();
                    temp.a = spotAngleCos;
                    lightTexRasters[2].setPixel(curIndex, 0, temp);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
        }
        //temp.r = temp.g = temp.b = temp.a = 0;
        // Since the drawing is sent within the loop branch, and actually before the actual glSwapBuffers, the gl commands
        // actually reside at the graphics driver level. So in order to correctly branch within the loop, the size must be
        // fixed here (while filling the number of light sources).
        //ColorRGBA temp2 = vars.color2;
        //for(;curIndex < this.lightNum;curIndex++){
        //    temp2 = lightTexRasters[0].getPixel(curIndex, 0);
        //    if(temp2.r == 0 && temp2.g == 0 && temp2.b == 0 && temp2.a == 0)break;
        //    lightTexRasters[0].setPixel(curIndex, 0, temp);
        //    lightTexRasters[1].setPixel(curIndex, 0, temp);
        //    lightTexRasters[2].setPixel(curIndex, 0, temp);
        //}
        vars.release();
        lightTextures[0].getImage().setUpdateNeeded();
        lightTextures[1].getImage().setUpdateNeeded();
        lightTextures[2].getImage().setUpdateNeeded();
        g.getMaterial().setTexture("LightPackData1", lightTextures[0]);
        g.getMaterial().setTexture("LightPackData2", lightTextures[1]);
        g.getMaterial().setTexture("LightPackData3", lightTextures[2]);
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
    private int updateLightListUniforms(Shader shader, Geometry g, LightList lightList, int numLights,
            RenderManager rm, int startIndex, boolean isLightCullStageDraw) {
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
            ambientColor.setValue(VarType.Vector4, ambientColor);
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
    public void render(RenderManager renderManager, Shader shader, Geometry geometry,
            LightList lights, BindUnits lastBindUnit) {
        int numLights = 0;
        Renderer renderer = renderManager.getRenderer();
        boolean isLightCullStageDraw = false;
        if(geometry.getUserData(LIGHT_CULL_DRAW_STAGE) != null){
            isLightCullStageDraw = geometry.getUserData(LIGHT_CULL_DRAW_STAGE);
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
        if(useLightTextures){
            if (lightNum != renderManager.getMaxDeferredShadingLights()) {
                cleanupLightData();
                prepareLightData(renderManager.getMaxDeferredShadingLights());
            }
            // todo:Currently, this texturePackMode is only suitable for scenes where there are a large number of light sources per frame. The number of light sources is submitted to the texture all at once, so lightNum can be pre-allocated, but light source information can also be submitted to the texture all at once here, and then drawn in multiple passes (drawing each time by the specified singlePassLightBatchSize)
            useAmbientLight = SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientColor, probes, true);
            int count = lights.size();
            // FIXME:Setting uniform variables this way will take effect immediately in the current frame, however lightData is set through geometry.getMaterial().setTexture(XXX) which will take effect next frame, so here I changed to use geometry.getMaterial().setParam() uniformly to update all parameters, to keep the frequency consistent.
//            Uniform lightCount = shader.getUniform("g_LightCount");
//            lightCount.setValue(VarType.Int, count);
//            lightCount.setValue(VarType.Int, this.lightNum);
            geometry.getMaterial().setInt("NBLight", count);
            if(count == 0){
                numLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, numLights, isLightCullStageDraw, lastBindUnit.textureUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            } else while (numLights < count) {
                // todo:Optimize deferred using the second method, here use the geometrys (rect, sphere) of the current class for drawing, instead of using the geometry passed in (or pass two geometry externally, one rect one sphereinstance)
                numLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, numLights, isLightCullStageDraw, lastBindUnit.textureUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        } else {
            int batchSize = renderManager.getSinglePassLightBatchSize();
            if (lights.size() == 0) {
                updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, 0, isLightCullStageDraw);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            } else while (numLights < lights.size()) {
                numLights = updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, numLights, isLightCullStageDraw);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
    }
    
}
