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
package jme3test.model;

import com.jme3.anim.AnimComposer;
import com.jme3.app.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.*;
import com.jme3.renderer.Limits;
import com.jme3.scene.*;
import com.jme3.scene.debug.custom.ArmatureDebugAppState;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import jme3test.model.anim.EraseTimer;

import java.io.File;
import java.util.*;

public class TestGltfLoading extends SimpleApplication {

    private final Node autoRotate = new Node("autoRotate");
    private final List<Spatial> assets = new ArrayList<>();
    private Node probeNode;
    private float time = 0;
    private int assetIndex = 0;
    private boolean useAutoRotate = false;
    private final static String indentString = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
    private final int duration = 1;
    private boolean playAnim = true;
    private ChaseCameraAppState chaseCam;

    private final Queue<String> anims = new LinkedList<>();
    private AnimComposer composer;

    public static void main(String[] args) {
        TestGltfLoading app = new TestGltfLoading();
        app.start();
    }

    /**
     * WARNING This test case will try to load models from $HOME/glTF-Sample-Models, if the models is not
     * found there, it will automatically try to load it from the repository
     * https://github.com/KhronosGroup/glTF-Sample-Models .
     * 
     * Depending on the your connection speed and github rate limiting, this can be quite slow.
     */
    @Override
    public void simpleInitApp() {

        ArmatureDebugAppState armatureDebugappState = new ArmatureDebugAppState();
        getStateManager().attach(armatureDebugappState);
        setTimer(new EraseTimer());

        String folder = System.getProperty("user.home") + "/glTF-Sample-Models";
        if (new File(folder).exists()) {
            assetManager.registerLocator(folder, FileLocator.class);
        }
        assetManager.registerLocator(
                "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/refs/heads/main/",
                UrlLocator.class);

        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 100f);
        renderer.setDefaultAnisotropicFilter(Math.min(renderer.getLimits().get(Limits.TextureAnisotropy), 8));
        setPauseOnLostFocus(false);

        flyCam.setMoveSpeed(5);
        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(new ColorRGBA().setAsSrgb(0.2f, 0.2f, 0.2f, 1.0f));
        rootNode.attachChild(autoRotate);
        probeNode = (Node) assetManager.loadModel("Scenes/defaultProbe.j3o");
        autoRotate.attachChild(probeNode);

        chaseCam = new ChaseCameraAppState();
        getStateManager().attach(chaseCam);

        loadModelSample("BoomBox", "gltf");

        // loadModelSample("Duck", "gltf");
        // loadModelSample("Duck", "glb");
        // loadModelSample("ABeautifulGame", "gltf");
        // loadModelSample("Avocado", "glb");
        // loadModelSample("Avocado", "gltf");
        // loadModelSample("CesiumMilkTruck", "glb");
        // loadModelSample("VirtualCity", "glb");
        // loadModelSample("BrainStem", "glb");
        // loadModelSample("Lantern", "glb");
        // loadModelSample("RiggedFigure", "glb");
        // loadModelSample("SciFiHelmet", "gltf");
        // loadModelSample("DamagedHelmet", "gltf");
        // loadModelSample("AnimatedCube", "gltf");
        // loadModelSample("AntiqueCamera", "glb");
        // loadModelSample("AnimatedMorphCube", "glb");

        // DRACO SAMPLES

        // loadModelSample("Avocado", "draco");

        // FIXME: wrong texture coords?
        // loadModelSample("BarramundiFish", "draco");

        // loadModelSample("BoomBox", "draco");

        // FIXME: bad skinning?
        // loadModelSample("BrainStem", "draco");

        // FIXME: wrong offsets?
        // loadModelSample("CesiumMilkTruck", "draco");

        // FIXME: FAILS WITH INDEX OUT OF BOUND EXCEPTION
        // loadModelSample("VirtualCity", "draco");

        // loadModelSample("Corset", "draco");

        // FIXME: unclear
        // loadModelSample("Lantern", "draco");

        // loadModelSample("MorphPrimitivesTest", "draco");

        // FIXME: skinning?
        // loadModelSample("RiggedFigure", "draco");

        // FIXME: skinning?
        // loadModelSample("RiggedSimple", "draco");

        // FIXME: "dracoMesh" is null
        // loadModelSample("SunglassesKhronos", "draco");
        // loadModelSample("WaterBottle", "draco");

