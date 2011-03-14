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
package com.jme3.bullet.collision;

import com.jme3.math.Vector3f;

/**
 * Contains the results of a PhysicsSpace rayTest
 * @author normenhansen
 */
public class PhysicsSweepTestResult {

    private PhysicsCollisionObject collisionObject;
    private Vector3f hitNormalLocal;
    private float hitFraction;
    private boolean normalInWorldSpace;

    public PhysicsSweepTestResult() {
    }

    public PhysicsSweepTestResult(PhysicsCollisionObject collisionObject, Vector3f hitNormalLocal, float hitFraction, boolean normalInWorldSpace) {
        this.collisionObject = collisionObject;
        this.hitNormalLocal = hitNormalLocal;
        this.hitFraction = hitFraction;
        this.normalInWorldSpace = normalInWorldSpace;
    }

    /**
     * @return the collisionObject
     */
    public PhysicsCollisionObject getCollisionObject() {
        return collisionObject;
    }

    /**
     * @return the hitNormalLocal
     */
    public Vector3f getHitNormalLocal() {
        return hitNormalLocal;
    }

    /**
     * @return the hitFraction
     */
    public float getHitFraction() {
        return hitFraction;
    }

    /**
     * @return the normalInWorldSpace
     */
    public boolean isNormalInWorldSpace() {
        return normalInWorldSpace;
    }

    public void fill(PhysicsCollisionObject collisionObject, Vector3f hitNormalLocal, float hitFraction, boolean normalInWorldSpace) {
        this.collisionObject = collisionObject;
        this.hitNormalLocal = hitNormalLocal;
        this.hitFraction = hitFraction;
        this.normalInWorldSpace = normalInWorldSpace;
    }
}
