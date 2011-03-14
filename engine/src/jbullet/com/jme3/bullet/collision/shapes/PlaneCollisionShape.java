/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.bullet.collision.shapes;

import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Plane;
import java.io.IOException;

/**
 *
 * @author normenhansen
 */
public class PlaneCollisionShape extends CollisionShape{
    private Plane plane;

    public PlaneCollisionShape() {
    }

    /**
     * Creates a plane Collision shape
     * @param plane the plane that defines the shape
     */
    public PlaneCollisionShape(Plane plane) {
        this.plane = plane;
        createShape();
    }

    public final Plane getPlane() {
        return plane;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(plane, "collisionPlane", new Plane());
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        plane = (Plane) capsule.readSavable("collisionPlane", new Plane());
        createShape();
    }

    protected void createShape() {
        cShape = new StaticPlaneShape(Converter.convert(plane.getNormal()),plane.getConstant());
        cShape.setLocalScaling(Converter.convert(getScale()));
        cShape.setMargin(margin);
    }

}
