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
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * Note that this example is only used to demonstrate the simple use of multi-target rendering. It is different from optimized deferred rendering.<br/>
 * @author JhonKkk
 * @date 2021年11月7日10点18分
 */
public class TestMultiRenderTarget2 extends SimpleApplication implements SceneProcessor {

    private FrameBuffer gBuffFrameBuffer;
    private Texture2D diffuseData, normalData, specularData, depthData;
    private Picture display1, display2, display3, display4;

    private Picture display;
    private Material mrtLightingMaterial;

    private String techOrig;
    private PointLight[] pls;

    public static void main(String[] args){
        TestMultiRenderTarget2 app = new TestMultiRenderTarget2();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        viewPort.addProcessor(this);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(4.8037705f, 4.851632f, 10.789033f));
        cam.setRotation(new Quaternion(-0.05143692f, 0.9483723f, -0.21131563f, -0.230846f));

        display1 = new Picture("Picture");
        display1.move(0, 0, -1);
        display2 = (Picture) display1.clone();
        display3 = (Picture) display1.clone();
        display4 = (Picture) display1.clone();
        display  = (Picture) display1.clone();

        ColorRGBA[] colors = new ColorRGBA[]{
                ColorRGBA.White,
                ColorRGBA.Blue,
                ColorRGBA.Cyan,
                ColorRGBA.DarkGray,
                ColorRGBA.Green,
                ColorRGBA.Magenta,
                ColorRGBA.Orange,
                ColorRGBA.Pink,
                ColorRGBA.Red,
                ColorRGBA.Yellow
        };

        // lights
        pls = new PointLight[20];
        for (int i = 0; i < pls.length; i++){
            PointLight pl = new PointLight();
            pl.setColor(colors[i % colors.length]);
            float angle = (float)Math.PI * (i + (timer.getTimeInSeconds() % 6)/3); // 3s for full loop
            pl.setPosition( new Vector3f(FastMath.cos(angle)*3f, 0,
                    FastMath.sin(angle)*3f));
            pl.setRadius(4.0f);
            display.addLight(pl);
            pls[i] = pl;
        }

        // test model
        Node scene = new Node("TestScene");
        Geometry model = (Geometry) ((Node)assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml")).getChild(0);
        MikktspaceTangentGenerator.generate(model);
        // GBuffer Pass
        Material tankMat = assetManager.loadMaterial("jme3test/post/tank.j3m");
        model.setMaterial(tankMat);
        scene.attachChild(model);
        // Create FrameBuffer for multi-target rendering
        initGBufferInfos();

        rootNode.attachChild(scene);
        guiViewPort.setClearFlags(true, true, true);
        guiNode.attachChild(display1);
        guiNode.attachChild(display2);
        guiNode.attachChild(display3);
        guiNode.attachChild(display4);
        guiNode.attachChild(display);
        guiNode.updateGeometricState();
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);//To change body of generated methods, choose Tools | Templates.
        // Motion light source
        for (int i = 0; i < pls.length; i++){
            PointLight pl = pls[i];
            float angle = (float)Math.PI * (i + (timer.getTimeInSeconds() % 6) / 3);
            pl.setPosition( new Vector3f(FastMath.cos(angle) * 3f, 0,
                    FastMath.sin(angle) * 3f));
        }
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        // do nothing
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // Delete FrameBuffer
        // ...

        // ReCreate GBuffer FrameBuffer
        diffuseData  = new Texture2D(w, h, Format.RGBA8);
        normalData   = new Texture2D(w, h, Format.RGBA8);
        specularData = new Texture2D(w, h, Format.RGBA8);
        depthData    = new Texture2D(w, h, Format.Depth);

        // MRTLightingMaterial
        // Light Pass
        mrtLightingMaterial = new Material(assetManager, "jme3test/post/MRTLighting.j3md");
        mrtLightingMaterial.selectTechnique("LightPass", renderManager);
        mrtLightingMaterial.setTexture("DiffuseMap",  diffuseData);
        mrtLightingMaterial.setTexture("SpecularMap", specularData);
        mrtLightingMaterial.setTexture("NormalMap",   normalData);
        mrtLightingMaterial.setTexture("DepthMap",    depthData);


        // display1,2,3,4 are used to display independent information in GBuff, and display is used to display the combined result.
        display.setMaterial(mrtLightingMaterial);
        display.setPosition(0, 0);
        display.setWidth(w);
        display.setHeight(h);

        display1.setTexture(assetManager, diffuseData, false);
        display2.setTexture(assetManager, normalData, false);
        display3.setTexture(assetManager, specularData, false);
        display4.setTexture(assetManager, depthData, false);

        display1.setPosition(0, 0);
        display2.setPosition(w/2, 0);
        display3.setPosition(0, h/2);
        display4.setPosition(w/2, h/2);

        display1.setWidth(w/2);
        display1.setHeight(h/2);

        display2.setWidth(w/2);
        display2.setHeight(h/2);

        display3.setWidth(w/2);
        display3.setHeight(h/2);

        display4.setWidth(w/2);
        display4.setHeight(h/2);

        guiNode.updateGeometricState();

        gBuffFrameBuffer = new FrameBuffer(w, h, 1);
        gBuffFrameBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depthData));
        gBuffFrameBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(diffuseData));
        gBuffFrameBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(normalData));
        gBuffFrameBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(specularData));
        gBuffFrameBuffer.setMultiTarget(true);
    }

    private void initGBufferInfos(){
        // the first time
        reshape(viewPort, viewPort.getCamera().getWidth(), viewPort.getCamera().getHeight());
    }

    @Override
    public boolean isInitialized() {
        return diffuseData != null;
    }

    @Override
    public void preFrame(float tpf) {
        viewPort.setOutputFrameBuffer(gBuffFrameBuffer);

        // Since ViewProjectionMatrixInverse is used in MRTLighting, I set it directly here.
        Matrix4f inverseViewProj = cam.getViewProjectionMatrix().invert();
        mrtLightingMaterial.setMatrix4("ViewProjectionMatrixInverse", inverseViewProj);
        techOrig = renderManager.getForcedTechnique();

        // use GBufPass Tech
        renderManager.setForcedTechnique("GBufPass");
    }

    @Override
    public void postQueue(RenderQueue rq) {
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // use Orig Tech
        renderManager.setForcedTechnique(techOrig);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

}
