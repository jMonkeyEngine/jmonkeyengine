/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.bullet.collision.shapes.infos;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.*;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * An element of a CompoundCollisionShape, consisting of a (non-compound) child
 * shape, offset and rotated with respect to its parent.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class ChildCollisionShape implements Savable {

    /**
     * translation relative to parent shape (not null)
     */
    public Vector3f location;
    /**
     * rotation relative to parent shape (not null)
     */
    public Matrix3f rotation;
    /**
     * base shape (not null, not a compound shape)
     */
    public CollisionShape shape;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected ChildCollisionShape() {
    }

    /**
     * Instantiate a child shape for use in a compound shape.
     *
     * @param location translation relative to the parent (not null, alias
     * created)
     * @param rotation rotation relative to the parent (not null, alias created)
     * @param shape the base shape (not null, not a compound shape, alias
     * created)
     */
    public ChildCollisionShape(Vector3f location, Matrix3f rotation, CollisionShape shape) {
        this.location = location;
        this.rotation = rotation;
        this.shape = shape;
    }

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(location, "location", new Vector3f());
        capsule.write(rotation, "rotation", new Matrix3f());
        capsule.write(shape, "shape", new BoxCollisionShape(new Vector3f(1, 1, 1)));
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
        location = (Vector3f) capsule.readSavable("location", new Vector3f());
        rotation = (Matrix3f) capsule.readSavable("rotation", new Matrix3f());
        shape = (CollisionShape) capsule.readSavable("shape", new BoxCollisionShape(new Vector3f(1, 1, 1)));
    }
}
