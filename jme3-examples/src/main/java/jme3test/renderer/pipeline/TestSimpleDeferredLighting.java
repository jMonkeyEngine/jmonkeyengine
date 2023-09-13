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

package jme3test.renderer.pipeline;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class TestSimpleDeferredLighting extends SimpleApplication implements ActionListener {
    private RenderManager.RenderPath currentRenderPath;
    private Material mat;
    private BitmapText hitText;

    private int sceneId;

    private float angle;
    private float angles[];
    private PointLight pl;
    private PointLight pls[];
    private Spatial lightMdl;
    private Geometry lightMdls[];

    public static void main(String[] args){
        TestSimpleDeferredLighting app = new TestSimpleDeferredLighting();
        app.start();
    }
    private void testScene1(){
        sceneId = 0;
        Geometry teapot = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        TangentBinormalGenerator.generate(teapot.getMesh(), true);

        teapot.setLocalScale(2f);
        renderManager.setSinglePassLightBatchSize(1);
        mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        m_currentTechnique = TechniqueDef.DEFAULT_TECHNIQUE_NAME;
//        mat.selectTechnique(m_currentTechnique, getRenderManager());
//        mat.selectTechnique("GBuf");
//        System.out.println("tech:" + mat.getMaterialDef().getTechniqueDefsNames().toString());
        mat.setFloat("Shininess", 25);
//        mat.setBoolean("UseMaterialColors", true);
        cam.setLocation(new Vector3f(0.015041917f, 0.4572918f, 5.2874837f));
        cam.setRotation(new Quaternion(-1.8875003E-4f, 0.99882424f, 0.04832061f, 0.0039016632f));

//        mat.setTexture("ColorRamp", assetManager.loadTexture("Textures/ColorRamp/cloudy.png"));
//
//        mat.setBoolean("VTangent", true);
//        mat.setBoolean("Minnaert", true);
//        mat.setBoolean("WardIso", true);
//        mat.setBoolean("VertexLighting", true);
//        mat.setBoolean("LowQuality", true);
//        mat.setBoolean("HighQuality", true);

        mat.setColor("Ambient",  ColorRGBA.Black);
        mat.setColor("Diffuse",  ColorRGBA.Gray);
        mat.setColor("Specular", ColorRGBA.Gray);

        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
    }
    private void testScene2(){
        sceneId = 1;
        Sphere sphMesh = new Sphere(32, 32, 1);
        sphMesh.setTextureMode(Sphere.TextureMode.Projected);
        sphMesh.updateGeometry(32, 32, 1, false, false);
        TangentBinormalGenerator.generate(sphMesh);

        Geometry sphere = new Geometry("Rock Ball", sphMesh);
        mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
//        m_currentTechnique = TechniqueDef.DEFAULT_TECHNIQUE_NAME;
//        mat.selectTechnique(m_currentTechnique, getRenderManager());
        sphere.setMaterial(mat);
        rootNode.attachChild(sphere);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        rootNode.attachChild(lightMdl);

        pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        pl.setPosition(new Vector3f(0f, 0f, 4f));
        rootNode.addLight(pl);
    }
    private void testScene3(){
        renderManager.setSinglePassLightBatchSize(300);
        sceneId = 2;
        Node tank = (Node) assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        rootNode.attachChild(tank);



        pls = new PointLight[1000];
        angles = new float[pls.length];
        ColorRGBA colors[] = new ColorRGBA[]{
                ColorRGBA.White,
                ColorRGBA.Red,
                ColorRGBA.Blue,
                ColorRGBA.Green,
                ColorRGBA.Yellow,
                ColorRGBA.Orange,
                ColorRGBA.Brown,
        };
        Material pml = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        lightMdls = new Geometry[pls.length];
        for(int i = 0;i < pls.length;i++){
            pls[i] = new PointLight();
            pls[i].setColor(colors[pls.length % colors.length]);
            pls[i].setRadius(FastMath.nextRandomFloat(1.0f, 2.0f));
            pls[i].setPosition(new Vector3f(FastMath.nextRandomFloat(-1.0f, 1.0f), FastMath.nextRandomFloat(-1.0f, 1.0f), FastMath.nextRandomFloat(-1.0f, 1.0f)));
            rootNode.addLight(pls[i]);

//            lightMdls[i] = new Geometry("Light", new Sphere(10, 10, 0.02f));
//            lightMdls[i].setMaterial(pml);
//            lightMdls[i].getMesh().setStatic();
//            rootNode.attachChild(lightMdls[i]);
        }

//        DirectionalLight dl = new DirectionalLight();
//        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
//        dl.setColor(ColorRGBA.Green);
//        rootNode.addLight(dl);
    }

    @Override
    public void simpleInitApp() {
        currentRenderPath = RenderManager.RenderPath.Deferred;
        renderManager.setRenderPath(currentRenderPath);
        testScene3();
        
        
//        MaterialDebugAppState debug = new MaterialDebugAppState();
//        debug.registerBinding("Common/ShaderLib/BlinnPhongLighting.glsllib", teapot);
//        stateManager.attach(debug);
        setPauseOnLostFocus(false);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(10.0f);

        makeHudText();
        registerInput();
    }

    private void makeHudText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hitText = new BitmapText(guiFont, false);
        hitText.setSize(guiFont.getCharSet().getRenderedSize());
        hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        hitText.setLocalTranslation(0, cam.getHeight(), 0);
//        guiNode.attachChild(hitText);
    }

    private void registerInput(){
        inputManager.addListener(this, "toggleRenderPath");
        inputManager.addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_K));
    }

    @Override
    public void simpleUpdate(float tpf){
        if(sceneId == 1){
            angle += tpf * 0.25f;
            angle %= FastMath.TWO_PI;

            pl.setPosition(new Vector3f(FastMath.cos(angle) * 4f, 0.5f, FastMath.sin(angle) * 4f));
            lightMdl.setLocalTranslation(pl.getPosition());
        }
        else if(sceneId == 2){
//            float t = 0;
//            for(int i = 0;i < pls.length;i++){
//                t = i * 1.0f / pls.length * 1.5f + 1.5f;
//                angles[i] += tpf * ((i + 1)) / pls.length;
//                angles[i] %= FastMath.TWO_PI;
//
//                pls[i].setPosition(new Vector3f(FastMath.cos(angles[i]) * t, i *1.0f / pls.length, FastMath.sin(angles[i]) * t));
//                lightMdls[i].setLocalTranslation(pls[i].getPosition());
//            }
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(name.equals("toggleRenderPath") && !isPressed){
            if(currentRenderPath == RenderManager.RenderPath.Deferred){
                currentRenderPath = RenderManager.RenderPath.Forward;
            }
            else{
                currentRenderPath = RenderManager.RenderPath.Deferred;
            }
            renderManager.setRenderPath(currentRenderPath);
//            getRenderManager().setForcedTechnique(null);
            hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        }
    }
}
