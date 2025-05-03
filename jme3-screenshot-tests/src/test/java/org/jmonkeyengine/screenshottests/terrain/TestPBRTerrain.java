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
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * This test uses 'PBRTerrain.j3md' to create a terrain Material for PBR.
 *
 * Upon running the app, the user should see a mountainous, terrain-based
 * landscape with some grassy areas, some snowy areas, and some tiled roads and
 * gravel paths weaving between the valleys. Snow should be slightly
 * shiny/reflective, and marble texture should be even shinier. If you would
 * like to know what each texture is supposed to look like, you can find the
 * textures used for this test case located in jme3-testdata.
 *
 * Uses assets from CC0Textures.com, licensed under CC0 1.0 Universal. For more
 * information on the textures this test case uses, view the license.txt file
 * located in the jme3-testdata directory where these textures are located:
 * jme3-testdata/src/main/resources/Textures/Terrain/PBR
 *
 * @author yaRnMcDonuts (Original manual test)
 * @author Richard Tingle (aka richtea) - screenshot test adaptation
 */
@SuppressWarnings("FieldCanBeLocal")
public class TestPBRTerrain extends ScreenshotTestBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
            Arguments.of("FinalRender", -1),
            Arguments.of("NormalMap", 1),
            Arguments.of("RoughnessMap", 2),
            Arguments.of("MetallicMap", 3),
            Arguments.of("GeometryNormals", 8)
        );
    }

    /**
     * Test PBR terrain with different debug modes
     * 
     * @param testName The name of the test (used for screenshot filename)
     * @param debugMode The debug mode to use
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testPBRTerrain(String testName, int debugMode, TestInfo testInfo) {

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
                // PBR terrain matdef
                matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/PBRTerrain.j3md");

                matTerrain.setBoolean("useTriPlanarMapping", false);

                // ALPHA map (for splat textures)
                matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
                matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));

                // DIRT texture
                Texture dirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                dirt.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_0", dirt);
                matTerrain.setFloat("AlbedoMap_0_scale", dirtScale);
                matTerrain.setFloat("Roughness_0", 1);
                matTerrain.setFloat("Metallic_0", 0);

                // DARK ROCK texture
                Texture darkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Color.png");
                darkRock.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_1", darkRock);
                matTerrain.setFloat("AlbedoMap_1_scale", darkRockScale);
                matTerrain.setFloat("Roughness_1", 0.92f);
                matTerrain.setFloat("Metallic_1", 0.02f);

                // SNOW texture
                Texture snow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Color.png");
                snow.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_2", snow);
                matTerrain.setFloat("AlbedoMap_2_scale", snowScale);
                matTerrain.setFloat("Roughness_2", 0.55f);
                matTerrain.setFloat("Metallic_2", 0.12f);

                // TILES texture
                Texture tiles = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Color.png");
                tiles.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_3", tiles);
                matTerrain.setFloat("AlbedoMap_3_scale", tileRoadScale);
                matTerrain.setFloat("Roughness_3", 0.87f);
                matTerrain.setFloat("Metallic_3", 0.08f);

                // GRASS texture
                Texture grass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                grass.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_4", grass);
                matTerrain.setFloat("AlbedoMap_4_scale", grassScale);
                matTerrain.setFloat("Roughness_4", 1);
                matTerrain.setFloat("Metallic_4", 0);

                // MARBLE texture
                Texture marble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Color.png");
                marble.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_5", marble);
                matTerrain.setFloat("AlbedoMap_5_scale", marbleScale);
                matTerrain.setFloat("Roughness_5", 0.06f);
                matTerrain.setFloat("Metallic_5", 0.8f);

                // Gravel texture
                Texture gravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Color.png");
                gravel.setWrap(WrapMode.Repeat);
                matTerrain.setTexture("AlbedoMap_6", gravel);
                matTerrain.setFloat("AlbedoMap_6_scale", gravelScale);
                matTerrain.setFloat("Roughness_6", 0.9f);
                matTerrain.setFloat("Metallic_6", 0.07f);

                // NORMAL MAPS
                Texture normalMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_1K_Normal.png");
                normalMapDirt.setWrap(WrapMode.Repeat);

                Texture normalMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Normal.png");
                normalMapDarkRock.setWrap(WrapMode.Repeat);

                Texture normalMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Normal.png");
                normalMapSnow.setWrap(WrapMode.Repeat);

                Texture normalMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Normal.png");
                normalMapGravel.setWrap(WrapMode.Repeat);

                Texture normalMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Normal.png");
                normalMapGrass.setWrap(WrapMode.Repeat);

                Texture normalMapTiles = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Normal.png");
                normalMapTiles.setWrap(WrapMode.Repeat);

                matTerrain.setTexture("NormalMap_0", normalMapDirt);
                matTerrain.setTexture("NormalMap_1", normalMapDarkRock);
                matTerrain.setTexture("NormalMap_2", normalMapSnow);
                matTerrain.setTexture("NormalMap_3", normalMapTiles);
                matTerrain.setTexture("NormalMap_4", normalMapGrass);
                matTerrain.setTexture("NormalMap_6", normalMapGravel);

                terrain.setMaterial(matTerrain);
            }

            private void setUpTerrain(SimpleApplication simpleApp, AssetManager assetManager) {
                // HEIGHTMAP image (for the terrain heightmap)
                TextureKey hmKey = new TextureKey("Textures/Terrain/splat/mountains512.png", false);
                Texture heightMapImage = assetManager.loadTexture(hmKey);

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
          .setFramesToTakeScreenshotsOn(4)
          .run();
    }
}
