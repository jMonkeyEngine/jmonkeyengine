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
package com.jme3.scene.plugins.blender.math;

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;

/**
 * <code>DQuaternion</code> defines a single example of a more general class of
 * hypercomplex numbers. DQuaternions extends a rotation in three dimensions to a
 * rotation in four dimensions. This avoids "gimbal lock" and allows for smooth
 * continuous rotation.
 * 
 * <code>DQuaternion</code> is defined by four double point numbers: {x y z w}.
 * 
 * This class's only purpose is to give better accuracy in floating point operations during computations.
 * This is made by copying the original Quaternion class from jme3 core and leaving only required methods and basic computation methods, so that
 * the class is smaller and easier to maintain.
 * Should any other methods be needed, they will be added.
 * 
 * @author Mark Powell
 * @author Joshua Slack
 * @author Marcin Roguski (Kaelthas)
 */
public final class DQuaternion implements Savable, Cloneable, java.io.Serializable {
    private static final long       serialVersionUID = 5009180713885017539L;

    /**
     * Represents the identity quaternion rotation (0, 0, 0, 1).
     */
    public static final DQuaternion IDENTITY         = new DQuaternion();
    public static final DQuaternion DIRECTION_Z      = new DQuaternion();
    public static final DQuaternion ZERO             = new DQuaternion(0, 0, 0, 0);
    protected double                x, y, z, w = 1;

    /**
     * Constructor instantiates a new <code>DQuaternion</code> object
     * initializing all values to zero, except w which is initialized to 1.
     *
     */
    public DQuaternion() {
    }

    /**
     * Constructor instantiates a new <code>DQuaternion</code> object from the
     * given list of parameters.
     *
     * @param x
     *            the x value of the quaternion.
     * @param y
     *            the y value of the quaternion.
     * @param z
     *            the z value of the quaternion.
     * @param w
     *            the w value of the quaternion.
     */
    public DQuaternion(double x, double y, double z, double w) {
        this.set(x, y, z, w);
    }

    public DQuaternion(Quaternion q) {
        this(q.getX(), q.getY(), q.getZ(), q.getW());
    }

    public Quaternion toQuaternion() {
        return new Quaternion((float) x, (float) y, (float) z, (float) w);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getW() {
        return w;
    }

    /**
     * sets the data in a <code>DQuaternion</code> object from the given list
     * of parameters.
     *
     * @param x
     *            the x value of the quaternion.
     * @param y
     *            the y value of the quaternion.
     * @param z
     *            the z value of the quaternion.
     * @param w
     *            the w value of the quaternion.
     * @return this
     */
    public DQuaternion set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Sets the data in this <code>DQuaternion</code> object to be equal to the
     * passed <code>DQuaternion</code> object. The values are copied producing
     * a new object.
     *
     * @param q
     *            The DQuaternion to copy values from.
     * @return this
     */
    public DQuaternion set(DQuaternion q) {
        x = q.x;
        y = q.y;
        z = q.z;
        w = q.w;
        return this;
    }

    /**
     * Sets this DQuaternion to {0, 0, 0, 1}. Same as calling set(0,0,0,1).
     */
    public void loadIdentity() {
        x = y = z = 0;
        w = 1;
    }

    /**
     * <code>fromAngleAxis</code> sets this quaternion to the values specified
     * by an angle and an axis of rotation. This method creates an object, so
     * use fromAngleNormalAxis if your axis is already normalized.
     *
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     * @return this quaternion
     */
    public DQuaternion fromAngleAxis(double angle, Vector3d axis) {
        Vector3d normAxis = axis.normalize();
        this.fromAngleNormalAxis(angle, normAxis);
        return this;
    }

    /**
     * <code>fromAngleNormalAxis</code> sets this quaternion to the values
     * specified by an angle and a normalized axis of rotation.
     *
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     */
    public DQuaternion fromAngleNormalAxis(double angle, Vector3d axis) {
        if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
            this.loadIdentity();
        } else {
            double halfAngle = 0.5f * angle;
            double sin = Math.sin(halfAngle);
            w = Math.cos(halfAngle);
            x = sin * axis.x;
            y = sin * axis.y;
            z = sin * axis.z;
        }
        return this;
    }

    /**
     * <code>add</code> adds the values of this quaternion to those of the
     * parameter quaternion. The result is returned as a new quaternion.
     *
     * @param q
     *            the quaternion to add to this.
     * @return the new quaternion.
     */
    public DQuaternion add(DQuaternion q) {
        return new DQuaternion(x + q.x, y + q.y, z + q.z, w + q.w);
    }

    /**
     * <code>add</code> adds the values of this quaternion to those of the
     * parameter quaternion. The result is stored in this DQuaternion.
     *
     * @param q
     *            the quaternion to add to this.
     * @return This DQuaternion after addition.
     */
    public DQuaternion addLocal(DQuaternion q) {
        x += q.x;
        y += q.y;
        z += q.z;
        w += q.w;
        return this;
    }

    /**
     * <code>subtract</code> subtracts the values of the parameter quaternion
     * from those of this quaternion. The result is returned as a new
     * quaternion.
     *
     * @param q
     *            the quaternion to subtract from this.
     * @return the new quaternion.
     */
    public DQuaternion subtract(DQuaternion q) {
        return new DQuaternion(x - q.x, y - q.y, z - q.z, w - q.w);
    }

    /**
     * <code>subtract</code> subtracts the values of the parameter quaternion
     * from those of this quaternion. The result is stored in this DQuaternion.
     *
     * @param q
     *            the quaternion to subtract from this.
     * @return This DQuaternion after subtraction.
     */
    public DQuaternion subtractLocal(DQuaternion q) {
        x -= q.x;
        y -= q.y;
        z -= q.z;
        w -= q.w;
        return this;
    }

