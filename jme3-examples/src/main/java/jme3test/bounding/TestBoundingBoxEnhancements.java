/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the new {@link BoundingBox} utility methods:
 * <ul>
 *   <li>{@link BoundingBox#getCorners()}</li>
 *   <li>{@link BoundingBox#getCorners(Vector3f[])}</li>
 *   <li>{@link BoundingBox#expand(float)}</li>
 *   <li>{@link BoundingBox#expand(Vector3f)}</li>
 * </ul>
 *
 * @author [Your Name]
 */
public class TestBoundingBoxEnhancements {

    private static final float DELTA = 1e-6f;

    // =========================================================================
    // getCorners() tests
    // =========================================================================

    /**
     * A box centred at the origin with extents (1,1,1) should produce the
     * 8 unit-cube corners at ±1 on each axis.
     */
    @Test
    public void testGetCornersUnitBox() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        Vector3f[] corners = box.getCorners();

        assertNotNull("getCorners() must not return null", corners);
        assertEquals("getCorners() must return exactly 8 elements", 8, corners.length);

        for (Vector3f c : corners) {
            assertNotNull("No corner element may be null", c);
        }

        // Verify that all 8 combinations of ±1 are present
        boolean[] found = new boolean[8];
        for (Vector3f c : corners) {
            assertTrue("|x| must equal 1", Math.abs(Math.abs(c.x) - 1f) < DELTA);
            assertTrue("|y| must equal 1", Math.abs(Math.abs(c.y) - 1f) < DELTA);
            assertTrue("|z| must equal 1", Math.abs(Math.abs(c.z) - 1f) < DELTA);
            int idx = (c.x > 0 ? 1 : 0) | (c.y > 0 ? 2 : 0) | (c.z > 0 ? 4 : 0);
            assertFalse("Each unique corner must appear exactly once (duplicate at idx " + idx + ")", found[idx]);
            found[idx] = true;
        }
        for (int i = 0; i < 8; i++) {
            assertTrue("Corner combination " + i + " was not produced", found[i]);
        }
    }

    /**
     * The returned corners must reflect a non-origin centre correctly.
     */
    @Test
    public void testGetCornersOffsetCenter() {
        Vector3f center = new Vector3f(5f, -3f, 2f);
        BoundingBox box = new BoundingBox(center, 2f, 1f, 3f);
        Vector3f[] corners = box.getCorners();

        // Expected min/max
        float minX = 3f, maxX = 7f;
        float minY = -4f, maxY = -2f;
        float minZ = -1f, maxZ = 5f;

        for (Vector3f c : corners) {
            assertTrue("x must be minX or maxX",
                    Math.abs(c.x - minX) < DELTA || Math.abs(c.x - maxX) < DELTA);
            assertTrue("y must be minY or maxY",
                    Math.abs(c.y - minY) < DELTA || Math.abs(c.y - maxY) < DELTA);
            assertTrue("z must be minZ or maxZ",
                    Math.abs(c.z - minZ) < DELTA || Math.abs(c.z - maxZ) < DELTA);
        }
    }

    /**
     * Passing an existing 8-element array must overwrite it in-place (no new
     * allocation for the outer array).
     */
    @Test
    public void testGetCornersReuseStoreArray() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);

        Vector3f[] store = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            store[i] = new Vector3f(999f, 999f, 999f);
        }

        Vector3f[] result = box.getCorners(store);
        assertSame("getCorners(store) must return the same array instance", store, result);

        // All elements should have been overwritten from 999 -> valid corners
        for (Vector3f c : result) {
            assertTrue("Corner x must be ±1, not 999", Math.abs(Math.abs(c.x) - 1f) < DELTA);
        }
    }

    /**
     * Passing null should produce a freshly allocated 8-element array.
     */
    @Test
    public void testGetCornersNullStore() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        Vector3f[] result = box.getCorners(null);
        assertNotNull(result);
        assertEquals(8, result.length);
    }

    /**
     * A box whose extents are all zero (a point) should produce 8 corners
     * that all equal the centre.
     */
    @Test
    public void testGetCornersPointBox() {
        Vector3f pt = new Vector3f(1f, 2f, 3f);
        BoundingBox box = new BoundingBox(pt, 0f, 0f, 0f);
        Vector3f[] corners = box.getCorners();

        for (Vector3f c : corners) {
            assertEquals("Point-box corner x", pt.x, c.x, DELTA);
            assertEquals("Point-box corner y", pt.y, c.y, DELTA);
            assertEquals("Point-box corner z", pt.z, c.z, DELTA);
        }
    }

    /**
     * Verify the documented corner ordering (indices 0-7).
     */
    @Test
    public void testGetCornersOrdering() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        Vector3f[] c = box.getCorners();
        // index: 0=(-,-,-), 1=(+,-,-), 2=(-,+,-), 3=(+,+,-),
        //        4=(-,-,+), 5=(+,-,+), 6=(-,+,+), 7=(+,+,+)
        assertCorner(c[0], -1, -1, -1);
        assertCorner(c[1],  1, -1, -1);
        assertCorner(c[2], -1,  1, -1);
        assertCorner(c[3],  1,  1, -1);
        assertCorner(c[4], -1, -1,  1);
        assertCorner(c[5],  1, -1,  1);
        assertCorner(c[6], -1,  1,  1);
        assertCorner(c[7],  1,  1,  1);
    }

    // =========================================================================
    // expand(float) tests
    // =========================================================================

    /**
     * Expanding uniformly increases all three extents.
     */
    @Test
    public void testExpandUniformGrow() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 2f, 3f, 4f);
        BoundingBox returned = box.expand(1f);

        assertSame("expand() must return 'this' for chaining", box, returned);
        assertEquals(3f, box.getXExtent(), DELTA);
        assertEquals(4f, box.getYExtent(), DELTA);
        assertEquals(5f, box.getZExtent(), DELTA);
    }

    /**
     * A negative expand value shrinks the box.
     */
    @Test
    public void testExpandUniformShrink() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 2f, 3f, 4f);
        box.expand(-1f);

        assertEquals(1f, box.getXExtent(), DELTA);
        assertEquals(2f, box.getYExtent(), DELTA);
        assertEquals(3f, box.getZExtent(), DELTA);
    }

    /**
     * Shrinking beyond zero must clamp to zero, not go negative.
     */
    @Test
    public void testExpandClampToZero() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        box.expand(-100f);

        assertEquals("xExtent must clamp at 0", 0f, box.getXExtent(), DELTA);
        assertEquals("yExtent must clamp at 0", 0f, box.getYExtent(), DELTA);
        assertEquals("zExtent must clamp at 0", 0f, box.getZExtent(), DELTA);
    }

    /**
     * The center must be unchanged after expand.
     */
    @Test
    public void testExpandDoesNotMoveCenter() {
        Vector3f center = new Vector3f(5f, 5f, 5f);
        BoundingBox box = new BoundingBox(center.clone(), 1f, 1f, 1f);
        box.expand(10f);

        assertEquals("center.x must not change", 5f, box.getCenter().x, DELTA);
        assertEquals("center.y must not change", 5f, box.getCenter().y, DELTA);
        assertEquals("center.z must not change", 5f, box.getCenter().z, DELTA);
    }

    // =========================================================================
    // expand(Vector3f) tests
    // =========================================================================

    /**
     * Per-axis expand applies independently.
     */
    @Test
    public void testExpandPerAxisGrow() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 2f, 3f);
        box.expand(new Vector3f(1f, 2f, 3f));

        assertEquals(2f, box.getXExtent(), DELTA);
        assertEquals(4f, box.getYExtent(), DELTA);
        assertEquals(6f, box.getZExtent(), DELTA);
    }

    /**
     * Mixed grow/shrink per axis.
     */
    @Test
    public void testExpandPerAxisMixed() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 5f, 5f, 5f);
        box.expand(new Vector3f(2f, -2f, 0f));

        assertEquals(7f, box.getXExtent(), DELTA);
        assertEquals(3f, box.getYExtent(), DELTA);
        assertEquals(5f, box.getZExtent(), DELTA);
    }

    /**
     * Per-axis clamp to zero.
     */
    @Test
    public void testExpandPerAxisClampToZero() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 2f, 3f);
        box.expand(new Vector3f(-100f, -100f, -100f));

        assertEquals(0f, box.getXExtent(), DELTA);
        assertEquals(0f, box.getYExtent(), DELTA);
        assertEquals(0f, box.getZExtent(), DELTA);
    }

    /**
     * Verify expand(Vector3f) returns {@code this} for fluent-chaining.
     */
    @Test
    public void testExpandVectorChaining() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        assertSame(box, box.expand(new Vector3f(1f, 1f, 1f)));
    }

    // =========================================================================
    // Interaction: getCorners() after expand()
    // =========================================================================

    /**
     * After expanding, getCorners() should reflect the new extents.
     */
    @Test
    public void testGetCornersAfterExpand() {
        BoundingBox box = new BoundingBox(Vector3f.ZERO.clone(), 1f, 1f, 1f);
        box.expand(1f); // extents now 2,2,2

        Vector3f[] corners = box.getCorners();
        for (Vector3f c : corners) {
            assertTrue("After expand, |x| must equal 2", Math.abs(Math.abs(c.x) - 2f) < DELTA);
            assertTrue("After expand, |y| must equal 2", Math.abs(Math.abs(c.y) - 2f) < DELTA);
            assertTrue("After expand, |z| must equal 2", Math.abs(Math.abs(c.z) - 2f) < DELTA);
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static void assertCorner(Vector3f actual, float ex, float ey, float ez) {
        assertEquals("corner x", ex, actual.x, DELTA);
        assertEquals("corner y", ey, actual.y, DELTA);
        assertEquals("corner z", ez, actual.z, DELTA);
    }
}
