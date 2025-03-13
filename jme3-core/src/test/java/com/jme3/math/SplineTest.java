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
package com.jme3.math;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.util.clone.Cloner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link Spline} class works correctly.
 *
 * @author Stephen Gold
 */
public class SplineTest {
    // *************************************************************************
    // fields

    private static final AssetManager assetManager = new DesktopAssetManager();
    // *************************************************************************
    // tests

    /**
     * Verifies that spline cloning works correctly.
     */
    @Test
    public void cloneSplines() {
        // Clone a Bézier spline:
        {
            Spline test1 = createBezier();
            Spline copy1 = Cloner.deepClone(test1);
            assertSplineEquals(test1, copy1);
        }

        // Clone a NURB spline:
        {
            Spline test2 = createNurb();
            Spline copy2 = Cloner.deepClone(test2);
            assertSplineEquals(test2, copy2);
        }

        // Clone a Catmull-Rom spline:
        {
            Spline test3 = createCatmullRom();
            Spline copy3 = Cloner.deepClone(test3);
            assertSplineEquals(test3, copy3);
        }

        // Clone a linear spline:
        {
            Spline test4 = createLinear();
            Spline copy4 = Cloner.deepClone(test4);
            assertSplineEquals(test4, copy4);
        }

        // Clone a default spline:
        {
            Spline test5 = new Spline();
            Spline copy5 = Cloner.deepClone(test5);
            assertSplineEquals(test5, copy5);
        }
    }

    /**
     * Verifies that spline serialization/deserialization works correctly.
     */
    @Test
    public void saveAndLoadSplines() {
        // Serialize and deserialize a Bezier spline:
        {
            Spline test1 = createBezier();
            Spline copy1 = BinaryExporter.saveAndLoad(assetManager, test1);
            assertSplineEquals(test1, copy1);
        }

        // Serialize and deserialize a NURB spline:
        {
            Spline test2 = createNurb();
            Spline copy2 = BinaryExporter.saveAndLoad(assetManager, test2);
            assertSplineEquals(test2, copy2);
        }

        // Serialize and deserialize a Catmull-Rom spline:
        {
            Spline test3 = createCatmullRom();
            Spline copy3 = BinaryExporter.saveAndLoad(assetManager, test3);
            assertSplineEquals(test3, copy3);
        }

        // Serialize and deserialize a linear spline:
        {
            Spline test4 = createLinear();
            Spline copy4 = BinaryExporter.saveAndLoad(assetManager, test4);
            assertSplineEquals(test4, copy4);
        }

        // Serialize and deserialize a default spline:
        {
            Spline test5 = new Spline();
            Spline copy5 = BinaryExporter.saveAndLoad(assetManager, test5);
            assertSplineEquals(test5, copy5);
        }
    }
    // *************************************************************************
    // private helper methods

    /**
     * Verifies that the specified lists are equivalent but distinct.
     *
     * @param s1 the first list to compare (may be null, unaffected)
     * @param s2 the 2nd list to compare (may be null, unaffected)
     */
    private static void assertListEquals(List<?> a1, List<?> a2) {
        if (a1 == null || a2 == null) {
            // If either list is null, verify that both are null:
            Assert.assertNull(a1);
            Assert.assertNull(a2);

        } else {
            // Verify that the lists are distinct and and of equal length:
            Assert.assertTrue(a1 != a2);
            Assert.assertEquals(a1.size(), a2.size());

            for (int i = 0; i < a1.size(); ++i) {
                Assert.assertEquals(a1.get(i), a2.get(i));
            }
        }
    }

