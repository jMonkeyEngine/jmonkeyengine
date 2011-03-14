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

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Node;
import java.io.IOException;

/**
 * <p>PhysicsVehicleNode - Special PhysicsNode that implements vehicle functions</p>
 * <p>
 * <i>From bullet manual:</i><br>
 * For most vehicle simulations, it is recommended to use the simplified Bullet
 * vehicle model as provided in btRaycast((PhysicsVehicleControl)collisionObject). Instead of simulation each wheel
 * and chassis as separate rigid bodies, connected by constraints, it uses a simplified model.
 * This simplified model has many benefits, and is widely used in commercial driving games.<br>
 * The entire vehicle is represented as a single rigidbody, the chassis.
 * The collision detection of the wheels is approximated by ray casts,
 * and the tire friction is a basic anisotropic friction model.
 * </p>
 * @see com.jmex.jbullet.nodes.PhysicsNode
 * @see com.jmex.jbullet.PhysicsSpace
 * @author normenhansen
 * @deprecated in favor of physics Controls
 */
@Deprecated
public class PhysicsVehicleNode extends PhysicsNode {

    public PhysicsVehicleNode() {
    }

    public PhysicsVehicleNode(CollisionShape shape) {
        collisionObject = new VehicleControl(shape);
        addControl(((VehicleControl) collisionObject));
    }

    public PhysicsVehicleNode(Spatial child, CollisionShape shape) {
        collisionObject = new VehicleControl(shape);
        attachChild(child);
        addControl(((VehicleControl) collisionObject));
    }

