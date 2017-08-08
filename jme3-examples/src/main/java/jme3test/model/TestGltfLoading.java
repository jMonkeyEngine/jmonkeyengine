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
package jme3test.model;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.scene.shape.Sphere;

public class TestGltfLoading extends SimpleApplication {


    public static void main(String[] args) {
        TestGltfLoading app = new TestGltfLoading();
        app.start();
    }

    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        // sunset light
//        DirectionalLight dl = new DirectionalLight();
//        dl.setDirection(new Vector3f(-1f, -1.0f, -1f).normalizeLocal());
//        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
//        rootNode.addLight(dl);
//
//        DirectionalLight dl2 = new DirectionalLight();
//        dl2.setDirection(new Vector3f(1f, 1.0f, 1f).normalizeLocal());
//        dl2.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
//        rootNode.addLight(dl2);

        PointLight pl = new PointLight(new Vector3f(5.0f, 5.0f, 5.0f), ColorRGBA.White, 30);
        rootNode.addLight(pl);
        PointLight pl1 = new PointLight(new Vector3f(-5.0f, -5.0f, -5.0f), ColorRGBA.White.mult(0.5f), 50);
        rootNode.addLight(pl1);

        rootNode.attachChild(assetManager.loadModel("Models/gltf/box/box.gltf"));
        //rootNode.attachChild(assetManager.loadModel(new GltfModelKey("Models/gltf/duck/Duck.gltf")));

        //rootNode.attachChild(assetManager.loadModel("Models/gltf/hornet/scene.gltf"));
    }


}