        probeNode.attachChild(assets.get(0));

        inputManager.addMapping("autorotate", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    useAutoRotate = !useAutoRotate;
                }
            }
        }, "autorotate");

        inputManager.addMapping("toggleAnim", new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    playAnim = !playAnim;
                    if (playAnim) {
                        playFirstAnim(rootNode);
                    } else {
                        stopAnim(rootNode);
                    }
                }
            }
        }, "toggleAnim");
        inputManager.addMapping("nextAnim", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && composer != null) {
                    String anim = anims.poll();
                    anims.add(anim);
                    composer.setCurrentAction(anim);
                }
            }
        }, "nextAnim");

        dumpScene(rootNode, 0);

        // stateManager.attach(new DetailedProfilerState());
    }

    private void loadModelSample(String name, String type) {
        String path = "Models/" + name;
        String ext = "gltf";
        switch (type) {
            case "draco":
                path += "/glTF-Draco/";
                ext = "gltf";
                break;
            case "glb":
                path += "/glTF-Binary/";
                ext = "glb";
                break;
            default:
                path += "/glTF/";
                ext = "gltf";
                break;
        }
        path += name + "." + ext;

        Spatial s = loadModel(path, new Vector3f(0, 0, 0), 1f);

        BoundingBox bbox = (BoundingBox) s.getWorldBound();

        float maxExtent = Math.max(bbox.getXExtent(), Math.max(bbox.getYExtent(), bbox.getZExtent()));
        if (maxExtent < 10f) {
            s.scale(10f / maxExtent);
            maxExtent = 10f;
        }
        float distance = 50f;

        chaseCam.setTarget(s);
        chaseCam.setInvertHorizontalAxis(true);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setZoomSpeed(0.5f);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI);
        chaseCam.setRotationSpeed(3);
        chaseCam.setDefaultDistance(distance);
        chaseCam.setMaxDistance(distance * 10);
        chaseCam.setDefaultVerticalRotation(0.3f);

    }

    private Spatial loadModel(String path, Vector3f offset, float scale) {
        return loadModel(path, offset, new Vector3f(scale, scale, scale));
    }

    private Spatial loadModel(String path, Vector3f offset, Vector3f scale) {
        System.out.println("Loading model: " + path);
        GltfModelKey k = new GltfModelKey(path);
        // k.setKeepSkeletonPose(true);
        long t = System.currentTimeMillis();
        Spatial s = assetManager.loadModel(k);
        System.out.println("Load time : " + (System.currentTimeMillis() - t) + " ms");

        s.scale(scale.x, scale.y, scale.z);
        s.move(offset);
        assets.add(s);
        if (playAnim) {
            playFirstAnim(s);
        }

        return s;
    }

    private void playFirstAnim(Spatial s) {

        AnimComposer control = s.getControl(AnimComposer.class);
        if (control != null) {
            anims.clear();
            for (String name : control.getAnimClipsNames()) {
                anims.add(name);
            }
            if (anims.isEmpty()) {
                return;
            }
            String anim = anims.poll();
            anims.add(anim);
            control.setCurrentAction(anim);
            composer = control;
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                playFirstAnim(spatial);
            }
        }
    }

    private void stopAnim(Spatial s) {

        AnimComposer control = s.getControl(AnimComposer.class);
        if (control != null) {
            control.reset();
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                stopAnim(spatial);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!useAutoRotate) {
            return;
        }
        time += tpf;
        // autoRotate.rotate(0, tpf * 0.5f, 0);
        if (time > duration) {
            // morphIndex++;
            // setMorphTarget(morphIndex);
            assets.get(assetIndex).removeFromParent();
            assetIndex = (assetIndex + 1) % assets.size();
            // if (assetIndex == 0) {
            // duration = 10;
            // }
            probeNode.attachChild(assets.get(assetIndex));
            time = 0;
        }
    }

    private void dumpScene(Spatial s, int indent) {
        System.err.println(indentString.substring(0, indent) + s.getName() + " ("
                + s.getClass().getSimpleName() + ") / " + s.getLocalTransform().getTranslation().toString()
                + ", " + s.getLocalTransform().getRotation().toString() + ", "
                + s.getLocalTransform().getScale().toString());
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                dumpScene(spatial, indent + 1);
            }
        }
    }
}
