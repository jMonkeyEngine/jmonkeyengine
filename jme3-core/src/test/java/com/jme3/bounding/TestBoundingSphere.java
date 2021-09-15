/*
 * Copyright (c) 2021 jMonkeyEngine
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
package com.jme3.bounding;

import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the BoundingSphere class.
 *
 * @author Stephen Gold
 */
public class TestBoundingSphere {

    /**
     * Verify that an infinite bounding sphere can be merged with a very
     * eccentric bounding box without producing NaNs. This was issue #1459 at
     * GitHub.
     */
    @Test
    public void testIssue1459() {
        Vector3f boxCenter = new Vector3f(-92f, 3.3194322e29f, 674.89886f);
        BoundingBox boundingBox = new BoundingBox(boxCenter,
                1.0685959f, 3.3194322e29f, 2.705017f);

        Vector3f sphCenter = new Vector3f(0f, 0f, 0f);
        float radius = Float.POSITIVE_INFINITY;
        BoundingSphere boundingSphere = new BoundingSphere(radius, sphCenter);

        boundingSphere.mergeLocal(boundingBox);

        Vector3f copyCenter = new Vector3f();
        boundingSphere.getCenter(copyCenter);
        Assert.assertTrue(Vector3f.isValidVector(copyCenter));
    }
}
