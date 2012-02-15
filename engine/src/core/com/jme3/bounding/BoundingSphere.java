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

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.*;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>BoundingSphere</code> defines a sphere that defines a container for a
 * group of vertices of a particular piece of geometry. This sphere defines a
 * radius and a center. <br>
 * <br>
 * A typical usage is to allow the class define the center and radius by calling
 * either <code>containAABB</code> or <code>averagePoints</code>. A call to
 * <code>computeFramePoint</code> in turn calls <code>containAABB</code>.
 *
 * @author Mark Powell
 * @version $Id: BoundingSphere.java,v 1.59 2007/08/17 10:34:26 rherlitz Exp $
 */
public class BoundingSphere extends BoundingVolume {

    private static final Logger logger =
            Logger.getLogger(BoundingSphere.class.getName());
    float radius;
    private static final float RADIUS_EPSILON = 1f + 0.00001f;

    /**
     * Default contstructor instantiates a new <code>BoundingSphere</code>
     * object.
     */
    public BoundingSphere() {
    }

    /**
     * Constructor instantiates a new <code>BoundingSphere</code> object.
     *
     * @param r
     *            the radius of the sphere.
     * @param c
     *            the center of the sphere.
     */
    public BoundingSphere(float r, Vector3f c) {
        this.center.set(c);
        this.radius = r;
    }

    public Type getType() {
        return Type.Sphere;
    }

    /**
     * <code>getRadius</code> returns the radius of the bounding sphere.
     *
     * @return the radius of the bounding sphere.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * <code>setRadius</code> sets the radius of this bounding sphere.
     *
     * @param radius
     *            the new radius of the bounding sphere.
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Sphere from a
     * given set of points. It uses the <code>calcWelzl</code> method as
     * default.
     *
     * @param points
     *            the points to contain.
     */
    public void computeFromPoints(FloatBuffer points) {
        calcWelzl(points);
    }

    /**
     * <code>computeFromTris</code> creates a new Bounding Box from a given
     * set of triangles. It is used in OBBTree calculations.
     *
     * @param tris
     * @param start
     * @param end
     */
    public void computeFromTris(Triangle[] tris, int start, int end) {
        if (end - start <= 0) {
            return;
        }

        Vector3f[] vertList = new Vector3f[(end - start) * 3];

        int count = 0;
        for (int i = start; i < end; i++) {
            vertList[count++] = tris[i].get(0);
            vertList[count++] = tris[i].get(1);
            vertList[count++] = tris[i].get(2);
        }
        averagePoints(vertList);
    }
//
//    /**
//     * <code>computeFromTris</code> creates a new Bounding Box from a given
//     * set of triangles. It is used in OBBTree calculations.
//     *
//	 * @param indices
//	 * @param mesh
//     * @param start
//     * @param end
//     */
//    public void computeFromTris(int[] indices, Mesh mesh, int start, int end) {
//    	if (end - start <= 0) {
//            return;
//        }
//
//    	Vector3f[] vertList = new Vector3f[(end - start) * 3];
//
//        int count = 0;
//        for (int i = start; i < end; i++) {
//        	mesh.getTriangle(indices[i], verts);
//        	vertList[count++] = new Vector3f(verts[0]);
//        	vertList[count++] = new Vector3f(verts[1]);
//        	vertList[count++] = new Vector3f(verts[2]);
//        }
//
//        averagePoints(vertList);
//    }

    /**
     * Calculates a minimum bounding sphere for the set of points. The algorithm
     * was originally found in C++ at
     * <p><a href="http://www.flipcode.com/cgi-bin/msg.cgi?showThread=COTD-SmallestEnclosingSpheres&forum=cotd&id=-1">
     * http://www.flipcode.com/cgi-bin/msg.cgi?showThread=COTD-SmallestEnclosingSpheres&forum=cotd&id=-1</a><br><strong>broken link</strong></p>
     * <p>and translated to java by Cep21</p>
     *
     * @param points
     *            The points to calculate the minimum bounds from.
     */
    public void calcWelzl(FloatBuffer points) {
        if (center == null) {
            center = new Vector3f();
        }
        FloatBuffer buf = BufferUtils.createFloatBuffer(points.limit());
        points.rewind();
        buf.put(points);
        buf.flip();
        recurseMini(buf, buf.limit() / 3, 0, 0);
    }

