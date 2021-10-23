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

package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

public class TestMultiRenderTarget extends SimpleApplication implements SceneProcessor {
    private FrameBuffer fb;
    private Picture display1, display2, display3, display4;
    private boolean initialized = false;

    public static void main(String[] args){
        TestMultiRenderTarget app = new TestMultiRenderTarget();
        app.start();
    }

    protected void buildScene() {
        Geometry cube1 = buildCube(ColorRGBA.Red);
        cube1.setLocalTranslation(-1f, 0f, 0f);
        Geometry cube2 = buildCube(ColorRGBA.Green);
        cube2.setLocalTranslation(0f, 0f, 0f);
        Geometry cube3 = buildCube(ColorRGBA.Blue);
        cube3.setLocalTranslation(1f, 0f, 0f);

        Geometry cube4 = buildCube(ColorRGBA.randomColor());
        cube4.setLocalTranslation(-0.5f, 1f, 0f);
        Geometry cube5 = buildCube(ColorRGBA.randomColor());
        cube5.setLocalTranslation(0.5f, 1f, 0f);

        rootNode.attachChild(cube1);
        rootNode.attachChild(cube2);
        rootNode.attachChild(cube3);
        rootNode.attachChild(cube4);
        rootNode.attachChild(cube5);
    }

    private Geometry buildCube(ColorRGBA color) {
        Geometry cube = new Geometry("Box", new Box(0.5f, 0.5f, 0.5f));
        Material mat = new Material(assetManager, "TestMRT/MatDefs/ExtractRGB.j3md");
        mat.setColor("Albedo", color);
        cube.setMaterial(mat);
        return cube;
    }

    @Override
    public void simpleInitApp() {
        viewPort.addProcessor(this);
        buildScene();

        display1 = new Picture("Picture");
        display1.move(0, 0, -1); // make it appear behind stats view
        display2 = (Picture) display1.clone();
        display3 = (Picture) display1.clone();
        display4 = (Picture) display1.clone();
    }

    @Override
    public void destroy() {
        viewPort.removeProcessor(this);
        super.destroy();
    }

    // Scene Processor from now on
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
        viewPort.setOutputFrameBuffer(fb);
        guiViewPort.setClearFlags(true, true, true);

        guiNode.attachChild(display1);
        guiNode.attachChild(display2);
        guiNode.attachChild(display3);
        guiNode.attachChild(display4);
        guiNode.updateGeometricState();
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // You can use multiple channel formats as well. That's why red is using RGBA8 as an example.
        Texture2D redTexture = new Texture2D(w, h, Format.RGBA8);
        Texture2D greenTexture = new Texture2D(w, h, Format.Luminance8);
        Texture2D blueTexture = new Texture2D(w, h, Format.Luminance8);
        Texture2D rgbTexture = new Texture2D(w, h, Format.RGBA8);

        fb = new FrameBuffer(w, h, 1);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(redTexture));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(greenTexture));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(blueTexture));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(rgbTexture));
        fb.setMultiTarget(true);

        display1.setTexture(assetManager, rgbTexture, false);
        display2.setTexture(assetManager, redTexture, false);
        display3.setTexture(assetManager, greenTexture, false);
        display4.setTexture(assetManager, blueTexture, false);

        display1.setPosition(0, 0);
        display1.setWidth(w/2f);
        display1.setHeight(h/2f);

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
