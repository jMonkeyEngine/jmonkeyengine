/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.collision.shapes.infos;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.*;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 *
 * @author normenhansen
 */
public class ChildCollisionShape implements Savable {

    public Vector3f location;
    public Matrix3f rotation;
    public CollisionShape shape;

    public ChildCollisionShape() {
    }

    public ChildCollisionShape(Vector3f location, Matrix3f rotation, CollisionShape shape) {
        this.location = location;
        this.rotation = rotation;
        this.shape = shape;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(location, "location", new Vector3f());
        capsule.write(rotation, "rotation", new Matrix3f());
        capsule.write(shape, "shape", new BoxCollisionShape(new Vector3f(1, 1, 1)));
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        location = (Vector3f) capsule.readSavable("location", new Vector3f());
        rotation = (Matrix3f) capsule.readSavable("rotation", new Matrix3f());
        shape = (CollisionShape) capsule.readSavable("shape", new BoxCollisionShape(new Vector3f(1, 1, 1)));
    }
}
