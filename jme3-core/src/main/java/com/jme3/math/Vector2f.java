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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Logger;

/**
 * A vector composed of 2 single-precision components, used to represent
 * locations, offsets, directions, and rotations in 2-dimensional space.
 *
 * <p>Methods with names ending in "Local" modify the current instance. They are
 * used to avoid creating garbage.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Vector2f implements Savable, Cloneable, java.io.Serializable {
    static final long serialVersionUID = 1;
    private static final Logger logger = Logger.getLogger(Vector2f.class.getName());
    /**
     * Shared instance of the all-zero vector (0,0). Do not modify!
     */
    public static final Vector2f ZERO = new Vector2f(0f, 0f);
    /**
     * Shared instance of the all-ones vector (1,1). Do not modify!
     */
    public static final Vector2f UNIT_XY = new Vector2f(1f, 1f);
    /**
     * The first (X) component.
     */
    public float x;
    /**
     * The 2nd (Y) component.
     */
    public float y;

    /**
     * Instantiates a vector with specified components.
     *
     * @param x the desired X component
     * @param y the desired Y component
     */
    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Instantiates an all-zero vector (0,0).
     */
    public Vector2f() {
        x = y = 0;
    }

    /**
     * Instantiates a copy of the argument.
     *
     * @param vector2f the vector to copy (not null, unaffected)
     */
    public Vector2f(Vector2f vector2f) {
        this.x = vector2f.x;
        this.y = vector2f.y;
    }

    /**
     * Sets both components to specified values.
     *
     * @param x the desired X component
     * @param y the desired Y component
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Copies both components from the argument.
     *
     * @param vec the Vector2f to copy (not null, unaffected)
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f set(Vector2f vec) {
        this.x = vec.x;
        this.y = vec.y;
        return this;
    }

    /**
     * Adds the argument and returns the sum as a new instance. If the argument
     * is null, null is returned. Either way, the current instance is
     * unaffected.
     *
     * @param vec the vector to add (unaffected) or null for none
     * @return a new Vector2f or null
     */
    public Vector2f add(Vector2f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        return new Vector2f(x + vec.x, y + vec.y);
    }

    /**
     * Adds the argument and returns the (modified) current instance. If the
     * argument is null, null is returned.
     *
     * @param vec the vector to add (unaffected unless it's {@code this}) or
     *     null for none
     * @return the (modified) current instance or null
     */
    public Vector2f addLocal(Vector2f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x += vec.x;
        y += vec.y;
        return this;
    }

    /**
     * Adds specified amounts to the vector's components and returns the
     * (modified) current instance.
     *
     * @param addX the amount to add to the X component
     * @param addY the amount to add to the Y component
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f addLocal(float addX, float addY) {
        x += addX;
        y += addY;
        return this;
    }

    /**
     * Adds a specified vector and returns the sum in a 3rd vector. If the
     * argument is null, null is returned. Either way, the current instance is
     * unaffected unless it's {@code result}.
     *
     * @param vec the vector to add (unaffected unless it's {@code result}) or
     *     null for none
     * @param result storage for the sum, or null for a new Vector2f
     * @return the sum (either {@code result} or a new Vector2f)
     */
    public Vector2f add(Vector2f vec, Vector2f result) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        if (result == null) {
            result = new Vector2f();
        }
        result.x = x + vec.x;
        result.y = y + vec.y;
        return result;
    }

    /**
     * Returns the dot (or inner) product with the argument. If the argument is
     * null, 0 is returned. Either way, the current instance is unaffected.
     *
     * @param vec the vector to multiply (unaffected) or null for none
     * @return the product or 0
     */
    public float dot(Vector2f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, 0 returned.");
            return 0;
        }
        return x * vec.x + y * vec.y;
    }

    /**
     * Calculates a cross product with the argument and returns the product as a
     * new instance. The current instance is unaffected.
     *
     * @param v the right factor (not null, unaffected)
     * @return {@code this} cross {@code v} (a new Vector3f)
     */
    public Vector3f cross(Vector2f v) {
        return new Vector3f(0, 0, determinant(v));
    }

    /**
     * Returns the Z component of the cross product with the argument. The
     * current instance is unaffected.
     *
     * @param v the right factor (not null, unaffected)
     * @return the Z component of {@code this} cross {@code v}
     */
    public float determinant(Vector2f v) {
        return (x * v.y) - (y * v.x);
    }

    /**
     * Interpolates linearly between the current instance and the specified
     * vector, returning the (modified) current instance.
     *
     * <p>this = (1 - changeAmount) * this + changeAmount * finalVec
     *
     * @param finalVec the desired value when changeAmount=1 (not null,
     *     unaffected unless it's {@code this})
     * @param changeAmount the fractional change amount
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f interpolateLocal(Vector2f finalVec, float changeAmount) {
        this.x = (1 - changeAmount) * this.x + changeAmount * finalVec.x;
        this.y = (1 - changeAmount) * this.y + changeAmount * finalVec.y;
        return this;
    }

    /**
     * Interpolates linearly between the specified beginning and final vectors,
     * returning the (modified) current instance.
     *
     * <p>this = (1 - changeAmount) * beginVec + changeAmount * finalVec
     *
     * @param beginVec the desired value when changeAmount=0 (not null,
     *     unaffected unless it's {@code this})
     * @param finalVec the desired value when changeAmount=1 (not null,
     *     unaffected unless it's {@code this})
     * @param changeAmount the fractional change amount
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f interpolateLocal(Vector2f beginVec, Vector2f finalVec,
            float changeAmount) {
        this.x = (1 - changeAmount) * beginVec.x + changeAmount * finalVec.x;
        this.y = (1 - changeAmount) * beginVec.y + changeAmount * finalVec.y;
        return this;
    }

    /**
     * Tests whether the argument is a valid vector, returning false if it's
     * null or if any component is NaN or infinite.
     *
     * @param vector the vector to test (unaffected)
     * @return true if non-null and finite, otherwise false
     */
    public static boolean isValidVector(Vector2f vector) {
        if (vector == null) {
            return false;
        }
        if (Float.isNaN(vector.x)
                || Float.isNaN(vector.y)) {
            return false;
        }
        if (Float.isInfinite(vector.x)
                || Float.isInfinite(vector.y)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the length (or magnitude). The current instance is unaffected.
     *
     * @return the root-sum of the squared components (not negative)
     */
    public float length() {
        return FastMath.sqrt(lengthSquared());
    }

    /**
     * Returns the square of the length. The current instance is unaffected.
     *
     * @return the sum of the squared components (not negative)
     */
    public float lengthSquared() {
        return x * x + y * y;
    }

    /**
     * Returns the square of the distance between the current instance and the
     * argument. The current instance is unaffected.
     *
     * @param v the vector to compare (not null, unaffected)
     * @return the square of the Euclidean distance (not negative)
     */
    public float distanceSquared(Vector2f v) {
        double dx = x - v.x;
        double dy = y - v.y;
        return (float) (dx * dx + dy * dy);
    }

    /**
     * Returns the square of the distance between the current instance and a
     * vector with the specified components. The current instance is unaffected.
     *
     * @param otherX the X component of the vector to compare
     * @param otherY the Y component of the vector to compare
     * @return the square of the Euclidean distance (not negative)
     */
    public float distanceSquared(float otherX, float otherY) {
        double dx = x - otherX;
        double dy = y - otherY;
        return (float) (dx * dx + dy * dy);
    }

    /**
     * Returns the distance between the current instance and the argument. The
     * current instance is unaffected.
     *
     * @param v the vector to compare (not null, unaffected)
     * @return the Euclidean distance (not negative)
     */
    public float distance(Vector2f v) {
        return FastMath.sqrt(distanceSquared(v));
    }

    /**
     * Multiplies with the scalar argument and returns the product as a new
     * instance. The current instance is unaffected.
     *
     * @param scalar the scaling factor
     * @return a new {@code Vector2f}
     */
    public Vector2f mult(float scalar) {
        return new Vector2f(x * scalar, y * scalar);
    }

    /**
     * Multiplies by the scalar argument and returns the (modified) current
     * instance.
     *
     * @param scalar the scaling factor
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f multLocal(float scalar) {
        x *= scalar;
        y *= scalar;
        return this;
    }

    /**
     * Multiplies component-wise by the argument and returns the (modified)
     * current instance. If the argument is null, null is returned.
     *
     * @param vec the scale vector (unaffected unless it's {@code this}) or
     *     null for none
     * @return the (modified) current instance (for chaining) or null
     */
    public Vector2f multLocal(Vector2f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x *= vec.x;
        y *= vec.y;
        return this;
    }

    /**
     * Multiplies with the specified scalar and stores the product in the
     * specified vector. The current instance is unaffected.
     *
     * @param scalar the scaling factor
     * @param product storage for the product, or null for a new Vector2f
     * @return either {@code product} or a new Vector2f
     */
    public Vector2f mult(float scalar, Vector2f product) {
        if (null == product) {
            product = new Vector2f();
        }

        product.x = x * scalar;
        product.y = y * scalar;
        return product;
    }

    /**
     * Multiplies component-wise by the specified components and returns the
     * product as a new instance. The current instance is unaffected.
     *
     * @param x the scale factor for the X component
     * @param y the scale factor for the Y component
     * @return a new Vector2f
     */
    public Vector2f mult(float x, float y) {
        return new Vector2f(this.x * x, this.y * y);
    }

    /**
     * Multiplies component-wise by the specified components and returns the
     * (modified) current instance.
     *
     * @param x the scale factor for the X component
     * @param y the scale factor for the Y component
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f multLocal(float x, float y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    /**
     * Divides by the scalar argument and returns the quotient as a new
     * instance. The current instance is unaffected.
     *
     * @param scalar the divisor
     * @return a new {@code Vector2f}
     */
    public Vector2f divide(float scalar) {
        return new Vector2f(x / scalar, y / scalar);
    }

    /**
     * Divides the vector by the scalar argument and returns the (modified)
     * current instance.
     *
     * @param scalar the divisor
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f divideLocal(float scalar) {
        x /= scalar;
        y /= scalar;
        return this;
    }

    /**
     * Divides component-wise by the specified components and returns the quotient
     * as a new instance. The current instance is unaffected.
     *
     * @param x the divisor for the X component
     * @param y the divisor for the Y component
     * @return a new Vector2f
     */
    public Vector2f divide(float x, float y) {
        return new Vector2f(this.x / x, this.y / y);
    }

    /**
     * Divides component-wise by the specified components returns the (modified)
     * current instance.
     *
     * @param x the divisor for the X component
     * @param y the divisor for the Y component
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f divideLocal(float x, float y) {
        this.x /= x;
        this.y /= y;
        return this;
    }

    /**
     * Returns the negative of the vector. The current instance is unaffected.
     *
     * @return a new Vector2f
     */
    public Vector2f negate() {
        return new Vector2f(-x, -y);
    }

    /**
     * Negates both components and returns the (modified) current instance.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f negateLocal() {
        x = -x;
        y = -y;
        return this;
    }

    /**
     * Subtracts the argument and returns the difference as a new instance. The
     * current instance is unaffected.
     *
     * @param vec the vector to subtract (not null, unaffected)
     * @return a new Vector2f
     */
    public Vector2f subtract(Vector2f vec) {
        return subtract(vec, null);
    }

    /**
     * Subtracts the specified vector and returns the difference in a 3rd
     * vector. The current instance is unaffected unless it's {@code store}.
     *
     * <p>It is safe for {@code vec} and {@code store} to be the same object.
     *
     * @param vec the vector to subtract (not null, unaffected unless it's
     *     {@code store})
     * @param store storage for the difference, or null for a new Vector2f
     * @return either {@code store} or a new Vector2f
     */
    public Vector2f subtract(Vector2f vec, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        store.x = x - vec.x;
        store.y = y - vec.y;
        return store;
    }

    /**
     * Subtracts the specified amounts from the vector's components and returns
     * the difference as a new instance. The current instance is unaffected.
     *
     * @param valX the amount to subtract from the X component
     * @param valY the amount to subtract from the Y component
     * @return a new Vector2f
     */
    public Vector2f subtract(float valX, float valY) {
        return new Vector2f(x - valX, y - valY);
    }

    /**
     * Subtracts the argument and returns the (modified) current instance. If
     * the argument is null, null is returned.
     *
     * @param vec the vector to subtract (unaffected unless it's {@code this})
     *     or null for none
     * @return the (modified) current instance or null
     */
    public Vector2f subtractLocal(Vector2f vec) {
        if (null == vec) {
            logger.warning("Provided vector is null, null returned.");
            return null;
        }
        x -= vec.x;
        y -= vec.y;
        return this;
    }

    /**
     * Subtracts the specified amounts from the vector's components and returns
     * the (modified) current instance.
     *
     * @param valX the amount to subtract from the X component
     * @param valY the amount to subtract from the Y component
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f subtractLocal(float valX, float valY) {
        x -= valX;
        y -= valY;
        return this;
    }

    /**
     * Normalizes the vector to length=1 and returns the result as a new
     * instance. If the vector has length=0, a clone is returned. Either way,
     * the current instance is unaffected.
     *
     * @return a new Vector2f
     */
    public Vector2f normalize() {
        float length = length();
        if (length != 0) {
            return divide(length);
        }

        return divide(1);
    }

    /**
     * Normalizes the vector to length=1 and returns the (modified) current
     * instance. If the vector has length=0, it's unchanged.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f normalizeLocal() {
        float length = length();
        if (length != 0) {
            return divideLocal(length);
        }

        return divideLocal(1);
    }

    /**
     * Returns the unsigned angle between the current instance and the argument,
     * provided both vectors have length=1. If {@code otherVector} is null, Pi/2
     * is returned. The current instance is unaffected.
     *
     * @param otherVector the unit vector to compare (unaffected) or null for
     *     none
     * @return the angle in radians (not negative)
     */
    public float smallestAngleBetween(Vector2f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }

    /**
     * Returns the signed angle between the current instance and the argument.
     * The current instance is unaffected.
     *
     * @param otherVector the vector to compare (not null, unaffected)
     * @return the angle in radians, measured counter-clockwise from {@code
     *     this} to {@code otherVector} (&ge;-2*Pi, &le;2*Pi)
     */
    public float angleBetween(Vector2f otherVector) {
        float angle = FastMath.atan2(otherVector.y, otherVector.x)
                - FastMath.atan2(y, x);
        return angle;
    }

    /**
     * Returns the X component. The vector is unaffected.
     *
     * @return the value of the {@link #x} component
     */
    public float getX() {
        return x;
    }

    /**
     * Sets the X component.
     *
     * @param x the desired value
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f setX(float x) {
        this.x = x;
        return this;
    }

    /**
     * Returns the Y component. The vector is unaffected.
     *
     * @return the value of the {@link #y} component
     */
    public float getY() {
        return y;
    }

    /**
     * Sets the Y component.
     *
     * @param y the desired value
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f setY(float y) {
        this.y = y;
        return this;
    }

    /**
     * Returns the angle of the vector in polar coordinates. The current
     * instance is unaffected.
     *
     * @return the polar angle in radians, measured counter-clockwise from the
     *     +X axis (&ge;-Pi, &le;Pi)
     */
    public float getAngle() {
        return FastMath.atan2(y, x);
    }

    /**
     * Sets both components to zero.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Vector2f zero() {
        x = y = 0;
        return this;
    }

    /**
     * Returns a hash code. If two vectors are logically equivalent, they will
     * return the same hash code. The current instance is unaffected.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + Float.floatToIntBits(x);
        hash += 37 * hash + Float.floatToIntBits(y);
        return hash;
    }

    /**
     * Creates a copy. The current instance is unaffected.
     *
     * @return a new instance, equivalent to the current one
     */
    @Override
    public Vector2f clone() {
        try {
            return (Vector2f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    /**
     * Copies the vector to the array argument. The current instance is
     * unaffected.
     *
     * @param floats storage for the components (must have length&ge;2) or null
     *     for a new float[2]
     * @return an array containing the X and Y components in that order (either
     *     {@code floats} or a new float[2])
     */
    public float[] toArray(float[] floats) {
        if (floats == null) {
            floats = new float[2];
        }
        floats[0] = x;
        floats[1] = y;
        return floats;
    }

    /**
     * Tests for exact equality with the argument, distinguishing -0 from 0. If
     * {@code o} is null, false is returned. Either way, the current instance is
     * unaffected.
     *
     * @param o the object to compare (unaffected) or null for none
     * @return true if equal, otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector2f)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        Vector2f comp = (Vector2f) o;
        if (Float.compare(x, comp.x) != 0) {
            return false;
        }
        if (Float.compare(y, comp.y) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Tests for approximate equality with the specified vector, using the
     * specified tolerance. If {@code other} is null, false is returned. Either
     * way, the current instance is unaffected.
     *
     * @param other the vector to compare (unaffected) or null for none
     * @param epsilon the tolerance for each component
     * @return true if both components are within tolerance, otherwise false
     */
    public boolean isSimilar(Vector2f other, float epsilon) {
        if (other == null) {
            return false;
        }
        if (Float.compare(Math.abs(other.x - x), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.y - y), epsilon) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string representation of the vector. The current instance is
     * unaffected. The format is:
     *
     * <p>(XX.XXXX, YY.YYYY)
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /**
     * Sets the vector from an {@code ObjectInput} object.
     *
     * <p>Used with serialization. Shouldn't be invoked directly by application
     * code.
     *
     * @param in the object to read from (not null)
     * @throws IOException if the ObjectInput cannot read a float
     * @throws ClassNotFoundException never
     * @see java.io.Externalizable
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        x = in.readFloat();
        y = in.readFloat();
    }

    /**
     * Writes the vector to an {@code ObjectOutput} object. The current instance
     * is unaffected.
     *
     * <p>Used with serialization. Shouldn't be invoked directly by application
     * code.
     *
     * @param out the object to write to (not null)
     * @throws IOException if the ObjectOuput cannot write a float
     * @see java.io.Externalizable
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
    }

    /**
     * Serializes the vector to the specified exporter, for example when saving
     * to a J3O file. The current instance is unaffected.
     *
     * @param e the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(x, "x", 0);
        capsule.write(y, "y", 0);
    }

    /**
     * De-serializes the vector from the specified importer, for example
     * when loading from a J3O file.
     *
     * @param importer the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        x = capsule.readFloat("x", 0);
        y = capsule.readFloat("y", 0);
    }

    /**
     * Rotates the vector around (0,0) by the specified angle.
     *
     * @param angle the rotation angle (in radians)
     * @param cw true to rotate clockwise, false to rotate counter-clockwise
     */
    public void rotateAroundOrigin(float angle, boolean cw) {
        if (cw) {
            angle = -angle;
        }
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        float newX = cos * x - sin * y;
        float newY = sin * x + cos * y;
        x = newX;
        y = newY;
    }
}
