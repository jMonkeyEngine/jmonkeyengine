/*
 * Copyright (c) 2018 jMonkeyEngine
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

import org.junit.Test;

/**
 * Verify that a Triangle center and normal get recomputed after a change. This
 * was issue #957 at GitHub.
 *
 * @author Stephen Gold
 */
public class TestIssue957 {

    final private Vector3f v0 = new Vector3f(0f, 0f, 0f);
    final private Vector3f v1 = new Vector3f(3f, 0f, 0f);
    final private Vector3f v2 = new Vector3f(0f, 3f, 0f);

    @Test
    public void testIssue957() {
        Vector3f v3 = new Vector3f(0f, 0f, 3f);

        Triangle t1 = makeTriangle();
        t1.set(2, v3);
        checkTriangle(t1);

        Triangle t2 = makeTriangle();
        t2.set(v3, v0, v1);
        checkTriangle(t2);

        Triangle t3 = makeTriangle();
        t3.set(2, v3.x, v3.y, v3.z);
        checkTriangle(t3);

        Triangle t4 = makeTriangle();
        t4.set3(v3);
        checkTriangle(t4);
    }

    private Triangle makeTriangle() {

        Triangle triangle = new Triangle(v0, v1, v2);
        /*
         * Check center and normal before modification.
        */
        Vector3f center = triangle.getCenter();
        Vector3f normal = triangle.getNormal();
        assert center.equals(new Vector3f(1f, 1f, 0f));
        assert normal.equals(new Vector3f(0f, 0f, 1f));

        return triangle;
    }

    /**
     * Check center and normal after modification.
    */
    private void checkTriangle(Triangle triangle) {
        Vector3f center = triangle.getCenter();
        Vector3f normal = triangle.getNormal();
        assert center.equals(new Vector3f(1f, 0f, 1f));
        assert normal.equals(new Vector3f(0f, -1f, 0f));
    }
}
