/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.water;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Filter;
import com.jme3.audio.LowPassFilter;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;
import com.jme3.water.WaterFilter;

/**
 * @author normenhansen
 */
public class TestPostWater extends SimpleApplication {

    private final Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private WaterFilter water;

    public static void main(String[] args) {
        TestPostWater app = new TestPostWater();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        Node mainScene = new Node("Main Scene");
        rootNode.attachChild(mainScene);

        configureCamera();
        createSky(mainScene);
        createTerrain(mainScene);
        createLights(mainScene);
        createWaterFilter();
        setupPostFilters();
        addAudioClip();
        setupUI();
        registerInputMappings();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(50f);
        cam.setLocation(new Vector3f(-370.31592f, 182.04016f, 196.81192f));
        cam.setRotation(new Quaternion(0.015302252f, 0.9304095f, -0.039101653f, 0.3641086f));
        cam.setFrustumFar(2000);
    }

    private void createLights(Node mainScene) {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        mainScene.addLight(sun);

        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        mainScene.addLight(al);
    }

    private void createSky(Node mainScene) {
        Spatial sky = SkyFactory.createSky(assetManager,
                "Scenes/Beach/FullskiesSunset0068.dds", EnvMapType.CubeMap);
        sky.setShadowMode(ShadowMode.Off);
        mainScene.attachChild(sky);
    }

    private void setupUI() {
        setText(0, 50, "1 - Set Foam Texture to Foam.jpg");
        setText(0, 80, "2 - Set Foam Texture to Foam2.jpg");
        setText(0, 110, "3 - Set Foam Texture to Foam3.jpg");
        setText(0, 140, "4 - Turn Dry Filter under water On/Off");
        setText(0, 240, "PgUp - Larger Reflection Map");
        setText(0, 270, "PgDn - Smaller Reflection Map");
    }

