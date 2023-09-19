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
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.debug.WireSphere;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
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

    private final Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    private float parallaxHeight = 0.05f;
    private boolean steep = false;
    private InstancedGeometry instancedGeometry;

    public static void main(String[] args){
        TestSimpleDeferredLighting app = new TestSimpleDeferredLighting();
        AppSettings appSettings = new AppSettings(true);
        appSettings.setRenderer(AppSettings.LWJGL_OPENGL40);
        app.setSettings(appSettings);
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



        pls = new PointLight[2];
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

            lightMdls[i] = new Geometry("Light", new Sphere(10, 10, 0.02f));
            lightMdls[i].setMaterial(pml);
            lightMdls[i].getMesh().setStatic();
            rootNode.attachChild(lightMdls[i]);
        }

//        DirectionalLight dl = new DirectionalLight();
//        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
//        dl.setColor(ColorRGBA.Green);
//        rootNode.addLight(dl);
    }
    private void testScene4(){
        renderManager.setSinglePassLightBatchSize(300);
        sceneId = 3;
        Node tank = (Node) assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        rootNode.attachChild(tank);

        ColorRGBA colors[] = new ColorRGBA[]{
                ColorRGBA.White,
                ColorRGBA.Red,
                ColorRGBA.Blue,
                ColorRGBA.Green,
                ColorRGBA.Yellow,
                ColorRGBA.Orange,
                ColorRGBA.Brown,
        };
        PointLight p1 = new PointLight(new Vector3f(0, 1, 0), ColorRGBA.White);
        PointLight p2 = new PointLight(new Vector3f(1, 0, 0), ColorRGBA.Green);
        p1.setRadius(10);
        p2.setRadius(10);
        rootNode.addLight(p1);
        rootNode.addLight(p2);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
    }
    private void testScene5(){
        sceneId = 4;
        // setupLighting
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(lightDir);
        dl.setColor(new ColorRGBA(.9f, .9f, .9f, 1));
        rootNode.addLight(dl);
        // setupSkyBox
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", SkyFactory.EnvMapType.CubeMap));
        // setupFloor
        mat = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m");

        Node floorGeom = new Node("floorGeom");
        Quad q = new Quad(100, 100);
        q.scaleTextureCoordinates(new Vector2f(10, 10));
        Geometry g = new Geometry("geom", q);
        g.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        floorGeom.attachChild(g);


        TangentBinormalGenerator.generate(floorGeom);
        floorGeom.setLocalTranslation(-50, 22, 60);
        //floorGeom.setLocalScale(100);

        floorGeom.setMaterial(mat);
        rootNode.attachChild(floorGeom);
        // setupSignpost
        Spatial signpost = assetManager.loadModel("Models/Sign Post/Sign Post.mesh.xml");
        Material matSp = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        TangentBinormalGenerator.generate(signpost);
        signpost.setMaterial(matSp);
        signpost.rotate(0, FastMath.HALF_PI, 0);
        signpost.setLocalTranslation(12, 23.5f, 30);
        signpost.setLocalScale(4);
        signpost.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        // other
        rootNode.attachChild(signpost);
        cam.setLocation(new Vector3f(-15.445636f, 30.162927f, 60.252777f));
        cam.setRotation(new Quaternion(0.05173137f, 0.92363626f, -0.13454558f, 0.35513034f));
        flyCam.setMoveSpeed(30);
        inputManager.addListener(new AnalogListener() {

            @Override
            public void onAnalog(String name, float value, float tpf) {
                if ("heightUP".equals(name)) {
                    parallaxHeight += 0.01;
                    mat.setFloat("ParallaxHeight", parallaxHeight);
                }
                if ("heightDown".equals(name)) {
                    parallaxHeight -= 0.01;
                    parallaxHeight = Math.max(parallaxHeight, 0);
                    mat.setFloat("ParallaxHeight", parallaxHeight);
                }

            }
        }, "heightUP", "heightDown");
        inputManager.addMapping("heightUP", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("heightDown", new KeyTrigger(KeyInput.KEY_K));

        inputManager.addListener(new ActionListener() {

            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "toggleSteep".equals(name)) {
                    steep = !steep;
                    mat.setBoolean("SteepParallax", steep);
                }
            }
        }, "toggleSteep");
        inputManager.addMapping("toggleSteep", new KeyTrigger(KeyInput.KEY_O));
    }
    private void testScene6(){
        sceneId = 6;
        final Node buggy = (Node) assetManager.loadModel("Models/Buggy/Buggy.j3o");

        TextureKey key = new TextureKey("Textures/Sky/Bright/BrightSky.dds", true);
        key.setGenerateMips(true);
        key.setTextureTypeHint(Texture.Type.CubeMap);
        final Texture tex = assetManager.loadTexture(key);

        for (Spatial geom : buggy.getChildren()) {
            if (geom instanceof Geometry) {
                Material m = ((Geometry) geom).getMaterial();
                m.setTexture("EnvMap", tex);
                m.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
            }
        }

        flyCam.setEnabled(false);

        ChaseCamera chaseCam = new ChaseCamera(cam, inputManager);
        chaseCam.setLookAtOffset(new Vector3f(0,0.5f,-1.0f));
        buggy.addControl(chaseCam);
        rootNode.attachChild(buggy);
        rootNode.attachChild(SkyFactory.createSky(assetManager, tex,
                SkyFactory.EnvMapType.CubeMap));

        DirectionalLight l = new DirectionalLight();
        l.setDirection(new Vector3f(0, -1, -1));
        rootNode.addLight(l);
    }
    private void testScene7(){
        sceneId = 6;
        Node scene = (Node) assetManager.loadModel("Scenes/ManyLights/Main.scene");
        rootNode.attachChild(scene);
        Node n = (Node) rootNode.getChild(0);
        final LightList lightList = n.getWorldLightList();
        final Geometry g = (Geometry) n.getChild("Grid-geom-1");

        g.getMaterial().setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));

        /* A colored lit cube. Needs light source! */
        Geometry boxGeo = new Geometry("shape", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Green);
        mat.setBoolean("UseInstancing", true);
        boxGeo.setMaterial(mat);

        InstancedNode instancedNode = new InstancedNode("instanced_node");
        n.attachChild(instancedNode);
        int nb = 0;
        for (Light light : lightList) {
            nb++;
            PointLight p = (PointLight) light;
            if (nb > 60) {
                n.removeLight(light);
            } else {
                int rand = FastMath.nextRandomInt(0, 3);
                switch (rand) {
                    case 0:
                        light.setColor(ColorRGBA.Red);
                        break;
                    case 1:
                        light.setColor(ColorRGBA.Yellow);
                        break;
                    case 2:
                        light.setColor(ColorRGBA.Green);
                        break;
                    case 3:
                        light.setColor(ColorRGBA.Orange);
                        break;
                }
            }
            Geometry b = boxGeo.clone(false);
            instancedNode.attachChild(b);
            b.setLocalTranslation(p.getPosition().x, p.getPosition().y, p.getPosition().z);
            b.setLocalScale(p.getRadius() * 0.5f);

        }
        instancedNode.instance();
        for(int i = 0,num = instancedNode.getChildren().size();i < num;i++){
            if(instancedNode.getChild(i) instanceof InstancedGeometry){
                instancedGeometry = (InstancedGeometry)instancedNode.getChild(i);
                instancedGeometry.setForceNumVisibleInstances(2);
            }
        }


