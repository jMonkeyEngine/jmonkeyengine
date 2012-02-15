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
import java.io.IOException;
import java.util.logging.Logger;

/**
 * <code>Plane</code> defines a plane where Normal dot (x,y,z) = Constant.
 * This provides methods for calculating a "distance" of a point from this
 * plane. The distance is pseudo due to the fact that it can be negative if the
 * point is on the non-normal side of the plane.
 * 
 * @author Mark Powell
 * @author Joshua Slack
 */
public class Plane implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = Logger
            .getLogger(Plane.class.getName());

    public static enum Side {
        None,
        Positive,
        Negative
    }

    /** 
     * Vector normal to the plane.
     */
    protected Vector3f normal = new Vector3f();

    /** 
     * Constant of the plane. See formula in class definition.
     */
    protected float constant;

    /**
     * Constructor instantiates a new <code>Plane</code> object. This is the
     * default object and contains a normal of (0,0,0) and a constant of 0.
     */
    public Plane() {
    }

    /**
     * Constructor instantiates a new <code>Plane</code> object. The normal
     * and constant values are set at creation.
     * 
     * @param normal
     *            the normal of the plane.
     * @param constant
     *            the constant of the plane.
     */
    public Plane(Vector3f normal, float constant) {
        if (normal == null) {
            throw new IllegalArgumentException("normal cannot be null");
        }

        this.normal.set(normal);
        this.constant = constant;
    }

    /**
     * <code>setNormal</code> sets the normal of the plane.
     * 
     * @param normal
     *            the new normal of the plane.
     */
    public void setNormal(Vector3f normal) {
        if (normal == null) {
            throw new IllegalArgumentException("normal cannot be null");
        }
        this.normal.set(normal);
    }

    /**
     * <code>setNormal</code> sets the normal of the plane.
     *
     */
    public void setNormal(float x, float y, float z) {
        this.normal.set(x,y,z);
    }

    /**
     * <code>getNormal</code> retrieves the normal of the plane.
     * 
     * @return the normal of the plane.
     */
    public Vector3f getNormal() {
        return normal;
    }

    /**
     * <code>setConstant</code> sets the constant value that helps define the
     * plane.
     * 
     * @param constant
     *            the new constant value.
     */
    public void setConstant(float constant) {
        this.constant = constant;
    }

    /**
     * <code>getConstant</code> returns the constant of the plane.
     * 
     * @return the constant of the plane.
     */
    public float getConstant() {
        return constant;
    }

    public Vector3f getClosestPoint(Vector3f point, Vector3f store){
//        float t = constant - normal.dot(point);
//        return store.set(normal).multLocal(t).addLocal(point);
        float t = (constant - normal.dot(point)) / normal.dot(normal);
        return store.set(normal).multLocal(t).addLocal(point);
    }

    public Vector3f getClosestPoint(Vector3f point){
        return getClosestPoint(point, new Vector3f());
    }

    public Vector3f reflect(Vector3f point, Vector3f store){
        if (store == null)
            store = new Vector3f();

        float d = pseudoDistance(point);
        store.set(normal).negateLocal().multLocal(d * 2f);
        store.addLocal(point);
        return store;
    }

    /**
     * <code>pseudoDistance</code> calculates the distance from this plane to
     * a provided point. If the point is on the negative side of the plane the
     * distance returned is negative, otherwise it is positive. If the point is
     * on the plane, it is zero.
     * 
     * @param point
     *            the point to check.
     * @return the signed distance from the plane to a point.
     */
    public float pseudoDistance(Vector3f point) {
        return normal.dot(point) - constant;
    }

    /**
     * <code>whichSide</code> returns the side at which a point lies on the
     * plane. The positive values returned are: NEGATIVE_SIDE, POSITIVE_SIDE and
     * NO_SIDE.
     * 
     * @param point
     *            the point to check.
     * @return the side at which the point lies.
     */
    public Side whichSide(Vector3f point) {
        float dis = pseudoDistance(point);
        if (dis < 0) {
            return Side.Negative;
        } else if (dis > 0) {
            return Side.Positive;
        } else {
            return Side.None;
        }
    }

    public boolean isOnPlane(Vector3f point){
        float dist = pseudoDistance(point);
        if (dist < FastMath.FLT_EPSILON && dist > -FastMath.FLT_EPSILON)
            return true;
        else
            return false;
    }

    /**
     * Initialize this plane using the three points of the given triangle.
     * 
     * @param t
     *            the triangle
     */
    public void setPlanePoints(AbstractTriangle t) {
        setPlanePoints(t.get1(), t.get2(), t.get3());
    }

    /**
     * Initialize this plane using a point of origin and a normal.
     *
     * @param origin
     * @param normal
     */
    public void setOriginNormal(Vector3f origin, Vector3f normal){
        this.normal.set(normal);
        this.constant = normal.x * origin.x + normal.y * origin.y + normal.z * origin.z;
    }

    /**
     * Initialize the Plane using the given 3 points as coplanar.
     * 
     * @param v1
     *            the first point
     * @param v2
     *            the second point
     * @param v3
     *            the third point
     */
    public void setPlanePoints(Vector3f v1, Vector3f v2, Vector3f v3) {
        normal.set(v2).subtractLocal(v1);
        normal.crossLocal(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z)
                .normalizeLocal();
        constant = normal.dot(v1);
    }

    /**
     * <code>toString</code> returns a string thta represents the string
     * representation of this plane. It represents the normal as a
     * <code>Vector3f</code> object, so the format is the following:
     * com.jme.math.Plane [Normal: org.jme.math.Vector3f [X=XX.XXXX, Y=YY.YYYY,
     * Z=ZZ.ZZZZ] - Constant: CC.CCCCC]
     * 
     * @return the string representation of this plane.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Normal: " + normal + " - Constant: "
                + constant + "]";
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(normal, "normal", Vector3f.ZERO);
        capsule.write(constant, "constant", 0);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        normal = (Vector3f) capsule.readSavable("normal", Vector3f.ZERO.clone());
        constant = capsule.readFloat("constant", 0);
    }

    @Override
    public Plane clone() {
        try {
            Plane p = (Plane) super.clone();
            p.normal = normal.clone();
            return p;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