    public PhysicsVehicleNode(Spatial child, CollisionShape shape, float mass) {
        collisionObject = new VehicleControl(shape);
        ((VehicleControl) collisionObject).setMass(mass);
        attachChild(child);
        addControl(((VehicleControl) collisionObject));
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
        if (spat != null) {
            Node wheelNode=new Node("wheelNode");
            wheelNode.attachChild(spat);
            attachChild(wheelNode);
            return ((VehicleControl) collisionObject).addWheel(wheelNode, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
        }
        return ((VehicleControl) collisionObject).addWheel(spat, connectionPoint, direction, axle, suspensionRestLength, wheelRadius, isFrontWheel);
    }

    /**
     * This rebuilds the vehicle as there is no way in bullet to remove a wheel.
     * @param wheel
     */
    public void removeWheel(int wheel) {
        ((VehicleControl) collisionObject).removeWheel(wheel);
    }

    /**
     * You can get access to the single wheels via this method.
     * @param wheel the wheel index
     * @return the WheelInfo of the selected wheel
     */
    public VehicleWheel getWheel(int wheel) {
        return ((VehicleControl) collisionObject).getWheel(wheel);
    }

    /**
     * @return the frictionSlip
     */
    public float getFrictionSlip() {
        return ((VehicleControl) collisionObject).getFrictionSlip();
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
        ((VehicleControl) collisionObject).setFrictionSlip(frictionSlip);
    }

    /**
     * The coefficient of friction between the tyre and the ground.
     * Should be about 0.8 for realistic cars, but can increased for better handling.
     * Set large (10000.0) for kart racers
     * @param wheel
     * @param frictionSlip
     */
    public void setFrictionSlip(int wheel, float frictionSlip) {
        ((VehicleControl) collisionObject).setFrictionSlip(wheel, frictionSlip);
    }

    /**
     * Reduces the rolling torque applied from the wheels that cause the vehicle to roll over.
     * This is a bit of a hack, but it's quite effective. 0.0 = no roll, 1.0 = physical behaviour.
     * If m_frictionSlip is too high, you'll need to reduce this to stop the vehicle rolling over.
     * You should also try lowering the vehicle's centre of mass
     */
    public void setRollInfluence(int wheel, float rollInfluence) {
        ((VehicleControl) collisionObject).setRollInfluence(wheel, rollInfluence);
    }

    /**
     * @return the maxSuspensionTravelCm
     */
    public float getMaxSuspensionTravelCm() {
        return ((VehicleControl) collisionObject).getMaxSuspensionTravelCm();
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The maximum distance the suspension can be compressed (centimetres)
     * @param maxSuspensionTravelCm the maxSuspensionTravelCm to set
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        ((VehicleControl) collisionObject).setMaxSuspensionTravelCm(maxSuspensionTravelCm);
    }

    /**
     * The maximum distance the suspension can be compressed (centimetres)
     * @param wheel
     * @param maxSuspensionTravelCm
     */
    public void setMaxSuspensionTravelCm(int wheel, float maxSuspensionTravelCm) {
        ((VehicleControl) collisionObject).setMaxSuspensionForce(wheel, maxSuspensionTravelCm);
    }

    public float getMaxSuspensionForce() {
        return ((VehicleControl) collisionObject).getMaxSuspensionForce();
    }

    /**
     * This vaue caps the maximum suspension force, raise this above the default 6000 if your suspension cannot
     * handle the weight of your vehcile.
     * @param maxSuspensionForce
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        ((VehicleControl) collisionObject).setMaxSuspensionForce(maxSuspensionForce);
    }

    /**
     * This vaue caps the maximum suspension force, raise this above the default 6000 if your suspension cannot
     * handle the weight of your vehcile.
     * @param wheel
     * @param maxSuspensionForce
     */
    public void setMaxSuspensionForce(int wheel, float maxSuspensionForce) {
        ((VehicleControl) collisionObject).setMaxSuspensionForce(wheel, maxSuspensionForce);
    }

    /**
     * @return the suspensionCompression
     */
    public float getSuspensionCompression() {
        return ((VehicleControl) collisionObject).getSuspensionCompression();
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is compressed.
     * Set to k * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to critical damping.<br>
     * k = 0.0 undamped & bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     * @param suspensionCompression the suspensionCompression to set
     */
    public void setSuspensionCompression(float suspensionCompression) {
        ((VehicleControl) collisionObject).setSuspensionCompression(suspensionCompression);
    }

    /**
     * The damping coefficient for when the suspension is compressed.
     * Set to k * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to critical damping.<br>
     * k = 0.0 undamped & bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     * @param wheel
     * @param suspensionCompression
     */
    public void setSuspensionCompression(int wheel, float suspensionCompression) {
        ((VehicleControl) collisionObject).setSuspensionCompression(wheel, suspensionCompression);
    }

    /**
     * @return the suspensionDamping
     */
    public float getSuspensionDamping() {
        return ((VehicleControl) collisionObject).getSuspensionDamping();
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is expanding.
     * See the comments for setSuspensionCompression for how to set k.
     * @param suspensionDamping the suspensionDamping to set
     */
    public void setSuspensionDamping(float suspensionDamping) {
        ((VehicleControl) collisionObject).setSuspensionDamping(suspensionDamping);
    }

    /**
     * The damping coefficient for when the suspension is expanding.
     * See the comments for setSuspensionCompression for how to set k.
     * @param wheel
     * @param suspensionDamping
     */
    public void setSuspensionDamping(int wheel, float suspensionDamping) {
        ((VehicleControl) collisionObject).setSuspensionDamping(wheel, suspensionDamping);
    }

    /**
     * @return the suspensionStiffness
     */
    public float getSuspensionStiffness() {
        return ((VehicleControl) collisionObject).getSuspensionStiffness();
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The stiffness constant for the suspension.  10.0 - Offroad buggy, 50.0 - Sports car, 200.0 - F1 Car
     * @param suspensionStiffness 
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        ((VehicleControl) collisionObject).setSuspensionStiffness(suspensionStiffness);
    }

    /**
     * The stiffness constant for the suspension.  10.0 - Offroad buggy, 50.0 - Sports car, 200.0 - F1 Car
     * @param wheel
     * @param suspensionStiffness
     */
    public void setSuspensionStiffness(int wheel, float suspensionStiffness) {
        ((VehicleControl) collisionObject).setSuspensionStiffness(wheel, suspensionStiffness);
    }

    /**
     * Reset the suspension
     */
    public void resetSuspension() {
        ((VehicleControl) collisionObject).resetSuspension();
    }

    /**
     * Apply the given engine force to all wheels, works continuously
     * @param force the force
     */
    public void accelerate(float force) {
        ((VehicleControl) collisionObject).accelerate(force);
    }

    /**
     * Apply the given engine force, works continuously
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void accelerate(int wheel, float force) {
        ((VehicleControl) collisionObject).accelerate(wheel, force);
    }

    /**
     * Set the given steering value to all front wheels (0 = forward)
     * @param value the steering angle of the front wheels (Pi = 360deg)
     */
    public void steer(float value) {
        ((VehicleControl) collisionObject).steer(value);
    }

    /**
     * Set the given steering value to the given wheel (0 = forward)
     * @param wheel the wheel to set the steering on
     * @param value the steering angle of the front wheels (Pi = 360deg)
     */
    public void steer(int wheel, float value) {
        ((VehicleControl) collisionObject).steer(wheel, value);
    }

    /**
     * Apply the given brake force to all wheels, works continuously
     * @param force the force
     */
    public void brake(float force) {
        ((VehicleControl) collisionObject).brake(force);
    }

    /**
     * Apply the given brake force, works continuously
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void brake(int wheel, float force) {
        ((VehicleControl) collisionObject).brake(wheel, force);
    }

    /**
     * Get the current speed of the vehicle in km/h
     * @return
     */
    public float getCurrentVehicleSpeedKmHour() {
        return ((VehicleControl) collisionObject).getCurrentVehicleSpeedKmHour();
    }

    /**
     * Get the current forward vector of the vehicle in world coordinates
     * @param vector
     * @return
     */
    public Vector3f getForwardVector(Vector3f vector) {
        return ((VehicleControl) collisionObject).getForwardVector(vector);
    }

    /**
     * used internally
     */
    public PhysicsVehicle getVehicle() {
        return ((VehicleControl) collisionObject);
    }

    public void destroy() {
        ((VehicleControl) collisionObject).destroy();
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
    }
}
