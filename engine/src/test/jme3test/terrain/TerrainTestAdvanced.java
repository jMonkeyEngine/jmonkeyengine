/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

/**
 * Uses the terrain's lighting texture with normal maps and lights.
 *
 * @author bowens
 */
public class TerrainTestAdvanced extends SimpleApplication {

    private TerrainQuad terrain;
    Material matTerrain;
    Material matWire;
    boolean wireframe = false;
    boolean triPlanar = false;
    boolean wardiso = false;
    boolean minnaert = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    private float dirtScale = 16;
    private float darkRockScale = 32;
    private float pinkRockScale = 32;
    private float riverRockScale = 80;
    private float grassScale = 32;
    private float brickScale = 128;
    private float roadScale = 200;
    

    public static void main(String[] args) {
        TerrainTestAdvanced app = new TerrainTestAdvanced();
        app.start();
    }

    @Override
    public void initialize() {
        super.initialize();

        loadHintText();
    }

    @Override
    public void simpleInitApp() {
        setupKeys();

        // First, we load up our textures and the heightmap texture for the terrain

        // TERRAIN TEXTURE material
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);

        // ALPHA map (for splat textures)
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alpha1.png"));
        matTerrain.setTexture("AlphaMap_1", assetManager.loadTexture("Textures/Terrain/splat/alpha2.png"));
        // this material also supports 'AlphaMap_2', so you can get up to 12 diffuse textures
        
        // HEIGHTMAP image (for the terrain heightmap)
        TextureKey hmKey = new TextureKey("Textures/Terrain/splat/mountains512.png", false);
        Texture heightMapImage = assetManager.loadTexture(hmKey);
        
        // DIRT texture, Diffuse textures 0 to 3 use the first AlphaMap
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", dirt);
        matTerrain.setFloat("DiffuseMap_0_scale", dirtScale);
        
        // DARK ROCK texture
        Texture darkRock = assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        darkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", darkRock);
        matTerrain.setFloat("DiffuseMap_1_scale", darkRockScale);
        
        // PINK ROCK texture
        Texture pinkRock = assetManager.loadTexture("Textures/Terrain/Rock/Rock.PNG");
        pinkRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_2", pinkRock);
        matTerrain.setFloat("DiffuseMap_2_scale", pinkRockScale);
        
        // RIVER ROCK texture, this texture will use the next alphaMap: AlphaMap_1
        Texture riverRock = assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg");
        riverRock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_3", riverRock);
        matTerrain.setFloat("DiffuseMap_3_scale", riverRockScale);
        
        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_4", grass);
        matTerrain.setFloat("DiffuseMap_4_scale", grassScale);

        // BRICK texture
        Texture brick = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        brick.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_5", brick);
        matTerrain.setFloat("DiffuseMap_5_scale", brickScale);
        
