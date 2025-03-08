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
package com.jme3.cinematic;

import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link MotionPath} class works.
 *
 * @author Stephen Gold
 */
public class MotionPathTest {

    /**
     * Verifies that MotionPath cloning works.
     */
    @Test
    public void cloneMotionPath() {
        MotionPath original = new MotionPath();
        original.setCycle(true);
        original.addWayPoint(new Vector3f(20, 3, 0));
        original.addWayPoint(new Vector3f(0, 3, 20));
        original.addWayPoint(new Vector3f(-20, 3, 0));
        original.addWayPoint(new Vector3f(0, 3, -20));
        original.setCurveTension(0.83f);

        MotionPath clone = Cloner.deepClone(original);

        // Verify that the clone is non-null and distinct from the original:
        Assert.assertNotNull(clone);
        Assert.assertTrue(clone != original);

        // Compare the return values of various getters:
        Assert.assertEquals(
                clone.getCurveTension(), original.getCurveTension(), 0f);
        Assert.assertEquals(clone.getLength(), original.getLength(), 0f);
        Assert.assertEquals(clone.getNbWayPoints(), original.getNbWayPoints());
        Assert.assertEquals(
                clone.getPathSplineType(), original.getPathSplineType());
        Assert.assertEquals(clone.getWayPoint(0), original.getWayPoint(0));
        Assert.assertEquals(clone.isCycle(), original.isCycle());
    }
}