    /**
     * Used from calcWelzl. This function recurses to calculate a minimum
     * bounding sphere a few points at a time.
     *
     * @param points
     *            The array of points to look through.
     * @param p
     *            The size of the list to be used.
     * @param b
     *            The number of points currently considering to include with the
     *            sphere.
     * @param ap
     *            A variable simulating pointer arithmatic from C++, and offset
     *            in <code>points</code>.
     */
    private void recurseMini(FloatBuffer points, int p, int b, int ap) {
        //TempVars vars = TempVars.get();

        Vector3f tempA = new Vector3f(); //vars.vect1;
        Vector3f tempB = new Vector3f(); //vars.vect2;
        Vector3f tempC = new Vector3f(); //vars.vect3;
        Vector3f tempD = new Vector3f(); //vars.vect4;

        switch (b) {
            case 0:
                this.radius = 0;
                this.center.set(0, 0, 0);
                break;
            case 1:
                this.radius = 1f - RADIUS_EPSILON;
                BufferUtils.populateFromBuffer(center, points, ap - 1);
                break;
            case 2:
                BufferUtils.populateFromBuffer(tempA, points, ap - 1);
                BufferUtils.populateFromBuffer(tempB, points, ap - 2);
                setSphere(tempA, tempB);
                break;
            case 3:
                BufferUtils.populateFromBuffer(tempA, points, ap - 1);
                BufferUtils.populateFromBuffer(tempB, points, ap - 2);
                BufferUtils.populateFromBuffer(tempC, points, ap - 3);
                setSphere(tempA, tempB, tempC);
                break;
            case 4:
                BufferUtils.populateFromBuffer(tempA, points, ap - 1);
                BufferUtils.populateFromBuffer(tempB, points, ap - 2);
                BufferUtils.populateFromBuffer(tempC, points, ap - 3);
                BufferUtils.populateFromBuffer(tempD, points, ap - 4);
                setSphere(tempA, tempB, tempC, tempD);
                //vars.release();
                return;
        }
        for (int i = 0; i < p; i++) {
            BufferUtils.populateFromBuffer(tempA, points, i + ap);
            if (tempA.distanceSquared(center) - (radius * radius) > RADIUS_EPSILON - 1f) {
                for (int j = i; j > 0; j--) {
                    BufferUtils.populateFromBuffer(tempB, points, j + ap);
                    BufferUtils.populateFromBuffer(tempC, points, j - 1 + ap);
                    BufferUtils.setInBuffer(tempC, points, j + ap);
                    BufferUtils.setInBuffer(tempB, points, j - 1 + ap);
                }
                recurseMini(points, i, b + 1, ap + 1);
            }
        }
        //vars.release();
    }

