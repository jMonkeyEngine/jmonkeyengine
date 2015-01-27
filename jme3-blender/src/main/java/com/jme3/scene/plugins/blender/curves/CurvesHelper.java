/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.curves;

import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class CurvesHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER                      = Logger.getLogger(CurvesHelper.class.getName());

    /** Minimum basis U function degree for NURBS curves and surfaces. */
    protected int               minimumBasisUFunctionDegree = 4;
    /** Minimum basis V function degree for NURBS curves and surfaces. */
    protected int               minimumBasisVFunctionDegree = 4;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public CurvesHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    public CurvesTemporalMesh toCurve(Structure curveStructure, BlenderContext blenderContext) throws BlenderFileException {
        CurvesTemporalMesh result = new CurvesTemporalMesh(curveStructure, blenderContext);

        if (blenderContext.getBlenderKey().isLoadObjectProperties()) {
            LOGGER.fine("Reading custom properties.");
            result.setProperties(this.loadProperties(curveStructure, blenderContext));
        }

        return result;
    }

    /**
     * The method transforms the bevel along the curve.
     * 
     * @param bevel
     *            the bevel to be transformed
     * @param prevPos
     *            previous curve point
     * @param currPos
     *            current curve point (here the center of the new bevel will be
     *            set)
     * @param nextPos
     *            next curve point
     * @return points of transformed bevel
     */
    protected Vector3f[] transformBevel(Vector3f[] bevel, Vector3f prevPos, Vector3f currPos, Vector3f nextPos) {
        bevel = bevel.clone();

        // currPos and directionVector define the line in 3D space
        Vector3f directionVector = prevPos != null ? currPos.subtract(prevPos) : nextPos.subtract(currPos);
        directionVector.normalizeLocal();

        // plane is described by equation: Ax + By + Cz + D = 0 where planeNormal = [A, B, C] and D = -(Ax + By + Cz)
        Vector3f planeNormal = null;
        if (prevPos != null) {
            planeNormal = currPos.subtract(prevPos).normalizeLocal();
            if (nextPos != null) {
                planeNormal.addLocal(nextPos.subtract(currPos).normalizeLocal()).normalizeLocal();
            }
        } else {
            planeNormal = nextPos.subtract(currPos).normalizeLocal();
        }
        float D = -planeNormal.dot(currPos);// D = -(Ax + By + Cz)

        // now we need to compute paralell cast of each bevel point on the plane, the leading line is already known
        // parametric equation of a line: x = px + vx * t; y = py + vy * t; z = pz + vz * t
        // where p = currPos and v = directionVector
        // using x, y and z in plane equation we get value of 't' that will allow us to compute the point where plane and line cross
        float temp = planeNormal.dot(directionVector);
        for (int i = 0; i < bevel.length; ++i) {
            float t = -(planeNormal.dot(bevel[i]) + D) / temp;
            if (fixUpAxis) {
                bevel[i] = new Vector3f(bevel[i].x + directionVector.x * t, bevel[i].y + directionVector.y * t, bevel[i].z + directionVector.z * t);
            } else {
                bevel[i] = new Vector3f(bevel[i].x + directionVector.x * t, -bevel[i].z + directionVector.z * t, bevel[i].y + directionVector.y * t);
            }
        }
        return bevel;
    }

    /**
     * This method transforms the first line of the bevel points positioning it
     * on the first point of the curve.
     * 
     * @param startingLinePoints
     *            the vbevel shape points
     * @param firstCurvePoint
     *            the first curve's point
     * @param secondCurvePoint
     *            the second curve's point
     * @return points of transformed bevel
     */
    protected Vector3f[] transformToFirstLineOfBevelPoints(Vector3f[] startingLinePoints, Vector3f firstCurvePoint, Vector3f secondCurvePoint) {
        Vector3f planeNormal = secondCurvePoint.subtract(firstCurvePoint).normalizeLocal();

        float angle = FastMath.acos(planeNormal.dot(Vector3f.UNIT_X));
        Vector3f rotationVector = Vector3f.UNIT_X.cross(planeNormal).normalizeLocal();
        
        Matrix4f m = new Matrix4f();
        m.setRotationQuaternion(new Quaternion().fromAngleAxis(angle, rotationVector));
        m.setTranslation(firstCurvePoint);

        Vector3f temp = new Vector3f();
        Vector3f[] verts = new Vector3f[startingLinePoints.length];
        for (int i = 0; i < verts.length; ++i) {
            verts[i] = m.mult(startingLinePoints[i], temp).clone();
        }
        return verts;
    }
}