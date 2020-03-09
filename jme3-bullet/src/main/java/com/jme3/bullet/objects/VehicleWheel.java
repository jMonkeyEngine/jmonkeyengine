/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
import com.jme3.export.*;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;

/**
 * Information about one wheel of a PhysicsVehicle.
 *
 * @author normenhansen
 */
public class VehicleWheel implements Savable {

    /**
     * unique identifier of the btRaycastVehicle
     */
    protected long wheelId = 0;
    /**
     * 0-origin index among the vehicle's wheels (&ge;0)
     */
    protected int wheelIndex = 0;
    /**
     * copy of wheel type: true&rarr;front (steering) wheel,
     * false&rarr;non-front wheel
     */
    protected boolean frontWheel;
    /**
     * location where the suspension connects to the chassis (in chassis
     * coordinates)
     */
    protected Vector3f location = new Vector3f();
    /**
     * suspension direction (in chassis coordinates, typically down/0,-1,0)
     */
    protected Vector3f direction = new Vector3f();
    /**
     * axis direction (in chassis coordinates, typically to the right/-1,0,0)
     */
    protected Vector3f axle = new Vector3f();
    /**
     * copy of suspension stiffness constant (10&rarr;off-road buggy, 
     * 50&rarr;sports car, 200&rarr;Formula-1 race car, default=20)
     */
    protected float suspensionStiffness = 20.0f;
    /**
     * copy of suspension damping when expanded (0&rarr;no damping, default=2.3)
     */
    protected float wheelsDampingRelaxation = 2.3f;
    /**
     * copy of suspension damping when compressed (0&rarr;no damping, 
     * default=4.4)
     */
    protected float wheelsDampingCompression = 4.4f;
    /**
     * copy of coefficient of friction between tyre and ground
     * (0.8&rarr;realistic car, 10000&rarr;kart racer, default=10.5)
     */
    protected float frictionSlip = 10.5f;
    /**
     * copy of roll-influence factor (0&rarr;no roll torque, 1&rarr;realistic
     * behavior, default=1)
     */
    protected float rollInfluence = 1.0f;
    /**
     * copy of maximum suspension travel distance (in centimeters, default=500)
     */
    protected float maxSuspensionTravelCm = 500f;
    /**
     * copy of maximum force exerted by the suspension (default=6000)
     */
    protected float maxSuspensionForce = 6000f;
    /**
     * copy of wheel radius (in physics-space units, &gt;0)
     */
    protected float radius = 0.5f;
    /**
     * copy of rest length of the suspension (in physics-space units)
     */
    protected float restLength = 1f;
    /**
     * wheel location in physics-space coordinates
     */
    protected Vector3f wheelWorldLocation = new Vector3f();
    /**
     * wheel orientation in physics-space coordinates
     */
    protected Quaternion wheelWorldRotation = new Quaternion();
    /**
     * associated spatial, or null if none
     */
    protected Spatial wheelSpatial;
    protected Matrix3f tmp_Matrix = new com.jme3.math.Matrix3f();
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();
    /**
     * true &rarr; physics coordinates match local transform, false &rarr;
     * physics coordinates match world transform
     */
    private boolean applyLocal = false;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected VehicleWheel() {
    }

    /**
     * Instantiate a wheel.
     *
     * @param spat the associated spatial, or null if none
     * @param location the location where the suspension connects to the chassis
     * (in chassis coordinates, not null, unaffected)
     * @param direction the suspension direction (in chassis coordinates, not
     * null, unaffected, typically down/0,-1,0)
     * @param axle the axis direction (in chassis coordinates, not null,
     * unaffected, typically right/-1,0,0)
     * @param restLength the rest length of the suspension (in physics-space
     * units)
     * @param radius the wheel's radius (in physics-space units, &ge;0)
     * @param frontWheel true&rarr;front (steering) wheel, false&rarr;non-front
     * wheel
     */
    public VehicleWheel(Spatial spat, Vector3f location, Vector3f direction, Vector3f axle,
            float restLength, float radius, boolean frontWheel) {
        this(location, direction, axle, restLength, radius, frontWheel);
        wheelSpatial = spat;
    }

