/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.Iterator;

/**
 * A physics control to link a PhysicsVehicle to a spatial.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class VehicleControl extends PhysicsVehicle implements PhysicsControl, JmeCloneable {

    /**
     * spatial to which this control is added, or null if none
     */
    protected Spatial spatial;
    /**
     * true&rarr;control is enabled, false&rarr;control is disabled
     */
    protected boolean enabled = true;
    /**
     * space to which the vehicle is (or would be) added
     */
    protected PhysicsSpace space = null;
    /**
     * true&rarr;vehicle is added to the physics space, false&rarr;not added
     */
    protected boolean added = false;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected VehicleControl() {
    }

    /**
     * Instantiate an enabled control with mass=1 and the specified collision
     * shape.
     *
     * @param shape the desired shape (not null, alias created)
     */
    public VehicleControl(CollisionShape shape) {
        super(shape);
    }

    /**
     * Instantiate an enabled with the specified collision shape and mass.
     *
     * @param shape the desired shape (not null, alias created)
     * @param mass (&gt;0)
     */
    public VehicleControl(CollisionShape shape, float mass) {
        super(shape, mass);
    }

    /**
     * Test whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @return true if matching local coordinates, false if matching world
     * coordinates
     */
    public boolean isApplyPhysicsLocal() {
        return motionState.isApplyPhysicsLocal();
    }

    /**
     * Alter whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @param applyPhysicsLocal true&rarr;match local coordinates,
     * false&rarr;match world coordinates (default=false)
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

    /**
     * Clone this control for a different spatial. No longer used as of JME 3.1.
     *
     * @param spatial the spatial for the clone to control (or null)
     * @return a new control (not null)
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new control (not null)
     */
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
        control.setContactResponse(isContactResponse());
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

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override   
    public void cloneFields( Cloner cloner, Object original ) {
        this.spatial = cloner.clone(spatial);
         
        for( VehicleWheel wheel : wheels ) {
            Spatial spatial = cloner.clone(wheel.getWheelSpatial());
            wheel.setWheelSpatial(spatial);
        }        
    }
         
    /**
     * Alter which spatial is controlled.
     *
     * @param spatial spatial to control (or null)
     */
    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        setUserObject(spatial);
        if (spatial == null) {
            return;
        }
        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
    }

    /**
     * Enable or disable this control.
     * <p>
     * When the control is disabled, the vehicle is removed from physics space.
     * When the control is enabled again, the physics object is moved to the
     * spatial's location and then added to the physics space.
     *
     * @param enabled true&rarr;enable the control, false&rarr;disable it
     */
    @Override
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

    /**
     * Test whether this control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Update this control. Invoked once per frame, during the logical-state
     * update, provided the control is added to a scene.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
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

    /**
     * Render this control. Invoked once per view port per frame, provided the
     * control is added to a scene. Should be invoked only by a subclass or by
     * the RenderManager.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * If enabled, add this control's physics object to the specified physics
     * space. In not enabled, alter where the object would be added. The object
     * is removed from any other space it's currently in.
     *
     * @param newSpace where to add, or null to simply remove
     */
    @Override
    public void setPhysicsSpace(PhysicsSpace newSpace) {
        if (space == newSpace) {
            return;
        }
        if (added) {
            space.removeCollisionObject(this);
            added = false;
        }
        if (newSpace != null && isEnabled()) {
            newSpace.addCollisionObject(this);
            added = true;
        }
        /*
         * If this control isn't enabled, its physics object will be
         * added to the new space when the control becomes enabled.
         */
        space = newSpace;
    }

    /**
     * Access the physics space to which the vehicle is (or would be) added.
     *
     * @return the pre-existing space, or null for none
     */
    @Override
    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(motionState.isApplyPhysicsLocal(), "applyLocalPhysics", false);
        oc.write(spatial, "spatial", null);
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
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