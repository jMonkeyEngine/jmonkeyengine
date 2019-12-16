/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.bullet.joints;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract base class for physics joints based on Bullet's
 * btTypedConstraint, used to connect 2 dynamic rigid bodies in the same
 * physics space.
 * <p>
 * Joints include ConeJoint, HingeJoint, Point2PointJoint, and SixDofJoint.
 *
 * @author normenhansen
 */
public abstract class PhysicsJoint implements Savable {

    /**
     * Unique identifier of the Bullet constraint. Constructors are responsible
     * for setting this to a non-zero value. After that, the id never changes.
     */
    protected long objectId = 0;
    /**
     * one of the connected rigid bodies
     */
    protected PhysicsRigidBody nodeA;
    /**
     * the other connected rigid body
     */
    protected PhysicsRigidBody nodeB;
    /**
     * local offset of this joint's connection point in node A
     */
    protected Vector3f pivotA;
    /**
     * local offset of this joint's connection point in node B
     */
    protected Vector3f pivotB;
    protected boolean collisionBetweenLinkedBodys = true;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected PhysicsJoint() {
    }

    /**
     * Instantiate a PhysicsJoint. To be effective, the joint must be added to
     * the physics space of the two bodies. Also, the bodies must be dynamic and
     * distinct.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA local offset of the joint connection point in node A (not
     * null, alias created)
     * @param pivotB local offset of the joint connection point in node B (not
     * null, alias created)
     */
    public PhysicsJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.pivotA = pivotA;
        this.pivotB = pivotB;
        nodeA.addJoint(this);
        nodeB.addJoint(this);
    }

    /**
     * Read the magnitude of the applied impulse.
     *
     * @return impulse
     */
    public float getAppliedImpulse() {
        return getAppliedImpulse(objectId);
    }

    private native float getAppliedImpulse(long objectId);

    /**
     * Read the id of the Bullet constraint.
     *
     * @return the unique identifier (not zero)
     */
    public long getObjectId() {
        return objectId;
    }

    /**
     * Test whether collisions are allowed between the linked bodies.
     *
     * @return true if collision are allowed, otherwise false
     */
    public boolean isCollisionBetweenLinkedBodys() {
        return collisionBetweenLinkedBodys;
    }

    /**
     * Enable or disable collisions between the linked bodies. The joint must be
     * removed from and added to PhysicsSpace for this change to be effective.
     *
     * @param collisionBetweenLinkedBodys true &rarr; allow collisions, false &rarr; prevent them
     */
    public void setCollisionBetweenLinkedBodys(boolean collisionBetweenLinkedBodys) {
        this.collisionBetweenLinkedBodys = collisionBetweenLinkedBodys;
    }

    /**
     * Access the 1st body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyA() {
        return nodeA;
    }

    /**
     * Access the 2nd body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyB() {
        return nodeB;
    }

    /**
     * Access the local offset of the joint connection point in node A.
     *
     * @return the pre-existing vector (not null)
     */
    public Vector3f getPivotA() {
        return pivotA;
    }

    /**
     * Access the local offset of the joint connection point in node A.
     *
     * @return the pre-existing vector (not null)
     */
    public Vector3f getPivotB() {
        return pivotB;
    }

    /**
     * Destroy this joint and remove it from the joint lists of its connected
     * bodies.
     */
    public void destroy() {
        getBodyA().removeJoint(this);
        getBodyB().removeJoint(this);
    }

    /**
     * Serialize this joint, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(nodeA, "nodeA", null);
        capsule.write(nodeB, "nodeB", null);
        capsule.write(pivotA, "pivotA", null);
        capsule.write(pivotB, "pivotB", null);
    }

    /**
     * De-serialize this joint, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.nodeA = ((PhysicsRigidBody) capsule.readSavable("nodeA", null));
        this.nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB", null);
        this.pivotA = (Vector3f) capsule.readSavable("pivotA", new Vector3f());
        this.pivotB = (Vector3f) capsule.readSavable("pivotB", new Vector3f());
    }

    /**
     * Finalize this physics joint just before it is destroyed. Should be
     * invoked only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Joint {0}", Long.toHexString(objectId));
        finalizeNative(objectId);
    }

    private native void finalizeNative(long objectId);
}