    /**
     * Calculates the minimum bounding sphere of 4 points. Used in welzl's
     * algorithm.
     *
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @param B
     *            The 3rd point inside the sphere.
     * @param C
     *            The 4th point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(Vector3f O, Vector3f A, Vector3f B, Vector3f C) {
        Vector3f a = A.subtract(O);
        Vector3f b = B.subtract(O);
        Vector3f c = C.subtract(O);

        float Denominator = 2.0f * (a.x * (b.y * c.z - c.y * b.z) - b.x
                * (a.y * c.z - c.y * a.z) + c.x * (a.y * b.z - b.y * a.z));
        if (Denominator == 0) {
            center.set(0, 0, 0);
            radius = 0;
        } else {
            Vector3f o = a.cross(b).multLocal(c.lengthSquared()).addLocal(
                    c.cross(a).multLocal(b.lengthSquared())).addLocal(
                    b.cross(c).multLocal(a.lengthSquared())).divideLocal(
                    Denominator);

            radius = o.length() * RADIUS_EPSILON;
            O.add(o, center);
        }
    }

    /**
     * Calculates the minimum bounding sphere of 3 points. Used in welzl's
     * algorithm.
     *
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @param B
     *            The 3rd point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(Vector3f O, Vector3f A, Vector3f B) {
        Vector3f a = A.subtract(O);
        Vector3f b = B.subtract(O);
        Vector3f acrossB = a.cross(b);

        float Denominator = 2.0f * acrossB.dot(acrossB);

        if (Denominator == 0) {
            center.set(0, 0, 0);
            radius = 0;
        } else {

            Vector3f o = acrossB.cross(a).multLocal(b.lengthSquared()).addLocal(b.cross(acrossB).multLocal(a.lengthSquared())).divideLocal(Denominator);
            radius = o.length() * RADIUS_EPSILON;
            O.add(o, center);
        }
    }

    /**
     * Calculates the minimum bounding sphere of 2 points. Used in welzl's
     * algorithm.
     *
     * @param O
     *            The 1st point inside the sphere.
     * @param A
     *            The 2nd point inside the sphere.
     * @see #calcWelzl(java.nio.FloatBuffer)
     */
    private void setSphere(Vector3f O, Vector3f A) {
        radius = FastMath.sqrt(((A.x - O.x) * (A.x - O.x) + (A.y - O.y)
                * (A.y - O.y) + (A.z - O.z) * (A.z - O.z)) / 4f) + RADIUS_EPSILON - 1f;
        center.interpolate(O, A, .5f);
    }

    /**
     * <code>averagePoints</code> selects the sphere center to be the average
     * of the points and the sphere radius to be the smallest value to enclose
     * all points.
     *
     * @param points
     *            the list of points to contain.
     */
    public void averagePoints(Vector3f[] points) {
        logger.info("Bounding Sphere calculated using average points.");
        center = points[0];

        for (int i = 1; i < points.length; i++) {
            center.addLocal(points[i]);
        }

        float quantity = 1.0f / points.length;
        center.multLocal(quantity);

        float maxRadiusSqr = 0;
        for (int i = 0; i < points.length; i++) {
            Vector3f diff = points[i].subtract(center);
            float radiusSqr = diff.lengthSquared();
            if (radiusSqr > maxRadiusSqr) {
                maxRadiusSqr = radiusSqr;
            }
        }

        radius = (float) Math.sqrt(maxRadiusSqr) + RADIUS_EPSILON - 1f;

    }

    /**
     * <code>transform</code> modifies the center of the sphere to reflect the
     * change made via a rotation, translation and scale.
     *
     * @param trans
     *            the transform to apply
     * @param store
     *            sphere to store result in
     * @return BoundingVolume
     * @return ref
     */
    public BoundingVolume transform(Transform trans, BoundingVolume store) {
        BoundingSphere sphere;
        if (store == null || store.getType() != BoundingVolume.Type.Sphere) {
            sphere = new BoundingSphere(1, new Vector3f(0, 0, 0));
        } else {
            sphere = (BoundingSphere) store;
        }

        center.mult(trans.getScale(), sphere.center);
        trans.getRotation().mult(sphere.center, sphere.center);
        sphere.center.addLocal(trans.getTranslation());
        sphere.radius = FastMath.abs(getMaxAxis(trans.getScale()) * radius) + RADIUS_EPSILON - 1f;
        return sphere;
    }

    public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
        BoundingSphere sphere;
        if (store == null || store.getType() != BoundingVolume.Type.Sphere) {
            sphere = new BoundingSphere(1, new Vector3f(0, 0, 0));
        } else {
            sphere = (BoundingSphere) store;
        }

