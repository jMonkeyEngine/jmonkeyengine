/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The motion state (transform) of a rigid body, with thread-safe access.
 *
 * @author normenhansen
 */
public class RigidBodyMotionState {
    long motionStateId = 0;
    private Vector3f worldLocation = new Vector3f();
    private Matrix3f worldRotation = new Matrix3f();
    private Quaternion worldRotationQuat = new Quaternion();
    private Quaternion tmp_inverseWorldRotation = new Quaternion();
    private PhysicsVehicle vehicle;
    /**
     * true &rarr; physics coordinates match local transform, false &rarr;
     * physics coordinates match world transform
     */
    private boolean applyPhysicsLocal = false;
//    protected LinkedList<PhysicsMotionStateListener> listeners = new LinkedList<PhysicsMotionStateListener>();

    /**
     * Instantiate a motion state.
     */
    public RigidBodyMotionState() {
        this.motionStateId = createMotionState();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created MotionState {0}", Long.toHexString(motionStateId));
    }

    private native long createMotionState();

    /**
     * If the motion state has been updated, apply the new transform to the
     * specified spatial.
     *
     * @param spatial where to apply the physics transform (not null, modified)
     * @return true if changed
     */
    public boolean applyTransform(Spatial spatial) {
        Vector3f localLocation = spatial.getLocalTranslation();
        Quaternion localRotationQuat = spatial.getLocalRotation();
        boolean physicsLocationDirty = applyTransform(motionStateId, localLocation, localRotationQuat);
        if (!physicsLocationDirty) {
            return false;
        }
        if (!applyPhysicsLocal && spatial.getParent() != null) {
            localLocation.subtractLocal(spatial.getParent().getWorldTranslation());
            localLocation.divideLocal(spatial.getParent().getWorldScale());
            tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);

//            localRotationQuat.set(worldRotationQuat);
            tmp_inverseWorldRotation.mult(localRotationQuat, localRotationQuat);

            spatial.setLocalTranslation(localLocation);
            spatial.setLocalRotation(localRotationQuat);
        } else {
            spatial.setLocalTranslation(localLocation);
            spatial.setLocalRotation(localRotationQuat);
//            spatial.setLocalTranslation(worldLocation);
//            spatial.setLocalRotation(worldRotationQuat);
        }
        if (vehicle != null) {
            vehicle.updateWheels();
        }
        return true;
    }

    private native boolean applyTransform(long stateId, Vector3f location, Quaternion rotation);

    /**
     * Copy the location from this motion state.
     *
     * @return the pre-existing location vector (in physics-space coordinates,
     * not null)
     */
    public Vector3f getWorldLocation() {
        getWorldLocation(motionStateId, worldLocation);
        return worldLocation;
    }

    private native void getWorldLocation(long stateId, Vector3f vec);

    /**
     * Read the rotation of this motion state (as a matrix).
     *
     * @return the pre-existing rotation matrix (in physics-space coordinates,
     * not null)
     */
    public Matrix3f getWorldRotation() {
        getWorldRotation(motionStateId, worldRotation);
        return worldRotation;
    }

    private native void getWorldRotation(long stateId, Matrix3f vec);

    /**
     * Read the rotation of this motion state (as a quaternion).
     *
     * @return the pre-existing instance (in physics-space coordinates, not
     * null)
     */
    public Quaternion getWorldRotationQuat() {
        getWorldRotationQuat(motionStateId, worldRotationQuat);
        return worldRotationQuat;
    }

    private native void getWorldRotationQuat(long stateId, Quaternion vec);

    /**
     * @param vehicle which vehicle will use this motion state
     */
    public void setVehicle(PhysicsVehicle vehicle) {
        this.vehicle = vehicle;
    }

    /**
     * Test whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @return true if matching local coordinates, false if matching world
     * coordinates
     */
    public boolean isApplyPhysicsLocal() {
        return applyPhysicsLocal;
    }

    /**
     * Alter whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @param applyPhysicsLocal true&rarr;match local coordinates,
     * false&rarr;match world coordinates (default=false)
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        this.applyPhysicsLocal = applyPhysicsLocal;
    }
    
    /**
     * Read the unique id of the native object.
     *
     * @return id (not zero)
     */
    public long getObjectId(){
        return motionStateId;
    }
//    public void addMotionStateListener(PhysicsMotionStateListener listener){
//        listeners.add(listener);
//    }
//
//    public void removeMotionStateListener(PhysicsMotionStateListener listener){
//        listeners.remove(listener);
//    }

    /**
     * Finalize this motion state just before it is destroyed. Should be invoked
     * only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing MotionState {0}", Long.toHexString(motionStateId));
        finalizeNative(motionStateId);
    }

    private native void finalizeNative(long objectId);
}
