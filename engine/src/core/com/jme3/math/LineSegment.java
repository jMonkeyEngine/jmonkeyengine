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
package com.jme3.math;

import com.jme3.export.*;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * <p>LineSegment represents a segment in the space. This is a portion of a Line
 * that has a limited start and end points.</p>
 * <p>A LineSegment is defined by an origin, a direction and an extent (or length).
 * Direction should be a normalized vector. It is not internally normalized.</p>
 * <p>This class provides methods to calculate distances between LineSegments, Rays and Vectors.
 * It is also possible to retrieve both end points of the segment {@link LineSegment#getPositiveEnd(Vector3f)}
 * and {@link LineSegment#getNegativeEnd(Vector3f)}. There are also methods to check whether
 * a point is within the segment bounds.</p>
 *
 * @see Ray
 * @author Mark Powell
 * @author Joshua Slack
 */
public class LineSegment implements Cloneable, Savable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private Vector3f origin;
    private Vector3f direction;
    private float extent;

    public LineSegment() {
        origin = new Vector3f();
        direction = new Vector3f();
    }

    public LineSegment(LineSegment ls) {
        this.origin = new Vector3f(ls.getOrigin());
        this.direction = new Vector3f(ls.getDirection());
        this.extent = ls.getExtent();
    }

    /**
     * <p>Creates a new LineSegment with the given origin, direction and extent.</p>
     * <p>Note that the origin is not one of the ends of the LineSegment, but its center.</p>
     */
    public LineSegment(Vector3f origin, Vector3f direction, float extent) {
        this.origin = origin;
        this.direction = direction;
        this.extent = extent;
    }

    /**
     * <p>Creates a new LineSegment with a given origin and end. This constructor will calculate the
     * center, the direction and the extent.</p>
     */
    public LineSegment(Vector3f start, Vector3f end) {
        this.origin = new Vector3f(0.5f * (start.x + end.x), 0.5f * (start.y + end.y), 0.5f * (start.z + end.z));
        this.direction = end.subtract(start);
        this.extent = direction.length() * 0.5f;
        direction.normalizeLocal();
    }

    public void set(LineSegment ls) {
        this.origin = new Vector3f(ls.getOrigin());
        this.direction = new Vector3f(ls.getDirection());
        this.extent = ls.getExtent();
    }

    public float distance(Vector3f point) {
        return FastMath.sqrt(distanceSquared(point));
    }

    public float distance(LineSegment ls) {
        return FastMath.sqrt(distanceSquared(ls));
    }

    public float distance(Ray r) {
        return FastMath.sqrt(distanceSquared(r));
    }

    public float distanceSquared(Vector3f point) {
        TempVars vars = TempVars.get();
        Vector3f compVec1 = vars.vect1;

        point.subtract(origin, compVec1);
        float segmentParameter = direction.dot(compVec1);

        if (-extent < segmentParameter) {
            if (segmentParameter < extent) {
                origin.add(direction.mult(segmentParameter, compVec1),
                        compVec1);
            } else {
                origin.add(direction.mult(extent, compVec1), compVec1);
            }
        } else {
            origin.subtract(direction.mult(extent, compVec1), compVec1);
        }

        compVec1.subtractLocal(point);
        float len = compVec1.lengthSquared();
        vars.release();
        return len;
    }

    public float distanceSquared(LineSegment test) {
        TempVars vars = TempVars.get();
        Vector3f compVec1 = vars.vect1;

        origin.subtract(test.getOrigin(), compVec1);
        float negativeDirectionDot = -(direction.dot(test.getDirection()));
        float diffThisDot = compVec1.dot(direction);
        float diffTestDot = -(compVec1.dot(test.getDirection()));
        float lengthOfDiff = compVec1.lengthSquared();
        vars.release();
        float determinant = FastMath.abs(1.0f - negativeDirectionDot
                * negativeDirectionDot);
        float s0, s1, squareDistance, extentDeterminant0, extentDeterminant1, tempS0, tempS1;

        if (determinant >= FastMath.FLT_EPSILON) {
            // segments are not parallel
            s0 = negativeDirectionDot * diffTestDot - diffThisDot;
            s1 = negativeDirectionDot * diffThisDot - diffTestDot;
            extentDeterminant0 = extent * determinant;
            extentDeterminant1 = test.getExtent() * determinant;

            if (s0 >= -extentDeterminant0) {
                if (s0 <= extentDeterminant0) {
                    if (s1 >= -extentDeterminant1) {
                        if (s1 <= extentDeterminant1) // region 0 (interior)
                        {
                            // minimum at two interior points of 3D lines
                            float inverseDeterminant = ((float) 1.0)
                                    / determinant;
                            s0 *= inverseDeterminant;
                            s1 *= inverseDeterminant;
                            squareDistance = s0
                                    * (s0 + negativeDirectionDot * s1 + (2.0f) * diffThisDot)
                                    + s1
                                    * (negativeDirectionDot * s0 + s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else // region 3 (side)
                        {
                            s1 = test.getExtent();
                            tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                            if (tempS0 < -extent) {
                                s0 = -extent;
                                squareDistance = s0 * (s0 - (2.0f) * tempS0)
                                        + s1 * (s1 + (2.0f) * diffTestDot)
                                        + lengthOfDiff;
                            } else if (tempS0 <= extent) {
                                s0 = tempS0;
                                squareDistance = -s0 * s0 + s1
                                        * (s1 + (2.0f) * diffTestDot)
                                        + lengthOfDiff;
                            } else {
                                s0 = extent;
                                squareDistance = s0 * (s0 - (2.0f) * tempS0)
                                        + s1 * (s1 + (2.0f) * diffTestDot)
                                        + lengthOfDiff;
                            }
                        }
                    } else // region 7 (side)
                    {
                        s1 = -test.getExtent();
                        tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                        if (tempS0 < -extent) {
                            s0 = -extent;
                            squareDistance = s0 * (s0 - (2.0f) * tempS0) + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else if (tempS0 <= extent) {
                            s0 = tempS0;
                            squareDistance = -s0 * s0 + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else {
                            s0 = extent;
                            squareDistance = s0 * (s0 - (2.0f) * tempS0) + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        }
                    }
                } else {
                    if (s1 >= -extentDeterminant1) {
                        if (s1 <= extentDeterminant1) // region 1 (side)
                        {
                            s0 = extent;
                            tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                            if (tempS1 < -test.getExtent()) {
                                s1 = -test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else if (tempS1 <= test.getExtent()) {
                                s1 = tempS1;
                                squareDistance = -s1 * s1 + s0
                                        * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else {
                                s1 = test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            }
                        } else // region 2 (corner)
                        {
                            s1 = test.getExtent();
                            tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                            if (tempS0 < -extent) {
                                s0 = -extent;
                                squareDistance = s0 * (s0 - (2.0f) * tempS0)
                                        + s1 * (s1 + (2.0f) * diffTestDot)
                                        + lengthOfDiff;
                            } else if (tempS0 <= extent) {
                                s0 = tempS0;
                                squareDistance = -s0 * s0 + s1
                                        * (s1 + (2.0f) * diffTestDot)
                                        + lengthOfDiff;
                            } else {
                                s0 = extent;
                                tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                                if (tempS1 < -test.getExtent()) {
                                    s1 = -test.getExtent();
                                    squareDistance = s1
                                            * (s1 - (2.0f) * tempS1) + s0
                                            * (s0 + (2.0f) * diffThisDot)
                                            + lengthOfDiff;
                                } else if (tempS1 <= test.getExtent()) {
                                    s1 = tempS1;
                                    squareDistance = -s1 * s1 + s0
                                            * (s0 + (2.0f) * diffThisDot)
                                            + lengthOfDiff;
                                } else {
                                    s1 = test.getExtent();
                                    squareDistance = s1
                                            * (s1 - (2.0f) * tempS1) + s0
                                            * (s0 + (2.0f) * diffThisDot)
                                            + lengthOfDiff;
                                }
                            }
                        }
                    } else // region 8 (corner)
                    {
                        s1 = -test.getExtent();
                        tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                        if (tempS0 < -extent) {
                            s0 = -extent;
                            squareDistance = s0 * (s0 - (2.0f) * tempS0) + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else if (tempS0 <= extent) {
                            s0 = tempS0;
                            squareDistance = -s0 * s0 + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else {
                            s0 = extent;
                            tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                            if (tempS1 > test.getExtent()) {
                                s1 = test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else if (tempS1 >= -test.getExtent()) {
                                s1 = tempS1;
                                squareDistance = -s1 * s1 + s0
                                        * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else {
                                s1 = -test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            }
                        }
                    }
                }
            } else {
                if (s1 >= -extentDeterminant1) {
                    if (s1 <= extentDeterminant1) // region 5 (side)
                    {
                        s0 = -extent;
                        tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                        if (tempS1 < -test.getExtent()) {
                            s1 = -test.getExtent();
                            squareDistance = s1 * (s1 - (2.0f) * tempS1) + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        } else if (tempS1 <= test.getExtent()) {
                            s1 = tempS1;
                            squareDistance = -s1 * s1 + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        } else {
                            s1 = test.getExtent();
                            squareDistance = s1 * (s1 - (2.0f) * tempS1) + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        }
                    } else // region 4 (corner)
                    {
                        s1 = test.getExtent();
                        tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                        if (tempS0 > extent) {
                            s0 = extent;
                            squareDistance = s0 * (s0 - (2.0f) * tempS0) + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else if (tempS0 >= -extent) {
                            s0 = tempS0;
                            squareDistance = -s0 * s0 + s1
                                    * (s1 + (2.0f) * diffTestDot)
                                    + lengthOfDiff;
                        } else {
                            s0 = -extent;
                            tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                            if (tempS1 < -test.getExtent()) {
                                s1 = -test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else if (tempS1 <= test.getExtent()) {
                                s1 = tempS1;
                                squareDistance = -s1 * s1 + s0
                                        * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            } else {
                                s1 = test.getExtent();
                                squareDistance = s1 * (s1 - (2.0f) * tempS1)
                                        + s0 * (s0 + (2.0f) * diffThisDot)
                                        + lengthOfDiff;
                            }
                        }
                    }
                } else // region 6 (corner)
                {
                    s1 = -test.getExtent();
                    tempS0 = -(negativeDirectionDot * s1 + diffThisDot);
                    if (tempS0 > extent) {
                        s0 = extent;
                        squareDistance = s0 * (s0 - (2.0f) * tempS0) + s1
                                * (s1 + (2.0f) * diffTestDot) + lengthOfDiff;
                    } else if (tempS0 >= -extent) {
                        s0 = tempS0;
                        squareDistance = -s0 * s0 + s1
                                * (s1 + (2.0f) * diffTestDot) + lengthOfDiff;
                    } else {
                        s0 = -extent;
                        tempS1 = -(negativeDirectionDot * s0 + diffTestDot);
                        if (tempS1 < -test.getExtent()) {
                            s1 = -test.getExtent();
                            squareDistance = s1 * (s1 - (2.0f) * tempS1) + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        } else if (tempS1 <= test.getExtent()) {
                            s1 = tempS1;
                            squareDistance = -s1 * s1 + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        } else {
                            s1 = test.getExtent();
                            squareDistance = s1 * (s1 - (2.0f) * tempS1) + s0
                                    * (s0 + (2.0f) * diffThisDot)
                                    + lengthOfDiff;
                        }
                    }
                }
            }
        } else {
            // The segments are parallel. The average b0 term is designed to
            // ensure symmetry of the function. That is, dist(seg0,seg1) and
            // dist(seg1,seg0) should produce the same number.get
            float extentSum = extent + test.getExtent();
            float sign = (negativeDirectionDot > 0.0f ? -1.0f : 1.0f);
            float averageB0 = (0.5f) * (diffThisDot - sign * diffTestDot);
            float lambda = -averageB0;
            if (lambda < -extentSum) {
                lambda = -extentSum;
            } else if (lambda > extentSum) {
                lambda = extentSum;
            }

            squareDistance = lambda * (lambda + (2.0f) * averageB0)
                    + lengthOfDiff;
        }

        return FastMath.abs(squareDistance);
    }

    public float distanceSquared(Ray r) {
        Vector3f kDiff = r.getOrigin().subtract(origin);
        float fA01 = -r.getDirection().dot(direction);
        float fB0 = kDiff.dot(r.getDirection());
        float fB1 = -kDiff.dot(direction);
        float fC = kDiff.lengthSquared();
        float fDet = FastMath.abs(1.0f - fA01 * fA01);
        float fS0, fS1, fSqrDist, fExtDet;

        if (fDet >= FastMath.FLT_EPSILON) {
            // The ray and segment are not parallel.
            fS0 = fA01 * fB1 - fB0;
            fS1 = fA01 * fB0 - fB1;
            fExtDet = extent * fDet;

            if (fS0 >= (float) 0.0) {
                if (fS1 >= -fExtDet) {
                    if (fS1 <= fExtDet) // region 0
                    {
                        // minimum at interior points of ray and segment
                        float fInvDet = ((float) 1.0) / fDet;
                        fS0 *= fInvDet;
                        fS1 *= fInvDet;
                        fSqrDist = fS0
                                * (fS0 + fA01 * fS1 + ((float) 2.0) * fB0)
                                + fS1
                                * (fA01 * fS0 + fS1 + ((float) 2.0) * fB1) + fC;
                    } else // region 1
                    {
                        fS1 = extent;
                        fS0 = -(fA01 * fS1 + fB0);
                        if (fS0 > (float) 0.0) {
                            fSqrDist = -fS0 * fS0 + fS1
                                    * (fS1 + ((float) 2.0) * fB1) + fC;
                        } else {
                            fS0 = (float) 0.0;
                            fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
                        }
                    }
                } else // region 5
                {
                    fS1 = -extent;
                    fS0 = -(fA01 * fS1 + fB0);
                    if (fS0 > (float) 0.0) {
                        fSqrDist = -fS0 * fS0 + fS1
                                * (fS1 + ((float) 2.0) * fB1) + fC;
                    } else {
                        fS0 = (float) 0.0;
                        fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
                    }
                }
            } else {
                if (fS1 <= -fExtDet) // region 4
                {
                    fS0 = -(-fA01 * extent + fB0);
                    if (fS0 > (float) 0.0) {
                        fS1 = -extent;
                        fSqrDist = -fS0 * fS0 + fS1
                                * (fS1 + ((float) 2.0) * fB1) + fC;
                    } else {
                        fS0 = (float) 0.0;
                        fS1 = -fB1;
                        if (fS1 < -extent) {
                            fS1 = -extent;
                        } else if (fS1 > extent) {
                            fS1 = extent;
                        }
                        fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
                    }
                } else if (fS1 <= fExtDet) // region 3
                {
                    fS0 = (float) 0.0;
                    fS1 = -fB1;
                    if (fS1 < -extent) {
                        fS1 = -extent;
                    } else if (fS1 > extent) {
                        fS1 = extent;
                    }
                    fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
                } else // region 2
                {
                    fS0 = -(fA01 * extent + fB0);
                    if (fS0 > (float) 0.0) {
                        fS1 = extent;
                        fSqrDist = -fS0 * fS0 + fS1
                                * (fS1 + ((float) 2.0) * fB1) + fC;
                    } else {
                        fS0 = (float) 0.0;
                        fS1 = -fB1;
                        if (fS1 < -extent) {
                            fS1 = -extent;
                        } else if (fS1 > extent) {
                            fS1 = extent;
                        }
                        fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
                    }
                }
            }
        } else {
            // ray and segment are parallel
            if (fA01 > (float) 0.0) {
                // opposite direction vectors
                fS1 = -extent;
            } else {
                // same direction vectors
                fS1 = extent;
            }

            fS0 = -(fA01 * fS1 + fB0);
            if (fS0 > (float) 0.0) {
                fSqrDist = -fS0 * fS0 + fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
            } else {
                fS0 = (float) 0.0;
                fSqrDist = fS1 * (fS1 + ((float) 2.0) * fB1) + fC;
            }
        }
        return FastMath.abs(fSqrDist);
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    public float getExtent() {
        return extent;
    }

    public void setExtent(float extent) {
        this.extent = extent;
    }

    public Vector3f getOrigin() {
        return origin;
    }

    public void setOrigin(Vector3f origin) {
        this.origin = origin;
    }

    // P+e*D
    public Vector3f getPositiveEnd(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return origin.add((direction.mult(extent, store)), store);
    }

    // P-e*D
    public Vector3f getNegativeEnd(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return origin.subtract((direction.mult(extent, store)), store);
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(origin, "origin", Vector3f.ZERO);
        capsule.write(direction, "direction", Vector3f.ZERO);
        capsule.write(extent, "extent", 0);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        origin = (Vector3f) capsule.readSavable("origin", Vector3f.ZERO.clone());
        direction = (Vector3f) capsule.readSavable("direction", Vector3f.ZERO.clone());
        extent = capsule.readFloat("extent", 0);
    }

    @Override
    public LineSegment clone() {
        try {
            LineSegment segment = (LineSegment) super.clone();
            segment.direction = direction.clone();
            segment.origin = origin.clone();
            return segment;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * <p>Evaluates whether a given point is contained within the axis aligned bounding box
     * that contains this LineSegment.</p><p>This function is float error aware.</p>
     */
    public boolean isPointInsideBounds(Vector3f point) {
        return isPointInsideBounds(point, Float.MIN_VALUE);
    }

    /**
     * <p>Evaluates whether a given point is contained within the axis aligned bounding box
     * that contains this LineSegment.</p><p>This function accepts an error parameter, which
     * is added to the extent of the bounding box.</p>
     */
    public boolean isPointInsideBounds(Vector3f point, float error) {

        if (FastMath.abs(point.x - origin.x) > FastMath.abs(direction.x * extent) + error) {
            return false;
        }
        if (FastMath.abs(point.y - origin.y) > FastMath.abs(direction.y * extent) + error) {
            return false;
        }
        if (FastMath.abs(point.z - origin.z) > FastMath.abs(direction.z * extent) + error) {
            return false;
        }

        return true;
    }
}
