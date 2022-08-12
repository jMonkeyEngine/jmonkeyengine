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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the Transform class using JUnit.
 *
 * @author Stephen Gold
 */
public class TestTransform {
    /**
     * Test the {@code toString()} method.
     */
    @Test
    public void testTransformToString() {
        // Test data that's never modified:
        final Vector3f t = new Vector3f(12f, -1f, 5f);
        final Quaternion r = new Quaternion(0f, 0.6f, -0.8f, 0f);
        final Vector3f s = new Vector3f(1.7f, 1f, 1.7f);
        final Transform test = new Transform(t, r, s);

        // Verify that the method doesn't throw an exception.
        String result = test.toString();
        /*
         * Verify that the result matches the javadoc
         * and can be parsed using a regular expression.
         */
        Pattern pattern = Pattern.compile(
                "^Transform\\[ (\\S+), (\\S+), (\\S+)\\]\\n"
                + "\\[ (\\S+), (\\S+), (\\S+), (\\S+)\\]\\n"
                + "\\[ (\\S+) , (\\S+), (\\S+)\\]$"
        );
        Matcher matcher = pattern.matcher(result);
        boolean valid = matcher.matches();
        Assert.assertTrue(valid);

        String txText = matcher.group(1);
        float tx = Float.parseFloat(txText);
        Assert.assertEquals(12f, tx, 1e-5f);

        String rzText = matcher.group(6);
        float rz = Float.parseFloat(rzText);
        Assert.assertEquals(-0.8f, rz, 1e-6f);

        String szText = matcher.group(10);
        float sz = Float.parseFloat(szText);
        Assert.assertEquals(1.7f, sz, 2e-6f);
    }
}
