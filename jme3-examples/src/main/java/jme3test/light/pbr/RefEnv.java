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
package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;

/**
 * test
 *
 * @author nehon
 */
public class RefEnv extends SimpleApplication {

    private Node tex;
    private Node ref;
    private Picture refImg;

    public static void main(String[] args) {
        RefEnv app = new RefEnv();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        cam.setLocation(new Vector3f(-17.95047f, 4.917353f, -17.970531f));
        cam.setRotation(new Quaternion(0.11724457f, 0.29356146f, -0.03630452f, 0.94802815f));
//        cam.setLocation(new Vector3f(14.790441f, 7.164179f, 19.720007f));
//        cam.setRotation(new Quaternion(-0.038261678f, 0.9578362f, -0.15233073f, -0.24058504f));
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(5);
        Spatial sc = assetManager.loadModel("Scenes/PBR/ref/scene.gltf");
        rootNode.attachChild(sc);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        rootNode.getChild(0).setCullHint(Spatial.CullHint.Always);

        ref = new Node("reference pictures");
        refImg = new Picture("refImg");
        refImg.setHeight(cam.getHeight());
        refImg.setWidth(cam.getWidth());
        refImg.setImage(assetManager, "jme3test/light/pbr/ref.png", false);

        ref.attachChild(refImg);

        stateManager.attach(new EnvironmentCamera(256, Vector3f.ZERO));

        inputManager.addMapping("tex", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("switch", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("ref", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(new ActionListener() {

            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("tex") && isPressed) {
                    if (tex == null) {
                        tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(stateManager.getState(EnvironmentCamera.class).debugEnv, assetManager);
                    }
                    if (tex.getParent() == null) {
                        guiNode.attachChild(tex);
                    } else {
                        tex.removeFromParent();
                    }
                }

                if (name.equals("switch") && isPressed) {
                    switchMat(rootNode.getChild("Scene"));
                }
                if (name.equals("ref") && isPressed) {
                    if (ref.getParent() == null) {
                        guiNode.attachChild(ref);
                    } else {
                        ref.removeFromParent();
                    }
                }
            }
        }, "tex", "switch", "ref");

    }

    private void switchMat(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial children : n.getChildren()) {
                switchMat(children);
            }
        } else if (s instanceof Geometry) {
            Geometry g = (Geometry) s;
            Material mat = g.getMaterial();
            if (((Float) mat.getParam("Metallic").getValue()) == 1f) {
                mat.setFloat("Metallic", 0);
                mat.setColor("BaseColor", ColorRGBA.Black);
                ref.attachChild(refImg);
            } else {
                mat.setFloat("Metallic", 1);
                mat.setColor("BaseColor", ColorRGBA.White);
                refImg.removeFromParent();
            }
        }
    }

    private int frame = 0;

    @Override
    public void simpleUpdate(float tpf) {
        frame++;

        EnvironmentCamera eCam = stateManager.getState(EnvironmentCamera.class);
        if (frame == 2) {
            final LightProbe probe = LightProbeFactory.makeProbe(eCam, rootNode, EnvMapUtils.GenerationType.Fast, new JobProgressAdapter<LightProbe>() {

                @Override
                public void done(LightProbe result) {
                    System.err.println("Done rendering env maps");
                    tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                    rootNode.getChild(0).setCullHint(Spatial.CullHint.Dynamic);
                }
            });
            probe.getArea().setRadius(100);
            rootNode.addLight(probe);

        }

        if (eCam.isBusy()) {
            System.out.println("EnvironmentCamera busy as of frame " + frame);
        }
    }
}
