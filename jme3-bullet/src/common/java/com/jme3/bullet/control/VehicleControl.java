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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author normenhansen
 */
public class VehicleControl extends PhysicsVehicle implements PhysicsControl, JmeCloneable {

    protected Spatial spatial;
    protected boolean enabled = true;
    protected PhysicsSpace space = null;
    protected boolean added = false;

    public VehicleControl() {
    }

    /**
     * Creates a new PhysicsNode with the supplied collision shape
     * @param shape
     */
    public VehicleControl(CollisionShape shape) {
        super(shape);
    }

    public VehicleControl(CollisionShape shape, float mass) {
        super(shape, mass);
    }

    public boolean isApplyPhysicsLocal() {
        return motionState.isApplyPhysicsLocal();
    }

    /**
     * When set to true, the physics coordinates will be applied to the local
     * translation of the Spatial
     * @param applyPhysicsLocal
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        motionState.setApplyPhysicsLocal(applyPhysicsLocal);
        for (Iterator<VehicleWheel> it = wheels.iterator(); it.hasNext();) {
            VehicleWheel vehicleWheel = it.next();
            vehicleWheel.setApplyLocal(applyPhysicsLocal);
        }
    }

    private Vector3f getSpatialTranslation(){
        if(motionState.isApplyPhysicsLocal()){
            return spatial.getLocalTranslation();
        }
        return spatial.getWorldTranslation();
    }

    private Quaternion getSpatialRotation(){
        if(motionState.isApplyPhysicsLocal()){
            return spatial.getLocalRotation();
        }
        return spatial.getWorldRotation();
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        VehicleControl control = new VehicleControl(collisionShape, mass);
        control.setAngularFactor(getAngularFactor());
        control.setAngularSleepingThreshold(getAngularSleepingThreshold());
        control.setAngularVelocity(getAngularVelocity());
        control.setCcdMotionThreshold(getCcdMotionThreshold());
        control.setCcdSweptSphereRadius(getCcdSweptSphereRadius());
        control.setCollideWithGroups(getCollideWithGroups());
        control.setCollisionGroup(getCollisionGroup());
        control.setDamping(getLinearDamping(), getAngularDamping());
        control.setFriction(getFriction());
        control.setGravity(getGravity());
        control.setKinematic(isKinematic());
        control.setLinearSleepingThreshold(getLinearSleepingThreshold());
        control.setLinearVelocity(getLinearVelocity());
        control.setPhysicsLocation(getPhysicsLocation());
        control.setPhysicsRotation(getPhysicsRotationMatrix());
        control.setRestitution(getRestitution());

        control.setFrictionSlip(getFrictionSlip());
        control.setMaxSuspensionTravelCm(getMaxSuspensionTravelCm());
        control.setSuspensionStiffness(getSuspensionStiffness());
        control.setSuspensionCompression(tuning.suspensionCompression);
        control.setSuspensionDamping(tuning.suspensionDamping);
        control.setMaxSuspensionForce(getMaxSuspensionForce());

        for (Iterator<VehicleWheel> it = wheels.iterator(); it.hasNext();) {
            VehicleWheel wheel = it.next();
            VehicleWheel newWheel = control.addWheel(wheel.getLocation(), wheel.getDirection(), wheel.getAxle(), wheel.getRestLength(), wheel.getRadius(), wheel.isFrontWheel());
            newWheel.setFrictionSlip(wheel.getFrictionSlip());
            newWheel.setMaxSuspensionTravelCm(wheel.getMaxSuspensionTravelCm());
            newWheel.setSuspensionStiffness(wheel.getSuspensionStiffness());
            newWheel.setWheelsDampingCompression(wheel.getWheelsDampingCompression());
            newWheel.setWheelsDampingRelaxation(wheel.getWheelsDampingRelaxation());
            newWheel.setMaxSuspensionForce(wheel.getMaxSuspensionForce());

            //TODO: bad way finding children!
            if (spatial instanceof Node) {
                Node node = (Node) spatial;
                Spatial wheelSpat = node.getChild(wheel.getWheelSpatial().getName());
                if (wheelSpat != null) {
                    newWheel.setWheelSpatial(wheelSpat);
                }
            }
        }
        control.setApplyPhysicsLocal(isApplyPhysicsLocal());
        return control;
    }

    @Override   
    public Object jmeClone() {
        VehicleControl control = new VehicleControl(collisionShape, mass);
        control.setAngularFactor(getAngularFactor());
        control.setAngularSleepingThreshold(getAngularSleepingThreshold());
        control.setAngularVelocity(getAngularVelocity());
        control.setCcdMotionThreshold(getCcdMotionThreshold());
        control.setCcdSweptSphereRadius(getCcdSweptSphereRadius());
        control.setCollideWithGroups(getCollideWithGroups());
        control.setCollisionGroup(getCollisionGroup());
        control.setDamping(getLinearDamping(), getAngularDamping());
        control.setFriction(getFriction());
        control.setGravity(getGravity());
        control.setKinematic(isKinematic());
        control.setLinearSleepingThreshold(getLinearSleepingThreshold());
        control.setLinearVelocity(getLinearVelocity());
        control.setPhysicsLocation(getPhysicsLocation());
        control.setPhysicsRotation(getPhysicsRotationMatrix());
        control.setRestitution(getRestitution());

        control.setFrictionSlip(getFrictionSlip());
        control.setMaxSuspensionTravelCm(getMaxSuspensionTravelCm());
        control.setSuspensionStiffness(getSuspensionStiffness());
        control.setSuspensionCompression(tuning.suspensionCompression);
        control.setSuspensionDamping(tuning.suspensionDamping);
        control.setMaxSuspensionForce(getMaxSuspensionForce());
    
        for (Iterator<VehicleWheel> it = wheels.iterator(); it.hasNext();) {
            VehicleWheel wheel = it.next();
            VehicleWheel newWheel = control.addWheel(wheel.getLocation(), wheel.getDirection(), wheel.getAxle(), wheel.getRestLength(), wheel.getRadius(), wheel.isFrontWheel());
            newWheel.setFrictionSlip(wheel.getFrictionSlip());
            newWheel.setMaxSuspensionTravelCm(wheel.getMaxSuspensionTravelCm());
            newWheel.setSuspensionStiffness(wheel.getSuspensionStiffness());
            newWheel.setWheelsDampingCompression(wheel.getWheelsDampingCompression());
            newWheel.setWheelsDampingRelaxation(wheel.getWheelsDampingRelaxation());
            newWheel.setMaxSuspensionForce(wheel.getMaxSuspensionForce());

            // Copy the wheel spatial reference directly for now.  They'll
            // get fixed up in the cloneFields() method
            newWheel.setWheelSpatial(wheel.getWheelSpatial());
        }
        control.setApplyPhysicsLocal(isApplyPhysicsLocal());
        control.setEnabled(isEnabled());
        
        control.spatial = spatial;
        return control;
    }     

    @Override   
    public void cloneFields( Cloner cloner, Object original ) {
        this.spatial = cloner.clone(spatial);
         
        for( VehicleWheel wheel : wheels ) {
            Spatial spatial = cloner.clone(wheel.getWheelSpatial());
            wheel.setWheelSpatial(spatial);
        }        
    }
         
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        setUserObject(spatial);
        if (spatial == null) {
            return;
        }
        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if(spatial!=null){
                    setPhysicsLocation(getSpatialTranslation());
                    setPhysicsRotation(getSpatialRotation());
                }
                space.addCollisionObject(this);
                added = true;
            } else if (!enabled && added) {
                space.removeCollisionObject(this);
                added = false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
        if (enabled && spatial != null) {
            if (getMotionState().applyTransform(spatial)) {
                spatial.getWorldTransform();
                applyWheelTransforms();
            }
        } else if (enabled) {
            applyWheelTransforms();
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        createVehicle(space);
        if (space == null) {
            if (this.space != null) {
                this.space.removeCollisionObject(this);
                added = false;
            }
        } else {
            if(this.space == space) return;
            // if this object isn't enabled, it will be added when it will be enabled.
            if (isEnabled()) {
                space.addCollisionObject(this);
                added = true;
            }
        }
        this.space = space;
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(motionState.isApplyPhysicsLocal(), "applyLocalPhysics", false);
        oc.write(spatial, "spatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        motionState.setApplyPhysicsLocal(ic.readBoolean("applyLocalPhysics", false));
        setUserObject(spatial);
    }
}