    /**
     * <code>mult</code> multiplies this quaternion by a parameter quaternion.
     * The result is returned as a new quaternion. It should be noted that
     * quaternion multiplication is not commutative so q * p != p * q.
     *
     * @param q
     *            the quaternion to multiply this quaternion by.
     * @return the new quaternion.
     */
    public DQuaternion mult(DQuaternion q) {
        return this.mult(q, null);
    }

    /**
     * <code>mult</code> multiplies this quaternion by a parameter quaternion.
     * The result is returned as a new quaternion. It should be noted that
     * quaternion multiplication is not commutative so q * p != p * q.
     *
     * It IS safe for q and res to be the same object.
     * It IS NOT safe for this and res to be the same object.
     *
     * @param q
     *            the quaternion to multiply this quaternion by.
     * @param res
     *            the quaternion to store the result in.
     * @return the new quaternion.
     */
    public DQuaternion mult(DQuaternion q, DQuaternion res) {
        if (res == null) {
            res = new DQuaternion();
        }
        double qw = q.w, qx = q.x, qy = q.y, qz = q.z;
        res.x = x * qw + y * qz - z * qy + w * qx;
        res.y = -x * qz + y * qw + z * qx + w * qy;
        res.z = x * qy - y * qx + z * qw + w * qz;
        res.w = -x * qx - y * qy - z * qz + w * qw;
        return res;
    }

    /**
     * <code>mult</code> multiplies this quaternion by a parameter vector. The
     * result is returned as a new vector.
     *
     * @param v
     *            the vector to multiply this quaternion by.
     * @return the new vector.
     */
    public Vector3d mult(Vector3d v) {
        return this.mult(v, null);
    }

    /**
     * Multiplies this DQuaternion by the supplied quaternion. The result is
     * stored in this DQuaternion, which is also returned for chaining. Similar
     * to this *= q.
     *
     * @param q
     *            The DQuaternion to multiply this one by.
     * @return This DQuaternion, after multiplication.
     */
    public DQuaternion multLocal(DQuaternion q) {
        double x1 = x * q.w + y * q.z - z * q.y + w * q.x;
        double y1 = -x * q.z + y * q.w + z * q.x + w * q.y;
        double z1 = x * q.y - y * q.x + z * q.w + w * q.z;
        w = -x * q.x - y * q.y - z * q.z + w * q.w;
        x = x1;
        y = y1;
        z = z1;
        return this;
    }

    /**
     * <code>mult</code> multiplies this quaternion by a parameter vector. The
     * result is returned as a new vector.
     * 
     * @param v
     *            the vector to multiply this quaternion by.
     * @param store
     *            the vector to store the result in. It IS safe for v and store
     *            to be the same object.
     * @return the result vector.
     */
    public Vector3d mult(Vector3d v, Vector3d store) {
        if (store == null) {
            store = new Vector3d();
        }
        if (v.x == 0 && v.y == 0 && v.z == 0) {
            store.set(0, 0, 0);
        } else {
            double vx = v.x, vy = v.y, vz = v.z;
            store.x = w * w * vx + 2 * y * w * vz - 2 * z * w * vy + x * x * vx + 2 * y * x * vy + 2 * z * x * vz - z * z * vx - y * y * vx;
            store.y = 2 * x * y * vx + y * y * vy + 2 * z * y * vz + 2 * w * z * vx - z * z * vy + w * w * vy - 2 * x * w * vz - x * x * vy;
            store.z = 2 * x * z * vx + 2 * y * z * vy + z * z * vz - 2 * w * y * vx - y * y * vz + 2 * w * x * vy - x * x * vz + w * w * vz;
        }
        return store;
    }

    /**
     *
     * <code>toString</code> creates the string representation of this <code>DQuaternion</code>. The values of the quaternion are displaced (x,
     * y, z, w), in the following manner: <br>
     * (x, y, z, w)
     *
     * @return the string representation of this object.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ", " + w + ")";
    }

    /**
     * <code>equals</code> determines if two quaternions are logically equal,
     * that is, if the values of (x, y, z, w) are the same for both quaternions.
     *
     * @param o
     *            the object to compare for equality
     * @return true if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DQuaternion)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        DQuaternion comp = (DQuaternion) o;
        if (Double.compare(x, comp.x) != 0) {
            return false;
        }
        if (Double.compare(y, comp.y) != 0) {
            return false;
        }
        if (Double.compare(z, comp.z) != 0) {
            return false;
        }
        if (Double.compare(w, comp.w) != 0) {
            return false;
        }
        return true;
    }

    /**
     * 
     * <code>hashCode</code> returns the hash code value as an integer and is
     * supported for the benefit of hashing based collection classes such as
     * Hashtable, HashMap, HashSet etc.
     * 
     * @return the hashcode for this instance of DQuaternion.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        long hash = 37;
        hash = 37 * hash + Double.doubleToLongBits(x);
        hash = 37 * hash + Double.doubleToLongBits(y);
        hash = 37 * hash + Double.doubleToLongBits(z);
        hash = 37 * hash + Double.doubleToLongBits(w);
        return (int) hash;

    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule cap = e.getCapsule(this);
        cap.write(x, "x", 0);
        cap.write(y, "y", 0);
        cap.write(z, "z", 0);
        cap.write(w, "w", 1);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule cap = e.getCapsule(this);
        x = cap.readFloat("x", 0);
        y = cap.readFloat("y", 0);
        z = cap.readFloat("z", 0);
        w = cap.readFloat("w", 1);
    }

    @Override
    public DQuaternion clone() {
        try {
            return (DQuaternion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }
}
