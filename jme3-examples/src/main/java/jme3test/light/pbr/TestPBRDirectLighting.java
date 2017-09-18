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

import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.environment.util.LightsDebugState;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.scene.*;
import com.jme3.scene.shape.Sphere;
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
public class TestPBRDirectLighting extends SimpleApplication {

    public static void main(String[] args) {
        TestPBRDirectLighting app = new TestPBRDirectLighting();
        app.start();
    }

    private DirectionalLight dl;

    private float roughness = 0.0f;

    @Override
    public void simpleInitApp() {


        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);

        ChaseCameraAppState chaser = new ChaseCameraAppState();
        chaser.setDragToRotate(true);
        chaser.setMinVerticalRotation(-FastMath.HALF_PI);
        chaser.setMaxDistance(1000);
        chaser.setInvertVerticalAxis(true);
        getStateManager().attach(chaser);
        chaser.setTarget(rootNode);
        flyCam.setEnabled(false);

        Geometry sphere = new Geometry("sphere", new Sphere(32, 32, 1));
        final Material m = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        m.setColor("BaseColor", ColorRGBA.Black);
        m.setFloat("Metallic", 0f);
        m.setFloat("Roughness", roughness);
        sphere.setMaterial(m);
        rootNode.attachChild(sphere);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {

                if (name.equals("rup") && isPressed) {
                    roughness = FastMath.clamp(roughness + 0.1f, 0.0f, 1.0f);
                    m.setFloat("Roughness", roughness);
                }
                if (name.equals("rdown") && isPressed) {
                    roughness = FastMath.clamp(roughness - 0.1f, 0.0f, 1.0f);
                    m.setFloat("Roughness", roughness);
                }

                if (name.equals("light") && isPressed) {
                    dl.setDirection(cam.getDirection().normalize());
                }
            }
        }, "light", "rup", "rdown");


        inputManager.addMapping("light", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("rup", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("rdown", new KeyTrigger(KeyInput.KEY_DOWN));


    }

    @Override
    public void simpleUpdate(float tpf) {
    }

}