    /**
     * Instantiate a wheel without an associated spatial.
     *
     * @param location the location where the suspension connects to the chassis
     * (in chassis coordinates, not null, unaffected)
     * @param direction the suspension direction (in chassis coordinates, not
     * null, unaffected, typically down/0,-1,0)
     * @param axle the axis direction (in chassis coordinates, not null,
     * unaffected, typically right/-1,0,0)
     * @param restLength the rest length of the suspension (in physics-space
     * units)
     * @param radius the wheel's radius (in physics-space units, &ge;0)
     * @param frontWheel true&rarr;front (steering) wheel, false&rarr;non-front
     * wheel
     */
    public VehicleWheel(Vector3f location, Vector3f direction, Vector3f axle,
            float restLength, float radius, boolean frontWheel) {
        this.location.set(location);
        this.direction.set(direction);
        this.axle.set(axle);
        this.frontWheel = frontWheel;
        this.restLength = restLength;
        this.radius = radius;
    }

    /**
     * Update this wheel's location and orientation in physics space.
     */
    public void updatePhysicsState() {
        getWheelLocation(wheelId, wheelIndex, wheelWorldLocation);
        getWheelRotation(wheelId, wheelIndex, tmp_Matrix);
        wheelWorldRotation.fromRotationMatrix(tmp_Matrix);
    }

    private native void getWheelLocation(long vehicleId, int wheelId, Vector3f location);

    private native void getWheelRotation(long vehicleId, int wheelId, Matrix3f location);

    /**
     * Apply this wheel's physics location and orientation to its associated
     * spatial, if any.
     */
    public void applyWheelTransform() {
        if (wheelSpatial == null) {
            return;
        }
        Quaternion localRotationQuat = wheelSpatial.getLocalRotation();
        Vector3f localLocation = wheelSpatial.getLocalTranslation();
        if (!applyLocal && wheelSpatial.getParent() != null) {
            localLocation.set(wheelWorldLocation).subtractLocal(wheelSpatial.getParent().getWorldTranslation());
            localLocation.divideLocal(wheelSpatial.getParent().getWorldScale());
            tmp_inverseWorldRotation.set(wheelSpatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);

            localRotationQuat.set(wheelWorldRotation);
            tmp_inverseWorldRotation.set(wheelSpatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);

            wheelSpatial.setLocalTranslation(localLocation);
            wheelSpatial.setLocalRotation(localRotationQuat);
        } else {
            wheelSpatial.setLocalTranslation(wheelWorldLocation);
            wheelSpatial.setLocalRotation(wheelWorldRotation);
        }
    }

    /**
     * Read the id of the btRaycastVehicle.
     *
     * @return the unique identifier (not zero)
     */
    public long getWheelId() {
        return wheelId;
    }

    /**
     * Assign this wheel to a vehicle.
     *
     * @param vehicleId the id of the btRaycastVehicle (not zero)
     * @param wheelIndex index among the vehicle's wheels (&ge;0)
     */
    public void setVehicleId(long vehicleId, int wheelIndex) {
        this.wheelId = vehicleId;
        this.wheelIndex = wheelIndex;
        applyInfo();
    }

    /**
     * Test whether this wheel is a front wheel.
     *
     * @return true if front wheel, otherwise false
     */
    public boolean isFrontWheel() {
        return frontWheel;
    }

    /**
     * Alter whether this wheel is a front (steering) wheel.
     *
     * @param frontWheel true&rarr;front wheel, false&rarr;non-front wheel
     */
    public void setFrontWheel(boolean frontWheel) {
        this.frontWheel = frontWheel;
        applyInfo();
    }

    /**
     * Access the location where the suspension connects to the chassis.
     *
     * @return the pre-existing location vector (in chassis coordinates, not 
     * null)
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * Access this wheel's suspension direction.
     *
     * @return the pre-existing direction vector (in chassis coordinates, not 
     * null)
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Access this wheel's axle direction.
     *
     * @return the pre-existing direction vector (not null)
     */
    public Vector3f getAxle() {
        return axle;
    }

