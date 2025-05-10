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
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;

/**
 * An {@link EmitterShape} that emits particles from a single point in space.
 */
public class EmitterPointShape implements EmitterShape {

    /**
     * The point in space from which particles are emitted.
     */
    private Vector3f point;

    /**
     * For serialization only. Do not use.
     */
    public EmitterPointShape() {
    }

    /**
     * Constructs an {@code EmitterPointShape} with the given point.
     *
     * @param point The point from which particles are emitted.
     */
    public EmitterPointShape(Vector3f point) {
        this.point = point;
    }

    @Override
    public EmitterShape deepClone() {
        try {
            EmitterPointShape clone = (EmitterPointShape) super.clone();
            clone.point = point.clone();
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
        this.point = cloner.clone(point);
    }

    /**
     * For a point shape, the generated point is
     * always the shape's defined point.
     *
     * @param store The {@link Vector3f} to store the generated point in.
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        store.set(point);
    }

    /**
     * For a point shape, the generated point is always the shape's defined point.
     * The normal is not defined for a point shape, so this method does not modify the normal parameter.
     *
     * @param store  The {@link Vector3f} to store the generated point in.
     * @param normal The {@link Vector3f} to store the generated normal in (unused).
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        store.set(point);
    }

    /**
     * Returns the point from which particles are emitted.
     *
     * @return The point.
     */
    public Vector3f getPoint() {
        return point;
    }

    /**
     * Sets the point from which particles are emitted.
     *
     * @param point The new point.
     */
    public void setPoint(Vector3f point) {
        this.point = point;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(point, "point", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        this.point = (Vector3f) ic.readSavable("point", null);
    }
}
