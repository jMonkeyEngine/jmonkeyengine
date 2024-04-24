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
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
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
 * Tile-based DeferredShading.
 * <p>
 * Tile-based deferred shading utilizes the overhead of one-sample MRT and efficient culling of lights to accelerate deferred rendering. <br/>
 * The implementation details here separate the data into three main parts:<br/>
 * <b>1.Global Light</b> List contains light properties(lightData1+lightData2+lightData3).<br/>
 * <b>2.The light index list</b> contains light indices to the global light list(lightsIndexData).<br/>
 * <b>3.Light grid</b> contains an offset and size of the light list for each tile(lightsDecodeData).<br/>
 * <a href="https://www.digipen.edu/sites/default/files/public/docs/theses/denis-ishmukhametov-master-of-science-in-computer-science-thesis-efficient-tile-based-deferred-shading-pipeline.pdf">https://www.digipen.edu/sites/default/files/public/docs/theses/denis-ishmukhametov-master-of-science-in-computer-science-thesis-efficient-tile-based-deferred-shading-pipeline.pdf</a>
 * @author JohnKkk
 */
public class TileBasedDeferredSinglePassLightingLogic extends DefaultTechniqueDefLogic{
    
    private final static String LIGHT_CULL_DRAW_STAGE = "Light_Cull_Draw_Stage";
    private final static String TILE_SIZE = "g_TileSize";
    private final static String TILE_WIDTH = "g_WidthTile";
    private final static String TILE_HEIGHT = "g_HeightTile";
    private static final String DEFINE_TILE_BASED_DEFERRED_SINGLE_PASS_LIGHTING = "TILE_BASED_DEFERRED_SINGLE_PASS_LIGHTING";
    private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
    private static final String DEFINE_USE_AMBIENT_LIGHT = "USE_AMBIENT_LIGHT";
    private static final String DEFINE_USE_TEXTURE_PACK_MODE = "USE_TEXTURE_PACK_MODE";
    private static final String DEFINE_PACK_NB_LIGHTS = "PACK_NB_LIGHTS";
    private static final String DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES = "NB_SKY_LIGHT_AND_REFLECTION_PROBES";
    // Light source retrieval encoded in ppx of Tile
    private static final String TILE_LIGHT_DECODE = "TileLightDecode";
    // Light source id encoded in ppx of Tile
    private static final String TILE_LIGHT_INDEX = "TileLightIndex";
    // Sampling offset size in Tile
    private static final String TILE_LIGHT_OFFSET_SIZE = "g_TileLightOffsetSize";
    private static final RenderState ADDITIVE_LIGHT = new RenderState();
    private boolean packAsTextures = true;
    // Use textures to store large amounts of light data at once, avoiding multiple draw calls
    private Texture2D lightData1;
    private Texture2D lightData2;
    private Texture2D lightData3;
    private ImageRaster lightDataUpdateIO1;
    private ImageRaster lightDataUpdateIO2;
    private ImageRaster lightDataUpdateIO3;
    private int lightNum;
    private boolean useAmbientLight;

