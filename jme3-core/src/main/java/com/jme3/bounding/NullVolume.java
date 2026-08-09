package com.jme3.bounding;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.math.*;
import com.jme3.util.struct.FieldSequence;

import java.nio.FloatBuffer;

/**
 * Volume that cannot be collided with. Geometries with this volume can never be visible.
 */
public class NullVolume extends BoundingVolume {

    public static final NullVolume INSTANCE = new NullVolume();

    @Override
    public Type getType() {
        return Type.Null;
    }

    @Override
    public BoundingVolume transform(Transform trans, BoundingVolume store) {
        return this;
    }

    @Override
    public BoundingVolume transform(Matrix4f trans, BoundingVolume store) {
        return this;
    }

    @Override
    public Plane.Side whichSide(Plane plane) {
        return Plane.Side.None;
    }

    @Override
    public void computeFromPoints(FloatBuffer points) {}

    @Override
    public void computeFromPoints(FieldSequence<Vector3f> field) {}

    @Override
    public BoundingVolume merge(BoundingVolume volume) {
        return volume.clone();
    }

    @Override
    public BoundingVolume mergeLocal(BoundingVolume volume) {
        return this;
    }

    @Override
    public BoundingVolume clone(BoundingVolume store) {
        return this;
    }

    @Override
    public float distanceToEdge(Vector3f point) {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public boolean intersects(BoundingVolume bv) {
        return false;
    }

    @Override
    public boolean intersects(Ray ray) {
        return false;
    }

    @Override
    public boolean intersectsSphere(BoundingSphere bs) {
        return false;
    }

    @Override
    public boolean intersectsBoundingBox(BoundingBox bb) {
        return false;
    }

    @Override
    public boolean contains(Vector3f point) {
        return false;
    }

    @Override
    public boolean intersects(Vector3f point) {
        return false;
    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        return 0;
    }

}
