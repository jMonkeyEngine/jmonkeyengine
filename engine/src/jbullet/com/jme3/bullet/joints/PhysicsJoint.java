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
package com.jme3.bullet.joints;

import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * <p>PhysicsJoint - Basic Phyiscs Joint</p>
 * @author normenhansen
 */
public abstract class PhysicsJoint implements Savable {

    protected TypedConstraint constraint;
    protected PhysicsRigidBody nodeA;
    protected PhysicsRigidBody nodeB;
    protected Vector3f pivotA;
    protected Vector3f pivotB;
    protected boolean collisionBetweenLinkedBodys = true;

    public PhysicsJoint() {
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public PhysicsJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.pivotA = pivotA;
        this.pivotB = pivotB;
        nodeA.addJoint(this);
        nodeB.addJoint(this);
    }

    public float getAppliedImpulse(){
        return constraint.getAppliedImpulse();
    }

    /**
     * @return the constraint
     */
    public TypedConstraint getObjectId() {
        return constraint;
    }

    /**
     * @return the collisionBetweenLinkedBodys
     */
    public boolean isCollisionBetweenLinkedBodys() {
        return collisionBetweenLinkedBodys;
    }

    /**
     * toggles collisions between linked bodys<br>
     * joint has to be removed from and added to PhyiscsSpace to apply this.
     * @param collisionBetweenLinkedBodys set to false to have no collisions between linked bodys
     */
    public void setCollisionBetweenLinkedBodys(boolean collisionBetweenLinkedBodys) {
        this.collisionBetweenLinkedBodys = collisionBetweenLinkedBodys;
    }

    public PhysicsRigidBody getBodyA() {
        return nodeA;
    }

    public PhysicsRigidBody getBodyB() {
        return nodeB;
    }

    public Vector3f getPivotA() {
        return pivotA;
    }

    public Vector3f getPivotB() {
        return pivotB;
    }

    /**
     * destroys this joint and removes it from its connected PhysicsRigidBodys joint lists
     */
    public void destroy() {
        getBodyA().removeJoint(this);
        getBodyB().removeJoint(this);
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(nodeA, "nodeA", null);
        capsule.write(nodeB, "nodeB", null);
        capsule.write(pivotA, "pivotA", null);
        capsule.write(pivotB, "pivotB", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.nodeA = ((PhysicsRigidBody) capsule.readSavable("nodeA", new PhysicsRigidBody()));
        this.nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB", new PhysicsRigidBody());
        this.pivotA = (Vector3f) capsule.readSavable("pivotA", new Vector3f());
        this.pivotB = (Vector3f) capsule.readSavable("pivotB", new Vector3f());
    }

}
