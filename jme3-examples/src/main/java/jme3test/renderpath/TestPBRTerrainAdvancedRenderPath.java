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
package jme3test.terrain;

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
import com.jme3.shader.VarType;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureArray;
import java.util.ArrayList;
import java.util.List;

/**
 * This test uses 'AdvancedPBRTerrain.j3md' to create a terrain Material with
 * more textures than 'PBRTerrain.j3md' can handle.
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
 * reached. At night the scene should be noticeably darker, and the marble and
 * tiled-road texture should be noticeably glowing from the emissiveColors and
 * the emissiveIntensity map that is packed into the alpha channel of the
 * MetallicRoughness maps.
 *
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
 * <p>
 * Notes: (as of 12 April 2021)
 * <ol>
 * <li>
 * The results look better with anti-aliasing, especially from a distance. This
 * may be due to the way that the terrain is generated from a heightmap, as
 * these same textures do not have this issue in my other project.
 * </li>
 * <li>
 * The number of images per texture array may still be limited by
 * GL_MAX_ARRAY_TEXTURE_LAYERS, however this value should be high enough that
 * users will likely run into issues with extremely low FPS from too many
 * texture-reads long before you surpass the limit of texture-layers per
 * textureArray. If this ever becomes an issue, a secondary set of
 * Albedo/Normal/MetallicRoughness texture arrays could be added to the shader
 * to store any textures that surpass the limit of the primary textureArrays.
 * </li>
 * </ol>
 *
 * @author yaRnMcDonuts
 */
public class PBRTerrainAdvancedTest extends SimpleApplication {

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

    private final ColorRGBA tilesEmissiveColor = new ColorRGBA(0.12f, 0.02f, 0.23f, 0.85f); //dim magenta emission
    private final ColorRGBA marbleEmissiveColor = new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f); //fully saturated blue emission

    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;
    private boolean isNight = false;

    private final float dayLightIntensity = 1.0f;
    private final float nightLightIntensity = 0.03f;

    private BitmapText keybindingsText;

    private final float camMoveSpeed = 50f;

    public static void main(String[] args) {
        PBRTerrainAdvancedTest app = new PBRTerrainAdvancedTest();
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
        setUpTerrainMaterial(); // <- This method contains the important info about using 'AdvancedPBRTerrain.j3md'
        setUpLights();
        setUpCamera();
    }

    private void setUpTerrainMaterial() {
        // advanced PBR terrain matdef
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/AdvancedPBRTerrain.j3md");

        matTerrain.setBoolean("useTriPlanarMapping", false);

        // ALPHA map (for splat textures)
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
        matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));
        // this material also supports 'AlphaMap_2', so you can get up to 12 texture slots

        // load textures for texture arrays
        // These MUST all have the same dimensions and format in order to be put into a texture array.
        //ALBEDO MAPS
        Texture dirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
        Texture darkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Color.png");
        Texture snow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Color.png");
        Texture tileRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Color.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Color.png");
        Texture marble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Color.png");
        Texture gravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Color.png");

        // NORMAL MAPS
        Texture normalMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_1K_Normal.png");
        Texture normalMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_1K_Normal.png");
        Texture normalMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_1K_Normal.png");
        Texture normalMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel015_1K_Normal.png");
        Texture normalMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_1K_Normal.png");
        Texture normalMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_1K_Normal.png");
        Texture normalMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_1K_Normal.png");

        //PACKED METALLIC/ROUGHNESS / AMBIENT OCCLUSION / EMISSIVE INTENSITY MAPS
        Texture metallicRoughnessAoEiMapDirt = assetManager.loadTexture("Textures/Terrain/PBR/Ground036_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapDarkRock = assetManager.loadTexture("Textures/Terrain/PBR/Rock035_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapSnow = assetManager.loadTexture("Textures/Terrain/PBR/Snow006_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapGravel = assetManager.loadTexture("Textures/Terrain/PBR/Gravel_015_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapGrass = assetManager.loadTexture("Textures/Terrain/PBR/Ground037_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapMarble = assetManager.loadTexture("Textures/Terrain/PBR/Marble013_PackedMetallicRoughnessMap.png");
        Texture metallicRoughnessAoEiMapRoad = assetManager.loadTexture("Textures/Terrain/PBR/Tiles083_PackedMetallicRoughnessMap.png");

        // put all images into lists to create texture arrays.
        //
        // The index of each image in its list will be
        // sent to the material to tell the shader to choose that texture from
        // the textureArray when setting up a texture slot's mat params.
        //
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

        //initiate texture arrays
        TextureArray albedoTextureArray = new TextureArray(albedoImages);
        TextureArray normalParallaxTextureArray = new TextureArray(normalMapImages); // parallax is not used currently
        TextureArray metallicRoughnessAoEiTextureArray = new TextureArray(metallicRoughnessAoEiMapImages);

        //apply wrapMode to the whole texture array, rather than each individual texture in the array
        albedoTextureArray.setWrap(WrapMode.Repeat);
        normalParallaxTextureArray.setWrap(WrapMode.Repeat);
        metallicRoughnessAoEiTextureArray.setWrap(WrapMode.Repeat);

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
        // int being passed to shader corresponds to the index of the texture's
        // image in the List of images used to create its texture array
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
        //these two texture slots (marble & tiledRoad, indexed in each texture array at 5 and 3 respectively) both
        // have packed MRAoEi maps with an emissiveTexture packed into the alpha channel

//        matTerrain.setColor("EmissiveColor_1", new ColorRGBA(0.08f, 0.01f, 0.1f, 0.4f));
//this texture slot does not have a unique emissiveIntensityMap packed into its MRAoEi map,
        // so setting an emissiveColor will apply equal intensity to every pixel

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
