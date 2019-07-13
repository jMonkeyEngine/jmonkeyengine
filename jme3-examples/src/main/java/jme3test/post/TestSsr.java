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
package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.SsrFilter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;

public class TestSsr extends SimpleApplication {

    Spatial model;

    public static void main(String[] args) {
        TestSsr app = new TestSsr();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(10, 5, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
//        cam.setRotation(new Quaternion(0.046916496f, -0.69500375f, 0.045538206f, 0.7160271f));


        flyCam.setMoveSpeed(50);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture diff = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        diff.setWrap(Texture.WrapMode.Repeat);
        Texture norm = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall_normal.jpg");
        norm.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("DiffuseMap", diff);
        mat.setTexture("NormalMap", norm);
        //mat.setFloat("Shininess", 10.0f);


        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.DarkGray);
                rootNode.addLight(al);

//        DirectionalLight dl = new DirectionalLight(new Vector3f(0f ,-1f, 0.f));
//        dl.setColor(ColorRGBA.LightGray);
//        rootNode.addLight(dl);

        PointLight p = new PointLight(new Vector3f(-5, 5, -5), ColorRGBA.Blue);
        p.setRadius(15);
        rootNode.addLight(p);
        
        PointLight p3 = new PointLight(new Vector3f(0, 10, 0), ColorRGBA.LightGray);
        p3.setRadius(15);
        rootNode.addLight(p3);

        PointLight p2 = new PointLight(new Vector3f(5, 5, 5), ColorRGBA.Red);
        p2.setRadius(15);
        rootNode.addLight(p2);

        model = assetManager.loadModel("Scenes/SSR/testScene.j3o");
        
//        model.setMaterial(mat);
        
//        model = (Geometry) assetManager.loadModel("Models/Sponza/Sponza.j3o");
//        model.getMesh().scaleTextureCoordinates(new Vector2f(2, 2));
//        model.setMaterial(mat);
//        TangentBinormalGenerator.generate(model);
        rootNode.attachChild(model);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        SsrFilter ssrFilter = new SsrFilter();
        ssrFilter.setDownSampleFactor(1.5f);
        ssrFilter.setApproximateNormals(false);
        ssrFilter.setFastBlur(false);
        ssrFilter.setStepLength(0.5f);
        ssrFilter.setRaySteps(16);
        ssrFilter.setSigma(2f);
        ssrFilter.setReflectionFactor(0.25f);
        fpp.addFilter(ssrFilter);
        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