        // ROAD texture
        Texture road = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        road.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_6", road);
        matTerrain.setFloat("DiffuseMap_6_scale", roadScale);

        
        // diffuse textures 0 to 3 use AlphaMap
        // diffuse textures 4 to 7 use AlphaMap_1
        // diffuse textures 8 to 11 use AlphaMap_2

        
        // NORMAL MAPS
        Texture normalMapDirt = assetManager.loadTexture("Textures/Terrain/splat/dirt_normal.png");
        normalMapDirt.setWrap(WrapMode.Repeat);
        Texture normalMapPinkRock = assetManager.loadTexture("Textures/Terrain/Rock/Rock_normal.png");
        normalMapPinkRock.setWrap(WrapMode.Repeat);
        Texture normalMapGrass = assetManager.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
        normalMapGrass.setWrap(WrapMode.Repeat);
        Texture normalMapRoad = assetManager.loadTexture("Textures/Terrain/splat/road_normal.png");
        normalMapRoad.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("NormalMap", normalMapDirt);
        matTerrain.setTexture("NormalMap_1", normalMapPinkRock);
        matTerrain.setTexture("NormalMap_2", normalMapPinkRock);
        matTerrain.setTexture("NormalMap_4", normalMapGrass);
        matTerrain.setTexture("NormalMap_6", normalMapRoad);

        
        // WIREFRAME material (used to debug the terrain, only useful for this test case)
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        
        createSky();

        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.3f);
            heightmap.load();
            heightmap.smooth(0.9f, 1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Here we create the actual terrain. The tiles will be 65x65, and the total size of the
         * terrain will be 513x513. It uses the heightmap we created to generate the height values.
         */
        /**
         * Optimal terrain patch size is 65 (64x64).
         * The total size is up to you. At 1025 it ran fine for me (200+FPS), however at
         * size=2049 it got really slow. But that is a jump from 2 million to 8 million triangles...
         */
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());//, new LodPerspectiveCalculatorFactory(getCamera(), 4)); // add this in to see it use entropy for LOD calculations
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(1f, 1f, 1f);
        rootNode.attachChild(terrain);
        
        //Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        //terrain.generateDebugTangents(debugMat);

        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.1f, -0.1f, -0.1f)).normalize());
        rootNode.addLight(light);

        cam.setLocation(new Vector3f(0, 10, -10));
        cam.lookAtDirection(new Vector3f(0, -1.5f, -1).normalizeLocal(), Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(400);
        
        rootNode.attachChild(createAxisMarker(20));
    }

    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit T to switch to wireframe,  P to switch to tri-planar texturing");
        guiNode.attachChild(hintText);
    }

    private void setupKeys() {
        flyCam.setMoveSpeed(50);
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
        inputManager.addMapping("triPlanar", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(actionListener, "triPlanar");
        inputManager.addMapping("WardIso", new KeyTrigger(KeyInput.KEY_9));
        inputManager.addListener(actionListener, "WardIso");
        inputManager.addMapping("DetachControl", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addListener(actionListener, "DetachControl");
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("wireframe") && !pressed) {
                wireframe = !wireframe;
                if (!wireframe) {
                    terrain.setMaterial(matWire);
                } else {
                    terrain.setMaterial(matTerrain);
                }
            } else if (name.equals("triPlanar") && !pressed) {
                triPlanar = !triPlanar;
                if (triPlanar) {
                    matTerrain.setBoolean("useTriPlanarMapping", true);
                    // planar textures don't use the mesh's texture coordinates but real world coordinates,
                    // so we need to convert these texture coordinate scales into real world scales so it looks
                    // the same when we switch to/from tr-planar mode (1024f is the alphamap size)
                    matTerrain.setFloat("DiffuseMap_0_scale", 1f / (float) (1024f / dirtScale));
                    matTerrain.setFloat("DiffuseMap_1_scale", 1f / (float) (1024f / darkRockScale));
                    matTerrain.setFloat("DiffuseMap_2_scale", 1f / (float) (1024f / pinkRockScale));
                    matTerrain.setFloat("DiffuseMap_3_scale", 1f / (float) (1024f / riverRockScale));
                    matTerrain.setFloat("DiffuseMap_4_scale", 1f / (float) (1024f / grassScale));
                    matTerrain.setFloat("DiffuseMap_5_scale", 1f / (float) (1024f / brickScale));
                    matTerrain.setFloat("DiffuseMap_6_scale", 1f / (float) (1024f / roadScale));
                } else {
                    matTerrain.setBoolean("useTriPlanarMapping", false);
                    
                    matTerrain.setFloat("DiffuseMap_0_scale", dirtScale);
                    matTerrain.setFloat("DiffuseMap_1_scale", darkRockScale);
                    matTerrain.setFloat("DiffuseMap_2_scale", pinkRockScale);
                    matTerrain.setFloat("DiffuseMap_3_scale", riverRockScale);
                    matTerrain.setFloat("DiffuseMap_4_scale", grassScale);
                    matTerrain.setFloat("DiffuseMap_5_scale", brickScale);
                    matTerrain.setFloat("DiffuseMap_6_scale", roadScale);
                    
                    
                    
                }
            } if (name.equals("DetachControl") && !pressed) {
                TerrainLodControl control = terrain.getControl(TerrainLodControl.class);
                if (control != null)
                    control.detachAndCleanUpControl();
                else {
                    control = new TerrainLodControl(terrain, cam);
                    terrain.addControl(control);
                }
                    
            }
        }
    };

    private void createSky() {
        Texture west = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg");
        Texture east = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg");
        Texture north = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg");
        Texture south = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg");
        Texture up = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg");
        Texture down = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg");

        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);
    }
    
    protected Node createAxisMarker(float arrowSize) {

        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.getAdditionalRenderState().setWireframe(true);
        redMat.setColor("Color", ColorRGBA.Red);
        
        Material greenMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.getAdditionalRenderState().setWireframe(true);
        greenMat.setColor("Color", ColorRGBA.Green);
        
        Material blueMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.getAdditionalRenderState().setWireframe(true);
        blueMat.setColor("Color", ColorRGBA.Blue);

        Node axis = new Node();

        // create arrows
        Geometry arrowX = new Geometry("arrowX", new Arrow(new Vector3f(arrowSize, 0, 0)));
        arrowX.setMaterial(redMat);
        Geometry arrowY = new Geometry("arrowY", new Arrow(new Vector3f(0, arrowSize, 0)));
        arrowY.setMaterial(greenMat);
        Geometry arrowZ = new Geometry("arrowZ", new Arrow(new Vector3f(0, 0, arrowSize)));
        arrowZ.setMaterial(blueMat);
        axis.attachChild(arrowX);
        axis.attachChild(arrowY);
        axis.attachChild(arrowZ);

        //axis.setModelBound(new BoundingBox());
        return axis;
    }
}
