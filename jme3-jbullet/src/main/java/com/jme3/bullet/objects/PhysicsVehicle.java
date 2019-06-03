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

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.vehicle.*;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <p>PhysicsVehicleNode - Special PhysicsNode that implements vehicle functions</p>
 * <p>
 * <i>From bullet manual:</i><br>
 * For most vehicle simulations, it is recommended to use the simplified Bullet
 * vehicle model as provided in btRaycastVehicle. Instead of simulation each wheel
 * and chassis as separate rigid bodies, connected by constraints, it uses a simplified model.
 * This simplified model has many benefits, and is widely used in commercial driving games.<br>
 * The entire vehicle is represented as a single rigidbody, the chassis.
 * The collision detection of the wheels is approximated by ray casts,
 * and the tire friction is a basic anisotropic friction model.
 * </p>
 * @author normenhansen
 */
public class PhysicsVehicle extends PhysicsRigidBody {

    protected RaycastVehicle vehicle;
    protected VehicleTuning tuning;
    protected VehicleRaycaster rayCaster;
    protected ArrayList<VehicleWheel> wheels = new ArrayList<VehicleWheel>();
    protected PhysicsSpace physicsSpace;

    public PhysicsVehicle() {
    }

    public PhysicsVehicle(CollisionShape shape) {
        super(shape);
    }

    public PhysicsVehicle(CollisionShape shape, float mass) {
        super(shape, mass);
    }

