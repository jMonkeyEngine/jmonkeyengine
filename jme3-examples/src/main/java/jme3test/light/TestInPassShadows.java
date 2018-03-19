/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.next.PreShadowArrayRenderer;
import com.jme3.system.AppSettings;

public class TestInPassShadows extends SimpleApplication {

    private DirectionalLight dl;
    private SpotLight sl;
    private PointLight pl;
    private PreShadowArrayRenderer psr;
    private ToneMapFilter tmf;
    
    public static void main(String[] args) {
        TestInPassShadows app = new TestInPassShadows();
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setGammaCorrection(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        renderManager.setPreferredLightMode(LightMode.SinglePassAndImageBased);
        renderManager.setSinglePassLightBatchSize(3);

        cam.setLocation(new Vector3f(8.079489f, 10.792628f, -6.714233f));
        cam.setRotation(new Quaternion(0.38442945f, -0.35025623f, 0.16050051f, 0.8389125f));
        flyCam.setMoveSpeed(5);
        
        tmf = new ToneMapFilter(new Vector3f(50, 50, 50));
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(tmf);
        viewPort.addProcessor(fpp);

        loadLights();
        loadScene();
        loadInputs();
    }

    private void loadLights() {
        AmbientLight al = new AmbientLight(new ColorRGBA(0.2f, 0.2f, 0.3f, 1.0f).mult(2f));
        rootNode.addLight(al);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -0.5f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1, 0.9f, 0.8f, 1).mult(2.5f));
        rootNode.addLight(dl);

        sl = new SpotLight();
        sl.setSpotRange(15);
        sl.setSpotInnerAngle(20 * FastMath.DEG_TO_RAD);
        sl.setSpotOuterAngle(25 * FastMath.DEG_TO_RAD);
        sl.setPosition(new Vector3f(-5.2193f, -0.5851393f, 4.831882f));
        sl.setDirection(new Vector3f(0.8429418f, -0.42458484f, -0.33041906f));
        sl.setColor(new ColorRGBA(0.5f, 0.7f, 1.0f, 1.0f).mult(50));
        rootNode.addLight(sl);

        pl = new PointLight(
                new Vector3f(-0.10135013f, 1.9986207f, -2.0745828f),
                new ColorRGBA(0.5f, 0.3f, 0.1f, 1f).mult(20),
                30);
        rootNode.addLight(pl);

        psr = new PreShadowArrayRenderer();
        psr.setTextureSize(512);
        psr.setPolyOffset(5, 0);
        psr.directional().setNumSplits(1);
        psr.addLight(dl);
        psr.addLight(sl);
        psr.addLight(pl);
        viewPort.addProcessor(psr);
    }

    private void loadScene() {
        Geometry box = new Geometry("Box", new Box(1, 1, 1));
        box.setShadowMode(ShadowMode.CastAndReceive);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        boxMat.setFloat("Roughness", 0.5f);
        boxMat.setFloat("Metallic", 0f);
        box.setMaterial(boxMat);
        rootNode.attachChild(box);
        
        Geometry box2 = box.clone(true);
        box2.move(3, 0, 0);
        rootNode.attachChild(box2);
        
        Geometry box3 = box.clone(true);
        box3.move(-3, 0, 0);
        rootNode.attachChild(box3);
        
        Geometry floor = new Geometry("floor", new Quad(100, 100));
        floor.rotate(-FastMath.HALF_PI, 0, 0);
        floor.center();
        floor.move(0, -1, 0);
        floor.setShadowMode(ShadowMode.Receive);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        floorMat.setFloat("Roughness", 0.5f);
        floorMat.setFloat("Metallic", 0f);
        floor.setMaterial(floorMat);
        rootNode.attachChild(floor);
    }

    private boolean moveLight = false;

    private void loadInputs() {
        inputManager.addMapping("MoveLight", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                moveLight = isPressed;
            }
        }, "MoveLight");
        
        inputManager.addMapping("OffsetFactorUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("OffsetFactorDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("OffsetUnitsUp", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("OffsetUnitsDown", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addListener(new AnalogListener() {
            private float factor, units;
            @Override
            public void onAnalog(String name, float value, float tpf) {
                switch (name) {
                    case "OffsetFactorUp":
                        factor += tpf * 5f;
                        break;
                    case "OffsetFactorDown":
                        factor -= tpf * 5f;
                        break;
                    case "OffsetUnitsUp":
                        units += tpf * 50f;
                        break;
                    case "OffsetUnitsDown":
                        units -= tpf * 50f;
                        break;
                }
                psr.setPolyOffset(factor, units);
                System.out.println("PolyOffset(" + factor + ", " + units + ")");
            }
            
        }, "OffsetFactorUp", "OffsetFactorDown", "OffsetUnitsUp", "OffsetUnitsDown");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (moveLight) {
            sl.setPosition(cam.getLocation());
            sl.setDirection(cam.getDirection());
            System.out.println(sl.getPosition());
            System.out.println(sl.getDirection());
        }
    }

}
