/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.bounding;

import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This class includes some utility methods for computing intersection
 * between bounding volumes and triangles.
 * @author Kirill
 */
public class Intersection {

    private static final void findMinMax(float x0, float x1, float x2, Vector3f minMax) {
        minMax.set(x0, x0, 0);
        if (x1 < minMax.x) {
            minMax.setX(x1);
        }
        if (x1 > minMax.y) {
            minMax.setY(x1);
        }
        if (x2 < minMax.x) {
            minMax.setX(x2);
        }
        if (x2 > minMax.y) {
            minMax.setY(x2);
        }
    }

//    private boolean axisTest(float a, float b, float fa, float fb, Vector3f v0, Vector3f v1, )
//    private boolean axisTestX01(float a, float b, float fa, float fb,
//                             Vector3f center, Vector3f ext,
//                             Vector3f v1, Vector3f v2, Vector3f v3){
//	float p0 = a * v0.y - b * v0.z;
//	float p2 = a * v2.y - b * v2.z;
//        if(p0 < p2){
//            min = p0;
//            max = p2;
//        } else {
//            min = p2;
//            max = p0;
//        }
//	float rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
//	if(min > rad || max < -rad)
//            return false;
//    }
    public static boolean intersect(BoundingBox bbox, Vector3f v1, Vector3f v2, Vector3f v3) {
        //  use separating axis theorem to test overlap between triangle and box
        //  need to test for overlap in these directions:
        //  1) the {x,y,z}-directions (actually, since we use the AABB of the triangle
        //     we do not even need to test these)
        //  2) normal of the triangle
        //  3) crossproduct(edge from tri, {x,y,z}-directin)
        //       this gives 3x3=9 more tests

        TempVars vars = TempVars.get();


        Vector3f tmp0 = vars.vect1,
                tmp1 = vars.vect2,
                tmp2 = vars.vect3;

        Vector3f e0 = vars.vect4,
                e1 = vars.vect5,
                e2 = vars.vect6;

        Vector3f center = bbox.getCenter();
        Vector3f extent = bbox.getExtent(null);

//   float min,max,p0,p1,p2,rad,fex,fey,fez;
//   float normal[3]

        // This is the fastest branch on Sun
        // move everything so that the boxcenter is in (0,0,0)
        v1.subtract(center, tmp0);
        v2.subtract(center, tmp1);
        v3.subtract(center, tmp2);

        // compute triangle edges
        tmp1.subtract(tmp0, e0); // tri edge 0
        tmp2.subtract(tmp1, e1); // tri edge 1
        tmp0.subtract(tmp2, e2); // tri edge 2

        // Bullet 3:
        //  test the 9 tests first (this was faster)
        float min, max;
        float p0, p1, p2, rad;
        float fex = FastMath.abs(e0.x);
        float fey = FastMath.abs(e0.y);
        float fez = FastMath.abs(e0.z);



        //AXISTEST_X01(e0[Z], e0[Y], fez, fey);
        p0 = e0.z * tmp0.y - e0.y * tmp0.z;
        p2 = e0.z * tmp2.y - e0.y * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        //   AXISTEST_Y02(e0[Z], e0[X], fez, fex);
        p0 = -e0.z * tmp0.x + e0.x * tmp0.z;
        p2 = -e0.z * tmp2.x + e0.x * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.x + fex * extent.z;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        // AXISTEST_Z12(e0[Y], e0[X], fey, fex);
        p1 = e0.y * tmp1.x - e0.x * tmp1.y;
        p2 = e0.y * tmp2.x - e0.x * tmp2.y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        fex = FastMath.abs(e1.x);
        fey = FastMath.abs(e1.y);
        fez = FastMath.abs(e1.z);

//        AXISTEST_X01(e1[Z], e1[Y], fez, fey);
        p0 = e1.z * tmp0.y - e1.y * tmp0.z;
        p2 = e1.z * tmp2.y - e1.y * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        //   AXISTEST_Y02(e1[Z], e1[X], fez, fex);
        p0 = -e1.z * tmp0.x + e1.x * tmp0.z;
        p2 = -e1.z * tmp2.x + e1.x * tmp2.z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * extent.x + fex * extent.z;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        // AXISTEST_Z0(e1[Y], e1[X], fey, fex);
        p0 = e1.y * tmp0.x - e1.x * tmp0.y;
        p1 = e1.y * tmp1.x - e1.x * tmp1.y;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }
//
        fex = FastMath.abs(e2.x);
        fey = FastMath.abs(e2.y);
        fez = FastMath.abs(e2.z);

        // AXISTEST_X2(e2[Z], e2[Y], fez, fey);
        p0 = e2.z * tmp0.y - e2.y * tmp0.z;
        p1 = e2.z * tmp1.y - e2.y * tmp1.z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * extent.y + fey * extent.z;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        // AXISTEST_Y1(e2[Z], e2[X], fez, fex);
        p0 = -e2.z * tmp0.x + e2.x * tmp0.z;
        p1 = -e2.z * tmp1.x + e2.x * tmp1.z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

//   AXISTEST_Z12(e2[Y], e2[X], fey, fex);
        p1 = e2.y * tmp1.x - e2.x * tmp1.y;
        p2 = e2.y * tmp2.x - e2.x * tmp2.y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * extent.x + fex * extent.y;
        if (min > rad || max < -rad) {
            vars.release();
            return false;
        }

        //  Bullet 1:
        //  first test overlap in the {x,y,z}-directions
        //  find min, max of the triangle each direction, and test for overlap in
        //  that direction -- this is equivalent to testing a minimal AABB around
        //  the triangle against the AABB


        Vector3f minMax = vars.vect7;

        // test in X-direction
        findMinMax(tmp0.x, tmp1.x, tmp2.x, minMax);
        if (minMax.x > extent.x || minMax.y < -extent.x) {
            vars.release();
            return false;
        }

        // test in Y-direction
        findMinMax(tmp0.y, tmp1.y, tmp2.y, minMax);
        if (minMax.x > extent.y || minMax.y < -extent.y) {
            vars.release();
            return false;
        }

        // test in Z-direction
        findMinMax(tmp0.z, tmp1.z, tmp2.z, minMax);
        if (minMax.x > extent.z || minMax.y < -extent.z) {
            vars.release();
            return false;
        }

//       // Bullet 2:
//       //  test if the box intersects the plane of the triangle
//       //  compute plane equation of triangle: normal * x + d = 0
//        Vector3f normal = new Vector3f();
//        e0.cross(e1, normal);
        Plane p = vars.plane;

        p.setPlanePoints(v1, v2, v3);
        if (bbox.whichSide(p) == Plane.Side.Negative) {
            vars.release();
            return false;
        }
//
//        if(!planeBoxOverlap(normal,v0,boxhalfsize)) return false;

        vars.release();

        return true;   /* box and triangle overlaps */
    }
}
