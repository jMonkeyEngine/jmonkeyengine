/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.effects;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.CenterQuad;
import com.jme3.scene.shape.Torus;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.texture.GlTexture;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

/**
 * @author Richard Tingle (aka richtea)
 */
public class TestIssue1773 extends ScreenshotTestBase{

    /**
     * Test case for Issue 1773 (Wrong particle position when using
     * 'EmitterMeshVertexShape' or 'EmitterMeshFaceShape' and worldSpace
     * flag equal to true)
     *
     * If the test succeeds, the particles will be generated from the vertices
     * (for EmitterMeshVertexShape) or from the faces (for EmitterMeshFaceShape)
     * of the torus mesh. If the test fails, the particles will appear in the
     * center of the torus when worldSpace flag is set to true.
     *
     */
    @ParameterizedTest(name = "Test Issue 1773 (emit in worldSpace = {0})")
    @ValueSource(booleans = {true, false})
    public void testIssue1773(boolean worldSpace, TestInfo testInfo){

        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + (worldSpace ? "_worldSpace" : "_localSpace");

        screenshotTest(new BaseAppState(){
            private ParticleEmitter emit;
            private Node myModel;

            AssetManager assetManager;

            Node rootNode;

            Camera cam;

            ViewPort viewPort;

            @Override
            public void initialize(Application app) {
                assetManager = app.getAssetManager();
                rootNode = ((SimpleApplication)app).getRootNode();
                cam = app.getCamera();
                viewPort = app.getViewPort();
                configCamera();
                setupLights();
                setupGround();
                setupCircle();
                createMotionControl();
            }

            @Override
            protected void cleanup(Application app){}

            @Override
            protected void onEnable(){}

            @Override
            protected void onDisable(){}

            /**
             * Crates particle emitter and adds it to root node.
             */
            private void setupCircle() {
                myModel = new Node("FieryCircle");

                Geometry torus = createTorus(1f);
                myModel.attachChild(torus);

                emit = createParticleEmitter(torus, true);
                myModel.attachChild(emit);

                rootNode.attachChild(myModel);
            }

            /**
             * Creates torus geometry used for the emitter shape.
             */
            private Geometry createTorus(float radius) {
                float s = radius / 8f;
                Geometry geo = new Geometry("CircleXZ", new Torus(64, 4, s, radius));
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", ColorRGBA.Blue);
                mat.getAdditionalRenderState().setWireframe(true);
                geo.setMaterial(mat);
                return geo;
            }

            /**
             * Creates a particle emitter that will emit the particles from
             * the given shape's vertices.
             */
            private ParticleEmitter createParticleEmitter(Geometry geo, boolean pointSprite) {
                ParticleMesh.Type type = pointSprite ? ParticleMesh.Type.Point : ParticleMesh.Type.Triangle;
                ParticleEmitter emitter = new ParticleEmitter("Emitter", type, 1000);
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
                mat.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
                mat.setBoolean("PointSprite", pointSprite);
                emitter.setMaterial(mat);
                emitter.setLowLife(1);
                emitter.setHighLife(1);
                emitter.setImagesX(15);
                emitter.setStartSize(0.04f);
                emitter.setEndSize(0.02f);
                emitter.setStartColor(ColorRGBA.Orange);
                emitter.setEndColor(ColorRGBA.Red);
                emitter.setParticlesPerSec(900);
                emitter.setGravity(0, 0f, 0);
                //emitter.getParticleInfluencer().setVelocityVariation(1);
                //emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0, .5f, 0));
                emitter.setShape(new EmitterMeshVertexShape(Arrays.asList(geo.getMesh())));
                emitter.setInWorldSpace(worldSpace);
                //emitter.setShape(new EmitterMeshFaceShape(Arrays.asList(geo.getMesh())));
                return emitter;
            }

            /**
             * Creates a motion control that will move particle emitter in
             * a circular path.
             */
            private void createMotionControl() {

                float radius = 5f;
                float height = 1.10f;

                MotionPath path = new MotionPath();
                path.setCycle(true);

                for (int i = 0; i < 8; i++) {
                    float x = FastMath.sin(FastMath.QUARTER_PI * i) * radius;
                    float z = FastMath.cos(FastMath.QUARTER_PI * i) * radius;
                    path.addWayPoint(new Vector3f(x, height, z));
                }
                MotionEvent motionControl = new MotionEvent(myModel, path);
                motionControl.setLoopMode(LoopMode.Loop);
                motionControl.setDirectionType(MotionEvent.Direction.Path);
                motionControl.play();
            }

            private void configCamera() {
                cam.setLocation(new Vector3f(0, 6f, 9.2f));
                cam.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_Y);

                float aspect = (float) cam.getWidth() / cam.getHeight();
                cam.setFrustumPerspective(45, aspect, 0.1f, 1000f);
            }

            /**
             * Adds a ground to the scene
             */
            private void setupGround() {
                CenterQuad quad = new CenterQuad(12, 12);
                quad.scaleTextureCoordinates(new Vector2f(2, 2));
                Geometry floor = new Geometry("Floor", quad);
                Material mat = new Material(assetManager, Materials.LIGHTING);
                GlTexture tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
                tex.setWrap(GlTexture.WrapMode.Repeat);
                mat.setTexture("DiffuseMap", tex);
                floor.setMaterial(mat);
                floor.rotate(-FastMath.HALF_PI, 0, 0);
                rootNode.attachChild(floor);
            }

            /**
             * Adds lights and filters
             */
            private void setupLights() {
                viewPort.setBackgroundColor(ColorRGBA.DarkGray);
                rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

                AmbientLight ambient = new AmbientLight();
                ambient.setColor(ColorRGBA.White);
                //rootNode.addLight(ambient);

                DirectionalLight sun = new DirectionalLight();
                sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
                sun.setColor(ColorRGBA.White);
                rootNode.addLight(sun);

                DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 4096, 3);
                dlsf.setLight(sun);
                dlsf.setShadowIntensity(0.4f);
                dlsf.setShadowZExtend(256);

                FXAAFilter fxaa = new FXAAFilter();
                BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);

                FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
                fpp.addFilter(bloom);
                fpp.addFilter(dlsf);
                fpp.addFilter(fxaa);
                viewPort.addProcessor(fpp);
            }


        }).setFramesToTakeScreenshotsOn(45)
          .setBaseImageFileName(imageName)
          .run();
    }
}
