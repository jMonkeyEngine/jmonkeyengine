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

/**
 * <code>Rectangle</code> defines a finite plane within three-dimensional space
 * that is specified via three points (A, B, C). These three points define a
 * triangle with the fourth point defining the rectangle (B + C) - A.
 *
 * <p>The corner points are named as follows:
 *
 * <pre>
 *     C +---+ D
 *       |   |
 *       |   |
 *       |   |
 *       |   |
 *     A +---+ B
 * </pre>
 *
 * <p>If angle BAC isn't exactly 90 degrees, then the resulting shape is
 * actually parallelogram, not a rectangle.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Rectangle implements Savable, Cloneable, java.io.Serializable {
    static final long serialVersionUID = 1;

    private Vector3f a, b, c;

    /**
     * Constructor creates a new <code>Rectangle</code> with no defined corners.
     * A, B, and C must be set to define a valid rectangle.
     */
    public Rectangle() {
        a = new Vector3f();
        b = new Vector3f();
        c = new Vector3f();
    }

    /**
     * Constructor creates a new <code>Rectangle</code> with defined A, B, and C
     * points that define the area of the rectangle.
     *
     * @param a   the first corner of the rectangle.
     * @param b   the second corner of the rectangle.
     * @param c   the third corner of the rectangle.
     */
    public Rectangle(Vector3f a, Vector3f b, Vector3f c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * <code>getA</code> returns the first point of the rectangle.
     *
     * @return the first point of the rectangle.
     */
    public Vector3f getA() {
        return a;
    }

    /**
     * <code>setA</code> sets the first point of the rectangle.
     *
     * @param a   the first point of the rectangle.
     */
    public void setA(Vector3f a) {
        this.a = a;
    }

    /**
     * <code>getB</code> returns the second point of the rectangle.
     *
     * @return the second point of the rectangle.
     */
    public Vector3f getB() {
        return b;
    }

    /**
     * <code>setB</code> sets the second point of the rectangle.
     *
     * @param b   the second point of the rectangle.
     */
    public void setB(Vector3f b) {
        this.b = b;
    }

    /**
     * <code>getC</code> returns the third point of the rectangle.
     *
     * @return the third point of the rectangle.
     */
    public Vector3f getC() {
        return c;
    }

    /**
     * <code>setC</code> sets the third point of the rectangle.
     *
     * @param c   the third point of the rectangle.
     */
    public void setC(Vector3f c) {
        this.c = c;
    }

    /**
     * Returns the coordinates of the 4th corner, calculated by the formula
     * D = (B + C) - A .
     *
     * @return the corner location (a new Vector3f)
     */
    public Vector3f calculateD() {
        float x = b.x + c.x - a.x;
        float y = b.y + c.y - a.y;
        float z = b.z + c.z - a.z;
        return new Vector3f(x, y, z);
    }

    /**
     * Returns a normal vector, calculated by the formula
     * <pre>
     *      (C - B) x (B - A)
     * N = -------------------
     *     |(C - B) x (B - A)|
     * </pre>
     *
     * @param normal storage for the normal, or null for a new Vector3f
     * @return the normal direction (either {@code normal} or a new Vector3f)
     */
    public Vector3f calculateNormal(Vector3f normal) {
        if (normal == null) {
            normal = new Vector3f();
        }

        Vector3f v1 = c.subtract(b);
        Vector3f v2 = a.subtract(b);
        normal.set(v1.crossLocal(v2).normalizeLocal());

        return normal;
    }

    /**
     * <code>random</code> returns a random point within the plane defined by:
     * A, B, C, and (B + C) - A.
     *
     * @return a random point within the rectangle.
     */
    public Vector3f random() {
        return random(null);
    }

    /**
     * <code>random</code> returns a random point within the plane defined by:
     * A, B, C, and (B + C) - A.
     *
     * @param result
     *            Vector to store result in
     * @return a random point within the rectangle.
     */
    public Vector3f random(Vector3f result) {
        if (result == null) {
            result = new Vector3f();
        }

        float s = FastMath.nextRandomFloat();
        float t = FastMath.nextRandomFloat();

        float aMod = 1.0f - s - t;
        result.set(a.mult(aMod).addLocal(b.mult(s).addLocal(c.mult(t))));
        return result;
    }

    /**
     * Serialize this rectangle to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param e (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(a, "a", Vector3f.ZERO);
        capsule.write(b, "b", Vector3f.ZERO);
        capsule.write(c, "c", Vector3f.ZERO);
    }

    /**
     * De-serialize this rectangle from the specified importer, for example
     * when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        a = (Vector3f) capsule.readSavable("a", Vector3f.ZERO.clone());
        b = (Vector3f) capsule.readSavable("b", Vector3f.ZERO.clone());
        c = (Vector3f) capsule.readSavable("c", Vector3f.ZERO.clone());
    }

    /**
     * Create a copy of this rectangle.
     *
     * @return a new instance, equivalent to this one
     */
    @Override
    public Rectangle clone() {
        try {
            Rectangle r = (Rectangle) super.clone();
            r.a = a.clone();
            r.b = b.clone();
            r.c = c.clone();
            return r;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
