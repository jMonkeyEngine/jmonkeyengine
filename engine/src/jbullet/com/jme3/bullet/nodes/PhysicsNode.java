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
package com.jme3.bullet.nodes;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import java.io.IOException;
import java.util.List;

/**
 * <p>PhysicsNode - Basic physics object</p>
 * @author normenhansen
 * @deprecated in favor of physics Controls
 */
@Deprecated
public class PhysicsNode extends PhysicsBaseNode {

    protected Vector3f continuousForce = new Vector3f();
    protected Vector3f continuousForceLocation = new Vector3f();
    protected Vector3f continuousTorque = new Vector3f();
    protected boolean applyForce = false;
    protected boolean applyTorque = false;

    public PhysicsNode() {
    }

    /**
     * Creates a new PhysicsNode with the supplied collision shape
     * @param child
     * @param shape
     */
    public PhysicsNode(CollisionShape shape) {
        collisionObject = new RigidBodyControl(shape);
        addControl(((RigidBodyControl)collisionObject));
    }

    public PhysicsNode(CollisionShape shape, float mass) {
        collisionObject = new RigidBodyControl(shape, mass);
        addControl(((RigidBodyControl)collisionObject));
    }

    /**
     * Creates a new PhysicsNode with the supplied child node or geometry and
     * sets the supplied collision shape to the PhysicsNode
     * @param child
     * @param shape
     */
    public PhysicsNode(Spatial child, CollisionShape shape) {
        this(child, shape, 1.0f);
    }

    /**
     * Creates a new PhysicsNode with the supplied child node or geometry and
     * uses the supplied collision shape for that PhysicsNode<br>
     * @param child
     * @param shape
     */
    public PhysicsNode(Spatial child, CollisionShape shape, float mass) {
        collisionObject = new RigidBodyControl(shape, mass);
        addControl(((RigidBodyControl)collisionObject));
        attachChild(child);
    }

