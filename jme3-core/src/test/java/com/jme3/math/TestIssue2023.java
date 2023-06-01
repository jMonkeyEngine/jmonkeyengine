/*
 * Copyright (c) 2023 jMonkeyEngine
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
 * Verify that getRotationColumn() returns correct values for non-normalized
 * Quaternions. This was issue #2023 at GitHub.
 *
 * @author Stephen Gold
 */
public class TestIssue2023 {

    /**
     * Test a couple non-normalized quaternions.
     */
    @Test
    public void testIssue2023() {
        Quaternion test1 = new Quaternion(2f, 0.5f, 1f, -0.3f);

        Vector3f col0 = test1.getRotationColumn(0);
        Assert.assertEquals(0.5318352f, col0.x, 1e-6f);
        Assert.assertEquals(0.26217228f, col0.y, 1e-6f);
        Assert.assertEquals(0.80524343f, col0.z, 1e-6f);

        Vector3f col1 = test1.getRotationColumn(1);
        Assert.assertEquals(0.4868914f, col1.x, 1e-6f);
        Assert.assertEquals(-0.8726592f, col1.y, 1e-6f);
        Assert.assertEquals(-0.03745319f, col1.z, 1e-6f);

        Vector3f col2 = test1.getRotationColumn(2);
        Assert.assertEquals(0.6928839f, col2.x, 1e-6f);
        Assert.assertEquals(0.41198504f, col2.y, 1e-6f);
        Assert.assertEquals(-0.5917603f, col2.z, 1e-6f);

        Quaternion test2 = new Quaternion(0f, -0.2f, 0f, 0.6f);

        col0 = test2.getRotationColumn(0);
        Assert.assertEquals(0.8f, col0.x, 1e-6f);
        Assert.assertEquals(0f, col0.y, 1e-6f);
        Assert.assertEquals(0.6f, col0.z, 1e-6f);

        col1 = test2.getRotationColumn(1);
        Assert.assertEquals(0f, col1.x, 1e-6f);
        Assert.assertEquals(1f, col1.y, 1e-6f);
        Assert.assertEquals(0f, col1.z, 1e-6f);

        col2 = test2.getRotationColumn(2);
        Assert.assertEquals(-0.6f, col2.x, 1e-6f);
        Assert.assertEquals(0f, col2.y, 1e-6f);
        Assert.assertEquals(0.8f, col2.z, 1e-6f);
    }
}
