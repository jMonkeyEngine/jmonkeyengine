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
package com.jme3.effect.shapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;

/**
 * An {@link EmitterShape} that emits particles randomly within the bounds of an axis-aligned box.
 * The box is defined by a minimum corner and a length vector.
 */
public class EmitterBoxShape implements EmitterShape {

    /**
     * The minimum corner of the box.
     */
    private Vector3f min;
    /**
     * The length of the box along each axis.  The x, y, and z components of this
     * vector represent the width, height, and depth of the box, respectively.
     */
    private Vector3f len;

    /**
     * For serialization only. Do not use.
     */
    public EmitterBoxShape() {
    }

    /**
     * Constructs an {@code EmitterBoxShape} defined by a minimum and maximum corner.
     *
     * @param min The minimum corner of the box. Not null.
     * @param max The maximum corner of the box. Not null.
     * @throws IllegalArgumentException If either {@code min} or {@code max} is null.
     */
    public EmitterBoxShape(Vector3f min, Vector3f max) {
        if (min == null || max == null) {
            throw new IllegalArgumentException("min or max cannot be null");
        }

        this.min = min;
        this.len = new Vector3f();
        this.len.set(max).subtractLocal(min);
    }

    /**
     * Generates a random point within the bounds of the box.
     * The generated point is stored in the provided {@code store} vector.
     *
     * @param store The {@link Vector3f} to store the generated point in.
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        store.x = min.x + len.x * FastMath.nextRandomFloat();
        store.y = min.y + len.y * FastMath.nextRandomFloat();
        store.z = min.z + len.z * FastMath.nextRandomFloat();
    }

    /**
     * For a box shape, the normal is not well-defined for points within the volume.
     * This implementation simply calls {@link #getRandomPoint(Vector3f)} and does not modify the provided normal.
     *
     * @param store  The {@link Vector3f} to store the generated point in.
     * @param normal The {@link Vector3f} to store the generated normal in (unused).
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        this.getRandomPoint(store);
    }

    @Override
    public EmitterShape deepClone() {
        try {
            EmitterBoxShape clone = (EmitterBoxShape) super.clone();
            clone.min = min.clone();
            clone.len = len.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.min = cloner.clone(min);
        this.len = cloner.clone(len);
    }

    /**
     * Returns the minimum corner of the emitting box.
     *
     * @return The minimum corner.
     */
    public Vector3f getMin() {
        return min;
    }

    /**
     * Sets the minimum corner of the emitting box.
     *
     * @param min The new minimum corner.
     */
    public void setMin(Vector3f min) {
        this.min = min;
    }

    /**
     * Returns the length vector of the emitting box. This vector represents the
     * extent of the box along each axis (length = max - min).
     *
     * @return The length vector.
     */
    public Vector3f getLen() {
        return len;
    }

    /**
     * Sets the length vector of the emitting box. This vector should represent
     * the extent of the box along each axis (length = max - min).
     *
     * @param len The new length vector.
     */
    public void setLen(Vector3f len) {
        this.len = len;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(min, "min", null);
        oc.write(len, "length", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        min = (Vector3f) ic.readSavable("min", null);
        len = (Vector3f) ic.readSavable("length", null);
    }
}