    /**
     * Read the stiffness constant for this wheel's suspension.
     *
     * @return the stiffness constant
     */
    public float getSuspensionStiffness() {
        return suspensionStiffness;
    }

    /**
     * Alter the stiffness constant for this wheel's suspension.
     *
     * @param suspensionStiffness the desired stiffness constant
     * (10&rarr;off-road buggy, 50&rarr;sports car, 200&rarr;Formula-1 race car,
     * default=20)
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        this.suspensionStiffness = suspensionStiffness;
        applyInfo();
    }

    /**
     * Read this wheel's damping when the suspension is expanded.
     *
     * @return the damping
     */
    public float getWheelsDampingRelaxation() {
        return wheelsDampingRelaxation;
    }

    /**
     * Alter this wheel's damping when the suspension is expanded.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param wheelsDampingRelaxation the desired damping (default=2.3)
     */
    public void setWheelsDampingRelaxation(float wheelsDampingRelaxation) {
        this.wheelsDampingRelaxation = wheelsDampingRelaxation;
        applyInfo();
    }

    /**
     * Read this wheel's damping when the suspension is compressed.
     *
     * @return the damping
     */
    public float getWheelsDampingCompression() {
        return wheelsDampingCompression;
    }

    /**
     * Alter this wheel's damping when the suspension is compressed.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param wheelsDampingCompression the desired damping (default=4.4)
     */
    public void setWheelsDampingCompression(float wheelsDampingCompression) {
        this.wheelsDampingCompression = wheelsDampingCompression;
        applyInfo();
    }

    /**
     * Read the friction between this wheel's tyre and the ground.
     *
     * @return the coefficient of friction
     */
    public float getFrictionSlip() {
        return frictionSlip;
    }

    /**
     * Alter the friction between this wheel's tyre and the ground.
     * <p>
     * Should be about 0.8 for realistic cars, but can increased for better
     * handling. Set large (10000.0) for kart racers.
     *
     * @param frictionSlip the desired coefficient of friction (default=10.5)
     */
    public void setFrictionSlip(float frictionSlip) {
        this.frictionSlip = frictionSlip;
        applyInfo();
    }

    /**
     * Read this wheel's roll influence.
     *
     * @return the roll-influence factor
     */
    public float getRollInfluence() {
        return rollInfluence;
    }

    /**
     * Alter this wheel's roll influence.
     * <p>
     * The roll-influence factor reduces (or magnifies) the torque contributed
     * by this wheel that tends to cause the vehicle to roll over. This is a bit
     * of a hack, but it's quite effective.
     * <p>
     * If the friction between the tyres and the ground is too high, you may
     * reduce this factor to prevent the vehicle from rolling over. You should
     * also try lowering the vehicle's centre of mass.
     *
     * @param rollInfluence the desired roll-influence factor (0&rarr;no roll
     * torque, 1&rarr;realistic behaviour, default=1)
     */
    public void setRollInfluence(float rollInfluence) {
        this.rollInfluence = rollInfluence;
        applyInfo();
    }

    /**
     * Read the travel distance for this wheel's suspension.
     *
     * @return the maximum travel distance (in centimeters)
     */
    public float getMaxSuspensionTravelCm() {
        return maxSuspensionTravelCm;
    }

    /**
     * Alter the travel distance for this wheel's suspension.
     *
     * @param maxSuspensionTravelCm the desired maximum travel distance (in
     * centimetres, default=500)
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        this.maxSuspensionTravelCm = maxSuspensionTravelCm;
        applyInfo();
    }

    /**
     * Read the maximum force exerted by this wheel's suspension.
     *
     * @return the maximum force
     */
    public float getMaxSuspensionForce() {
        return maxSuspensionForce;
    }