    private final ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);
    final private List<LightProbe> skyLightAndReflectionProbes = new ArrayList<>(3);

    static {
        ADDITIVE_LIGHT.setBlendMode(RenderState.BlendMode.AlphaAdditive);
        ADDITIVE_LIGHT.setDepthWrite(false);
    }

    private final int singlePassLightingDefineId;
    private int nbLightsDefineId;
    private int packNbLightsDefineId;
    private int packTextureModeDefineId;
    private final int nbSkyLightAndReflectionProbesDefineId;
    private final int useAmbientLightDefineId;



    // temp var
    private Matrix4f vp;
    private float camLeftCoeff = -1;
    private float camTopCoeff = -1;
    private float viewPortWidth = -1;
    private float viewPortHeight = -1;
    private final float[] matArray1 = new float[16];
    private final Vector3f tempVec3 = new Vector3f();
    private final Vector3f tempVec3_2 = new Vector3f();
    private final Vector4f tempVec4 = new Vector4f();
    private final Vector4f tempvec4_2 = new Vector4f();
    private final Vector4f tempVec4_3 = new Vector4f();
    private final Vector4f camUp = new Vector4f();
    private final Vector4f camLeft = new Vector4f();
    private final Vector4f lightLeft = new Vector4f();
    private final Vector4f lightUp = new Vector4f();
    private final Vector4f lightCenter = new Vector4f();
    private final LightFrustum _lightFrustum = new LightFrustum(0, 0, 0, 0);

    // tile info
    private TileInfoProvider tileProvider;
    private TiledRenderGrid tileInfo;

    // tile light ids
    private ArrayList<ArrayList<Integer>> tiles;
    // lightsIndex per tile
    private ArrayList<Integer> lightsIndex;
    // lightsDecode per tile
    private ArrayList<Integer> lightsDecode;
    private Texture2D lightsIndexData;
    private Texture2D lightsDecodeData;
    private ImageRaster lightsIndexDataUpdateIO;
    private ImageRaster lightsDecodeDataUpdateIO;
    private int lightIndexWidth;

    /**
     * LightFrustum
     * <p>This is an internal helper class to determine the on-screen Rect for the current PointLight. It's called Frustum because it may be expanded to 3D Cluster in space in the future.</p>
     * @author JohnKkk
     */
    private static class LightFrustum {
        float left;
        float right;
        float top;
        float bottom;

        public LightFrustum(float left, float right, float top, float bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    public TileBasedDeferredSinglePassLightingLogic(TechniqueDef techniqueDef, TileInfoProvider tileProvider) {
        super(techniqueDef);
        this.tileProvider = tileProvider;
        lightIndexWidth = (int)(Math.floor(Math.sqrt(1024)));
        singlePassLightingDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_TILE_BASED_DEFERRED_SINGLE_PASS_LIGHTING, VarType.Boolean);
        if(packAsTextures){
            packNbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_PACK_NB_LIGHTS, VarType.Int);
            packTextureModeDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_TEXTURE_PACK_MODE, VarType.Boolean);
            prepareLightData(1024);
        }
        else{
            nbLightsDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
        }
        nbSkyLightAndReflectionProbesDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_NB_SKY_LIGHT_AND_REFLECTION_PROBES, VarType.Int);
        useAmbientLightDefineId = techniqueDef.addShaderUnmappedDefine(DEFINE_USE_AMBIENT_LIGHT, VarType.Boolean);
    }

    /**
     * Clear the lightsIndex cache texture for rebuilding.
     */
    private void cleanupLightsIndexTexture(){
        if(lightsIndexData != null){
            lightsIndexData.getImage().dispose();
            lightsIndexData = null;
        }
    }

    /**
     * Rebuild the lightsIndex cache texture.
     * <p>The square root of the total number of lights that need to be queried. Note that the total number of lights to be queried is different from the total number of visible lights in the current view frustum. One tile may contain 100 lights, another tile may also contain 100 lights, then the total lights to query is 200, while the total lights in the current view frustum may only be 100!</p>
     * @param lightIndexWidth
     */
    private void createLightsIndexTexture(int lightIndexWidth){
        this.lightIndexWidth = lightIndexWidth;
        lightsIndexData = new Texture2D(lightIndexWidth, lightIndexWidth, Image.Format.RGBA32F);
        lightsIndexData.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightsIndexData.setMagFilter(Texture.MagFilter.Nearest);
        lightsIndexData.setWrap(Texture.WrapMode.EdgeClamp);
        ByteBuffer dataT = BufferUtils.createByteBuffer( (int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * lightIndexWidth * lightIndexWidth);
        Image convertedImageT = new Image(Image.Format.RGBA32F, lightIndexWidth, lightIndexWidth, dataT, null, ColorSpace.Linear);
        lightsIndexData.setImage(convertedImageT);
        lightsIndexData.getImage().setMipmapsGenerated(false);
        lightsIndexDataUpdateIO = ImageRaster.create(lightsIndexData.getImage());
    }

    /**
     * Clear the lightsDecode cache texture for rebuilding.
     */
    private void cleanupLightsDecodeTexture(){
        if(lightsDecodeData != null){
            lightsDecodeData.getImage().dispose();
            lightsDecodeData = null;
        }
    }

    /**
     * Clear the lightsIndex and lightsDecode cache texture for rebuilding.
     */
    private void cleanupTileTexture(){
        cleanupLightsIndexTexture();
        cleanupLightsDecodeTexture();
    }

    /**
     * Reallocate texture memory for the query texture for the specified tile size.
     * <p>
     * This detection is performed every frame to ensure the cache textures
     * lightsDecode and lightsIndex are recreated when screen changes or tiles change dynamically.
     * 
     * @param tileWidth Number of tiles in horizontal direction
     * @param tileHeight Number of tiles in vertical direction
     * @param tileNum tileWidth * tileHeight
     */
    private void reset(boolean gridDemensionsChanged, boolean numTilesChanged) {
        if (gridDemensionsChanged) {
            cleanupTileTexture();
            createLightsIndexTexture(lightIndexWidth);
            lightsDecodeData = new Texture2D(tileInfo.getGridHeight(), tileInfo.getGridHeight(), Image.Format.RGBA32F);
            lightsDecodeData.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            lightsDecodeData.setMagFilter(Texture.MagFilter.Nearest);
            lightsDecodeData.setWrap(Texture.WrapMode.EdgeClamp);
            ByteBuffer dataU = BufferUtils.createByteBuffer((int)Math.ceil(Image.Format.RGBA32F.getBitsPerPixel() / 8.0) * tileInfo.getNumTiles());
            Image convertedImageU = new Image(Image.Format.RGBA32F, tileInfo.getGridWidth(), tileInfo.getGridHeight(), dataU, null, ColorSpace.Linear);
            lightsDecodeData.setImage(convertedImageU);
            lightsDecodeData.getImage().setMipmapsGenerated(false);
            lightsDecodeDataUpdateIO = ImageRaster.create(lightsDecodeData.getImage());
        }
        if (numTilesChanged) {
            tiles = new ArrayList<>();
            for (int i = 0, n = tileInfo.getNumTiles(); i < n; i++) {
                tiles.add(new ArrayList<>());
            }
        } else for (ArrayList l : tiles) {
            l.clear();
        }
        if (lightsDecode == null) {
            lightsDecode = new ArrayList<>();
        } else {
            lightsDecode.clear();
        }
        if (lightsIndex == null) {
            lightsIndex = new ArrayList<>();
        } else {
            lightsIndex.clear();
        }
    }

    /**
     * Some lights affect the whole screen, such as DirectionalLight, PointLight with infinite radius, and SpotLight. These lights will affect all tiles, so they are passed to all tiles in this method.
     * @param tileWidth
     * @param tileHeight
     * @param tileNum
     * @param tiles
     * @param lightId
     */
    private void tilesFullUpdate(int tileWidth, int tileHeight, int tileNum, ArrayList<ArrayList<Integer>> tiles, int lightId){
        // calc tiles
        int tileId = 0;
        for(int l = 0;l < tileWidth;l++){
            for(int b = 0;b < tileHeight;b++){
                tileId = l + b * tileWidth;
                if(tileId >= 0 && tileId < tileNum){
                    tiles.get(tileId).add(lightId);
                }
            }
        }
    }

    /**
     * Assigns the given light source to affected tiles based on its screen space view frustum.
     * @param tileSize
     * @param tileWidth
     * @param tileHeight
     * @param tileNum
     * @param tiles
     * @param lightFrustum screen space lightFrustum(rect)
     * @param lightId targetLightId
     */
    private void tilesUpdate(int tileSize, int tileWidth, int tileHeight, int tileNum, ArrayList<ArrayList<Integer>> tiles, LightFrustum lightFrustum, int lightId){
        // tile built on
        //⬆
        //|
        //|
        //----------➡
        // so,Using pixel screen precision, stepping top-right
        int tileLeft = (int) Math.max(Math.floor(lightFrustum.left / tileSize), 0);
        int tileRight = (int) Math.min(Math.ceil(lightFrustum.right / tileSize), tileWidth);
        int tileBottom = (int) Math.max(Math.floor(lightFrustum.bottom / tileSize), 0);
        int tileTop = (int) Math.min(Math.ceil(lightFrustum.top / tileSize), tileHeight);

        // calc tiles
        int tileId = 0;
        for(int l = tileLeft;l < tileRight;l++){
            for(int b = tileBottom;b < tileTop;b++){
                tileId = l + b * tileWidth;
                if(tileId >= 0 && tileId < tileNum){
                    tiles.get(tileId).add(lightId);
                }
            }
        }
    }

    /**
     * Keep the method but don't use it.<br/>
     * @param camera
     * @param light
     * @return
     */
    @Deprecated
    private final LightFrustum lightClip(Camera camera, Light light){
        if(light instanceof PointLight){
            PointLight pl = (PointLight)light;
            float r = pl.getRadius();
            if(r <= 0)return null;
            camera.getUp().add(pl.getPosition(), tempVec3);
            tempVec3.multLocal(r);
            camera.getScreenCoordinates(tempVec3, tempVec3_2);
            float t = tempVec3_2.y;

            camera.getUp().mult(-1, tempVec3);
            tempVec3.add(pl.getPosition(), tempVec3_2);
            tempVec3_2.multLocal(r);
            camera.getScreenCoordinates(tempVec3_2, tempVec3);
            float b = tempVec3.y;

            camera.getLeft().add(pl.getPosition(), tempVec3_2);
            tempVec3_2.multLocal(r);
            camera.getScreenCoordinates(tempVec3_2, tempVec3);
            float l = tempVec3.x;

            camera.getLeft().mult(-1, tempVec3);
            tempVec3.add(pl.getPosition(), tempVec3_2);
            camera.getScreenCoordinates(tempVec3_2, tempVec3);
            r = tempVec3.x;
            _lightFrustum.left = l;
            _lightFrustum.right = r;
            _lightFrustum.bottom = b;
            _lightFrustum.top = t;
            return _lightFrustum;
        }
        return null;
    }

    /**
     * Find out the view frustum (currently 2D so it's Rect) of the given light source in screen space. Once the screen-space view frustum of the light is determined, it can be assigned to associated tiles so that the tiles can use it for lighting shading.
     * @param light targetLight
     * @return Returns the screen space view frustum of the light, used to find associated tiles
     */
    private final LightFrustum lightClip(Light light){
        // todo:Currently, only point light sources are processed
        if(light instanceof PointLight){
            PointLight pl = (PointLight)light;
            float r = pl.getRadius();
            if(r <= 0)return null;
            float lr = r * camLeftCoeff;
            float tr = r * camTopCoeff;
            tempVec4.set(pl.getPosition().x, pl.getPosition().y, pl.getPosition().z, 1.0f);
            Vector4f center = tempVec4;
            tempvec4_2.w = 1.0f;
            tempVec4_3.w = 1.0f;

            camLeft.mult(lr, tempvec4_2);
            tempvec4_2.addLocal(center);
            Vector4f lightFrustumLeft = tempvec4_2;
            lightFrustumLeft.w = 1.0f;

            camUp.mult(tr, tempVec4_3);
            tempVec4_3.addLocal(center);
            Vector4f lightFrustumUp = tempVec4_3;
            lightFrustumUp.w = 1.0f;

            vp.mult(lightFrustumLeft, lightLeft);
            vp.mult(lightFrustumUp, lightUp);
            vp.mult(center, lightCenter);

            lightLeft.x /= lightLeft.w;
            lightLeft.y /= lightLeft.w;

            lightUp.x /= lightUp.w;
            lightUp.y /= lightUp.w;

            lightCenter.x /= lightCenter.w;
            lightCenter.y /= lightCenter.w;

            lightLeft.x = viewPortWidth * (1.0f + lightLeft.x);
            lightUp.x = viewPortWidth * (1.0f + lightUp.x);
            lightCenter.x = viewPortWidth * (1.0f + lightCenter.x);

            lightLeft.y = viewPortHeight * (1.0f - lightLeft.y);
            lightUp.y = viewPortHeight * (1.0f - lightUp.y);
            lightCenter.y = viewPortHeight * (1.0f - lightCenter.y);

            // light frustum rect
            float lw = Math.abs(lightLeft.x - lightCenter.x);
            float lh = Math.abs(lightCenter.y - lightUp.y);
            float left = -1.0f, btm = -1.0f;
            if(lightCenter.z < -lightCenter.w){
                left = -lightCenter.x - lw;
                btm = -lightCenter.y + lh;
            }
            else{
                left = lightCenter.x - lw;
                btm = lightCenter.y + lh;
            }
            float bottom = viewPortHeight * 2.0f - btm;
            _lightFrustum.left = left;
            _lightFrustum.right = lw * 2.0f + left;
            _lightFrustum.top = lh * 2.0f + bottom;
            _lightFrustum.bottom = bottom;
            return _lightFrustum;
        }
        return null;
    }

    /**
     * Encode light information for each tile, which contains two parts:<br/>
     * 1. The light offset and total number of lights to draw for each tile<br/>
     * 2. The encoded list of light ids<br/>
     * This method is called every frame, and the texture is dynamically sized (although jme3's encapsulation of dynamic texture sizes is a bit problematic, so it's currently a fixed-size dynamic texture).
     * @param tileNum    Total number of tiles
     * @param tiles      List of light ids for each tile
     * @param tileWidth  Number of tiles in horizontal direction
     * @param tileHeight Number of tiles in vertical direction
     * @param shader     Current shader used for rendering (a global shader)
     * @param g          Current geometry used for rendering (a rect)
     * @param lights     Information about all visible lights this frame
     */
    private void tileLightDecode(int tileNum, ArrayList<ArrayList<Integer>> tiles, int tileWidth, int tileHeight, Shader shader, Geometry g, LightList lights){
        int len = lights.size();

        ArrayList<Integer> tile = null;
        for(int i = 0, offset = 0;i < tileNum;i++){
            tile = tiles.get(i);
            len = tile.size();
            for(int l = 0;l < len;l++){
                lightsIndex.add(tile.get(l));
                lightsIndex.add(0);
                lightsIndex.add(0);
            }
            // u offset
            lightsDecode.add(offset);
            // tile light num
            lightsDecode.add(len);
            // Add in next step
            lightsDecode.add(-1);
            offset += len;
        }
        // Calculate light sampling size
        int lightIndexWidth = (int) Math.ceil(Math.sqrt(lightsIndex.size() / 3));
        if(lightIndexWidth > this.lightIndexWidth){
            // recreate
            cleanupLightsIndexTexture();
            // Expanding the texture size by 1.5 times can avoid the flickering issue caused by repeatedly allocating new textures due to insufficient size in consecutive frames.
            createLightsIndexTexture((int) (lightIndexWidth * 1.5));
        }
        else{
            // todo:Due to the unknown dynamic texture size causing tile flickering, the current fixed texture size is forced to be used each time here.
            // todo:Adjust to dynamic texture size after finding the cause later, otherwise a lot of padding data needs to be filled each time.
            lightIndexWidth = this.lightIndexWidth;
        }
//        else{
//            lightIndexWidth = this.lightIndexWidth;
//        }
//        int _lightIndexWidth = (int) Math.ceil(lightIndexWidth / 3);
        // updateData
        Uniform tileLightOffsetSizeUniform = shader.getUniform(TILE_LIGHT_OFFSET_SIZE);
        tileLightOffsetSizeUniform.setValue(VarType.Int, lightIndexWidth);

        // padding
        for(int i = lightsIndex.size(), size = lightIndexWidth * lightIndexWidth * 3;i < size;i++){
            lightsIndex.add(-1);
        }

        // Normalize the light uv of each tile to the light size range
        for(int i = 0, size = lightsDecode.size();i < size;i+=3){
            // The b component stores the v offset
            lightsDecode.set(i + 2, lightsDecode.get(i) / lightIndexWidth);
            lightsDecode.set(i, lightsDecode.get(i) % lightIndexWidth);
        }
        // updateData
        TempVars vars = TempVars.get();
        ColorRGBA temp = vars.color;
        temp.b = temp.g = 0.0f;
        temp.a = 1.0f;
        for(int i = 0, size = lightIndexWidth;i < size;i++){
            for(int j = 0, size2 = lightIndexWidth;j < size2;j++){
                temp.r = lightsIndex.get((j + i * lightIndexWidth) * 3);
                temp.g = 0.0f;
                lightsIndexDataUpdateIO.setPixel(j, i, temp);
            }
        }
        // todo:Due to the unknown dynamic texture size causing tile flickering, the current fixed texture size is forced to be used each time here.
        // todo:Adjust to dynamic texture size after finding the cause later, otherwise a lot of padding data needs to be filled each time.
//        lightsIndexData.getImage().setWidth(lightIndexWidth);
//        lightsIndexData.getImage().setHeight(lightIndexWidth);
//        for(int i = 0, x = 0, y = 0;i < lightsIndex.size();i+=3){
//            temp.r = lightsIndex.get(i);
//            temp.g = 0.0f;
//            lightsIndexDataUpdateIO.setPixel(x++, y, temp);
//            if(x >= this.lightIndexWidth){
//                x = 0;
//                y++;
//            }
//        }
        for(int i = 0;i < tileHeight;i++){
            for(int j = 0;j < tileWidth;j++){
                temp.r = lightsDecode.get((j + i * tileWidth) * 3);
                temp.g = lightsDecode.get((j + i * tileWidth) * 3 + 1);
                temp.b = lightsDecode.get((j + i * tileWidth) * 3 + 2);
                lightsDecodeDataUpdateIO.setPixel(j, i, temp);
            }
        }
        vars.release();

        lightsIndexData.getImage().setUpdateNeeded();
        lightsDecodeData.getImage().setUpdateNeeded();
        g.getMaterial().setTexture(TILE_LIGHT_INDEX, lightsIndexData);
        g.getMaterial().setTexture(TILE_LIGHT_DECODE, lightsDecodeData);
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

    /**
     * Try reallocating textures to accommodate enough light data.
     * <p>Currently, a large amount of light information is stored in textures, divided into three texture1d, <br/>
     * lightData1 stores lightColor (rgb stores lightColor, a stores lightType), lightData2 stores lightPosition + <br/>
     * invRange/lightDir, lightData3 stores dir and spotAngleCos about SpotLight.</p>
     * @param lightNum By preallocating texture memory for the known number of lights, dynamic reallocation at runtime can be prevented.
     */
    private void prepareLightData(int lightNum){
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

//        TempVars vars = TempVars.get();
//        ColorRGBA temp = vars.color;
//        temp.r = temp.g = temp.b = temp.a = 0;
//        // Since the drawing is sent within the loop branch, and actually before the actual glSwapBuffers, the gl commands actually reside at the graphics driver level. So in order to correctly branch within the loop, the size must be fixed here (while filling the number of light sources).
//        ColorRGBA temp2 = vars.color2;
//        for(int curIndex = 0;curIndex < this.lightNum;curIndex++){
//            temp2 = lightDataUpdateIO1.getPixel(curIndex, 0);
//            if(temp2.r == 0 && temp2.g == 0 && temp2.b == 0 && temp2.a == 0)break;
//            lightDataUpdateIO1.setPixel(curIndex, 0, temp);
//            lightDataUpdateIO2.setPixel(curIndex, 0, temp);
//            lightDataUpdateIO3.setPixel(curIndex, 0, temp);
//        }
//        vars.release();
    }

    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
                              EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        if(packAsTextures){
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
            useAmbientLight = SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientLightColor, skyLightAndReflectionProbes, false);
            defines.set(nbSkyLightAndReflectionProbesDefineId, skyLightAndReflectionProbes.size());
            defines.set(useAmbientLightDefineId, useAmbientLight);
        }
        return super.makeCurrent(assetManager, renderManager, rendererCaps, lights, defines);
    }

    /**
     * It seems the lighting information is encoded into 3 1D textures that are updated each frame with the currently visible lights:<br/>
     * lightData1:<br/>
     * - rgb stores lightColor<br/>
     * - a stores lightTypeId<br/>
     * lightData2:<br/>
     * - directionalLightDirection<br/>
     * - pointLightPosition + invRadius<br/>
     * - spotLightPosition + invRadius<br/>
     * lightData3:<br/>
     * - spotLightDirection<br/>
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
    protected int updateLightListPackToTexture(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex, boolean isLightCullStageDraw, int lastTexUnit) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return 0;
        }

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");

        if (startIndex != 0 || isLightCullStageDraw) {
            // apply additive blending for 2nd and future passes
            rm.getRenderer().applyRenderState(ADDITIVE_LIGHT);
            ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
        } else {
            ambientColor.setValue(VarType.Vector4, ambientLightColor);
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
        int skips = 0;
        ColorRGBA temp = vars.color;
        for (curIndex = startIndex; curIndex < endIndex && curIndex < lightList.size(); curIndex++) {
            Light l = lightList.get(curIndex);
            if (l.getType() == Light.Type.Ambient || l.getType() == Light.Type.Probe) {
                endIndex++;
                continue;
            }
            // check that the number of visible lights does not exceed the maximum
            if (curIndex >= lightNum) {
                throw new IllegalStateException("Number of lights exceeded specified maximum of "+lightNum+".");
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
//        ColorRGBA temp2 = vars.color2;
//        for(;curIndex < this.lightNum;curIndex++){
//            temp2 = lightDataUpdateIO1.getPixel(curIndex, 0);
//            if(temp2.r == 0 && temp2.g == 0 && temp2.b == 0 && temp2.a == 0)break;
//            lightDataUpdateIO1.setPixel(curIndex, 0, temp);
//            lightDataUpdateIO2.setPixel(curIndex, 0, temp);
//            lightDataUpdateIO3.setPixel(curIndex, 0, temp);
//        }
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
            ambientColor.setValue(VarType.Vector4, ambientLightColor);
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
    public void render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, BindUnits lastBindUnit) {
        int renderedLights = 0;
        Renderer renderer = renderManager.getRenderer();
        boolean isLightCullStageDraw = false;
        if(geometry.getUserData(LIGHT_CULL_DRAW_STAGE) != null){
            isLightCullStageDraw = geometry.getUserData(LIGHT_CULL_DRAW_STAGE);
        }
        if(packAsTextures){
            if(this.lightNum != renderManager.getMaxDeferredShadingLights()){
                cleanupLightData();
                prepareLightData(renderManager.getMaxDeferredShadingLights());
            }
            useAmbientLight = SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(lights, ambientLightColor, skyLightAndReflectionProbes, true);
            int count = lights.size();
            // FIXME:Setting uniform variables this way will take effect immediately in the current frame, however lightData is set through geometry.getMaterial().setTexture(XXX) which will take effect next frame, so here I changed to use geometry.getMaterial().setParam() uniformly to update all parameters, to keep the frequency consistent.
//            Uniform lightCount = shader.getUniform("g_LightCount");
//            lightCount.setValue(VarType.Int, count);
            // Divide lights into full screen lights and non-full screen lights. Currently only PointLights with radius are treated as non-full screen lights.
            // The lights passed in here must be PointLights with valid radii. Another approach is to fill tiles with infinite range Lights.
            if(count > 0){
                
                // get render grid info
                TiledRenderGrid src = tileProvider.getTiledRenderGrid();
                src.verify();
                boolean gridChanged = tileInfo.gridDemensionsDiffer(src);
                boolean numTilesChanged = tileInfo.numTilesDiffer(src);
                tileInfo.copyFrom(tileProvider.getTiledRenderGrid());
                shader.getUniform(TILE_SIZE).setValue(VarType.Int, tileInfo.getTileSize());
                shader.getUniform(TILE_WIDTH).setValue(VarType.Int, tileInfo.getGridWidth());
                shader.getUniform(TILE_HEIGHT).setValue(VarType.Int, tileInfo.getGridHeight());
                reset(gridChanged, numTilesChanged);

                Camera camera = renderManager.getCurrentCamera();
                viewPortWidth = camera.getWidth() * 0.5f;
                viewPortHeight = camera.getHeight() * 0.5f;
                vp = camera.getViewProjectionMatrix();
                Matrix4f v = camera.getViewMatrix();
                v.get(matArray1);
                tempVec3.set(matArray1[0], matArray1[1], matArray1[2]);
                camLeftCoeff = 1.0f / camera.getWorldPlane(1).getNormal().dot(tempVec3);
                tempVec3.set(matArray1[4], matArray1[5], matArray1[6]);
                camTopCoeff = 1.0f / camera.getWorldPlane(2).getNormal().dot(tempVec3);
                camLeft.set(matArray1[0], matArray1[1], matArray1[2], -1.0f).multLocal(-1.0f);
                camUp.set(matArray1[4], matArray1[5], matArray1[6], 1.0f);

                // update tiles
                LightFrustum lightFrustum = null;
                for(int i = 0;i < count;i++){
                    // filterLights(remove ambientLight,lightprobe...)
                    if(lights.get(i).getType() == Light.Type.Ambient || lights.get(i).getType() == Light.Type.Probe)continue;
                    lightFrustum = lightClip(lights.get(i));
                    if(lightFrustum != null){
                        tilesUpdate(tileInfo.getTileSize(), tileInfo.getGridWidth(), tileInfo.getGridHeight(), tileInfo.getNumTiles(), tiles, lightFrustum, i);
                    }
                    else{
                        // full tilesLight
                        tilesFullUpdate(tileInfo.getGridWidth(), tileInfo.getGridHeight(), tileInfo.getNumTiles(), tiles, i);
                    }
                }

                // Encode light source information
//                lightCount.setValue(VarType.Int, count);
//                geometry.getMaterial().setInt("NBLight", count);
                tileLightDecode(tileInfo.getNumTiles(), tiles, tileInfo.getGridWidth(), tileInfo.getGridHeight(), shader, geometry, lights);
                while (renderedLights < count) {
                    renderedLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, renderedLights, isLightCullStageDraw, lastBindUnit.textureUnit);
                    renderer.setShader(shader);
                    renderMeshFromGeometry(renderer, geometry);
                }
            }
            else{
//                geometry.getMaterial().setInt("NBLight", 0);
                renderedLights = updateLightListPackToTexture(shader, geometry, lights, count, renderManager, renderedLights, isLightCullStageDraw, lastBindUnit.textureUnit);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            }
        }
        else{
            // Do not use this branch, but keep it for possible future use
            int batchSize = renderManager.getSinglePassLightBatchSize();
            if (lights.size() == 0) {
                updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, 0, isLightCullStageDraw);
                renderer.setShader(shader);
                renderMeshFromGeometry(renderer, geometry);
            } else {
                while (renderedLights < lights.size()) {
                    renderedLights = updateLightListUniforms(shader, geometry, lights, batchSize, renderManager, renderedLights, isLightCullStageDraw);
                    renderer.setShader(shader);
                    renderMeshFromGeometry(renderer, geometry);
                }
            }
        }
    }
}
