package jme3test.terrain;

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
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
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

/**
 * This test uses 'PBRTerrain.j3md' to create a terrain Material for PBR.
 *
 * Upon running the app, the user should see a mountainous, terrain-based
 * landscape with some grassy areas, some snowy areas, and some tiled roads and
 * gravel paths weaving between the valleys. Snow should be slightly
 * shiny/reflective, and marble texture should be even shinier. If you would
 * like to know what each texture is supposed to look like, you can find the
 * textures used for this test case located in jme3-testdata. (Screenshots
 * showing how this test-case should look will also be available soon so you can
 * compare your results, and I will replace this comment with a link to their
 * location as soon as they are posted.)
 *
 * Press 'p' to toggle tri-planar mode. Enabling tri-planar mode should prevent
 * stretching of textures in steep areas of the terrain.
 *
 * Press 'n' to toggle between night and day. Pressing 'n' will cause the light
 * to gradually fade darker/brighter until the min/max lighting levels are
 * reached. At night the scene should be noticeably darker.
 *
 * Uses assets from CC0Textures.com, licensed under CC0 1.0 Universal. For more
 * information on the textures this test case uses, view the license.txt file
 * located in the jme3-testdata directory where these textures are located:
 * jme3-testdata/src/main/resources/Textures/Terrain/PBR
 *
 * <p>
 * Notes: (as of 12 April 2021)
 * <ol>
 * <li>
 * This shader is subject to the GLSL max limit of 16 textures, and users should
 * consider using "AdvancedPBRTerrain.j3md" instead if they need additional
 * texture slots.
 * </li>
 * </ol>
 *
 * @author yaRnMcDonuts
 */
public class PBRTerrainTest extends SimpleApplication {

    private TerrainQuad terrain;
    private Material matTerrain;
    private boolean triPlanar = false;

    private final int terrainSize = 512;
    private final int patchSize = 256;
    private final float dirtScale = 24;
    private final float darkRockScale = 24;
    private final float snowScale = 64;
    private final float tileRoadScale = 64;
    private final float grassScale = 24;
    private final float marbleScale = 64;
    private final float gravelScale = 64;

    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;
    private boolean isNight = false;

    private final float dayLightIntensity = 1.0f;
    private final float nightLightIntensity = 0.03f;

    private BitmapText keybindingsText;

    private final float camMoveSpeed = 50f;

    public static void main(String[] args) {
        PBRTerrainTest app = new PBRTerrainTest();
        app.start();
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("triPlanar") && !pressed) {
                triPlanar = !triPlanar;
                if (triPlanar) {
                    matTerrain.setBoolean("useTriPlanarMapping", true);
                    // Tri-planar textures don't use the mesh's texture coordinates but real world coordinates,
                    // so we need to convert these texture coordinate scales into real world scales so it looks
                    // the same when we switch to/from tri-planar mode.
                    matTerrain.setFloat("AlbedoMap_0_scale", (dirtScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_1_scale", (darkRockScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_2_scale", (snowScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_3_scale", (tileRoadScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_4_scale", (grassScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_5_scale", (marbleScale / terrainSize));
                    matTerrain.setFloat("AlbedoMap_6_scale", (gravelScale / terrainSize));
                } else {
                    matTerrain.setBoolean("useTriPlanarMapping", false);

                    matTerrain.setFloat("AlbedoMap_0_scale", dirtScale);
                    matTerrain.setFloat("AlbedoMap_1_scale", darkRockScale);
                    matTerrain.setFloat("AlbedoMap_2_scale", snowScale);
                    matTerrain.setFloat("AlbedoMap_3_scale", tileRoadScale);
                    matTerrain.setFloat("AlbedoMap_4_scale", grassScale);
                    matTerrain.setFloat("AlbedoMap_5_scale", marbleScale);
                    matTerrain.setFloat("AlbedoMap_6_scale", gravelScale);
                }
            }
            if (name.equals("toggleNight") && !pressed) {
                isNight = !isNight;
                // Ambient and directional light are faded smoothly in update loop below.
            }
        }
    };

    @Override
    public void simpleInitApp() {
        setupKeys();
        setUpTerrain();
        setUpTerrainMaterial();
        setUpLights();
        setUpCamera();
    }

    private void setUpTerrainMaterial() {
        // PBR terrain matdef
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/PBRTerrain.j3md");

        matTerrain.setBoolean("useTriPlanarMapping", false);

        // ALPHA map (for splat textures)
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
        matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));
        // this material also supports 'AlphaMap_2', so you can get up to 12 diffuse textures

        // DIRT texture, Diffuse textures 0 to 3 use the first AlphaMap
        Texture dirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("AlbedoMap_0", dirt);
        matTerrain.setFloat("AlbedoMap_0_scale", dirtScale);
        matTerrain.setFloat("Roughness_0", 1);
        matTerrain.setFloat("Metallic_0", 0);
        //matTerrain.setInt("AfflictionMode_0", 0);

        // DARK ROCK texture
        Texture darkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Color.png");
        darkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("AlbedoMap_1", darkRock);
        matTerrain.setFloat("AlbedoMap_1_scale", darkRockScale);
        matTerrain.setFloat("Roughness_1", 0.92f);
        matTerrain.setFloat("Metallic_1", 0.02f);
        //matTerrain.setInt("AfflictionMode_1", 0);

        // SNOW texture
        Texture snow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Color.png");
        snow.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("AlbedoMap_2", snow);
        matTerrain.setFloat("AlbedoMap_2_scale", snowScale);
        matTerrain.setFloat("Roughness_2", 0.55f);
        matTerrain.setFloat("Metallic_2", 0.12f);

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

//        Texture normalMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Normal.png");
//        normalMapMarble.setWrap(WrapMode.Repeat);

        Texture normalMapTiles = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Normal.png");
        normalMapTiles.setWrap(WrapMode.Repeat);

        matTerrain.setTexture("NormalMap_0", normalMapDirt);
        matTerrain.setTexture("NormalMap_1", normalMapDarkRock);
        matTerrain.setTexture("NormalMap_2", normalMapSnow);
        matTerrain.setTexture("NormalMap_3", normalMapTiles);
        matTerrain.setTexture("NormalMap_4", normalMapGrass);
//        matTerrain.setTexture("NormalMap_5", normalMapMarble);  // Adding this texture would exceed the 16 texture limit.
        matTerrain.setTexture("NormalMap_6", normalMapGravel);

        terrain.setMaterial(matTerrain);
    }

