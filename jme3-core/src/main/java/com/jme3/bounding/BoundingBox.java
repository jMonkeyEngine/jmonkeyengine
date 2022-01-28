/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import com.jme3.math.*;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;
//import com.jme.scene.TriMesh;

/**
 * <code>BoundingBox</code> describes a bounding volume as an axis-aligned box.
 * <br>
 * Instances may be initialized by invoking the <code>containAABB</code> method.
 *
 * @author Joshua Slack
 * @version $Id: BoundingBox.java,v 1.50 2007/09/22 16:46:35 irrisor Exp $
 */
public class BoundingBox extends BoundingVolume {
    /**
     * the X-extent of the box (>=0, may be +Infinity)
     */
    float xExtent;
    /**
     * the Y-extent of the box (>=0, may be +Infinity)
     */
    float yExtent;
    /**
     * the Z-extent of the box (>=0, may be +Infinity)
     */
    float zExtent;

    /**
     * Instantiate a <code>BoundingBox</code> without initializing it.
     */
    public BoundingBox() {
    }

    /**
     * Instantiate a <code>BoundingBox</code> with given center and extents.
     *
     * @param c the coordinates of the center of the box (not null, not altered)
     * @param x the X-extent of the box (0 or greater, may be +Infinity)
     * @param y the Y-extent of the box (0 or greater, may be +Infinity)
     * @param z the Z-extent of the box (0 or greater, may be +Infinity)
     */
    public BoundingBox(Vector3f c, float x, float y, float z) {
        this.center.set(c);
        this.xExtent = x;
        this.yExtent = y;
        this.zExtent = z;
    }

    /**
     * Instantiate a <code>BoundingBox</code> equivalent to an existing box.
     *
     * @param source the existing box (not null, not altered)
     */
    public BoundingBox(BoundingBox source) {
        this.center.set(source.center);
        this.xExtent = source.xExtent;
        this.yExtent = source.yExtent;
        this.zExtent = source.zExtent;
    }

    /**
     * Instantiate a BoundingBox with the specified extremes.
     *
     * @param min the desired minimum coordinate value for each axis (not null,
     * not altered)
     * @param max the desired maximum coordinate value for each axis (not null,
     * not altered)
     */
    public BoundingBox(Vector3f min, Vector3f max) {
        setMinMax(min, max);
    }

