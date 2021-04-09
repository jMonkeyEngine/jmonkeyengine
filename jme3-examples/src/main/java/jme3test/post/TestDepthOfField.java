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
package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;
import java.util.ArrayList;
import java.util.List;

/**
 * test
 * @author Nehon
 */
public class TestDepthOfField extends SimpleApplication {

    final private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private TerrainQuad terrain;
    private DepthOfFieldFilter dofFilter;

    public static void main(String[] args) {
        TestDepthOfField app = new TestDepthOfField();
        app.start();
    }

    @Override
    public void simpleInitApp() {


        Node mainScene = new Node("Main Scene");
        rootNode.attachChild(mainScene);

        createTerrain(mainScene);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1.7f));
        mainScene.addLight(sun);

        DirectionalLight l = new DirectionalLight();
        l.setDirection(Vector3f.UNIT_Y.mult(-1));
        l.setColor(ColorRGBA.White.clone().multLocal(0.3f));
        mainScene.addLight(l);

        flyCam.setMoveSpeed(50);
        cam.setFrustumFar(3000);
        cam.setLocation(new Vector3f(-700, 100, 300));
        cam.setRotation(new Quaternion().fromAngles(new float[]{FastMath.PI * 0.06f, FastMath.PI * 0.65f, 0}));


        Spatial sky = SkyFactory.createSky(assetManager, 
                "Scenes/Beach/FullskiesSunset0068.dds", EnvMapType.CubeMap);
        sky.setLocalScale(350);
        mainScene.attachChild(sky);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        //     fpp.setNumSamples(4);
        int numSamples = getContext().getSettings().getSamples();
        if( numSamples > 0 ) {
            fpp.setNumSamples(numSamples); 
        }

        dofFilter = new DepthOfFieldFilter();
        dofFilter.setFocusDistance(0);
        dofFilter.setFocusRange(50);
        dofFilter.setBlurScale(1.4f);
        fpp.addFilter(dofFilter);
        viewPort.addProcessor(fpp);

        inputManager.addListener(new ActionListener() {

            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("toggle")) {
                        dofFilter.setEnabled(!dofFilter.isEnabled());
                    }


                }
            }
        }, "toggle");
        inputManager.addListener(new AnalogListener() {

            @Override
            public void onAnalog(String name, float value, float tpf) {
                if (name.equals("blurScaleUp")) {
                    dofFilter.setBlurScale(dofFilter.getBlurScale() + 0.01f);
                    System.out.println("blurScale : " + dofFilter.getBlurScale());
                }
                if (name.equals("blurScaleDown")) {
                    dofFilter.setBlurScale(dofFilter.getBlurScale() - 0.01f);
                    System.out.println("blurScale : " + dofFilter.getBlurScale());
                }
                if (name.equals("focusRangeUp")) {
                    dofFilter.setFocusRange(dofFilter.getFocusRange() + 1f);
                    System.out.println("focusRange : " + dofFilter.getFocusRange());
                }
                if (name.equals("focusRangeDown")) {
                    dofFilter.setFocusRange(dofFilter.getFocusRange() - 1f);
                    System.out.println("focusRange : " + dofFilter.getFocusRange());
                }
                if (name.equals("focusDistanceUp")) {
                    dofFilter.setFocusDistance(dofFilter.getFocusDistance() + 1f);
                    System.out.println("focusDistance : " + dofFilter.getFocusDistance());
                }
                if (name.equals("focusDistanceDown")) {
                    dofFilter.setFocusDistance(dofFilter.getFocusDistance() - 1f);
                    System.out.println("focusDistance : " + dofFilter.getFocusDistance());
                }

            }
        }, "blurScaleUp", "blurScaleDown", "focusRangeUp", "focusRangeDown", "focusDistanceUp", "focusDistanceDown");


        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("blurScaleUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("blurScaleDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("focusRangeUp", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("focusRangeDown", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("focusDistanceUp", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("focusDistanceDown", new KeyTrigger(KeyInput.KEY_L));

    }

    private void createTerrain(Node rootNode) {
        Material matRock = new Material(assetManager,
                "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);
        matRock.setBoolean("WardIso", true);
        matRock.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap", grass);
        matRock.setFloat("DiffuseMap_0_scale", 64);
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_1", dirt);
        matRock.setFloat("DiffuseMap_1_scale", 16);
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_2", rock);
        matRock.setFloat("DiffuseMap_2_scale", 128);
        Texture normalMap0 = assetManager.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
        normalMap0.setWrap(WrapMode.Repeat);
        Texture normalMap1 = assetManager.loadTexture("Textures/Terrain/splat/dirt_normal.png");
        normalMap1.setWrap(WrapMode.Repeat);
        Texture normalMap2 = assetManager.loadTexture("Textures/Terrain/splat/road_normal.png");
        normalMap2.setWrap(WrapMode.Repeat);
        matRock.setTexture("NormalMap", normalMap0);
        matRock.setTexture("NormalMap_1", normalMap1);
        matRock.setTexture("NormalMap_2", normalMap2);

        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
            heightmap.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        List<Camera> cameras = new ArrayList<>();
        cameras.add(getCamera());
        terrain.setMaterial(matRock);
        terrain.setLocalScale(new Vector3f(5, 5, 5));
        terrain.setLocalTranslation(new Vector3f(0, -30, 0));
        terrain.setLocked(false); // unlock it so we can edit the height

        terrain.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(terrain);

    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.3f);
        direction.subtractLocal(origin).normalizeLocal();
        Ray ray = new Ray(origin, direction);
        CollisionResults results = new CollisionResults();
        int numCollisions = terrain.collideWith(ray, results);
        if (numCollisions > 0) {
            CollisionResult hit = results.getClosestCollision();
            fpsText.setText(""+hit.getDistance());
            dofFilter.setFocusDistance(hit.getDistance()/10.0f);
        }
    }
}
