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
package com.jme3.bullet.objects.infos;

import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.util.Converter;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * stores transform info of a PhysicsNode in a threadsafe manner to
 * allow multithreaded access from the jme scenegraph and the bullet physicsspace
 * @author normenhansen
 */
public class RigidBodyMotionState extends MotionState {
    //stores the bullet transform

    private Transform motionStateTrans = new Transform(Converter.convert(new Matrix3f()));
    private Vector3f worldLocation = new Vector3f();
    private Matrix3f worldRotation = new Matrix3f();
    private Quaternion worldRotationQuat = new Quaternion();
    private Vector3f localLocation = new Vector3f();
    private Quaternion localRotationQuat = new Quaternion();
    //keep track of transform changes
    private boolean physicsLocationDirty = false;
    private boolean jmeLocationDirty = false;
    //temp variable for conversion
    private Quaternion tmp_inverseWorldRotation = new Quaternion();
    private PhysicsVehicle vehicle;
    private boolean applyPhysicsLocal = false;
//    protected LinkedList<PhysicsMotionStateListener> listeners = new LinkedList<PhysicsMotionStateListener>();

    public RigidBodyMotionState() {
    }

    /**
     * called from bullet when creating the rigidbody
     * @param t
     * @return
     */
    public synchronized Transform getWorldTransform(Transform t) {
        t.set(motionStateTrans);
        return t;
    }

    /**
     * called from bullet when the transform of the rigidbody changes
     * @param worldTrans
     */
    public synchronized void setWorldTransform(Transform worldTrans) {
        if (jmeLocationDirty) {
            return;
        }
        motionStateTrans.set(worldTrans);
        Converter.convert(worldTrans.origin, worldLocation);
        Converter.convert(worldTrans.basis, worldRotation);
        worldRotationQuat.fromRotationMatrix(worldRotation);
//        for (Iterator<PhysicsMotionStateListener> it = listeners.iterator(); it.hasNext();) {
//            PhysicsMotionStateListener physicsMotionStateListener = it.next();
//            physicsMotionStateListener.stateChanged(worldLocation, worldRotation);
//        }
        physicsLocationDirty = true;
        if (vehicle != null) {
            vehicle.updateWheels();
        }
    }

    /**
     * applies the current transform to the given jme Node if the location has been updated on the physics side
     * @param spatial
     */
    public synchronized boolean applyTransform(Spatial spatial) {
        if (!physicsLocationDirty) {
            return false;
        }
        if (!applyPhysicsLocal && spatial.getParent() != null) {
            localLocation.set(worldLocation).subtractLocal(spatial.getParent().getWorldTranslation());
            localLocation.divideLocal(spatial.getParent().getWorldScale());
            tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);

            localRotationQuat.set(worldRotationQuat);
            tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);

            spatial.setLocalTranslation(localLocation);
            spatial.setLocalRotation(localRotationQuat);
        } else {
            spatial.setLocalTranslation(worldLocation);
            spatial.setLocalRotation(worldRotationQuat);
        }
        physicsLocationDirty = false;
        return true;
    }

    /**
     * @return the worldLocation
     */
    public Vector3f getWorldLocation() {
        return worldLocation;
    }

    /**
     * @return the worldRotation
     */
    public Matrix3f getWorldRotation() {
        return worldRotation;
    }

    /**
     * @return the worldRotationQuat
     */
    public Quaternion getWorldRotationQuat() {
        return worldRotationQuat;
    }

    /**
     * @param vehicle the vehicle to set
     */
    public void setVehicle(PhysicsVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public boolean isApplyPhysicsLocal() {
        return applyPhysicsLocal;
    }

    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        this.applyPhysicsLocal = applyPhysicsLocal;
    }
//    public void addMotionStateListener(PhysicsMotionStateListener listener){
//        listeners.add(listener);
//    }
//
//    public void removeMotionStateListener(PhysicsMotionStateListener listener){
//        listeners.remove(listener);
//    }
//    public synchronized boolean applyTransform(com.jme3.math.Transform trans) {
//        if (!physicsLocationDirty) {
//            return false;
//        }
//        trans.setTranslation(worldLocation);
//        trans.setRotation(worldRotationQuat);
//        physicsLocationDirty = false;
//        return true;
//    }
//    
//    /**
//     * called from jme when the location of the jme Node changes
//     * @param location
//     * @param rotation
//     */
//    public synchronized void setWorldTransform(Vector3f location, Quaternion rotation) {
//        worldLocation.set(location);
//        worldRotationQuat.set(rotation);
//        worldRotation.set(rotation.toRotationMatrix());
//        Converter.convert(worldLocation, motionStateTrans.origin);
//        Converter.convert(worldRotation, motionStateTrans.basis);
//        jmeLocationDirty = true;
//    }
//
//    /**
//     * applies the current transform to the given RigidBody if the value has been changed on the jme side
//     * @param rBody
//     */
//    public synchronized void applyTransform(RigidBody rBody) {
//        if (!jmeLocationDirty) {
//            return;
//        }
//        assert (rBody != null);
//        rBody.setWorldTransform(motionStateTrans);
//        rBody.activate();
//        jmeLocationDirty = false;
//    }
}
