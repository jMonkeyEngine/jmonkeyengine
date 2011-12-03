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

import com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint;
import com.bulletphysics.linearmath.Transform;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * <i>From bullet manual:</i><br>
 * To create ragdolls, the conve twist constraint is very useful for limbs like the upper arm.
 * It is a special point to point constraint that adds cone and twist axis limits.
 * The x-axis serves as twist axis.
 * @author normenhansen
 */
public class ConeJoint extends PhysicsJoint {

    protected Matrix3f rotA, rotB;
    protected float swingSpan1 = 1e30f;
    protected float swingSpan2 = 1e30f;
    protected float twistSpan = 1e30f;
    protected boolean angularOnly = false;

    public ConeJoint() {
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public ConeJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = new Matrix3f();
        this.rotB = new Matrix3f();
        createJoint();
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public ConeJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = rotA;
        this.rotB = rotB;
        createJoint();
    }

    public void setLimit(float swingSpan1, float swingSpan2, float twistSpan) {
        this.swingSpan1 = swingSpan1;
        this.swingSpan2 = swingSpan2;
        this.twistSpan = twistSpan;
        ((ConeTwistConstraint) constraint).setLimit(swingSpan1, swingSpan2, twistSpan);
    }

    public void setAngularOnly(boolean value) {
        angularOnly = value;
        ((ConeTwistConstraint) constraint).setAngularOnly(value);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(rotA, "rotA", new Matrix3f());
        capsule.write(rotB, "rotB", new Matrix3f());

        capsule.write(angularOnly, "angularOnly", false);
        capsule.write(swingSpan1, "swingSpan1", 1e30f);
        capsule.write(swingSpan2, "swingSpan2", 1e30f);
        capsule.write(twistSpan, "twistSpan", 1e30f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        this.rotA = (Matrix3f) capsule.readSavable("rotA", new Matrix3f());
        this.rotB = (Matrix3f) capsule.readSavable("rotB", new Matrix3f());

        this.angularOnly = capsule.readBoolean("angularOnly", false);
        this.swingSpan1 = capsule.readFloat("swingSpan1", 1e30f);
        this.swingSpan2 = capsule.readFloat("swingSpan2", 1e30f);
        this.twistSpan = capsule.readFloat("twistSpan", 1e30f);
        createJoint();
    }

    protected void createJoint() {
        Transform transA = new Transform(Converter.convert(rotA));
        Converter.convert(pivotA, transA.origin);
        Converter.convert(rotA, transA.basis);

        Transform transB = new Transform(Converter.convert(rotB));
        Converter.convert(pivotB, transB.origin);
        Converter.convert(rotB, transB.basis);

        constraint = new ConeTwistConstraint(nodeA.getObjectId(), nodeB.getObjectId(), transA, transB);
        ((ConeTwistConstraint) constraint).setLimit(swingSpan1, swingSpan2, twistSpan);
        ((ConeTwistConstraint) constraint).setAngularOnly(angularOnly);
    }
}
