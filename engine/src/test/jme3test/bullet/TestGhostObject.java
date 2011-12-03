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

package jme3test.bullet;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 *
 * @author tim8dev [at] gmail [dot com]
 */
public class TestGhostObject extends SimpleApplication {

    private BulletAppState bulletAppState;
    private GhostControl ghostControl;

    public static void main(String[] args) {
        Application app = new TestGhostObject();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // Mesh to be shared across several boxes.
        Box boxGeom = new Box(Vector3f.ZERO, 1f, 1f, 1f);
        // CollisionShape to be shared across several boxes.
        CollisionShape shape = new BoxCollisionShape(new Vector3f(1, 1, 1));

        Node physicsBox = PhysicsTestHelper.createPhysicsTestNode(assetManager, shape, 1);
        physicsBox.setName("box0");
        physicsBox.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(.6f, 4, .5f));
        rootNode.attachChild(physicsBox);
        getPhysicsSpace().add(physicsBox);

        Node physicsBox1 = PhysicsTestHelper.createPhysicsTestNode(assetManager, shape, 1);
        physicsBox1.setName("box1");
        physicsBox1.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0, 40, 0));
        rootNode.attachChild(physicsBox1);
        getPhysicsSpace().add(physicsBox1);

        Node physicsBox2 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new BoxCollisionShape(new Vector3f(1, 1, 1)), 1);
        physicsBox2.setName("box0");
        physicsBox2.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(.5f, 80, -.8f));
        rootNode.attachChild(physicsBox2);
        getPhysicsSpace().add(physicsBox2);

        // the floor, does not move (mass=0)
        Node node = PhysicsTestHelper.createPhysicsTestNode(assetManager, new BoxCollisionShape(new Vector3f(100, 1, 100)), 0);
        node.setName("floor");
        node.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0f, -6, 0f));
        rootNode.attachChild(node);
        getPhysicsSpace().add(node);

        initGhostObject();
    }

    private PhysicsSpace getPhysicsSpace(){
        return bulletAppState.getPhysicsSpace();
    }

    private void initGhostObject() {
        Vector3f halfExtents = new Vector3f(3, 4.2f, 1);
        ghostControl = new GhostControl(new BoxCollisionShape(halfExtents));
        Node node=new Node("Ghost Object");
        node.addControl(ghostControl);
        rootNode.attachChild(node);
        getPhysicsSpace().add(ghostControl);
    }

    @Override
    public void simpleUpdate(float tpf) {
        fpsText.setText("Overlapping objects: " + ghostControl.getOverlappingObjects().toString());
    }
}
