/*
 * Copyright (c) 2017-2021 jMonkeyEngine
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

import com.jme3.renderer.Camera;
import com.jme3.util.TempVars;

/**
 * Created by Nehon on 23/04/2017.
 */
public class MathUtils {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MathUtils() {
    }

    /**
     * Calculate the natural logarithm of a unit quaternion.
     *
     * @param q the input Quaternion (not null, normalized, unaffected)
     * @param store storage for the result (not null, modified)
     * @return the logarithm (store)
     */
    public static Quaternion log(Quaternion q, Quaternion store) {
        float a = FastMath.acos(q.w);
        float sina = FastMath.sin(a);

        store.w = 0;
        if (sina > 0) {
            store.x = a * q.x / sina;
            store.y = a * q.y / sina;
            store.z = a * q.z / sina;
        } else {
            store.x = 0;
            store.y = 0;
            store.z = 0;
        }
        return store;
    }

    /**
     * Calculate the exponential of a pure quaternion.
     *
     * @param q the input Quaternion (not null, w=0, unaffected)
     * @param store storage for the result (not null, modified)
     * @return the exponential (store)
     */
    public static Quaternion exp(Quaternion q, Quaternion store) {

        float len = FastMath.sqrt(q.x * q.x + q.y * q.y + q.z * q.z);
        float sinLen = FastMath.sin(len);
        float cosLen = FastMath.cos(len);

        store.w = cosLen;
        if (len > 0) {
            store.x = sinLen * q.x / len;
            store.y = sinLen * q.y / len;
            store.z = sinLen * q.z / len;
        } else {
            store.x = 0;
            store.y = 0;
            store.z = 0;
        }
        return store;
    }

    //! This version of slerp, used by squad, does not check for theta > 90.
    public static Quaternion slerpNoInvert(Quaternion q1, Quaternion q2, float t, Quaternion store) {
        float dot = q1.dot(q2);

        if (dot > -0.95f && dot < 0.95f) {
            float angle = FastMath.acos(dot);
            float sin1 = FastMath.sin(angle * (1 - t));
            float sin2 = FastMath.sin(angle * t);
            float sin3 = FastMath.sin(angle);
            store.x = (q1.x * sin1 + q2.x * sin2) / sin3;
            store.y = (q1.y * sin1 + q2.y * sin2) / sin3;
            store.z = (q1.z * sin1 + q2.z * sin2) / sin3;
            store.w = (q1.w * sin1 + q2.w * sin2) / sin3;
            System.err.println("real slerp");
        } else {
            // if the angle is small, use linear interpolation
            store.set(q1).nlerp(q2, t);
            System.err.println("nlerp");
        }
        return store;
    }

    /**
     * Interpolate between 2 quaternions using Slerp.
     *
     * @param q1 the desired value for t=0
     * @param q2 the desired value for t=1
     * @param t the fractional parameter (&ge;0, &le;1)
     * @param store storage for the result (not null, modified)
     * @return the interpolated Quaternion (store)
     */
    public static Quaternion slerp(Quaternion q1, Quaternion q2, float t, Quaternion store) {

        float dot = (q1.x * q2.x) + (q1.y * q2.y) + (q1.z * q2.z)
                + (q1.w * q2.w);

        if (dot < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            q2.x = -q2.x;
            q2.y = -q2.y;
            q2.z = -q2.z;
            q2.w = -q2.w;
            dot = -dot;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t;
        float scale1 = t;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if (dot < 0.9f) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            float theta = FastMath.acos(dot);
            float invSinTheta = 1f / FastMath.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // its sine value
            scale0 = FastMath.sin((1 - t) * theta) * invSinTheta;
            scale1 = FastMath.sin((t * theta)) * invSinTheta;

            // Calculate the x, y, z and w values for the quaternion by using a
            // special
            // form of linear interpolation for quaternions.
            store.x = (scale0 * q1.x) + (scale1 * q2.x);
            store.y = (scale0 * q1.y) + (scale1 * q2.y);
            store.z = (scale0 * q1.z) + (scale1 * q2.z);
            store.w = (scale0 * q1.w) + (scale1 * q2.w);
        } else {
            store.x = (scale0 * q1.x) + (scale1 * q2.x);
            store.y = (scale0 * q1.y) + (scale1 * q2.y);
            store.z = (scale0 * q1.z) + (scale1 * q2.z);
            store.w = (scale0 * q1.w) + (scale1 * q2.w);
            store.normalizeLocal();
        }
        // Return the interpolated quaternion
        return store;
    }

//    //! Given 3 quaternions, qn-1,qn and qn+1, calculate a control point to be used in spline interpolation
//    private static Quaternion spline(Quaternion qnm1, Quaternion qn, Quaternion qnp1, Quaternion store, Quaternion tmp) {
//        store.set(-qn.x, -qn.y, -qn.z, qn.w);
//        //store.set(qn).inverseLocal();
//        tmp.set(store);
//
//        log(store.multLocal(qnm1), store);
//        log(tmp.multLocal(qnp1), tmp);
//        store.addLocal(tmp).multLocal(1f / -4f);
//        exp(store, tmp);
//        store.set(tmp).multLocal(qn);
//
//        return store.normalizeLocal();
//        //return qn * (((qni * qnm1).log() + (qni * qnp1).log()) / -4).exp();
//    }

