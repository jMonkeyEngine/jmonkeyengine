/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.terrain;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.GlImage;
import com.jme3.texture.GlTexture;
import com.jme3.texture.GlTexture.WrapMode;
import com.jme3.texture.GlTexture.MagFilter;
import com.jme3.texture.GlTexture.MinFilter;
import com.jme3.texture.TextureArray;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * This test uses 'AdvancedPBRTerrain.j3md' to create a terrain Material with
 * more textures than 'PBRTerrain.j3md' can handle.
 *
 * Upon running the app, the user should see a mountainous, terrain-based
 * landscape with some grassy areas, some snowy areas, and some tiled roads and
 * gravel paths weaving between the valleys. Snow should be slightly
 * shiny/reflective, and marble texture should be even shinier. If you would
 * like to know what each texture is supposed to look like, you can find the
 * textures used for this test case located in jme3-testdata.

 * The MetallicRoughness map stores:
 * <ul>
 * <li> AmbientOcclusion in the Red channel </li>
 * <li> Roughness in the Green channel </li>
 * <li> Metallic in the Blue channel </li>
 * <li> EmissiveIntensity in the Alpha channel </li>
 * </ul>
 *
 * The shaders are still subject to the GLSL max limit of 16 textures, however
 * each TextureArray counts as a single texture, and each TextureArray can store
 * multiple images. For more information on texture arrays see:
 * https://www.khronos.org/opengl/wiki/Array_Texture
 *
 * Uses assets from CC0Textures.com, licensed under CC0 1.0 Universal. For more
 * information on the textures this test case uses, view the license.txt file
 * located in the jme3-testdata directory where these textures are located:
 * jme3-testdata/src/main/resources/Textures/Terrain/PBR
 *
 * @author yaRnMcDonuts - original test
 * @author Richard Tingle (aka richtea) - screenshot test adaptation
 */
