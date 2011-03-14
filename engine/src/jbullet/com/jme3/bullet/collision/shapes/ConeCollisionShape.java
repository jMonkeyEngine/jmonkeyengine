/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author normenhansen
 */
public class ConeCollisionShape extends CollisionShape {

    protected float radius;
    protected float height;
    protected int axis;

    public ConeCollisionShape() {
    }

    public ConeCollisionShape(float radius, float height, int axis) {
        this.radius = radius;
        this.height = radius;
        this.axis = axis;
        createShape();
    }

    public ConeCollisionShape(float radius, float height) {
        this.radius = radius;
        this.height = radius;
        this.axis = PhysicsSpace.AXIS_Y;
        createShape();
    }

    public float getRadius() {
        return radius;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 0.5f);
        capsule.write(axis, "axis", 0.5f);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        radius = capsule.readFloat("radius", 0.5f);
        radius = capsule.readFloat("height", 0.5f);
        radius = capsule.readFloat("axis", 0.5f);
        createShape();
    }

    protected void createShape() {
        if (axis == PhysicsSpace.AXIS_X) {
            cShape = new ConeShapeX(radius, height);
        } else if (axis == PhysicsSpace.AXIS_Y) {
            cShape = new ConeShape(radius, height);
        } else if (axis == PhysicsSpace.AXIS_Z) {
            cShape = new ConeShapeZ(radius, height);
        }
        cShape.setLocalScaling(Converter.convert(getScale()));
        cShape.setMargin(margin);
    }
}
