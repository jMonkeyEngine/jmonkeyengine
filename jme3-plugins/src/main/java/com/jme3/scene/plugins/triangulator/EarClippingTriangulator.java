/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins.triangulator;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.IrPolygon;
import com.jme3.scene.plugins.IrVertex;
import java.util.ArrayList;

/**
 * Implemented according to
 * <ul>
 * <li>http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf</li>
 * <li>http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html</li>
 * </ul>
 */
public final class EarClippingTriangulator {

    private static enum VertexType {
        Convex,
        Reflex,
        Ear;
    }
    
    private final ArrayList<Integer> indices = new ArrayList<Integer>();
    private final ArrayList<VertexType> types = new ArrayList<VertexType>();
    private final ArrayList<Vector2f> positions = new ArrayList<Vector2f>();
    
    public EarClippingTriangulator() {
    }
    
    private static int ccw(Vector2f p0, Vector2f p1, Vector2f p2) {
        float result = (p1.x - p0.x) * (p2.y - p1.y) - (p1.y - p0.y) * (p2.x - p1.x);
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        } else {
            return 0;
        }
    }
    
    private static boolean pointInTriangle(Vector2f t0, Vector2f t1, Vector2f t2, Vector2f p) {
        float d = ((t1.y - t2.y) * (t0.x - t2.x) + (t2.x - t1.x) * (t0.y - t2.y));
        float a = ((t1.y - t2.y) * (p.x - t2.x) + (t2.x - t1.x) * (p.y - t2.y)) / d;
        float b = ((t2.y - t0.y) * (p.x - t2.x) + (t0.x - t2.x) * (p.y - t2.y)) / d;
        float c = 1 - a - b;
        return 0 <= a && a <= 1 && 0 <= b && b <= 1 && 0 <= c && c <= 1;
    }
    
    private static Matrix3f normalToMatrix(Vector3f norm) {
        Vector3f tang1 = norm.cross(Vector3f.UNIT_X);
        if (tang1.lengthSquared() < FastMath.ZERO_TOLERANCE) {
            tang1 = norm.cross(Vector3f.UNIT_Y);
        }
        tang1.normalizeLocal();
        Vector3f tang2 = norm.cross(tang1).normalizeLocal();

        return new Matrix3f(
                tang1.x, tang1.y, tang1.z,
                tang2.x, tang2.y, tang2.z,
                norm.x, norm.y, norm.z);
    }
    
    private int prev(int index) {
        if (index == 0) {
            return indices.size() - 1;
        } else {
            return index - 1;
        }
    }
    
    private int next(int index) {
        if (index == indices.size() - 1) {
            return 0;
        } else {
            return index + 1;
        }
    }
    
    private VertexType calcType(int index) {
        int prev = prev(index);
        int next = next(index);
        
        Vector2f p0 = positions.get(prev);
        Vector2f p1 = positions.get(index);
        Vector2f p2 = positions.get(next);

        if (ccw(p0, p1, p2) <= 0) {
            return VertexType.Reflex;
        } else {
            for (int i = 0; i < positions.size() - 3; i++) {
                int testIndex = (index + 2 + i) % positions.size();
                if (types.get(testIndex) != VertexType.Reflex) {
                    continue;
                }
                Vector2f p = positions.get(testIndex);
                if (pointInTriangle(p0, p1, p2, p)) {
                    return VertexType.Convex;
                }
            }
            return VertexType.Ear;
        }
    }
    
    private void updateType(int index) {
        if (types.get(index) == VertexType.Convex) {
            return;
        }
        types.set(index, calcType(index));
    }
    
    private void loadVertices(IrVertex[] vertices) {
        indices.ensureCapacity(vertices.length);
        types.ensureCapacity(vertices.length);
        positions.ensureCapacity(vertices.length);
        
        Vector3f normal = FastMath.computeNormal(
                                        vertices[0].pos,
                                        vertices[1].pos,
                                        vertices[2].pos);

        Matrix3f transform = normalToMatrix(normal);
        
        for (int i = 0; i < vertices.length; i++) {
            Vector3f projected = transform.mult(vertices[i].pos);
            indices.add(i);
            positions.add(new Vector2f(projected.x, projected.y));
            types.add(VertexType.Reflex);
        }
        
        for (int i = 0; i < vertices.length; i++) {
            types.set(i, calcType(i));
        }
    }
    
    private IrPolygon createTriangle(IrPolygon polygon, int prev, int index, int next) { 
        int p0 = indices.get(prev);
        int p1 = indices.get(index);
        int p2 = indices.get(next);
        IrPolygon triangle = new IrPolygon();
        triangle.vertices = new IrVertex[] {
            polygon.vertices[p0],
            polygon.vertices[p1],
            polygon.vertices[p2],
        };
        return triangle;
    }
    
    /**
     * Triangulates the given polygon.
     * 
     * Five or more vertices are required, if less are given, an exception
     * is thrown.
     * 
     * @param polygon The polygon to triangulate.
     * @return N - 2 triangles, where N is the number of vertices in the polygon.
     * 
     * @throws IllegalArgumentException If the polygon has less than 5 vertices.
     */
    public IrPolygon[] triangulate(IrPolygon polygon) {
        if (polygon.vertices.length < 5) {
            throw new IllegalArgumentException("Only polygons with 5 or more vertices are supported");
        }
        
        try {
            int numTris = 0;
            IrPolygon[] triangles = new IrPolygon[polygon.vertices.length - 2];
            
            loadVertices(polygon.vertices);
            
            int index = 0;
            while (types.size() > 3) {
                if (types.get(index) == VertexType.Ear) {
                    int prev = prev(index);
                    int next = next(index);
                    
                    triangles[numTris++] = createTriangle(polygon, prev, index, next);
                    
                    indices.remove(index);
                    types.remove(index);
                    positions.remove(index);
                    
                    next = next(prev);
                    updateType(prev);
                    updateType(next);
                    
                    index = next(next);
                } else {
                    index = next(index);
                }
            }
            
            if (types.size() == 3) {
                triangles[numTris++] = createTriangle(polygon, 0, 1, 2);
            }
            
            if (numTris != triangles.length) {
                throw new AssertionError("Triangulation failed to generate enough triangles");
            }
            
            return triangles;
        } finally {
            indices.clear();
            positions.clear();
            types.clear();
        }
    }
}
