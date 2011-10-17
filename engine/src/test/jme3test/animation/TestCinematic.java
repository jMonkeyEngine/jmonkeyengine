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
package jme3test.animation;

import com.jme3.cinematic.Cinematic;
import com.jme3.animation.LoopMode;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.MotionTrack;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.events.SoundTrack;
import com.jme3.app.SimpleApplication;
import com.jme3.cinematic.events.AbstractCinematicEvent;
import com.jme3.cinematic.events.AnimationTrack;
import com.jme3.cinematic.PlayState;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.cinematic.events.PositionTrack;
import com.jme3.cinematic.events.RotationTrack;
import com.jme3.cinematic.events.ScaleTrack;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FadeFilter;
import com.jme3.renderer.Caps;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.PssmShadowRenderer;

public class TestCinematic extends SimpleApplication {

    private Spatial model;
    private Spatial teapot;
    private MotionPath path;
    private MotionTrack cameraMotionTrack;
    private Cinematic cinematic;
    private ChaseCamera chaseCam;
    private FilterPostProcessor fpp;
    private FadeFilter fade;

    public static void main(String[] args) {
        TestCinematic app = new TestCinematic();
        app.start();

    }

    @Override
    public void simpleInitApp() {
        //just some text
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        final BitmapText text = new BitmapText(guiFont, false);
        text.setSize(guiFont.getCharSet().getRenderedSize());
        text.setText("Press enter to play/pause cinematic");
        text.setLocalTranslation((cam.getWidth() - text.getLineWidth()) / 2, cam.getHeight(), 0);
        guiNode.attachChild(text);


        createScene();

        cinematic = new Cinematic(rootNode, 20);
        cinematic.bindUi("Interface/Nifty/CinematicTest.xml");
        stateManager.attach(cinematic);

        createCameraMotion();


        cinematic.addCinematicEvent(0, new AbstractCinematicEvent() {

            @Override
            public void onPlay() {
                fade.setValue(0);
                fade.fadeIn();
            }

            @Override
            public void onUpdate(float tpf) {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onPause() {
            }
        });
        cinematic.addCinematicEvent(0, new PositionTrack(teapot, new Vector3f(10, 0, 10), 0));
        cinematic.addCinematicEvent(0, new ScaleTrack(teapot, new Vector3f(1, 1, 1), 0));
        Quaternion q = new Quaternion();
        q.loadIdentity();
        cinematic.addCinematicEvent(0, new RotationTrack(teapot, q, 0));

        cinematic.addCinematicEvent(0, new PositionTrack(teapot, new Vector3f(10, 0, -10), 20));
        cinematic.addCinematicEvent(0, new ScaleTrack(teapot, new Vector3f(4, 4, 4), 10));
        cinematic.addCinematicEvent(10, new ScaleTrack(teapot, new Vector3f(1, 1, 1), 10));
        Quaternion rotation2 = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);
        cinematic.addCinematicEvent(0, new RotationTrack(teapot, rotation2, 20));

        cinematic.activateCamera(0, "aroundCam");
        cinematic.addCinematicEvent(0, cameraMotionTrack);
        cinematic.addCinematicEvent(0, new SoundTrack("Sound/Environment/Nature.ogg", LoopMode.Loop));
        cinematic.addCinematicEvent(3, new SoundTrack("Sound/Effects/kick.wav"));
        cinematic.addCinematicEvent(3, new SubtitleTrack("start", 3, "jMonkey engine really kicks A..."));
        cinematic.addCinematicEvent(5.0f, new SoundTrack("Sound/Effects/Beep.ogg", 1));
        cinematic.addCinematicEvent(6, new AnimationTrack(model, "Walk", LoopMode.Loop));
        cinematic.activateCamera(6, "topView");
        cinematic.activateCamera(10, "aroundCam");

        cinematic.addCinematicEvent(19, new AbstractCinematicEvent() {

            @Override
            public void onPlay() {
                fade.fadeOut();
            }

            @Override
            public void onUpdate(float tpf) {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onPause() {
            }
        });

        cinematic.addListener(new CinematicEventListener() {

            public void onPlay(CinematicEvent cinematic) {
                chaseCam.setEnabled(false);
                System.out.println("play");
            }

            public void onPause(CinematicEvent cinematic) {
                chaseCam.setEnabled(true);
                System.out.println("pause");
            }

            public void onStop(CinematicEvent cinematic) {
                chaseCam.setEnabled(true);
                fade.setValue(1);
                System.out.println("stop");
            }
        });

        flyCam.setEnabled(false);
        chaseCam = new ChaseCamera(cam, model, inputManager);
        initInputs();

    }

    private void createCameraMotion() {

        CameraNode camNode = cinematic.bindCamera("topView", cam);
        camNode.setLocalTranslation(new Vector3f(0, 50, 0));
        camNode.lookAt(model.getLocalTranslation(), Vector3f.UNIT_Y);

        CameraNode camNode2 = cinematic.bindCamera("aroundCam", cam);
        path = new MotionPath();
        path.setCycle(true);
        path.addWayPoint(new Vector3f(20, 3, 0));
        path.addWayPoint(new Vector3f(0, 3, 20));
        path.addWayPoint(new Vector3f(-20, 3, 0));
        path.addWayPoint(new Vector3f(0, 3, -20));
        path.setCurveTension(0.83f);
        cameraMotionTrack = new MotionTrack(camNode2, path);
        cameraMotionTrack.setLoopMode(LoopMode.Loop);
        cameraMotionTrack.setLookAt(model.getWorldTranslation(), Vector3f.UNIT_Y);
        cameraMotionTrack.setDirectionType(MotionTrack.Direction.LookAt);

    }

    private void createScene() {

        model = (Spatial) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        model.center();
        model.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(model);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Cyan);

        teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalTranslation(10, 0, 10);
        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(teapot);

        Material matSoil = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        matSoil.setBoolean("UseMaterialColors", true);
        matSoil.setColor("Ambient", ColorRGBA.Gray);
        matSoil.setColor("Diffuse", ColorRGBA.Green);
        matSoil.setColor("Specular", ColorRGBA.Black);

        Geometry soil = new Geometry("soil", new Box(new Vector3f(0, -6.0f, 0), 50, 1, 50));
        soil.setMaterial(matSoil);
        soil.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(soil);
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(0, -1, -1).normalizeLocal());
        light.setColor(ColorRGBA.White.mult(1.5f));
        rootNode.addLight(light);

        fpp = new FilterPostProcessor(assetManager);
        fade = new FadeFilter();
        fpp.addFilter(fade);
        
        if (renderer.getCaps().contains(Caps.GLSL100)){
            PssmShadowRenderer pssm = new PssmShadowRenderer(assetManager, 512, 1);
            pssm.setDirection(new Vector3f(0, -1, -1).normalizeLocal());
            pssm.setShadowIntensity(0.4f);
            viewPort.addProcessor(pssm);
            viewPort.addProcessor(fpp);
        }
    }

    private void initInputs() {
        inputManager.addMapping("togglePause", new KeyTrigger(keyInput.KEY_RETURN));
        ActionListener acl = new ActionListener() {
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("togglePause") && keyPressed) {
                    if (cinematic.getPlayState() == PlayState.Playing) {
                        cinematic.pause();
                    } else {
                        cinematic.play();
                    }
                }

            }
        };
        inputManager.addListener(acl, "togglePause");
    }
}
