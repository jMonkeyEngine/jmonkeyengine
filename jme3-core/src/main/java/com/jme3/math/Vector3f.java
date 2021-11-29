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
package com.jme3.math;

import com.jme3.export.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A Vector for a three float value tuple.
 * <code>Vector3f</code> can represent any three dimensional value, such as a
 * vertex, a normal, etc. Utility methods are also included to aid in
 * mathematical calculations.
 *
 * <p>Methods with names ending in "Local" modify the current instance. They are
 * used to cut down on the creation of new instances.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Vector3f implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;
    private static final Logger logger = Logger.getLogger(Vector3f.class.getName());
    /**
     * Shared instance of the all-zero vector (0,0,0). Do not modify!
     */
    public final static Vector3f ZERO = new Vector3f(0, 0, 0);
    /**
     * Shared instance of the all-NaN vector (NaN,NaN,NaN). Do not modify!
     */
    public final static Vector3f NAN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    /**
     * Shared instance of the +X direction (1,0,0). Do not modify!
     */
    public final static Vector3f UNIT_X = new Vector3f(1, 0, 0);
    /**
     * Shared instance of the +Y direction (0,1,0). Do not modify!
     */
    public final static Vector3f UNIT_Y = new Vector3f(0, 1, 0);
    /**
     * Shared instance of the +Z direction (0,0,1). Do not modify!
     */
    public final static Vector3f UNIT_Z = new Vector3f(0, 0, 1);
    /**
     * Shared instance of the all-ones vector (1,1,1). Do not modify!
     */
    public final static Vector3f UNIT_XYZ = new Vector3f(1, 1, 1);
    /**
     * Shared instance of the all-plus-infinity vector (+Inf,+Inf,+Inf). Do not
     * modify!
     */
    public final static Vector3f POSITIVE_INFINITY = new Vector3f(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    /**
     * Shared instance of the all-negative-infinity vector (-Inf,-Inf,-Inf). Do
     * not modify!
     */
    public final static Vector3f NEGATIVE_INFINITY = new Vector3f(
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);
    /**
     * The X component of the vector.
     */
    public float x;
    /**
     * The Y component of the vector.
     */
    public float y;
    /**
     * The Z component of the vector.
     */
    public float z;

    /**
     * Instantiates a new <code>Vector3f</code> with the default
     * value of (0,0,0).
     *
     */
    public Vector3f() {
        x = y = z = 0;
    }

    /**
     * Instantiates a new <code>Vector3f</code> with the provided
     * components.
     *
     * @param x   the X component of the vector.
     * @param y   the Y component of the vector.
     * @param z   the Z component of the vector.
     */
    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector.
     *
     * @param copy the Vector3f to copy (not null, unaffected)
     */
    public Vector3f(Vector3f copy) {
        this.set(copy);
    }

    /**
     * Sets the X, Y, and Z components of the vector based on passed
     * parameters.
     *
     * @param x   the X component of the vector.
     * @param y   the Y component of the vector.
     * @param z   the Z component of the vector.
     * @return this vector (modified)
     */
    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Sets the X, Y, and Z components of the vector by copying the
     * supplied vector.
     *
     * @param vect the Vector3f to copy (not null, unaffected)
     * @return this vector (modified)
     */
    public Vector3f set(Vector3f vect) {
        this.x = vect.x;
        this.y = vect.y;
        this.z = vect.z;
        return this;
    }

    /**
     * Adds a provided vector to this vector, creating a
     * resultant vector which is returned. If the provided vector is null, null
     * is returned. Either way, the current instance is unaffected.
     *
     * @param vec
     *            the vector to add to this (unaffected) or null for none
     * @return a new Vector3f or null
     */
    public Vector3f add(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        return new Vector3f(x + vec.x, y + vec.y, z + vec.z);
    }

    /**
     * Adds the components of a provided vector, storing the
     * sum in the supplied vector. The current instance is unaffected unless it is
     * <code>result</code>.
     *
     * @param vec the vector to add to this (not null, unaffected unless it is
     *     <code>result</code>)
     * @param result
     *            the vector to store the result in (not null)
     * @return result returns the supplied result vector.
     */
    public Vector3f add(Vector3f vec, Vector3f result) {
        result.x = x + vec.x;
        result.y = y + vec.y;
        result.z = z + vec.z;
        return result;
    }

    /**
     * Adds a provided vector to this vector internally
     * and returns a handle to this (modified) vector for chaining. If the
     * provided vector is null, null is returned.
     *
     * @param vec the vector to add to this vector (unaffected unless it is <code>this</code>)
     *     or null for none
     * @return this (modified) or null
     */
    public Vector3f addLocal(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x += vec.x;
        y += vec.y;
        z += vec.z;
        return this;
    }

    /**
     * Adds the provided offsets to this vector, creating a
     * new vector that is then returned. The current instance is unaffected.
     *
     * @param addX the amount to add to the X component
     * @param addY the amount to add to the Y component
     * @param addZ the amount to add to the Z component
     * @return the result vector.
     */
    public Vector3f add(float addX, float addY, float addZ) {
        return new Vector3f(x + addX, y + addY, z + addZ);
    }

    /**
     * Adds the provided offsets to this vector
     * internally and returns a handle to this (modified) vector for chaining.
     *
     * @param addX the amount to add to the X component
     * @param addY the amount to add to the Y component
     * @param addZ the amount to add to the Z component
     * @return this (modified)
     */
    public Vector3f addLocal(float addX, float addY, float addZ) {
        x += addX;
        y += addY;
        z += addZ;
        return this;
    }

    /**
     * Multiplies this vector by a scalar, then adds the
     * given Vector3f.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @param add the value to add (not null)
     * @return this (modified)
     */
    public Vector3f scaleAdd(float scalar, Vector3f add) {
        x = x * scalar + add.x;
        y = y * scalar + add.y;
        z = z * scalar + add.z;
        return this;
    }

    /**
     * Multiplies the given vector by a scalar, then adds
     * the given vector, storing the result in the current instance.
     *
     * @param scalar the scaling factor
     * @param mult the vector to multiply the scalar by (not null, unaffected unless it is
     *     <code>this</code>)
     * @param add the vector to add (not null, unaffected unless it is
     *     <code>this</code>)
     * @return this
     */
    public Vector3f scaleAdd(float scalar, Vector3f mult, Vector3f add) {
        this.x = mult.x * scalar + add.x;
        this.y = mult.y * scalar + add.y;
        this.z = mult.z * scalar + add.z;
        return this;
    }

    /**
     * Calculates the dot product of this vector with a
     * provided vector. If the provided vector is null, 0 is returned.
     * Either way, the current instance is unaffected.
     *
     * @param vec the vector to dot with this vector (unaffected) or null for none
     * @return the resultant dot product of this vector and a given vector.
     */
    public float dot(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, 0 returned.");
            return 0;
        }
        return x * vec.x + y * vec.y + z * vec.z;
    }

    /**
     * Calculates the cross product of this vector with a
     * parameter vector v. The current instance is unaffected.
     *
     * @param v the right factor (not null, unaffected)
     * @return the cross product (a new Vector3f)
     */
    public Vector3f cross(Vector3f v) {
        return cross(v, null);
    }

    /**
     * Calculates the cross product of this vector with a
     * parameter vector v.  The result is stored in <code>result</code>.
     * The current instance is unaffected unless it is <code>result</code>.
     *
     * @param v the right factor (not null, unaffected unless it is
     *     <code>result</code>)
     * @param result
     *            the vector to store the cross product result, or null for a new Vector3f
     * @return result, after receiving the cross product vector.
     */
    public Vector3f cross(Vector3f v, Vector3f result) {
        return cross(v.x, v.y, v.z, result);
    }

    /**
     * Calculates the cross product of this vector with
     * specified components.  The result is stored in <code>result</code>.
     * The current instance is unaffected unless it is <code>result</code>.
     *
     * @param otherX the X component of the right factor
     * @param otherY the Y component of the right factor
     * @param otherZ the Z component of the right factor
     * @param result
     *            the vector to store the product, or null for a new Vector3f
     * @return result, after receiving the cross product (either <code>result</code> or a new Vector3f)
     */
    public Vector3f cross(float otherX, float otherY, float otherZ, Vector3f result) {
        if (result == null) {
            result = new Vector3f();
        }
        float resX = ((y * otherZ) - (z * otherY));
        float resY = ((z * otherX) - (x * otherZ));
        float resZ = ((x * otherY) - (y * otherX));
        result.set(resX, resY, resZ);
        return result;
    }

    /**
     * Calculates the cross product of this vector
     * with a parameter vector v.
     *
     * @param v the right factor (not null, unaffected unless it is
     *     <code>this</code>)
     * @return this (modified)
     */
    public Vector3f crossLocal(Vector3f v) {
        return crossLocal(v.x, v.y, v.z);
    }

    /**
     * Calculates the cross product of this vector
     * with a parameter vector v.
     *
     * @param otherX the X component of the right factor
     * @param otherY the Y component of the right factor
     * @param otherZ the Z component of the right factor
     * @return this (modified)
     */
    public Vector3f crossLocal(float otherX, float otherY, float otherZ) {
        float tempx = (y * otherZ) - (z * otherY);
        float tempy = (z * otherX) - (x * otherZ);
        z = (x * otherY) - (y * otherX);
        x = tempx;
        y = tempy;
        return this;
    }

    /**
     * Projects this vector onto another vector. The current instance is
     * unaffected.
     *
     * @param other The vector to project this vector onto (not null, unaffected)
     * @return A new vector with the projection result
     */
    public Vector3f project(Vector3f other) {
        float n = this.dot(other); // A . B
        float d = other.lengthSquared(); // |B|^2
        return new Vector3f(other).multLocal(n / d);
    }

    /**
     * Projects this vector onto another vector and stores the result in this
     * vector.
     *
     * @param other The vector to project this vector onto (not null)
     * @return This Vector3f (modified)
     */
    public Vector3f projectLocal(Vector3f other) {
        float n = this.dot(other); // A . B
        float d = other.lengthSquared(); // |B|^2
        return set(other).multLocal(n / d);
    }

    /**
     * Returns true if this vector is a unit vector, with 1% tolerance.
     * Returns false otherwise. The current instance is unaffected.
     *
     * @return true if the current vector's length is between 0.99 and 1.01
     *     inclusive, or false otherwise.
     */
    public boolean isUnitVector() {
        float len = length();
        return 0.99f < len && len < 1.01f;
    }

    /**
     * Calculates the magnitude of this vector. The current instance is
     * unaffected.
     *
     * @return the length or magnitude of the vector.
     */
    public float length() {
        /*
         * Use double-precision arithmetic to reduce the chance of overflow
         * (when lengthSquared > Float.MAX_VALUE) or underflow (when
         * lengthSquared is < Float.MIN_VALUE).
         */
        double xx = x;
        double yy = y;
        double zz = z;
        double lengthSquared = xx * xx + yy * yy + zz * zz;
        float result = (float) Math.sqrt(lengthSquared);

        return result;
    }

    /**
     * Calculates the squared value of the
     * magnitude of the vector. The current instance is unaffected.
     *
     * @return the magnitude squared of the vector.
     */
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Calculates the distance squared between
     * this vector and vector v. The current instance is unaffected.
     *
     * @param v the second vector to determine the distance squared.
     * @return the square of the Euclidean distance between the two vectors.
     */
    public float distanceSquared(Vector3f v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        return (float) (dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the distance between this vector and
     * vector v. The current instance is unaffected.
     *
     * @param v the second vector to determine the distance (not null, unaffected)
     * @return the Euclidean distance between the two vectors.
     */
    public float distance(Vector3f v) {
        /*
         * Use double-precision arithmetic to reduce the chance of overflow
         * (when distanceSquared > Float.MAX_VALUE) or underflow (when
         * distanceSquared is < Float.MIN_VALUE).
         */
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        float result = (float) Math.sqrt(distanceSquared);

        return result;
    }

    /**
     * Multiplies this vector by a scalar. The resultant
     * vector is returned. The current instance is unaffected.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @return a new Vector3f
     */
    public Vector3f mult(float scalar) {
        return new Vector3f(x * scalar, y * scalar, z * scalar);
    }

    /**
     * Multiplies this vector by a scalar. The resultant
     * vector is stored in the second parameter and returned.
     * The current instance is unaffected, unless it is <code>product</code>.
     *
     * @param scalar the scalar to multiply this vector by.
     * @param product storage for the product, or null for a new Vector3f
     * @return either <code>product</code> or a new Vector3f
     */
    public Vector3f mult(float scalar, Vector3f product) {
        if (null == product) {
            product = new Vector3f();
        }

        product.x = x * scalar;
        product.y = y * scalar;
        product.z = z * scalar;
        return product;
    }

    /**
     * Multiplies this vector by a scalar internally,
     * and returns a handle to this (modified) vector for chaining.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @return this (modified)
     */
    public Vector3f multLocal(float scalar) {
        x *= scalar;
        y *= scalar;
        z *= scalar;
        return this;
    }

    /**
     * Multiplies a provided vector component-wise by this vector
     * internally, and returns a handle to this (modified) vector for chaining.
     * If the provided vector is null, null is returned.
     *
     * @param vec the vector to mult to this vector (unaffected unless it is <code>this</code>)
     *     or null for none
     * @return this (modified) or null
     */
    public Vector3f multLocal(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x *= vec.x;
        y *= vec.y;
        z *= vec.z;
        return this;
    }

    /**
     * Multiplies this vector component-wise by 3 scalars
     * internally, and returns a handle to this (modified) vector for chaining.
     *
     * @param x the scale factor for the X component
     * @param y the scale factor for the Y component
     * @param z the scale factor for the Z component
     * @return this (modified)
     */
    public Vector3f multLocal(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    /**
     * Multiplies a provided vector component-wise with
     * this vector and returns a handle to this vector for chaining.
     * If the provided vector is null, null is returned.
     * Either way, the current instance is unaffected.
     *
     * @param vec
     *            the vector to multiply with this vector, or null for none
     * @return a new Vector3f or null
     */
    public Vector3f mult(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        return mult(vec, null);
    }

    /**
     * Multiplies a provided vector component-wise with
     * this vector and stores the result in a 3rd vector. If the provided
     * vector is null, null is returned. Either way, the current instance is unaffected, unless it is
     * <code>store</code>.
     *
     * @param vec the vector to mult to this vector (unaffected unless it is <code>store</code>)
     *     or null for none
     * @param store result vector (null to create a new vector)
     * @return either <code>store</code> or a new Vector3f or null
     */
    public Vector3f mult(Vector3f vec, Vector3f store) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        if (store == null) {
            store = new Vector3f();
        }
        return store.set(x * vec.x, y * vec.y, z * vec.z);
    }

    /**
     * Divides the vector by a scalar and
     * returns the result. The current instance remains untouched.
     *
     * @param scalar
     *            the value to divide this vectors attributes by.
     * @return a new Vector3f
     */
    public Vector3f divide(float scalar) {
        scalar = 1f / scalar;
        return new Vector3f(x * scalar, y * scalar, z * scalar);
    }

    /**
     * Divides this vector by a scalar internally
     * and returns a handle to this (modified) vector for chaining.
     *
     * @param scalar
     *            the value to divide this vector by.
     * @return this (modified)
     */
    public Vector3f divideLocal(float scalar) {
        scalar = 1f / scalar;
        x *= scalar;
        y *= scalar;
        z *= scalar;
        return this;
    }

    /**
     * Divides this vector component-wise by the argument and
     * returns the result. The current instance remains untouched.
     *
     * @param scalar the divisor (not null, unaffected) TODO rename argument!
     * @return the result <code>Vector</code>.
     */
    public Vector3f divide(Vector3f scalar) {
        return new Vector3f(x / scalar.x, y / scalar.y, z / scalar.z);
    }

    /**
     * Divides this vector component-wise by a vector internally,
     * and returns a handle to this (modified) vector for chaining.
     *
     * @param scalar the divisor (not null) TODO rename argument!
     * @return this (modified)
     */
    public Vector3f divideLocal(Vector3f scalar) {
        x /= scalar.x;
        y /= scalar.y;
        z /= scalar.z;
        return this;
    }

    /**
     * Returns the negative of this vector. All components are
     * negated. The current instance is unaffected.
     *
     * @return a new Vector3f
     */
    public Vector3f negate() {
        return new Vector3f(-x, -y, -z);
    }

    /**
     * Negates the components of this vector.
     *
     * @return this (modified)
     */
    public Vector3f negateLocal() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Subtracts the given vector from this vector creating a new vector object.
     * The current instance is unaffected.
     *
     * @param vec the vector to subtract from this vector (not null, unaffected)
     * @return the result vector.
     */
    public Vector3f subtract(Vector3f vec) {
        return new Vector3f(x - vec.x, y - vec.y, z - vec.z);
    }

    /**
     * Subtracts a provided vector to this vector
     * internally and returns a handle to this (modified) vector for chaining.
     * If the provided vector is null, null is returned.
     *
     * @param vec the vector to subtract (unaffected unless it is
     *     <code>this</code>) or null for none
     * @return this (modified) or null
     */
    public Vector3f subtractLocal(Vector3f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x -= vec.x;
        y -= vec.y;
        z -= vec.z;
        return this;
    }

    /**
     * Subtracts the specified vector from this vector, storing the difference
     * in a 3rd vector. The current instnace is unaffected unless it is
     * <code>result</code>.
     *
     * @param vec the vector to subtract from this (not null, unaffected unless it is
     *     <code>result</code>)
     * @param result
     *            the vector to store the result in, or null for a new Vector3f
     * @return result or a new Vector3f
     */
    public Vector3f subtract(Vector3f vec, Vector3f result) {
        if (result == null) {
            result = new Vector3f();
        }
        result.x = x - vec.x;
        result.y = y - vec.y;
        result.z = z - vec.z;
        return result;
    }

    /**
     * Subtracts the provided values from this vector,
     * creating a new vector that is then returned. The current instance is unaffected.
     *
     * @param subtractX the amount to subtract from the X component
     * @param subtractY the amount to subtract from the Y component
     * @param subtractZ the amount to subtract from the Z component
     * @return the result vector.
     */
    public Vector3f subtract(float subtractX, float subtractY, float subtractZ) {
        return new Vector3f(x - subtractX, y - subtractY, z - subtractZ);
    }

    /**
     * Subtracts the provided values from this vector
     * internally and returns a handle to this (modified) vector for chaining.
     *
     * @param subtractX the amount to subtract from the X component
     * @param subtractY the amount to subtract from the Y component
     * @param subtractZ the amount to subtract from the Z component
     * @return this (modified)
     */
    public Vector3f subtractLocal(float subtractX, float subtractY, float subtractZ) {
        x -= subtractX;
        y -= subtractY;
        z -= subtractZ;
        return this;
    }

    /**
     * Normalizes the vector to length=1 and returns the result as a new
     * instance. If the vector has length=0, a clone is returned. Either way,
     * the current instance is unaffected.
     *
     * @return unit vector of this vector.
     */
    public Vector3f normalize() {
//        float length = length();
//        if (length != 0) {
//            return divide(length);
//        }
//
//        return divide(1);
        float length = x * x + y * y + z * z;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            return new Vector3f(x * length, y * length, z * length);
        }
        return clone();
    }

    /**
     * Normalizes the vector to length=1 and returns the (modified) current
     * instance. If the vector has length=0, it is unchanged.
     *
     * @return this (modified)
     */
    public Vector3f normalizeLocal() {
        // NOTE: this implementation is more optimized
        // than the old jme normalize as this method
        // is commonly used.
        float length = x * x + y * y + z * z;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            x *= length;
            y *= length;
            z *= length;
        }
        return this;
    }

    /**
     * Computes the maximum value for each
     * component in this and <code>other</code> vector. The result is stored
     * in this vector.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @return this (modified)
     */
    public Vector3f maxLocal(Vector3f other) {
        x = other.x > x ? other.x : x;
        y = other.y > y ? other.y : y;
        z = other.z > z ? other.z : z;
        return this;
    }

    /**
     * Computes the minimum value for each
     * component in this and <code>other</code> vector. The result is stored
     * in this vector.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @return this (modified)
     */
    public Vector3f minLocal(Vector3f other) {
        x = other.x < x ? other.x : x;
        y = other.y < y ? other.y : y;
        z = other.z < z ? other.z : z;
        return this;
    }

    /**
     * Resets this vector's components to zero.
     *
     * @return this (modified)
     */
    public Vector3f zero() {
        x = y = z = 0;
        return this;
    }

    /**
     * Returns (in radians) the angle between two vectors.
     * It is assumed that both this vector and the given vector are unit vectors (iow, normalized).
     * The current instance is unaffected.
     *
     * @param otherVector a unit vector to find the angle against (not null, unaffected)
     * @return the angle (in radians, not negative)
     */
    public float angleBetween(Vector3f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }

    /**
     * Interpolates linearly between this vector and the specified vector,
     * storing the result in the current instance and returning it.
     *
     * <p>this = (1 - changeAmnt) * this + changeAmnt * finalVec
     *
     * @param finalVec the desired value when changeAmnt=1 (not null, unaffected)
     * @param changeAmnt the fractional change amount
     * @return this (modified)
     */
    public Vector3f interpolateLocal(Vector3f finalVec, float changeAmnt) {
        this.x = (1 - changeAmnt) * this.x + changeAmnt * finalVec.x;
        this.y = (1 - changeAmnt) * this.y + changeAmnt * finalVec.y;
        this.z = (1 - changeAmnt) * this.z + changeAmnt * finalVec.z;
        return this;
    }

    /**
     * Interpolates linearly between the specified beginning and final vectors,
     * storing the result in the current instance and returning it.
     *
     * <p>this = (1 - changeAmnt) * beginVec + changeAmnt * finalVec
     *
     * @param beginVec the desired value when changeAmnt=0 (not null, unaffected
     *     unless it's this)
     * @param finalVec the desired value when changeAmnt=1 (not null, unaffected
     *     unless it's this)
     * @param changeAmnt the fractional change amount
     * @return this (modified)
     */
    public Vector3f interpolateLocal(Vector3f beginVec, Vector3f finalVec, float changeAmnt) {
        this.x = (1 - changeAmnt) * beginVec.x + changeAmnt * finalVec.x;
        this.y = (1 - changeAmnt) * beginVec.y + changeAmnt * finalVec.y;
        this.z = (1 - changeAmnt) * beginVec.z + changeAmnt * finalVec.z;
        return this;
    }

    /**
     * Checks a vector. If it is null or if any component is NaN or infinite,
     * return false.  Else return true.
     *
     * @param vector the vector to check (unaffected)
     * @return true or false as stated above.
     */
    public static boolean isValidVector(Vector3f vector) {
        if (vector == null) {
            return false;
        }
        if (Float.isNaN(vector.x)
                || Float.isNaN(vector.y)
                || Float.isNaN(vector.z)) {
            return false;
        }
        if (Float.isInfinite(vector.x)
                || Float.isInfinite(vector.y)
                || Float.isInfinite(vector.z)) {
            return false;
        }
        return true;
    }

    public static void generateOrthonormalBasis(Vector3f u, Vector3f v, Vector3f w) {
        w.normalizeLocal();
        generateComplementBasis(u, v, w);
    }

    public static void generateComplementBasis(Vector3f u, Vector3f v,
            Vector3f w) {
        float fInvLength;

        if (FastMath.abs(w.x) >= FastMath.abs(w.y)) {
            // w.x or w.z is the largest magnitude component, swap them
            fInvLength = FastMath.invSqrt(w.x * w.x + w.z * w.z);
            u.x = -w.z * fInvLength;
            u.y = 0.0f;
            u.z = +w.x * fInvLength;
            v.x = w.y * u.z;
            v.y = w.z * u.x - w.x * u.z;
            v.z = -w.y * u.x;
        } else {
            // w.y or w.z is the largest magnitude component, swap them
            fInvLength = FastMath.invSqrt(w.y * w.y + w.z * w.z);
            u.x = 0.0f;
            u.y = +w.z * fInvLength;
            u.z = -w.y * fInvLength;
            v.x = w.y * u.z - w.z * u.y;
            v.y = -w.x * u.z;
            v.z = w.x * u.y;
        }
    }

    /**
     * Creates a copy of this vector. The current instance is unaffected.
     *
     * @return a new instance, equivalent to this one
     */
    @Override
    public Vector3f clone() {
        try {
            return (Vector3f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    /**
     * Saves this Vector3f into the given float[] object. The current instance is
     * unaffected.
     *
     * @param floats storage for the components (must have length&ge;3. If null, a new float[3] is
     *            created.
     * @return an array, with X, Y, Z float components in that order (either <code>floats</code> or a new float[3])
     */
    public float[] toArray(float[] floats) {
        if (floats == null) {
            floats = new float[3];
        }
        floats[0] = x;
        floats[1] = y;
        floats[2] = z;
        return floats;
    }

    /**
     * Tests whether the vector is equal to the argument, distinguishing -0
     * from 0. The current instance is unaffected.
     *
     * @param o the object to compare for equality (may be null, unaffected)
     * @return true if they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector3f)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        Vector3f comp = (Vector3f) o;
        if (Float.compare(x, comp.x) != 0) {
            return false;
        }
        if (Float.compare(y, comp.y) != 0) {
            return false;
        }
        if (Float.compare(z, comp.z) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this vector is approximately equal to the specified vector within
     * the specified tolerance. The current instance is unaffected.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @param epsilon the desired error tolerance for each component
     * @return true if all 3 components are within tolerance, otherwise false
     */
    public boolean isSimilar(Vector3f other, float epsilon) {
        if (other == null) {
            return false;
        }
        if (Float.compare(Math.abs(other.x - x), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.y - y), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.z - z), epsilon) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this vector object based
     * on its components. If two vectors are logically equivalent, they will return
     * the same hash code. The current instance is
     * unaffected.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + Float.floatToIntBits(x);
        hash += 37 * hash + Float.floatToIntBits(y);
        hash += 37 * hash + Float.floatToIntBits(z);
        return hash;
    }

    /**
     * Returns a string representation of this vector. The current instance is
     * unaffected. The format is:
     *
     * (XX.XXXX, YY.YYYY, ZZ.ZZZZ)
     *
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Serializes this vector to the specified exporter, for example when
     * saving to a J3O file. The current instance is unaffected.
     *
     * @param e the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(x, "x", 0);
        capsule.write(y, "y", 0);
        capsule.write(z, "z", 0);
    }

    /**
     * De-serializes this vector from the specified importer, for example
     * when loading from a J3O file.
     *
     * @param e the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        x = capsule.readFloat("x", 0);
        y = capsule.readFloat("y", 0);
        z = capsule.readFloat("z", 0);
    }

    /**
     * Determines the X component of this vector. The vector is unaffected.
     *
     * @return the X component
     */
    public float getX() {
        return x;
    }

    /**
     * Alters the X component of this vector.
     *
     * @param x the desired value
     * @return this vector, modified
     */
    public Vector3f setX(float x) {
        this.x = x;
        return this;
    }

    /**
     * Determines the Y component of this vector. The vector is unaffected.
     *
     * @return the Y component
     */
    public float getY() {
        return y;
    }

    /**
     * Alters the Y component of this vector.
     *
     * @param y the desired value
     * @return this vector, modified
     */
    public Vector3f setY(float y) {
        this.y = y;
        return this;
    }

    /**
     * Determines the Z component of this vector. The vector is unaffected.
     *
     * @return the Z component
     */
    public float getZ() {
        return z;
    }

    /**
     * Alters the Z component of this vector.
     *
     * @param z the desired value
     * @return this vector, modified
     */
    public Vector3f setZ(float z) {
        this.z = z;
        return this;
    }

    /**
     * Returns the indexed component. The vector is unaffected.
     *
     * @param index 0, 1, or 2
     * @return the X component if index == 0, the Y component if index == 1 or the Z component if index == 2
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    public float get(int index) {
        switch (index) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
        }
        throw new IllegalArgumentException("index must be either 0, 1 or 2");
    }

    /**
     * Sets the indexed component.
     *
     * @param index which component to set: 0 &rarr; the X component, 1 &rarr;
     *     the Ycomponent, 2 &rarr; the Z component
     * @param value the desired component value
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    public void set(int index, float value) {
        switch (index) {
            case 0:
                x = value;
                return;
            case 1:
                y = value;
                return;
            case 2:
                z = value;
                return;
        }
        throw new IllegalArgumentException("index must be either 0, 1 or 2");
    }
}
