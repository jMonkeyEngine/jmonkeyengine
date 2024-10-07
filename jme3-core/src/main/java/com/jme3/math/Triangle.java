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
package com.jme3.math;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * Describes a triangle in terms of its vertex locations, with auxiliary storage
 * for its centroid, normal vector, projection, and index.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public class Triangle extends AbstractTriangle implements Savable, Cloneable, java.io.Serializable {
    static final long serialVersionUID = 1;
    /**
     * The location of the first vertex in winding order.
     */
    private Vector3f pointA = new Vector3f();
    /**
     * The location of the 2nd vertex in winding order.
     */
    private Vector3f pointB = new Vector3f();
    /**
     * The location of the 3rd vertex in winding order.
     */
    private Vector3f pointC = new Vector3f();
    private transient Vector3f center;
    private transient Vector3f normal;
    private float projection;
    /**
     * The index of the triangle, used to identify it in an OBBTree.
     */
    private int index;

    /**
     * Instantiate a zero-size triangle at the origin.
     */
    public Triangle() {
    }

    /**
     * Instantiates a triangle with the specified vertex locations. Vertices
     * should be listed in the desired winding order, typically
     * counter-clockwise.
     *
     * @param p1 the location of the first vertex (not null, unaffected)
     * @param p2 the location of the 2nd vertex (not null, unaffected)
     * @param p3 the location of the 3rd vertex (not null, unaffected)
     */
    public Triangle(Vector3f p1, Vector3f p2, Vector3f p3) {
        pointA.set(p1);
        pointB.set(p2);
        pointC.set(p3);
    }

    /**
     * Accesses the location of the indexed vertex.
     *
     * @param i the index of the vertex to access (0, 1, or 2)
     * @return a pre-existing location vector, or null if the index is invalid
     */
    public Vector3f get(int i) {
        switch (i) {
            case 0:
                return pointA;
            case 1:
                return pointB;
            case 2:
                return pointC;
            default:
                return null;
        }
    }

    /**
     * Accesses the location of the first vertex.
     *
     * @return the pre-existing location vector (not null)
     */
    @Override
    public Vector3f get1() {
        return pointA;
    }

    /**
     * Accesses the location of the 2nd vertex.
     *
     * @return the pre-existing location vector (not null)
     */
    @Override
    public Vector3f get2() {
        return pointB;
    }

    /**
     * Accesses the location of the 3rd vertex.
     *
     * @return the pre-existing location vector (not null)
     */
    @Override
    public Vector3f get3() {
        return pointC;
    }

    /**
     * Alters the location of the indexed vertex and deletes the stored centroid
     * and normal.
     *
     * @param i the index of the vertex to alter (0, 1, or 2)
     * @param point the desired location (not null, unaffected)
     */
    public void set(int i, Vector3f point) {
        center = null;
        normal = null;

        switch (i) {
            case 0:
                pointA.set(point);
                break;
            case 1:
                pointB.set(point);
                break;
            case 2:
                pointC.set(point);
                break;
        }
    }

    /**
     * Alters the location of the indexed vertex and deletes the stored centroid
     * and normal.
     *
     * @param i the index of the vertex to alter (0, 1, or 2)
     * @param x the desired X coordinate
     * @param y the desired Y coordinate
     * @param z the desired Z coordinate
     */
    public void set(int i, float x, float y, float z) {
        center = null;
        normal = null;

        switch (i) {
            case 0:
                pointA.set(x, y, z);
                break;
            case 1:
                pointB.set(x, y, z);
                break;
            case 2:
                pointC.set(x, y, z);
                break;
        }
    }

    /**
     * Alters the location of the first vertex and deletes the stored centroid
     * and normal.
     *
     * @param v the desired location (not null, unaffected)
     */
    public void set1(Vector3f v) {
        center = null;
        normal = null;

        pointA.set(v);
    }

    /**
     * Alters the location of the 2nd vertex and deletes the stored centroid and
     * normal.
     *
     * @param v the desired location (not null, unaffected)
     */
    public void set2(Vector3f v) {
        center = null;
        normal = null;

        pointB.set(v);
    }

    /**
     * Alters the location of the 3rd vertex and deletes the stored centroid and
     * normal.
     *
     * @param v the desired location (not null, unaffected)
     */
    public void set3(Vector3f v) {
        center = null;
        normal = null;

        pointC.set(v);
    }

    /**
     * Alters the locations of all 3 vertices and deletes the stored centroid
     * and normal.
     *
     * @param v1 the desired location of the first vertex (not null, unaffected)
     * @param v2 the desired location of the 2nd vertex (not null, unaffected)
     * @param v3 the desired location of the 3rd vertex (not null, unaffected)
     */
    @Override
    public void set(Vector3f v1, Vector3f v2, Vector3f v3) {
        center = null;
        normal = null;

        pointA.set(v1);
        pointB.set(v2);
        pointC.set(v3);
    }

    /**
     * Recalculates the stored centroid based on the current vertex locations.
     */
    public void calculateCenter() {
        if (center == null) {
            center = new Vector3f(pointA);
        } else {
            center.set(pointA);
        }
        center.addLocal(pointB).addLocal(pointC).multLocal(FastMath.ONE_THIRD);
    }

    /**
     * Recalculates the stored normal based on the current vertex locations.
     */
    public void calculateNormal() {
        if (normal == null) {
            normal = new Vector3f(pointB);
        } else {
            normal.set(pointB);
        }
        normal.subtractLocal(pointA).crossLocal(pointC.x - pointA.x, pointC.y - pointA.y, pointC.z - pointA.z);
        normal.normalizeLocal();
    }

    /**
     * Accesses the stored centroid (the average of the 3 vertex locations)
     * calculating it if it is null.
     *
     * @return the coordinates of the center (an internal vector subject to
     * re-use)
     */
    public Vector3f getCenter() {
        if (center == null) {
            calculateCenter();
        }
        return center;
    }

    /**
     * Alters the stored centroid without affecting the stored normal or any
     * vertex locations.
     *
     * @param center the desired value (alias created if not null)
     */
    public void setCenter(Vector3f center) {
        this.center = center;
    }

    /**
     * Accesses the stored normal, updating it if it is null.
     *
     * @return unit normal vector (an internal vector subject to re-use)
     */
    public Vector3f getNormal() {
        if (normal == null) {
            calculateNormal();
        }
        return normal;
    }

    /**
     * Alters the stored normal without affecting the stored centroid or any
     * vertex locations.
     *
     * @param normal the desired value (alias created if not null)
     */
    public void setNormal(Vector3f normal) {
        this.normal = normal;
    }

    /**
     * Returns the projection of the vertices relative to the line origin.
     *
     * @return the stored projection value
     */
    public float getProjection() {
        return this.projection;
    }

    /**
     * Alters the projection of the vertices relative to the line origin.
     *
     * @param projection the desired projection value
     */
    public void setProjection(float projection) {
        this.projection = projection;
    }

    /**
     * Returns the index of this triangle, used to identify it in an OBBTree.
     *
     * @return the stored index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Alters the index of this triangle, used to identify it in an OBBTree.
     *
     * @param index the desired index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    public static Vector3f computeTriangleNormal(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f store) {
        if (store == null) {
            store = new Vector3f(v2);
        } else {
            store.set(v2);
        }

        store.subtractLocal(v1).crossLocal(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
        return store.normalizeLocal();
    }

    /**
     * Serializes this triangle to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param e (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        e.getCapsule(this).write(pointA, "pointa", Vector3f.ZERO);
        e.getCapsule(this).write(pointB, "pointb", Vector3f.ZERO);
        e.getCapsule(this).write(pointC, "pointc", Vector3f.ZERO);
    }

    /**
     * De-serializes this triangle from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        pointA = (Vector3f) importer.getCapsule(this).readSavable("pointa", Vector3f.ZERO.clone());
        pointB = (Vector3f) importer.getCapsule(this).readSavable("pointb", Vector3f.ZERO.clone());
        pointC = (Vector3f) importer.getCapsule(this).readSavable("pointc", Vector3f.ZERO.clone());
    }

    /**
     * Creates a copy of this triangle.
     *
     * @return a new instance, equivalent to this one
     */
    @Override
    public Triangle clone() {
        try {
            Triangle t = (Triangle) super.clone();
            t.pointA = pointA.clone();
            t.pointB = pointB.clone();
            t.pointC = pointC.clone();
            // XXX: the center and normal are not cloned!
            return t;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