        trans.mult(center, sphere.center);
        Vector3f axes = new Vector3f(1, 1, 1);
        trans.mult(axes, axes);
        float ax = getMaxAxis(axes);
        sphere.radius = FastMath.abs(ax * radius) + RADIUS_EPSILON - 1f;
        return sphere;
    }

    private float getMaxAxis(Vector3f scale) {
        float x = FastMath.abs(scale.x);
        float y = FastMath.abs(scale.y);
        float z = FastMath.abs(scale.z);

        if (x >= y) {
            if (x >= z) {
                return x;
            }
            return z;
        }

        if (y >= z) {
            return y;
        }

        return z;
    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view
     * frustum) to determine which side this bound is on.
     *
     * @param plane
     *            the plane to check against.
     * @return side
     */
    public Plane.Side whichSide(Plane plane) {
        float distance = plane.pseudoDistance(center);

        if (distance <= -radius) {
            return Plane.Side.Negative;
        } else if (distance >= radius) {
            return Plane.Side.Positive;
        } else {
            return Plane.Side.None;
        }
    }

    /**
     * <code>merge</code> combines this sphere with a second bounding sphere.
     * This new sphere contains both bounding spheres and is returned.
     *
     * @param volume
     *            the sphere to combine with this sphere.
     * @return a new sphere
     */
    public BoundingVolume merge(BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {

            case Sphere: {
                BoundingSphere sphere = (BoundingSphere) volume;
                float temp_radius = sphere.getRadius();
                Vector3f temp_center = sphere.center;
                BoundingSphere rVal = new BoundingSphere();
                return merge(temp_radius, temp_center, rVal);
            }

            case AABB: {
                BoundingBox box = (BoundingBox) volume;
                Vector3f radVect = new Vector3f(box.xExtent, box.yExtent,
                        box.zExtent);
                Vector3f temp_center = box.center;
                BoundingSphere rVal = new BoundingSphere();
                return merge(radVect.length(), temp_center, rVal);
            }

//        case OBB: {
//        	OrientedBoundingBox box = (OrientedBoundingBox) volume;
//            BoundingSphere rVal = (BoundingSphere) this.clone(null);
//            return rVal.mergeOBB(box);
//        }

            default:
                return null;

        }
    }

    /**
     * <code>mergeLocal</code> combines this sphere with a second bounding
     * sphere locally. Altering this sphere to contain both the original and the
     * additional sphere volumes;
     *
     * @param volume
     *            the sphere to combine with this sphere.
     * @return this
     */
    public BoundingVolume mergeLocal(BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {

            case Sphere: {
                BoundingSphere sphere = (BoundingSphere) volume;
                float temp_radius = sphere.getRadius();
                Vector3f temp_center = sphere.center;
                return merge(temp_radius, temp_center, this);
            }

            case AABB: {
                BoundingBox box = (BoundingBox) volume;
                TempVars vars = TempVars.get();
                Vector3f radVect = vars.vect1;
                radVect.set(box.xExtent, box.yExtent, box.zExtent);
                Vector3f temp_center = box.center;
                float len = radVect.length();
                vars.release();
                return merge(len, temp_center, this);
            }

//        case OBB: {
//        	return mergeOBB((OrientedBoundingBox) volume);
//        }

            default:
                return null;
        }
    }

