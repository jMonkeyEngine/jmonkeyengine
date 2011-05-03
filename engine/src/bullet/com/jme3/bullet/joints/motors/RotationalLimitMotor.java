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
package com.jme3.bullet.joints.motors;

/**
 *
 * @author normenhansen
 */
public class RotationalLimitMotor {

    private long motorId = 0;

    public RotationalLimitMotor(long motor) {
        this.motorId = motor;
    }

    public long getMotor() {
        return motorId;
    }

    public float getLoLimit() {
        return getLoLimit(motorId);
    }

    private native float getLoLimit(long motorId);

    public void setLoLimit(float loLimit) {
        setLoLimit(motorId, loLimit);
    }

    private native void setLoLimit(long motorId, float loLimit);

    public float getHiLimit() {
        return getHiLimit(motorId);
    }

    private native float getHiLimit(long motorId);

    public void setHiLimit(float hiLimit) {
        setHiLimit(motorId, hiLimit);
    }

    private native void setHiLimit(long motorId, float hiLimit);

    public float getTargetVelocity() {
        return getTargetVelocity(motorId);
    }

    private native float getTargetVelocity(long motorId);

    public void setTargetVelocity(float targetVelocity) {
        setTargetVelocity(motorId, targetVelocity);
    }

    private native void setTargetVelocity(long motorId, float targetVelocity);

    public float getMaxMotorForce() {
        return getMaxMotorForce(motorId);
    }

    private native float getMaxMotorForce(long motorId);

    public void setMaxMotorForce(float maxMotorForce) {
        setMaxMotorForce(motorId, maxMotorForce);
    }

    private native void setMaxMotorForce(long motorId, float maxMotorForce);

    public float getMaxLimitForce() {
        return getMaxLimitForce(motorId);
    }

    private native float getMaxLimitForce(long motorId);

    public void setMaxLimitForce(float maxLimitForce) {
        setMaxLimitForce(motorId, maxLimitForce);
    }

    private native void setMaxLimitForce(long motorId, float maxLimitForce);

    public float getDamping() {
        return getDamping(motorId);
    }

    private native float getDamping(long motorId);

    public void setDamping(float damping) {
        setDamping(motorId, damping);
    }

    private native void setDamping(long motorId, float damping);

    public float getLimitSoftness() {
        return getLimitSoftness(motorId);
    }

    private native float getLimitSoftness(long motorId);

    public void setLimitSoftness(float limitSoftness) {
        setLimitSoftness(motorId, limitSoftness);
    }

    private native void setLimitSoftness(long motorId, float limitSoftness);

    public float getERP() {
        return getERP(motorId);
    }

    private native float getERP(long motorId);

    public void setERP(float ERP) {
        setERP(motorId, ERP);
    }

    private native void setERP(long motorId, float ERP);

    public float getBounce() {
        return getBounce(motorId);
    }

    private native float getBounce(long motorId);

    public void setBounce(float bounce) {
        setBounce(motorId, bounce);
    }

    private native void setBounce(long motorId, float limitSoftness);

    public boolean isEnableMotor() {
        return isEnableMotor(motorId);
    }

    private native boolean isEnableMotor(long motorId);

    public void setEnableMotor(boolean enableMotor) {
        setEnableMotor(motorId, enableMotor);
    }

    private native void setEnableMotor(long motorId, boolean enableMotor);
}