    @Override
    public Type getType() {
        return Type.AABB;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Box from a given
     * set of points. It uses the <code>containAABB</code> method as default.
     *
     * @param points
     *            the points to contain.
     */
    @Override
    public void computeFromPoints(FloatBuffer points) {
        containAABB(points);
    }

    /**
     * <code>computeFromTris</code> creates a new Bounding Box from a given
     * set of triangles. It is used in OBBTree calculations.
     *
     * @param tris triangle data (unaffected)
     * @param start the index of the first triangle to be used
     * @param end the index of the triangle after the last one to be used
     */
    public void computeFromTris(Triangle[] tris, int start, int end) {
        if (end - start <= 0) {
            return;
        }

        TempVars vars = TempVars.get();

        Vector3f min = vars.vect1.set(new Vector3f(Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        Vector3f max = vars.vect2.set(new Vector3f(Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));

        Vector3f point;
        for (int i = start; i < end; i++) {
            point = tris[i].get(0);
            checkMinMax(min, max, point);
            point = tris[i].get(1);
            checkMinMax(min, max, point);
            point = tris[i].get(2);
            checkMinMax(min, max, point);
        }

        center.set(min.addLocal(max));
        center.multLocal(0.5f);

        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;

        vars.release();
    }

    public void computeFromTris(int[] indices, Mesh mesh, int start, int end) {
        if (end - start <= 0) {
            return;
        }

        TempVars vars = TempVars.get();

        Vector3f vect1 = vars.vect1;
        Vector3f vect2 = vars.vect2;
        Triangle triangle = vars.triangle;

        Vector3f min = vect1.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = vect2.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f point;

        for (int i = start; i < end; i++) {
            mesh.getTriangle(indices[i], triangle);
            point = triangle.get(0);
            checkMinMax(min, max, point);
            point = triangle.get(1);
            checkMinMax(min, max, point);
            point = triangle.get(2);
            checkMinMax(min, max, point);
        }

        center.set(min.addLocal(max));
        center.multLocal(0.5f);

        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;

        vars.release();
    }

    public static void checkMinMax(Vector3f min, Vector3f max, Vector3f point) {
        if (point.x < min.x) {
            min.x = point.x;
        }
        if (point.x > max.x) {
            max.x = point.x;
        }
        if (point.y < min.y) {
            min.y = point.y;
        }
        if (point.y > max.y) {
            max.y = point.y;
        }
        if (point.z < min.z) {
            min.z = point.z;
        }
        if (point.z > max.z) {
            max.z = point.z;
        }
    }

    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned bounding
     * box of the points, then selects the smallest enclosing sphere of the box
     * with the sphere centered at the boxes center.
     *
     * @param points
     *            the list of points.
     */
    public void containAABB(FloatBuffer points) {
        if (points == null) {
            return;
        }

        points.rewind();
        if (points.remaining() <= 2) // we need at least a 3 float vector
        {
            return;
        }

        TempVars vars = TempVars.get();

        float[] tmpArray = vars.skinPositions;

        float minX = Float.POSITIVE_INFINITY,
                minY = Float.POSITIVE_INFINITY,
                minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY,
                maxY = Float.NEGATIVE_INFINITY,
                maxZ = Float.NEGATIVE_INFINITY;

        int iterations = (int) FastMath.ceil(points.limit() / ((float) tmpArray.length));
        for (int i = iterations - 1; i >= 0; i--) {
            int bufLength = Math.min(tmpArray.length, points.remaining());
            points.get(tmpArray, 0, bufLength);

            for (int j = 0; j < bufLength; j += 3) {
                vars.vect1.x = tmpArray[j];
                vars.vect1.y = tmpArray[j + 1];
                vars.vect1.z = tmpArray[j + 2];

                if (vars.vect1.x < minX) {
                    minX = vars.vect1.x;
                }
                if (vars.vect1.x > maxX) {
                    maxX = vars.vect1.x;
                }

                if (vars.vect1.y < minY) {
                    minY = vars.vect1.y;
                }
                if (vars.vect1.y > maxY) {
                    maxY = vars.vect1.y;
                }

                if (vars.vect1.z < minZ) {
                    minZ = vars.vect1.z;
                }
                if (vars.vect1.z > maxZ) {
                    maxZ = vars.vect1.z;
                }
            }
        }

        vars.release();

        center.set(minX + maxX, minY + maxY, minZ + maxZ);
        center.multLocal(0.5f);

        xExtent = maxX - center.x;
        yExtent = maxY - center.y;
        zExtent = maxZ - center.z;
    }

    /**
     * <code>transform</code> modifies the center of the box to reflect the
     * change made via a rotation, translation and scale.
     *
     * @param trans
     *            the transform to apply
     * @param store
     *            box to store result in
     */
    @Override
    public BoundingVolume transform(Transform trans, BoundingVolume store) {

        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        center.mult(trans.getScale(), box.center);
        trans.getRotation().mult(box.center, box.center);
        box.center.addLocal(trans.getTranslation());

        TempVars vars = TempVars.get();

        Matrix3f transMatrix = vars.tempMat3;
        transMatrix.set(trans.getRotation());
        // Make the rotation matrix all positive to get the maximum x/y/z extent
        transMatrix.absoluteLocal();

        Vector3f scale = trans.getScale();
        vars.vect1.set(xExtent * FastMath.abs(scale.x),
                yExtent * FastMath.abs(scale.y),
                zExtent * FastMath.abs(scale.z));
        transMatrix.mult(vars.vect1, vars.vect2);
        // Assign the biggest rotations after scales.
        box.xExtent = FastMath.abs(vars.vect2.getX());
        box.yExtent = FastMath.abs(vars.vect2.getY());
        box.zExtent = FastMath.abs(vars.vect2.getZ());

        vars.release();

        return box;
    }

    @Override
    public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }
        TempVars vars = TempVars.get();

        float w = trans.multProj(center, box.center);
        box.center.divideLocal(w);

        Matrix3f transMatrix = vars.tempMat3;
        trans.toRotationMatrix(transMatrix);

        // Make the rotation matrix all positive to get the maximum x/y/z extent
        transMatrix.absoluteLocal();

        vars.vect1.set(xExtent, yExtent, zExtent);
        transMatrix.mult(vars.vect1, vars.vect1);

        // Assign the biggest rotations after scales.
        box.xExtent = FastMath.abs(vars.vect1.getX());
        box.yExtent = FastMath.abs(vars.vect1.getY());
        box.zExtent = FastMath.abs(vars.vect1.getZ());

        vars.release();

        return box;
    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view
     * frustum) to determine which side this bound is on.
     *
     * @param plane
     *            the plane to check against.
     */
    @Override
    public Plane.Side whichSide(Plane plane) {
        float radius = FastMath.abs(xExtent * plane.getNormal().getX())
                + FastMath.abs(yExtent * plane.getNormal().getY())
                + FastMath.abs(zExtent * plane.getNormal().getZ());

        float distance = plane.pseudoDistance(center);

        //changed to < and > to prevent floating point precision problems
        if (distance < -radius) {
            return Plane.Side.Negative;
        } else if (distance > radius) {
            return Plane.Side.Positive;
        } else {
            return Plane.Side.None;
        }
    }

    /**
     * <code>merge</code> combines this bounding box locally with a second
     * bounding volume. The result contains both the original box and the second
     * volume.
     *
     * @param volume the bounding volume to combine with this box (or null) (not
     * altered)
     * @return this box (with its components modified) or null if the second
     * volume is of some type other than AABB or Sphere
     */
    @Override
    public BoundingVolume merge(BoundingVolume volume) {
        return mergeLocal(volume);
    }

    /**
     * <code>mergeLocal</code> combines this bounding box locally with a second
     * bounding volume. The result contains both the original box and the second
     * volume.
     *
     * @param volume the bounding volume to combine with this box (or null) (not
     * altered)
     * @return this box (with its components modified) or null if the second
     * volume is of some type other than AABB or Sphere
     */
    @Override
    public BoundingVolume mergeLocal(BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case AABB:
                BoundingBox vBox = (BoundingBox) volume;
                return mergeLocal(vBox.center, vBox.xExtent, vBox.yExtent,
                        vBox.zExtent);

            case Sphere:
                BoundingSphere vSphere = (BoundingSphere) volume;
                return mergeLocal(vSphere.center, vSphere.radius,
                        vSphere.radius, vSphere.radius);

//            case OBB: {
//                return mergeOBB((OrientedBoundingBox) volume);
//            }
            default:
                return null;
        }
    }