    //! Given 3 quaternions, qn-1,qn and qn+1, calculate a control point to be used in spline interpolation
    private static Quaternion spline(Quaternion qnm1, Quaternion qn, Quaternion qnp1, Quaternion store, Quaternion tmp) {
        Quaternion invQn = new Quaternion(-qn.x, -qn.y, -qn.z, qn.w);

        log(invQn.mult(qnp1), tmp);
        log(invQn.mult(qnm1), store);
        store.addLocal(tmp).multLocal(-1f / 4f);
        exp(store, tmp);
        store.set(qn).multLocal(tmp);

        return store.normalizeLocal();
        //return qn * (((qni * qnm1).log() + (qni * qnp1).log()) / -4).exp();
    }

    //! spherical cubic interpolation
    public static Quaternion squad(Quaternion q0, Quaternion q1, Quaternion q2, Quaternion q3, Quaternion a, Quaternion b, float t, Quaternion store) {

        spline(q0, q1, q2, a, store);
        spline(q1, q2, q3, b, store);

        slerp(a, b, t, store);
        slerp(q1, q2, t, a);
        return slerp(a, store, 2 * t * (1 - t), b);
        //slerpNoInvert(a, b, t, store);
        //slerpNoInvert(q1, q2, t, a);
        //return slerpNoInvert(a, store, 2 * t * (1 - t), b);

//        quaternion c = slerpNoInvert(q1, q2, t),
//                d = slerpNoInvert(a, b, t);
//        return slerpNoInvert(c, d, 2 * t * (1 - t));
    }

    /**
     * Returns the shortest distance between a Ray and a segment.
     * The segment is defined by a start position and an end position in world space
     * The distance returned will be in world space (world units).
     * If the camera parameter is not null the distance will be returned in screen space (pixels)
     *
     * @param ray      The ray
     * @param segStart The start position of the segment in world space
     * @param segEnd   The end position of the segment in world space
     * @param camera   The renderer camera if the distance is required in screen space. Null if the distance is required in world space
     * @return the shortest distance between the ray and the segment or -1 if no solution is found.
     */
    public static float raySegmentShortestDistance(Ray ray, Vector3f segStart, Vector3f segEnd, Camera camera) {
        // Algorithm is ported from the C algorithm of
        // Paul Bourke at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline3d/
        TempVars vars = TempVars.get();
        Vector3f resultSegmentPoint1 = vars.vect1;
        Vector3f resultSegmentPoint2 = vars.vect2;

        Vector3f p1 = segStart;
        Vector3f p2 = segEnd;
        Vector3f p3 = ray.origin;
        Vector3f p4 = vars.vect3.set(ray.getDirection()).multLocal(Math.min(ray.getLimit(), 1000)).addLocal(ray.getOrigin());
        Vector3f p13 = vars.vect4.set(p1).subtractLocal(p3);
        Vector3f p43 = vars.vect5.set(p4).subtractLocal(p3);

        if (p43.lengthSquared() < 0.0001) {
            vars.release();
            return -1;
        }
        Vector3f p21 = vars.vect6.set(p2).subtractLocal(p1);
        if (p21.lengthSquared() < 0.0001) {
            vars.release();
            return -1;
        }

        double d1343 = p13.x * (double) p43.x + (double) p13.y * p43.y + (double) p13.z * p43.z;
        double d4321 = p43.x * (double) p21.x + (double) p43.y * p21.y + (double) p43.z * p21.z;
        double d1321 = p13.x * (double) p21.x + (double) p13.y * p21.y + (double) p13.z * p21.z;
        double d4343 = p43.x * (double) p43.x + (double) p43.y * p43.y + (double) p43.z * p43.z;
        double d2121 = p21.x * (double) p21.x + (double) p21.y * p21.y + (double) p21.z * p21.z;

        double denominator = d2121 * d4343 - d4321 * d4321;
        if (Math.abs(denominator) < 0.0001) {
            vars.release();
            return -1;
        }
        double numerator = d1343 * d4321 - d1321 * d4343;

        double mua = numerator / denominator;
        double mub = (d1343 + d4321 * (mua)) / d4343;

        resultSegmentPoint1.x = (float) (p1.x + mua * p21.x);
        resultSegmentPoint1.y = (float) (p1.y + mua * p21.y);
        resultSegmentPoint1.z = (float) (p1.z + mua * p21.z);
        resultSegmentPoint2.x = (float) (p3.x + mub * p43.x);
        resultSegmentPoint2.y = (float) (p3.y + mub * p43.y);
        resultSegmentPoint2.z = (float) (p3.z + mub * p43.z);

        //check if result 1 is in the segment section.
        float startToPoint = vars.vect3.set(resultSegmentPoint1).subtractLocal(segStart).lengthSquared();
        float endToPoint = vars.vect3.set(resultSegmentPoint1).subtractLocal(segEnd).lengthSquared();
        float segLength = vars.vect3.set(segEnd).subtractLocal(segStart).lengthSquared();
        if (startToPoint > segLength || endToPoint > segLength) {
            vars.release();
            return -1;
        }

        if (camera != null) {
            //camera is not null let's convert the points in screen space
            camera.getScreenCoordinates(resultSegmentPoint1, resultSegmentPoint1);
            camera.getScreenCoordinates(resultSegmentPoint2, resultSegmentPoint2);
        }

        float length = resultSegmentPoint1.subtractLocal(resultSegmentPoint2).length();
        vars.release();
        return length;
    }
}
