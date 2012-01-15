/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.bulletphysics.dynamics.RigidBody;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.util.Converter;
import com.jme3.export.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;

/**
 * Stores info about one wheel of a PhysicsVehicle
 * @author normenhansen
 */
public class VehicleWheel implements Savable {

    protected com.bulletphysics.dynamics.vehicle.WheelInfo wheelInfo;
    protected boolean frontWheel;
    protected Vector3f location = new Vector3f();
    protected Vector3f direction = new Vector3f();
    protected Vector3f axle = new Vector3f();
    protected float suspensionStiffness = 20.0f;
    protected float wheelsDampingRelaxation = 2.3f;
    protected float wheelsDampingCompression = 4.4f;
    protected float frictionSlip = 10.5f;
    protected float rollInfluence = 1.0f;
    protected float maxSuspensionTravelCm = 500f;
    protected float maxSuspensionForce = 6000f;
    protected float radius = 0.5f;
    protected float restLength = 1f;
    protected Vector3f wheelWorldLocation = new Vector3f();
    protected Quaternion wheelWorldRotation = new Quaternion();
    protected Spatial wheelSpatial;
    protected com.jme3.math.Matrix3f tmp_Matrix = new com.jme3.math.Matrix3f();
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();
    private boolean applyLocal = false;

    public VehicleWheel() {
    }

    public VehicleWheel(Spatial spat, Vector3f location, Vector3f direction, Vector3f axle,
            float restLength, float radius, boolean frontWheel) {
        this(location, direction, axle, restLength, radius, frontWheel);
        wheelSpatial = spat;
    }

    public VehicleWheel(Vector3f location, Vector3f direction, Vector3f axle,
            float restLength, float radius, boolean frontWheel) {
        this.location.set(location);
        this.direction.set(direction);
        this.axle.set(axle);
        this.frontWheel = frontWheel;
        this.restLength = restLength;
        this.radius = radius;
    }

    public synchronized void updatePhysicsState() {
        Converter.convert(wheelInfo.worldTransform.origin, wheelWorldLocation);
        Converter.convert(wheelInfo.worldTransform.basis, tmp_Matrix);
        wheelWorldRotation.fromRotationMatrix(tmp_Matrix);
    }