    private void setText(int x, int y, String text) {
        BitmapText bmp = new BitmapText(guiFont);
        bmp.setText(text);
        bmp.setLocalTranslation(x, cam.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
    }

    private void registerInputMappings() {
        addMapping("foam1", new KeyTrigger(KeyInput.KEY_1));
        addMapping("foam2", new KeyTrigger(KeyInput.KEY_2));
        addMapping("foam3", new KeyTrigger(KeyInput.KEY_3));
        addMapping("dryFilter", new KeyTrigger(KeyInput.KEY_4));
        addMapping("upRM", new KeyTrigger(KeyInput.KEY_PGUP));
        addMapping("downRM", new KeyTrigger(KeyInput.KEY_PGDN));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(actionListener, mappingName);
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;

            if (name.equals("foam1")) {
                water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam.jpg"));

            } else if (name.equals("foam2")) {
                water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));

            } else if (name.equals("foam3")) {
                water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam3.jpg"));

            } else if (name.equals("upRM")) {
                water.setReflectionMapSize(Math.min(water.getReflectionMapSize() * 2, 4096));
                System.out.println("Reflection map size : " + water.getReflectionMapSize());

            } else if (name.equals("downRM")) {
                water.setReflectionMapSize(Math.max(water.getReflectionMapSize() / 2, 32));
                System.out.println("Reflection map size : " + water.getReflectionMapSize());

            } else if (name.equals("dryFilter")) {
                useDryFilter = !useDryFilter;
            }
        }
    };

    private void setupPostFilters() {
        BloomFilter bloom = new BloomFilter();
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);

        LightScatteringFilter lsf = new LightScatteringFilter(lightDir.mult(-300));
        lsf.setLightDensity(0.5f);

        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(0);
        dof.setFocusRange(100);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(water);
        fpp.addFilter(bloom);
        fpp.addFilter(dof);
        fpp.addFilter(lsf);
        fpp.addFilter(new FXAAFilter());

        int numSamples = getContext().getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }
        viewPort.addProcessor(fpp);
    }

    private void createWaterFilter() {
        //Water Filter
        water = new WaterFilter(rootNode, lightDir);
        water.setWaterColor(new ColorRGBA().setAsSrgb(0.0078f, 0.3176f, 0.5f, 1.0f));
        water.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
        water.setUnderWaterFogDistance(80);
        water.setWaterTransparency(0.12f);
        water.setFoamIntensity(0.4f);
        water.setFoamHardness(0.3f);
        water.setFoamExistence(new Vector3f(0.8f, 8f, 1f));
        water.setReflectionDisplace(50);
        water.setRefractionConstant(0.25f);
        water.setColorExtinction(new Vector3f(30, 50, 70));
        water.setCausticsIntensity(0.4f);
        water.setWaveScale(0.003f);
        water.setMaxAmplitude(2f);
        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        water.setRefractionStrength(0.2f);
        water.setWaterHeight(initialWaterHeight);
    }

    private void createTerrain(Node mainScene) {
        Material matRock = createTerrainMaterial();

        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
            heightmap.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int patchSize = 64;
        int totalSize = 512;
        TerrainQuad terrain = new TerrainQuad("terrain", patchSize + 1, totalSize + 1, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator(new DistanceLodCalculator(patchSize + 1, 2.7f)); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matRock);

        terrain.setLocalTranslation(new Vector3f(0, -30, 0));
        terrain.setLocalScale(new Vector3f(5, 5, 5));

        terrain.setShadowMode(ShadowMode.Receive);
        mainScene.attachChild(terrain);
    }

    private Material createTerrainMaterial() {
        Material matRock = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);
        matRock.setBoolean("WardIso", true);
        matRock.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        setTexture("Textures/Terrain/splat/grass.jpg", matRock, "DiffuseMap");
        setTexture("Textures/Terrain/splat/dirt.jpg", matRock, "DiffuseMap_1");
        setTexture("Textures/Terrain/splat/road.jpg", matRock, "DiffuseMap_2");
        matRock.setFloat("DiffuseMap_0_scale", 64);
        matRock.setFloat("DiffuseMap_1_scale", 16);
        matRock.setFloat("DiffuseMap_2_scale", 128);

        setTexture("Textures/Terrain/splat/grass_normal.jpg", matRock, "NormalMap");
        setTexture("Textures/Terrain/splat/dirt_normal.png", matRock, "NormalMap_1");
        setTexture("Textures/Terrain/splat/road_normal.png", matRock, "NormalMap_2");

        return matRock;
    }

    private void setTexture(String texture, Material mat, String param) {
        Texture tex = assetManager.loadTexture(texture);
        tex.setWrap(WrapMode.Repeat);
        mat.setTexture(param, tex);
    }

    // This part is to emulate tides, slightly varying the height of the water plane
    private float time = 0.0f;
    private float waterHeight = 0.0f;
    private final float initialWaterHeight = 90f;
    private boolean underWater = false;
    
    private AudioNode waves;
    private final LowPassFilter aboveWaterAudioFilter = new LowPassFilter(1, 1);
    private final LowPassFilter underWaterAudioFilter = new LowPassFilter(0.5f, 0.1f);
    private boolean useDryFilter = true;

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        waterHeight = (float) Math.cos(((time * 0.6f) % FastMath.TWO_PI)) * 1.5f;
        water.setWaterHeight(initialWaterHeight + waterHeight);
        underWater = water.isUnderWater();
        updateAudio();
    }

    private void addAudioClip() {
        underWater = cam.getLocation().y < waterHeight;

        waves = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", DataType.Buffer);
        waves.setLooping(true);
        updateAudio();
        waves.play();
    }

    /**
     * Update the audio settings (dry filter and reverb)
     * based on boolean fields ({@code underWater} and {@code useDryFilter}).
     */
    private void updateAudio() {
        Filter newDryFilter;
        if (!useDryFilter) {
            newDryFilter = null;
        } else if (underWater) {
            newDryFilter = underWaterAudioFilter;
        } else {
            newDryFilter = aboveWaterAudioFilter;
        }
        Filter oldDryFilter = waves.getDryFilter();
        if (oldDryFilter != newDryFilter) {
            System.out.println("dry filter : " + newDryFilter);
            waves.setDryFilter(newDryFilter);
        }

        boolean newReverbEnabled = !underWater;
        boolean oldReverbEnabled = waves.isReverbEnabled();
        if (oldReverbEnabled != newReverbEnabled) {
            System.out.println("reverb enabled : " + newReverbEnabled);
            waves.setReverbEnabled(newReverbEnabled);
        }
    }
}
