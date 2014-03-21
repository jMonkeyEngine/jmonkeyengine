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
package com.jme3.bullet.collision.shapes;

import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.ConeShapeX;
import com.bulletphysics.collision.shapes.ConeShapeZ;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Cone collision shape represents a 3D cone with a radius, height, and axis (X, Y or Z).
 *
 * @author normenhansen
 */
public class ConeCollisionShape extends CollisionShape {

    protected float radius;
    protected float height;
    protected int axis;

    /**
     * Serialization only, do not use.
     */
    public ConeCollisionShape() {
    }

    /**
     * Creates a new cone collision shape with the given height, radius, and axis.
     *
     * @param radius The radius of the cone in world units.
     * @param height The height of the cone in world units.
     * @param The axis towards which the cone faces, see the PhysicsSpace.AXIS_* constants.
     */
    public ConeCollisionShape(float radius, float height, int axis) {
        this.radius = radius;
        this.height = height;
        this.axis = axis;
        if (axis < PhysicsSpace.AXIS_X || axis > PhysicsSpace.AXIS_Z) {
            throw new UnsupportedOperationException("axis must be one of the PhysicsSpace.AXIS_* constants!");
        }
        createShape();
    }

    /**
     * Creates a new cone collision shape with the given height, radius and default Y axis.
     *
     * @param radius The radius of the cone in world units.
     * @param height The height of the cone in world units.
     */
    public ConeCollisionShape(float radius, float height) {
        this.radius = radius;
        this.height = height;
        this.axis = PhysicsSpace.AXIS_Y;
        createShape();
    }

    public float getRadius() {
        return radius;
    }
    
    public float getHeight() {
        return height;
    }
    
    public int getAxis() {
        return axis;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 0.5f);
        capsule.write(axis, "axis", PhysicsSpace.AXIS_Y);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        radius = capsule.readFloat("radius", 0.5f);
        height = capsule.readFloat("height", 0.5f);
        axis = capsule.readInt("axis", PhysicsSpace.AXIS_Y);
        createShape();
    }

    protected void createShape() {
        if (axis == PhysicsSpace.AXIS_X) {
            cShape = new ConeShapeX(radius, height);
        } else if (axis == PhysicsSpace.AXIS_Y) {
            cShape = new ConeShape(radius, height);
        } else if (axis == PhysicsSpace.AXIS_Z) {
            cShape = new ConeShapeZ(radius, height);
        } else {
            throw new UnsupportedOperationException("Unexpected axis: " + axis);
        }
        cShape.setLocalScaling(Converter.convert(getScale()));
        cShape.setMargin(margin);
    }
}
