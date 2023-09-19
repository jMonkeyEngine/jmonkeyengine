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
import com.jme3.renderer.pipeline.Deferred;
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
import java.util.EnumSet;

public final class DeferredSinglePassLightingLogic extends DefaultTechniqueDefLogic {

    private static final String DEFINE_DEFERRED_SINGLE_PASS_LIGHTING = "DEFERRED_SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_USE_TEXTURE_PACK_MODE = "USE_TEXTURE_PACK_MODE";
    private static final String DEFINE_PACK_NB_LIGHTS = "PACK_NB_LIGHTS";
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

    static {
        ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private int nbLightsDefineId;
    private int packNbLightsDefineId;
    private int packTextureModeDefineId;

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
        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
    }

    protected int updateLightListPackToTexture(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex, boolean isLightCullStageDraw) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");

        if (startIndex != 0 || isLightCullStageDraw) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList, true, ambientLightColor));
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
        if(geometry.getUserData(Deferred.S_LIGHT_CULL_DRAW_STAGE) != null){
            isLightCullStageDraw = geometry.getUserData(Deferred.S_LIGHT_CULL_DRAW_STAGE);
        }
        // todo:这里的一种优化方式是:
        // todo:使用uniform数组存储的方案:
        // todo:首先将lights分成DirectionalLights,PointLights,SpotLights三种列表
        // todo:然后首先绘制DirectionalLights和SpotLights,这两个都是用Rect绘制,一次提交SingleBatch指定的数量灯光
        // todo:然后使用Sphere(开启FrontFace剔除,避免相机进入球内光源着色消失)绘制点光源,根据SingleBatch一次绘制一组Sphere实例化

        // todo:另一种做法是,将lights分为全屏和非全屏,DirectionalLight和SpotLight按全屏处理,然后便利所有PointLight中属于无效半径的也归为全屏
        // todo:然后把剩下的PointLights作为非全屏光源,然后纹理中一次性存储所有光源信息,内存存放按全屏在前非全屏在后
        // todo:然后发起一个RectPass,在一个DC内绘制完所有全屏光,更新light_offset,然后使用SingleBatch预创建的SphereInstance绘制剩下的非全屏光
        // todo:这里将采用第二种方法

        // todo:关于light probes(暂时实现基于preCompute light probe),根据当前视锥体可见范围获取到light probe grid,按light probe grid执行multi pass
        // todo:关于reflection probes,使用textureArray(八面体投影,带mipmap),收集相机可见范围内的reflection probes,并限制当前视锥体内只能有多少个reflection probes
        if(bUseTexturePackMode){
            Uniform lightCount = shader.getUniform("g_LightCount");
            int count = lights.size();
            lightCount.setValue(VarType.Int, count);
            while (nbRenderedLights < count) {
                // todo:采用第二种方法优化deferred,则这里使用当前类的geometrys(rect,sphere)进行绘制,而不使用这个传递进来的geometry(或者在外部传递两个geometry,一个rect一个sphereinstance)
                nbRenderedLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, nbRenderedLights, isLightCullStageDraw);
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
