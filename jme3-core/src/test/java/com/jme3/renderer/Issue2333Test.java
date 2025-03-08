/*
 * Copyright (c) 2025 jMonkeyEngine
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
package com.jme3.renderer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Automated tests for "Camera Viewport Dimensions not Checked" (issue #2333 at
 * GitHub).
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Issue2333Test {

    /**
     * Tests some basic functionality of the viewport settings.
     */
    @Test
    public void testIssue2333() {
        Camera c = new Camera(1, 1);

        // Verify some Camera defaults:
        Assert.assertEquals(0f, c.getViewPortBottom(), 0f);
        Assert.assertEquals(0f, c.getViewPortLeft(), 0f);
        Assert.assertEquals(1f, c.getViewPortRight(), 0f);
        Assert.assertEquals(1f, c.getViewPortTop(), 0f);

        // Try some valid settings:
        new Camera(1, 1).setViewPort(0.5f, 0.7f, 0.1f, 0.3f);
        new Camera(1, 1).setViewPortBottom(0.9f);
        new Camera(1, 1).setViewPortLeft(0.99f);
        new Camera(1, 1).setViewPortRight(0.01f);
        new Camera(1, 1).setViewPortTop(0.1f);
    }

    /**
     * Verifies that setViewPort() with left = right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase01() {
        new Camera(1, 1).setViewPort(0.5f, 0.5f, 0f, 1f);
    }

    /**
     * Verifies that setViewPort() with left > right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase02() {
        new Camera(1, 1).setViewPort(0.7f, 0.5f, 0f, 1f);
    }

    /**
     * Verifies that setViewPortLeft() resulting in left = right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase03() {
        new Camera(1, 1).setViewPortLeft(1f);
    }

    /**
     * Verifies that setViewPortLeft() resulting in left > right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase04() {
        new Camera(1, 1).setViewPortLeft(1.1f);
    }

    /**
     * Verifies that setViewPortRight() resulting in left = right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase05() {
        new Camera(1, 1).setViewPortRight(0f);
    }

    /**
     * Verifies that setViewPortRight() resulting in left > right throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase06() {
        new Camera(1, 1).setViewPortRight(-0.1f);
    }

    /**
     * Verifies that setViewPort() with bottom = top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase07() {
        new Camera(1, 1).setViewPort(0f, 1f, 0.5f, 0.5f);
    }

    /**
     * Verifies that setViewPort() with bottom > top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase08() {
        new Camera(1, 1).setViewPort(0f, 1f, 0.7f, 0.6f);
    }

    /**
     * Verifies that setViewPortBottom() resulting in bottom = top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase09() {
        new Camera(1, 1).setViewPortBottom(1f);
    }

    /**
     * Verifies that setViewPortBottom() resulting in bottom > top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase10() {
        new Camera(1, 1).setViewPortBottom(2f);
    }

    /**
     * Verifies that setViewPortTop() resulting in bottom = top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase11() {
        new Camera(1, 1).setViewPortTop(0f);
    }

    /**
     * Verifies that setViewPortTop() resulting in bottom > top throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase12() {
        new Camera(1, 1).setViewPortTop(-1f);
    }

    /**
     * Verifies that setViewPort() with left = NaN throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase13() {
        new Camera(1, 1).setViewPort(Float.NaN, 1f, 0f, 1f);
    }

    /**
     * Verifies that setViewPort() with right = NaN throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase14() {
        new Camera(1, 1).setViewPort(0f, Float.NaN, 0f, 1f);
    }

    /**
     * Verifies that setViewPort() with bottom = NaN throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase15() {
        new Camera(1, 1).setViewPort(0f, 1f, Float.NaN, 1f);
    }

    /**
     * Verifies that setViewPort() with top = NaN throws an
     * IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase16() {
        new Camera(1, 1).setViewPort(0f, 1f, 0f, Float.NaN);
    }

    /**
     * Verifies that setViewPortBottom(NaN) throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase17() {
        new Camera(1, 1).setViewPortBottom(Float.NaN);
    }

    /**
     * Verifies that setViewPortLeft(NaN) throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase18() {
        new Camera(1, 1).setViewPortLeft(Float.NaN);
    }

    /**
     * Verifies that setViewPortRight(NaN) throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase19() {
        new Camera(1, 1).setViewPortRight(Float.NaN);
    }

    /**
     * Verifies that setViewPortTop(NaN) throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void iaeCase20() {
        new Camera(1, 1).setViewPortTop(Float.NaN);
    }
}
