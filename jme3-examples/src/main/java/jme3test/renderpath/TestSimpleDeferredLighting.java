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

package jme3test.renderpath;

import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.environment.util.LightsDebugState;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

public class TestSimpleDeferredLighting extends SimpleApplication implements ActionListener {
    
    private boolean bUseFramegraph = true;
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
    private DirectionalLight dl;

    private float roughness = 0.0f;

    private Node modelNode;
    private int frame = 0;
    private Material pbrMat;
    private Geometry model;
    private Node tex;

    public static void main(String[] args){
        TestSimpleDeferredLighting app = new TestSimpleDeferredLighting();
        AppSettings appSettings = new AppSettings(true);
        //appSettings.setRenderer(AppSettings.LWJGL_OPENGL40);
        appSettings.setWidth(768);
        appSettings.setHeight(768);
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
    public Geometry putShape(Mesh shape, ColorRGBA color, float lineWidth){
        Geometry g = new Geometry("shape", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setLineWidth(lineWidth);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
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

//        Geometry g = putShape(new WireSphere(1), ColorRGBA.Yellow, 1);
//        g.setLocalTranslation(p1.getPosition());
//        g.setLocalScale(p1.getRadius() * 0.5f);
//
//        g = putShape(new WireSphere(1), ColorRGBA.Yellow, 1);
//        g.setLocalTranslation(p2.getPosition());
//        g.setLocalScale(p2.getRadius());

//        DirectionalLight dl = new DirectionalLight();
//        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
//        dl.setColor(ColorRGBA.White);
//        rootNode.addLight(dl);
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
        g.getMaterial().setBoolean("VertexLighting", false);

        /* A colored lit cube. Needs light source! */
//        Geometry boxGeo = new Geometry("shape", new Box(1, 1, 1));
//        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.getAdditionalRenderState().setWireframe(true);
//        mat.setColor("Color", ColorRGBA.Green);
//        mat.setBoolean("UseInstancing", true);
//        boxGeo.setMaterial(mat);
//
//        InstancedNode instancedNode = new InstancedNode("instanced_node");
//        n.attachChild(instancedNode);
        int nb = 0;
        for (Light light : lightList) {
            nb++;
            PointLight p = (PointLight) light;
            if (nb > 20) {
                n.removeLight(light);
            } else {
                int rand = FastMath.nextRandomInt(0, 3);
                switch (rand) {
                    case 0:
                        light.setColor(ColorRGBA.Red);
                        break;
                    case 1:
                        light.setColor(ColorRGBA.Blue);
                        break;
                    case 2:
                        light.setColor(ColorRGBA.Green);
                        break;
                    case 3:
                        light.setColor(ColorRGBA.Yellow);
                        break;
                }
            }
//            Geometry b = boxGeo.clone(false);
//            instancedNode.attachChild(b);
//            b.setLocalTranslation(p.getPosition().x, p.getPosition().y, p.getPosition().z);
//            b.setLocalScale(p.getRadius() * 0.5f);

        }
//        instancedNode.instance();
//        for(int i = 0,num = instancedNode.getChildren().size();i < num;i++){
//            if(instancedNode.getChild(i) instanceof InstancedGeometry){
//                instancedGeometry = (InstancedGeometry)instancedNode.getChild(i);
//                instancedGeometry.setForceNumVisibleInstances(2);
//            }
//        }


//        cam.setLocation(new Vector3f(3.1893547f, 17.977385f, 30.8378f));
//        cam.setRotation(new Quaternion(0.14317635f, 0.82302624f, -0.23777823f, 0.49557027f));

        cam.setLocation(new Vector3f(-180.61f, 64, 7.657533f));
        cam.lookAtDirection(new Vector3f(0.93f, -0.344f, 0.044f), Vector3f.UNIT_Y);

        cam.setLocation(new Vector3f(-26.85569f, 15.701239f, -19.206047f));
        cam.lookAtDirection(new Vector3f(0.13871355f, -0.6151029f, 0.7761488f), Vector3f.UNIT_Y);
    }
    private void testScene8(){
        Quad quadMesh = new Quad(512,512);
        Geometry quad = new Geometry("Quad", quadMesh);
        quad.setQueueBucket(RenderQueue.Bucket.Opaque);

        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
        quad.setMaterial(mat);

        rootNode.attachChild(quad);
    }
    private void testScene9(){
        viewPort.setBackgroundColor(ColorRGBA.Black);

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
    private void testScene10(){
        sceneId = 9;
        roughness = 1.0f;
        assetManager.registerLoader(KTXLoader.class, "ktx");

        viewPort.setBackgroundColor(ColorRGBA.White);
        modelNode = new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        rootNode.attachChild(modelNode);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

//        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(4.0f)));
//        fpp.addFilter(new SSAOFilter(0.5f, 3, 0.2f, 0.2f));
        viewPort.addProcessor(fpp);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);


        final EnvironmentCamera envCam = new EnvironmentCamera(256, new Vector3f(0, 3f, 0));
        stateManager.attach(envCam);

//        EnvironmentManager envManager = new EnvironmentManager();
//        stateManager.attach(envManager);

        //       envManager.setScene(rootNode);

        LightsDebugState debugState = new LightsDebugState();
        stateManager.attach(debugState);

        ChaseCamera chaser = new ChaseCamera(cam, modelNode, inputManager);
        chaser.setDragToRotate(true);
        chaser.setMinVerticalRotation(-FastMath.HALF_PI);
        chaser.setMaxDistance(1000);
        chaser.setSmoothMotion(true);
        chaser.setRotationSensitivity(10);
        chaser.setZoomSensitivity(5);
        flyCam.setEnabled(false);
        //flyCam.setMoveSpeed(100);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("debug") && isPressed) {
                    if (tex == null) {
                        return;
                    }
                    if (tex.getParent() == null) {
                        guiNode.attachChild(tex);
                    } else {
                        tex.removeFromParent();
                    }
                }

                if (name.equals("rup") && isPressed) {
                    roughness = FastMath.clamp(roughness + 0.1f, 0.0f, 1.0f);
                    pbrMat.setFloat("Roughness", roughness);
                }
                if (name.equals("rdown") && isPressed) {
                    roughness = FastMath.clamp(roughness - 0.1f, 0.0f, 1.0f);
                    pbrMat.setFloat("Roughness", roughness);
                }


                if (name.equals("up") && isPressed) {
                    model.move(0, tpf * 100f, 0);
                }

                if (name.equals("down") && isPressed) {
                    model.move(0, -tpf * 100f, 0);
                }
                if (name.equals("left") && isPressed) {
                    model.move(0, 0, tpf * 100f);
                }
                if (name.equals("right") && isPressed) {
                    model.move(0, 0, -tpf * 100f);
                }
                if (name.equals("light") && isPressed) {
                    dl.setDirection(cam.getDirection().normalize());
                }
            }
        }, "toggle", "light", "up", "down", "left", "right", "debug", "rup", "rdown");

        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("light", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("rup", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("rdown", new KeyTrigger(KeyInput.KEY_G));
    }
    private void testScene11(){
//        Box boxMesh = new Box(0.5f,0.5f,0.5f);
//        Geometry boxGeo = new Geometry("Colored Box", boxMesh);
//        boxGeo.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        boxMat.setBoolean("UseMaterialColors", true);
//        boxMat.setColor("Ambient", ColorRGBA.Green);
//        boxMat.setColor("Diffuse", ColorRGBA.Green);
//        boxGeo.setMaterial(boxMat);
//        rootNode.attachChild(boxGeo);
        Node tank = (Node) assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        tank.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        tank.setLocalScale(0.3f);
        rootNode.attachChild(tank);

        Quad plane = new Quad(10, 10);
        Geometry planeGeo = new Geometry("Plane", plane);
        planeGeo.setShadowMode(RenderQueue.ShadowMode.Receive);
        planeGeo.rotate(-45, 0, 0);
        planeGeo.setLocalTranslation(-5, -5, 0);
        Material planeMat = boxMat.clone();
        planeMat.setBoolean("UseMaterialColors", true);
        planeMat.setColor("Ambient", ColorRGBA.White);
        planeMat.setColor("Diffuse", ColorRGBA.Gray);
        planeGeo.setMaterial(planeMat);
        rootNode.attachChild(planeGeo);


        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf.setLight(sun);

        sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf2 = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf2.setLight(sun);

        sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.0f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf3 = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf3.setLight(sun);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        fpp.addFilter(dlsf2);
        fpp.addFilter(dlsf3);
        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleInitApp() {
        
        stateManager.attach(new DetailedProfilerState());
        
        //FrameGraph graph = RenderPipelineFactory.create(this, RenderManager.RenderPath.Deferred);
        FrameGraph graph = new FrameGraph(assetManager, renderManager);
        graph.applyData(assetManager.loadFrameGraph("Common/FrameGraphs/Deferred.j3g"));
        //FrameGraph graph = FrameGraphFactory.forward(assetManager, renderManager);
        viewPort.setFrameGraph(graph);
        
        viewPort.setBackgroundColor(ColorRGBA.Green.mult(.1f));
        
        Geometry debugView = new Geometry("debug", new Quad(200, 200));
        debugView.setLocalTranslation(0, 200, 0);
        Material debugMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        //debugMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //debugMat.setTransparent(true);
        debugView.setMaterial(debugMat);
        //MatRenderParam texParam = new MatRenderParam("ColorMap", debugMat, VarType.Texture2D);
        //texParam.enableDebug();
        //graph.bindToOutput(GBufferModule.RENDER_TARGETS[1], texParam);
        //guiNode.attachChild(debugView);
        
        //renderManager.setRenderPath(currentRenderPath);
        testScene1();
//        cam.setFrustumPerspective(45.0f, 4.0f / 3.0f, 0.01f, 100.0f);
        flyCam.setMoveSpeed(10.0f);
        // deferred
//        testScene7();
        
        
//        MaterialDebugAppState debug = new MaterialDebugAppState();
//        debug.registerBinding("Common/ShaderLib/BlinnPhongLighting.glsllib", teapot);
//        stateManager.attach(debug);
        setPauseOnLostFocus(false);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50.0f);
        
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));

        registerInput();
    }

    private void registerInput(){
        inputManager.addListener(this, "toggleRenderPath");
        inputManager.addListener(this, "toggleFramegraph");
        inputManager.addListener(this, "addInstNum");
        inputManager.addListener(this, "deleteInstNum");
        inputManager.addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("toggleFramegraph", new KeyTrigger(KeyInput.KEY_N));
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
        else if(sceneId == 9){
            frame++;

            if (frame == 2) {
                modelNode.removeFromParent();
                final LightProbe probe = LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {

                    @Override
                    public void done(LightProbe result) {
                        System.err.println("Done rendering env maps");
                        tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                    }
                });
                probe.getArea().setRadius(100);
                rootNode.addLight(probe);
                //getStateManager().getState(EnvironmentManager.class).addEnvProbe(probe);

            }
            if (frame > 10 && modelNode.getParent() == null) {
                rootNode.attachChild(modelNode);
            }
        }
//        System.out.println("cam.pos:" + cam.getLocation());
//        System.out.println("cam.look:" + cam.getDirection());
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(name.equals("toggleFramegraph") && !isPressed){
            bUseFramegraph = !bUseFramegraph;
            //renderManager.enableFramegraph(bUseFramegraph);
        }
        if(name.equals("toggleRenderPath") && !isPressed){
            //renderManager.setRenderPath(currentRenderPath);
//            getRenderManager().setForcedTechnique(null);
        }
//        if(name.equals("addInstNum") && !isPressed){
//            if(sceneId == 6){
//                instancedGeometry.setForceNumVisibleInstances(instancedGeometry.getNumVisibleInstances() + 1);
//            }
//        }
//        else if(name.equals("deleteInstNum") && !isPressed){
//            if(sceneId == 6){
//                instancedGeometry.setForceNumVisibleInstances(instancedGeometry.getNumVisibleInstances() - 1);
//            }
//        }
    }
}