    /**
     * Alter the maximum force exerted by this wheel's suspension.
     * <p>
     * Increase this if your suspension cannot handle the weight of your
     * vehicle.
     *
     * @param maxSuspensionForce the desired maximum force (default=6000)
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        this.maxSuspensionForce = maxSuspensionForce;
        applyInfo();
    }

    private void applyInfo() {
        if (wheelId == 0) {
            return;
        }
        applyInfo(wheelId, wheelIndex, suspensionStiffness, wheelsDampingRelaxation, wheelsDampingCompression, frictionSlip, rollInfluence, maxSuspensionTravelCm, maxSuspensionForce, radius, frontWheel, restLength);
    }

    private native void applyInfo(long wheelId, int wheelIndex,
            float suspensionStiffness,
            float wheelsDampingRelaxation,
            float wheelsDampingCompression,
            float frictionSlip,
            float rollInfluence,
            float maxSuspensionTravelCm,
            float maxSuspensionForce,
            float wheelsRadius,
            boolean frontWheel,
            float suspensionRestLength);

    /**
     * Read the radius of this wheel.
     *
     * @return the radius (in physics-space units, &ge;0)
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Alter the radius of this wheel.
     *
     * @param radius the desired radius (in physics-space units, &ge;0,
     * default=0.5)
     */
    public void setRadius(float radius) {
        this.radius = radius;
        applyInfo();
    }

    /**
     * Read the rest length of this wheel.
     *
     * @return the length
     */
    public float getRestLength() {
        return restLength;
    }

    /**
     * Alter the rest length of the suspension of this wheel.
     *
     * @param restLength the desired length (default=1)
     */
    public void setRestLength(float restLength) {
        this.restLength = restLength;
        applyInfo();
    }

    /**
     * returns the object this wheel is in contact with or null if no contact
     * @return the PhysicsCollisionObject (PhysicsRigidBody, PhysicsGhostObject)
     */
    public PhysicsCollisionObject getGroundObject() {
//        if (wheelInfo.raycastInfo.groundObject == null) {
//            return null;
//        } else if (wheelInfo.raycastInfo.groundObject instanceof RigidBody) {
//            System.out.println("RigidBody");
//            return (PhysicsRigidBody) ((RigidBody) wheelInfo.raycastInfo.groundObject).getUserPointer();
//        } else {
        return null;
//        }
    }

    /**
     * Copy the location where the wheel touches the ground.
     *
     * @param vec storage for the result (not null, modified)
     * @return a new location vector (in physics-space coordinates, not null)
     */
    public Vector3f getCollisionLocation(Vector3f vec) {
        getCollisionLocation(wheelId, wheelIndex, vec);
        return vec;
    }

    private native void getCollisionLocation(long wheelId, int wheelIndex, Vector3f vec);

    /**
     * Copy the location where the wheel collides with the ground.
     *
     * @return a new location vector (in physics-space coordinates)
     */
    public Vector3f getCollisionLocation() {
        Vector3f vec = new Vector3f();
        getCollisionLocation(wheelId, wheelIndex, vec);
        return vec;
    }

    /**
     * Copy the normal where the wheel touches the ground.
     *
     * @param vec storage for the result (not null, modified)
     * @return a unit vector (in physics-space coordinates, not null)
     */
    public Vector3f getCollisionNormal(Vector3f vec) {
        getCollisionNormal(wheelId, wheelIndex, vec);
        return vec;
    }

    private native void getCollisionNormal(long wheelId, int wheelIndex, Vector3f vec);

    /**
     * Copy the normal where the wheel touches the ground.
     *
     * @return a new unit vector (in physics-space coordinates, not null)
     */
    public Vector3f getCollisionNormal() {
        Vector3f vec = new Vector3f();
        getCollisionNormal(wheelId, wheelIndex, vec);
        return vec;
    }

    /**
     * Calculate to what extent the wheel is skidding (for skid sounds/smoke
     * etc.)
     *
     * @return the relative amount of traction (0&rarr;wheel is sliding,
     * 1&rarr;wheel has full traction)
     */
    public float getSkidInfo() {
        return getSkidInfo(wheelId, wheelIndex);
    }

    public native float getSkidInfo(long wheelId, int wheelIndex);
    
