package org.jmonkeyengine.screenshottests.scenarios.terrain;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

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
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.TextureArray;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

import java.util.ArrayList;
import java.util.List;

public class ScenarioPBRTerrainAdvanced {

    public static ScreenshotTest testPBRTerrainAdvanced(int debugMode) {
        return screenshotTest(new BaseAppState() {
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

            private final ColorRGBA tilesEmissiveColor = new ColorRGBA(0.12f, 0.02f, 0.23f, 0.85f);
            private final ColorRGBA marbleEmissiveColor = new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f);

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
                matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/AdvancedPBRTerrain.j3md");
                matTerrain.setBoolean("useTriPlanarMapping", false);

                matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
                matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));

                Texture dirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                Texture darkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Color.png");
                Texture snow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Color.png");
                Texture tileRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Color.png");
                Texture grass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
                Texture marble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Color.png");
                Texture gravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Color.png");

                Texture normalMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_1K_Normal.png");
                Texture normalMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Normal.png");
                Texture normalMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Normal.png");
                Texture normalMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Normal.png");
                Texture normalMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Normal.png");
                Texture normalMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Normal.png");
                Texture normalMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Normal.png");

                Texture metallicRoughnessAoEiMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel_015_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_PackedMetallicRoughnessMap.png");
                Texture metallicRoughnessAoEiMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_PackedMetallicRoughnessMap.png");

                List<Image> albedoImages = new ArrayList<>();
                List<Image> normalMapImages = new ArrayList<>();
                List<Image> metallicRoughnessAoEiMapImages = new ArrayList<>();

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

                TextureArray albedoTextureArray = new TextureArray(albedoImages);
                TextureArray normalParallaxTextureArray = new TextureArray(normalMapImages);
                TextureArray metallicRoughnessAoEiTextureArray = new TextureArray(metallicRoughnessAoEiMapImages);

                setWrapAndMipMaps(albedoTextureArray);
                setWrapAndMipMaps(normalParallaxTextureArray);
                setWrapAndMipMaps(metallicRoughnessAoEiTextureArray);

                matTerrain.setParam("AlbedoTextureArray", VarType.TextureArray, albedoTextureArray);
                matTerrain.setParam("NormalParallaxTextureArray", VarType.TextureArray, normalParallaxTextureArray);
                matTerrain.setParam("MetallicRoughnessAoEiTextureArray", VarType.TextureArray, metallicRoughnessAoEiTextureArray);

                matTerrain.setInt("AlbedoMap_0", 0);
                matTerrain.setFloat("AlbedoMap_0_scale", dirtScale);
                matTerrain.setFloat("Roughness_0", 1);
                matTerrain.setFloat("Metallic_0", 0.02f);

                matTerrain.setInt("AlbedoMap_1", 1);
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

                matTerrain.setInt("NormalMap_0", 0);
                matTerrain.setInt("NormalMap_1", 1);
                matTerrain.setInt("NormalMap_2", 2);
                matTerrain.setInt("NormalMap_3", 3);
                matTerrain.setInt("NormalMap_4", 4);
                matTerrain.setInt("NormalMap_5", 5);
                matTerrain.setInt("NormalMap_6", 6);

                matTerrain.setInt("MetallicRoughnessMap_0", 0);
                matTerrain.setInt("MetallicRoughnessMap_1", 1);
                matTerrain.setInt("MetallicRoughnessMap_2", 2);
                matTerrain.setInt("MetallicRoughnessMap_3", 3);
                matTerrain.setInt("MetallicRoughnessMap_4", 4);
                matTerrain.setInt("MetallicRoughnessMap_5", 5);
                matTerrain.setInt("MetallicRoughnessMap_6", 6);

                matTerrain.setColor("EmissiveColor_5", marbleEmissiveColor);
                matTerrain.setColor("EmissiveColor_3", tilesEmissiveColor);

                terrain.setMaterial(matTerrain);
            }

            private void setWrapAndMipMaps(Texture texture) {
                texture.setWrap(WrapMode.Repeat);
                texture.setMinFilter(MinFilter.Trilinear);
                texture.setMagFilter(MagFilter.Bilinear);
            }

            private void setUpTerrain(SimpleApplication simpleApp, AssetManager assetManager) {
                TextureKey hmKey = new TextureKey("Textures/Terrain/splat/mountains512.png", false);
                Texture heightMapImage = assetManager.loadTexture(hmKey);

                AbstractHeightMap heightmap;
                try {
                    heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.3f);
                    heightmap.load();
                    heightmap.smooth(0.9f, 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                terrain = new TerrainQuad("terrain", patchSize + 1, terrainSize + 1, heightmap.getHeightMap());
                TerrainLodControl control = new TerrainLodControl(terrain, simpleApp.getCamera());
                control.setLodCalculator(new DistanceLodCalculator(patchSize + 1, 2.7f));
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
            protected void cleanup(Application app) {
            }

            @Override
            protected void onEnable() {
            }

            @Override
            protected void onDisable() {
            }
        }).setFramesToTakeScreenshotsOn(5);
    }
}
