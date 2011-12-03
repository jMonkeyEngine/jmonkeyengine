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
package com.jme3.bullet.objects;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <i>From Bullet manual:</i><br>
 * GhostObject can keep track of all objects that are overlapping.
 * By default, this overlap is based on the AABB.
 * This is useful for creating a character controller,
 * collision sensors/triggers, explosions etc.<br>
 * @author normenhansen
 */
public class PhysicsGhostObject extends PhysicsCollisionObject {

    protected boolean locationDirty = false;
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();
    private List<PhysicsCollisionObject> overlappingObjects = new LinkedList<PhysicsCollisionObject>();

    public PhysicsGhostObject() {
    }

    public PhysicsGhostObject(CollisionShape shape) {
        collisionShape = shape;
        buildObject();
    }

    public PhysicsGhostObject(Spatial child, CollisionShape shape) {
        collisionShape = shape;
        buildObject();
    }

    protected void buildObject() {
        if (objectId == 0) {
//            gObject = new PairCachingGhostObject();
            objectId = createGhostObject();
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Ghost Object {0}", Long.toHexString(objectId));
            setGhostFlags(objectId);
            initUserPointer();
        }
//        if (gObject == null) {
//            gObject = new PairCachingGhostObject();
//            gObject.setCollisionFlags(gObject.getCollisionFlags() | CollisionFlags.NO_CONTACT_RESPONSE);
//        }
        attachCollisionShape(objectId, collisionShape.getObjectId());
    }

    private native long createGhostObject();

    private native void setGhostFlags(long objectId);

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (objectId == 0) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Sets the physics object location
     * @param location the location of the actual physics object
     */
    public void setPhysicsLocation(Vector3f location) {
        setPhysicsLocation(objectId, location);
    }

    private native void setPhysicsLocation(long objectId, Vector3f location);

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Matrix3f rotation);

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Quaternion rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Quaternion rotation);

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        getPhysicsLocation(objectId, trans);
        return trans;
    }

    private native void getPhysicsLocation(long objectId, Vector3f vector);

    /**
     * @return the physicsLocation
     */
    public Quaternion getPhysicsRotation(Quaternion rot) {
        if (rot == null) {
            rot = new Quaternion();
        }
        getPhysicsRotation(objectId, rot);
        return rot;
    }

    private native void getPhysicsRotation(long objectId, Quaternion rot);

    /**
     * @return the physicsLocation
     */
    public Matrix3f getPhysicsRotationMatrix(Matrix3f rot) {
        if (rot == null) {
            rot = new Matrix3f();
        }
        getPhysicsRotationMatrix(objectId, rot);
        return rot;
    }

    private native void getPhysicsRotationMatrix(long objectId, Matrix3f rot);

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation() {
        Vector3f vec = new Vector3f();
        getPhysicsLocation(objectId, vec);
        return vec;
    }

    /**
     * @return the physicsLocation
     */
    public Quaternion getPhysicsRotation() {
        Quaternion quat = new Quaternion();
        getPhysicsRotation(objectId, quat);
        return quat;
    }

    public Matrix3f getPhysicsRotationMatrix() {
        Matrix3f mtx = new Matrix3f();
        getPhysicsRotationMatrix(objectId, mtx);
        return mtx;
    }

    /**
     * used internally
     */
//    public PairCachingGhostObject getObjectId() {
//        return gObject;
//    }
    /**
     * destroys this PhysicsGhostNode and removes it from memory
     */
    public void destroy() {
    }

    /**
     * Another Object is overlapping with this GhostNode,
     * if and if only there CollisionShapes overlaps.
     * They could be both regular PhysicsRigidBodys or PhysicsGhostObjects.
     * @return All CollisionObjects overlapping with this GhostNode.
     */
    public List<PhysicsCollisionObject> getOverlappingObjects() {
        overlappingObjects.clear();
        getOverlappingObjects(objectId);
//        for (com.bulletphysics.collision.dispatch.CollisionObject collObj : gObject.getOverlappingPairs()) {
//            overlappingObjects.add((PhysicsCollisionObject) collObj.getUserPointer());
//        }
        return overlappingObjects;
    }

    protected native void getOverlappingObjects(long objectId);

    private void addOverlappingObject_native(PhysicsCollisionObject co) {
        overlappingObjects.add(co);
    }

    /**
     *
     * @return With how many other CollisionObjects this GhostNode is currently overlapping.
     */
    public int getOverlappingCount() {
        return getOverlappingCount(objectId);
    }

    private native int getOverlappingCount(long objectId);

    /**
     *
     * @param index The index of the overlapping Node to retrieve.
     * @return The Overlapping CollisionObject at the given index.
     */
    public PhysicsCollisionObject getOverlapping(int index) {
        return overlappingObjects.get(index);
    }

    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

    public void setCcdMotionThreshold(float threshold) {
        setCcdMotionThreshold(objectId, threshold);
    }

    private native void setCcdMotionThreshold(long objectId, float threshold);

    public float getCcdSweptSphereRadius() {
        return getCcdSweptSphereRadius(objectId);
    }

    private native float getCcdSweptSphereRadius(long objectId);

    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    public float getCcdSquareMotionThreshold() {
        return getCcdSquareMotionThreshold(objectId);
    }

    private native float getCcdSquareMotionThreshold(long objectId);

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()), "physicsRotation", new Matrix3f());
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        buildObject();
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
        setPhysicsRotation(((Matrix3f) capsule.readSavable("physicsRotation", new Matrix3f())));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));
    }
}
