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
package com.jme3.bullet.collision;

import com.jme3.math.Vector3f;

/**
 * Represent the results of a Bullet sweep test.
 *
 * @author normenhansen
 */
public class PhysicsSweepTestResult {

    /**
     * collision object that was hit
     */
    private PhysicsCollisionObject collisionObject;
    /**
     * normal vector at the point of contact
     */
    private Vector3f hitNormalLocal;
    /**
     * fraction of the way between the transforms (from=0, to=1, &ge;0, &le;1)
     */
    private float hitFraction;
    /**
     * true&rarr;need to transform normal into world space
     */
    private boolean normalInWorldSpace;

    /**
     * A private constructor to inhibit instantiation of this class by Java.
     * These results are instantiated exclusively by native code.
     */
    public PhysicsSweepTestResult() {
    }

    public PhysicsSweepTestResult(PhysicsCollisionObject collisionObject, Vector3f hitNormalLocal, float hitFraction, boolean normalInWorldSpace) {
        this.collisionObject = collisionObject;
        this.hitNormalLocal = hitNormalLocal;
        this.hitFraction = hitFraction;
        this.normalInWorldSpace = normalInWorldSpace;
    }

    /**
     * Access the collision object that was hit.
     *
     * @return the pre-existing instance
     */
    public PhysicsCollisionObject getCollisionObject() {
        return collisionObject;
    }

    /**
     * Access the normal vector at the point of contact.
     *
     * @return the pre-existing vector (not null)
     */
    public Vector3f getHitNormalLocal() {
        return hitNormalLocal;
    }

    /**
     * Read the fraction of fraction of the way between the transforms (from=0,
     * to=1, &ge;0, &le;1)
     *
     * @return fraction (from=0, to=1, &ge;0, &le;1)
     */
    public float getHitFraction() {
        return hitFraction;
    }

    /**
     * Test whether the normal is in world space.
     *
     * @return true if in world space, otherwise false
     */
    public boolean isNormalInWorldSpace() {
        return normalInWorldSpace;
    }

    /**
     * Fill in the fields of this result.
     * 
     * @param collisionObject
     * @param hitNormalLocal
     * @param hitFraction
     * @param normalInWorldSpace 
     */
    public void fill(PhysicsCollisionObject collisionObject, Vector3f hitNormalLocal, float hitFraction, boolean normalInWorldSpace) {
        this.collisionObject = collisionObject;
        this.hitNormalLocal = hitNormalLocal;
        this.hitFraction = hitFraction;
        this.normalInWorldSpace = normalInWorldSpace;
    }
}
