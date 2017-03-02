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
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.app.DetailedProfilerState;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;

public class TestSSAO2 extends SimpleApplication {

    Geometry model;

    public static void main(String[] args) {
        TestSSAO2 app = new TestSSAO2();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1,-1,-1).normalizeLocal());
        rootNode.addLight(dl);

        flyCam.setDragToRotate(true);
        setPauseOnLostFocus(false);

        getStateManager().attach(new DetailedProfilerState());

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setFloat("Shininess", 16f);
        //mat.setBoolean("VertexLighting", true);


        Geometry floor = new Geometry("floor", new Box(1000,0.1f,1000));
        floor.setMaterial(mat);
        rootNode.attachChild(floor);

        Node teapotNode = (Node) assetManager.loadModel("Models/Teapot/Teapot.mesh.xml");
        Geometry teapot = (Geometry) teapotNode.getChild(0);
        teapot.setMaterial(mat);
//        Sphere sph = new Sphere(16, 16, 4);
//        Geometry teapot = new Geometry("teapot", sph);



        // show normals as material
        //Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        for (int f = 10; f > 3; f--) {
            for (int y = -f; y < f; y++) {
                for (int x = -f; x < f; x++) {
                    Geometry clonePot = teapot.clone();

                    //clonePot.setMaterial(mat);
                    clonePot.setLocalTranslation(x * .5f, 10 - f, y * .5f);
                    clonePot.setLocalScale(.15f);

                    rootNode.attachChild(clonePot);
                }
            }
        }

        cam.setLocation(new Vector3f(10.247649f, 8.275992f, 10.405156f));
        cam.setRotation(new Quaternion(-0.083419204f, 0.90370524f, -0.20599906f, -0.36595422f));


        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        SSAOFilter ssaoFilter = new SSAOFilter(2.9299974f,25f,5.8100376f,0.091000035f);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

        //ssaoFilter.setApproximateNormals(true);
        fpp.addFilter(ssaoFilter);
        SSAOUI ui = new SSAOUI(inputManager, ssaoFilter);

        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
