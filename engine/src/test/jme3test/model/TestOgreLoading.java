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
package jme3test.model;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;

public class TestOgreLoading extends SimpleApplication {

    float angle1;
    float angle2;
    PointLight pl;
    PointLight p2;
    Spatial lightMdl;
    Spatial lightMd2;

    public static void main(String[] args) {
        TestOgreLoading app = new TestOgreLoading();
        app.start();
    }

    public void simpleInitApp() {
//        PointLight pl = new PointLight();
//        pl.setPosition(new Vector3f(10, 10, -10));
//        rootNode.addLight(pl);
        flyCam.setMoveSpeed(10f);

        // sunset light
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, 1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);


        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        rootNode.attachChild(lightMdl);

        lightMd2 = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMd2.setMaterial(assetManager.loadMaterial("Common/Materials/WhiteColor.j3m"));
        rootNode.attachChild(lightMd2);


        pl = new PointLight();
        pl.setColor(new ColorRGBA(1, 0.9f, 0.9f, 0));
        pl.setPosition(new Vector3f(0f, 0f, 4f));
        rootNode.addLight(pl);

        p2 = new PointLight();
        p2.setColor(new ColorRGBA(0.9f, 1, 0.9f, 0));
        p2.setPosition(new Vector3f(0f, 0f, 3f));
        rootNode.addLight(p2);


        // create the geometry and attach it
        Spatial elephant = (Spatial) assetManager.loadModel("Models/Elephant/Elephant.mesh.xml");
        float scale = 0.05f;
        elephant.scale(scale, scale, scale);
        rootNode.attachChild(elephant);
    }

    @Override
    public void simpleUpdate(float tpf) {
        angle1 += tpf * 0.25f;
        angle1 %= FastMath.TWO_PI;

        angle2 += tpf * 0.50f;
        angle2 %= FastMath.TWO_PI;

        pl.setPosition(new Vector3f(FastMath.cos(angle1) * 4f, 0.5f, FastMath.sin(angle1) * 4f));
        p2.setPosition(new Vector3f(FastMath.cos(angle2) * 4f, 0.5f, FastMath.sin(angle2) * 4f));
        lightMdl.setLocalTranslation(pl.getPosition());
        lightMd2.setLocalTranslation(p2.getPosition());
    }
}
