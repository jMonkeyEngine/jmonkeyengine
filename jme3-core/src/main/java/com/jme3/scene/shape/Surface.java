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
package com.jme3.scene.shape;

import com.jme3.math.CurveAndSurfaceMath;
import com.jme3.math.FastMath;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a surface described by knots, weights and control points.
 * Currently the following types are supported:
 * a) NURBS
 * @author Marcin Roguski (Kealthas)
 */
public class Surface extends Mesh {
    private SplineType           type;                // the type of the surface
    private List<List<Vector4f>> controlPoints;       // space control points and their weights
    private List<Float>[]        knots;               // knots of the surface
    private int                  basisUFunctionDegree; // the degree of basis U function
    private int                  basisVFunctionDegree; // the degree of basis V function
    private int                  uSegments;           // the amount of U segments
    private int                  vSegments;           // the amount of V segments

    /**
     * Constructor. Constructs required surface.
     * @param controlPoints space control points
     * @param nurbKnots knots of the surface
     * @param uSegments the amount of U segments
     * @param vSegments the amount of V segments
     * @param basisUFunctionDegree the degree of basis U function
     * @param basisVFunctionDegree the degree of basis V function
     * @param smooth defines if the mesu should be smooth (true) or flat (false)
     */
    private Surface(List<List<Vector4f>> controlPoints, List<Float>[] nurbKnots, int uSegments, int vSegments, int basisUFunctionDegree, int basisVFunctionDegree, boolean smooth) {
        this.validateInputData(controlPoints, nurbKnots, uSegments, vSegments);
        type = SplineType.Nurb;
        this.uSegments = uSegments;
        this.vSegments = vSegments;
        this.controlPoints = controlPoints;
        knots = nurbKnots;
        this.basisUFunctionDegree = basisUFunctionDegree;
        CurveAndSurfaceMath.prepareNurbsKnots(nurbKnots[0], basisUFunctionDegree);
        if (nurbKnots[1] != null) {
            this.basisVFunctionDegree = basisVFunctionDegree;
            CurveAndSurfaceMath.prepareNurbsKnots(nurbKnots[1], basisVFunctionDegree);
        }

        this.buildSurface(smooth);
    }

    /**
     * This method creates a NURBS surface. The created mesh is smooth by default.
     * @param controlPoints
     *            space control points
     * @param nurbKnots
     *            knots of the surface
     * @param uSegments
     *            the amount of U segments
     * @param vSegments
     *            the amount of V segments
     * @param basisUFunctionDegree
     *            the degree of basis U function
     * @param basisVFunctionDegree
     *            the degree of basis V function
     * @return an instance of NURBS surface
     */
    public static final Surface createNurbsSurface(List<List<Vector4f>> controlPoints, List<Float>[] nurbKnots, int uSegments, int vSegments, int basisUFunctionDegree, int basisVFunctionDegree) {
        return Surface.createNurbsSurface(controlPoints, nurbKnots, uSegments, vSegments, basisUFunctionDegree, basisVFunctionDegree, true);
    }

    /**
     * This method creates a NURBS surface.
     * @param controlPoints space control points
     * @param nurbKnots knots of the surface
     * @param uSegments the amount of U segments
     * @param vSegments the amount of V segments
     * @param basisUFunctionDegree the degree of basis U function
     * @param basisVFunctionDegree the degree of basis V function
     * @return an instance of NURBS surface
     */
    public static final Surface createNurbsSurface(List<List<Vector4f>> controlPoints, List<Float>[] nurbKnots, int uSegments, int vSegments, int basisUFunctionDegree, int basisVFunctionDegree, boolean smooth) {
        Surface result = new Surface(controlPoints, nurbKnots, uSegments, vSegments, basisUFunctionDegree, basisVFunctionDegree, smooth);
        result.type = SplineType.Nurb;
        return result;
    }