    @Override
    public void setLocalTransform(Transform t) {
        super.setLocalTransform(t);
        ((PhysicsRigidBody)collisionObject).setPhysicsLocation(getWorldTranslation());
        ((PhysicsRigidBody)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        super.setLocalTranslation(localTranslation);
        ((PhysicsRigidBody)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalTranslation(float x, float y, float z) {
        super.setLocalTranslation(x, y, z);
        ((PhysicsRigidBody)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalRotation(Matrix3f rotation) {
        super.setLocalRotation(rotation);
        ((PhysicsRigidBody)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    @Override
    public void setLocalRotation(Quaternion quaternion) {
        super.setLocalRotation(quaternion);
        ((PhysicsRigidBody)collisionObject).setPhysicsRotation(getWorldRotation().toRotationMatrix());
    }

    /**
     * This is normally only needed when using detached physics
     * @param location the location of the actual physics object
     */
    public void setPhysicsLocation(Vector3f location) {
        ((PhysicsRigidBody)collisionObject).setPhysicsLocation(location);
    }

    /**
     * This is normally only needed when using detached physics
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        ((PhysicsRigidBody)collisionObject).setPhysicsRotation(rotation);
    }

    /**
     * This is normally only needed when using detached physics
     * @param location the location of the actual physics object is stored in this Vector3f
     */
    public void getPhysicsLocation(Vector3f location) {
        ((PhysicsRigidBody)collisionObject).getPhysicsLocation(location);
    }

    /**
     * This is normally only needed when using detached physics
     * @param rotation the rotation of the actual physics object is stored in this Matrix3f
     */
    public void getPhysicsRotation(Matrix3f rotation) {
        ((PhysicsRigidBody)collisionObject).getPhysicsRotationMatrix(rotation);
    }

    /**
     * Sets the node to kinematic mode. in this mode the node is not affected by physics
     * but affects other physics objects. Iits kinetic force is calculated by the amount
     * of movement it is exposed to and its weight.
     * @param kinematic
     */
    public void setKinematic(boolean kinematic) {
        ((PhysicsRigidBody)collisionObject).setKinematic(kinematic);
    }

    public boolean isKinematic() {
        return ((PhysicsRigidBody)collisionObject).isKinematic();
    }

    public void setCcdSweptSphereRadius(float radius) {
        ((PhysicsRigidBody)collisionObject).setCcdSweptSphereRadius(radius);
    }

    /**
     * Sets the amount of motion that has to happen in one physics tick to trigger the continuous motion detection<br/>
     * Set to zero to disable (default)
     * @param threshold
     */
    public void setCcdMotionThreshold(float threshold) {
        ((PhysicsRigidBody)collisionObject).setCcdMotionThreshold(threshold);
    }

    public float getCcdSweptSphereRadius() {
        return ((PhysicsRigidBody)collisionObject).getCcdSweptSphereRadius();
    }

    public float getCcdMotionThreshold() {
        return ((PhysicsRigidBody)collisionObject).getCcdMotionThreshold();
    }

    public float getCcdSquareMotionThreshold() {
        return ((PhysicsRigidBody)collisionObject).getCcdSquareMotionThreshold();
    }

    public float getMass() {
        return ((PhysicsRigidBody)collisionObject).getMass();
    }

    /**
     * Sets the mass of this PhysicsNode, objects with mass=0 are static.
     * @param mass
     */
    public void setMass(float mass) {
        ((PhysicsRigidBody)collisionObject).setMass(mass);
    }

    public Vector3f getGravity() {
        return ((PhysicsRigidBody)collisionObject).getGravity();
    }

    public Vector3f getGravity(Vector3f gravity) {
        return ((PhysicsRigidBody)collisionObject).getGravity(gravity);
    }

    /**
     * Set the local gravity of this PhysicsNode<br/>
     * Set this after adding the node to the PhysicsSpace,
     * the PhysicsSpace assigns its current gravity to the physics node when its added.
     * @param gravity the gravity vector to set
     */
    public void setGravity(Vector3f gravity) {
        ((PhysicsRigidBody)collisionObject).setGravity(gravity);
    }

    public float getFriction() {
        return ((PhysicsRigidBody)collisionObject).getFriction();
    }

    /**
     * Sets the friction of this physics object
     * @param friction the friction of this physics object
     */
    public void setFriction(float friction) {
        ((PhysicsRigidBody)collisionObject).setFriction(friction);
    }

    public void setDamping(float linearDamping, float angularDamping) {
        ((PhysicsRigidBody)collisionObject).setDamping(linearDamping, angularDamping);
    }

    public void setLinearDamping(float linearDamping) {
        ((PhysicsRigidBody)collisionObject).setLinearDamping(linearDamping);
    }

    public void setAngularDamping(float angularDamping) {
        ((PhysicsRigidBody)collisionObject).setAngularDamping(angularDamping);
    }

    public float getLinearDamping() {
        return ((PhysicsRigidBody)collisionObject).getLinearDamping();
    }

    public float getAngularDamping() {
        return ((PhysicsRigidBody)collisionObject).getAngularDamping();
    }

    public float getRestitution() {
        return ((PhysicsRigidBody)collisionObject).getRestitution();
    }

    /**
     * The "bouncyness" of the PhysicsNode, best performance if restitution=0
     * @param restitution
     */
    public void setRestitution(float restitution) {
        ((PhysicsRigidBody)collisionObject).setRestitution(restitution);
    }

    /**
     * Get the current angular velocity of this PhysicsNode
     * @return the current linear velocity
     */
    public Vector3f getAngularVelocity() {
        return ((PhysicsRigidBody)collisionObject).getAngularVelocity();
    }

    /**
     * Get the current angular velocity of this PhysicsNode
     * @param vec the vector to store the velocity in
     */
    public void getAngularVelocity(Vector3f vec) {
        ((PhysicsRigidBody)collisionObject).getAngularVelocity(vec);
    }

    /**
     * Sets the angular velocity of this PhysicsNode
     * @param vec the angular velocity of this PhysicsNode
     */
    public void setAngularVelocity(Vector3f vec) {
        ((PhysicsRigidBody)collisionObject).setAngularVelocity(vec);
    }

    /**
     * Get the current linear velocity of this PhysicsNode
     * @return the current linear velocity
     */
    public Vector3f getLinearVelocity() {
        return ((PhysicsRigidBody)collisionObject).getLinearVelocity();
    }

    /**
     * Get the current linear velocity of this PhysicsNode
     * @param vec the vector to store the velocity in
     */
    public void getLinearVelocity(Vector3f vec) {
        ((PhysicsRigidBody)collisionObject).getLinearVelocity(vec);
    }

    /**
     * Sets the linear velocity of this PhysicsNode
     * @param vec the linear velocity of this PhysicsNode
     */
    public void setLinearVelocity(Vector3f vec) {
        ((PhysicsRigidBody)collisionObject).setLinearVelocity(vec);
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        if (applyForce) {
            ((PhysicsRigidBody)collisionObject).applyForce(continuousForce,continuousForceLocation);
        }
        if (applyTorque) {
            ((PhysicsRigidBody)collisionObject).applyTorque(continuousTorque);
        }
    }

    /**
     * Get the currently applied continuous force
     * @param vec the vector to store the continuous force in
     * @return null if no force is applied
     */
    public synchronized Vector3f getContinuousForce(Vector3f vec) {
        if (applyForce) {
            return vec.set(continuousForce);
        } else {
            return null;
        }
    }

    /**
     * get the currently applied continuous force
     * @return null if no force is applied
     */
    public synchronized Vector3f getContinuousForce() {
        if (applyForce) {
            return continuousForce;
        } else {
            return null;
        }
    }

    /**
     * Get the currently applied continuous force location
     * @return null if no force is applied
     */
    public synchronized Vector3f getContinuousForceLocation() {
        if (applyForce) {
            return continuousForceLocation;
        } else {
            return null;
        }
    }

    /**
     * Apply a continuous force to this PhysicsNode, the force is updated automatically each
     * tick so you only need to set it once and then set it to false to stop applying
     * the force.
     * @param apply true if the force should be applied each physics tick
     * @param force the vector of the force to apply
     */
    public synchronized void applyContinuousForce(boolean apply, Vector3f force) {
        if (force != null) {
            continuousForce.set(force);
        }
        continuousForceLocation.set(0, 0, 0);
        applyForce = apply;

    }

    /**
     * Apply a continuous force to this PhysicsNode, the force is updated automatically each
     * tick so you only need to set it once and then set it to false to stop applying
     * the force.
     * @param apply true if the force should be applied each physics tick
     * @param force the offset of the force
     */
    public synchronized void applyContinuousForce(boolean apply, Vector3f force, Vector3f location) {
        if (force != null) {
            continuousForce.set(force);
        }
        if (location != null) {
            continuousForceLocation.set(location);
        }
        applyForce = apply;

    }

    /**
     * Use to enable/disable continuous force
     * @param apply set to false to disable
     */
    public synchronized void applyContinuousForce(boolean apply) {
        applyForce = apply;
    }

    /**
     * Get the currently applied continuous torque
     * @return null if no torque is applied
     */
    public synchronized Vector3f getContinuousTorque() {
        if (applyTorque) {
            return continuousTorque;
        } else {
            return null;
        }
    }

    /**
     * Get the currently applied continuous torque
     * @param vec the vector to store the continuous torque in
     * @return null if no torque is applied
     */
    public synchronized Vector3f getContinuousTorque(Vector3f vec) {
        if (applyTorque) {
            return vec.set(continuousTorque);
        } else {
            return null;
        }
    }

    /**
     * Apply a continuous torque to this PhysicsNode. The torque is updated automatically each
     * tick so you only need to set it once and then set it to false to stop applying
     * the torque.
     * @param apply true if the force should be applied each physics tick
     * @param vec the vector of the force to apply
     */
    public synchronized void applyContinuousTorque(boolean apply, Vector3f vec) {
        if (vec != null) {
            continuousTorque.set(vec);
        }
        applyTorque = apply;
    }

    /**
     * Use to enable/disable continuous torque
     * @param apply set to false to disable
     */
    public synchronized void applyContinuousTorque(boolean apply) {
        applyTorque = apply;
    }



    /**
     * Apply a force to the PhysicsNode, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply continuous force.
     * @param force the force
     * @param location the location of the force
     */
    public void applyForce(final Vector3f force, final Vector3f location) {
        ((PhysicsRigidBody)collisionObject).applyForce(force, location);
    }

    /**
     * Apply a force to the PhysicsNode, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply continuous force.
     *
     * @param force the force
     */
    public void applyCentralForce(final Vector3f force) {
        ((PhysicsRigidBody)collisionObject).applyCentralForce(force);
    }

    /**
     * Apply a force to the PhysicsNode, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply continuous force.
     *
     * @param torque the torque
     */
    public void applyTorque(final Vector3f torque) {
        ((PhysicsRigidBody)collisionObject).applyTorque(torque);
    }

    /**
     * Apply an impulse to the PhysicsNode in the next physics update.
     * @param impulse applied impulse
     * @param rel_pos location relative to object
     */
    public void applyImpulse(final Vector3f impulse, final Vector3f rel_pos) {
        ((PhysicsRigidBody)collisionObject).applyImpulse(impulse, rel_pos);
    }

    /**
     * Apply a torque impulse to the PhysicsNode in the next physics update.
     * @param vec
     */
    public void applyTorqueImpulse(final Vector3f vec) {
        ((PhysicsRigidBody)collisionObject).applyTorqueImpulse(vec);
    }

    /**
     * Clear all forces from the PhysicsNode
     *
     */
    public void clearForces() {
        ((PhysicsRigidBody)collisionObject).clearForces();
    }

    public void setCollisionShape(CollisionShape collisionShape) {
        ((PhysicsRigidBody)collisionObject).setCollisionShape(collisionShape);
    }

    /**
     * reactivates this PhysicsNode when it has been deactivated because it was not moving
     */
    public void activate() {
        ((PhysicsRigidBody)collisionObject).activate();
    }

    public boolean isActive() {
        return ((PhysicsRigidBody)collisionObject).isActive();
    }

    /**
     * sets the sleeping thresholds, these define when the object gets deactivated
     * to save ressources. Low values keep the object active when it barely moves
     * @param linear the linear sleeping threshold
     * @param angular the angular sleeping threshold
     */
    public void setSleepingThresholds(float linear, float angular) {
        ((PhysicsRigidBody)collisionObject).setSleepingThresholds(linear, angular);
    }

    public void setLinearSleepingThreshold(float linearSleepingThreshold) {
        ((PhysicsRigidBody)collisionObject).setLinearSleepingThreshold(linearSleepingThreshold);
    }

    public void setAngularSleepingThreshold(float angularSleepingThreshold) {
        ((PhysicsRigidBody)collisionObject).setAngularSleepingThreshold(angularSleepingThreshold);
    }

    public float getLinearSleepingThreshold() {
        return ((PhysicsRigidBody)collisionObject).getLinearSleepingThreshold();
    }

    public float getAngularSleepingThreshold() {
        return ((PhysicsRigidBody)collisionObject).getAngularSleepingThreshold();
    }

    /**
     * do not use manually, joints are added automatically
     */
    public void addJoint(PhysicsJoint joint) {
        ((PhysicsRigidBody)collisionObject).addJoint(joint);
    }

    /**
     *
     */
    public void removeJoint(PhysicsJoint joint) {
        ((PhysicsRigidBody)collisionObject).removeJoint(joint);
    }

    /**
     * Returns a list of connected joints. This list is only filled when
     * the PhysicsNode is actually added to the physics space or loaded from disk.
     * @return list of active joints connected to this physicsnode
     */
    public List<PhysicsJoint> getJoints() {
        return ((PhysicsRigidBody)collisionObject).getJoints();
    }

    /**
     * used internally
     */
    public PhysicsRigidBody getRigidBody() {
        return ((PhysicsRigidBody)collisionObject);
    }

    /**
     * destroys this PhysicsNode and removes it from memory
     */
    public void destroy() {
        ((PhysicsRigidBody)collisionObject).destroy();
    }

    public void attachDebugShape(AssetManager manager) {
        collisionObject.attachDebugShape(manager);
    }

    public void attachDebugShape(Material mat) {
        collisionObject.attachDebugShape(mat);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
    }
}
