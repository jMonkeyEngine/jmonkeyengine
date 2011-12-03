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
package jme3test.water;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import java.io.File;

public class TestPostWaterLake extends SimpleApplication {

    // set default for applets
    private static boolean useHttp = true;

    public static void main(String[] args) {
     
        TestPostWaterLake app = new TestPostWaterLake();
        app.start();
    }

    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(-27.0f, 1.0f, 75.0f));
      //  cam.setRotation(new Quaternion(0.03f, 0.9f, 0f, 0.4f));

        // load sky
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

        File file = new File("wildhouse.zip");
        
        if (file.exists()) {
            useHttp = false;
        }
        // create the geometry and attach it
        // load the level from zip or http zip
        if (useHttp) {
            assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip", HttpZipLocator.class.getName());
        } else {
            assetManager.registerLocator("wildhouse.zip", ZipLocator.class.getName());
        }
        Spatial scene = assetManager.loadModel("main.scene");
        rootNode.attachChild(scene);

        DirectionalLight sun = new DirectionalLight();
        Vector3f lightDir = new Vector3f(-0.37352666f, -0.50444174f, -0.7784704f);
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        scene.addLight(sun);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);        
        final WaterFilter water = new WaterFilter(rootNode, lightDir);
        water.setWaterHeight(-20);
        water.setUseFoam(false);
        water.setUseRipples(false);
        water.setDeepWaterColor(ColorRGBA.Brown);
        water.setWaterColor(ColorRGBA.Brown.mult(2.0f));
        water.setWaterTransparency(0.2f);
        water.setMaxAmplitude(0.3f);
        water.setWaveScale(0.008f);
        water.setSpeed(0.7f);
        water.setShoreHardness(1.0f);
        water.setRefractionConstant(0.2f);
        water.setShininess(0.3f);
        water.setSunScale(1.0f);
        water.setColorExtinction(new Vector3f(10.0f, 20.0f, 30.0f));
        fpp.addFilter(water);
        viewPort.addProcessor(fpp);

        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
              if(isPressed){
                  if(water.isUseHQShoreline()){
                      water.setUseHQShoreline(false);
                  }else{
                      water.setUseHQShoreline(true);
                  }
              }
            }
        }, "HQ");

        inputManager.addMapping("HQ", new KeyTrigger(keyInput.KEY_SPACE));
    }
}
