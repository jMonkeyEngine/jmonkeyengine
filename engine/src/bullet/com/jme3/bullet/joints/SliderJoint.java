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
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <i>From bullet manual:</i><br>
 * The slider constraint allows the body to rotate around one axis and translate along this axis.
 * @author normenhansen
 */
public class SliderJoint extends PhysicsJoint {

    protected Matrix3f rotA, rotB;
    protected boolean useLinearReferenceFrameA;

    public SliderJoint() {
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = rotA;
        this.rotB = rotB;
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = new Matrix3f();
        this.rotB = new Matrix3f();
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }

    public float getLowerLinLimit() {
        return getLowerLinLimit(objectId);
    }

    private native float getLowerLinLimit(long objectId);

    public void setLowerLinLimit(float lowerLinLimit) {
        setLowerLinLimit(objectId, lowerLinLimit);
    }

    private native void setLowerLinLimit(long objectId, float value);

    public float getUpperLinLimit() {
        return getUpperLinLimit(objectId);
    }

    private native float getUpperLinLimit(long objectId);

    public void setUpperLinLimit(float upperLinLimit) {
        setUpperLinLimit(objectId, upperLinLimit);
    }

    private native void setUpperLinLimit(long objectId, float value);

    public float getLowerAngLimit() {
        return getLowerAngLimit(objectId);
    }

    private native float getLowerAngLimit(long objectId);

    public void setLowerAngLimit(float lowerAngLimit) {
        setLowerAngLimit(objectId, lowerAngLimit);
    }

    private native void setLowerAngLimit(long objectId, float value);

    public float getUpperAngLimit() {
        return getUpperAngLimit(objectId);
    }

    private native float getUpperAngLimit(long objectId);

    public void setUpperAngLimit(float upperAngLimit) {
        setUpperAngLimit(objectId, upperAngLimit);
    }

    private native void setUpperAngLimit(long objectId, float value);

    public float getSoftnessDirLin() {
        return getSoftnessDirLin(objectId);
    }

    private native float getSoftnessDirLin(long objectId);

    public void setSoftnessDirLin(float softnessDirLin) {
        setSoftnessDirLin(objectId, softnessDirLin);
    }

    private native void setSoftnessDirLin(long objectId, float value);

    public float getRestitutionDirLin() {
        return getRestitutionDirLin(objectId);
    }

    private native float getRestitutionDirLin(long objectId);

    public void setRestitutionDirLin(float restitutionDirLin) {
        setRestitutionDirLin(objectId, restitutionDirLin);
    }

    private native void setRestitutionDirLin(long objectId, float value);

    public float getDampingDirLin() {
        return getDampingDirLin(objectId);
    }

    private native float getDampingDirLin(long objectId);

    public void setDampingDirLin(float dampingDirLin) {
        setDampingDirLin(objectId, dampingDirLin);
    }

    private native void setDampingDirLin(long objectId, float value);

    public float getSoftnessDirAng() {
        return getSoftnessDirAng(objectId);
    }

    private native float getSoftnessDirAng(long objectId);

    public void setSoftnessDirAng(float softnessDirAng) {
        setSoftnessDirAng(objectId, softnessDirAng);
    }

    private native void setSoftnessDirAng(long objectId, float value);

    public float getRestitutionDirAng() {
        return getRestitutionDirAng(objectId);
    }

    private native float getRestitutionDirAng(long objectId);

    public void setRestitutionDirAng(float restitutionDirAng) {
        setRestitutionDirAng(objectId, restitutionDirAng);
    }

    private native void setRestitutionDirAng(long objectId, float value);

    public float getDampingDirAng() {
        return getDampingDirAng(objectId);
    }

    private native float getDampingDirAng(long objectId);

    public void setDampingDirAng(float dampingDirAng) {
        setDampingDirAng(objectId, dampingDirAng);
    }

    private native void setDampingDirAng(long objectId, float value);

    public float getSoftnessLimLin() {
        return getSoftnessLimLin(objectId);
    }

    private native float getSoftnessLimLin(long objectId);

    public void setSoftnessLimLin(float softnessLimLin) {
        setSoftnessLimLin(objectId, softnessLimLin);
    }

    private native void setSoftnessLimLin(long objectId, float value);

    public float getRestitutionLimLin() {
        return getRestitutionLimLin(objectId);
    }

    private native float getRestitutionLimLin(long objectId);

