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
 * Verifies that the {@link Easing} functions work correctly.
 */
public class EasingTest {

    private static final float TOLERANCE = 1e-5f;

    // --- constant ---

    @Test
    public void testConstantAlwaysZero() {
        Assert.assertEquals(0f, Easing.constant.apply(0f), TOLERANCE);
        Assert.assertEquals(0f, Easing.constant.apply(0.5f), TOLERANCE);
        Assert.assertEquals(0f, Easing.constant.apply(1f), TOLERANCE);
    }

    // --- linear ---

    @Test
    public void testLinearEndpoints() {
        Assert.assertEquals(0f, Easing.linear.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.linear.apply(1f), TOLERANCE);
        Assert.assertEquals(0.5f, Easing.linear.apply(0.5f), TOLERANCE);
    }

    // --- inQuad ---

    @Test
    public void testInQuadEndpoints() {
        Assert.assertEquals(0f, Easing.inQuad.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.inQuad.apply(1f), TOLERANCE);
    }

    @Test
    public void testInQuadMidpoint() {
        // f(0.5) = 0.25
        Assert.assertEquals(0.25f, Easing.inQuad.apply(0.5f), TOLERANCE);
    }

    // --- inCubic ---

    @Test
    public void testInCubicEndpoints() {
        Assert.assertEquals(0f, Easing.inCubic.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.inCubic.apply(1f), TOLERANCE);
    }

    @Test
    public void testInCubicMidpoint() {
        Assert.assertEquals(0.125f, Easing.inCubic.apply(0.5f), TOLERANCE);
    }

    // --- outQuad ---

    @Test
    public void testOutQuadEndpoints() {
        Assert.assertEquals(0f, Easing.outQuad.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.outQuad.apply(1f), TOLERANCE);
    }

    @Test
    public void testOutQuadMidpoint() {
        // outQuad is inversion of inQuad: f(t) = 1 - (1-t)^2
        // f(0.5) = 1 - 0.25 = 0.75
        Assert.assertEquals(0.75f, Easing.outQuad.apply(0.5f), TOLERANCE);
    }

    // --- inOutQuad ---

    @Test
    public void testInOutQuadEndpoints() {
        Assert.assertEquals(0f, Easing.inOutQuad.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.inOutQuad.apply(1f), TOLERANCE);
    }

    @Test
    public void testInOutQuadMidpoint() {
        Assert.assertEquals(0.5f, Easing.inOutQuad.apply(0.5f), TOLERANCE);
    }

    // --- smoothStep ---

    @Test
    public void testSmoothStepEndpoints() {
        Assert.assertEquals(0f, Easing.smoothStep.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.smoothStep.apply(1f), TOLERANCE);
    }

    @Test
    public void testSmoothStepMidpoint() {
        Assert.assertEquals(0.5f, Easing.smoothStep.apply(0.5f), TOLERANCE);
    }

    // --- smootherStep ---

    @Test
    public void testSmootherStepEndpoints() {
        Assert.assertEquals(0f, Easing.smootherStep.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.smootherStep.apply(1f), TOLERANCE);
    }

    @Test
    public void testSmootherStepMidpoint() {
        Assert.assertEquals(0.5f, Easing.smootherStep.apply(0.5f), TOLERANCE);
    }

    // --- outBounce ---

    @Test
    public void testOutBounceAtZero() {
        Assert.assertEquals(0f, Easing.outBounce.apply(0f), TOLERANCE);
    }

    @Test
    public void testOutBounceAtOne() {
        Assert.assertEquals(1f, Easing.outBounce.apply(1f), TOLERANCE);
    }

    // --- inOutElastic ---

    @Test
    public void testInOutElasticEndpoints() {
        Assert.assertEquals(0f, Easing.inOutElastic.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.inOutElastic.apply(1f), TOLERANCE);
    }

    // --- inQuart / inQuint ---

    @Test
    public void testInQuartMidpoint() {
        Assert.assertEquals(0.0625f, Easing.inQuart.apply(0.5f), TOLERANCE);
    }

    @Test
    public void testInQuintMidpoint() {
        Assert.assertEquals(0.03125f, Easing.inQuint.apply(0.5f), TOLERANCE);
    }
}
