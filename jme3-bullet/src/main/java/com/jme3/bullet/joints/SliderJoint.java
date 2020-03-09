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
 * A slider joint based on Bullet's btSliderConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * The slider constraint allows the body to rotate around one axis and translate
 * along this axis.
 *
 * @author normenhansen
 */
public class SliderJoint extends PhysicsJoint {

    protected Matrix3f rotA, rotB;
    protected boolean useLinearReferenceFrameA;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected SliderJoint() {
    }

    /**
     * Instantiate a SliderJoint. To be effective, the joint must be added to a
     * physics space.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA the local offset of the connection point in node A (not
     * null, alias created)
     * @param pivotB the local offset of the connection point in node B (not
     * null, alias created)
     * @param rotA the local orientation of the connection to node A (not null, alias created)
     * @param rotB the local orientation of the connection to node B (not null, alias created)
     * @param useLinearReferenceFrameA true&rarr;use node A, false&rarr;use node
     * B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = rotA;
        this.rotB = rotB;
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * Instantiate a SliderJoint. To be effective, the joint must be added to a
     * physics space.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA the local offset of the connection point in node A (not
     * null, alias created)
     * @param pivotB the local offset of the connection point in node B (not
     * null, alias created)
     * @param useLinearReferenceFrameA true&rarr;use node A, false&rarr;use node
     * B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = new Matrix3f();
        this.rotB = new Matrix3f();
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * Read the joint's lower limit for on-axis translation.
     *
     * @return the lower limit
     */
    public float getLowerLinLimit() {
        return getLowerLinLimit(objectId);
    }

    private native float getLowerLinLimit(long objectId);

    /**
     * Alter the joint's lower limit for on-axis translation.
     *
     * @param lowerLinLimit the desired lower limit (default=-1)
     */
    public void setLowerLinLimit(float lowerLinLimit) {
        setLowerLinLimit(objectId, lowerLinLimit);
    }

    private native void setLowerLinLimit(long objectId, float value);

    /**
     * Read the joint's upper limit for on-axis translation.
     *
     * @return the upper limit
     */
    public float getUpperLinLimit() {
        return getUpperLinLimit(objectId);
    }

    private native float getUpperLinLimit(long objectId);

    /**
     * Alter the joint's upper limit for on-axis translation.
     *
     * @param upperLinLimit the desired upper limit (default=1)
     */
    public void setUpperLinLimit(float upperLinLimit) {
        setUpperLinLimit(objectId, upperLinLimit);
    }

    private native void setUpperLinLimit(long objectId, float value);

    /**
     * Read the joint's lower limit for on-axis rotation.
     *
     * @return the lower limit angle (in radians)
     */
    public float getLowerAngLimit() {
        return getLowerAngLimit(objectId);
    }

    private native float getLowerAngLimit(long objectId);

    /**
     * Alter the joint's lower limit for on-axis rotation.
     *
     * @param lowerAngLimit the desired lower limit angle (in radians,
     * default=0)
     */
    public void setLowerAngLimit(float lowerAngLimit) {
        setLowerAngLimit(objectId, lowerAngLimit);
    }

    private native void setLowerAngLimit(long objectId, float value);

    /**
     * Read the joint's upper limit for on-axis rotation.
     *
     * @return the upper limit angle (in radians)
     */
    public float getUpperAngLimit() {
        return getUpperAngLimit(objectId);
    }

    private native float getUpperAngLimit(long objectId);

    /**
     * Alter the joint's upper limit for on-axis rotation.
     *
     * @param upperAngLimit the desired upper limit angle (in radians,
     * default=0)
     */
    public void setUpperAngLimit(float upperAngLimit) {
        setUpperAngLimit(objectId, upperAngLimit);
    }

    private native void setUpperAngLimit(long objectId, float value);

    /**
     * Read the joint's softness for on-axis translation between the limits.
     *
     * @return the softness
     */
    public float getSoftnessDirLin() {
        return getSoftnessDirLin(objectId);
    }

    private native float getSoftnessDirLin(long objectId);

    /**
     * Alter the joint's softness for on-axis translation between the limits.
     *
     * @param softnessDirLin the desired softness (default=1)
     */
    public void setSoftnessDirLin(float softnessDirLin) {
        setSoftnessDirLin(objectId, softnessDirLin);
    }

    private native void setSoftnessDirLin(long objectId, float value);

