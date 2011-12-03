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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>PhysicsRigidBody - Basic physics object</p>
 * @author normenhansen
 */
public class PhysicsRigidBody extends PhysicsCollisionObject {

    protected RigidBodyMotionState motionState = new RigidBodyMotionState();
    protected float mass = 1.0f;
    protected boolean kinematic = false;
    protected ArrayList<PhysicsJoint> joints = new ArrayList<PhysicsJoint>();

    public PhysicsRigidBody() {
    }

    /**
     * Creates a new PhysicsRigidBody with the supplied collision shape
     * @param child
     * @param shape
     */
    public PhysicsRigidBody(CollisionShape shape) {
        collisionShape = shape;
        rebuildRigidBody();
    }

    public PhysicsRigidBody(CollisionShape shape, float mass) {
        collisionShape = shape;
        this.mass = mass;
        rebuildRigidBody();
    }

    /**
     * Builds/rebuilds the phyiscs body when parameters have changed
     */
    protected void rebuildRigidBody() {
        boolean removed = false;
        if (collisionShape instanceof MeshCollisionShape && mass != 0) {
            throw new IllegalStateException("Dynamic rigidbody can not have mesh collision shape!");
        }
        if (objectId != 0) {
            if (isInWorld(objectId)) {
                PhysicsSpace.getPhysicsSpace().remove(this);
                removed = true;
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Clearing RigidBody {0}", Long.toHexString(objectId));
            finalizeNative(objectId);
        }
        preRebuild();
        objectId = createRigidBody(mass, motionState.getObjectId(), collisionShape.getObjectId());
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created RigidBody {0}", Long.toHexString(objectId));
        postRebuild();
        if (removed) {
            PhysicsSpace.getPhysicsSpace().add(this);
        }
    }

    protected void preRebuild() {
    }

    private native long createRigidBody(float mass, long motionStateId, long collisionShapeId);

    protected void postRebuild() {
        if (mass == 0.0f) {
            setStatic(objectId, true);
        } else {
            setStatic(objectId, false);
        }
        initUserPointer();
    }

    /**
     * @return the motionState
     */
    public RigidBodyMotionState getMotionState() {
        return motionState;
    }

    public boolean isInWorld() {
        return isInWorld(objectId);
    }

    private native boolean isInWorld(long objectId);

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

//    /**
//     * Gets the physics object location
//     * @param location the location of the actual physics object is stored in this Vector3f
//     */
//    public Vector3f getInterpolatedPhysicsLocation(Vector3f location) {
//        if (location == null) {
//            location = new Vector3f();
//        }
//        rBody.getInterpolationWorldTransform(tempTrans);
//        return Converter.convert(tempTrans.origin, location);
//    }
//
//    /**
//     * Gets the physics object rotation
//     * @param rotation the rotation of the actual physics object is stored in this Matrix3f
//     */
//    public Matrix3f getInterpolatedPhysicsRotation(Matrix3f rotation) {
//        if (rotation == null) {
//            rotation = new Matrix3f();
//        }
//        rBody.getInterpolationWorldTransform(tempTrans);
//        return Converter.convert(tempTrans.basis, rotation);
//    }
    /**
     * Sets the node to kinematic mode. in this mode the node is not affected by physics
     * but affects other physics objects. Iits kinetic force is calculated by the amount
     * of movement it is exposed to and its weight.
     * @param kinematic
     */
    public void setKinematic(boolean kinematic) {
        this.kinematic = kinematic;
        setKinematic(objectId, kinematic);
    }

    private native void setKinematic(long objectId, boolean kinematic);

    public boolean isKinematic() {
        return kinematic;
    }

    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

    /**
     * Sets the amount of motion that has to happen in one physics tick to trigger the continuous motion detection<br/>
     * This avoids the problem of fast objects moving through other objects, set to zero to disable (default)
     * @param threshold
     */
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

    public float getMass() {
        return mass;
    }

    /**
     * Sets the mass of this PhysicsRigidBody, objects with mass=0 are static.
     * @param mass
     */
    public void setMass(float mass) {
        this.mass = mass;
        if (collisionShape instanceof MeshCollisionShape && mass != 0) {
            throw new IllegalStateException("Dynamic rigidbody can not have mesh collision shape!");
        }
        if (objectId != 0) {
            if (collisionShape != null) {
                updateMassProps(objectId, collisionShape.getObjectId(), mass);
            }
            if (mass == 0.0f) {
                setStatic(objectId, true);
            } else {
                setStatic(objectId, false);
            }
        }
    }

    private native void setStatic(long objectId, boolean state);

    private native long updateMassProps(long objectId, long collisionShapeId, float mass);

    public Vector3f getGravity() {
        return getGravity(null);
    }

    public Vector3f getGravity(Vector3f gravity) {
        if (gravity == null) {
            gravity = new Vector3f();
        }
        getGravity(objectId, gravity);
        return gravity;
    }

    private native void getGravity(long objectId, Vector3f gravity);

    /**
     * Set the local gravity of this PhysicsRigidBody<br/>
     * Set this after adding the node to the PhysicsSpace,
     * the PhysicsSpace assigns its current gravity to the physics node when its added.
     * @param gravity the gravity vector to set
     */
    public void setGravity(Vector3f gravity) {
        setGravity(objectId, gravity);
    }

    private native void setGravity(long objectId, Vector3f gravity);

    public float getFriction() {
        return getFriction(objectId);
    }

    private native float getFriction(long objectId);

    /**
     * Sets the friction of this physics object
     * @param friction the friction of this physics object
     */
    public void setFriction(float friction) {
        setFriction(objectId, friction);
    }

    private native void setFriction(long objectId, float friction);

    public void setDamping(float linearDamping, float angularDamping) {
        setDamping(objectId, linearDamping, angularDamping);
    }

    private native void setDamping(long objectId, float linearDamping, float angularDamping);

//    private native void setRestitution(long objectId, float factor);
//
//    public void setLinearDamping(float linearDamping) {
//        constructionInfo.linearDamping = linearDamping;
//        rBody.setDamping(linearDamping, constructionInfo.angularDamping);
//    }
//
//    private native void setRestitution(long objectId, float factor);
//
    public void setLinearDamping(float linearDamping) {
        setDamping(objectId, linearDamping, getAngularDamping());
    }
    
    public void setAngularDamping(float angularDamping) {
        setAngularDamping(objectId, angularDamping);
    }
    private native void setAngularDamping(long objectId, float factor);

    public float getLinearDamping() {
        return getLinearDamping(objectId);
    }

    private native float getLinearDamping(long objectId);

    public float getAngularDamping() {
        return getAngularDamping(objectId);
    }

    private native float getAngularDamping(long objectId);

    public float getRestitution() {
        return getRestitution(objectId);
    }

    private native float getRestitution(long objectId);

    /**
     * The "bouncyness" of the PhysicsRigidBody, best performance if restitution=0
     * @param restitution
     */
    public void setRestitution(float restitution) {
        setRestitution(objectId, restitution);
    }

    private native void setRestitution(long objectId, float factor);

    /**
     * Get the current angular velocity of this PhysicsRigidBody
     * @return the current linear velocity
     */
    public Vector3f getAngularVelocity() {
        Vector3f vec = new Vector3f();
        getAngularVelocity(objectId, vec);
        return vec;
    }

    private native void getAngularVelocity(long objectId, Vector3f vec);

    /**
     * Get the current angular velocity of this PhysicsRigidBody
     * @param vec the vector to store the velocity in
     */
    public void getAngularVelocity(Vector3f vec) {
        getAngularVelocity(objectId, vec);
    }

    /**
     * Sets the angular velocity of this PhysicsRigidBody
     * @param vec the angular velocity of this PhysicsRigidBody
     */
    public void setAngularVelocity(Vector3f vec) {
        setAngularVelocity(objectId, vec);
        activate();
    }

    private native void setAngularVelocity(long objectId, Vector3f vec);

    /**
     * Get the current linear velocity of this PhysicsRigidBody
     * @return the current linear velocity
     */
    public Vector3f getLinearVelocity() {
        Vector3f vec = new Vector3f();
        getLinearVelocity(objectId, vec);
        return vec;
    }

    private native void getLinearVelocity(long objectId, Vector3f vec);

    /**
     * Get the current linear velocity of this PhysicsRigidBody
     * @param vec the vector to store the velocity in
     */
    public void getLinearVelocity(Vector3f vec) {
        getLinearVelocity(objectId, vec);
    }

    /**
     * Sets the linear velocity of this PhysicsRigidBody
     * @param vec the linear velocity of this PhysicsRigidBody
     */
    public void setLinearVelocity(Vector3f vec) {
        setLinearVelocity(objectId, vec);
        activate();
    }

    private native void setLinearVelocity(long objectId, Vector3f vec);

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply continuous force.
     * @param force the force
     * @param location the location of the force
     */
    public void applyForce(Vector3f force, Vector3f location) {
        applyForce(objectId, force, location);
        activate();
    }

    private native void applyForce(long objectId, Vector3f force, Vector3f location);

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse.
     * 
     * @param force the force
     */
    public void applyCentralForce(Vector3f force) {
        applyCentralForce(objectId, force);
        activate();
    }

    private native void applyCentralForce(long objectId, Vector3f force);

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse.
     * 
     * @param torque the torque
     */
    public void applyTorque(Vector3f torque) {
        applyTorque(objectId, torque);
        activate();
    }

    private native void applyTorque(long objectId, Vector3f vec);

    /**
     * Apply an impulse to the PhysicsRigidBody in the next physics update.
     * @param impulse applied impulse
     * @param rel_pos location relative to object
     */
    public void applyImpulse(Vector3f impulse, Vector3f rel_pos) {
        applyImpulse(objectId, impulse, rel_pos);
        activate();
    }

    private native void applyImpulse(long objectId, Vector3f impulse, Vector3f rel_pos);

    /**
     * Apply a torque impulse to the PhysicsRigidBody in the next physics update.
     * @param vec
     */
    public void applyTorqueImpulse(Vector3f vec) {
        applyTorqueImpulse(objectId, vec);
        activate();
    }

    private native void applyTorqueImpulse(long objectId, Vector3f vec);

    /**
     * Clear all forces from the PhysicsRigidBody
     * 
     */
    public void clearForces() {
        clearForces(objectId);
    }

    private native void clearForces(long objectId);

    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (collisionShape instanceof MeshCollisionShape && mass != 0) {
            throw new IllegalStateException("Dynamic rigidbody can not have mesh collision shape!");
        }
        if (objectId == 0) {
            rebuildRigidBody();
        } else {
            setCollisionShape(objectId, collisionShape.getObjectId());
            updateMassProps(objectId, collisionShape.getObjectId(), mass);
        }
    }

