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

import junit.framework.TestCase;

/**
 * Verifies that the {@link Quaternion} class works correctly.
 *
 * @author Richard Tingle (aka Richtea)
 */
public class QuaternionTest extends TestCase{

    /**
     * Verify that the {@link Quaternion#isValidQuaternion(com.jme3.math.Quaternion)} method works correctly. Testing
     * for NaNs and infinities (which are not "valid")
     */
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
}