    /*
     * Merges this AABB with the given OBB.
     *
     * @param volume
     *            the OBB to merge this AABB with.
     * @return This AABB extended to fit the given OBB.
     */
//    private BoundingBox mergeOBB(OrientedBoundingBox volume) {
//        if (!volume.correctCorners)
//            volume.computeCorners();
//
//        TempVars vars = TempVars.get();
//        Vector3f min = vars.compVect1.set(center.x - xExtent, center.y - yExtent,
//                center.z - zExtent);
//        Vector3f max = vars.compVect2.set(center.x + xExtent, center.y + yExtent,
//                center.z + zExtent);
//
//        for (int i = 1; i < volume.vectorStore.length; i++) {
//            Vector3f temp = volume.vectorStore[i];
//            if (temp.x < min.x)
//                min.x = temp.x;
//            else if (temp.x > max.x)
//                max.x = temp.x;
//
//            if (temp.y < min.y)
//                min.y = temp.y;
//            else if (temp.y > max.y)
//                max.y = temp.y;
//
//            if (temp.z < min.z)
//                min.z = temp.z;
//            else if (temp.z > max.z)
//                max.z = temp.z;
//        }
//
//        center.set(min.addLocal(max));
//        center.multLocal(0.5f);
//
//        xExtent = max.x - center.x;
//        yExtent = max.y - center.y;
//        zExtent = max.z - center.z;
//        return this;
//    }