    /**
     * Verify that the specified splines are equivalent.
     *
     * @param s1 the first spline to compare (not null, unaffected)
     * @param s2 the 2nd split to compare (not null, unaffected)
     */
    private static void assertSplineEquals(Spline s1, Spline s2) {
        Assert.assertEquals(s1.getType(), s2.getType());
        Assert.assertEquals(s1.isCycle(), s2.isCycle());

        Assert.assertEquals(
                s1.getBasisFunctionDegree(), s2.getBasisFunctionDegree());
        assertListEquals(s1.getControlPoints(), s2.getControlPoints());
        Assert.assertEquals(s1.getCurveTension(), s2.getCurveTension(), 0f);
        assertListEquals(s1.getKnots(), s2.getKnots());

        if (s1.getType() == Spline.SplineType.Nurb) {
            // These methods throw NPEs on non-NURB splines.
            Assert.assertEquals(s1.getMaxNurbKnot(), s2.getMaxNurbKnot(), 0f);
            Assert.assertEquals(s1.getMinNurbKnot(), s2.getMinNurbKnot(), 0f);
        }

        assertListEquals(s1.getSegmentsLength(), s2.getSegmentsLength());
        Assert.assertEquals(
                s1.getTotalLength(), s2.getTotalLength(), 0f);
        Assert.assertArrayEquals(s1.getWeights(), s2.getWeights(), 0f);
    }

    /**
     * Generates a simple cyclic Bézier spline for testing.
     *
     * @return a new Spline
     */
    private static Spline createBezier() {
        Vector3f[] controlPoints1 = {
            new Vector3f(0f, 1f, 0f), new Vector3f(1f, 2f, 1f),
            new Vector3f(1.5f, 1.5f, 1.5f), new Vector3f(2f, 0f, 1f)
        };

        Spline result = new Spline(
                Spline.SplineType.Bezier, controlPoints1, 0.1f, true);
        return result;
    }

    /**
     * Generates a simple acyclic Catmull-Rom spline for testing.
     *
     * @return a new Spline
     */
    private static Spline createCatmullRom() {
        List<Vector3f> controlPoints3 = new ArrayList<>(6);
        controlPoints3.add(new Vector3f(0f, 1f, 2f));
        controlPoints3.add(new Vector3f(3f, -1f, 4f));
        controlPoints3.add(new Vector3f(2f, 5f, 3f));
        controlPoints3.add(new Vector3f(3f, -2f, 3f));
        controlPoints3.add(new Vector3f(0.5f, 1f, 0.6f));
        controlPoints3.add(new Vector3f(-0.5f, 4f, 0.2f));

        Spline result = new Spline(
                Spline.SplineType.CatmullRom, controlPoints3, 0.01f, false);
        return result;
    }

    /**
     * Generates a simple cyclic linear spline for testing.
     *
     * @return a new Spline
     */
    private static Spline createLinear() {
        List<Vector3f> controlPoints4 = new ArrayList<>(3);
        controlPoints4.add(new Vector3f(3f, -1f, 4f));
        controlPoints4.add(new Vector3f(2f, 0f, 3f));
        controlPoints4.add(new Vector3f(3f, -2f, 3f));

        Spline result = new Spline(
                Spline.SplineType.Linear, controlPoints4, 0f, true);
        return result;
    }

    /**
     * Generates a simple NURB spline for testing.
     *
     * @return a new Spline
     */
    private static Spline createNurb() {
        List<Vector4f> controlPoints2 = new ArrayList<>(5);
        controlPoints2.add(new Vector4f(0f, 1f, 2f, 3f));
        controlPoints2.add(new Vector4f(3f, 1f, 4f, 0f));
        controlPoints2.add(new Vector4f(2f, 5f, 3f, 0f));
        controlPoints2.add(new Vector4f(3f, 2f, 3f, 1f));
        controlPoints2.add(new Vector4f(0.5f, 1f, 0.6f, 5f));

        List<Float> nurbKnots = new ArrayList<>(6);
        nurbKnots.add(0.2f);
        nurbKnots.add(0.3f);
        nurbKnots.add(0.4f);
        nurbKnots.add(0.43f);
        nurbKnots.add(0.51f);
        nurbKnots.add(0.52f);

        Spline result = new Spline(controlPoints2, nurbKnots);
        return result;
    }
}