    /**
     * used internally
     */
    public void updateWheels() {
        if (vehicle != null) {
            for (int i = 0; i < wheels.size(); i++) {
                vehicle.updateWheelTransform(i, true);
                wheels.get(i).updatePhysicsState();
            }
        }
    }

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
        if (tuning == null) {
            tuning = new VehicleTuning();
        }
        rBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
        motionState.setVehicle(this);
        if (physicsSpace != null) {
            createVehicle(physicsSpace);
        }
    }

    /**
     * Used internally, creates the actual vehicle constraint when vehicle is added to phyicsspace
     */
    public void createVehicle(PhysicsSpace space) {
        physicsSpace = space;
        if (space == null) {
            return;
        }
        rayCaster = new DefaultVehicleRaycaster(space.getDynamicsWorld());
        vehicle = new RaycastVehicle(tuning, rBody, rayCaster);
        vehicle.setCoordinateSystem(0, 1, 2);
        for (VehicleWheel wheel : wheels) {
            wheel.setWheelInfo(vehicle.addWheel(Converter.convert(wheel.getLocation()), Converter.convert(wheel.getDirection()), Converter.convert(wheel.getAxle()),
                    wheel.getRestLength(), wheel.getRadius(), tuning, wheel.isFrontWheel()));
        }
    }

    /**
     * Add a wheel to this vehicle
     * @param connectionPoint The starting point of the ray, where the suspension connects to the chassis (chassis space)
     * @param direction the direction of the wheel (should be -Y / 0,-1,0 for a normal car)
     * @param axle The axis of the wheel, pointing right in vehicle direction (should be -X / -1,0,0 for a normal car)
     * @param suspensionRestLength The current length of the suspension (metres)
     * @param wheelRadius the wheel radius
     * @param isFrontWheel sets if this wheel is a front wheel (steering)
     * @return the PhysicsVehicleWheel object to get/set infos on the wheel
     */
    public VehicleWheel addWheel(Vector3f connectionPoint, Vector3f direction, Vector3f axle, float suspensionRestLength, float wheelRadius, boolean isFrontWheel) {
        return addWheel(null, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
    }

    /**
     * Add a wheel to this vehicle
     * @param spat the wheel Geometry
     * @param connectionPoint The starting point of the ray, where the suspension connects to the chassis (chassis space)
     * @param direction the direction of the wheel (should be -Y / 0,-1,0 for a normal car)
     * @param axle The axis of the wheel, pointing right in vehicle direction (should be -X / -1,0,0 for a normal car)
     * @param suspensionRestLength The current length of the suspension (metres)
     * @param wheelRadius the wheel radius
     * @param isFrontWheel sets if this wheel is a front wheel (steering)
     * @return the PhysicsVehicleWheel object to get/set infos on the wheel
     */
    public VehicleWheel addWheel(Spatial spat, Vector3f connectionPoint, Vector3f direction, Vector3f axle, float suspensionRestLength, float wheelRadius, boolean isFrontWheel) {
        VehicleWheel wheel = null;
        if (spat == null) {
            wheel = new VehicleWheel(connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
        } else {
            wheel = new VehicleWheel(spat, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
        }
        if (vehicle != null) {
            WheelInfo info = vehicle.addWheel(Converter.convert(connectionPoint), Converter.convert(direction), Converter.convert(axle),
                    suspensionRestLength, wheelRadius, tuning, isFrontWheel);
            wheel.setWheelInfo(info);
        }
        wheel.setFrictionSlip(tuning.frictionSlip);
        wheel.setMaxSuspensionTravelCm(tuning.maxSuspensionTravelCm);
        wheel.setSuspensionStiffness(tuning.suspensionStiffness);
        wheel.setWheelsDampingCompression(tuning.suspensionCompression);
        wheel.setWheelsDampingRelaxation(tuning.suspensionDamping);
        wheel.setMaxSuspensionForce(tuning.maxSuspensionForce);
        wheels.add(wheel);
        return wheel;
    }

    /**
     * This rebuilds the vehicle as there is no way in bullet to remove a wheel.
     * @param wheel
     */
    public void removeWheel(int wheel) {
        wheels.remove(wheel);
        rebuildRigidBody();
    }

    /**
     * You can get access to the single wheels via this method.
     * @param wheel the wheel index
     * @return the WheelInfo of the selected wheel
     */
    public VehicleWheel getWheel(int wheel) {
        return wheels.get(wheel);
    }

    public int getNumWheels() {
        return wheels.size();
    }

    /**
     * @return the frictionSlip
     */
    public float getFrictionSlip() {
        return tuning.frictionSlip;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The coefficient of friction between the tyre and the ground.
     * Should be about 0.8 for realistic cars, but can increased for better handling.
     * Set large (10000.0) for kart racers
     * @param frictionSlip the frictionSlip to set
     */
    public void setFrictionSlip(float frictionSlip) {
        tuning.frictionSlip = frictionSlip;
    }

    /**
     * The coefficient of friction between the tyre and the ground.
     * Should be about 0.8 for realistic cars, but can increased for better handling.
     * Set large (10000.0) for kart racers
     * @param wheel
     * @param frictionSlip
     */
    public void setFrictionSlip(int wheel, float frictionSlip) {
        wheels.get(wheel).setFrictionSlip(frictionSlip);
    }

    /**
     * Reduces the rolling torque applied from the wheels that cause the vehicle to roll over.
     * This is a bit of a hack, but it's quite effective. 0.0 = no roll, 1.0 = physical behaviour.
     * If m_frictionSlip is too high, you'll need to reduce this to stop the vehicle rolling over.
     * You should also try lowering the vehicle's centre of mass
     */
    public void setRollInfluence(int wheel, float rollInfluence) {
        wheels.get(wheel).setRollInfluence(rollInfluence);
    }

    /**
     * @return the maxSuspensionTravelCm
     */
    public float getMaxSuspensionTravelCm() {
        return tuning.maxSuspensionTravelCm;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The maximum distance the suspension can be compressed (centimetres)
     * @param maxSuspensionTravelCm the maxSuspensionTravelCm to set
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        tuning.maxSuspensionTravelCm = maxSuspensionTravelCm;
    }

    /**
     * The maximum distance the suspension can be compressed (centimetres)
     * @param wheel
     * @param maxSuspensionTravelCm
     */
    public void setMaxSuspensionTravelCm(int wheel, float maxSuspensionTravelCm) {
        wheels.get(wheel).setMaxSuspensionTravelCm(maxSuspensionTravelCm);
    }

    public float getMaxSuspensionForce() {
        return tuning.maxSuspensionForce;
    }

    /**
     * This value caps the maximum suspension force, raise this above the default 6000 if your suspension cannot
     * handle the weight of your vehicle.
     * @param maxSuspensionForce
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        tuning.maxSuspensionForce = maxSuspensionForce;
    }

    /**
     * This value caps the maximum suspension force, raise this above the default 6000 if your suspension cannot
     * handle the weight of your vehicle.
     * @param wheel
     * @param maxSuspensionForce
     */
    public void setMaxSuspensionForce(int wheel, float maxSuspensionForce) {
        wheels.get(wheel).setMaxSuspensionForce(maxSuspensionForce);
    }

    /**
     * @return the suspensionCompression
     */
    public float getSuspensionCompression() {
        return tuning.suspensionCompression;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is compressed.
     * Set to k * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to critical damping.<br>
     * k = 0.0 undamped/bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     * @param suspensionCompression the suspensionCompression to set
     */
    public void setSuspensionCompression(float suspensionCompression) {
        tuning.suspensionCompression = suspensionCompression;
    }

    /**
     * The damping coefficient for when the suspension is compressed.
     * Set to k * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to critical damping.<br>
     * k = 0.0 undamped/bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     * @param wheel
     * @param suspensionCompression
     */
    public void setSuspensionCompression(int wheel, float suspensionCompression) {
        wheels.get(wheel).setWheelsDampingCompression(suspensionCompression);
    }

    /**
     * @return the suspensionDamping
     */
    public float getSuspensionDamping() {
        return tuning.suspensionDamping;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is expanding.
     * See the comments for setSuspensionCompression for how to set k.
     * @param suspensionDamping the suspensionDamping to set
     */
    public void setSuspensionDamping(float suspensionDamping) {
        tuning.suspensionDamping = suspensionDamping;
    }

    /**
     * The damping coefficient for when the suspension is expanding.
     * See the comments for setSuspensionCompression for how to set k.
     * @param wheel
     * @param suspensionDamping
     */
    public void setSuspensionDamping(int wheel, float suspensionDamping) {
        wheels.get(wheel).setWheelsDampingRelaxation(suspensionDamping);
    }

    /**
     * @return the suspensionStiffness
     */
    public float getSuspensionStiffness() {
        return tuning.suspensionStiffness;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The stiffness constant for the suspension.  10.0 - Offroad buggy, 50.0 - Sports car, 200.0 - F1 Car
     * @param suspensionStiffness 
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        tuning.suspensionStiffness = suspensionStiffness;
    }

    /**
     * The stiffness constant for the suspension.  10.0 - Offroad buggy, 50.0 - Sports car, 200.0 - F1 Car
     * @param wheel
     * @param suspensionStiffness
     */
    public void setSuspensionStiffness(int wheel, float suspensionStiffness) {
        wheels.get(wheel).setSuspensionStiffness(suspensionStiffness);
    }

    /**
     * Reset the suspension
     */
    public void resetSuspension() {
        vehicle.resetSuspension();
    }

    /**
     * Apply the given engine force to all wheels, works continuously
     * @param force the force
     */
    public void accelerate(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            vehicle.applyEngineForce(force, i);
        }
    }

    /**
     * Apply the given engine force, works continuously
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void accelerate(int wheel, float force) {
        vehicle.applyEngineForce(force, wheel);
    }

    /**
     * Set the given steering value to all front wheels (0 = forward)
     * @param value the steering angle of the front wheels (Pi = 360deg)
     */
    public void steer(float value) {
        for (int i = 0; i < wheels.size(); i++) {
            if (getWheel(i).isFrontWheel()) {
                vehicle.setSteeringValue(value, i);
            }
        }
    }

    /**
     * Set the given steering value to the given wheel (0 = forward)
     * @param wheel the wheel to set the steering on
     * @param value the steering angle of the front wheels (Pi = 360deg)
     */
    public void steer(int wheel, float value) {
        vehicle.setSteeringValue(value, wheel);
    }

    /**
     * Apply the given brake force to all wheels, works continuously
     * @param force the force
     */
    public void brake(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            vehicle.setBrake(force, i);
        }
    }

    /**
     * Apply the given brake force, works continuously
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void brake(int wheel, float force) {
        vehicle.setBrake(force, wheel);
    }

    /**
     * Get the current speed of the vehicle in km/h
     * @return the speed
     */
    public float getCurrentVehicleSpeedKmHour() {
        return vehicle.getCurrentSpeedKmHour();
    }

    /**
     * Get the current forward vector of the vehicle in world coordinates
     * @param vector The object to write the forward vector values to.
     * Passing null will cause a new {@link Vector3f} to be created.
     * @return The forward vector
     */
    public Vector3f getForwardVector(Vector3f vector) {
        if (vector == null) {
            vector = new Vector3f();
        }
        vehicle.getForwardVector(tempVec);
        Converter.convert(tempVec, vector);
        return vector;
    }

    /**
     * used internally
     */
    public RaycastVehicle getVehicleId() {
        return vehicle;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

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
}