    private native void setCollisionShape(long objectId, long collisionShapeId);

    /**
     * reactivates this PhysicsRigidBody when it has been deactivated because it was not moving
     */
    public void activate() {
        activate(objectId);
    }

    private native void activate(long objectId);

    public boolean isActive() {
        return isActive(objectId);
    }

    private native boolean isActive(long objectId);

    /**
     * sets the sleeping thresholds, these define when the object gets deactivated
     * to save ressources. Low values keep the object active when it barely moves
     * @param linear the linear sleeping threshold
     * @param angular the angular sleeping threshold
     */
    public void setSleepingThresholds(float linear, float angular) {
        setSleepingThresholds(objectId, linear, angular);
    }

    private native void setSleepingThresholds(long objectId, float linear, float angular);

    public void setLinearSleepingThreshold(float linearSleepingThreshold) {
        setLinearSleepingThreshold(objectId, linearSleepingThreshold);
    }

    private native void setLinearSleepingThreshold(long objectId, float linearSleepingThreshold);

    public void setAngularSleepingThreshold(float angularSleepingThreshold) {
        setAngularSleepingThreshold(objectId, angularSleepingThreshold);
    }

    private native void setAngularSleepingThreshold(long objectId, float angularSleepingThreshold);

    public float getLinearSleepingThreshold() {
        return getLinearSleepingThreshold(objectId);
    }

