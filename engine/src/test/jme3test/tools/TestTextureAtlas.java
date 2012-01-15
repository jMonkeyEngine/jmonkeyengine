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
package jme3test.tools;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import jme3tools.optimize.TextureAtlas;

public class TestTextureAtlas extends SimpleApplication {

    public static void main(String[] args) {
        TestTextureAtlas app = new TestTextureAtlas();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(50);
        Node scene = new Node("Scene");
        Spatial obj1 = assetManager.loadModel("Models/Ferrari/Car.scene");
        obj1.setLocalTranslation(-4, 0, 0);
        Spatial obj2 = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        obj2.setLocalTranslation(-2, 0, 0);
        Spatial obj3 = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        obj3.setLocalTranslation(-0, 0, 0);
        Spatial obj4 = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        obj4.setLocalTranslation(2, 0, 0);
        Spatial obj5 = assetManager.loadModel("Models/Tree/Tree.mesh.j3o");
        obj5.setLocalTranslation(4, 0, 0);
        scene.attachChild(obj1);
        scene.attachChild(obj2);
        scene.attachChild(obj3);
        scene.attachChild(obj4);
        scene.attachChild(obj5);

        Geometry geom = TextureAtlas.makeAtlasBatch(scene, assetManager, 2048);

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(0.69077975f, -0.6277887f, -0.35875428f).normalizeLocal());
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        rootNode.addLight(sun);

        rootNode.attachChild(geom);

        //quad to display material
        Geometry box = new Geometry("displayquad", new Quad(4, 4));
        box.setMaterial(geom.getMaterial());
        box.setLocalTranslation(0, 1, 3);
        rootNode.attachChild(box);
    }
}