    public synchronized void applyWheelTransform() {
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

    public com.bulletphysics.dynamics.vehicle.WheelInfo getWheelInfo() {
        return wheelInfo;
    }

    public void setWheelInfo(com.bulletphysics.dynamics.vehicle.WheelInfo wheelInfo) {
        this.wheelInfo = wheelInfo;
        applyInfo();
    }

    public boolean isFrontWheel() {
        return frontWheel;
    }

    public void setFrontWheel(boolean frontWheel) {
        this.frontWheel = frontWheel;
        applyInfo();
    }

    public Vector3f getLocation() {
        return location;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public Vector3f getAxle() {
        return axle;
    }

    public float getSuspensionStiffness() {
        return suspensionStiffness;
    }

    /**
     * the stiffness constant for the suspension.  10.0 - Offroad buggy, 50.0 - Sports car, 200.0 - F1 Car
     * @param suspensionStiffness
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        this.suspensionStiffness = suspensionStiffness;
        applyInfo();
    }

    public float getWheelsDampingRelaxation() {
        return wheelsDampingRelaxation;
    }

    /**
     * the damping coefficient for when the suspension is expanding.
     * See the comments for setWheelsDampingCompression for how to set k.
     * @param wheelsDampingRelaxation
     */
    public void setWheelsDampingRelaxation(float wheelsDampingRelaxation) {
        this.wheelsDampingRelaxation = wheelsDampingRelaxation;
        applyInfo();
    }

    public float getWheelsDampingCompression() {
        return wheelsDampingCompression;
    }

    /**
     * the damping coefficient for when the suspension is compressed.
     * Set to k * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to critical damping.<br>
     * k = 0.0 undamped & bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     * @param wheelsDampingCompression
     */
    public void setWheelsDampingCompression(float wheelsDampingCompression) {
        this.wheelsDampingCompression = wheelsDampingCompression;
        applyInfo();
    }

    public float getFrictionSlip() {
        return frictionSlip;
    }

    /**
     * the coefficient of friction between the tyre and the ground.
     * Should be about 0.8 for realistic cars, but can increased for better handling.
     * Set large (10000.0) for kart racers
     * @param frictionSlip
     */
    public void setFrictionSlip(float frictionSlip) {
        this.frictionSlip = frictionSlip;
        applyInfo();
    }

    public float getRollInfluence() {
        return rollInfluence;
    }

    /**
     * reduces the rolling torque applied from the wheels that cause the vehicle to roll over.
     * This is a bit of a hack, but it's quite effective. 0.0 = no roll, 1.0 = physical behaviour.
     * If m_frictionSlip is too high, you'll need to reduce this to stop the vehicle rolling over.
     * You should also try lowering the vehicle's centre of mass
     * @param rollInfluence the rollInfluence to set
     */
    public void setRollInfluence(float rollInfluence) {
        this.rollInfluence = rollInfluence;
        applyInfo();
    }

    public float getMaxSuspensionTravelCm() {
        return maxSuspensionTravelCm;
    }

    /**
     * the maximum distance the suspension can be compressed (centimetres)
     * @param maxSuspensionTravelCm
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        this.maxSuspensionTravelCm = maxSuspensionTravelCm;
        applyInfo();
    }

    public float getMaxSuspensionForce() {
        return maxSuspensionForce;
    }

    /**
     * The maximum suspension force, raise this above the default 6000 if your suspension cannot
     * handle the weight of your vehcile.
     * @param maxSuspensionForce
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        this.maxSuspensionForce = maxSuspensionForce;
        applyInfo();
    }

    private void applyInfo() {
        if (wheelInfo == null) {
            return;
        }
        wheelInfo.suspensionStiffness = suspensionStiffness;
        wheelInfo.wheelsDampingRelaxation = wheelsDampingRelaxation;
        wheelInfo.wheelsDampingCompression = wheelsDampingCompression;
        wheelInfo.frictionSlip = frictionSlip;
        wheelInfo.rollInfluence = rollInfluence;
        wheelInfo.maxSuspensionTravelCm = maxSuspensionTravelCm;
        wheelInfo.maxSuspensionForce = maxSuspensionForce;
        wheelInfo.wheelsRadius = radius;
        wheelInfo.bIsFrontWheel = frontWheel;
        wheelInfo.suspensionRestLength1 = restLength;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        applyInfo();
    }

    public float getRestLength() {
        return restLength;
    }

    public void setRestLength(float restLength) {
        this.restLength = restLength;
        applyInfo();
    }

    /**
     * returns the object this wheel is in contact with or null if no contact
     * @return the PhysicsCollisionObject (PhysicsRigidBody, PhysicsGhostObject)
     */
    public PhysicsCollisionObject getGroundObject() {
        if (wheelInfo.raycastInfo.groundObject == null) {
            return null;
        } else if (wheelInfo.raycastInfo.groundObject instanceof RigidBody) {
            System.out.println("RigidBody");
            return (PhysicsRigidBody) ((RigidBody) wheelInfo.raycastInfo.groundObject).getUserPointer();
        } else {
            return null;
        }
    }

    /**
     * returns the location where the wheel collides with the ground (world space)
     */
    public Vector3f getCollisionLocation(Vector3f vec) {
        Converter.convert(wheelInfo.raycastInfo.contactPointWS, vec);
        return vec;
    }

    /**
     * returns the location where the wheel collides with the ground (world space)
     */
    public Vector3f getCollisionLocation() {
        return Converter.convert(wheelInfo.raycastInfo.contactPointWS);
    }

    /**
     * returns the normal where the wheel collides with the ground (world space)
     */
    public Vector3f getCollisionNormal(Vector3f vec) {
        Converter.convert(wheelInfo.raycastInfo.contactNormalWS, vec);
        return vec;
    }

    /**
     * returns the normal where the wheel collides with the ground (world space)
     */
    public Vector3f getCollisionNormal() {
        return Converter.convert(wheelInfo.raycastInfo.contactNormalWS);
    }

    /**
     * returns how much the wheel skids on the ground (for skid sounds/smoke etc.)<br>
     * 0.0 = wheels are sliding, 1.0 = wheels have traction.
     */
    public float getSkidInfo() {
        return wheelInfo.skidInfo;
    }
    
    /**
     * returns how many degrees the wheel has turned since the last physics
     * step.
     */
    public float getDeltaRotation() {
        return wheelInfo.deltaRotation;
    }

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
     * @return the wheelSpatial
     */
    public Spatial getWheelSpatial() {
        return wheelSpatial;
    }

    /**
     * @param wheelSpatial the wheelSpatial to set
     */
    public void setWheelSpatial(Spatial wheelSpatial) {
        this.wheelSpatial = wheelSpatial;
    }

    public boolean isApplyLocal() {
        return applyLocal;
    }

    public void setApplyLocal(boolean applyLocal) {
        this.applyLocal = applyLocal;
    }
}
