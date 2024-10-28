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
     * Verify that equals() behaves as expected.
     */
    @Test
    public void testEquals() {
        BoundingSphere bs1 = new BoundingSphere(0f, new Vector3f(3f, 4f, 5f));
        BoundingSphere bs2 = new BoundingSphere(-0f, new Vector3f(3f, 4f, 5f));

        BoundingSphere bs3 = new BoundingSphere(1f, new Vector3f(3f, 0f, 2f));
        BoundingSphere bs4 = new BoundingSphere(1f, new Vector3f(3f, -0f, 2f));

        BoundingSphere bs5 = new BoundingSphere(2f, new Vector3f(4f, 5f, 6f));
        BoundingSphere bs6 = (BoundingSphere) bs5.clone();
        bs6.setCheckPlane(1);

        // Clones are equal to their base instances:
        Assert.assertEquals(bs1, bs1.clone());
        Assert.assertEquals(bs2, bs2.clone());
        Assert.assertEquals(bs3, bs3.clone());
        Assert.assertEquals(bs4, bs4.clone());
        Assert.assertEquals(bs5, bs5.clone());
        Assert.assertEquals(bs6, bs6.clone());

        Assert.assertNotEquals(bs1, bs2); // because their radii differ
        Assert.assertNotEquals(bs3, bs4); // because their centers differ
        Assert.assertEquals(bs5, bs6); // because check planes are ignored
    }

    /**
     * Verify that isSimilar() behaves as expected.
     */
    @Test
    public void testIsSimilar() {
        BoundingSphere bs1 = new BoundingSphere(0f, new Vector3f(3f, 4f, 5f));
        BoundingSphere bs2 = new BoundingSphere(0.1f, new Vector3f(3f, 4f, 5f));

        BoundingSphere bs3 = new BoundingSphere(1f, new Vector3f(3f, 4f, 2f));
        BoundingSphere bs4 = new BoundingSphere(1f, new Vector3f(3f, 3.9f, 2f));

        BoundingSphere bs5 = new BoundingSphere(2f, new Vector3f(4f, 5f, 6f));
        BoundingSphere bs6 = (BoundingSphere) bs5.clone();
        bs6.setCheckPlane(1);

        Assert.assertFalse(bs1.isSimilar(bs2, 0.09999f));
        Assert.assertTrue(bs1.isSimilar(bs2, 0.10001f));

        Assert.assertFalse(bs3.isSimilar(bs4, 0.09999f));
        Assert.assertTrue(bs3.isSimilar(bs4, 0.10001f));

        Assert.assertTrue(bs5.isSimilar(bs6, 0f)); // check planes are ignored
    }

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
