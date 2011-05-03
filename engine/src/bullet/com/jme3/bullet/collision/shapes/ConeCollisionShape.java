/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        objectId = createShape(axis, radius, height);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Shape {0}", Long.toHexString(objectId));
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