    /**
     * Read the joint's restitution for on-axis translation between the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionDirLin() {
        return getRestitutionDirLin(objectId);
    }

    private native float getRestitutionDirLin(long objectId);

    /**
     * Alter the joint's restitution for on-axis translation between the limits.
     *
     * @param restitutionDirLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionDirLin(float restitutionDirLin) {
        setRestitutionDirLin(objectId, restitutionDirLin);
    }

    private native void setRestitutionDirLin(long objectId, float value);

    /**
     * Read the joint's damping for on-axis translation between the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingDirLin() {
        return getDampingDirLin(objectId);
    }

    private native float getDampingDirLin(long objectId);

    /**
     * Alter the joint's damping for on-axis translation between the limits.
     *
     * @param dampingDirLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=0)
     */
    public void setDampingDirLin(float dampingDirLin) {
        setDampingDirLin(objectId, dampingDirLin);
    }

    private native void setDampingDirLin(long objectId, float value);

    /**
     * Read the joint's softness for on-axis rotation between the limits.
     *
     * @return the softness
     */
    public float getSoftnessDirAng() {
        return getSoftnessDirAng(objectId);
    }

    private native float getSoftnessDirAng(long objectId);

    /**
     * Alter the joint's softness for on-axis rotation between the limits.
     *
     * @param softnessDirAng the desired softness (default=1)
     */
    public void setSoftnessDirAng(float softnessDirAng) {
        setSoftnessDirAng(objectId, softnessDirAng);
    }

    private native void setSoftnessDirAng(long objectId, float value);

    /**
     * Read the joint's restitution for on-axis rotation between the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionDirAng() {
        return getRestitutionDirAng(objectId);
    }

    private native float getRestitutionDirAng(long objectId);

    /**
     * Alter the joint's restitution for on-axis rotation between the limits.
     *
     * @param restitutionDirAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionDirAng(float restitutionDirAng) {
        setRestitutionDirAng(objectId, restitutionDirAng);
    }

    private native void setRestitutionDirAng(long objectId, float value);

    /**
     * Read the joint's damping for on-axis rotation between the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingDirAng() {
        return getDampingDirAng(objectId);
    }

    private native float getDampingDirAng(long objectId);

    /**
     * Alter the joint's damping for on-axis rotation between the limits.
     *
     * @param dampingDirAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=0)
     */
    public void setDampingDirAng(float dampingDirAng) {
        setDampingDirAng(objectId, dampingDirAng);
    }

    private native void setDampingDirAng(long objectId, float value);

    /**
     * Read the joint's softness for on-axis translation hitting the limits.
     *
     * @return the softness
     */
    public float getSoftnessLimLin() {
        return getSoftnessLimLin(objectId);
    }

    private native float getSoftnessLimLin(long objectId);

    /**
     * Alter the joint's softness for on-axis translation hitting the limits.
     *
     * @param softnessLimLin the desired softness (default=1)
     */
    public void setSoftnessLimLin(float softnessLimLin) {
        setSoftnessLimLin(objectId, softnessLimLin);
    }

    private native void setSoftnessLimLin(long objectId, float value);

    /**
     * Read the joint's restitution for on-axis translation hitting the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionLimLin() {
        return getRestitutionLimLin(objectId);
    }

    private native float getRestitutionLimLin(long objectId);

    /**
     * Alter the joint's restitution for on-axis translation hitting the limits.
     *
     * @param restitutionLimLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionLimLin(float restitutionLimLin) {
        setRestitutionLimLin(objectId, restitutionLimLin);
    }

    private native void setRestitutionLimLin(long objectId, float value);

    /**
     * Read the joint's damping for on-axis translation hitting the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingLimLin() {
        return getDampingLimLin(objectId);
    }

    private native float getDampingLimLin(long objectId);

    /**
     * Alter the joint's damping for on-axis translation hitting the limits.
     *
     * @param dampingLimLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingLimLin(float dampingLimLin) {
        setDampingLimLin(objectId, dampingLimLin);
    }

    private native void setDampingLimLin(long objectId, float value);

    /**
     * Read the joint's softness for on-axis rotation hitting the limits.
     *
     * @return the softness
     */
    public float getSoftnessLimAng() {
        return getSoftnessLimAng(objectId);
    }

    private native float getSoftnessLimAng(long objectId);