    public void setRestitutionLimLin(float restitutionLimLin) {
        setRestitutionLimLin(objectId, restitutionLimLin);
    }

    private native void setRestitutionLimLin(long objectId, float value);

    public float getDampingLimLin() {
        return getDampingLimLin(objectId);
    }

    private native float getDampingLimLin(long objectId);

    public void setDampingLimLin(float dampingLimLin) {
        setDampingLimLin(objectId, dampingLimLin);
    }

    private native void setDampingLimLin(long objectId, float value);

    public float getSoftnessLimAng() {
        return getSoftnessLimAng(objectId);
    }

    private native float getSoftnessLimAng(long objectId);

    public void setSoftnessLimAng(float softnessLimAng) {
        setSoftnessLimAng(objectId, softnessLimAng);
    }

    private native void setSoftnessLimAng(long objectId, float value);

    public float getRestitutionLimAng() {
        return getRestitutionLimAng(objectId);
    }

    private native float getRestitutionLimAng(long objectId);

    public void setRestitutionLimAng(float restitutionLimAng) {
        setRestitutionLimAng(objectId, restitutionLimAng);
    }

    private native void setRestitutionLimAng(long objectId, float value);

    public float getDampingLimAng() {
        return getDampingLimAng(objectId);
    }

    private native float getDampingLimAng(long objectId);

    public void setDampingLimAng(float dampingLimAng) {
        setDampingLimAng(objectId, dampingLimAng);
    }

    private native void setDampingLimAng(long objectId, float value);

    public float getSoftnessOrthoLin() {
        return getSoftnessOrthoLin(objectId);
    }

    private native float getSoftnessOrthoLin(long objectId);

    public void setSoftnessOrthoLin(float softnessOrthoLin) {
        setSoftnessOrthoLin(objectId, softnessOrthoLin);
    }

    private native void setSoftnessOrthoLin(long objectId, float value);

    public float getRestitutionOrthoLin() {
        return getRestitutionOrthoLin(objectId);
    }

    private native float getRestitutionOrthoLin(long objectId);

    public void setRestitutionOrthoLin(float restitutionOrthoLin) {
        setDampingOrthoLin(objectId, restitutionOrthoLin);
    }

    private native void setRestitutionOrthoLin(long objectId, float value);

    public float getDampingOrthoLin() {
        return getDampingOrthoLin(objectId);
    }

    private native float getDampingOrthoLin(long objectId);

    public void setDampingOrthoLin(float dampingOrthoLin) {
        setDampingOrthoLin(objectId, dampingOrthoLin);
    }

    private native void setDampingOrthoLin(long objectId, float value);

    public float getSoftnessOrthoAng() {
        return getSoftnessOrthoAng(objectId);
    }

    private native float getSoftnessOrthoAng(long objectId);

    public void setSoftnessOrthoAng(float softnessOrthoAng) {
        setSoftnessOrthoAng(objectId, softnessOrthoAng);
    }

    private native void setSoftnessOrthoAng(long objectId, float value);

    public float getRestitutionOrthoAng() {
        return getRestitutionOrthoAng(objectId);
    }

    private native float getRestitutionOrthoAng(long objectId);

    public void setRestitutionOrthoAng(float restitutionOrthoAng) {
        setRestitutionOrthoAng(objectId, restitutionOrthoAng);
    }

    private native void setRestitutionOrthoAng(long objectId, float value);

    public float getDampingOrthoAng() {
        return getDampingOrthoAng(objectId);
    }

    private native float getDampingOrthoAng(long objectId);

    public void setDampingOrthoAng(float dampingOrthoAng) {
        setDampingOrthoAng(objectId, dampingOrthoAng);
    }

    private native void setDampingOrthoAng(long objectId, float value);

    public boolean isPoweredLinMotor() {
        return isPoweredLinMotor(objectId);
    }

    private native boolean isPoweredLinMotor(long objectId);

    public void setPoweredLinMotor(boolean poweredLinMotor) {
        setPoweredLinMotor(objectId, poweredLinMotor);
    }

    private native void setPoweredLinMotor(long objectId, boolean value);

    public float getTargetLinMotorVelocity() {
        return getTargetLinMotorVelocity(objectId);
    }

    private native float getTargetLinMotorVelocity(long objectId);

