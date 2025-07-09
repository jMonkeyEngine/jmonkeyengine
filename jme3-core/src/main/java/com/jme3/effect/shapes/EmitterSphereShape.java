/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
 * An {@link EmitterShape} that emits particles randomly from within the volume of a sphere.
 * The sphere is defined by a center point and a radius.
 */
public class EmitterSphereShape implements EmitterShape {

    /**
     * The center point of the sphere.
     */
    private Vector3f center;
    /**
     * The radius of the sphere.
     */
    private float radius;

    /**
     * For serialization only. Do not use.
     */
    public EmitterSphereShape() {
    }

    /**
     * Constructs an {@code EmitterSphereShape} with the given center and radius.
     *
     * @param center The center point of the sphere.
     * @param radius The radius of the sphere.
     * @throws IllegalArgumentException If {@code center} is null, or if {@code radius} is not greater than 0.
     */
    public EmitterSphereShape(Vector3f center, float radius) {
        if (center == null) {
            throw new IllegalArgumentException("center cannot be null");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }

        this.center = center;
        this.radius = radius;
    }

    @Override
    public EmitterShape deepClone() {
        try {
            EmitterSphereShape clone = (EmitterSphereShape) super.clone();
            clone.center = center.clone();
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
        this.center = cloner.clone(center);
    }

    /**
     * Generates a random point within the volume of the sphere.
     *
     * @param store The {@link Vector3f} to store the generated point in.
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        do {
            store.x = (FastMath.nextRandomFloat() * 2f - 1f);
            store.y = (FastMath.nextRandomFloat() * 2f - 1f);
            store.z = (FastMath.nextRandomFloat() * 2f - 1f);
        } while (store.lengthSquared() > 1);
        store.multLocal(radius);
        store.addLocal(center);
    }

    /**
     * For a sphere shape, the normal is not well-defined for points within the volume.
     * This implementation simply calls {@link #getRandomPoint(Vector3f)} and does not modify the provided normal.
     *
     * @param store  The {@link Vector3f} to store the generated point in.
     * @param normal The {@link Vector3f} to store the generated normal in (unused).
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        this.getRandomPoint(store);
        normal.set(store).subtractLocal(center).normalizeLocal();
    }

    /**
     * Returns the center point of the sphere.
     *
     * @return The center point.
     */
    public Vector3f getCenter() {
        return center;
    }

    /**
     * Sets the center point of the sphere.
     *
     * @param center The new center point.
     */
    public void setCenter(Vector3f center) {
        this.center = center;
    }

    /**
     * Returns the radius of the sphere.
     *
     * @return The radius.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Sets the radius of the sphere.
     *
     * @param radius The new radius.
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(center, "center", null);
        oc.write(radius, "radius", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        center = (Vector3f) ic.readSavable("center", null);
        radius = ic.readFloat("radius", 0);
    }
}