@SuppressWarnings("FieldCanBeLocal")
public class TestPBRTerrainAdvanced extends ScreenshotTestBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
            Arguments.of("FinalRender", 0),
            Arguments.of("AmbientOcclusion", 4),
            Arguments.of("Emissive", 5)
        );
    }

    /**
     * Test advanced PBR terrain with different debug modes
     * 
     * @param testName The name of the test (used for screenshot filename)
     * @param debugMode The debug mode to use
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testPBRTerrainAdvanced(String testName, int debugMode, TestInfo testInfo) {
        if(!testInfo.getTestClass().isPresent() || !testInfo.getTestMethod().isPresent()) {
            throw new RuntimeException("Test preconditions not met");
        }

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        screenshotTest(new BaseAppState() {
            private TerrainQuad terrain;
            private Material matTerrain;
            
            private final int terrainSize = 512;
            private final int patchSize = 256;
            private final float dirtScale = 24;
            private final float darkRockScale = 24;
            private final float snowScale = 64;
            private final float tileRoadScale = 64;
            private final float grassScale = 24;
            private final float marbleScale = 64;
            private final float gravelScale = 64;
            
            private final ColorRGBA tilesEmissiveColor = new ColorRGBA(0.12f, 0.02f, 0.23f, 0.85f); //dim magenta emission
            private final ColorRGBA marbleEmissiveColor = new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f); //fully saturated blue emission
            
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApp = (SimpleApplication) app;
                AssetManager assetManager = app.getAssetManager();
                
                setUpTerrain(simpleApp, assetManager);
                setUpTerrainMaterial(assetManager);
                setUpLights(simpleApp, assetManager);
                setUpCamera(app);
                
                // Set debug mode
                matTerrain.setInt("DebugValuesMode", debugMode);
            }

            private void setUpTerrainMaterial(AssetManager assetManager) {
                // advanced PBR terrain matdef
                matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/AdvancedPBRTerrain.j3md");

                matTerrain.setBoolean("useTriPlanarMapping", false);

                // ALPHA map (for splat textures)
                matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
                matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));

                // load textures for texture arrays
                // These MUST all have the same dimensions and format in order to be put into a texture array.
                //ALBEDO MAPS
                GlTexture dirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                GlTexture darkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Color.png");
                GlTexture snow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Color.png");
                GlTexture tileRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Color.png");
                GlTexture grass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                GlTexture marble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Color.png");
                GlTexture gravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Color.png");

                // NORMAL MAPS
                GlTexture normalMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_1K_Normal.png");
                GlTexture normalMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Normal.png");
                GlTexture normalMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Normal.png");
                GlTexture normalMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Normal.png");
                GlTexture normalMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Normal.png");
                GlTexture normalMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Normal.png");
                GlTexture normalMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Normal.png");

                //PACKED METALLIC/ROUGHNESS / AMBIENT OCCLUSION / EMISSIVE INTENSITY MAPS
                GlTexture metallicRoughnessAoEiMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel_015_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_PackedMetallicRoughnessMap.png");
                GlTexture metallicRoughnessAoEiMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_PackedMetallicRoughnessMap.png");

                // put all images into lists to create texture arrays.
                List<GlImage> albedoImages = new ArrayList<>();
                List<GlImage> normalMapImages = new ArrayList<>();
                List<GlImage> metallicRoughnessAoEiMapImages = new ArrayList<>();

                albedoImages.add(dirt.getImage());  //0
                albedoImages.add(darkRock.getImage()); //1
                albedoImages.add(snow.getImage()); //2
                albedoImages.add(tileRoad.getImage()); //3
                albedoImages.add(grass.getImage()); //4
                albedoImages.add(marble.getImage()); //5
                albedoImages.add(gravel.getImage()); //6

                normalMapImages.add(normalMapDirt.getImage());  //0
                normalMapImages.add(normalMapDarkRock.getImage());  //1
                normalMapImages.add(normalMapSnow.getImage());  //2
                normalMapImages.add(normalMapRoad.getImage());   //3
                normalMapImages.add(normalMapGrass.getImage());   //4
                normalMapImages.add(normalMapMarble.getImage());   //5
                normalMapImages.add(normalMapGravel.getImage());   //6

                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapDirt.getImage());  //0
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapDarkRock.getImage());  //1
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapSnow.getImage());  //2
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapRoad.getImage());   //3
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapGrass.getImage());   //4
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapMarble.getImage());   //5
                metallicRoughnessAoEiMapImages.add(metallicRoughnessAoEiMapGravel.getImage());   //6

                //initiate texture arrays
                TextureArray albedoTextureArray = new TextureArray(albedoImages);
                TextureArray normalParallaxTextureArray = new TextureArray(normalMapImages); // parallax is not used currently
                TextureArray metallicRoughnessAoEiTextureArray = new TextureArray(metallicRoughnessAoEiMapImages);

                //apply wrapMode to the whole texture array, rather than each individual texture in the array
                setWrapAndMipMaps(albedoTextureArray);
                setWrapAndMipMaps(normalParallaxTextureArray);
                setWrapAndMipMaps(metallicRoughnessAoEiTextureArray);
                
                //assign texture array to materials
                matTerrain.setParam("AlbedoTextureArray", VarType.TextureArray, albedoTextureArray);
                matTerrain.setParam("NormalParallaxTextureArray", VarType.TextureArray, normalParallaxTextureArray);
                matTerrain.setParam("MetallicRoughnessAoEiTextureArray", VarType.TextureArray, metallicRoughnessAoEiTextureArray);

                //set up texture slots:
                matTerrain.setInt("AlbedoMap_0", 0); // dirt is index 0 in the albedo image list
                matTerrain.setFloat("AlbedoMap_0_scale", dirtScale);
                matTerrain.setFloat("Roughness_0", 1);
                matTerrain.setFloat("Metallic_0", 0.02f);

                matTerrain.setInt("AlbedoMap_1", 1);   // darkRock is index 1 in the albedo image list
                matTerrain.setFloat("AlbedoMap_1_scale", darkRockScale);
                matTerrain.setFloat("Roughness_1", 1);
                matTerrain.setFloat("Metallic_1", 0.04f);

                matTerrain.setInt("AlbedoMap_2", 2);
                matTerrain.setFloat("AlbedoMap_2_scale", snowScale);
                matTerrain.setFloat("Roughness_2", 0.72f);
                matTerrain.setFloat("Metallic_2", 0.12f);

                matTerrain.setInt("AlbedoMap_3", 3);
                matTerrain.setFloat("AlbedoMap_3_scale", tileRoadScale);
                matTerrain.setFloat("Roughness_3", 1);
                matTerrain.setFloat("Metallic_3", 0.04f);

                matTerrain.setInt("AlbedoMap_4", 4);
                matTerrain.setFloat("AlbedoMap_4_scale", grassScale);
                matTerrain.setFloat("Roughness_4", 1);
                matTerrain.setFloat("Metallic_4", 0);

                matTerrain.setInt("AlbedoMap_5", 5);
                matTerrain.setFloat("AlbedoMap_5_scale", marbleScale);
                matTerrain.setFloat("Roughness_5", 1);
                matTerrain.setFloat("Metallic_5", 0.2f);

                matTerrain.setInt("AlbedoMap_6", 6);
                matTerrain.setFloat("AlbedoMap_6_scale", gravelScale);
                matTerrain.setFloat("Roughness_6", 1);
                matTerrain.setFloat("Metallic_6", 0.01f);

                // NORMAL MAPS
                matTerrain.setInt("NormalMap_0", 0);
                matTerrain.setInt("NormalMap_1", 1);
                matTerrain.setInt("NormalMap_2", 2);
                matTerrain.setInt("NormalMap_3", 3);
                matTerrain.setInt("NormalMap_4", 4);
                matTerrain.setInt("NormalMap_5", 5);
                matTerrain.setInt("NormalMap_6", 6);

                //METALLIC/ROUGHNESS/AO/EI MAPS
                matTerrain.setInt("MetallicRoughnessMap_0", 0);
                matTerrain.setInt("MetallicRoughnessMap_1", 1);
                matTerrain.setInt("MetallicRoughnessMap_2", 2);
                matTerrain.setInt("MetallicRoughnessMap_3", 3);
                matTerrain.setInt("MetallicRoughnessMap_4", 4);
                matTerrain.setInt("MetallicRoughnessMap_5", 5);
                matTerrain.setInt("MetallicRoughnessMap_6", 6);

                //EMISSIVE
                matTerrain.setColor("EmissiveColor_5", marbleEmissiveColor);
                matTerrain.setColor("EmissiveColor_3", tilesEmissiveColor);

                terrain.setMaterial(matTerrain);
            }

            private void setWrapAndMipMaps(GlTexture texture) {
                texture.setWrap(WrapMode.Repeat);
                texture.setMinFilter(MinFilter.Trilinear);
                texture.setMagFilter(MagFilter.Bilinear);
            }

            private void setUpTerrain(SimpleApplication simpleApp, AssetManager assetManager) {
                // HEIGHTMAP image (for the terrain heightmap)
                TextureKey hmKey = new TextureKey("Textures/Terrain/splat/mountains512.png", false);
                GlTexture heightMapImage = assetManager.loadTexture(hmKey);

                // CREATE HEIGHTMAP
                AbstractHeightMap heightmap;
                try {
                    heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.3f);
                    heightmap.load();
                    heightmap.smooth(0.9f, 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                terrain = new TerrainQuad("terrain", patchSize + 1, terrainSize + 1, heightmap.getHeightMap());
                TerrainLodControl control = new TerrainLodControl(terrain, getApplication().getCamera());
                control.setLodCalculator(new DistanceLodCalculator(patchSize + 1, 2.7f)); // patch size, and a multiplier
                terrain.addControl(control);
                terrain.setMaterial(matTerrain);
                terrain.setLocalTranslation(0, -100, 0);
                terrain.setLocalScale(1f, 1f, 1f);
                simpleApp.getRootNode().attachChild(terrain);
            }

            private void setUpLights(SimpleApplication simpleApp, AssetManager assetManager) {
                LightProbe probe = (LightProbe) assetManager.loadAsset("Scenes/LightProbes/quarry_Probe.j3o");

                probe.setAreaType(LightProbe.AreaType.Spherical);
                probe.getArea().setRadius(2000);
                probe.getArea().setCenter(new Vector3f(0, 0, 0));
                simpleApp.getRootNode().addLight(probe);

                DirectionalLight directionalLight = new DirectionalLight();
                directionalLight.setDirection((new Vector3f(-0.3f, -0.5f, -0.3f)).normalize());
                directionalLight.setColor(ColorRGBA.White);
                simpleApp.getRootNode().addLight(directionalLight);

                AmbientLight ambientLight = new AmbientLight();
                ambientLight.setColor(ColorRGBA.White);
                simpleApp.getRootNode().addLight(ambientLight);
            }

            private void setUpCamera(Application app) {
                app.getCamera().setLocation(new Vector3f(0, 10, -10));
                app.getCamera().lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
            }

            @Override
            protected void cleanup(Application app) {}

            @Override
            protected void onEnable() {}

            @Override
            protected void onDisable() {}

        }).setBaseImageFileName(imageName)
          .setFramesToTakeScreenshotsOn(5)
          .run();
    }
}