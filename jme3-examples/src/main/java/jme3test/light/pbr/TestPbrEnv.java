/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
import com.jme3.bounding.BoundingSphere;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.util.LightsDebugState;
import com.jme3.light.LightProbe;
import com.jme3.material.TechniqueDef;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Node;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

public class TestPbrEnv extends SimpleApplication implements ActionListener {

    public static final int SHADOWMAP_SIZE = 1024;
    private Spatial[] obj;
    private Material[] mat;
    private DirectionalLightShadowRenderer dlsr;
    private LightsDebugState debugState;

    private EnvironmentCamera envCam;

    private Geometry ground;
    private Material matGroundU;
    private Material matGroundL;

    private Geometry camGeom;

    public static void main(String[] args) {
        TestPbrEnv app = new TestPbrEnv();
        app.start();
    } 

    
    public void loadScene() {
        
        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        renderManager.setSinglePassLightBatchSize(3);
        obj = new Spatial[2];
        // Setup first view

        mat = new Material[2];
        mat[0] = assetManager.loadMaterial("jme3test/light/pbr/pbrMat.j3m");
        //mat[1] = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat[1] = assetManager.loadMaterial("jme3test/light/pbr/pbrMat2.j3m");
//        mat[1].setBoolean("UseMaterialColors", true);
//        mat[1].setColor("Ambient", ColorRGBA.White.mult(0.5f));
//        mat[1].setColor("Diffuse", ColorRGBA.White.clone());

        obj[0] = new Geometry("sphere", new Sphere(30, 30, 2));
        obj[0].setShadowMode(ShadowMode.CastAndReceive);
        obj[1] = new Geometry("cube", new Box(1.0f, 1.0f, 1.0f));
        obj[1].setShadowMode(ShadowMode.CastAndReceive);
        TangentBinormalGenerator.generate(obj[1]);
        TangentBinormalGenerator.generate(obj[0]);

//        for (int i = 0; i < 60; i++) {
//            Spatial t = obj[FastMath.nextRandomInt(0, obj.length - 1)].clone(false);
//            t.setName("Cube" + i);
//            t.setLocalScale(FastMath.nextRandomFloat() * 10f);
//            t.setMaterial(mat[FastMath.nextRandomInt(0, mat.length - 1)]);
//            rootNode.attachChild(t);
//            t.setLocalTranslation(FastMath.nextRandomFloat() * 200f, FastMath.nextRandomFloat() * 30f + 20, 30f * (i + 2f));
//        }

        for (int i = 0; i < 2; i++) {
            Spatial t = obj[0].clone(false);
            t.setName("Cube" + i);
            t.setLocalScale( 10f);
            t.setMaterial(mat[1].clone());
            rootNode.attachChild(t);
            t.setLocalTranslation(i * 200f+ 100f, 50, 800f * (i));
        }
        
        Box b = new Box(1000, 2, 1000);
        b.scaleTextureCoordinates(new Vector2f(20, 20));
        ground = new Geometry("soil", b);
        TangentBinormalGenerator.generate(ground);
        ground.setLocalTranslation(0, 10, 550);
        matGroundU = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matGroundU.setColor("Color", ColorRGBA.Green);

//        matGroundL = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
//        grass.setWrap(WrapMode.Repeat);
//        matGroundL.setTexture("DiffuseMap", grass);

        matGroundL = assetManager.loadMaterial("jme3test/light/pbr/pbrMat4.j3m");
        
        ground.setMaterial(matGroundL);

        //ground.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(ground);

        l = new DirectionalLight();
        l.setColor(ColorRGBA.White);
        //l.setDirection(new Vector3f(0.5973172f, -0.16583486f, 0.7846725f).normalizeLocal());
        l.setDirection(new Vector3f(-0.2823181f, -0.41889593f, 0.863031f).normalizeLocal());
        
        rootNode.addLight(l);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.5f));
      //  rootNode.addLight(al);

        //Spatial sky = SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", SkyFactory.EnvMapType.CubeMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        sky.setLocalScale(350);

        rootNode.attachChild(sky);
    }
    DirectionalLight l;

    @Override
    public void simpleInitApp() {
        assetManager.registerLoader(KTXLoader.class, "ktx");
        
        
        // put the camera in a bad position
        cam.setLocation(new Vector3f(-52.433647f, 68.69636f, -118.60924f));
        cam.setRotation(new Quaternion(0.10294232f, 0.25269797f, -0.027049713f, 0.96167296f));      

        flyCam.setMoveSpeed(100);

        loadScene();

        dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 4);
        dlsr.setLight(l);
        //dlsr.setLambda(0.55f);
        dlsr.setShadowIntensity(0.5f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        //dlsr.displayDebug();
 //       viewPort.addProcessor(dlsr);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(6.0f)));
        SSAOFilter ssao = new SSAOFilter();
        ssao.setIntensity(5);
                
        fpp.addFilter(ssao);
        
        BloomFilter bloomFilter = new BloomFilter();
        fpp.addFilter(bloomFilter);
        fpp.addFilter(new FXAAFilter());
        //viewPort.addProcessor(fpp);

        initInputs();

//        envManager = new EnvironmentManager();
//        getStateManager().attach(envManager);
//        
        envCam = new EnvironmentCamera();
        getStateManager().attach(envCam);

        debugState = new LightsDebugState();
        debugState.setProbeScale(5);        
        getStateManager().attach(debugState);

        camGeom = new Geometry("camGeom", new Sphere(16, 16, 2));
//        Material m = new Material(assetManager, "Common/MatDefs/Misc/UnshadedNodes.j3md");
//        m.setColor("Color", ColorRGBA.Green);
        Material m = assetManager.loadMaterial("jme3test/light/pbr/pbrMat3.j3m");
        camGeom.setMaterial(m);
        camGeom.setLocalTranslation(0, 20, 0);
        camGeom.setLocalScale(5);
        rootNode.attachChild(camGeom);
        
   //     envManager.setScene(rootNode);
        
//        MaterialDebugAppState debug = new MaterialDebugAppState();
//        debug.registerBinding("MatDefs/PBRLighting.frag", rootNode);
//        getStateManager().attach(debug);
        
        flyCam.setDragToRotate(true);
        setPauseOnLostFocus(false);
        
       // cam.lookAt(camGeom.getWorldTranslation(), Vector3f.UNIT_Y);

    }

    private void fixFLyCamInputs() {
        inputManager.deleteMapping(CameraInput.FLYCAM_LEFT);
        inputManager.deleteMapping(CameraInput.FLYCAM_RIGHT);
        inputManager.deleteMapping(CameraInput.FLYCAM_UP);
        inputManager.deleteMapping(CameraInput.FLYCAM_DOWN);

        inputManager.addMapping(CameraInput.FLYCAM_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));

        inputManager.addMapping(CameraInput.FLYCAM_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));

        inputManager.addMapping(CameraInput.FLYCAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));

        inputManager.addMapping(CameraInput.FLYCAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        inputManager.addListener(flyCam, CameraInput.FLYCAM_LEFT, CameraInput.FLYCAM_RIGHT, CameraInput.FLYCAM_UP, CameraInput.FLYCAM_DOWN);
    }

    private void initInputs() {
        inputManager.addMapping("switchGroundMat", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("snapshot", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("fc", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("debugProbe", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("debugTex", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("delete", new KeyTrigger(KeyInput.KEY_DELETE));

        inputManager.addListener(this, "delete","switchGroundMat", "snapshot", "debugTex", "debugProbe", "fc", "up", "down", "left", "right");
    }
    
    private LightProbe lastProbe;
    private Node debugGui ;

    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {

        if (name.equals("switchGroundMat") && keyPressed) {
            if (ground.getMaterial() == matGroundL) {
                ground.setMaterial(matGroundU);
            } else {
                
                ground.setMaterial(matGroundL);
            }
        }

        if (name.equals("snapshot") && keyPressed) {
            envCam.setPosition(camGeom.getWorldTranslation());
            lastProbe = LightProbeFactory.makeProbe(envCam, rootNode, new ConsoleProgressReporter());            
            ((BoundingSphere)lastProbe.getBounds()).setRadius(200);
            rootNode.addLight(lastProbe);

        }
        
        if (name.equals("delete") && keyPressed) {           
            System.err.println(rootNode.getWorldLightList().size());
            rootNode.removeLight(lastProbe);           
            System.err.println("deleted");
            System.err.println(rootNode.getWorldLightList().size());
        }

        if (name.equals("fc") && keyPressed) {

            flyCam.setEnabled(true);
        }

        if (name.equals("debugProbe") && keyPressed) {
            debugState.setEnabled(!debugState.isEnabled());
        }
        
        if (name.equals("debugTex") && keyPressed) {
            if(debugGui == null || debugGui.getParent() == null){
                debugGui = lastProbe.getDebugGui(assetManager);
                debugGui.setLocalTranslation(10, 200, 0);
                guiNode.attachChild(debugGui);
            } else if(debugGui != null){
                debugGui.removeFromParent();
            }
        }

        if (name.equals("up")) {
            up = keyPressed;
        }
        if (name.equals("down")) {
            down = keyPressed;
        }
        if (name.equals("right")) {
            right = keyPressed;
        }
        if (name.equals("left")) {
            left = keyPressed;
        }
        if (name.equals("fwd")) {
            fwd = keyPressed;
        }
        if (name.equals("back")) {
            back = keyPressed;
        }

    }
    boolean up = false;
    boolean down = false;
    boolean left = false;
    boolean right = false;
    boolean fwd = false;
    boolean back = false;
    float time = 0;
    float s = 50f;
    boolean initialized = false;

    @Override
    public void simpleUpdate(float tpf) {

        if (!initialized) {
            fixFLyCamInputs();
            initialized = true;
        }
        float val = tpf * s;
        if (up) {
            camGeom.move(0, 0, val);
        }
        if (down) {
            camGeom.move(0, 0, -val);

        }
        if (right) {
            camGeom.move(-val, 0, 0);

        }
        if (left) {
            camGeom.move(val, 0, 0);

        }

    }

}