    public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
        setTargetLinMotorVelocity(objectId, targetLinMotorVelocity);
    }

    private native void setTargetLinMotorVelocity(long objectId, float value);

    public float getMaxLinMotorForce() {
        return getMaxLinMotorForce(objectId);
    }

    private native float getMaxLinMotorForce(long objectId);

    public void setMaxLinMotorForce(float maxLinMotorForce) {
        setMaxLinMotorForce(objectId, maxLinMotorForce);
    }

    private native void setMaxLinMotorForce(long objectId, float value);

    public boolean isPoweredAngMotor() {
        return isPoweredAngMotor(objectId);
    }

    private native boolean isPoweredAngMotor(long objectId);

    public void setPoweredAngMotor(boolean poweredAngMotor) {
        setPoweredAngMotor(objectId, poweredAngMotor);
    }

    private native void setPoweredAngMotor(long objectId, boolean value);

    public float getTargetAngMotorVelocity() {
        return getTargetAngMotorVelocity(objectId);
    }

    private native float getTargetAngMotorVelocity(long objectId);

    public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
        setTargetAngMotorVelocity(objectId, targetAngMotorVelocity);
    }

    private native void setTargetAngMotorVelocity(long objectId, float value);

    public float getMaxAngMotorForce() {
        return getMaxAngMotorForce(objectId);
    }

    private native float getMaxAngMotorForce(long objectId);

    public void setMaxAngMotorForce(float maxAngMotorForce) {
        setMaxAngMotorForce(objectId, maxAngMotorForce);
    }

    private native void setMaxAngMotorForce(long objectId, float value);

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        //TODO: standard values..
        capsule.write(getDampingDirAng(), "dampingDirAng", 0f);
        capsule.write(getDampingDirLin(), "dampingDirLin", 0f);
        capsule.write(getDampingLimAng(), "dampingLimAng", 0f);
        capsule.write(getDampingLimLin(), "dampingLimLin", 0f);
        capsule.write(getDampingOrthoAng(), "dampingOrthoAng", 0f);
        capsule.write(getDampingOrthoLin(), "dampingOrthoLin", 0f);
        capsule.write(getLowerAngLimit(), "lowerAngLimit", 0f);
        capsule.write(getLowerLinLimit(), "lowerLinLimit", 0f);
        capsule.write(getMaxAngMotorForce(), "maxAngMotorForce", 0f);
        capsule.write(getMaxLinMotorForce(), "maxLinMotorForce", 0f);
        capsule.write(isPoweredAngMotor(), "poweredAngMotor", false);
        capsule.write(isPoweredLinMotor(), "poweredLinMotor", false);
        capsule.write(getRestitutionDirAng(), "restitutionDirAng", 0f);
        capsule.write(getRestitutionDirLin(), "restitutionDirLin", 0f);
        capsule.write(getRestitutionLimAng(), "restitutionLimAng", 0f);
        capsule.write(getRestitutionLimLin(), "restitutionLimLin", 0f);
        capsule.write(getRestitutionOrthoAng(), "restitutionOrthoAng", 0f);
        capsule.write(getRestitutionOrthoLin(), "restitutionOrthoLin", 0f);

        capsule.write(getSoftnessDirAng(), "softnessDirAng", 0f);
        capsule.write(getSoftnessDirLin(), "softnessDirLin", 0f);
        capsule.write(getSoftnessLimAng(), "softnessLimAng", 0f);
        capsule.write(getSoftnessLimLin(), "softnessLimLin", 0f);
        capsule.write(getSoftnessOrthoAng(), "softnessOrthoAng", 0f);
        capsule.write(getSoftnessOrthoLin(), "softnessOrthoLin", 0f);

        capsule.write(getTargetAngMotorVelocity(), "targetAngMotorVelicoty", 0f);
        capsule.write(getTargetLinMotorVelocity(), "targetLinMotorVelicoty", 0f);

        capsule.write(getUpperAngLimit(), "upperAngLimit", 0f);
        capsule.write(getUpperLinLimit(), "upperLinLimit", 0f);

        capsule.write(useLinearReferenceFrameA, "useLinearReferenceFrameA", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        float dampingDirAng = capsule.readFloat("dampingDirAng", 0f);
        float dampingDirLin = capsule.readFloat("dampingDirLin", 0f);
        float dampingLimAng = capsule.readFloat("dampingLimAng", 0f);
        float dampingLimLin = capsule.readFloat("dampingLimLin", 0f);
        float dampingOrthoAng = capsule.readFloat("dampingOrthoAng", 0f);
        float dampingOrthoLin = capsule.readFloat("dampingOrthoLin", 0f);
        float lowerAngLimit = capsule.readFloat("lowerAngLimit", 0f);
        float lowerLinLimit = capsule.readFloat("lowerLinLimit", 0f);
        float maxAngMotorForce = capsule.readFloat("maxAngMotorForce", 0f);
        float maxLinMotorForce = capsule.readFloat("maxLinMotorForce", 0f);
        boolean poweredAngMotor = capsule.readBoolean("poweredAngMotor", false);
        boolean poweredLinMotor = capsule.readBoolean("poweredLinMotor", false);
        float restitutionDirAng = capsule.readFloat("restitutionDirAng", 0f);
        float restitutionDirLin = capsule.readFloat("restitutionDirLin", 0f);
        float restitutionLimAng = capsule.readFloat("restitutionLimAng", 0f);
        float restitutionLimLin = capsule.readFloat("restitutionLimLin", 0f);
        float restitutionOrthoAng = capsule.readFloat("restitutionOrthoAng", 0f);
        float restitutionOrthoLin = capsule.readFloat("restitutionOrthoLin", 0f);

        float softnessDirAng = capsule.readFloat("softnessDirAng", 0f);
        float softnessDirLin = capsule.readFloat("softnessDirLin", 0f);
        float softnessLimAng = capsule.readFloat("softnessLimAng", 0f);
        float softnessLimLin = capsule.readFloat("softnessLimLin", 0f);
        float softnessOrthoAng = capsule.readFloat("softnessOrthoAng", 0f);
        float softnessOrthoLin = capsule.readFloat("softnessOrthoLin", 0f);

        float targetAngMotorVelicoty = capsule.readFloat("targetAngMotorVelicoty", 0f);
        float targetLinMotorVelicoty = capsule.readFloat("targetLinMotorVelicoty", 0f);

        float upperAngLimit = capsule.readFloat("upperAngLimit", 0f);
        float upperLinLimit = capsule.readFloat("upperLinLimit", 0f);

        useLinearReferenceFrameA = capsule.readBoolean("useLinearReferenceFrameA", false);

        createJoint();

        setDampingDirAng(dampingDirAng);
        setDampingDirLin(dampingDirLin);
        setDampingLimAng(dampingLimAng);
        setDampingLimLin(dampingLimLin);
        setDampingOrthoAng(dampingOrthoAng);
        setDampingOrthoLin(dampingOrthoLin);
        setLowerAngLimit(lowerAngLimit);
        setLowerLinLimit(lowerLinLimit);
        setMaxAngMotorForce(maxAngMotorForce);
        setMaxLinMotorForce(maxLinMotorForce);
        setPoweredAngMotor(poweredAngMotor);
        setPoweredLinMotor(poweredLinMotor);
        setRestitutionDirAng(restitutionDirAng);
        setRestitutionDirLin(restitutionDirLin);
        setRestitutionLimAng(restitutionLimAng);
        setRestitutionLimLin(restitutionLimLin);
        setRestitutionOrthoAng(restitutionOrthoAng);
        setRestitutionOrthoLin(restitutionOrthoLin);

        setSoftnessDirAng(softnessDirAng);
        setSoftnessDirLin(softnessDirLin);
        setSoftnessLimAng(softnessLimAng);
        setSoftnessLimLin(softnessLimLin);
        setSoftnessOrthoAng(softnessOrthoAng);
        setSoftnessOrthoLin(softnessOrthoLin);

        setTargetAngMotorVelocity(targetAngMotorVelicoty);
        setTargetLinMotorVelocity(targetLinMotorVelicoty);

        setUpperAngLimit(upperAngLimit);
        setUpperLinLimit(upperLinLimit);
    }

    protected void createJoint() {
        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(), pivotA, rotA, pivotB, rotB, useLinearReferenceFrameA);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Joint {0}", Long.toHexString(objectId));
        // = new SliderConstraint(nodeA.getObjectId(), nodeB.getObjectId(), transA, transB, useLinearReferenceFrameA);
    }

    private native long createJoint(long objectIdA, long objectIdB, Vector3f pivotA, Matrix3f rotA, Vector3f pivotB, Matrix3f rotB, boolean useLinearReferenceFrameA);
}
