/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import java.io.File;

public class TestMultiplesFilters extends SimpleApplication {

    private static boolean useHttp = false;

    public static void main(String[] args) {
        File file = new File("wildhouse.zip");
        if (!file.exists()) {
            useHttp = true;
        }
        TestMultiplesFilters app = new TestMultiplesFilters();
        app.start();
    }
    SSAOFilter ssaoFilter;
    FilterPostProcessor fpp;
    boolean en = true;

    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(6.0344796f, 1.5054002f, 55.572033f));
        cam.setRotation(new Quaternion(0.0016069f, 0.9810479f, -0.008143323f, 0.19358753f));

        // load sky
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

        // create the geometry and attach it
        // load the level from zip or http zip
        if (useHttp) {
            assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip", HttpZipLocator.class.getName());
        } else {
            assetManager.registerLocator("wildhouse.zip", ZipLocator.class.getName());
        }
        Spatial scene = assetManager.loadModel("main.scene");


        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4790551f, -0.39247334f, -0.7851566f));
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        scene.addLight(sun);

        fpp = new FilterPostProcessor(assetManager);
      //  fpp.setNumSamples(4);
        ssaoFilter = new SSAOFilter(0.92f, 2.2f, 0.46f, 0.2f);
        final WaterFilter water=new WaterFilter(rootNode,new Vector3f(-0.4790551f, -0.39247334f, -0.7851566f));
        water.setWaterHeight(-20);
        SSAOUI ui=new SSAOUI(inputManager,ssaoFilter);
        final BloomFilter bloom = new BloomFilter();
        final ColorOverlayFilter overlay = new ColorOverlayFilter(ColorRGBA.LightGray);
        

        fpp.addFilter(ssaoFilter);
        
        fpp.addFilter(water);

        fpp.addFilter(bloom);

        fpp.addFilter(overlay);

        viewPort.addProcessor(fpp);

        rootNode.attachChild(scene);
        
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("toggleSSAO".equals(name) && isPressed) {
                    if (ssaoFilter.isEnabled()) {
                        ssaoFilter.setEnabled(false);
                    } else {
                        ssaoFilter.setEnabled(true);
                    }
                }
                if ("toggleWater".equals(name) && isPressed) {
                    if (water.isEnabled()) {
                        water.setEnabled(false);
                    } else {
                        water.setEnabled(true);
                    }
                }
                if ("toggleBloom".equals(name) && isPressed) {
                    if (bloom.isEnabled()) {
                        bloom.setEnabled(false);
                    } else {
                        bloom.setEnabled(true);
                    }
                }
                if ("toggleOverlay".equals(name) && isPressed) {
                    if (overlay.isEnabled()) {
                        overlay.setEnabled(false);
                    } else {
                        overlay.setEnabled(true);
                    }
                }
            }
        }, "toggleSSAO", "toggleBloom", "toggleWater","toggleOverlay");
        inputManager.addMapping("toggleSSAO", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("toggleWater", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("toggleBloom", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("toggleOverlay", new KeyTrigger(KeyInput.KEY_4));
        
    }
}
