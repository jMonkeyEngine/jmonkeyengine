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
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 *
 * @author normenhansen
 */
public class TestRagDoll extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState = new BulletAppState();
    private Node ragDoll = new Node();
    private Node shoulders;
    private Vector3f upforce = new Vector3f(0, 200, 0);
    private boolean applyForce = false;

    public static void main(String[] args) {
        TestRagDoll app = new TestRagDoll();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        inputManager.addMapping("Pull ragdoll up", new MouseButtonTrigger(0));
        inputManager.addListener(this, "Pull ragdoll up");
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        createRagDoll();
    }

    private void createRagDoll() {
        shoulders = createLimb(0.2f, 1.0f, new Vector3f(0.00f, 1.5f, 0), true);
        Node uArmL = createLimb(0.2f, 0.5f, new Vector3f(-0.75f, 0.8f, 0), false);
        Node uArmR = createLimb(0.2f, 0.5f, new Vector3f(0.75f, 0.8f, 0), false);
        Node lArmL = createLimb(0.2f, 0.5f, new Vector3f(-0.75f, -0.2f, 0), false);
        Node lArmR = createLimb(0.2f, 0.5f, new Vector3f(0.75f, -0.2f, 0), false);
        Node body = createLimb(0.2f, 1.0f, new Vector3f(0.00f, 0.5f, 0), false);
        Node hips = createLimb(0.2f, 0.5f, new Vector3f(0.00f, -0.5f, 0), true);
        Node uLegL = createLimb(0.2f, 0.5f, new Vector3f(-0.25f, -1.2f, 0), false);
        Node uLegR = createLimb(0.2f, 0.5f, new Vector3f(0.25f, -1.2f, 0), false);
        Node lLegL = createLimb(0.2f, 0.5f, new Vector3f(-0.25f, -2.2f, 0), false);
        Node lLegR = createLimb(0.2f, 0.5f, new Vector3f(0.25f, -2.2f, 0), false);

        join(body, shoulders, new Vector3f(0f, 1.4f, 0));
        join(body, hips, new Vector3f(0f, -0.5f, 0));

        join(uArmL, shoulders, new Vector3f(-0.75f, 1.4f, 0));
        join(uArmR, shoulders, new Vector3f(0.75f, 1.4f, 0));
        join(uArmL, lArmL, new Vector3f(-0.75f, .4f, 0));
        join(uArmR, lArmR, new Vector3f(0.75f, .4f, 0));

        join(uLegL, hips, new Vector3f(-.25f, -0.5f, 0));
        join(uLegR, hips, new Vector3f(.25f, -0.5f, 0));
        join(uLegL, lLegL, new Vector3f(-.25f, -1.7f, 0));
        join(uLegR, lLegR, new Vector3f(.25f, -1.7f, 0));

        ragDoll.attachChild(shoulders);
        ragDoll.attachChild(body);
        ragDoll.attachChild(hips);
        ragDoll.attachChild(uArmL);
        ragDoll.attachChild(uArmR);
        ragDoll.attachChild(lArmL);
        ragDoll.attachChild(lArmR);
        ragDoll.attachChild(uLegL);
        ragDoll.attachChild(uLegR);
        ragDoll.attachChild(lLegL);
        ragDoll.attachChild(lLegR);

        rootNode.attachChild(ragDoll);
        bulletAppState.getPhysicsSpace().addAll(ragDoll);
    }

    private Node createLimb(float width, float height, Vector3f location, boolean rotate) {
        int axis = rotate ? PhysicsSpace.AXIS_X : PhysicsSpace.AXIS_Y;
        CapsuleCollisionShape shape = new CapsuleCollisionShape(width, height, axis);
        Node node = new Node("Limb");
        RigidBodyControl rigidBodyControl = new RigidBodyControl(shape, 1);
        node.setLocalTranslation(location);
        node.addControl(rigidBodyControl);
        return node;
    }

    private PhysicsJoint join(Node A, Node B, Vector3f connectionPoint) {
        Vector3f pivotA = A.worldToLocal(connectionPoint, new Vector3f());
        Vector3f pivotB = B.worldToLocal(connectionPoint, new Vector3f());
        ConeJoint joint = new ConeJoint(A.getControl(RigidBodyControl.class), B.getControl(RigidBodyControl.class), pivotA, pivotB);
        joint.setLimit(1f, 1f, 0);
        return joint;
    }

    public void onAction(String string, boolean bln, float tpf) {
        if ("Pull ragdoll up".equals(string)) {
            if (bln) {
                shoulders.getControl(RigidBodyControl.class).activate();
                applyForce = true;
            } else {
                applyForce = false;
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (applyForce) {
            shoulders.getControl(RigidBodyControl.class).applyForce(upforce, Vector3f.ZERO);
        }
    }
}