    private native float getLinearSleepingThreshold(long objectId);

    public float getAngularSleepingThreshold() {
        return getAngularSleepingThreshold(objectId);
    }

    private native float getAngularSleepingThreshold(long objectId);

    public float getAngularFactor() {
        return getAngularFactor(objectId);
    }

    private native float getAngularFactor(long objectId);

    public void setAngularFactor(float factor) {
        setAngularFactor(objectId, factor);
    }

    private native void setAngularFactor(long objectId, float factor);

    /**
     * do not use manually, joints are added automatically
     */
    public void addJoint(PhysicsJoint joint) {
        if (!joints.contains(joint)) {
            joints.add(joint);
        }
        updateDebugShape();
    }

    /**
     * 
     */
    public void removeJoint(PhysicsJoint joint) {
        joints.remove(joint);
    }

    /**
     * Returns a list of connected joints. This list is only filled when
     * the PhysicsRigidBody is actually added to the physics space or loaded from disk.
     * @return list of active joints connected to this PhysicsRigidBody
     */
    public List<PhysicsJoint> getJoints() {
        return joints;
    }

    @Override
    protected Spatial getDebugShape() {
        //add joints
        Spatial shape = super.getDebugShape();
        Node node = null;
        if (shape instanceof Node) {
            node = (Node) shape;
        } else {
            node = new Node("DebugShapeNode");
            node.attachChild(shape);
        }
        int i = 0;
        for (Iterator<PhysicsJoint> it = joints.iterator(); it.hasNext();) {
            PhysicsJoint physicsJoint = it.next();
            Vector3f pivot = null;
            if (physicsJoint.getBodyA() == this) {
                pivot = physicsJoint.getPivotA();
            } else {
                pivot = physicsJoint.getPivotB();
            }
            Arrow arrow = new Arrow(pivot);
            Geometry geom = new Geometry("DebugBone" + i, arrow);
            geom.setMaterial(debugMaterialGreen);
            node.attachChild(geom);
            i++;
        }
        return node;
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);

