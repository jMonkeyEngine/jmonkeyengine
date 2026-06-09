/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * <code>BoundingBox</code> describes a bounding volume as an axis-aligned box.
 * <p>
 * Instances may be initialized by invoking the {@code containAABB} method.
 * </p>
 *
 * @author Joshua Slack
 */
public class BoundingBox extends BoundingVolume {

    /**
     * The X-extent of the box (half-width along X axis).
     */
    float xExtent, yExtent, zExtent;

    /**
     * Instantiate a <code>BoundingBox</code> without initializing it.
     */
    public BoundingBox() {
    }

    /**
     * Instantiate a <code>BoundingBox</code> with given center and extents.
     *
     * @param c the center of the box (not null, unaffected)
     * @param x the X-extent of the box (half-width along X axis, &ge;0)
     * @param y the Y-extent of the box (half-height along Y axis, &ge;0)
     * @param z the Z-extent of the box (half-depth along Z axis, &ge;0)
     */
    public BoundingBox(Vector3f c, float x, float y, float z) {
        this.center.set(c);
        this.xExtent = x;
        this.yExtent = y;
        this.zExtent = z;
    }

    /**
     * Instantiate a <code>BoundingBox</code> from min/max corner points.
     *
     * @param min the minimum corner (not null, unaffected)
     * @param max the maximum corner (not null, unaffected)
     */
    public BoundingBox(Vector3f min, Vector3f max) {
        setMinMax(min, max);
    }

    /**
     * Instantiate a <code>BoundingBox</code> equivalent to an existing box.
     *
     * @param source the existing box (not null, unaffected)
     */
    public BoundingBox(BoundingBox source) {
        this.center.set(source.center);
        this.xExtent = source.xExtent;
        this.yExtent = source.yExtent;
        this.zExtent = source.zExtent;
    }

