/*
 * Copyright (c) 2026 jMonkeyEngine
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT OWNER AND CONTRIBUTORS
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
package com.jme3.util;

import com.jme3.bounding.BoundingSphere;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests screen-area estimates that use a camera projection. */
public class AreaUtilsTest {

    private static final float EPSILON = 0.01f;

    @Test
    public void testPerspectiveProjectionScales() {
        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(60f, 4f / 3f, 1f, 100f);
        BoundingSphere bounds = new BoundingSphere(1f, new Vector3f(0f, 0f, -10f));

        float radiusX = camera.getProjectionMatrix().m00 * camera.getWidth() / 20f;
        float radiusY = camera.getProjectionMatrix().m11 * camera.getHeight() / 20f;
        float expectedArea = FastMath.PI * radiusX * radiusY;

        assertEquals(expectedArea,
                AreaUtils.calcScreenArea(bounds, 10f, camera), EPSILON);
    }

    @Test
    public void testPerspectiveAreaChangesWithFieldOfView() {
        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(60f, 4f / 3f, 1f, 100f);
        BoundingSphere bounds = new BoundingSphere(1f, new Vector3f(0f, 0f, -10f));
        float narrowFieldOfView = AreaUtils.calcScreenArea(bounds, 10f, camera);

        camera.setFrustumPerspective(90f, 4f / 3f, 1f, 100f);
        float wideFieldOfView = AreaUtils.calcScreenArea(bounds, 10f, camera);

        assertTrue(narrowFieldOfView > wideFieldOfView);
    }

    @Test
    public void testParallelProjectionDoesNotDependOnDistance() {
        Camera camera = new Camera(800, 600);
        camera.setParallelProjection(true);
        camera.setFrustum(1f, 100f, -4f, 4f, 3f, -3f);
        BoundingSphere bounds = new BoundingSphere(1f, Vector3f.ZERO);
        float radiusX = camera.getProjectionMatrix().m00 * camera.getWidth() / 2f;
        float radiusY = camera.getProjectionMatrix().m11 * camera.getHeight() / 2f;
        float expectedArea = FastMath.PI * radiusX * radiusY;

        assertEquals(expectedArea,
                AreaUtils.calcScreenArea(bounds, 10f, camera), EPSILON);
        assertEquals(expectedArea,
                AreaUtils.calcScreenArea(bounds, 100f, camera), EPSILON);
    }

    @Test
    public void testViewportDimensionsScaleProjectedArea() {
        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(60f, 4f / 3f, 1f, 100f);
        BoundingSphere bounds = new BoundingSphere(1f, new Vector3f(0f, 0f, -10f));
        float fullViewportArea = AreaUtils.calcScreenArea(bounds, 10f, camera);

        camera.setViewPort(0.25f, 0.75f, 0.25f, 0.75f);
        float halfWidthAndHeightArea = AreaUtils.calcScreenArea(bounds, 10f, camera);

        assertEquals(fullViewportArea * 0.25f, halfWidthAndHeightArea, EPSILON);
    }
}
