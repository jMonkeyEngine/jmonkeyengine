/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package jme3test.effect;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.Caps;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

public class TestEverything extends SimpleApplication {

    private DirectionalLightShadowRenderer dlsr;
    private ToneMapFilter toneMapFilter;
    private Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();

    public static void main(String[] args){
        TestEverything app = new TestEverything();
        app.start();
    }

    public void setupHdr(){
        if (renderer.getCaps().contains(Caps.GLSL100)){
            toneMapFilter = new ToneMapFilter();
            toneMapFilter.setWhitePoint(new Vector3f(3f, 3f, 3f));
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            fpp.addFilter(toneMapFilter);
            viewPort.addProcessor(fpp);

    //        setPauseOnLostFocus(false);
        }
    }

    public void setupBasicShadow(){
        if (renderer.getCaps().contains(Caps.GLSL100)){
            dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 1);
            viewPort.addProcessor(dlsr);
        }
    }

    public void setupSkyBox(){
        Texture envMap;
        if (renderer.getCaps().contains(Caps.FloatTexture)){
            envMap = assetManager.loadTexture("Textures/Sky/St Peters/StPeters.hdr");
        }else{
            envMap = assetManager.loadTexture("Textures/Sky/St Peters/StPeters.jpg");
        }
        rootNode.attachChild(SkyFactory.createSky(assetManager, envMap, 
                new Vector3f(-1,-1,-1), SkyFactory.EnvMapType.SphereMap));
    }

    public void setupLighting(){
        boolean hdr = false;
        if (toneMapFilter != null){
            hdr = toneMapFilter.isEnabled();
        }

        DirectionalLight dl = new DirectionalLight();
        if (dlsr != null) {
            dlsr.setLight(dl);
        }
        dl.setDirection(lightDir);
        if (hdr){
            dl.setColor(new ColorRGBA(3, 3, 3, 1));
        }else{
            dl.setColor(new ColorRGBA(.9f, .9f, .9f, 1));
        }
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, 0, -1).normalizeLocal());
        if (hdr){
            dl.setColor(new ColorRGBA(1, 1, 1, 1));
        }else{
            dl.setColor(new ColorRGBA(.4f, .4f, .4f, 1));
        }
        rootNode.addLight(dl);
    }

    public void setupFloor(){
        Material mat = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m");
        Box floor = new Box(50, 1f, 50);
        TangentBinormalGenerator.generate(floor);
        floor.scaleTextureCoordinates(new Vector2f(5, 5));
        Geometry floorGeom = new Geometry("Floor", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(floorGeom);
    }

//    public void setupTerrain(){
//        Material mat = manager.loadMaterial("Textures/Terrain/Rock/Rock.j3m");
//        mat.getTextureParam("DiffuseMap").getValue().setWrap(WrapMode.Repeat);
//        mat.getTextureParam("NormalMap").getValue().setWrap(WrapMode.Repeat);
//        try{
//            Geomap map = GeomapLoader.fromImage(TestEverything.class.getResource("/textures/heightmap.png"));
//            Mesh m = map.createMesh(new Vector3f(0.35f, 0.0005f, 0.35f), new Vector2f(10, 10), true);
//            Logger.getLogger(TangentBinormalGenerator.class.getName()).setLevel(Level.SEVERE);
//            TangentBinormalGenerator.generate(m);
//            Geometry t = new Geometry("Terrain", m);
//            t.setLocalTranslation(85, -15, 0);
//            t.setMaterial(mat);
//            t.updateModelBound();
//            t.setShadowMode(ShadowMode.Receive);
//            rootNode.attachChild(t);
//        }catch (IOException ex){
//            ex.printStackTrace();
//        }
//
//    }

    public void setupRobotGuy(){
        Node model = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        Material mat = assetManager.loadMaterial("Models/Oto/Oto.j3m");
        model.getChild(0).setMaterial(mat);
//        model.setAnimation("Walk");
        model.setLocalTranslation(30, 10.5f, 30);
        model.setLocalScale(2);
        model.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(model);
    }

    public void setupSignpost(){
        Spatial signpost = assetManager.loadModel("Models/Sign Post/Sign Post.mesh.xml");
        Material mat = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        signpost.setMaterial(mat);
        signpost.rotate(0, FastMath.HALF_PI, 0);
        signpost.setLocalTranslation(12, 3.5f, 30);
        signpost.setLocalScale(4);
        signpost.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(signpost);
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-32.295086f, 54.80136f, 79.59805f));
        cam.setRotation(new Quaternion(0.074364014f, 0.92519957f, -0.24794696f, 0.27748522f));
        cam.update();

        cam.setFrustumFar(300);
        flyCam.setMoveSpeed(30);

        rootNode.setCullHint(CullHint.Never);

        setupBasicShadow();
        setupHdr();

        setupLighting();
        setupSkyBox();

//        setupTerrain();
        setupFloor();
//        setupRobotGuy();
        setupSignpost();

        
    }

}
