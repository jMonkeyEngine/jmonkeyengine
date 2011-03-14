/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;

public class TestSSAO extends SimpleApplication {

    Geometry model;

    public static void main(String[] args) {
        TestSSAO app = new TestSSAO();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(68.45442f, 8.235511f, 7.9676695f));
        cam.setRotation(new Quaternion(0.046916496f, -0.69500375f, 0.045538206f, 0.7160271f));


        flyCam.setMoveSpeed(50);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture diff = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        diff.setWrap(Texture.WrapMode.Repeat);
        Texture norm = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall_normal.jpg");
        norm.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("DiffuseMap", diff);
        mat.setTexture("NormalMap", norm);
        mat.setFloat("Shininess", 2.0f);


        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(1.8f, 1.8f, 1.8f, 1.0f));

        rootNode.addLight(al);

        model = (Geometry) assetManager.loadModel("Models/Sponza/Sponza.j3o");
        model.getMesh().scaleTextureCoordinates(new Vector2f(2, 2));

        model.setMaterial(mat);

        rootNode.attachChild(model);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        SSAOFilter ssaoFilter = new SSAOFilter(12.940201f, 43.928635f, 0.32999992f, 0.6059958f);
        fpp.addFilter(ssaoFilter);
        SSAOUI ui = new SSAOUI(inputManager, ssaoFilter);

        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