//    /**
//     * Merges this sphere with the given OBB.
//     *
//     * @param volume
//     *            The OBB to merge.
//     * @return This sphere, after merging.
//     */
//    private BoundingSphere mergeOBB(OrientedBoundingBox volume) {
//        // compute edge points from the obb
//        if (!volume.correctCorners)
//            volume.computeCorners();
//        _mergeBuf.rewind();
//        for (int i = 0; i < 8; i++) {
//            _mergeBuf.put(volume.vectorStore[i].x);
//            _mergeBuf.put(volume.vectorStore[i].y);
//            _mergeBuf.put(volume.vectorStore[i].z);
//        }
//
//        // remember old radius and center
//        float oldRadius = radius;
//        Vector3f oldCenter = _compVect2.set( center );
//
//        // compute new radius and center from obb points
//        computeFromPoints(_mergeBuf);
//        Vector3f newCenter = _compVect3.set( center );
//        float newRadius = radius;
//
//        // restore old center and radius
//        center.set( oldCenter );
//        radius = oldRadius;
//
//        //merge obb points result
//        merge( newRadius, newCenter, this );
//
//        return this;
//    }
    private BoundingVolume merge(float temp_radius, Vector3f temp_center,
            BoundingSphere rVal) {
        TempVars vars = TempVars.get();

        Vector3f diff = temp_center.subtract(center, vars.vect1);
        float lengthSquared = diff.lengthSquared();
        float radiusDiff = temp_radius - radius;

        float fRDiffSqr = radiusDiff * radiusDiff;

        if (fRDiffSqr >= lengthSquared) {
            if (radiusDiff <= 0.0f) {
                vars.release();
                return this;
            }

            Vector3f rCenter = rVal.center;
            if (rCenter == null) {
                rVal.setCenter(rCenter = new Vector3f());
            }
            rCenter.set(temp_center);
            rVal.setRadius(temp_radius);
            vars.release();
            return rVal;
        }

        float length = (float) Math.sqrt(lengthSquared);

        Vector3f rCenter = rVal.center;
        if (rCenter == null) {
            rVal.setCenter(rCenter = new Vector3f());
        }
        if (length > RADIUS_EPSILON) {
            float coeff = (length + radiusDiff) / (2.0f * length);
            rCenter.set(center.addLocal(diff.multLocal(coeff)));
        } else {
            rCenter.set(center);
        }

        rVal.setRadius(0.5f * (length + radius + temp_radius));
        vars.release();
        return rVal;
    }

    /**
     * <code>clone</code> creates a new BoundingSphere object containing the
     * same data as this one.
     *
     * @param store
     *            where to store the cloned information. if null or wrong class,
     *            a new store is created.
     * @return the new BoundingSphere
     */
    public BoundingVolume clone(BoundingVolume store) {
        if (store != null && store.getType() == Type.Sphere) {
            BoundingSphere rVal = (BoundingSphere) store;
            if (null == rVal.center) {
                rVal.center = new Vector3f();
            }
            rVal.center.set(center);
            rVal.radius = radius;
            rVal.checkPlane = checkPlane;
            return rVal;
        }

        return new BoundingSphere(radius,
                (center != null ? (Vector3f) center.clone() : null));
    }

    /**
     * <code>toString</code> returns the string representation of this object.
     * The form is: "Radius: RRR.SSSS Center: <Vector>".
     *
     * @return the string representation of this.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Radius: " + radius + " Center: "
                + center + "]";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersects(com.jme.bounding.BoundingVolume)
     */
    public boolean intersects(BoundingVolume bv) {
        return bv.intersectsSphere(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersectsSphere(com.jme.bounding.BoundingSphere)
     */
    public boolean intersectsSphere(BoundingSphere bs) {
        assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bs.center);

        TempVars vars = TempVars.get();

        Vector3f diff = center.subtract(bs.center, vars.vect1);
        float rsum = getRadius() + bs.getRadius();
        boolean eq = (diff.dot(diff) <= rsum * rsum);
        vars.release();
        return eq;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersectsBoundingBox(com.jme.bounding.BoundingBox)
     */
    public boolean intersectsBoundingBox(BoundingBox bb) {
        assert Vector3f.isValidVector(center) && Vector3f.isValidVector(bb.center);

        if (FastMath.abs(bb.center.x - center.x) < getRadius()
                + bb.xExtent
                && FastMath.abs(bb.center.y - center.y) < getRadius()
                + bb.yExtent
                && FastMath.abs(bb.center.z - center.z) < getRadius()
                + bb.zExtent) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.jme.bounding.OrientedBoundingBox)
     */
//    public boolean intersectsOrientedBoundingBox(OrientedBoundingBox obb) {
//        return obb.intersectsSphere(this);
//    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersects(com.jme.math.Ray)
     */
    public boolean intersects(Ray ray) {
        assert Vector3f.isValidVector(center);

        TempVars vars = TempVars.get();

        Vector3f diff = vars.vect1.set(ray.getOrigin()).subtractLocal(center);
        float radiusSquared = getRadius() * getRadius();
        float a = diff.dot(diff) - radiusSquared;
        if (a <= 0.0) {
            // in sphere
            return true;
        }

        // outside sphere
        float b = ray.getDirection().dot(diff);
        vars.release();
        if (b >= 0.0) {
            return false;
        }
        return b * b >= a;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jme.bounding.BoundingVolume#intersectsWhere(com.jme.math.Ray)
     */
    private int collideWithRay(Ray ray, CollisionResults results) {
        TempVars vars = TempVars.get();

        Vector3f diff = vars.vect1.set(ray.getOrigin()).subtractLocal(
                center);
        float a = diff.dot(diff) - (getRadius() * getRadius());
        float a1, discr, root;
        if (a <= 0.0) {
            // inside sphere
            a1 = ray.direction.dot(diff);
            discr = (a1 * a1) - a;
            root = FastMath.sqrt(discr);

            float distance = root - a1;
            Vector3f point = new Vector3f(ray.direction).multLocal(distance).addLocal(ray.origin);

            CollisionResult result = new CollisionResult(point, distance);
            results.addCollision(result);
            vars.release();
            return 1;
        }

        a1 = ray.direction.dot(diff);
        vars.release();
        if (a1 >= 0.0) {
            return 0;
        }

        discr = a1 * a1 - a;
        if (discr < 0.0) {
            return 0;
        } else if (discr >= FastMath.ZERO_TOLERANCE) {
            root = FastMath.sqrt(discr);
            float dist = -a1 - root;
            Vector3f point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
            results.addCollision(new CollisionResult(point, dist));

            dist = -a1 + root;
            point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
            results.addCollision(new CollisionResult(point, dist));
            return 2;
        } else {
            float dist = -a1;
            Vector3f point = new Vector3f(ray.direction).multLocal(dist).addLocal(ray.origin);
            results.addCollision(new CollisionResult(point, dist));
            return 1;
        }
    }
    
    public int collideWith(Collidable other, CollisionResults results) {
        if (other instanceof Ray) {
            Ray ray = (Ray) other;
            return collideWithRay(ray, results);
        } else if (other instanceof Triangle){
            Triangle t = (Triangle) other;
            
            float r2 = radius * radius;
            float d1 = center.distanceSquared(t.get1());
            float d2 = center.distanceSquared(t.get2());
            float d3 = center.distanceSquared(t.get3());
            
            if (d1 <= r2 || d2 <= r2 || d3 <= r2) {
                CollisionResult r = new CollisionResult();
                r.setDistance(FastMath.sqrt(Math.min(Math.min(d1, d2), d3)) - radius);
                results.addCollision(r);
                return 1;
            }

            return 0;
        } else {
            throw new UnsupportedCollisionException();
        }
    }

    @Override
    public boolean contains(Vector3f point) {
        return center.distanceSquared(point) < (getRadius() * getRadius());
    }

    @Override
    public boolean intersects(Vector3f point) {
        return center.distanceSquared(point) <= (getRadius() * getRadius());
    }

    public float distanceToEdge(Vector3f point) {
        return center.distance(point) - radius;
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        try {
            e.getCapsule(this).write(radius, "radius", 0);
        } catch (IOException ex) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "write(JMEExporter)", "Exception", ex);
        }
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        try {
            radius = e.getCapsule(this).readFloat("radius", 0);
        } catch (IOException ex) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "read(JMEImporter)", "Exception", ex);
        }
    }

    @Override
    public float getVolume() {
        return 4 * FastMath.ONE_THIRD * FastMath.PI * radius * radius * radius;
    }
}