    /**
     * Alter the joint's softness for on-axis rotation hitting the limits.
     *
     * @param softnessLimAng the desired softness (default=1)
     */
    public void setSoftnessLimAng(float softnessLimAng) {
        setSoftnessLimAng(objectId, softnessLimAng);
    }

    private native void setSoftnessLimAng(long objectId, float value);

    /**
     * Read the joint's restitution for on-axis rotation hitting the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionLimAng() {
        return getRestitutionLimAng(objectId);
    }

    private native float getRestitutionLimAng(long objectId);

    /**
     * Alter the joint's restitution for on-axis rotation hitting the limits.
     *
     * @param restitutionLimAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionLimAng(float restitutionLimAng) {
        setRestitutionLimAng(objectId, restitutionLimAng);
    }

    private native void setRestitutionLimAng(long objectId, float value);

    /**
     * Read the joint's damping for on-axis rotation hitting the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingLimAng() {
        return getDampingLimAng(objectId);
    }

    private native float getDampingLimAng(long objectId);

    /**
     * Alter the joint's damping for on-axis rotation hitting the limits.
     *
     * @param dampingLimAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingLimAng(float dampingLimAng) {
        setDampingLimAng(objectId, dampingLimAng);
    }

    private native void setDampingLimAng(long objectId, float value);

    /**
     * Read the joint's softness for off-axis translation.
     *
     * @return the softness
     */
    public float getSoftnessOrthoLin() {
        return getSoftnessOrthoLin(objectId);
    }

    private native float getSoftnessOrthoLin(long objectId);

    /**
     * Alter the joint's softness for off-axis translation.
     *
     * @param softnessOrthoLin the desired softness (default=1)
     */
    public void setSoftnessOrthoLin(float softnessOrthoLin) {
        setSoftnessOrthoLin(objectId, softnessOrthoLin);
    }

    private native void setSoftnessOrthoLin(long objectId, float value);

    /**
     * Read the joint's restitution for off-axis translation.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionOrthoLin() {
        return getRestitutionOrthoLin(objectId);
    }

    private native float getRestitutionOrthoLin(long objectId);

    /**
     * Alter the joint's restitution for off-axis translation.
     *
     * @param restitutionOrthoLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionOrthoLin(float restitutionOrthoLin) {
        setRestitutionOrthoLin(objectId, restitutionOrthoLin);
    }

    private native void setRestitutionOrthoLin(long objectId, float value);

    /**
     * Read the joint's damping for off-axis translation.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingOrthoLin() {
        return getDampingOrthoLin(objectId);
    }

    private native float getDampingOrthoLin(long objectId);

    /**
     * Alter the joint's damping for off-axis translation.
     *
     * @param dampingOrthoLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingOrthoLin(float dampingOrthoLin) {
        setDampingOrthoLin(objectId, dampingOrthoLin);
    }

    private native void setDampingOrthoLin(long objectId, float value);

    /**
     * Read the joint's softness for off-axis rotation.
     *
     * @return the softness
     */
    public float getSoftnessOrthoAng() {
        return getSoftnessOrthoAng(objectId);
    }

    private native float getSoftnessOrthoAng(long objectId);

    /**
     * Alter the joint's softness for off-axis rotation.
     *
     * @param softnessOrthoAng the desired softness (default=1)
     */
    public void setSoftnessOrthoAng(float softnessOrthoAng) {
        setSoftnessOrthoAng(objectId, softnessOrthoAng);
    }

    private native void setSoftnessOrthoAng(long objectId, float value);

    /**
     * Read the joint's restitution for off-axis rotation.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionOrthoAng() {
        return getRestitutionOrthoAng(objectId);
    }

    private native float getRestitutionOrthoAng(long objectId);

    /**
     * Alter the joint's restitution for off-axis rotation.
     *
     * @param restitutionOrthoAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionOrthoAng(float restitutionOrthoAng) {
        setRestitutionOrthoAng(objectId, restitutionOrthoAng);
    }

    private native void setRestitutionOrthoAng(long objectId, float value);

    /**
     * Read the joint's damping for off-axis rotation.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingOrthoAng() {
        return getDampingOrthoAng(objectId);
    }

    private native float getDampingOrthoAng(long objectId);

    /**
     * Alter the joint's damping for off-axis rotation.
     *
     * @param dampingOrthoAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingOrthoAng(float dampingOrthoAng) {
        setDampingOrthoAng(objectId, dampingOrthoAng);
    }

    private native void setDampingOrthoAng(long objectId, float value);

    /**
     * Test whether the translation motor is powered.
     *
     * @return true if powered, otherwise false
     */
    public boolean isPoweredLinMotor() {
        return isPoweredLinMotor(objectId);
    }