    private void setupKeys() {
        flyCam.setMoveSpeed(50);
        inputManager.addMapping("triPlanar", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("toggleNight", new KeyTrigger(KeyInput.KEY_N));

        inputManager.addListener(actionListener, "triPlanar");
        inputManager.addListener(actionListener, "toggleNight");

        keybindingsText = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        keybindingsText.setText("Press 'N' to toggle day/night fade (takes a moment) \nPress 'P' to toggle tri-planar mode");

        getGuiNode().attachChild(keybindingsText);
        keybindingsText.move(new Vector3f(200, 120, 0));
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        //smoothly transition from day to night
        float currentLightIntensity = ambientLight.getColor().getRed();
        float incrementPerFrame = tpf * 0.3f;

        if (isNight) {
            if (ambientLight.getColor().getRed() > nightLightIntensity) {
                currentLightIntensity -= incrementPerFrame;
                if (currentLightIntensity < nightLightIntensity) {
                    currentLightIntensity = nightLightIntensity;
                }

                ambientLight.getColor().set(currentLightIntensity, currentLightIntensity, currentLightIntensity, 1.0f);
                directionalLight.getColor().set(currentLightIntensity, currentLightIntensity, currentLightIntensity, 1.0f);
            }
        } else {
            if (ambientLight.getColor().getRed() < dayLightIntensity) {
                currentLightIntensity += incrementPerFrame;
                if (currentLightIntensity > dayLightIntensity) {
                    currentLightIntensity = dayLightIntensity;
                }

                ambientLight.getColor().set(currentLightIntensity, currentLightIntensity, currentLightIntensity, 1.0f);
                directionalLight.getColor().set(currentLightIntensity, currentLightIntensity, currentLightIntensity, 1.0f);
            }
        }
    }

    private void setUpTerrain() {
        // HEIGHTMAP image (for the terrain heightmap)
        TextureKey hmKey = new TextureKey("Textures/Terrain/splat/mountains512.png", false);
        Texture heightMapImage = assetManager.loadTexture(hmKey);

        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.3f);
            heightmap.load();
            heightmap.smooth(0.9f, 1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        terrain = new TerrainQuad("terrain", patchSize + 1, terrainSize + 1, heightmap.getHeightMap());
//, new LodPerspectiveCalculatorFactory(getCamera(), 4)); // add this in to see it use entropy for LOD calculations
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator(new DistanceLodCalculator(patchSize + 1, 2.7f)); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(1f, 1f, 1f);
        rootNode.attachChild(terrain);
    }

    private void setUpLights() {
        LightProbe probe = (LightProbe) assetManager.loadAsset("Scenes/LightProbes/quarry_Probe.j3o");

        probe.setAreaType(LightProbe.AreaType.Spherical);
        probe.getArea().setRadius(2000);
        probe.getArea().setCenter(new Vector3f(0, 0, 0));
        rootNode.addLight(probe);

        directionalLight = new DirectionalLight();
        directionalLight.setDirection((new Vector3f(-0.3f, -0.5f, -0.3f)).normalize());
        directionalLight.setColor(ColorRGBA.White);
        rootNode.addLight(directionalLight);

        ambientLight = new AmbientLight();
        directionalLight.setColor(ColorRGBA.White);
        rootNode.addLight(ambientLight);
    }

    private void setUpCamera() {
        cam.setLocation(new Vector3f(0, 10, -10));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);

        getFlyByCamera().setMoveSpeed(camMoveSpeed);
    }
}
