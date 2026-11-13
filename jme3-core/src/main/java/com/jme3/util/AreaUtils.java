/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.util;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;

/**
 * <code>AreaUtils</code> is used to calculate the area of various objects, such as bounding volumes.  These
 * functions are very loose approximations.
 *
 * @author Joshua Slack
 * @version $Id: AreaUtils.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
public final class AreaUtils {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AreaUtils() {
    }
    
    /**
     * Estimate the screen area of a bounding volume. If the volume isn't a
     * BoundingSphere, BoundingBox, or OrientedBoundingBox, 0 is returned.
     * This overload assumes a unit projection scale and does not account for
     * the camera's field of view. Use the camera overload when a camera is
     * available.
    *
    * @param bound The bounds to calculate the volume from.
    * @param distance The distance from camera to object.
    * @param screenWidth The width of the screen.
    * @return The area in pixels on the screen of the bounding volume.
    */
    public static float calcScreenArea(BoundingVolume bound, float distance, float screenWidth) {
        if (bound.getType() == BoundingVolume.Type.Sphere) {
            return calcScreenArea((BoundingSphere) bound, distance, screenWidth);
        } else if (bound.getType() == BoundingVolume.Type.AABB) {
            return calcScreenArea((BoundingBox) bound, distance, screenWidth);
        }
        return 0.0f;
    }

    /**
     * Estimate the projected screen area of a bounding volume using the
     * camera's projection and viewport dimensions. The volume is approximated
     * by a circle whose radius is the radius of its bounding sphere. For a
     * perspective camera, {@code distance} should be the distance from the
     * camera to the center of the volume. For a parallel camera, distance is
     * ignored.
     *
     * @param bound the bounds to calculate the area from (not null)
     * @param distance the distance from the camera to the center of the bounds
     * @param camera the camera whose projection and viewport dimensions are
     *     used (not null)
     * @return the estimated area in pixels, or 0 if the bounds are unsupported
     */
    public static float calcScreenArea(BoundingVolume bound, float distance, Camera camera) {
        float radius;
        if (bound.getType() == BoundingVolume.Type.Sphere) {
            radius = ((BoundingSphere) bound).getRadius();
        } else if (bound.getType() == BoundingVolume.Type.AABB) {
            BoundingBox box = (BoundingBox) bound;
            radius = FastMath.sqrt(box.getXExtent() * box.getXExtent()
                    + box.getYExtent() * box.getYExtent()
                    + box.getZExtent() * box.getZExtent());
        } else {
            return 0f;
        }

        if (radius == 0f) {
            return 0f;
        }

        float viewportWidth = camera.getWidth()
                * (camera.getViewPortRight() - camera.getViewPortLeft());
        float viewportHeight = camera.getHeight()
                * (camera.getViewPortTop() - camera.getViewPortBottom());
        float radiusX = radius * Math.abs(camera.getProjectionMatrix().m00)
                * viewportWidth * 0.5f;
        float radiusY = radius * Math.abs(camera.getProjectionMatrix().m11)
                * viewportHeight * 0.5f;

        if (!camera.isParallelProjection()) {
            float absoluteDistance = Math.abs(distance);
            if (absoluteDistance == 0f) {
                return Float.POSITIVE_INFINITY;
            }
            radiusX /= absoluteDistance;
            radiusY /= absoluteDistance;
        }

        return FastMath.PI * radiusX * radiusY;
    }

    private static float calcScreenArea(BoundingSphere bound, float distance, float screenWidth) {
        // Where is the center point and a radius point that lies in a plan parallel to the view plane?
        //    // Calc radius based on these two points and plug into circle area formula.
        //    Vector2f centerSP = null;
        //    Vector2f outerSP = null;
        //    float radiusSq = centerSP.subtract(outerSP).lengthSquared();
        float radius = (bound.getRadius() * screenWidth) / (distance * 2);
        return radius * radius * FastMath.PI;
    }

    private static float calcScreenArea(BoundingBox bound, float distance, float screenWidth) {
        // Calc as if we are a BoundingSphere for now...
        float radiusSquare = bound.getXExtent() * bound.getXExtent()
                         + bound.getYExtent() * bound.getYExtent()
                         + bound.getZExtent() * bound.getZExtent();
        return ((radiusSquare * screenWidth * screenWidth) / (distance * distance * 4)) * FastMath.PI;
    }
}
