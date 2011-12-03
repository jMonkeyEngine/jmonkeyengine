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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

/**
 * This is a basic Test of jbullet-jme functions
 *
 * @author normenhansen
 */
public class TestSimplePhysics extends SimpleApplication {

    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        TestSimplePhysics app = new TestSimplePhysics();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // Add a physics sphere to the world
        Node physicsSphere = PhysicsTestHelper.createPhysicsTestNode(assetManager, new SphereCollisionShape(1), 1);
        physicsSphere.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(3, 6, 0));
        rootNode.attachChild(physicsSphere);
        getPhysicsSpace().add(physicsSphere);

        // Add a physics sphere to the world using the collision shape from sphere one
        Node physicsSphere2 = PhysicsTestHelper.createPhysicsTestNode(assetManager, physicsSphere.getControl(RigidBodyControl.class).getCollisionShape(), 1);
        physicsSphere2.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(4, 8, 0));
        rootNode.attachChild(physicsSphere2);
        getPhysicsSpace().add(physicsSphere2);

        // Add a physics box to the world
        Node physicsBox = PhysicsTestHelper.createPhysicsTestNode(assetManager, new BoxCollisionShape(new Vector3f(1, 1, 1)), 1);
        physicsBox.getControl(RigidBodyControl.class).setFriction(0.1f);
        physicsBox.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(.6f, 4, .5f));
        rootNode.attachChild(physicsBox);
        getPhysicsSpace().add(physicsBox);

        // Add a physics cylinder to the world
        Node physicsCylinder = PhysicsTestHelper.createPhysicsTestNode(assetManager, new CylinderCollisionShape(new Vector3f(1f, 1f, 1.5f)), 1);
        physicsCylinder.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(2, 2, 0));
        rootNode.attachChild(physicsCylinder);
        getPhysicsSpace().add(physicsCylinder);

        // an obstacle mesh, does not move (mass=0)
        Node node2 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new MeshCollisionShape(new Sphere(16, 16, 1.2f)), 0);
        node2.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(2.5f, -4, 0f));
        rootNode.attachChild(node2);
        getPhysicsSpace().add(node2);

        // the floor mesh, does not move (mass=0)
        Node node3 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new PlaneCollisionShape(new Plane(new Vector3f(0, 1, 0), 0)), 0);
        node3.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0f, -6, 0f));
        rootNode.attachChild(node3);
        getPhysicsSpace().add(node3);

        // Join the physics objects with a Point2Point joint
//        PhysicsPoint2PointJoint joint=new PhysicsPoint2PointJoint(physicsSphere, physicsBox, new Vector3f(-2,0,0), new Vector3f(2,0,0));
//        PhysicsHingeJoint joint=new PhysicsHingeJoint(physicsSphere, physicsBox, new Vector3f(-2,0,0), new Vector3f(2,0,0), Vector3f.UNIT_Z,Vector3f.UNIT_Z);
//        getPhysicsSpace().add(joint);

    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
}
