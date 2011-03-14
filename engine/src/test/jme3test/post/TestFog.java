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
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import java.io.File;

public class TestFog extends SimpleApplication {

    private FilterPostProcessor fpp;
    private boolean enabled=true;
    private FogFilter fog;

    // set default for applets
    private static boolean useHttp = true;

    public static void main(String[] args) {
        File file = new File("wildhouse.zip");
        if (file.exists()) {
            useHttp = false;
        }
        TestFog app = new TestFog();
        app.start();
    }

    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(10);
        Node mainScene=new Node();
        cam.setLocation(new Vector3f(-27.0f, 1.0f, 75.0f));
        cam.setRotation(new Quaternion(0.03f, 0.9f, 0f, 0.4f));

        // load sky
        mainScene.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

        // create the geometry and attach it
        // load the level from zip or http zip
        if (useHttp) {
            assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip", HttpZipLocator.class.getName());
        } else {
            assetManager.registerLocator("wildhouse.zip", ZipLocator.class.getName());
        }
        Spatial scene = assetManager.loadModel("main.scene");

        DirectionalLight sun = new DirectionalLight();
        Vector3f lightDir=new Vector3f(-0.37352666f, -0.50444174f, -0.7784704f);
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        scene.addLight(sun);


        mainScene.attachChild(scene);
        rootNode.attachChild(mainScene);

        fpp=new FilterPostProcessor(assetManager);
        //fpp.setNumSamples(4);
        fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(155);
        fog.setFogDensity(2.0f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
        initInputs();
    }

     private void initInputs() {
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("DensityUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("DensityDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("DistanceUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("DistanceDown", new KeyTrigger(KeyInput.KEY_J));


        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("toggle") && keyPressed) {
                    if(enabled){
                        enabled=false;
                        viewPort.removeProcessor(fpp);
                    }else{
                        enabled=true;
                        viewPort.addProcessor(fpp);
                    }
                }

            }
        };

        AnalogListener anl=new AnalogListener() {

            public void onAnalog(String name, float isPressed, float tpf) {
                if(name.equals("DensityUp")){
                    fog.setFogDensity(fog.getFogDensity()+0.001f);
                    System.out.println("Fog density : "+fog.getFogDensity());
                }
                if(name.equals("DensityDown")){
                    fog.setFogDensity(fog.getFogDensity()-0.010f);
                    System.out.println("Fog density : "+fog.getFogDensity());
                }
                if(name.equals("DistanceUp")){
                    fog.setFogDistance(fog.getFogDistance()+0.5f);
                    System.out.println("Fog Distance : "+fog.getFogDistance());
                }
                if(name.equals("DistanceDown")){
                    fog.setFogDistance(fog.getFogDistance()-0.5f);
                    System.out.println("Fog Distance : "+fog.getFogDistance());
                }

            }
        };

        inputManager.addListener(acl, "toggle");
        inputManager.addListener(anl, "DensityUp","DensityDown","DistanceUp","DistanceDown");

    }
}