    /**
     * Returns the type of bounding volume.
     *
     * @return {@code BoundingVolume.Type.AABB}
     */
    @Override
    public Type getType() {
        return Type.AABB;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Box from a given
     * set of points. It uses the <code>containAABB</code> method as default.
     *
     * @param points the points to contain (not null)
     */
    @Override
    public void computeFromPoints(FloatBuffer points) {
        containAABB(points);
    }

    /**
     * <code>computeFromTris</code> creates a new Bounding Box from a given
     * set of triangles.
     *
     * @param tris the triangles to contain (not null)
     * @param start the index of the first triangle to process
     * @param end the index after the last triangle to process
     */
    public void computeFromTris(Triangle[] tris, int start, int end) {
        if (end - start <= 0) {
            return;
        }
        TempVars vars = TempVars.get();
        try {
            Vector3f min = vars.vect1.set(tris[start].get(0));
            Vector3f max = vars.vect2.set(min);
            Vector3f point;
            for (int i = start; i < end; i++) {
                point = tris[i].get(0);
                checkMinMax(min, max, point);
                point = tris[i].get(1);
                checkMinMax(min, max, point);
                point = tris[i].get(2);
                checkMinMax(min, max, point);
            }
            center.set(min).addLocal(max).multLocal(0.5f);
            xExtent = max.x - center.x;
            yExtent = max.y - center.y;
            zExtent = max.z - center.z;
        } finally {
            vars.release();
        }
    }

    /**
     * Compute from mesh triangles.
     */
    public void computeFromTris(int[] indices, Mesh mesh, int start, int end) {
        if (end - start <= 0) {
            return;
        }
        TempVars vars = TempVars.get();
        try {
            Triangle triangle = new Triangle();
            mesh.getTriangle(indices[start], triangle);
            Vector3f min = vars.vect1.set(triangle.get(0));
            Vector3f max = vars.vect2.set(min);
            for (int i = start; i < end; i++) {
                mesh.getTriangle(indices[i], triangle);
                checkMinMax(min, max, triangle.get(0));
                checkMinMax(min, max, triangle.get(1));
                checkMinMax(min, max, triangle.get(2));
            }
            center.set(min).addLocal(max).multLocal(0.5f);
            xExtent = max.x - center.x;
            yExtent = max.y - center.y;
            zExtent = max.z - center.z;
        } finally {
            vars.release();
        }
    }

    /**
     * Update min/max vectors to include the given point.
     *
     * @param min the current minimum corner (modified)
     * @param max the current maximum corner (modified)
     * @param point the point to include (not null, unaffected)
     */
    public static void checkMinMax(Vector3f min, Vector3f max, Vector3f point) {
        if (point.x < min.x) min.x = point.x;
        if (point.x > max.x) max.x = point.x;
        if (point.y < min.y) min.y = point.y;
        if (point.y > max.y) max.y = point.y;
        if (point.z < min.z) min.z = point.z;
        if (point.z > max.z) max.z = point.z;
    }

    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned bounding
     * box of the points, then selects the smallest enclosing sphere of the box
     * with the sphere centered at the boxes center.
     *
     * @param points the list of points (not null, limit a multiple of 3)
     */
    public void containAABB(FloatBuffer points) {
        if (points.limit() <= 2) {
            return;
        }
        TempVars vars = TempVars.get();
        try {
            float minX = points.get(0), minY = points.get(1), minZ = points.get(2);
            float maxX = minX, maxY = minY, maxZ = minZ;
            for (int i = 3; i < points.limit(); i += 3) {
                float x = points.get(i);
                float y = points.get(i + 1);
                float z = points.get(i + 2);
                if (x < minX) minX = x; else if (x > maxX) maxX = x;
                if (y < minY) minY = y; else if (y > maxY) maxY = y;
                if (z < minZ) minZ = z; else if (z > maxZ) maxZ = z;
            }
            center.set(minX + maxX, minY + maxY, minZ + maxZ).multLocal(0.5f);
            xExtent = maxX - center.x;
            yExtent = maxY - center.y;
            zExtent = maxZ - center.z;
        } finally {
            vars.release();
        }
    }

    /**
     * Set this box from min and max corner vectors.
     *
     * @param min the minimum corner (not null, unaffected)
     * @param max the maximum corner (not null, unaffected)
     */
    public void setMinMax(Vector3f min, Vector3f max) {
        this.center.set(min).addLocal(max).multLocal(0.5f);
        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;
    }

    /**
     * Returns the X extent (half-width) of this box.
     *
     * @return the X extent (&ge;0)
     */
    public float getXExtent() {
        return xExtent;
    }

    /**
     * Returns the Y extent (half-height) of this box.
     *
     * @return the Y extent (&ge;0)
     */
    public float getYExtent() {
        return yExtent;
    }

    /**
     * Returns the Z extent (half-depth) of this box.
     *
     * @return the Z extent (&ge;0)
     */
    public float getZExtent() {
        return zExtent;
    }

    /**
     * Set the extents of this box.
     *
     * @param xExtent the X extent (&ge;0)
     * @param yExtent the Y extent (&ge;0)
     * @param zExtent the Z extent (&ge;0)
     */
    public void setXYZExtents(float xExtent, float yExtent, float zExtent) {
        this.xExtent = xExtent;
        this.yExtent = yExtent;
        this.zExtent = zExtent;
    }

    /**
     * Query the extent vector of this box.
     *
     * @param store storage for the result, or null to create one
     * @return the half-extent in each dimension (either {@code store} or a new vector)
     */
    public Vector3f getExtent(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(xExtent, yExtent, zExtent);
        return store;
    }

    /**
     * Returns the minimum corner of this bounding box.
     *
     * @param store storage for the result, or null to create one
     * @return center minus extents (either {@code store} or a new vector)
     */
    public Vector3f getMin(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(center.x - xExtent, center.y - yExtent, center.z - zExtent);
        return store;
    }

    /**
     * Returns the maximum corner of this bounding box.
     *
     * @param store storage for the result, or null to create one
     * @return center plus extents (either {@code store} or a new vector)
     */
    public Vector3f getMax(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(center.x + xExtent, center.y + yExtent, center.z + zExtent);
        return store;
    }

    // =========================================================================
    // NEW METHODS ADDED IN THIS PR
    // =========================================================================

    /**
     * Returns all 8 corner vertices of this axis-aligned bounding box.
     * <p>
     * The corners are ordered as follows (min = center - extent,
     * max = center + extent):
     * <ol start="0">
     *   <li>min.x, min.y, min.z</li>
     *   <li>max.x, min.y, min.z</li>
     *   <li>min.x, max.y, min.z</li>
     *   <li>max.x, max.y, min.z</li>
     *   <li>min.x, min.y, max.z</li>
     *   <li>max.x, min.y, max.z</li>
     *   <li>min.x, max.y, max.z</li>
     *   <li>max.x, max.y, max.z</li>
     * </ol>
     * </p>
     * <p>
     * If {@code store} is non-null and contains at least 8 elements, those
     * elements are overwritten and {@code store} is returned; otherwise a new
     * 8-element array is allocated.
     * </p>
     *
     * @param store an array of at least 8 {@link Vector3f} instances to reuse,
     *              or null to allocate a new array
     * @return an 8-element array of the corner positions (in world/local space
     *         matching the box's center)
     */
    public Vector3f[] getCorners(Vector3f[] store) {
        if (store == null || store.length < 8) {
            store = new Vector3f[8];
        }
        float minX = center.x - xExtent;
        float minY = center.y - yExtent;
        float minZ = center.z - zExtent;
        float maxX = center.x + xExtent;
        float maxY = center.y + yExtent;
        float maxZ = center.z + zExtent;

        // Initialise any null slots so callers never get null elements
        for (int i = 0; i < 8; i++) {
            if (store[i] == null) {
                store[i] = new Vector3f();
            }
        }

        store[0].set(minX, minY, minZ);
        store[1].set(maxX, minY, minZ);
        store[2].set(minX, maxY, minZ);
        store[3].set(maxX, maxY, minZ);
        store[4].set(minX, minY, maxZ);
        store[5].set(maxX, minY, maxZ);
        store[6].set(minX, maxY, maxZ);
        store[7].set(maxX, maxY, maxZ);

        return store;
    }

    /**
     * Returns all 8 corner vertices of this bounding box as a newly allocated
     * array.  Equivalent to {@code getCorners(null)}.
     *
     * @return a new 8-element array of corner positions
     * @see #getCorners(Vector3f[])
     */
    public Vector3f[] getCorners() {
        return getCorners(null);
    }

    /**
     * Expands this bounding box uniformly by {@code amount} in all three axes.
     * <p>
     * Each extent grows by {@code amount}, so the overall size increases by
     * {@code 2 * amount} per axis.  The center is unchanged.  If the resulting
     * extent would be negative (i.e. {@code amount} &lt; &minus;extent) the
     * corresponding extent is clamped to zero.
     * </p>
     *
     * @param amount the amount to add to each extent (may be negative to shrink)
     * @return this (for chaining)
     */
    public BoundingBox expand(float amount) {
        xExtent = Math.max(0f, xExtent + amount);
        yExtent = Math.max(0f, yExtent + amount);
        zExtent = Math.max(0f, zExtent + amount);
        return this;
    }

    /**
     * Expands this bounding box by independent amounts along each axis.
     * <p>
     * Each extent grows by the corresponding component of {@code amounts}.
     * The center is unchanged.  Negative values shrink the box; extents are
     * clamped to zero rather than going negative.
     * </p>
     *
     * @param amounts the per-axis expansion (not null, unaffected)
     * @return this (for chaining)
     */
    public BoundingBox expand(Vector3f amounts) {
        xExtent = Math.max(0f, xExtent + amounts.x);
        yExtent = Math.max(0f, yExtent + amounts.y);
        zExtent = Math.max(0f, zExtent + amounts.z);
        return this;
    }

    // =========================================================================
    // END NEW METHODS
    // =========================================================================

    /**
     * Transform this bounding box by the given matrix.
     *
     * @param trans    the transform matrix (not null, unaffected)
     * @param store    storage for the result, or null to create one
     * @return a transformed bounding box (either {@code store} or a new instance)
     */
    @Override
    public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }
        TempVars vars = TempVars.get();
        try {
            float w = trans.multProj(center, box.center);
            box.center.divideLocal(w);

            Matrix4f transAbs = vars.tempMat4;
            transAbs.m00 = FastMath.abs(trans.m00);
            transAbs.m01 = FastMath.abs(trans.m01);
            transAbs.m02 = FastMath.abs(trans.m02);
            transAbs.m10 = FastMath.abs(trans.m10);
            transAbs.m11 = FastMath.abs(trans.m11);
            transAbs.m12 = FastMath.abs(trans.m12);
            transAbs.m20 = FastMath.abs(trans.m20);
            transAbs.m21 = FastMath.abs(trans.m21);
            transAbs.m22 = FastMath.abs(trans.m22);

            Vector3f extentOld = vars.vect1.set(xExtent, yExtent, zExtent);
            Vector3f extentNew = vars.vect2;
            transAbs.mult(extentOld, extentNew);
            box.xExtent = FastMath.abs(extentNew.x);
            box.yExtent = FastMath.abs(extentNew.y);
            box.zExtent = FastMath.abs(extentNew.z);
        } finally {
            vars.release();
        }
        return box;
    }

    /**
     * Determine on which side of a plane this bounding box lies.
     *
     * @param plane the plane to test against (not null)
     * @return {@link Plane.Side#Positive}, {@link Plane.Side#Negative},
     *         or {@link Plane.Side#None}
     */
    @Override
    public Plane.Side whichSide(Plane plane) {
        Vector3f normal = plane.getNormal();
        float radius = FastMath.abs(xExtent * normal.x)
                + FastMath.abs(yExtent * normal.y)
                + FastMath.abs(zExtent * normal.z);
        float distance = plane.pseudoDistance(center);
        if (distance < -radius) return Plane.Side.Negative;
        if (distance >  radius) return Plane.Side.Positive;
        return Plane.Side.None;
    }

    /**
     * Merge this bounding box with another bounding volume.
     *
     * @param volume the second volume (not null, unaffected)
     * @return a new BoundingBox containing both volumes, or null if types differ
     */
    @Override
    public BoundingVolume merge(BoundingVolume volume) {
        return mergeLocal(volume);
    }

    /**
     * Merge this bounding box locally with another bounding volume.
     *
     * @param volume the second volume (not null, unaffected)
     * @return this box (modified) or null if types are incompatible
     */
    @Override
    public BoundingVolume mergeLocal(BoundingVolume volume) {
        if (volume == null) return this;
        switch (volume.getType()) {
            case AABB: {
                BoundingBox other = (BoundingBox) volume;
                TempVars vars = TempVars.get();
                try {
                    Vector3f min = vars.vect1.set(
                            Math.min(center.x - xExtent, other.center.x - other.xExtent),
                            Math.min(center.y - yExtent, other.center.y - other.yExtent),
                            Math.min(center.z - zExtent, other.center.z - other.zExtent));
                    Vector3f max = vars.vect2.set(
                            Math.max(center.x + xExtent, other.center.x + other.xExtent),
                            Math.max(center.y + yExtent, other.center.y + other.yExtent),
                            Math.max(center.z + zExtent, other.center.z + other.zExtent));
                    center.set(min).addLocal(max).multLocal(0.5f);
                    xExtent = max.x - center.x;
                    yExtent = max.y - center.y;
                    zExtent = max.z - center.z;
                } finally {
                    vars.release();
                }
                return this;
            }
            case Sphere: {
                BoundingSphere sphere = (BoundingSphere) volume;
                TempVars vars = TempVars.get();
                try {
                    Vector3f min = vars.vect1.set(
                            Math.min(center.x - xExtent, sphere.center.x - sphere.radius),
                            Math.min(center.y - yExtent, sphere.center.y - sphere.radius),
                            Math.min(center.z - zExtent, sphere.center.z - sphere.radius));
                    Vector3f max = vars.vect2.set(
                            Math.max(center.x + xExtent, sphere.center.x + sphere.radius),
                            Math.max(center.y + yExtent, sphere.center.y + sphere.radius),
                            Math.max(center.z + zExtent, sphere.center.z + sphere.radius));
                    center.set(min).addLocal(max).multLocal(0.5f);
                    xExtent = max.x - center.x;
                    yExtent = max.y - center.y;
                    zExtent = max.z - center.z;
                } finally {
                    vars.release();
                }
                return this;
            }
            default:
                return null;
        }
    }

    /**
     * Clone this bounding box.
     *
     * @param store storage for the result, or null to create one
     * @return an equivalent bounding box (either {@code store} or a new instance)
     */
    @Override
    public BoundingVolume clone(BoundingVolume store) {
        if (store != null && store.getType() == Type.AABB) {
            BoundingBox box = (BoundingBox) store;
            box.center.set(center);
            box.xExtent = xExtent;
            box.yExtent = yExtent;
            box.zExtent = zExtent;
            box.checkPlane = checkPlane;
            return box;
        }
        BoundingBox box = new BoundingBox(center, xExtent, yExtent, zExtent);
        box.checkPlane = checkPlane;
        return box;
    }

    /**
     * Returns a string representation of this bounding box.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "BoundingBox [Center: " + center + "  xExtent: " + xExtent
                + "  yExtent: " + yExtent + "  zExtent: " + zExtent + "]";
    }

    /**
     * Determine whether a given point is contained in (or on the surface of)
     * this bounding box.
     *
     * @param point the point to test (not null, unaffected)
     * @return true if the point is inside or on the boundary
     */
    @Override
    public boolean contains(Vector3f point) {
        return FastMath.abs(center.x - point.x) <= xExtent
                && FastMath.abs(center.y - point.y) <= yExtent
                && FastMath.abs(center.z - point.z) <= zExtent;
    }

    /**
     * Determine whether a given point is strictly inside this bounding box
     * (not touching the boundary).
     *
     * @param point the point to test (not null, unaffected)
     * @return true if the point is strictly inside
     */
    @Override
    public boolean intersects(Vector3f point) {
        return FastMath.abs(center.x - point.x) < xExtent
                && FastMath.abs(center.y - point.y) < yExtent
                && FastMath.abs(center.z - point.z) < zExtent;
    }

    /**
     * Determine whether this bounding box intersects another bounding volume.
     *
     * @param volume the second volume (not null)
     * @return true if they overlap
     */
    @Override
    public boolean intersects(BoundingVolume volume) {
        return volume.intersectsBoundingBox(this);
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere) {
        return sphere.intersectsBoundingBox(this);
    }

    @Override
    public boolean intersectsBoundingBox(BoundingBox box) {
        if (center.x + xExtent < box.center.x - box.xExtent
                || center.x - xExtent > box.center.x + box.xExtent) return false;
        if (center.y + yExtent < box.center.y - box.yExtent
                || center.y - yExtent > box.center.y + box.yExtent) return false;
        if (center.z + zExtent < box.center.z - box.zExtent
                || center.z - zExtent > box.center.z + box.zExtent) return false;
        return true;
    }

    @Override
    public boolean intersects(Ray ray) {
        float rhs;
        TempVars vars = TempVars.get();
        try {
            Vector3f diff = vars.vect1.set(ray.origin).subtractLocal(center);
            final float[] fWdU = vars.fWdU;
            final float[] fAWdU = vars.fAWdU;
            final float[] fDdU = vars.fDdU;
            final float[] fADdU = vars.fADdU;
            final float[] fAWxDdU = vars.fAWxDdU;
            final float[] extent = {xExtent, yExtent, zExtent};

            fWdU[0] = ray.getDirection().x;
            fAWdU[0] = FastMath.abs(fWdU[0]);
            fDdU[0] = diff.x;
            fADdU[0] = FastMath.abs(fDdU[0]);
            if (fADdU[0] > extent[0] && fDdU[0] * fWdU[0] >= 0.0) return false;

            fWdU[1] = ray.getDirection().y;
            fAWdU[1] = FastMath.abs(fWdU[1]);
            fDdU[1] = diff.y;
            fADdU[1] = FastMath.abs(fDdU[1]);
            if (fADdU[1] > extent[1] && fDdU[1] * fWdU[1] >= 0.0) return false;

            fWdU[2] = ray.getDirection().z;
            fAWdU[2] = FastMath.abs(fWdU[2]);
            fDdU[2] = diff.z;
            fADdU[2] = FastMath.abs(fDdU[2]);
            if (fADdU[2] > extent[2] && fDdU[2] * fWdU[2] >= 0.0) return false;

            Vector3f wCrossD = vars.vect2.set(ray.getDirection()).crossLocal(diff);

            fAWxDdU[0] = FastMath.abs(wCrossD.x);
            rhs = extent[1] * fAWdU[2] + extent[2] * fAWdU[1];
            if (fAWxDdU[0] > rhs) return false;

            fAWxDdU[1] = FastMath.abs(wCrossD.y);
            rhs = extent[0] * fAWdU[2] + extent[2] * fAWdU[0];
            if (fAWxDdU[1] > rhs) return false;

            fAWxDdU[2] = FastMath.abs(wCrossD.z);
            rhs = extent[0] * fAWdU[1] + extent[1] * fAWdU[0];
            if (fAWxDdU[2] > rhs) return false;
        } finally {
            vars.release();
        }
        return true;
    }

    @Override
    public float distanceToEdge(Vector3f point) {
        float dx = Math.max(0, Math.max(center.x - xExtent - point.x, point.x - center.x - xExtent));
        float dy = Math.max(0, Math.max(center.y - yExtent - point.y, point.y - center.y - yExtent));
        float dz = Math.max(0, Math.max(center.z - zExtent - point.z, point.z - center.z - zExtent));
        return FastMath.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        if (other instanceof Ray) {
            Ray ray = (Ray) other;
            return collideWithRay(ray, results);
        } else if (other instanceof BoundingVolume) {
            return intersects((BoundingVolume) other) ? 1 : 0;
        }
        throw new UnsupportedCollisionException("BoundingBox can not collide with " + other.getClass().getName());
    }

    private int collideWithRay(Ray ray, CollisionResults results) {
        TempVars vars = TempVars.get();
        try {
            Vector3f diff = vars.vect1.set(ray.origin).subtractLocal(center);
            float[] distances = vars.fWdU;
            if (!intersects(ray)) return 0;

            float t1 = (-xExtent - diff.x) / ray.direction.x;
            float t2 = ( xExtent - diff.x) / ray.direction.x;
            float tMin = Math.min(t1, t2);
            float tMax = Math.max(t1, t2);
            t1 = (-yExtent - diff.y) / ray.direction.y;
            t2 = ( yExtent - diff.y) / ray.direction.y;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            t1 = (-zExtent - diff.z) / ray.direction.z;
            t2 = ( zExtent - diff.z) / ray.direction.z;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));

            if (tMax < 0 || tMin > tMax) return 0;

            int count = 0;
            if (tMin >= 0) {
                CollisionResult cr = new CollisionResult();
                cr.setDistance(tMin);
                cr.setContactPoint(ray.direction.mult(tMin).add(ray.origin));
                results.addCollision(cr);
                count++;
            }
            CollisionResult cr = new CollisionResult();
            cr.setDistance(tMax);
            cr.setContactPoint(ray.direction.mult(tMax).add(ray.origin));
            results.addCollision(cr);
            count++;
            return count;
        } finally {
            vars.release();
        }
    }

    @Override
    public int collideWith(Collidable other) {
        if (other instanceof Ray) {
            return intersects((Ray) other) ? 1 : 0;
        } else if (other instanceof BoundingVolume) {
            return intersects((BoundingVolume) other) ? 1 : 0;
        }
        throw new UnsupportedCollisionException();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(xExtent, "xExtent", 0);
        capsule.write(yExtent, "yExtent", 0);
        capsule.write(zExtent, "zExtent", 0);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        xExtent = capsule.readFloat("xExtent", 0);
        yExtent = capsule.readFloat("yExtent", 0);
        zExtent = capsule.readFloat("zExtent", 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BoundingBox)) return false;
        BoundingBox other = (BoundingBox) obj;
        return center.equals(other.center)
                && xExtent == other.xExtent
                && yExtent == other.yExtent
                && zExtent == other.zExtent;
    }

    @Override
    public int hashCode() {
        int result = center.hashCode();
        result = 31 * result + Float.floatToIntBits(xExtent);
        result = 31 * result + Float.floatToIntBits(yExtent);
        result = 31 * result + Float.floatToIntBits(zExtent);
        return result;
    }
}
