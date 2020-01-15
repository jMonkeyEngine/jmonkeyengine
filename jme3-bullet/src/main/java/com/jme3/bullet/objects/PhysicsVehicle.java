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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.infos.VehicleTuning;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collision object for simplified vehicle simulation based on Bullet's
 * btRaycastVehicle.
 * <p>
 * <i>From Bullet manual:</i><br>
 * For arcade style vehicle simulations, it is recommended to use the simplified
 * Bullet vehicle model as provided in btRaycastVehicle. Instead of simulation
 * each wheel and chassis as separate rigid bodies, connected by constraints, it
 * uses a simplified model. This simplified model has many benefits, and is
 * widely used in commercial driving games.
 * <p>
 * The entire vehicle is represented as a single rigidbody, the chassis. The
 * collision detection of the wheels is approximated by ray casts, and the tire
 * friction is a basic anisotropic friction model.
 *
 * @author normenhansen
 */
public class PhysicsVehicle extends PhysicsRigidBody {

    /**
     * Unique identifier of the btRaycastVehicle. The constructor sets this to a
     * non-zero value.
     */
    protected long vehicleId = 0;
    /**
     * Unique identifier of the ray caster.
     */
    protected long rayCasterId = 0;
    /**
     * tuning parameters applied when a wheel is created
     */
    protected VehicleTuning tuning = new VehicleTuning();
    /**
     * list of wheels
     */
    protected ArrayList<VehicleWheel> wheels = new ArrayList<VehicleWheel>();
    /**
     * physics space where this vehicle is added, or null if none
     */
    protected PhysicsSpace physicsSpace;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected PhysicsVehicle() {
    }

    /**
     * Instantiate a vehicle with the specified collision shape and mass=1.
     *
     * @param shape the desired shape (not null, alias created)
     */
    public PhysicsVehicle(CollisionShape shape) {
        super(shape);
    }

    /**
     * Instantiate a vehicle with the specified collision shape and mass.
     *
     * @param shape the desired shape (not null, alias created)
     * @param mass (&gt;0)
     */
    public PhysicsVehicle(CollisionShape shape, float mass) {
        super(shape, mass);
    }

    /**
     * used internally
     */
    public void updateWheels() {
        if (vehicleId != 0) {
            for (int i = 0; i < wheels.size(); i++) {
                updateWheelTransform(vehicleId, i, true);
                wheels.get(i).updatePhysicsState();
            }
        }
    }

    private native void updateWheelTransform(long vehicleId, int wheel, boolean interpolated);

    /**
     * used internally
     */
    public void applyWheelTransforms() {
        if (wheels != null) {
            for (int i = 0; i < wheels.size(); i++) {
                wheels.get(i).applyWheelTransform();
            }
        }
    }

    @Override
    protected void postRebuild() {
        super.postRebuild();
        motionState.setVehicle(this);
        createVehicle(physicsSpace);
    }

    /**
     * Used internally, creates the actual vehicle constraint when vehicle is
     * added to physics space.
     *
     * @param space which physics space
     */
    public void createVehicle(PhysicsSpace space) {
        physicsSpace = space;
        if (space == null) {
            return;
        }
        if (space.getSpaceId() == 0) {
            throw new IllegalStateException("Physics space is not initialized!");
        }
        if (rayCasterId != 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Clearing RayCaster {0}", Long.toHexString(rayCasterId));
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Clearing Vehicle {0}", Long.toHexString(vehicleId));
            finalizeNative(rayCasterId, vehicleId);
        }
        rayCasterId = createVehicleRaycaster(objectId, space.getSpaceId());
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created RayCaster {0}", Long.toHexString(rayCasterId));
        vehicleId = createRaycastVehicle(objectId, rayCasterId);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Vehicle {0}", Long.toHexString(vehicleId));
        setCoordinateSystem(vehicleId, 0, 1, 2);
        for (VehicleWheel wheel : wheels) {
            wheel.setVehicleId(vehicleId, addWheel(vehicleId, wheel.getLocation(), wheel.getDirection(), wheel.getAxle(), wheel.getRestLength(), wheel.getRadius(), tuning, wheel.isFrontWheel()));
        }
    }

