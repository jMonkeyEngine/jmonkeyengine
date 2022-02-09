/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;
import com.jme3.water.WaterFilter;

/**
 * test
 *
 * @author normenhansen
 */
public class TestPostWater extends SimpleApplication {

    final private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private WaterFilter water;
    private AudioNode waves;
    final private LowPassFilter aboveWaterAudioFilter = new LowPassFilter(1, 1);
    final private Filter underWaterAudioFilter = new LowPassFilter(0.5f, 0.1f);
    private boolean useDryFilter = true;

    public static void main(String[] args) {
        TestPostWater app = new TestPostWater();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        setDisplayFps(false);
        setDisplayStatView(false);
        
        Node mainScene = new Node("Main Scene");
        rootNode.attachChild(mainScene);

        createTerrain(mainScene);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1f));
        mainScene.addLight(sun);
        
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        mainScene.addLight(al);
        
        flyCam.setMoveSpeed(50);

        //cam.setLocation(new Vector3f(-700, 100, 300));
        //cam.setRotation(new Quaternion().fromAngleAxis(0.5f, Vector3f.UNIT_Z));
//        cam.setLocation(new Vector3f(-327.21957f, 61.6459f, 126.884346f));
//        cam.setRotation(new Quaternion(0.052168474f, 0.9443102f, -0.18395276f, 0.2678024f));


        cam.setLocation(new Vector3f(-370.31592f, 182.04016f, 196.81192f));
        cam.setRotation(new Quaternion(0.015302252f, 0.9304095f, -0.039101653f, 0.3641086f));




        Spatial sky = SkyFactory.createSky(assetManager, 
                "Scenes/Beach/FullskiesSunset0068.dds", EnvMapType.CubeMap);
        sky.setLocalScale(350);

        mainScene.attachChild(sky);
        cam.setFrustumFar(4000);

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
        
        //Bloom Filter
        BloomFilter bloom = new BloomFilter();        
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        
        //Light Scattering Filter
        LightScatteringFilter lsf = new LightScatteringFilter(lightDir.mult(-300));
        lsf.setLightDensity(0.5f);   
        
        //Depth of field Filter
        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(0);
        dof.setFocusRange(100);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        
        fpp.addFilter(water);
        fpp.addFilter(bloom);
        fpp.addFilter(dof);
        fpp.addFilter(lsf);
        fpp.addFilter(new FXAAFilter());
        
//      fpp.addFilter(new TranslucentBucketFilter());
        int numSamples = getContext().getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

        
        uw = cam.getLocation().y < waterHeight;

        waves = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg",
                DataType.Buffer);
        waves.setLooping(true);
        updateAudio();
        audioRenderer.playSource(waves);
        //  
        viewPort.addProcessor(fpp);

        setText(0, 50, "1 - Set Foam Texture to Foam.jpg");
        setText(0, 80, "2 - Set Foam Texture to Foam2.jpg");
        setText(0, 110, "3 - Set Foam Texture to Foam3.jpg");
        setText(0, 140, "4 - Turn Dry Filter under water On/Off");
        setText(0, 240, "PgUp - Larger Reflection Map");
        setText(0, 270, "PgDn - Smaller Reflection Map");

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("foam1")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam.jpg"));
                    }
                    if (name.equals("foam2")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
                    }
                    if (name.equals("foam3")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam3.jpg"));
                    }

                    if (name.equals("upRM")) {
                        water.setReflectionMapSize(Math.min(water.getReflectionMapSize() * 2, 4096));
                        System.out.println("Reflection map size : " + water.getReflectionMapSize());
                    }
                    if (name.equals("downRM")) {
                        water.setReflectionMapSize(Math.max(water.getReflectionMapSize() / 2, 32));
                        System.out.println("Reflection map size : " + water.getReflectionMapSize());
                    }
                    if (name.equals("dryFilter")) {
                        useDryFilter = !useDryFilter; 
                    }
                }
            }
        }, "foam1", "foam2", "foam3", "upRM", "downRM", "dryFilter");
        inputManager.addMapping("foam1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("foam2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("foam3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("dryFilter", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("upRM", new KeyTrigger(KeyInput.KEY_PGUP));
        inputManager.addMapping("downRM", new KeyTrigger(KeyInput.KEY_PGDN));
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
        TerrainQuad terrain
                = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        terrain.setMaterial(matRock);
        terrain.setLocalScale(new Vector3f(5, 5, 5));
        terrain.setLocalTranslation(new Vector3f(0, -30, 0));
        terrain.setLocked(false); // unlock it so we can edit the height

        terrain.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(terrain);

    }
    //This part is to emulate tides, slightly varying the height of the water plane
    private float time = 0.0f;
    private float waterHeight = 0.0f;
    final private float initialWaterHeight = 90f;//0.8f;
    private boolean uw = false;

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        //     box.updateGeometricState();
        time += tpf;
        waterHeight = (float) Math.cos(((time * 0.6f) % FastMath.TWO_PI)) * 1.5f;
        water.setWaterHeight(initialWaterHeight + waterHeight);
        uw = water.isUnderWater();
        updateAudio();
    }
    
    protected void setText(int x, int y, String text) {
        BitmapText txt2 = new BitmapText(guiFont);
        txt2.setText(text);
        txt2.setLocalTranslation(x, cam.getHeight() - y, 0);
        txt2.setColor(ColorRGBA.Red);
        guiNode.attachChild(txt2);
    }

    /**
     * Update the audio settings (dry filter and reverb)
     * based on boolean fields ({@code uw} and {@code useDryFilter}).
     */
    protected void updateAudio() {
        Filter newDryFilter;
        if (!useDryFilter) {
            newDryFilter = null;
        } else if (uw) {
            newDryFilter = underWaterAudioFilter;
        } else {
            newDryFilter = aboveWaterAudioFilter;
        }
        Filter oldDryFilter = waves.getDryFilter();
        if (oldDryFilter != newDryFilter) {
            System.out.println("dry filter : " + newDryFilter);
            waves.setDryFilter(newDryFilter);
        }

        boolean newReverbEnabled = !uw;
        boolean oldReverbEnabled = waves.isReverbEnabled();
        if (oldReverbEnabled != newReverbEnabled) {
            System.out.println("reverb enabled : " + newReverbEnabled);
            waves.setReverbEnabled(newReverbEnabled);
        }
    }
}