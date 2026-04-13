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

    // -----------------------------------------------------------------------
    // Complex behavioral tests
    // -----------------------------------------------------------------------

    /** linear must be monotonically non-decreasing on [0, 1]. */
    @Test
    public void testMonotonicity_linear() {
        float prev = Easing.linear.apply(0f);
        for (int i = 1; i <= 10; i++) {
            float t = i / 10f;
            float cur = Easing.linear.apply(t);
            Assert.assertTrue("linear must be non-decreasing at t=" + t, cur >= prev - TOLERANCE);
            prev = cur;
        }
    }

    /** inQuad must be monotonically increasing on [0, 1]. */
    @Test
    public void testMonotonicity_inQuad() {
        float prev = Easing.inQuad.apply(0f);
        for (int i = 1; i <= 10; i++) {
            float t = i / 10f;
            float cur = Easing.inQuad.apply(t);
            Assert.assertTrue("inQuad must be non-decreasing at t=" + t, cur >= prev - TOLERANCE);
            prev = cur;
        }
    }

    /** outBounce must start at 0 and end at 1. */
    @Test
    public void testMonotonicity_outBounce() {
        // outBounce starts at 0 and ends at 1 (it "bounces" up, hence not strictly monotone)
        Assert.assertEquals(0f, Easing.outBounce.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, Easing.outBounce.apply(1f), TOLERANCE);
        // Values must stay within [0, 1]
        for (int i = 0; i <= 20; i++) {
            float t = i / 20f;
            float val = Easing.outBounce.apply(t);
            Assert.assertTrue("outBounce value at t=" + t + " must be >= 0", val >= -TOLERANCE);
            Assert.assertTrue("outBounce value at t=" + t + " must be <= 1", val <= 1f + TOLERANCE);
        }
    }

    /**
     * inOutQuad must be symmetric around (0.5, 0.5): f(0.5) == 0.5 and
     * f(1−t) == 1 − f(t) for all t.
     */
    @Test
    public void testInOutSymmetry_inOutQuad() {
        Assert.assertEquals(0.5f, Easing.inOutQuad.apply(0.5f), TOLERANCE);
        float[] testPoints = {0.1f, 0.25f, 0.4f, 0.6f, 0.75f, 0.9f};
        for (float t : testPoints) {
            float ft  = Easing.inOutQuad.apply(t);
            float f1t = Easing.inOutQuad.apply(1f - t);
            Assert.assertEquals("inOutQuad symmetry at t=" + t, 1f - ft, f1t, TOLERANCE);
        }
    }

    /** outQuad must progress faster early than inQuad (out is fast then slow). */
    @Test
    public void testOutIsFastThenSlow_outQuad() {
        float tEarly = 0.25f;
        Assert.assertTrue("outQuad(0.25) must be > inQuad(0.25)",
                Easing.outQuad.apply(tEarly) > Easing.inQuad.apply(tEarly));
    }

    /**
     * smoothStep has zero derivative at the endpoints: f(ε) must be very
     * close to 0, confirming the flat start.
     */
    @Test
    public void testSmoothStepDerivativeAtEndsIsZero() {
        float eps = 1e-3f;
        // At t=0 the derivative is 0, so f(eps)/eps ≈ 0
        float slope0 = Easing.smoothStep.apply(eps) / eps;
        Assert.assertEquals(0f, slope0, 1e-2f);
        // At t=1 the derivative is 0, so (f(1) - f(1-eps))/eps ≈ 0
        float slope1 = (Easing.smoothStep.apply(1f) - Easing.smoothStep.apply(1f - eps)) / eps;
        Assert.assertEquals(0f, slope1, 1e-2f);
    }

    /**
     * A custom InOut built from inQuad and outCubic must satisfy:
     * apply(0) == 0, apply(1) == 1, and apply(0.5) is the boundary value
     * (inQuad(1)/2 == 0.5).
     */
    @Test
    public void testCustomInOut() {
        EaseFunction custom = new Easing.InOut(Easing.inQuad, Easing.outCubic);
        Assert.assertEquals(0f, custom.apply(0f), TOLERANCE);
        Assert.assertEquals(1f, custom.apply(1f), TOLERANCE);
        // At t=0.5: InOut evaluates in-half: in.apply(1.0)/2 = inQuad(1)/2 = 0.5
        Assert.assertEquals(0.5f, custom.apply(0.5f), TOLERANCE);
    }
}