    private native long createVehicleRaycaster(long objectId, long physicsSpaceId);

    private native long createRaycastVehicle(long objectId, long rayCasterId);

    private native void setCoordinateSystem(long objectId, int a, int b, int c);

    private native int addWheel(long objectId, Vector3f location, Vector3f direction, Vector3f axle, float restLength, float radius, VehicleTuning tuning, boolean frontWheel);

    /**
     * Add a wheel to this vehicle.
     *
     * @param connectionPoint the location where the suspension connects to the
     * chassis (in chassis coordinates, not null, unaffected)
     * @param direction the suspension direction (in chassis coordinates, not
     * null, unaffected, typically down/0,-1,0)
     * @param axle the axis direction (in chassis coordinates, not null,
     * unaffected, typically -1,0,0)
     * @param suspensionRestLength the rest length of the suspension (in
     * physics-space units)
     * @param wheelRadius the wheel radius (in physics-space units, &gt;0)
     * @param isFrontWheel true&rarr;front (steering) wheel,
     * false&rarr;non-front wheel
     * @return a new VehicleWheel for access (not null)
     */
    public VehicleWheel addWheel(Vector3f connectionPoint, Vector3f direction, Vector3f axle, float suspensionRestLength, float wheelRadius, boolean isFrontWheel) {
        return addWheel(null, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
    }

    /**
     * Add a wheel to this vehicle
     *
     * @param spat the associated spatial, or null if none
     * @param connectionPoint the location where the suspension connects to the
     * chassis (in chassis coordinates, not null, unaffected)
     * @param direction the suspension direction (in chassis coordinates, not
     * null, unaffected, typically down/0,-1,0)
     * @param axle the axis direction (in chassis coordinates, not null,
     * unaffected, typically -1,0,0)
     * @param suspensionRestLength the rest length of the suspension (in
     * physics-space units)
     * @param wheelRadius the wheel radius (in physics-space units, &gt;0)
     * @param isFrontWheel true&rarr;front (steering) wheel,
     * false&rarr;non-front wheel
     * @return a new VehicleWheel for access (not null)
     */
    public VehicleWheel addWheel(Spatial spat, Vector3f connectionPoint, Vector3f direction, Vector3f axle, float suspensionRestLength, float wheelRadius, boolean isFrontWheel) {
        VehicleWheel wheel = null;
        if (spat == null) {
            wheel = new VehicleWheel(connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
        } else {
            wheel = new VehicleWheel(spat, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
        }
        wheel.setFrictionSlip(tuning.frictionSlip);
        wheel.setMaxSuspensionTravelCm(tuning.maxSuspensionTravelCm);
        wheel.setSuspensionStiffness(tuning.suspensionStiffness);
        wheel.setWheelsDampingCompression(tuning.suspensionCompression);
        wheel.setWheelsDampingRelaxation(tuning.suspensionDamping);
        wheel.setMaxSuspensionForce(tuning.maxSuspensionForce);
        wheels.add(wheel);
        if (vehicleId != 0) {
            wheel.setVehicleId(vehicleId, addWheel(vehicleId, wheel.getLocation(), wheel.getDirection(), wheel.getAxle(), wheel.getRestLength(), wheel.getRadius(), tuning, wheel.isFrontWheel()));
        }
        return wheel;
    }

    /**
     * Remove a wheel.
     *
     * @param wheel the index of the wheel to remove (&ge;0)
     */
    public void removeWheel(int wheel) {
        wheels.remove(wheel);
        rebuildRigidBody();
//        updateDebugShape();
    }

    /**
     * Access the indexed wheel of this vehicle.
     *
     * @param wheel the index of the wheel to access (&ge;0, &lt;count)
     * @return the pre-existing instance
     */
    public VehicleWheel getWheel(int wheel) {
        return wheels.get(wheel);
    }

    /**
     * Read the number of wheels on this vehicle.
     *
     * @return count (&ge;0)
     */
    public int getNumWheels() {
        return wheels.size();
    }

    /**
     * Read the initial friction for new wheels.
     *
     * @return the coefficient of friction between tyre and ground
     * (0.8&rarr;realistic car, 10000&rarr;kart racer)
     */
    public float getFrictionSlip() {
        return tuning.frictionSlip;
    }

    /**
     * Alter the initial friction for new wheels. Effective only before adding
     * wheels. After adding a wheel, use {@link #setFrictionSlip(int, float)}.
     * <p>
     * For better handling, increase the friction.
     *
     * @param frictionSlip the desired coefficient of friction between tyre and
     * ground (0.8&rarr;realistic car, 10000&rarr;kart racer, default=10.5)
     */
    public void setFrictionSlip(float frictionSlip) {
        tuning.frictionSlip = frictionSlip;
    }

    /**
     * Alter the friction of the indexed wheel.
     * <p>
     * For better handling, increase the friction.
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param frictionSlip the desired coefficient of friction between tyre and
     * ground (0.8&rarr;realistic car, 10000&rarr;kart racer)
     */
    public void setFrictionSlip(int wheel, float frictionSlip) {
        wheels.get(wheel).setFrictionSlip(frictionSlip);
    }

    /**
     * Alter the roll influence of the indexed wheel.
     * <p>
     * The roll-influence factor reduces (or magnifies) any torque contributed
     * by the wheel that would tend to cause the vehicle to roll over. This is a
     * bit of a hack, but it's quite effective.
     * <p>
     * If the friction between the tyres and the ground is too high, you may
     * reduce this factor to prevent the vehicle from rolling over. You should
     * also try lowering the vehicle's center of mass.
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param rollInfluence the desired roll-influence factor (0&rarr;no roll
     * torque, 1&rarr;realistic behavior, default=1)
     */
    public void setRollInfluence(int wheel, float rollInfluence) {
        wheels.get(wheel).setRollInfluence(rollInfluence);
    }

    /**
     * Read the initial maximum suspension travel distance for new wheels.
     *
     * @return the maximum distance the suspension can be compressed (in
     * centimeters)
     */
    public float getMaxSuspensionTravelCm() {
        return tuning.maxSuspensionTravelCm;
    }

    /**
     * Alter the initial maximum suspension travel distance for new wheels.
     * Effective only before adding wheels. After adding a wheel, use
     * {@link #setMaxSuspensionTravelCm(int, float)}.
     *
     * @param maxSuspensionTravelCm the desired maximum distance the suspension
     * can be compressed (in centimeters, default=500)
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        tuning.maxSuspensionTravelCm = maxSuspensionTravelCm;
    }

    /**
     * Alter the maximum suspension travel distance for the indexed wheel.
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param maxSuspensionTravelCm the desired maximum distance the suspension
     * can be compressed (in centimeters)
     */
    public void setMaxSuspensionTravelCm(int wheel, float maxSuspensionTravelCm) {
        wheels.get(wheel).setMaxSuspensionTravelCm(maxSuspensionTravelCm);
    }

    /**
     * Read the initial maximum suspension force for new wheels.
     *
     * @return the maximum force per wheel
     */
    public float getMaxSuspensionForce() {
        return tuning.maxSuspensionForce;
    }

    /**
     * Alter the initial maximum suspension force for new wheels. Effective only
     * before adding wheels. After adding a wheel, use
     * {@link #setMaxSuspensionForce(int, float)}.
     * <p>
     * If the suspension cannot handle the vehicle's weight, increase this
     * limit.
     *
     * @param maxSuspensionForce the desired maximum force per wheel
     * (default=6000)
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        tuning.maxSuspensionForce = maxSuspensionForce;
    }

    /**
     * Alter the maximum suspension force for the specified wheel.
     * <p>
     * If the suspension cannot handle the vehicle's weight, increase this
     * limit.
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param maxSuspensionForce the desired maximum force per wheel
     * (default=6000)
     */
    public void setMaxSuspensionForce(int wheel, float maxSuspensionForce) {
        wheels.get(wheel).setMaxSuspensionForce(maxSuspensionForce);
    }

    /**
     * Read the initial damping (when the suspension is compressed) for new
     * wheels.
     *
     * @return the damping coefficient
     */
    public float getSuspensionCompression() {
        return tuning.suspensionCompression;
    }

    /**
     * Alter the initial damping (when the suspension is compressed) for new
     * wheels. Effective only before adding wheels. After adding a wheel, use
     * {@link #setSuspensionCompression(int, float)}.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param suspensionCompression the desired damping coefficient
     * (default=0.83)
     */
    public void setSuspensionCompression(float suspensionCompression) {
        tuning.suspensionCompression = suspensionCompression;
    }

    /**
     * Alter the damping (when the suspension is compressed) for the indexed
     * wheel.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param suspensionCompression the desired damping coefficient
     */
    public void setSuspensionCompression(int wheel, float suspensionCompression) {
        wheels.get(wheel).setWheelsDampingCompression(suspensionCompression);
    }

    /**
     * Read the initial damping (when the suspension is expanded) for new
     * wheels.
     *
     * @return the damping coefficient
     */
    public float getSuspensionDamping() {
        return tuning.suspensionDamping;
    }

    /**
     * Alter the initial damping (when the suspension is expanded) for new
     * wheels. Effective only before adding wheels. After adding a wheel, use
     * {@link #setSuspensionCompression(int, float)}.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param suspensionDamping the desired damping coefficient (default=0.88)
     */
    public void setSuspensionDamping(float suspensionDamping) {
        tuning.suspensionDamping = suspensionDamping;
    }

    /**
     * Alter the damping (when the suspension is expanded) for the indexed
     * wheel.
     * <p>
     * Set to k * 2 * FastMath.sqrt(m_suspensionStiffness) where k is the
     * damping ratio:
     * <p>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping, k between 0.1 and
     * 0.3 are good values
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param suspensionDamping the desired damping coefficient
     */
    public void setSuspensionDamping(int wheel, float suspensionDamping) {
        wheels.get(wheel).setWheelsDampingRelaxation(suspensionDamping);
    }

    /**
     * Read the initial suspension stiffness for new wheels.
     *
     * @return the stiffness constant (10&rarr;off-road buggy, 50&rarr;sports
     * car, 200&rarr;Formula-1 race car)
     */
    public float getSuspensionStiffness() {
        return tuning.suspensionStiffness;
    }

    /**
     * Alter the initial suspension stiffness for new wheels. Effective only
     * before adding wheels. After adding a wheel, use
     * {@link #setSuspensionStiffness(int, float)}.
     *
     * @param suspensionStiffness the desired stiffness coefficient
     * (10&rarr;off-road buggy, 50&rarr;sports car, 200&rarr;Formula-1 race car,
     * default=5.88)
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        tuning.suspensionStiffness = suspensionStiffness;
    }

    /**
     * Alter the suspension stiffness of the indexed wheel.
     *
     * @param wheel the index of the wheel to modify (&ge;0)
     * @param suspensionStiffness the desired stiffness coefficient
     * (10&rarr;off-road buggy, 50&rarr;sports car, 200&rarr;Formula-1 race car,
     * default=5.88)
     */
    public void setSuspensionStiffness(int wheel, float suspensionStiffness) {
        wheels.get(wheel).setSuspensionStiffness(suspensionStiffness);
    }

    /**
     * Reset this vehicle's suspension.
     */
    public void resetSuspension() {
        resetSuspension(vehicleId);
    }

    private native void resetSuspension(long vehicleId);

    /**
     * Apply the specified engine force to all wheels. Works continuously.
     *
     * @param force the desired amount of force
     */
    public void accelerate(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            accelerate(i, force);
        }
    }

    /**
     * Apply the given engine force to the indexed wheel. Works continuously.
     *
     * @param wheel the index of the wheel to apply the force to (&ge;0)
     * @param force the desired amount of force
     */
    public void accelerate(int wheel, float force) {
        applyEngineForce(vehicleId, wheel, force);

    }

    private native void applyEngineForce(long vehicleId, int wheel, float force);

    /**
     * Alter the steering angle of all front wheels.
     *
     * @param value the desired steering angle (in radians, 0=straight)
     */
    public void steer(float value) {
        for (int i = 0; i < wheels.size(); i++) {
            if (getWheel(i).isFrontWheel()) {
                steer(i, value);
            }
        }
    }

    /**
     * Alter the steering angle of the indexed wheel.
     *
     * @param wheel the index of the wheel to steer (&ge;0)
     * @param value the desired steering angle (in radians, 0=straight)
     */
    public void steer(int wheel, float value) {
        steer(vehicleId, wheel, value);
    }

    private native void steer(long vehicleId, int wheel, float value);

    /**
     * Apply the given brake force to all wheels. Works continuously.
     *
     * @param force the desired amount of force
     */
    public void brake(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            brake(i, force);
        }
    }

    /**
     * Apply the given brake force to the indexed wheel. Works continuously.
     *
     * @param wheel the index of the wheel to apply the force to (&ge;0)
     * @param force the desired amount of force
     */
    public void brake(int wheel, float force) {
        brake(vehicleId, wheel, force);
    }

    private native void brake(long vehicleId, int wheel, float force);

    /**
     * Read the vehicle's speed in km/h.
     *
     * @return speed (in kilometers per hour)
     */
    public float getCurrentVehicleSpeedKmHour() {
        return getCurrentVehicleSpeedKmHour(vehicleId);
    }

    private native float getCurrentVehicleSpeedKmHour(long vehicleId);

    /**
     * Copy the vehicle's forward direction.
     *
     * @param vector storage for the result (modified if not null)
     * @return a direction vector (in physics-space coordinates, either the
     * provided storage or a new vector, not null)
     */
    public Vector3f getForwardVector(Vector3f vector) {
        if (vector == null) {
            vector = new Vector3f();
        }
        getForwardVector(vehicleId, vector);
        return vector;
    }

    private native void getForwardVector(long objectId, Vector3f vector);

    /**
     * used internally
     *
     * @return the unique identifier
     */
    public long getVehicleId() {
        return vehicleId;
    }

    /**
     * De-serialize this vehicle, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        tuning = new VehicleTuning();
        tuning.frictionSlip = capsule.readFloat("frictionSlip", 10.5f);
        tuning.maxSuspensionTravelCm = capsule.readFloat("maxSuspensionTravelCm", 500f);
        tuning.maxSuspensionForce = capsule.readFloat("maxSuspensionForce", 6000f);
        tuning.suspensionCompression = capsule.readFloat("suspensionCompression", 0.83f);
        tuning.suspensionDamping = capsule.readFloat("suspensionDamping", 0.88f);
        tuning.suspensionStiffness = capsule.readFloat("suspensionStiffness", 5.88f);
        wheels = capsule.readSavableArrayList("wheelsList", new ArrayList<VehicleWheel>());
        motionState.setVehicle(this);
        super.read(im);
    }

    /**
     * Serialize this vehicle, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(tuning.frictionSlip, "frictionSlip", 10.5f);
        capsule.write(tuning.maxSuspensionTravelCm, "maxSuspensionTravelCm", 500f);
        capsule.write(tuning.maxSuspensionForce, "maxSuspensionForce", 6000f);
        capsule.write(tuning.suspensionCompression, "suspensionCompression", 0.83f);
        capsule.write(tuning.suspensionDamping, "suspensionDamping", 0.88f);
        capsule.write(tuning.suspensionStiffness, "suspensionStiffness", 5.88f);
        capsule.writeSavableArrayList(wheels, "wheelsList", new ArrayList<VehicleWheel>());
        super.write(ex);
    }

    /**
     * Finalize this vehicle just before it is destroyed. Should be invoked only
     * by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing RayCaster {0}", Long.toHexString(rayCasterId));
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Vehicle {0}", Long.toHexString(vehicleId));
        finalizeNative(rayCasterId, vehicleId);
    }

    private native void finalizeNative(long rayCaster, long vehicle);
}
