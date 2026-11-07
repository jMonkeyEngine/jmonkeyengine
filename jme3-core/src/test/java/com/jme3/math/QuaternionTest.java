/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;

/**
 * Verifies that the {@link Quaternion} class works correctly.
 *
 * @author Richard Tingle (aka Richtea)
 */
public class QuaternionTest {

    /**
     * Verify that the {@link Quaternion#isValidQuaternion(com.jme3.math.Quaternion)} method works correctly. Testing
     * for NaNs and infinities (which are not "valid")
     */
    @Test
    public void testIsValidQuaternion(){
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NaN, 2.1f, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, Float.NaN, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, Float.NaN, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, 3.0f, Float.NaN)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.POSITIVE_INFINITY, 1.5f, 1.9f, 2.0f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NEGATIVE_INFINITY, 2.5f, 8.2f, 3.0f)));
        assertFalse(Quaternion.isValidQuaternion(null));

        assertTrue(Quaternion.isValidQuaternion(new Quaternion()));
        assertTrue(Quaternion.isValidQuaternion(new Quaternion(1.5f, -5.7f, 8.2f, 3.0f)));
    }
    
    @Test
    public void testInverseWithSeparateStore() {
        // Non-unit quaternion to test non-trivial inverse calculation
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
        Quaternion originalCopy = new Quaternion(q);
        Quaternion store = new Quaternion();

        Quaternion result = q.inverse(store);

        // Verify returned object is the store (zero-allocation)
        Assertions.assertSame(store, result);

        // Verify source quaternion remains unaffected
        Assertions.assertEquals(originalCopy, q);

        // Verify store holds the correct inverse values (norm = 1^2+2^2+3^2+4^2 = 30)
        float invNorm = 1.0f / 30.0f;
        Assertions.assertEquals(-1f * invNorm, store.getX(), 1e-6f);
        Assertions.assertEquals(-2f * invNorm, store.getY(), 1e-6f);
        Assertions.assertEquals(-3f * invNorm, store.getZ(), 1e-6f);
        Assertions.assertEquals(4f * invNorm, store.getW(), 1e-6f);
    }

    @Test
    public void testInverseWithNullStore() {
        Quaternion q = new Quaternion(0.5f, 0.5f, 0.5f, 0.5f);
        Quaternion result = q.inverse(null);

        Assertions.assertNotNull(result);

        // Compare with baseline parameterless inverse()
        Assertions.assertEquals(q.inverse(), result);
    }

    @Test
    public void testInverseWithAliasingStore() {
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
        Quaternion expectedInverse = q.inverse(); // independent allocation

        // Pass 'q' as its own store (aliasing case)
        Quaternion result = q.inverse(q);

        Assertions.assertSame(q, result);
        Assertions.assertEquals(expectedInverse, q);
    }

    @Test
    public void testInverseZeroQuaternionReturnsNull() {
        Quaternion zeroQuat = new Quaternion(0f, 0f, 0f, 0f);
        Quaternion store = new Quaternion(1f, 2f, 3f, 4f);
        Quaternion originalStoreCopy = new Quaternion(store);

        Quaternion result = zeroQuat.inverse(store);

        // Zero quaternion inverse should yield null and leave the store untouched
        Assertions.assertNull(result);
        Assertions.assertEquals(originalStoreCopy, store);
    }

    @Test
    public void testParameterlessInverseBehavior() {
        Quaternion q = new Quaternion(2f, 1f, -1f, 3f);
        Quaternion result = q.inverse();

        Assertions.assertNotNull(result);

        // Verify cross-check with separate store version
        Quaternion storeCheck = new Quaternion();
        q.inverse(storeCheck);
        Assertions.assertEquals(storeCheck, result);
    }
}
