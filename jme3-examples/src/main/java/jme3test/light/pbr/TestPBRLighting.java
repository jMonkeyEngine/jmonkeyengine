/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.environment.util.*;
import com.jme3.light.LightProbe;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.MaterialDebugAppState;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * A test case for PBR lighting.
 * Still experimental.
 *
 * @author nehon
 */
public class TestPBRLighting extends SimpleApplication {

    public static void main(String[] args) {
        TestPBRLighting app = new TestPBRLighting();
        app.start();
    }

    private Node tex;
    private Node tex2;

    private Geometry model;
    private DirectionalLight dl;
    private Node modelNode;
    private int frame = 0;   
    private Material pbrMat;    

    @Override
    public void simpleInitApp() {
        assetManager.registerLoader(KTXLoader.class, "ktx");

        viewPort.setBackgroundColor(ColorRGBA.White);
        modelNode = (Node) new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        rootNode.attachChild(modelNode);

      
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(4.0f)));
//        fpp.addFilter(new SSAOFilter(0.5f, 3, 0.2f, 0.2f));
        viewPort.addProcessor(fpp);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);


        final EnvironmentCamera envCam = new EnvironmentCamera(128, new Vector3f(0, 3f, 0));
        stateManager.attach(envCam);
        
//        EnvironmentManager envManager = new EnvironmentManager();
//        stateManager.attach(envManager);
        
 //       envManager.setScene(rootNode);
        
        LightsDebugState debugState = new LightsDebugState();
        stateManager.attach(debugState);
        
        ChaseCamera chaser = new ChaseCamera(cam, modelNode, inputManager);
        chaser.setDragToRotate(true);
        chaser.setMinVerticalRotation(-FastMath.HALF_PI);
        chaser.setMaxDistance(1000);
        chaser.setSmoothMotion(true);
        chaser.setRotationSensitivity(10);
        chaser.setZoomSensitivity(5);
        flyCam.setEnabled(false);
        //flyCam.setMoveSpeed(100);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("debug") && isPressed) {
                    if (tex == null) {
                        return;
                    }
                    if (tex.getParent() == null && tex2.getParent() == null) {
                        guiNode.attachChild(tex);
                    } else if (tex2.getParent() == null){
                        tex.removeFromParent();
                        guiNode.attachChild(tex2);
                    } else {
                        tex2.removeFromParent();
                    }
                }

                if (name.equals("up") && isPressed) {
                    model.move(0, tpf * 100f, 0);
                }

                if (name.equals("down") && isPressed) {
                    model.move(0, -tpf * 100f, 0);
                }
                if (name.equals("left") && isPressed) {
                    model.move(0, 0, tpf * 100f);
                }
                if (name.equals("right") && isPressed) {
                    model.move(0, 0, -tpf * 100f);
                }
                if (name.equals("light") && isPressed) {
                    dl.setDirection(cam.getDirection().normalize());
                }
            }
        }, "toggle", "light", "up", "down", "left", "right", "debug");

        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("light", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_D));
        
        
        MaterialDebugAppState debug = new MaterialDebugAppState();
        debug.registerBinding("Common/MatDefs/Light/PBRLighting.frag", rootNode);
        getStateManager().attach(debug);

    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;

        if (frame == 2) {
            modelNode.removeFromParent();
            final LightProbe probe = LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {

                @Override
                public void done(LightProbe result) {
                    System.err.println("Done rendering env maps");
                    tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                    tex2 = EnvMapUtils.getCubeMapCrossDebugView(result.getIrradianceMap(), assetManager);
                }
            });
            ((BoundingSphere)probe.getBounds()).setRadius(100);
            rootNode.addLight(probe);
            //getStateManager().getState(EnvironmentManager.class).addEnvProbe(probe);
            
        }
        if (frame > 10 && modelNode.getParent() == null) {
            rootNode.attachChild(modelNode);
        }
    }

}

