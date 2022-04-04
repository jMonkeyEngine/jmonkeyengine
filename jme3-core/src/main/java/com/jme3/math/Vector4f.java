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
 * <code>Vector4f</code> defines a Vector for a four float value tuple.
 * <code>Vector4f</code> can represent any four dimensional value, such as a
 * vertex, a normal, etc. Utility methods are also included to aid in
 * mathematical calculations.
 *
 * @author Maarten Steur
 */
public final class Vector4f implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = Logger.getLogger(Vector4f.class.getName());
    /**
     * shared instance of the all-zero vector (0,0,0,0) - Do not modify!
     */
    public final static Vector4f ZERO = new Vector4f(0, 0, 0, 0);
    /**
     * shared instance of the all-NaN vector (NaN,NaN,NaN,NaN) - Do not modify!
     */
    public final static Vector4f NAN = new Vector4f(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
    /**
     * shared instance of the +X direction (1,0,0,0) - Do not modify!
     */
    public final static Vector4f UNIT_X = new Vector4f(1, 0, 0, 0);
    /**
     * shared instance of the +Y direction (0,1,0,0) - Do not modify!
     */
    public final static Vector4f UNIT_Y = new Vector4f(0, 1, 0, 0);
    /**
     * shared instance of the +Z direction (0,0,1,0) - Do not modify!
     */
    public final static Vector4f UNIT_Z = new Vector4f(0, 0, 1, 0);
    /**
     * shared instance of the +W direction (0,0,0,1) - Do not modify!
     */
    public final static Vector4f UNIT_W = new Vector4f(0, 0, 0, 1);
    /**
     * shared instance of the all-ones vector (1,1,1,1) - Do not modify!
     */
    public final static Vector4f UNIT_XYZW = new Vector4f(1, 1, 1, 1);
    /**
     * shared instance of the all-plus-infinity vector (+Inf,+Inf,+Inf,+Inf)
     * - Do not modify!
     */
    public final static Vector4f POSITIVE_INFINITY = new Vector4f(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    /**
     * shared instance of the all-negative-infinity vector (-Inf,-Inf,-Inf,-Inf)
     * - Do not modify!
     */
    public final static Vector4f NEGATIVE_INFINITY = new Vector4f(
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);
    /**
     * the x value of the vector.
     */
    public float x;
    /**
     * the y value of the vector.
     */
    public float y;
    /**
     * the z value of the vector.
     */
    public float z;
    /**
     * the w value of the vector.
     */
    public float w;

    /**
     * Instantiate a <code>Vector4f</code> with the value (0,0,0,0).
     */
    public Vector4f() {
        x = y = z = w = 0;
    }

    /**
     * Constructor instantiates a new <code>Vector4f</code> with provides
     * values.
     *
     * @param x   the x value of the vector.
     * @param y   the y value of the vector.
     * @param z   the z value of the vector.
     * @param w   the w value of the vector.
     */
    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Instantiate a <code>Vector4f</code> that is a copy of the provided vector.
     *
     * @param copy The Vector4f to copy
     */
    public Vector4f(Vector4f copy) {
        this.set(copy);
    }

    /**
     * <code>set</code> sets the x,y,z,w values of the vector based on passed
     * parameters.
     *
     * @param x   the x value of the vector.
     * @param y   the y value of the vector.
     * @param z   the z value of the vector.
     * @param w   the w value of the vector.
     * @return this vector
     */
    public Vector4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * <code>set</code> sets the x,y,z values of the vector by copying the
     * supplied vector.
     *
     * @param vect
     *            the vector to copy.
     * @return this vector
     */
    public Vector4f set(Vector4f vect) {
        this.x = vect.x;
        this.y = vect.y;
        this.z = vect.z;
        this.w = vect.w;
        return this;
    }

    /**
     * <code>add</code> adds a provided vector to this vector creating a
     * resultant vector which is returned. If the provided vector is null, null
     * is returned.
     *
     * @param vec
     *            the vector to add to this.
     * @return the resultant vector.
     */
    public Vector4f add(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        return new Vector4f(x + vec.x, y + vec.y, z + vec.z, w + vec.w);
    }

    /**
     * <code>add</code> adds the values of a provided vector storing the
     * values in the supplied vector.
     *
     * @param vec
     *            the vector to add to this
     * @param result
     *            the vector to store the result in
     * @return result returns the supplied result vector.
     */
    public Vector4f add(Vector4f vec, Vector4f result) {
        result.x = x + vec.x;
        result.y = y + vec.y;
        result.z = z + vec.z;
        result.w = w + vec.w;
        return result;
    }

    /**
     * <code>addLocal</code> adds a provided vector to this vector internally,
     * and returns a handle to this vector for easy chaining of calls. If the
     * provided vector is null, null is returned.
     *
     * @param vec
     *            the vector to add to this vector.
     * @return this
     */
    public Vector4f addLocal(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x += vec.x;
        y += vec.y;
        z += vec.z;
        w += vec.w;
        return this;
    }

    /**
     * <code>add</code> adds the provided values to this vector, creating a
     * new vector that is then returned.
     *
     * @param addX
     *            the x value to add.
     * @param addY
     *            the y value to add.
     * @param addZ
     *            the z value to add.
     * @param addW
     *            the w value to add.
     * @return the result vector.
     */
    public Vector4f add(float addX, float addY, float addZ, float addW) {
        return new Vector4f(x + addX, y + addY, z + addZ, w + addW);
    }

    /**
     * <code>addLocal</code> adds the provided values to this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls.
     *
     * @param addX
     *            value to add to x
     * @param addY
     *            value to add to y
     * @param addZ
     *            value to add to z
     * @param addW
     *            the w value to add.
     * @return this
     */
    public Vector4f addLocal(float addX, float addY, float addZ, float addW) {
        x += addX;
        y += addY;
        z += addZ;
        w += addW;
        return this;
    }

    /**
     * <code>scaleAdd</code> multiplies this vector by a scalar then adds the
     * given Vector4f.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @param add
     *            the value to add
     * @return this
     */
    public Vector4f scaleAdd(float scalar, Vector4f add) {
        x = x * scalar + add.x;
        y = y * scalar + add.y;
        z = z * scalar + add.z;
        w = w * scalar + add.w;
        return this;
    }

    /**
     * <code>scaleAdd</code> multiplies the given vector by a scalar then adds
     * the given vector.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @param mult
     *            the value to multiply the scalar by
     * @param add
     *            the value to add
     * @return this
     */
    public Vector4f scaleAdd(float scalar, Vector4f mult, Vector4f add) {
        this.x = mult.x * scalar + add.x;
        this.y = mult.y * scalar + add.y;
        this.z = mult.z * scalar + add.z;
        this.w = mult.w * scalar + add.w;
        return this;
    }

    /**
     * <code>dot</code> calculates the dot product of this vector with a
     * provided vector. If the provided vector is null, 0 is returned.
     *
     * @param vec
     *            the vector to dot with this vector.
     * @return the resultant dot product of this vector and a given vector.
     */
    public float dot(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, 0 returned.");
            return 0;
        }
        return x * vec.x + y * vec.y + z * vec.z + w * vec.w;
    }

    public Vector4f project(Vector4f other) {
        float n = this.dot(other); // A . B
        float d = other.lengthSquared(); // |B|^2
        return new Vector4f(other).multLocal(n / d);
    }

    /**
     * Returns true if this vector is a unit vector (length() ~= 1),
     * returns false otherwise.
     *
     * @return true if this vector is a unit vector (length() ~= 1),
     * or false otherwise.
     */
    public boolean isUnitVector() {
        float len = length();
        return 0.99f < len && len < 1.01f;
    }

    /**
     * <code>length</code> calculates the magnitude of this vector.
     *
     * @return the length or magnitude of the vector.
     */
    public float length() {
        return FastMath.sqrt(lengthSquared());
    }

    /**
     * <code>lengthSquared</code> calculates the squared value of the
     * magnitude of the vector.
     *
     * @return the magnitude squared of the vector.
     */
    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * <code>distanceSquared</code> calculates the distance squared between
     * this vector and vector v.
     *
     * @param v the second vector to determine the distance squared.
     * @return the distance squared between the two vectors.
     */
    public float distanceSquared(Vector4f v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        double dw = w - v.w;
        return (float) (dx * dx + dy * dy + dz * dz + dw * dw);
    }

    /**
     * <code>distance</code> calculates the distance between this vector and
     * vector v.
     *
     * @param v the second vector to determine the distance.
     * @return the distance between the two vectors.
     */
    public float distance(Vector4f v) {
        return FastMath.sqrt(distanceSquared(v));
    }

    /**
     * <code>mult</code> multiplies this vector by a scalar. The resultant
     * vector is returned.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @return the new vector.
     */
    public Vector4f mult(float scalar) {
        return new Vector4f(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    /**
     * <code>mult</code> multiplies this vector by a scalar. The resultant
     * vector is supplied as the second parameter and returned.
     *
     * @param scalar the scalar to multiply this vector by.
     * @param product the product to store the result in.
     * @return product
     */
    public Vector4f mult(float scalar, Vector4f product) {
        if (null == product) {
            product = new Vector4f();
        }

        product.x = x * scalar;
        product.y = y * scalar;
        product.z = z * scalar;
        product.w = w * scalar;
        return product;
    }

    /**
     * Multiplies component-wise by the specified components and returns the
     * product as a new instance. The current instance is unaffected.
     *
     * @param x the scale factor for the X component
     * @param y the scale factor for the Y component
     * @param z the scale factor for the Z component
     * @param w the scale factor for the W component
     * @return a new Vector4f
     */
    public Vector4f mult(float x, float y, float z, float w) {
        return new Vector4f(this.x * x, this.y * y, this.z * z, this.w * w);
    }

    /**
     * <code>multLocal</code> multiplies this vector by a scalar internally,
     * and returns a handle to this vector for easy chaining of calls.
     *
     * @param scalar
     *            the value to multiply this vector by.
     * @return this
     */
    public Vector4f multLocal(float scalar) {
        x *= scalar;
        y *= scalar;
        z *= scalar;
        w *= scalar;
        return this;
    }

    /**
     * <code>multLocal</code> multiplies a provided vector to this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls. If the provided vector is null, null is returned.
     *
     * @param vec
     *            the vector to mult to this vector.
     * @return this
     */
    public Vector4f multLocal(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x *= vec.x;
        y *= vec.y;
        z *= vec.z;
        w *= vec.w;
        return this;
    }

    /**
     * <code>multLocal</code> multiplies this vector by 4 scalars
     * internally, and returns a handle to this vector for easy chaining of
     * calls.
     *
     * @param x the scaling factor for the X component
     * @param y the scaling factor for the Y component
     * @param z the scaling factor for the Z component
     * @param w the scaling factor for the W component
     * @return this
     */
    public Vector4f multLocal(float x, float y, float z, float w) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        this.w *= w;
        return this;
    }

    /**
     * <code>multLocal</code> multiplies a provided vector to this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls. If the provided vector is null, null is returned.
     *
     * @param vec
     *            the vector to mult to this vector.
     * @return this
     */
    public Vector4f mult(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        return mult(vec, null);
    }

    /**
     * <code>multLocal</code> multiplies a provided vector to this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls. If the provided vector is null, null is returned.
     *
     * @param vec
     *            the vector to mult to this vector.
     * @param store result vector (null to create a new vector)
     * @return this
     */
    public Vector4f mult(Vector4f vec, Vector4f store) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        if (store == null) {
            store = new Vector4f();
        }
        return store.set(x * vec.x, y * vec.y, z * vec.z, w * vec.w);
    }

    /**
     * <code>divide</code> divides the values of this vector by a scalar and
     * returns the result. The values of this vector remain untouched.
     *
     * @param scalar
     *            the value to divide this vectors attributes by.
     * @return the result <code>Vector</code>.
     */
    public Vector4f divide(float scalar) {
        scalar = 1f / scalar;
        return new Vector4f(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    /**
     * <code>divideLocal</code> divides this vector by a scalar internally,
     * and returns a handle to this vector for easy chaining of calls. Dividing
     * by zero will result in an exception.
     *
     * @param scalar
     *            the value to divides this vector by.
     * @return this
     */
    public Vector4f divideLocal(float scalar) {
        scalar = 1f / scalar;
        x *= scalar;
        y *= scalar;
        z *= scalar;
        w *= scalar;
        return this;
    }

    /**
     * <code>divide</code> divides the values of this vector by a scalar and
     * returns the result. The values of this vector remain untouched.
     *
     * @param divisor
     *            the value to divide this vectors attributes by.
     * @return the result <code>Vector</code>.
     */
    public Vector4f divide(Vector4f divisor) {
        return new Vector4f(x / divisor.x, y / divisor.y, z / divisor.z, w / divisor.w);
    }

    /**
     * <code>divideLocal</code> divides this vector by a scalar internally,
     * and returns a handle to this vector for easy chaining of calls. Dividing
     * by zero will result in an exception.
     *
     * @param divisor
     *            the value to divides this vector by.
     * @return this
     */
    public Vector4f divideLocal(Vector4f divisor) {
        x /= divisor.x;
        y /= divisor.y;
        z /= divisor.z;
        w /= divisor.w;
        return this;
    }

    /**
     * Divides component-wise by the specified components returns the (modified)
     * current instance.
     *
     * @param x the divisor for the X component
     * @param y the divisor for the Y component
     * @param z the divisor for the Z component
     * @param w the divisor for the W component
     * @return the (modified) current instance (for chaining)
     */
    public Vector4f divideLocal(float x, float y, float z, float w) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        this.w /= w;
        return this;
    }

    /**
     * Divides component-wise by the specified components and returns the quotient
     * as a new instance. The current instance is unaffected.
     *
     * @param x the divisor for the X component
     * @param y the divisor for the Y component
     * @param z the divisor for the Z component
     * @param w the divisor for the W component
     * @return a new Vector4f
     */
    public Vector4f divide(float x, float y, float z, float w) {
        return new Vector4f(this.x / x, this.y / y, this.z / z, this.w / w);
    }

    /**
     * <code>negate</code> returns the negative of this vector. All values are
     * negated and set to a new vector.
     *
     * @return the negated vector.
     */
    public Vector4f negate() {
        return new Vector4f(-x, -y, -z, -w);
    }

    /**
     * <code>negateLocal</code> negates the internal values of this vector.
     *
     * @return this.
     */
    public Vector4f negateLocal() {
        x = -x;
        y = -y;
        z = -z;
        w = -w;
        return this;
    }

    /**
     * <code>subtract</code> subtracts the values of a given vector from those
     * of this vector creating a new vector object. If the provided vector is
     * null, null is returned.
     *
     * @param vec
     *            the vector to subtract from this vector.
     * @return the result vector.
     */
    public Vector4f subtract(Vector4f vec) {
        return new Vector4f(x - vec.x, y - vec.y, z - vec.z, w - vec.w);
    }

    /**
     * <code>subtractLocal</code> subtracts a provided vector to this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls. If the provided vector is null, null is returned.
     *
     * @param vec
     *            the vector to subtract
     * @return this
     */
    public Vector4f subtractLocal(Vector4f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x -= vec.x;
        y -= vec.y;
        z -= vec.z;
        w -= vec.w;
        return this;
    }

    /**
     * <code>subtract</code>
     *
     * @param vec
     *            the vector to subtract from this
     * @param result
     *            the vector to store the result in
     * @return result
     */
    public Vector4f subtract(Vector4f vec, Vector4f result) {
        if (result == null) {
            result = new Vector4f();
        }
        result.x = x - vec.x;
        result.y = y - vec.y;
        result.z = z - vec.z;
        result.w = w - vec.w;
        return result;
    }

    /**
     * <code>subtract</code> subtracts the provided values from this vector,
     * creating a new vector that is then returned.
     *
     * @param subtractX
     *            the x value to subtract.
     * @param subtractY
     *            the y value to subtract.
     * @param subtractZ
     *            the z value to subtract.
     * @param subtractW
     *            the w value to subtract.
     * @return the result vector.
     */
    public Vector4f subtract(float subtractX, float subtractY, float subtractZ, float subtractW) {
        return new Vector4f(x - subtractX, y - subtractY, z - subtractZ, w - subtractW);
    }

    /**
     * <code>subtractLocal</code> subtracts the provided values from this vector
     * internally, and returns a handle to this vector for easy chaining of
     * calls.
     *
     * @param subtractX
     *            the x value to subtract.
     * @param subtractY
     *            the y value to subtract.
     * @param subtractZ
     *            the z value to subtract.
     * @param subtractW
     *            the w value to subtract.
     * @return this
     */
    public Vector4f subtractLocal(float subtractX, float subtractY, float subtractZ, float subtractW) {
        x -= subtractX;
        y -= subtractY;
        z -= subtractZ;
        w -= subtractW;
        return this;
    }

    /**
     * <code>normalize</code> returns the unit vector of this vector.
     *
     * @return unit vector of this vector.
     */
    public Vector4f normalize() {
//        float length = length();
//        if (length != 0) {
//            return divide(length);
//        }
//
//        return divide(1);
        float length = x * x + y * y + z * z + w * w;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            return new Vector4f(x * length, y * length, z * length, w * length);
        }
        return clone();
    }

    /**
     * <code>normalizeLocal</code> makes this vector into a unit vector of
     * itself.
     *
     * @return this.
     */
    public Vector4f normalizeLocal() {
        // NOTE: this implementation is more optimized
        // than the old jme normalize as this method
        // is commonly used.
        float length = x * x + y * y + z * z + w * w;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            x *= length;
            y *= length;
            z *= length;
            w *= length;
        }
        return this;
    }

    /**
     * <code>maxLocal</code> computes the maximum value for each
     * component in this and <code>other</code> vector. The result is stored
     * in this vector.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @return this
     */
    public Vector4f maxLocal(Vector4f other) {
        x = other.x > x ? other.x : x;
        y = other.y > y ? other.y : y;
        z = other.z > z ? other.z : z;
        w = other.w > w ? other.w : w;
        return this;
    }

    /**
     * <code>minLocal</code> computes the minimum value for each
     * component in this and <code>other</code> vector. The result is stored
     * in this vector.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @return this
     */
    public Vector4f minLocal(Vector4f other) {
        x = other.x < x ? other.x : x;
        y = other.y < y ? other.y : y;
        z = other.z < z ? other.z : z;
        w = other.w < w ? other.w : w;
        return this;
    }

    /**
     * <code>zero</code> resets this vector's data to zero internally.
     *
     * @return this, with all components set to zero
     */
    public Vector4f zero() {
        x = y = z = w = 0;
        return this;
    }

    /**
     * <code>angleBetween</code> returns (in radians) the angle between two vectors.
     * It is assumed that both this vector and the given vector are unit vectors (iow, normalized).
     *
     * @param otherVector a unit vector to find the angle against
     * @return the angle in radians.
     */
    public float angleBetween(Vector4f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }

    /**
     * Sets this vector to the interpolation by changeAmount from this to the finalVec
     * this=(1-changeAmount)*this + changeAmount * finalVec
     *
     * @param finalVec The final vector to interpolate towards
     * @param changeAmount An amount between 0.0 - 1.0 representing a percentage
     *  change from this towards finalVec
     * @return this
     */
    public Vector4f interpolateLocal(Vector4f finalVec, float changeAmount) {
        this.x = (1 - changeAmount) * this.x + changeAmount * finalVec.x;
        this.y = (1 - changeAmount) * this.y + changeAmount * finalVec.y;
        this.z = (1 - changeAmount) * this.z + changeAmount * finalVec.z;
        this.w = (1 - changeAmount) * this.w + changeAmount * finalVec.w;
        return this;
    }

    /**
     * Sets this vector to the interpolation by changeAmount from beginVec to finalVec
     * this=(1-changeAmount)*beginVec + changeAmount * finalVec
     *
     * @param beginVec the beginning vector (changeAmount=0)
     * @param finalVec The final vector to interpolate towards
     * @param changeAmount An amount between 0.0 - 1.0 representing a percentage
     *  change from beginVec towards finalVec
     * @return this
     */
    public Vector4f interpolateLocal(Vector4f beginVec, Vector4f finalVec, float changeAmount) {
        this.x = (1 - changeAmount) * beginVec.x + changeAmount * finalVec.x;
        this.y = (1 - changeAmount) * beginVec.y + changeAmount * finalVec.y;
        this.z = (1 - changeAmount) * beginVec.z + changeAmount * finalVec.z;
        this.w = (1 - changeAmount) * beginVec.w + changeAmount * finalVec.w;
        return this;
    }

    /**
     * Check a vector... if it is null or its floats are NaN or infinite,
     * return false.  Else return true.
     *
     * @param vector the vector to check
     * @return true or false as stated above.
     */
    public static boolean isValidVector(Vector4f vector) {
        if (vector == null) {
            return false;
        }
        if (Float.isNaN(vector.x)
                || Float.isNaN(vector.y)
                || Float.isNaN(vector.z)
                || Float.isNaN(vector.w)) {
            return false;
        }
        if (Float.isInfinite(vector.x)
                || Float.isInfinite(vector.y)
                || Float.isInfinite(vector.z)
                || Float.isInfinite(vector.w)) {
            return false;
        }
        return true;
    }

    /**
     * Create a copy of this vector.
     *
     * @return a new instance, equivalent to this one
     */
    @Override
    public Vector4f clone() {
        try {
            return (Vector4f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    /**
     * Saves this Vector4f into the given float[] object.
     *
     * @param floats
     *            The float[] to take this Vector4f. If null, a new float[4] is
     *            created.
     * @return The array, with X, Y, Z, W float values in that order
     */
    public float[] toArray(float[] floats) {
        if (floats == null) {
            floats = new float[4];
        }
        floats[0] = x;
        floats[1] = y;
        floats[2] = z;
        floats[3] = w;
        return floats;
    }

    /**
     * Are these two vectors the same? They are if they have the same x, y,
     * z, and w values.
     *
     * @param o   the object to compare for equality
     * @return true if they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector4f)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        Vector4f comp = (Vector4f) o;
        if (Float.compare(x, comp.x) != 0) {
            return false;
        }
        if (Float.compare(y, comp.y) != 0) {
            return false;
        }
        if (Float.compare(z, comp.z) != 0) {
            return false;
        }
        if (Float.compare(w, comp.w) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this vector is similar to the specified vector within
     * some value of epsilon.
     *
     * @param other the vector to compare with (not null, unaffected)
     * @param epsilon the desired error tolerance for each component
     * @return true if all 4 components are within tolerance, otherwise false
     */
    public boolean isSimilar(Vector4f other, float epsilon) {
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
        if (Float.compare(Math.abs(other.w - w), epsilon) > 0) {
            return false;
        }
        return true;
    }

    /**
     * <code>hashCode</code> returns a unique code for this vector object based
     * on its values. If two vectors are logically equivalent, they will return
     * the same hash code value.
     *
     * @return the hash code value of this vector.
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + Float.floatToIntBits(x);
        hash += 37 * hash + Float.floatToIntBits(y);
        hash += 37 * hash + Float.floatToIntBits(z);
        hash += 37 * hash + Float.floatToIntBits(w);
        return hash;
    }

    /**
     * <code>toString</code> returns the string representation of this vector.
     * The format is:
     *
     * (XX.XXXX, YY.YYYY, ZZ.ZZZZ, WW.WWWW)
     *
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ", " + w + ")";
    }

    /**
     * Serialize this vector to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param e (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(x, "x", 0);
        capsule.write(y, "y", 0);
        capsule.write(z, "z", 0);
        capsule.write(w, "w", 0);
    }

    /**
     * De-serialize this vector from the specified importer, for example
     * when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        x = capsule.readFloat("x", 0);
        y = capsule.readFloat("y", 0);
        z = capsule.readFloat("z", 0);
        w = capsule.readFloat("w", 0);
    }

    /**
     * Determine the X component of this vector.
     *
     * @return x
     */
    public float getX() {
        return x;
    }

    /**
     * Alter the X component of this vector.
     *
     * @param x the desired value
     * @return this vector, modified
     */
    public Vector4f setX(float x) {
        this.x = x;
        return this;
    }

    /**
     * Determine the Y component of this vector.
     *
     * @return y
     */
    public float getY() {
        return y;
    }

    /**
     * Alter the Y component of this vector.
     *
     * @param y the desired value
     * @return this vector, modified
     */
    public Vector4f setY(float y) {
        this.y = y;
        return this;
    }

    /**
     * Determine the Z component of this vector.
     *
     * @return z
     */
    public float getZ() {
        return z;
    }

    /**
     * Alter the Z component of this vector.
     *
     * @param z the desired value
     * @return this vector, modified
     */
    public Vector4f setZ(float z) {
        this.z = z;
        return this;
    }

    /**
     * Determine the W component of this vector.
     *
     * @return w
     */
    public float getW() {
        return w;
    }

    /**
     * Alter the W component of this vector.
     *
     * @param w the desired value
     * @return this vector, modified
     */
    public Vector4f setW(float w) {
        this.w = w;
        return this;
    }

    /**
     * @param index which component (&ge;0, &lt;4)
     * @return x value if index == 0, y value if index == 1 or z value if index == 2
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
            case 3:
                return w;
        }
        throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
    }

    /**
     * @param index
     *            which field index in this vector to set.
     * @param value
     *            to set to one of x, y, z or w.
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2, 3.
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
            case 3:
                w = value;
                return;
        }
        throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
    }
}
