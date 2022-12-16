/*
 * Copyright (c) 2022 jMonkeyEngine
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link Triangle} class works correctly.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TriangleTest {
    /**
     * Basic functionality of a Triangle.
     */
    @Test
    public void test1() {
        Triangle triangle1 = new Triangle(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(6f, 5f, 4f),
                new Vector3f(5f, 8f, 2f)
        );

        // Verify that centroid and normal are calculated correctly.
        Vector3f c1 = triangle1.getCenter();
        Assert.assertEquals(4f, c1.x, 1e-6f);
        Assert.assertEquals(5f, c1.y, 1e-6f);
        Assert.assertEquals(3f, c1.z, 1e-6f);

        Vector3f n1 = triangle1.getNormal();
        Assert.assertEquals(-0.408248, n1.x, 1e-6f);
        Assert.assertEquals(0.408248, n1.y, 1e-6f);
        Assert.assertEquals(0.816497, n1.z, 1e-6f);

        // Clone triangle1 and verify its vertices.
        Triangle triangle2 = triangle1.clone();
        Assert.assertTrue(triangle1 != triangle2);
        Assert.assertEquals(triangle1.get1(), triangle2.get1());
        Assert.assertEquals(triangle1.get2(), triangle2.get2());
        Assert.assertEquals(triangle1.get3(), triangle2.get3());

        // Modify triangle1 and verify its new centroid.
        triangle1.set1(new Vector3f(-2f, -1f, 0f));
        c1 = triangle1.getCenter();
        Assert.assertEquals(3f, c1.x, 1e-6f);
        Assert.assertEquals(4f, c1.y, 1e-6f);
        Assert.assertEquals(2f, c1.z, 1e-6f);

        // Verify that triangle2's centroid and normal are (still) correct.
        Vector3f c2 = triangle2.getCenter();
        Assert.assertEquals(4f, c2.x, 1e-6f);
        Assert.assertEquals(5f, c2.y, 1e-6f);
        Assert.assertEquals(3f, c2.z, 1e-6f);

        Vector3f n2 = triangle2.getNormal();
        Assert.assertEquals(-0.408248, n2.x, 1e-6f);
        Assert.assertEquals(0.408248, n2.y, 1e-6f);
        Assert.assertEquals(0.816497, n2.z, 1e-6f);
    }
}