    /**
     * <code>mergeLocal</code> combines this bounding box locally with a second
     * bounding box described by its center and extents.
     *
     * @param c the center of the second box (not null, not altered)
     * @param x the X-extent of the second box
     * @param y the Y-extent of the second box
     * @param z the Z-extent of the second box
     * @return the resulting merged box.
     */
    private BoundingBox mergeLocal(Vector3f c, float x, float y, float z) {
        if (xExtent == Float.POSITIVE_INFINITY
                || x == Float.POSITIVE_INFINITY) {
            center.x = 0;
            xExtent = Float.POSITIVE_INFINITY;
        } else {
            float low = center.x - xExtent;
            if (low > c.x - x) {
                low = c.x - x;
            }
            float high = center.x + xExtent;
            if (high < c.x + x) {
                high = c.x + x;
            }
            center.x = (low + high) / 2;
            xExtent = high - center.x;
        }

        if (yExtent == Float.POSITIVE_INFINITY
                || y == Float.POSITIVE_INFINITY) {
            center.y = 0;
            yExtent = Float.POSITIVE_INFINITY;
        } else {
            float low = center.y - yExtent;
            if (low > c.y - y) {
                low = c.y - y;
            }
            float high = center.y + yExtent;
            if (high < c.y + y) {
                high = c.y + y;
            }
            center.y = (low + high) / 2;
            yExtent = high - center.y;
        }

        if (zExtent == Float.POSITIVE_INFINITY
                || z == Float.POSITIVE_INFINITY) {
            center.z = 0;
            zExtent = Float.POSITIVE_INFINITY;
        } else {
            float low = center.z - zExtent;
            if (low > c.z - z) {
                low = c.z - z;
            }
            float high = center.z + zExtent;
            if (high < c.z + z) {
                high = c.z + z;
            }
            center.z = (low + high) / 2;
            zExtent = high - center.z;
        }

        return this;
    }

    /**
     * <code>clone</code> creates a new BoundingBox object containing the same
     * data as this one.
     *
     * @param store
     *            where to store the cloned information. if null or wrong class,
     *            a new store is created.
     * @return the new BoundingBox
     */
    @Override
    public BoundingVolume clone(BoundingVolume store) {
        if (store != null && store.getType() == Type.AABB) {
            BoundingBox rVal = (BoundingBox) store;
            rVal.center.set(center);
            rVal.xExtent = xExtent;
            rVal.yExtent = yExtent;
            rVal.zExtent = zExtent;
            rVal.checkPlane = checkPlane;
            return rVal;
        }

        BoundingBox rVal = new BoundingBox(center.clone(),
                xExtent, yExtent, zExtent);
        return rVal;
    }

    /**
     * <code>toString</code> returns the string representation of this object.
     * The form is: "[Center: vector xExtent: X.XX yExtent: Y.YY zExtent:
     * Z.ZZ]".
     *
     * @return the string representation of this.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Center: " + center + "  xExtent: "
                + xExtent + "  yExtent: " + yExtent + "  zExtent: " + zExtent
                + "]";
    }

    /**
     * intersects determines if this Bounding Box intersects with another given
     * bounding volume. If so, true is returned, otherwise, false is returned.
     *
     * @see BoundingVolume#intersects(com.jme3.bounding.BoundingVolume)
     */
    @Override
    public boolean intersects(BoundingVolume bv) {
        return bv.intersectsBoundingBox(this);
    }

    /**
     * determines if this bounding box intersects a given bounding sphere.
     *
     * @see BoundingVolume#intersectsSphere(com.jme3.bounding.BoundingSphere)
     */
    @Override
    public boolean intersectsSphere(BoundingSphere bs) {
        return bs.intersectsBoundingBox(this);
    }

