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

package jme3test.collision;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;

public class TestMousePick extends SimpleApplication {

    public static void main(String[] args) {
        TestMousePick app = new TestMousePick();
        app.start();
    }
    
    Node shootables;
    Geometry mark;

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        initMark();       // a red sphere to mark the hit

        /** create four colored boxes and a floor to shoot at: */
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);
        shootables.attachChild(makeCube("a Dragon", -2f, 0f, 1f));
        shootables.attachChild(makeCube("a tin can", 1f, -2f, 0f));
        shootables.attachChild(makeCube("the Sheriff", 0f, 1f, -2f));
        shootables.attachChild(makeCube("the Deputy", 1f, 0f, -4f));
        shootables.attachChild(makeFloor());
        shootables.attachChild(makeCharacter());
    }

    @Override
    public void simpleUpdate(float tpf){
        Vector3f origin    = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f);
        Vector3f direction = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f);
        direction.subtractLocal(origin).normalizeLocal();

        Ray ray = new Ray(origin, direction);
        CollisionResults results = new CollisionResults();
        shootables.collideWith(ray, results);
//        System.out.println("----- Collisions? " + results.size() + "-----");
//        for (int i = 0; i < results.size(); i++) {
//            // For each hit, we know distance, impact point, name of geometry.
//            float dist = results.getCollision(i).getDistance();
//            Vector3f pt = results.getCollision(i).getWorldContactPoint();
//            String hit = results.getCollision(i).getGeometry().getName();
//            System.out.println("* Collision #" + i);
//            System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");
//        }
        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            mark.setLocalTranslation(closest.getContactPoint());

            Quaternion q = new Quaternion();
            q.lookAt(closest.getContactNormal(), Vector3f.UNIT_Y);
            mark.setLocalRotation(q);

            rootNode.attachChild(mark);
        } else {
            rootNode.detachChild(mark);
        }
    }
 
    /** A cube object for target practice */
    protected Geometry makeCube(String name, float x, float y, float z) {
        Box box = new Box(new Vector3f(x, y, z), 1, 1, 1);
        Geometry cube = new Geometry(name, box);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.randomColor());
        cube.setMaterial(mat1);
        return cube;
    }

    /** A floor to show that the "shot" can go through several objects. */
    protected Geometry makeFloor() {
        Box box = new Box(new Vector3f(0, -4, -5), 15, .2f, 15);
        Geometry floor = new Geometry("the Floor", box);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.Gray);
        floor.setMaterial(mat1);
        return floor;
    }

    /** A red ball that marks the last spot that was "hit" by the "shot". */
    protected void initMark() {
        Arrow arrow = new Arrow(Vector3f.UNIT_Z.mult(2f));
        arrow.setLineWidth(3);

        //Sphere sphere = new Sphere(30, 30, 0.2f);
        mark = new Geometry("BOOM!", arrow);
        //mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }

    protected Spatial makeCharacter() {
        // load a character from jme3test-test-data
        Spatial golem = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        golem.scale(0.5f);
        golem.setLocalTranslation(-1.0f, -1.5f, -0.6f);

        // We must add a light to make the model visible
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f).normalizeLocal());
        golem.addLight(sun);
        return golem;
    }
}
