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
import com.jme3.math.Transform;

/**
 * Started Date: Jul 16, 2004<br>
 * <br>
 * Represents a translation, rotation and scale in one object.
 * 
 * This class's only purpose is to give better accuracy in floating point operations during computations.
 * This is made by copying the original Transfrom class from jme3 core and removing unnecessary methods so that
 * the class is smaller and easier to maintain.
 * Should any other methods be needed, they will be added.
 * 
 * @author Jack Lindamood
 * @author Joshua Slack
 * @author Marcin Roguski (Kaelthas)
 */
public final class DTransform implements Savable, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 7812915425940606722L;

    private DQuaternion       rotation;
    private Vector3d          translation;
    private Vector3d          scale;

    public DTransform() {
        translation = new Vector3d();
        rotation = new DQuaternion();
        scale = new Vector3d();
    }
    
    public DTransform(Transform transform) {
        translation = new Vector3d(transform.getTranslation());
        rotation = new DQuaternion(transform.getRotation());
        scale = new Vector3d(transform.getScale());
    }

    public Transform toTransform() {
        return new Transform(translation.toVector3f(), rotation.toQuaternion(), scale.toVector3f());
    }
    
    public Matrix toMatrix() {
        Matrix m = Matrix.identity(4);
        m.setTranslation(translation);
        m.setRotationQuaternion(rotation);
        m.setScale(scale);
        return m;
    }
    
    /**
     * Sets this translation to the given value.
     * @param trans
     *            The new translation for this matrix.
     * @return this
     */
    public DTransform setTranslation(Vector3d trans) {
        translation.set(trans);
        return this;
    }

    /**
     * Sets this rotation to the given DQuaternion value.
     * @param rot
     *            The new rotation for this matrix.
     * @return this
     */
    public DTransform setRotation(DQuaternion rot) {
        rotation.set(rot);
        return this;
    }

    /**
     * Sets this scale to the given value.
     * @param scale
     *            The new scale for this matrix.
     * @return this
     */
    public DTransform setScale(Vector3d scale) {
        this.scale.set(scale);
        return this;
    }

    /**
     * Sets this scale to the given value.
     * @param scale
     *            The new scale for this matrix.
     * @return this
     */
    public DTransform setScale(float scale) {
        this.scale.set(scale, scale, scale);
        return this;
    }

    /**
     * Return the translation vector in this matrix.
     * @return translation vector.
     */
    public Vector3d getTranslation() {
        return translation;
    }

    /**
     * Return the rotation quaternion in this matrix.
     * @return rotation quaternion.
     */
    public DQuaternion getRotation() {
        return rotation;
    }

    /**
     * Return the scale vector in this matrix.
     * @return scale vector.
     */
    public Vector3d getScale() {
        return scale;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ " + translation.x + ", " + translation.y + ", " + translation.z + "]\n" + "[ " + rotation.x + ", " + rotation.y + ", " + rotation.z + ", " + rotation.w + "]\n" + "[ " + scale.x + " , " + scale.y + ", " + scale.z + "]";
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(rotation, "rot", new DQuaternion());
        capsule.write(translation, "translation", Vector3d.ZERO);
        capsule.write(scale, "scale", Vector3d.UNIT_XYZ);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);

        rotation = (DQuaternion) capsule.readSavable("rot", new DQuaternion());
        translation = (Vector3d) capsule.readSavable("translation", Vector3d.ZERO);
        scale = (Vector3d) capsule.readSavable("scale", Vector3d.UNIT_XYZ);
    }

    @Override
    public DTransform clone() {
        try {
            DTransform tq = (DTransform) super.clone();
            tq.rotation = rotation.clone();
            tq.scale = scale.clone();
            tq.translation = translation.clone();
            return tq;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