    private native boolean isPoweredLinMotor(long objectId);

    /**
     * Alter whether the translation motor is powered.
     *
     * @param poweredLinMotor true to power the motor, false to de-power it
     * (default=false)
     */
    public void setPoweredLinMotor(boolean poweredLinMotor) {
        setPoweredLinMotor(objectId, poweredLinMotor);
    }

    private native void setPoweredLinMotor(long objectId, boolean value);

    /**
     * Read the velocity target of the translation motor.
     *
     * @return the velocity target
     */
    public float getTargetLinMotorVelocity() {
        return getTargetLinMotorVelocity(objectId);
    }

    private native float getTargetLinMotorVelocity(long objectId);

    /**
     * Alter the velocity target of the translation motor.
     *
     * @param targetLinMotorVelocity the desired velocity target (default=0)
     */
    public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
        setTargetLinMotorVelocity(objectId, targetLinMotorVelocity);
    }

    private native void setTargetLinMotorVelocity(long objectId, float value);

    /**
     * Read the maximum force of the translation motor.
     *
     * @return the maximum force
     */
    public float getMaxLinMotorForce() {
        return getMaxLinMotorForce(objectId);
    }

    private native float getMaxLinMotorForce(long objectId);

    /**
     * Alter the maximum force of the translation motor.
     *
     * @param maxLinMotorForce the desired maximum force (default=0)
     */
    public void setMaxLinMotorForce(float maxLinMotorForce) {
        setMaxLinMotorForce(objectId, maxLinMotorForce);
    }

    private native void setMaxLinMotorForce(long objectId, float value);

    /**
     * Test whether the rotation motor is powered.
     *
     * @return true if powered, otherwise false
     */
    public boolean isPoweredAngMotor() {
        return isPoweredAngMotor(objectId);
    }

    private native boolean isPoweredAngMotor(long objectId);

    /**
     * Alter whether the rotation motor is powered.
     *
     * @param poweredAngMotor true to power the motor, false to de-power it
     * (default=false)
     */
    public void setPoweredAngMotor(boolean poweredAngMotor) {
        setPoweredAngMotor(objectId, poweredAngMotor);
    }

    private native void setPoweredAngMotor(long objectId, boolean value);

    /**
     * Read the velocity target of the rotation motor.
     *
     * @return the velocity target (in radians per second)
     */
    public float getTargetAngMotorVelocity() {
        return getTargetAngMotorVelocity(objectId);
    }

    private native float getTargetAngMotorVelocity(long objectId);

    /**
     * Alter the velocity target of the rotation motor.
     *
     * @param targetAngMotorVelocity the desired velocity target (in radians per
     * second, default=0)
     */
    public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
        setTargetAngMotorVelocity(objectId, targetAngMotorVelocity);
    }

    private native void setTargetAngMotorVelocity(long objectId, float value);

    /**
     * Read the maximum force of the rotation motor.
     *
     * @return the maximum force
     */
    public float getMaxAngMotorForce() {
        return getMaxAngMotorForce(objectId);
    }

    private native float getMaxAngMotorForce(long objectId);

    /**
     * Alter the maximum force of the rotation motor.
     *
     * @param maxAngMotorForce the desired maximum force (default=0)
     */
    public void setMaxAngMotorForce(float maxAngMotorForce) {
        setMaxAngMotorForce(objectId, maxAngMotorForce);
    }

    private native void setMaxAngMotorForce(long objectId, float value);

    /**
     * Serialize this joint, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
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

    /**
     * De-serialize this joint, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
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

    /**
     * Instantiate the configured constraint in Bullet.
     */
    protected void createJoint() {
        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(), pivotA, rotA, pivotB, rotB, useLinearReferenceFrameA);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Joint {0}", Long.toHexString(objectId));
        // = new SliderConstraint(nodeA.getObjectId(), nodeB.getObjectId(), transA, transB, useLinearReferenceFrameA);
    }

    private native long createJoint(long objectIdA, long objectIdB, Vector3f pivotA, Matrix3f rotA, Vector3f pivotB, Matrix3f rotB, boolean useLinearReferenceFrameA);
}
