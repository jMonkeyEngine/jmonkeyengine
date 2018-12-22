/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.bullet.joints.motors;

/**
 * A motor based on Bullet's btRotationalLimitMotor. Motors are used to drive
 * joints.
 *
 * @author normenhansen
 */
public class RotationalLimitMotor {

    /**
     * Unique identifier of the btRotationalLimitMotor. The constructor sets
     * this to a non-zero value.
     */
    private long motorId = 0;

    /**
     * Instantiate a motor for the identified btRotationalLimitMotor.
     *
     * @param motor the unique identifier (not zero)
     */
    public RotationalLimitMotor(long motor) {
        this.motorId = motor;
    }

    /**
     * Read the id of the btRotationalLimitMotor.
     *
     * @return the identifier of the btRotationalLimitMotor (not zero)
     */
    public long getMotor() {
        return motorId;
    }

    /**
     * Read this motor's constraint lower limit.
     *
     * @return the limit value
     */
    public float getLoLimit() {
        return getLoLimit(motorId);
    }

    private native float getLoLimit(long motorId);

    /**
     * Alter this motor's constraint lower limit.
     *
     * @param loLimit the desired limit value
     */
    public void setLoLimit(float loLimit) {
        setLoLimit(motorId, loLimit);
    }

    private native void setLoLimit(long motorId, float loLimit);

    /**
     * Read this motor's constraint upper limit.
     *
     * @return the limit value
     */
    public float getHiLimit() {
        return getHiLimit(motorId);
    }

    private native float getHiLimit(long motorId);

    /**
     * Alter this motor's constraint upper limit.
     *
     * @param hiLimit the desired limit value
     */
    public void setHiLimit(float hiLimit) {
        setHiLimit(motorId, hiLimit);
    }

    private native void setHiLimit(long motorId, float hiLimit);

    /**
     * Read this motor's target velocity.
     *
     * @return the target velocity (in radians per second)
     */
    public float getTargetVelocity() {
        return getTargetVelocity(motorId);
    }

    private native float getTargetVelocity(long motorId);

    /**
     * Alter this motor's target velocity.
     *
     * @param targetVelocity the desired target velocity (in radians per second)
     */
    public void setTargetVelocity(float targetVelocity) {
        setTargetVelocity(motorId, targetVelocity);
    }

    private native void setTargetVelocity(long motorId, float targetVelocity);

    /**
     * Read this motor's maximum force.
     *
     * @return the maximum force
     */
    public float getMaxMotorForce() {
        return getMaxMotorForce(motorId);
    }

    private native float getMaxMotorForce(long motorId);

    /**
     * Alter this motor's maximum force.
     *
     * @param maxMotorForce the desired maximum force on the motor
     */
    public void setMaxMotorForce(float maxMotorForce) {
        setMaxMotorForce(motorId, maxMotorForce);
    }

    private native void setMaxMotorForce(long motorId, float maxMotorForce);

    /**
     * Read the limit's maximum force.
     *
     * @return the maximum force on the limit
     */
    public float getMaxLimitForce() {
        return getMaxLimitForce(motorId);
    }

    private native float getMaxLimitForce(long motorId);

    /**
     * Alter the limit's maximum force.
     *
     * @param maxLimitForce the desired maximum force on the limit
     */
    public void setMaxLimitForce(float maxLimitForce) {
        setMaxLimitForce(motorId, maxLimitForce);
    }

    private native void setMaxLimitForce(long motorId, float maxLimitForce);

    /**
     * Read this motor's damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDamping() {
        return getDamping(motorId);
    }

    private native float getDamping(long motorId);

    /**
     * Alter this motor's damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=1)
     */
    public void setDamping(float damping) {
        setDamping(motorId, damping);
    }

    private native void setDamping(long motorId, float damping);

    /**
     * Read this motor's limit softness.
     *
     * @return the limit softness
     */
    public float getLimitSoftness() {
        return getLimitSoftness(motorId);
    }

    private native float getLimitSoftness(long motorId);

    /**
     * Alter this motor's limit softness.
     *
     * @param limitSoftness the desired limit softness
     */
    public void setLimitSoftness(float limitSoftness) {
        setLimitSoftness(motorId, limitSoftness);
    }

    private native void setLimitSoftness(long motorId, float limitSoftness);

    /**
     * Read this motor's error tolerance at limits.
     *
     * @return the error tolerance (&gt;0)
     */
    public float getERP() {
        return getERP(motorId);
    }

    private native float getERP(long motorId);

    /**
     * Alter this motor's error tolerance at limits.
     *
     * @param ERP the desired error tolerance (&gt;0)
     */
    public void setERP(float ERP) {
        setERP(motorId, ERP);
    }

    private native void setERP(long motorId, float ERP);

    /**
     * Read this motor's bounce.
     *
     * @return the bounce (restitution factor)
     */
    public float getBounce() {
        return getBounce(motorId);
    }

    private native float getBounce(long motorId);

    /**
     * Alter this motor's bounce.
     *
     * @param bounce the desired bounce (restitution factor)
     */
    public void setBounce(float bounce) {
        setBounce(motorId, bounce);
    }

    private native void setBounce(long motorId, float limitSoftness);

    /**
     * Test whether this motor is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnableMotor() {
        return isEnableMotor(motorId);
    }

    private native boolean isEnableMotor(long motorId);

    /**
     * Enable or disable this motor.
     *
     * @param enableMotor true&rarr;enable, false&rarr;disable
     */
    public void setEnableMotor(boolean enableMotor) {
        setEnableMotor(motorId, enableMotor);
    }

    private native void setEnableMotor(long motorId, boolean enableMotor);
}
