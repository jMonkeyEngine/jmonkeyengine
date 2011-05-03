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

import com.jme3.math.Vector3f;

/**
 *
 * @author normenhansen
 */
public class TranslationalLimitMotor {

    private long motorId = 0;

    public TranslationalLimitMotor(long motor) {
        this.motorId = motor;
    }

    public long getMotor() {
        return motorId;
    }

    public Vector3f getLowerLimit() {
        Vector3f vec = new Vector3f();
        getLowerLimit(motorId, vec);
        return vec;
    }

    private native void getLowerLimit(long motorId, Vector3f vector);

    public void setLowerLimit(Vector3f lowerLimit) {
        setLowerLimit(motorId, lowerLimit);
    }

    private native void setLowerLimit(long motorId, Vector3f vector);
    
    public Vector3f getUpperLimit() {
        Vector3f vec = new Vector3f();
        getUpperLimit(motorId, vec);
        return vec;
    }

    private native void getUpperLimit(long motorId, Vector3f vector);

    public void setUpperLimit(Vector3f upperLimit) {
        setUpperLimit(motorId, upperLimit);
    }

    private native void setUpperLimit(long motorId, Vector3f vector);

    public Vector3f getAccumulatedImpulse() {
        Vector3f vec = new Vector3f();
        getAccumulatedImpulse(motorId, vec);
        return vec;
    }

    private native void getAccumulatedImpulse(long motorId, Vector3f vector);
    
    public void setAccumulatedImpulse(Vector3f accumulatedImpulse) {
        setAccumulatedImpulse(motorId, accumulatedImpulse);
    }

    private native void setAccumulatedImpulse(long motorId, Vector3f vector);

    public float getLimitSoftness() {
        return getLimitSoftness(motorId);
    }
    
    private native float getLimitSoftness(long motorId);

    public void setLimitSoftness(float limitSoftness) {
        setLimitSoftness(motorId, limitSoftness);
    }
    
    private native void setLimitSoftness(long motorId, float limitSoftness);

    public float getDamping() {
        return getDamping(motorId);
    }

    private native float getDamping(long motorId);
    
    public void setDamping(float damping) {
        setDamping(motorId, damping);
    }

    private native void setDamping(long motorId, float damping);
    
    public float getRestitution() {
        return getRestitution(motorId);
    }
    
    private native float getRestitution(long motorId);

    public void setRestitution(float restitution) {
        setRestitution(motorId, restitution);
    }

    private native void setRestitution(long motorId, float restitution);
}
