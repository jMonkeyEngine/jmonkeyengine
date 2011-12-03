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

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <i>From bullet manual:</i><br>
 * Hinge constraint, or revolute joint restricts two additional angular degrees of freedom,
 * so the body can only rotate around one axis, the hinge axis.
 * This can be useful to represent doors or wheels rotating around one axis.
 * The user can specify limits and motor for the hinge.
 * @author normenhansen
 */
public class HingeJoint extends PhysicsJoint {

    protected Vector3f axisA;
    protected Vector3f axisB;
    protected boolean angularOnly = false;
    protected float biasFactor = 0.3f;
    protected float relaxationFactor = 1.0f;
    protected float limitSoftness = 0.9f;

    public HingeJoint() {
    }

    /**
     * Creates a new HingeJoint
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public HingeJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Vector3f axisA, Vector3f axisB) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.axisA = axisA;
        this.axisB = axisB;
        createJoint();
    }

    public void enableMotor(boolean enable, float targetVelocity, float maxMotorImpulse) {
        enableMotor(objectId, enable, targetVelocity, maxMotorImpulse);
    }

    private native void enableMotor(long objectId, boolean enable, float targetVelocity, float maxMotorImpulse);

    public boolean getEnableMotor() {
        return getEnableAngularMotor(objectId);
    }

    private native boolean getEnableAngularMotor(long objectId);

    public float getMotorTargetVelocity() {
        return getMotorTargetVelocity(objectId);
    }

    private native float getMotorTargetVelocity(long objectId);

    public float getMaxMotorImpulse() {
        return getMaxMotorImpulse(objectId);
    }

    private native float getMaxMotorImpulse(long objectId);

    public void setLimit(float low, float high) {
        setLimit(objectId, low, high);
    }

    private native void setLimit(long objectId, float low, float high);

    public void setLimit(float low, float high, float _softness, float _biasFactor, float _relaxationFactor) {
        biasFactor = _biasFactor;
        relaxationFactor = _relaxationFactor;
        limitSoftness = _softness;
        setLimit(objectId, low, high, _softness, _biasFactor, _relaxationFactor);
    }

    private native void setLimit(long objectId, float low, float high, float _softness, float _biasFactor, float _relaxationFactor);

    public float getUpperLimit() {
        return getUpperLimit(objectId);
    }

    private native float getUpperLimit(long objectId);

    public float getLowerLimit() {
        return getLowerLimit(objectId);
    }

    private native float getLowerLimit(long objectId);

    public void setAngularOnly(boolean angularOnly) {
        this.angularOnly = angularOnly;
        setAngularOnly(objectId, angularOnly);
    }

    private native void setAngularOnly(long objectId, boolean angularOnly);

    public float getHingeAngle() {
        return getHingeAngle(objectId);
    }

    private native float getHingeAngle(long objectId);

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(axisA, "axisA", new Vector3f());
        capsule.write(axisB, "axisB", new Vector3f());

        capsule.write(angularOnly, "angularOnly", false);

        capsule.write(getLowerLimit(), "lowerLimit", 1e30f);
        capsule.write(getUpperLimit(), "upperLimit", -1e30f);

        capsule.write(biasFactor, "biasFactor", 0.3f);
        capsule.write(relaxationFactor, "relaxationFactor", 1f);
        capsule.write(limitSoftness, "limitSoftness", 0.9f);

        capsule.write(getEnableMotor(), "enableAngularMotor", false);
        capsule.write(getMotorTargetVelocity(), "targetVelocity", 0.0f);
        capsule.write(getMaxMotorImpulse(), "maxMotorImpulse", 0.0f);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        this.axisA = (Vector3f) capsule.readSavable("axisA", new Vector3f());
        this.axisB = (Vector3f) capsule.readSavable("axisB", new Vector3f());

        this.angularOnly = capsule.readBoolean("angularOnly", false);
        float lowerLimit = capsule.readFloat("lowerLimit", 1e30f);
        float upperLimit = capsule.readFloat("upperLimit", -1e30f);

        this.biasFactor = capsule.readFloat("biasFactor", 0.3f);
        this.relaxationFactor = capsule.readFloat("relaxationFactor", 1f);
        this.limitSoftness = capsule.readFloat("limitSoftness", 0.9f);

        boolean enableAngularMotor = capsule.readBoolean("enableAngularMotor", false);
        float targetVelocity = capsule.readFloat("targetVelocity", 0.0f);
        float maxMotorImpulse = capsule.readFloat("maxMotorImpulse", 0.0f);

        createJoint();
        enableMotor(enableAngularMotor, targetVelocity, maxMotorImpulse);
        setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor, relaxationFactor);
    }

    protected void createJoint() {
        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(), pivotA, axisA, pivotB, axisB);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Joint {0}", Long.toHexString(objectId));
    }

    private native long createJoint(long objectIdA, long objectIdB, Vector3f pivotA, Vector3f axisA, Vector3f pivotB, Vector3f axisB);
}
