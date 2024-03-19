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
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.PBRBloomFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

public class TestPBRBloom extends SimpleApplication {

    private FilterPostProcessor fpp;
    
    public static void main(String[] args){
        TestPBRBloom app = new TestPBRBloom();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        assetManager.registerLocator("../jmonkeyengine/jme3-effects/src/main/resources", FileLocator.class);
        assetManager.registerLocator("../jmonkeyengine/jme3-testdata/src/main/resources", FileLocator.class);
        
        // put the camera in a bad position
        //cam.setLocation(new Vector3f(-2.336393f, 11.91392f, -7.139601f));
        //cam.setRotation(new Quaternion(0.23602544f, 0.11321983f, -0.027698677f, 0.96473104f));
        //cam.setFrustumFar(1000);
        flyCam.setMoveSpeed(20);

        Material mat = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");
        mat.setFloat("Shininess", 15f);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Yellow.mult(0.2f));
        mat.setColor("Diffuse", ColorRGBA.Yellow.mult(0.2f));
        mat.setColor("Specular", ColorRGBA.Yellow.mult(0.8f));


        Material matSoil = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");
        matSoil.setFloat("Shininess", 15f);
        matSoil.setBoolean("UseMaterialColors", true);
        matSoil.setColor("Ambient", ColorRGBA.Gray);
        matSoil.setColor("Diffuse", ColorRGBA.Gray);
        matSoil.setColor("Specular", ColorRGBA.Gray);

        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalTranslation(0,0,10);

        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        teapot.setLocalScale(10.0f);
        rootNode.attachChild(teapot);

        Geometry soil = new Geometry("soil", new Box(800, 10, 700));
        soil.setLocalTranslation(0, -13, 550);
        soil.setMaterial(matSoil);
        soil.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(soil);
        
        Material tankMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        tankMat.setTexture("BaseColorMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_diffuse.jpg", !true)));
        tankMat.setTexture("SpecularMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_specular.jpg", !true)));
        tankMat.setTexture("NormalMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_normals.png", !true)));
        tankMat.setTexture("EmissiveMap", assetManager.loadTexture(new TextureKey("Models/HoverTank/tank_glow_map.jpg", !true)));
        tankMat.setFloat("EmissivePower", 100);
        tankMat.setFloat("EmissiveIntensity", 100);
        tankMat.setFloat("Metallic", .5f);
        Spatial tank = assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        tank.setLocalTranslation(-10, 5, -10);
        tank.setMaterial(tankMat);
        rootNode.attachChild(tank);

        DirectionalLight light=new DirectionalLight();
        light.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        light.setColor(ColorRGBA.White);
        //rootNode.addLight(light);
        
        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(5, 5, 5));
        pl.setRadius(1000);
        pl.setColor(ColorRGBA.White);
        rootNode.addLight(pl);

        // load sky
        Spatial sky = SkyFactory.createSky(assetManager, 
                "Textures/Sky/Bright/FullskiesBlueClear03.dds", 
                EnvMapType.CubeMap);
        sky.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);
        
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
        
        fpp=new FilterPostProcessor(assetManager);
        PBRBloomFilter bloom=new PBRBloomFilter();
        fpp.addFilter(bloom);
        //ToneMapFilter toneMap = new ToneMapFilter();
        //fpp.addFilter(toneMap);
        viewPort.addProcessor(fpp);

    }

 

}