        capsule.write(getMass(), "mass", 1.0f);

        capsule.write(getGravity(), "gravity", Vector3f.ZERO);
        capsule.write(getFriction(), "friction", 0.5f);
        capsule.write(getRestitution(), "restitution", 0);
        capsule.write(getAngularFactor(), "angularFactor", 1);
        capsule.write(kinematic, "kinematic", false);

        capsule.write(getLinearDamping(), "linearDamping", 0);
        capsule.write(getAngularDamping(), "angularDamping", 0);
        capsule.write(getLinearSleepingThreshold(), "linearSleepingThreshold", 0.8f);
        capsule.write(getAngularSleepingThreshold(), "angularSleepingThreshold", 1.0f);

        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);

        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()), "physicsRotation", new Matrix3f());

        capsule.writeSavableArrayList(joints, "joints", null);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);

        InputCapsule capsule = e.getCapsule(this);
        float mass = capsule.readFloat("mass", 1.0f);
        this.mass = mass;
        rebuildRigidBody();
        setGravity((Vector3f) capsule.readSavable("gravity", Vector3f.ZERO.clone()));
        setFriction(capsule.readFloat("friction", 0.5f));
        setKinematic(capsule.readBoolean("kinematic", false));

        setRestitution(capsule.readFloat("restitution", 0));
        setAngularFactor(capsule.readFloat("angularFactor", 1));
        setDamping(capsule.readFloat("linearDamping", 0), capsule.readFloat("angularDamping", 0));
        setSleepingThresholds(capsule.readFloat("linearSleepingThreshold", 0.8f), capsule.readFloat("angularSleepingThreshold", 1.0f));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));

        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
        setPhysicsRotation((Matrix3f) capsule.readSavable("physicsRotation", new Matrix3f()));

        joints = capsule.readSavableArrayList("joints", null);
    }
}