    /**
     * Calculate how much this wheel has turned since the last physics step.
     *
     * @return the rotation angle (in radians)
     */
    public float getDeltaRotation() {
        return getDeltaRotation(wheelId, wheelIndex);
    }
    
    public native float getDeltaRotation(long wheelId, int wheelIndex);

    /**
     * De-serialize this wheel, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        wheelSpatial = (Spatial) capsule.readSavable("wheelSpatial", null);
        frontWheel = capsule.readBoolean("frontWheel", false);
        location = (Vector3f) capsule.readSavable("wheelLocation", new Vector3f());
        direction = (Vector3f) capsule.readSavable("wheelDirection", new Vector3f());
        axle = (Vector3f) capsule.readSavable("wheelAxle", new Vector3f());
        suspensionStiffness = capsule.readFloat("suspensionStiffness", 20.0f);
        wheelsDampingRelaxation = capsule.readFloat("wheelsDampingRelaxation", 2.3f);
        wheelsDampingCompression = capsule.readFloat("wheelsDampingCompression", 4.4f);
        frictionSlip = capsule.readFloat("frictionSlip", 10.5f);
        rollInfluence = capsule.readFloat("rollInfluence", 1.0f);
        maxSuspensionTravelCm = capsule.readFloat("maxSuspensionTravelCm", 500f);
        maxSuspensionForce = capsule.readFloat("maxSuspensionForce", 6000f);
        radius = capsule.readFloat("wheelRadius", 0.5f);
        restLength = capsule.readFloat("restLength", 1f);
    }

    /**
     * Serialize this wheel, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(wheelSpatial, "wheelSpatial", null);
        capsule.write(frontWheel, "frontWheel", false);
        capsule.write(location, "wheelLocation", new Vector3f());
        capsule.write(direction, "wheelDirection", new Vector3f());
        capsule.write(axle, "wheelAxle", new Vector3f());
        capsule.write(suspensionStiffness, "suspensionStiffness", 20.0f);
        capsule.write(wheelsDampingRelaxation, "wheelsDampingRelaxation", 2.3f);
        capsule.write(wheelsDampingCompression, "wheelsDampingCompression", 4.4f);
        capsule.write(frictionSlip, "frictionSlip", 10.5f);
        capsule.write(rollInfluence, "rollInfluence", 1.0f);
        capsule.write(maxSuspensionTravelCm, "maxSuspensionTravelCm", 500f);
        capsule.write(maxSuspensionForce, "maxSuspensionForce", 6000f);
        capsule.write(radius, "wheelRadius", 0.5f);
        capsule.write(restLength, "restLength", 1f);
    }

    /**
     * Access the spatial associated with this wheel.
     *
     * @return the pre-existing instance, or null
     */
    public Spatial getWheelSpatial() {
        return wheelSpatial;
    }

    /**
     * Alter which spatial is associated with this wheel.
     *
     * @param wheelSpatial the desired spatial, or null for none
     */
    public void setWheelSpatial(Spatial wheelSpatial) {
        this.wheelSpatial = wheelSpatial;
    }

    /**
     * Test whether physics coordinates should match the local transform of the
     * Spatial.
     *
     * @return true if matching local transform, false if matching world
     * transform
     */
    public boolean isApplyLocal() {
        return applyLocal;
    }

    /**
     * Alter whether physics coordinates should match the local transform of the
     * Spatial.
     *
     * @param applyLocal true&rarr;match local transform, false&rarr;match world
     * transform (default=false)
     */
    public void setApplyLocal(boolean applyLocal) {
        this.applyLocal = applyLocal;
    }

    /**
     * Copy this wheel's physics-space orientation to the specified quaternion.
     *
     * @param store storage for the result (not null, modified)
     */
    public void getWheelWorldRotation(final Quaternion store) {
        store.set(this.wheelWorldRotation);
    }

    /**
     * Copy this wheel's physics-space location to the specified vector.
     *
     * @param store storage for the result (not null, modified)
     */
    public void getWheelWorldLocation(final Vector3f store) {
        store.set(this.wheelWorldLocation);
    }

}