    /**
     * determines if this bounding box intersects a given bounding box. If the
     * two boxes intersect in any way, true is returned. Otherwise, false is
     * returned.
     *
     * @see BoundingVolume#intersectsBoundingBox(com.jme3.bounding.BoundingBox)
     */
    @Override
    public boolean intersectsBoundingBox(BoundingBox bb) {
        assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bb.center);

        if (center.x + xExtent < bb.center.x - bb.xExtent
                || center.x - xExtent > bb.center.x + bb.xExtent) {
            return false;
        } else if (center.y + yExtent < bb.center.y - bb.yExtent
                || center.y - yExtent > bb.center.y + bb.yExtent) {
            return false;
        } else if (center.z + zExtent < bb.center.z - bb.zExtent
                || center.z - zExtent > bb.center.z + bb.zExtent) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * determines if this bounding box intersects with a given oriented bounding
     * box.
     *
     * @see com.jme.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.jme.bounding.OrientedBoundingBox)
     */
//    public boolean intersectsOrientedBoundingBox(OrientedBoundingBox obb) {
//        return obb.intersectsBoundingBox(this);
//    }
    /**
     * determines if this bounding box intersects with a given ray object. If an
     * intersection has occurred, true is returned, otherwise false is returned.
     *
     * @see BoundingVolume#intersects(com.jme3.math.Ray)
     */
    @Override
    public boolean intersects(Ray ray) {
        assert Vector3f.isValidVector(center);

        float rhs;

        TempVars vars = TempVars.get();

        Vector3f diff = ray.origin.subtract(getCenter(vars.vect2), vars.vect1);

        final float[] fWdU = vars.fWdU;
        final float[] fAWdU = vars.fAWdU;
        final float[] fDdU = vars.fDdU;
        final float[] fADdU = vars.fADdU;
        final float[] fAWxDdU = vars.fAWxDdU;

        fWdU[0] = ray.getDirection().dot(Vector3f.UNIT_X);
        fAWdU[0] = FastMath.abs(fWdU[0]);
        fDdU[0] = diff.dot(Vector3f.UNIT_X);
        fADdU[0] = FastMath.abs(fDdU[0]);
        if (fADdU[0] > xExtent && fDdU[0] * fWdU[0] >= 0.0) {
            vars.release();
            return false;
        }

        fWdU[1] = ray.getDirection().dot(Vector3f.UNIT_Y);
        fAWdU[1] = FastMath.abs(fWdU[1]);
        fDdU[1] = diff.dot(Vector3f.UNIT_Y);
        fADdU[1] = FastMath.abs(fDdU[1]);
        if (fADdU[1] > yExtent && fDdU[1] * fWdU[1] >= 0.0) {
            vars.release();
            return false;
        }

        fWdU[2] = ray.getDirection().dot(Vector3f.UNIT_Z);
        fAWdU[2] = FastMath.abs(fWdU[2]);
        fDdU[2] = diff.dot(Vector3f.UNIT_Z);
        fADdU[2] = FastMath.abs(fDdU[2]);
        if (fADdU[2] > zExtent && fDdU[2] * fWdU[2] >= 0.0) {
            vars.release();
            return false;
        }

        Vector3f wCrossD = ray.getDirection().cross(diff, vars.vect2);

        fAWxDdU[0] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_X));
        rhs = yExtent * fAWdU[2] + zExtent * fAWdU[1];
        if (fAWxDdU[0] > rhs) {
            vars.release();
            return false;
        }

        fAWxDdU[1] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Y));
        rhs = xExtent * fAWdU[2] + zExtent * fAWdU[0];
        if (fAWxDdU[1] > rhs) {
            vars.release();
            return false;
        }

        fAWxDdU[2] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Z));
        rhs = xExtent * fAWdU[1] + yExtent * fAWdU[0];
        if (fAWxDdU[2] > rhs) {
            vars.release();
            return false;
        }

        vars.release();
        return true;
    }

    /**
     * @see com.jme3.bounding.BoundingVolume#intersects(com.jme3.math.Ray)
     */
    private int collideWithRay(Ray ray, CollisionResults results) {
        TempVars vars = TempVars.get();
        try {
            Vector3f diff = vars.vect1.set(ray.origin).subtractLocal(center);
            Vector3f direction = vars.vect2.set(ray.direction);

            //float[] t = {0f, Float.POSITIVE_INFINITY};
            float[] t = vars.fWdU; // use one of the tempvars arrays
            t[0] = 0;
            t[1] = Float.POSITIVE_INFINITY;

            float saveT0 = t[0], saveT1 = t[1];
            boolean notEntirelyClipped = clip(+direction.x, -diff.x - xExtent, t)
                    && clip(-direction.x, +diff.x - xExtent, t)
                    && clip(+direction.y, -diff.y - yExtent, t)
                    && clip(-direction.y, +diff.y - yExtent, t)
                    && clip(+direction.z, -diff.z - zExtent, t)
                    && clip(-direction.z, +diff.z - zExtent, t);

            if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
                if (t[1] > t[0]) {
                    float[] distances = t;
                    Vector3f point0 = new Vector3f(ray.direction).multLocal(distances[0]).addLocal(ray.origin);
                    Vector3f point1 = new Vector3f(ray.direction).multLocal(distances[1]).addLocal(ray.origin);

                    CollisionResult result = new CollisionResult(point0, distances[0]);
                    results.addCollision(result);
                    result = new CollisionResult(point1, distances[1]);
                    results.addCollision(result);
                    return 2;
                }

                Vector3f point = new Vector3f(ray.direction).multLocal(t[0]).addLocal(ray.origin);
                CollisionResult result = new CollisionResult(point, t[0]);
                results.addCollision(result);
                return 1;
            }
            return 0;
        } finally {
            vars.release();
        }
    }

    private int collideWithRay(Ray ray) {
        TempVars vars = TempVars.get();
        try {
            Vector3f diff = vars.vect1.set(ray.origin).subtractLocal(center);
            Vector3f direction = vars.vect2.set(ray.direction);

            //float[] t = {0f, Float.POSITIVE_INFINITY};
            float[] t = vars.fWdU; // use one of the tempvars arrays
            t[0] = 0;
            t[1] = Float.POSITIVE_INFINITY;

            float saveT0 = t[0], saveT1 = t[1];
            boolean notEntirelyClipped = clip(+direction.x, -diff.x - xExtent, t)
                    && clip(-direction.x, +diff.x - xExtent, t)
                    && clip(+direction.y, -diff.y - yExtent, t)
                    && clip(-direction.y, +diff.y - yExtent, t)
                    && clip(+direction.z, -diff.z - zExtent, t)
                    && clip(-direction.z, +diff.z - zExtent, t);

            if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
                if (t[1] > t[0]) {
                    return 2;
                } else {
                    return 1;
                }
            }
            return 0;
        } finally {
            vars.release();
        }
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        if (other instanceof Ray) {
            Ray ray = (Ray) other;
            return collideWithRay(ray, results);
        } else if (other instanceof Triangle) {
            Triangle t = (Triangle) other;
            if (intersects(t.get1(), t.get2(), t.get3())) {
                CollisionResult r = new CollisionResult();
                results.addCollision(r);
                return 1;
            }
            return 0;
        } else if (other instanceof BoundingVolume) {
            if (intersects((BoundingVolume) other)) {
                CollisionResult r = new CollisionResult();
                results.addCollision(r);
                return 1;
            }
            return 0;
        } else if (other instanceof Spatial) {
            return other.collideWith(this, results);
        } else {
            throw new UnsupportedCollisionException("With: " + other.getClass().getSimpleName());
        }
    }

    @Override
    public int collideWith(Collidable other) {
        if (other instanceof Ray) {
            Ray ray = (Ray) other;
            return collideWithRay(ray);
        } else if (other instanceof Triangle) {
            Triangle t = (Triangle) other;
            if (intersects(t.get1(), t.get2(), t.get3())) {
                return 1;
            }
            return 0;
        } else if (other instanceof BoundingVolume) {
            return intersects((BoundingVolume) other) ? 1 : 0;
        } else {
            throw new UnsupportedCollisionException("With: " + other.getClass().getSimpleName());
        }
    }

    /**
     * C code ported from <a href="http://www.cs.lth.se/home/Tomas_Akenine_Moller/code/tribox3.txt">
     * http://www.cs.lth.se/home/Tomas_Akenine_Moller/code/tribox3.txt</a>
     *
     * @param v1 The first point in the triangle
     * @param v2 The second point in the triangle
     * @param v3 The third point in the triangle
     * @return True if the bounding box intersects the triangle, false
     * otherwise.
     */
    public boolean intersects(Vector3f v1, Vector3f v2, Vector3f v3) {
        return Intersection.intersect(this, v1, v2, v3);
    }

    @Override
    public boolean contains(Vector3f point) {
        return FastMath.abs(center.x - point.x) < xExtent
                && FastMath.abs(center.y - point.y) < yExtent
                && FastMath.abs(center.z - point.z) < zExtent;
    }

    @Override
    public boolean intersects(Vector3f point) {
        return FastMath.abs(center.x - point.x) <= xExtent
                && FastMath.abs(center.y - point.y) <= yExtent
                && FastMath.abs(center.z - point.z) <= zExtent;
    }

    @Override
    public float distanceToEdge(Vector3f point) {
        // compute coordinates of point in box coordinate system
        TempVars vars = TempVars.get();
        Vector3f closest = vars.vect1;

        point.subtract(center, closest);

        // project test point onto box
        float sqrDistance = 0.0f;
        float delta;

        if (closest.x < -xExtent) {
            delta = closest.x + xExtent;
            sqrDistance += delta * delta;
            closest.x = -xExtent;
        } else if (closest.x > xExtent) {
            delta = closest.x - xExtent;
            sqrDistance += delta * delta;
            closest.x = xExtent;
        }

        if (closest.y < -yExtent) {
            delta = closest.y + yExtent;
            sqrDistance += delta * delta;
            closest.y = -yExtent;
        } else if (closest.y > yExtent) {
            delta = closest.y - yExtent;
            sqrDistance += delta * delta;
            closest.y = yExtent;
        }

        if (closest.z < -zExtent) {
            delta = closest.z + zExtent;
            sqrDistance += delta * delta;
            closest.z = -zExtent;
        } else if (closest.z > zExtent) {
            delta = closest.z - zExtent;
            sqrDistance += delta * delta;
            closest.z = zExtent;
        }

        vars.release();
        return FastMath.sqrt(sqrDistance);
    }

    /**
     * <code>clip</code> determines if a line segment intersects the current
     * test plane.
     *
     * @param denom
     *            the denominator of the line segment.
     * @param numerator
     *            the numerator of the line segment.
     * @param t
     *            test values of the plane.
     * @return true if the line segment intersects the plane, false otherwise.
     */
    private boolean clip(float denom, float numerator, float[] t) {
        // Return value is 'true' if line segment intersects the current test
        // plane. Otherwise, 'false' is returned, in which case the line segment
        // is entirely clipped.
        if (denom > 0.0f) {
            // This is the old if statement...
            // if (numerator > denom * t[1]) {
            //
            // The problem is that what is actually stored is
            // numerator/denom.  In non-floating point, this math should
            // work out the same but in floating point there can
            // be subtle math errors.  The multiply will exaggerate
            // errors that may have been introduced when the value
            // was originally divided.
            //
            // This is especially true when the bounding box has zero
            // extents in some plane because the error rate is critical.
            // comparing a to b * c is not the same as comparing a/b to c
            // in this case.  In fact, I tried converting this method to
            // double and the and the error was in the last decimal place.
            //
            // So, instead, we now compare the divided version to the divided
            // version.  We lose some slight performance here as divide
            // will be more expensive than the divide.  Some microbenchmarks
            // show divide to be 3x slower than multiple on Java 1.6.
            // BUT... we also saved a multiply in the non-clipped case because
            // we can reuse the divided version in both if checks.
            // I think it's better to be right in this case.
            //
            // Bug that I'm fixing: rays going right through quads at certain
            // angles and distances because they fail the bounding box test.
            // Many Bothans died bring you this fix.
            //    -pspeed
            float newT = numerator / denom;
            if (newT > t[1]) {
                return false;
            }
            if (newT > t[0]) {
                t[0] = newT;
            }
            return true;
        } else if (denom < 0.0f) {
            // Old if statement... see above
            // if (numerator > denom * t[0]) {
            //
            // Note though that denom is always negative in this block.
            // When we move it over to the other side we have to flip
            // the comparison.  Algebra for the win.
            float newT = numerator / denom;
            if (newT < t[0]) {
                return false;
            }
            if (newT < t[1]) {
                t[1] = newT;
            }
            return true;
        } else {
            return numerator <= 0.0;
        }
    }

    /**
     * Query extent.
     *
     * @param store
     *            where extent gets stored - null to return a new vector
     * @return store / new vector
     */
    public Vector3f getExtent(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(xExtent, yExtent, zExtent);
        return store;
    }

    /**
     * Determine the X-axis distance between the center and the boundary.
     *
     * @return the distance
     */
    public float getXExtent() {
        return xExtent;
    }

    /**
     * Determine the Y-axis distance between the center and the boundary.
     *
     * @return the distance
     */
    public float getYExtent() {
        return yExtent;
    }

    /**
     * Determine the Z-axis distance between the center and the boundary.
     *
     * @return the distance
     */
    public float getZExtent() {
        return zExtent;
    }

    /**
     * Alter the X-axis distance between the center and the boundary.
     *
     * @param xExtent the desired distance (&ge;0)
     */
    public void setXExtent(float xExtent) {
        if (xExtent < 0) {
            throw new IllegalArgumentException();
        }

        this.xExtent = xExtent;
    }

    /**
     * Alter the Y-axis distance between the center and the boundary.
     *
     * @param yExtent the desired distance (&ge;0)
     */
    public void setYExtent(float yExtent) {
        if (yExtent < 0) {
            throw new IllegalArgumentException();
        }

        this.yExtent = yExtent;
    }

    /**
     * Alter the Z-axis distance between the center and the boundary.
     *
     * @param zExtent the desired distance (&ge;0)
     */
    public void setZExtent(float zExtent) {
        if (zExtent < 0) {
            throw new IllegalArgumentException();
        }

        this.zExtent = zExtent;
    }

    /**
     * Determine the minimum coordinate value for each axis.
     *
     * @param store storage for the result (modified if not null)
     * @return either storeResult or a new vector
     */
    public Vector3f getMin(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(center).subtractLocal(xExtent, yExtent, zExtent);
        return store;
    }

    /**
     * Determine the maximum coordinate value for each axis.
     *
     * @param store storage for the result (modified if not null)
     * @return either storeResult or a new vector
     */
    public Vector3f getMax(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(center).addLocal(xExtent, yExtent, zExtent);
        return store;
    }

    /**
     * Reconfigure with the specified extremes.
     *
     * @param min the desired minimum coordinate value for each axis (not null,
     * not altered)
     * @param max the desired maximum coordinate value for each axis (not null,
     * not altered)
     */
    public void setMinMax(Vector3f min, Vector3f max) {
        this.center.set(max).addLocal(min).multLocal(0.5f);
        xExtent = FastMath.abs(max.x - center.x);
        yExtent = FastMath.abs(max.y - center.y);
        zExtent = FastMath.abs(max.z - center.z);
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
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        xExtent = capsule.readFloat("xExtent", 0);
        yExtent = capsule.readFloat("yExtent", 0);
        zExtent = capsule.readFloat("zExtent", 0);
    }

    @Override
    public float getVolume() {
        return (8 * xExtent * yExtent * zExtent);
    }
}
