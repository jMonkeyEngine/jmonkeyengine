/*
 * Copyright (c) 2009-2011 jMonkeyEngine
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

package com.jme3.gde.terraineditor.tools;

import com.jme3.math.Vector2f;

/**
 * Handy utilities for the editor tools
 * @author Brent Owens
 */
public class ToolUtils {

    /**
     * See if the X,Y coordinate is in the radius of the circle. It is assumed
     * that the "grid" being tested is located at 0,0 and its dimensions are 2*radius.
     * @param x
     * @param z
     * @param radius
     * @return
     */
    public static boolean isInRadius(float x, float y, float radius) {
        Vector2f point = new Vector2f(x,y);
        // return true if the distance is less than equal to the radius
        return Math.abs(point.length()) <= radius;
    }

    /**
     * Interpolate the height value based on its distance from the center (how far along
     * the radius it is).
     * The farther from the center, the less the height will be.
     * This produces a linear height falloff.
     * @param radius of the tool
     * @param heightFactor potential height value to be adjusted
     * @param x location
     * @param z location
     * @return the adjusted height value
     */
    public static float calculateHeight(float radius, float heightFactor, float x, float z) {
        float val = calculateRadiusPercent(radius, x, z);
        return heightFactor * val;
    }

    public static float calculateRadiusPercent(float radius, float x, float z) {
         // find percentage for each 'unit' in radius
        Vector2f point = new Vector2f(x,z);
        float val = Math.abs(point.length()) / radius;
        val = 1f - val;
        return val;
    }
    
    public static int compareFloat(float a, float b, float epsilon) {
        if (floatEquals(a, b, epsilon))
            return 0;
        else if (floatLessThan(a, b, epsilon))
            return -1;
        else
            return 1;
    }

    public static boolean floatEquals(float a, float b, float epsilon) {
        return a == b ? true : Math.abs(a - b) < epsilon;
    }

    public static boolean floatLessThan(float a, float b, float epsilon) {
        return b - a > epsilon;
    }

    public static boolean floatGreaterThan(float a, float b, float epsilon) {
        return a - b > epsilon;
    }
}
