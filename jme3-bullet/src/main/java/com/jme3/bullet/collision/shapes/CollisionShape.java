/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract base class for collision shapes based on Bullet's
 * btCollisionShape.
 * <p>
 * Collision shapes include BoxCollisionShape and CapsuleCollisionShape. As
 * suggested in the Bullet manual, a single collision shape can be shared among
 * multiple collision objects.
 *
 * @author normenhansen
 */
public abstract class CollisionShape implements Savable {

    /**
     * default margin for new non-sphere/non-capsule shapes (in physics-space
     * units, &gt;0, default=0.04)
     */
    private static float defaultMargin = 0.04f;
    /**
     * unique identifier of the btCollisionShape
     * <p>
     * Constructors are responsible for setting this to a non-zero value. After
     * that, the id never changes.
     */
    protected long objectId = 0;
    /**
     * copy of scaling factors: one for each local axis (default=1,1,1)
     */
    protected Vector3f scale = new Vector3f(1, 1, 1);
    /**
     * copy of collision margin (in physics-space units, &gt;0, default=0)
     */
    protected float margin = defaultMargin;

    public CollisionShape() {
    }

//    /**
//     * used internally, not safe
//     */
//    public void calculateLocalInertia(long objectId, float mass) {
//        if (this.objectId == 0) {
//            return;
//        }
////        if (this instanceof MeshCollisionShape) {
////            vector.set(0, 0, 0);
////        } else {
//        calculateLocalInertia(objectId, this.objectId, mass);
////            objectId.calculateLocalInertia(mass, vector);
////        }
//    }
//
//    private native void calculateLocalInertia(long objectId, long shapeId, float mass);

    /**
     * Read the id of the btCollisionShape.
     *
     * @return the unique identifier (not zero)
     */
    public long getObjectId() {
        return objectId;
    }

    /**
     * used internally
     */
    public void setObjectId(long id) {
        this.objectId = id;
    }

    /**
     * Alter the scaling factors of this shape. CAUTION: Not all shapes can be
     * scaled.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) changes can have unintended consequences.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=1,1,1)
     */
    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        setLocalScaling(objectId, scale);
    }
    /**
     * Access the scaling factors.
     *
     * @return the pre-existing vector (not null)
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Test whether this shape can be applied to a dynamic rigid body. The only
     * non-moving shapes are the heightfield, mesh, and plane shapes.
     *
     * @return true if non-moving, false otherwise
     */
    public boolean isNonMoving() {
        boolean result = isNonMoving(objectId);
        return result;
    }

    native private boolean isNonMoving(long objectId);

    /**
     * Read the collision margin for this shape.
     *
     * @return the margin distance (in physics-space units, &ge;0)
     */
    public float getMargin() {
        return getMargin(objectId);
    }

    private native float getMargin(long objectId);

    /**
     * Alter the default margin for new shapes that are neither capsules nor
     * spheres.
     *
     * @param margin the desired margin distance (in physics-space units, &gt;0,
     * default=0.04)
     */
    public static void setDefaultMargin(float margin) {
        defaultMargin = margin;
    }

    /**
     * Read the default margin for new shapes.
     *
     * @return margin the default margin distance (in physics-space units,
     * &gt;0)
     */
    public static float getDefaultMargin() {
        return defaultMargin;
    }

    /**
     * Alter the collision margin of this shape. CAUTION: Margin is applied
     * differently, depending on the type of shape. Generally the collision
     * margin expands the object, creating a gap. Don't set the collision margin
     * to zero.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) changes can have unintended consequences.
     *
     * @param margin the desired margin distance (in physics-space units, &gt;0,
     * default=0.04)
     */
    public void setMargin(float margin) {
        setMargin(objectId, margin);
        this.margin = margin;
    }

    private native void setLocalScaling(long obectId, Vector3f scale);

    private native void setMargin(long objectId, float margin);

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(scale, "scale", new Vector3f(1, 1, 1));
        capsule.write(getMargin(), "margin", 0.0f);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.scale = (Vector3f) capsule.readSavable("scale", new Vector3f(1, 1, 1));
        this.margin = capsule.readFloat("margin", 0.0f);
    }

    /**
     * Finalize this shape just before it is destroyed. Should be invoked only
     * by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing CollisionShape {0}", Long.toHexString(objectId));
        finalizeNative(objectId);
    }

    private native void finalizeNative(long objectId);
}
