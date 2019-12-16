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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A conical collision shape based on Bullet's btConeShapeX, btConeShape, or
 * btConeShapeZ.
 *
 * @author normenhansen
 */
public class ConeCollisionShape extends CollisionShape {

    /**
     * copy of radius (&ge;0)
     */
    protected float radius;
    /**
     * copy of height (&ge;0)
     */
    protected float height;
    /**
     * copy of main (height) axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     */
    protected int axis;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected ConeCollisionShape() {
    }

    /**
     * Instantiate a cone shape around the specified main (height) axis.
     *
     * @param radius the desired radius (&ge;0)
     * @param height the desired height (&ge;0)
     * @param axis which local axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public ConeCollisionShape(float radius, float height, int axis) {
        this.radius = radius;
        this.height = height;
        this.axis = axis;
        createShape();
    }

    /**
     * Instantiate a cone shape oriented along the Y axis.
     *
     * @param radius the desired radius (&ge;0)
     * @param height the desired height (&ge;0)
     */
    public ConeCollisionShape(float radius, float height) {
        this.radius = radius;
        this.height = height;
        this.axis = PhysicsSpace.AXIS_Y;
        createShape();
    }

    /**
     * Read the radius of the cone.
     *
     * @return radius (&ge;0)
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Read the height of the cone.
     *
     * @return height (&ge;0)
     */
    public float getHeight() {
        return height;
    }

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 0.5f);
        capsule.write(axis, "axis", PhysicsSpace.AXIS_Y);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        radius = capsule.readFloat("radius", 0.5f);
        height = capsule.readFloat("height", 0.5f);
        axis = capsule.readInt("axis", PhysicsSpace.AXIS_Y);
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape() {
        objectId = createShape(axis, radius, height);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
//        if (axis == PhysicsSpace.AXIS_X) {
//            objectId = new ConeShapeX(radius, height);
//        } else if (axis == PhysicsSpace.AXIS_Y) {
//            objectId = new ConeShape(radius, height);
//        } else if (axis == PhysicsSpace.AXIS_Z) {
//            objectId = new ConeShapeZ(radius, height);
//        }
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(int axis, float radius, float height);
}