    /**
     * This method creates the surface.
     * @param smooth
     *            defines if the mesu should be smooth (true) or flat (false)
     */
    private void buildSurface(boolean smooth) {
        float minUKnot = this.getMinUNurbKnot();
        float maxUKnot = this.getMaxUNurbKnot();
        float deltaU = (maxUKnot - minUKnot) / uSegments;

        float minVKnot = this.getMinVNurbKnot();
        float maxVKnot = this.getMaxVNurbKnot();
        float deltaV = (maxVKnot - minVKnot) / vSegments;

        List<Vector3f> vertices = new ArrayList<Vector3f>((uSegments + 1) * (vSegments + 1));// new Vector3f[(uSegments + 1) * (vSegments + 1)];

        float u = minUKnot, v = minVKnot;
        for (int i = 0; i <= vSegments; ++i) {
            for (int j = 0; j <= uSegments; ++j) {
                Vector3f interpolationResult = new Vector3f();
                CurveAndSurfaceMath.interpolate(u, v, controlPoints, knots, basisUFunctionDegree, basisVFunctionDegree, interpolationResult);
                vertices.add(interpolationResult);
                u += deltaU;
            }
            u = minUKnot;
            v += deltaV;
        }
        if(!smooth) {
            // separate the vertices that will share faces (they will need separate normals anyway)
            // what happens with the mesh is represented here (be careful with code formatting here !!!)
            // * -- * -- *       * -- * * -- *
            // |    |    |       |    | |    |
            // * -- * -- *       * -- * * -- *
            // |    |    |  ==>  * -- * * -- *
            // * -- * -- *       |    | |    |
            // |    |    |       * -- * * -- *
            // * -- * -- *       .............
            // first duplicate all verts that are not on the border along the U axis
            int uVerticesAmount = uSegments + 1;
            int vVerticesAmount = vSegments + 1;
            int newUVerticesAmount = 2 + (uVerticesAmount - 2) * 2;
            List<Vector3f> verticesWithUDuplicates = new ArrayList<Vector3f>(vVerticesAmount * newUVerticesAmount);
            for(int i=0;i<vertices.size();++i) {
                verticesWithUDuplicates.add(vertices.get(i));
                if(i % uVerticesAmount != 0 && i % uVerticesAmount != uVerticesAmount - 1) {
                    verticesWithUDuplicates.add(vertices.get(i));
                }
            }
            // and then duplicate all verts that are not on the border along the V axis
            List<Vector3f> verticesWithVDuplicates = new ArrayList<Vector3f>(verticesWithUDuplicates.size() * vVerticesAmount);
            verticesWithVDuplicates.addAll(verticesWithUDuplicates.subList(0, newUVerticesAmount));
            for(int i=1;i<vSegments;++i) {
                verticesWithVDuplicates.addAll(verticesWithUDuplicates.subList(i * newUVerticesAmount, i * newUVerticesAmount + newUVerticesAmount));
                verticesWithVDuplicates.addAll(verticesWithUDuplicates.subList(i * newUVerticesAmount, i * newUVerticesAmount + newUVerticesAmount));
            }
            verticesWithVDuplicates.addAll(verticesWithUDuplicates.subList(vSegments * newUVerticesAmount, vSegments * newUVerticesAmount + newUVerticesAmount));
            vertices = verticesWithVDuplicates;
        }

        // adding indexes
        int[] indices = new int[uSegments * vSegments * 6];
        int arrayIndex = 0;
        int uVerticesAmount = smooth ? uSegments + 1 : uSegments * 2;
        if(smooth) {
            for (int i = 0; i < vSegments; ++i) { 
                for (int j = 0; j < uSegments; ++j) {
                    indices[arrayIndex++] = j + i * uVerticesAmount + uVerticesAmount;
                    indices[arrayIndex++] = j + i * uVerticesAmount + 1;
                    indices[arrayIndex++] = j + i * uVerticesAmount;
                    indices[arrayIndex++] = j + i * uVerticesAmount + uVerticesAmount;
                    indices[arrayIndex++] = j + i * uVerticesAmount + uVerticesAmount + 1;
                    indices[arrayIndex++] = j + i * uVerticesAmount + 1;
                }
            }
        } else {
            for (int i = 0; i < vSegments; ++i) {
                for (int j = 0; j < uSegments; ++j) {
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + uVerticesAmount + j * 2;
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + j * 2 + 1;
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + j * 2;
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + uVerticesAmount + j * 2;
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + uVerticesAmount + j * 2 + 1;
                    indices[arrayIndex++] = i * 2 * uVerticesAmount + j * 2 + 1;
                }
            }
        }

        Vector3f[] verticesArray = vertices.toArray(new Vector3f[vertices.size()]);
        // normalMap merges normals of faces that will be rendered smooth
        Map<Vector3f, Vector3f> normalMap = new HashMap<Vector3f, Vector3f>(verticesArray.length);
        for (int i = 0; i < indices.length; i += 3) {
            Vector3f n = FastMath.computeNormal(verticesArray[indices[i]], verticesArray[indices[i + 1]], verticesArray[indices[i + 2]]);
            this.addNormal(n, normalMap, smooth, verticesArray[indices[i]], verticesArray[indices[i + 1]], verticesArray[indices[i + 2]]);
        }
        // preparing normal list (the order of normals must match the order of vertices)
        float[] normals = new float[verticesArray.length * 3];
        arrayIndex = 0;
        for (int i = 0; i < verticesArray.length; ++i) {
            Vector3f n = normalMap.get(verticesArray[i]);
            normals[arrayIndex++] = n.x;
            normals[arrayIndex++] = n.y;
            normals[arrayIndex++] = n.z;
        }

        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verticesArray));
        this.setBuffer(VertexBuffer.Type.Index, 3, indices);
        this.setBuffer(VertexBuffer.Type.Normal, 3, normals);
        this.updateBound();
        this.updateCounts();
    }

    public List<List<Vector4f>> getControlPoints() {
        return controlPoints;
    }

    /**
     * This method returns the amount of U control points.
     * @return the amount of U control points
     */
    public int getUControlPointsAmount() {
        return controlPoints.size();
    }

    /**
     * This method returns the amount of V control points.
     * @return the amount of V control points
     */
    public int getVControlPointsAmount() {
        return controlPoints.get(0) == null ? 0 : controlPoints.get(0).size();
    }

    /**
     * This method returns the degree of basis U function.
     * @return the degree of basis U function
     */
    public int getBasisUFunctionDegree() {
        return basisUFunctionDegree;
    }

    /**
     * This method returns the degree of basis V function.
     * @return the degree of basis V function
     */
    public int getBasisVFunctionDegree() {
        return basisVFunctionDegree;
    }

    /**
     * This method returns the knots for specified dimension (U knots - value: '0',
     * V knots - value: '1').
     * @param dim an integer specifying if the U or V knots are required
     * @return an array of knots
     */
    public List<Float> getKnots(int dim) {
        return knots[dim];
    }

    /**
     * This method returns the type of the surface.
     * @return the type of the surface
     */
    public SplineType getType() {
        return type;
    }

    /**
     * This method returns the minimum nurb curve U knot value.
     * @return the minimum nurb curve knot value
     */
    private float getMinUNurbKnot() {
        return knots[0].get(basisUFunctionDegree - 1);
    }

    /**
     * This method returns the maximum nurb curve U knot value.
     * @return the maximum nurb curve knot value
     */
    private float getMaxUNurbKnot() {
        return knots[0].get(knots[0].size() - basisUFunctionDegree);
    }

    /**
     * This method returns the minimum nurb curve U knot value.
     * @return the minimum nurb curve knot value
     */
    private float getMinVNurbKnot() {
        return knots[1].get(basisVFunctionDegree - 1);
    }

    /**
     * This method returns the maximum nurb curve U knot value.
     * @return the maximum nurb curve knot value
     */
    private float getMaxVNurbKnot() {
        return knots[1].get(knots[1].size() - basisVFunctionDegree);
    }

    /**
     * This method adds a normal to a normal's map. This map is used to merge
     * normals of a vector that should be rendered smooth.
     *
     * @param normalToAdd
     *            a normal to be added
     * @param normalMap
     *            merges normals of faces that will be rendered smooth; the key is the vertex and the value - its normal vector
     * @param smooth the variable that indicates whether to merge normals
     * (creating the smooth mesh) or not
     * @param vertices
     *            a list of vertices read from the blender file
     */
    private void addNormal(Vector3f normalToAdd, Map<Vector3f, Vector3f> normalMap, boolean smooth, Vector3f... vertices) {
        for (Vector3f v : vertices) {
            Vector3f n = normalMap.get(v);
            if (!smooth || n == null) {
                normalMap.put(v, normalToAdd.clone());
            } else {
                n.addLocal(normalToAdd).normalizeLocal();
            }
        }
    }

    /**
     * This method validates the input data. It throws {@link IllegalArgumentException} if
     * the data is invalid.
     * @param controlPoints space control points
     * @param nurbKnots knots of the surface
     * @param uSegments the amount of U segments
     * @param vSegments the amount of V segments
     */
    private void validateInputData(List<List<Vector4f>> controlPoints, List<Float>[] nurbKnots,
            int uSegments, int vSegments) {
        int uPointsAmount = controlPoints.get(0).size();
        for (int i = 1; i < controlPoints.size(); ++i) {
            if (controlPoints.get(i).size() != uPointsAmount) {
                throw new IllegalArgumentException("The amount of 'U' control points is invalid!");
            }
        }
        if (uSegments <= 0) {
            throw new IllegalArgumentException("U segments amount should be positive!");
        }
        if (vSegments < 0) {
            throw new IllegalArgumentException("V segments amount cannot be negative!");
        }
        if (nurbKnots.length != 2) {
            throw new IllegalArgumentException("Nurb surface should have two rows of knots!");
        }
        for (int i = 0; i < nurbKnots.length; ++i) {
            for (int j = 0; j < nurbKnots[i].size() - 1; ++j) {
                if (nurbKnots[i].get(j) > nurbKnots[i].get(j + 1)) {
                    throw new IllegalArgumentException("The knots' values cannot decrease!");
                }
            }
        }
    }
}
