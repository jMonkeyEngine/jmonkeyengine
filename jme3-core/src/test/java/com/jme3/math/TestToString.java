/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
 * Test various toString() methods using JUnit. See also
 * {@link com.jme3.math.TestTransform}.
 *
 * @author Stephen Gold
 */
public class TestToString {
    /**
     * Test various {@code toString()} methods against their javadoc.
     */
    @Test
    public void testToString() {
        // Test data that's never modified:
        Line line = new Line(
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));

        LineSegment segment = new LineSegment(
                new Vector3f(1f, 0f, 0f), new Vector3f(0f, 1f, 0f), 1f);

        Rectangle rectangle = new Rectangle(
                new Vector3f(1f, 0f, 0f),
                new Vector3f(2f, 0f, 0f),
                new Vector3f(1f, 2f, 0f));

        Triangle triangle = new Triangle(
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                new Vector3f(0f, 0f, 1f));

        // Verify that the methods don't throw an exception:
        String lineString = line.toString();
        String segmentString = segment.toString();
        String rectangleString = rectangle.toString();
        String triangleString = triangle.toString();

        // Verify that the results match the javadoc:
        Assert.assertEquals(
                "Line [Origin: (1.0, 0.0, 0.0)  Direction: (0.0, 1.0, 0.0)]",
                lineString);
        Assert.assertEquals(
                "LineSegment [Origin: (1.0, 0.0, 0.0)  Direction: (0.0, 1.0, 0.0)  Extent: 1.0]",
                segmentString);
        Assert.assertEquals(
                "Rectangle [A: (1.0, 0.0, 0.0)  B: (2.0, 0.0, 0.0)  C: (1.0, 2.0, 0.0)]",
                rectangleString);
        Assert.assertEquals(
                "Triangle [V1: (1.0, 0.0, 0.0)  V2: (0.0, 1.0, 0.0)  V3: (0.0, 0.0, 1.0)]",
                triangleString);
    }
}