//        cam.setLocation(new Vector3f(3.1893547f, 17.977385f, 30.8378f));
//        cam.setRotation(new Quaternion(0.14317635f, 0.82302624f, -0.23777823f, 0.49557027f));

        cam.setLocation(new Vector3f(-180.61f, 64, 7.657533f));
        cam.lookAtDirection(new Vector3f(0.93f, -0.344f, 0.044f), Vector3f.UNIT_Y);
    }

    @Override
    public void simpleInitApp() {
        currentRenderPath = RenderManager.RenderPath.Forward;
        renderManager.setRenderPath(currentRenderPath);
        testScene4();
        
        
//        MaterialDebugAppState debug = new MaterialDebugAppState();
//        debug.registerBinding("Common/ShaderLib/BlinnPhongLighting.glsllib", teapot);
//        stateManager.attach(debug);
        setPauseOnLostFocus(false);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50.0f);

        makeHudText();
        registerInput();
    }

    private void makeHudText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hitText = new BitmapText(guiFont, false);
        hitText.setSize(guiFont.getCharSet().getRenderedSize());
        hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        hitText.setLocalTranslation(0, cam.getHeight(), 0);
        guiNode.attachChild(hitText);
    }

    private void registerInput(){
        inputManager.addListener(this, "toggleRenderPath");
        inputManager.addListener(this, "addInstNum");
        inputManager.addListener(this, "deleteInstNum");
        inputManager.addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("addInstNum", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("deleteInstNum", new KeyTrigger(KeyInput.KEY_2));
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
        if(name.equals("addInstNum") && !isPressed){
            if(sceneId == 6){
                instancedGeometry.setForceNumVisibleInstances(instancedGeometry.getNumVisibleInstances() + 1);
            }
        }
        else if(name.equals("deleteInstNum") && !isPressed){
            if(sceneId == 6){
                instancedGeometry.setForceNumVisibleInstances(instancedGeometry.getNumVisibleInstances() - 1);
            }
        }
    }
}
