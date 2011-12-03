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
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.util.Converter;
import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * This Object holds information about a jbullet CollisionShape to be able to reuse
 * CollisionShapes (as suggested in bullet manuals)
 * TODO: add static methods to create shapes from nodes (like jbullet-jme constructor)
 * @author normenhansen
 */
public abstract class CollisionShape implements Savable {

    protected com.bulletphysics.collision.shapes.CollisionShape cShape;
    protected Vector3f scale = new Vector3f(1, 1, 1);
    protected float margin = 0.0f;

    public CollisionShape() {
    }

    /**
     * used internally, not safe
     */
    public void calculateLocalInertia(float mass, javax.vecmath.Vector3f vector) {
        if (cShape == null) {
            return;
        }
        if (this instanceof MeshCollisionShape) {
            vector.set(0, 0, 0);
        } else {
            cShape.calculateLocalInertia(mass, vector);
        }
    }

    /**
     * used internally
     */
    public com.bulletphysics.collision.shapes.CollisionShape getCShape() {
        return cShape;
    }

    /**
     * used internally
     */
    public void setCShape(com.bulletphysics.collision.shapes.CollisionShape cShape) {
        this.cShape = cShape;
    }

    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        cShape.setLocalScaling(Converter.convert(scale));
    }

    public float getMargin() {
        return cShape.getMargin();
    }

    public void setMargin(float margin) {
        cShape.setMargin(margin);
        this.margin = margin;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(scale, "scale", new Vector3f(1, 1, 1));
        capsule.write(getMargin(), "margin", 0.0f);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.scale = (Vector3f) capsule.readSavable("scale", new Vector3f(1, 1, 1));
        this.margin = capsule.readFloat("margin", 0.0f);
    }
}
