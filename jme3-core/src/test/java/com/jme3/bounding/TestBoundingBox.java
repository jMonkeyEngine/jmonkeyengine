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
package com.jme3.bounding;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the BoundingBox class.
 *
 * @author Stephen Gold
 */
public class TestBoundingBox {
    /**
     * Verify that equals() behaves as expected.
     */
    @Test
    public void testEquals() {
        BoundingBox bb1 = new BoundingBox(new Vector3f(3f, 4f, 5f), 0f, 1f, 2f);
        BoundingBox bb2
                = new BoundingBox(new Vector3f(3f, 4f, 5f), -0f, 1f, 2f);

        BoundingBox bb3 = new BoundingBox(new Vector3f(3f, 0f, 2f), 9f, 8f, 7f);
        BoundingBox bb4
                = new BoundingBox(new Vector3f(3f, -0f, 2f), 9f, 8f, 7f);

        BoundingBox bb5 = new BoundingBox(new Vector3f(4f, 5f, 6f), 9f, 8f, 7f);
        BoundingBox bb6 = (BoundingBox) bb5.clone();
        bb6.setCheckPlane(1);

        // Clones are equal to their base instances:
        Assertions.assertEquals(bb1, bb1.clone());
        Assertions.assertEquals(bb2, bb2.clone());
        Assertions.assertEquals(bb3, bb3.clone());
        Assertions.assertEquals(bb4, bb4.clone());
        Assertions.assertEquals(bb5, bb5.clone());
        Assertions.assertEquals(bb6, bb6.clone());

        Assertions.assertNotEquals(bb1, bb2); // because their extents differ
        Assertions.assertNotEquals(bb3, bb4); // because their centers differ
        Assertions.assertEquals(bb5, bb6); // because check planes are ignored
    }

    /**
     * Verify that merge() still modifies the original BoundingBox in place
     * (deprecated behavior preserved for backward compatibility).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testMergeModifiesReceiver() {
        BoundingBox bb1 = new BoundingBox(new Vector3f(0f, 0f, 0f), 1f, 1f, 1f);
        BoundingBox bb2 = new BoundingBox(new Vector3f(4f, 0f, 0f), 1f, 1f, 1f);

        BoundingBox bb1Before = (BoundingBox) bb1.clone();

        BoundingVolume result = bb1.merge(bb2);

        // merge() delegates to mergeLocal(), so bb1 IS modified.
        Assertions.assertNotEquals(bb1Before, bb1);

        // The result is the same object as bb1.
        Assertions.assertSame(bb1, result);
    }

    /**
     * Verify that mergeWith() does not modify the original bounding box, and
     * that the returned box contains both inputs.
     */
    @Test
    public void testMergeWithDoesNotModifyReceiver() {
        BoundingBox bb1 = new BoundingBox(new Vector3f(0f, 0f, 0f), 1f, 1f, 1f);
        BoundingBox bb2 = new BoundingBox(new Vector3f(4f, 0f, 0f), 1f, 1f, 1f);

        // Record the original state of bb1 before merging.
        BoundingBox bb1Before = (BoundingBox) bb1.clone();

        BoundingVolume result = bb1.mergeWith(bb2);

        // bb1 must be unmodified.
        Assertions.assertEquals(bb1Before, bb1);

        // The result must be a different object than bb1.
        Assertions.assertNotSame(bb1, result);

        // The result must contain both inputs' centers.
        Assertions.assertTrue(result instanceof BoundingBox);
        BoundingBox merged = (BoundingBox) result;
        Assertions.assertTrue(merged.contains(bb2.getCenter()));
        Assertions.assertTrue(merged.contains(bb1.getCenter()));
    }

    /**
     * Verify that isSimilar() behaves as expected.
     */
    @Test
    public void testIsSimilar() {
        BoundingBox bb1 = new BoundingBox(new Vector3f(3f, 4f, 5f), 0f, 1f, 2f);
        BoundingBox bb2
                = new BoundingBox(new Vector3f(3f, 4f, 5f), 0f, 1.1f, 2f);

        BoundingBox bb3 = new BoundingBox(new Vector3f(3f, 4f, 2f), 9f, 8f, 7f);
        BoundingBox bb4
                = new BoundingBox(new Vector3f(3f, 3.9f, 2f), 9f, 8f, 7f);

        BoundingBox bb5 = new BoundingBox(new Vector3f(4f, 5f, 6f), 9f, 8f, 7f);
        BoundingBox bb6 = (BoundingBox) bb5.clone();
        bb6.setCheckPlane(1);

        Assertions.assertFalse(bb1.isSimilar(bb2, 0.09999f));
        Assertions.assertTrue(bb1.isSimilar(bb2, 0.10001f));

        Assertions.assertFalse(bb3.isSimilar(bb4, 0.09999f));
        Assertions.assertTrue(bb3.isSimilar(bb4, 0.10001f));

        Assertions.assertTrue(bb5.isSimilar(bb6, 0f)); // check planes are ignored
    }
}
