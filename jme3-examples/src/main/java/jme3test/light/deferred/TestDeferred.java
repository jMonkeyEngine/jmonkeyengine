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

package jme3test.light.deferred;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * An example implementation of deferred rendering, where lighting is done after the initial geometry rendering.<br />
 * It is by no means a competitive rendering implementation to be used on reallife applications, because it lacks the
 * critical performance feature of light culling and is only implemented with a very limited lighting model and a
 * hardcoded ambience light (and a hardcoded number of lights to be used by the renderer).<br />
 * Furthermore, it renders to a second viewport instead of the display viewport, so that the rendering result can be
 * used as a GUI Picture (PiP) instead.<br />
 *
 * When SSBO/UBO is properly working, you should be looking into using them instead and invoking the filter
 * (fragment shader) multiple times, in case you exceed the hardcoded number of lights (UBO: You need to choose them,
 * so that sizeof(UBO) <= 16 kiB).
 * @author MeFisto94
 */
public class TestDeferred extends SimpleApplication {
    private Picture display1, display2, display3, display4;
    private FilterPostProcessor fpp;
    private ViewPort gBufferViewPort, lightingViewPort;

    public static void main(String[] args){
        TestDeferred app = new TestDeferred();
        app.start();
    }

    protected Spatial buildScene() {
        Node root = new Node("GeometryRoot");

        Geometry cube1 = buildCube(ColorRGBA.Red);
        cube1.setLocalTranslation(-1f, 0f, 0f);
        Geometry cube2 = buildCube(ColorRGBA.Green);
        cube2.setLocalTranslation(0f, 0f, 0f);
        Geometry cube3 = buildCube(ColorRGBA.Blue);
        cube3.setLocalTranslation(1f, 0f, 0f);

        ColorRGBA color4 = ColorRGBA.White;
        Geometry cube4 = buildCube(color4);
        cube4.setLocalTranslation(-0.5f, 1f, 0f);
        ColorRGBA color5 = ColorRGBA.Yellow;
        Geometry cube5 = buildCube(color5);
        cube5.setLocalTranslation(0.5f, 1f, 0f);

        root.attachChild(cube1);
        root.attachChild(cube2);
        root.attachChild(cube3);
        root.attachChild(cube4);
        root.attachChild(cube5);

        root.addLight(new PointLight(cube1.getLocalTranslation(), ColorRGBA.Red, 4f));
        root.addLight(new PointLight(cube2.getLocalTranslation(), ColorRGBA.Green, 4f));
        root.addLight(new PointLight(cube3.getLocalTranslation(), ColorRGBA.Blue, 4f));
        root.addLight(new PointLight(cube4.getLocalTranslation(), color4, 4f));
        root.addLight(new PointLight(cube5.getLocalTranslation(), color5, 4f));

        return root;
    }

    private Geometry buildCube(ColorRGBA color) {
        Geometry cube = new Geometry("Box", new Sphere(32, 32, 0.5f));
        Material mat = new Material(assetManager, "TestDeferred/MatDefs/GeometryPass.j3md");
        mat.setColor("Albedo", color);
        cube.setMaterial(mat);
        return cube;
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(0f, 0.5f, 3f));

        // Geometry Pass
        gBufferViewPort = renderManager.createMainView("GBuffer", getCamera());
        gBufferViewPort.setClearFlags(true, true, true);
        gBufferViewPort.addProcessor(new GBufferSceneProcessor());
        gBufferViewPort.attachScene(buildScene());

        // Lighting pass
        // This uses its own viewPort, because we don't want to render the output to the display fb, but instead to
        // another GUI Picture / Texture, so we can display it as a fourth PiP element. Typically, you'd add the filter
        // to your main post FX stack and make it directly render to the screen instead. Note that we specifically
        // require a dedicated viewport (instead of just setting the output texture), because GUI is rendered _before_
        // Filter passes(!)
        lightingViewPort = renderManager.createMainView("LightingPass", getCamera());
        lightingViewPort.setClearFlags(true, true, true);
        lightingViewPort.addProcessor(new LightingSceneProcessor());

        fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(new DeferredLightingPass((Node)gBufferViewPort.getScenes().get(0)));
        lightingViewPort.addProcessor(fpp);

        display1 = new Picture("Picture");
        display1.move(0, 0, -1); // make it appear behind stats view
        display2 = (Picture) display1.clone();
        display3 = (Picture) display1.clone();
        display4 = (Picture) display1.clone();
    }

    @Override
    public void destroy() {
        gBufferViewPort.clearProcessors();
        lightingViewPort.clearProcessors();
        super.destroy();
    }

    @Override
    public void simpleUpdate(float tpf) {
        for (Spatial spatial : gBufferViewPort.getScenes()) {
            spatial.updateLogicalState(tpf);
        }

        for (Spatial spatial : gBufferViewPort.getScenes()) {
            spatial.updateGeometricState();
        }
    }

    private class GBufferSceneProcessor implements SceneProcessor {
        private boolean initialized = false;
        private FrameBuffer gBuffer;

        // Scene Processor from now on
        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
            gBufferViewPort.setOutputFrameBuffer(gBuffer);
            guiViewPort.setClearFlags(true, true, true);

            guiNode.attachChild(display1);
            guiNode.attachChild(display2);
            guiNode.attachChild(display3);
            guiNode.attachChild(display4);
            guiNode.updateGeometricState();
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
            Texture2D positionTexture = new Texture2D(w, h, Format.RGBA16F);
            Texture2D normalsTexture = new Texture2D(w, h, Format.RGBA16F);
            Texture2D albedoTexture = new Texture2D(w, h, Format.RGBA16F);

            gBuffer = new FrameBuffer(w, h, 1);
            gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(positionTexture));
            gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(normalsTexture));
            gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(albedoTexture));
            gBuffer.setMultiTarget(true);

            display2.setTexture(assetManager, positionTexture, false);
            display3.setTexture(assetManager, normalsTexture, false);
            display4.setTexture(assetManager, albedoTexture, false);

            display2.setPosition(0, h/2f);
            display2.setWidth(w/2f);
            display2.setHeight(h/2f);

            display3.setPosition(w/2f, h/2f);
            display3.setWidth(w/2f);
            display3.setHeight(h/2f);

            display4.setPosition(w/2f, 0f);
            display4.setWidth(w/2f);
            display4.setHeight(h/2f);

            guiNode.updateGeometricState();

            Material mat = new Material(assetManager, "TestDeferred/MatDefs/LightingPass.j3md");
            mat.setTexture("WorldPosition", positionTexture);
            mat.setTexture("Normal", normalsTexture);
            mat.setTexture("Albedo", albedoTexture);
            fpp.getFilter(DeferredLightingPass.class).setMaterial(mat);

            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
        }

        @Override
        public void cleanup() {
            initialized = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
            // not implemented
        }
    }

    private class LightingSceneProcessor implements SceneProcessor {
        private boolean initialized = false;
        private FrameBuffer lightingBuffer;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
            lightingViewPort.setOutputFrameBuffer(lightingBuffer);

            guiNode.attachChild(display1);
            guiNode.updateGeometricState();
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
            Texture2D outputTexture = new Texture2D(w, h, Format.RGB111110F);
            lightingBuffer = new FrameBuffer(w, h, 1);
            lightingBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(outputTexture));
            display1.setTexture(assetManager, outputTexture, false);
            display1.setPosition(0, 0);
            display1.setWidth(w/2f);
            display1.setHeight(h/2f);

            guiNode.updateGeometricState();
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
        }

        @Override
        public void cleanup() {
            initialized = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
            // not implemented
        }
    }
}
