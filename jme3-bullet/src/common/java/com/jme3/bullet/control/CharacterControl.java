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
import com.jme3.bullet.objects.PhysicsCharacter;
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

/**
 * You might want to try <code>BetterCharacterControl</code> as well.
 * @author normenhansen
 */
public class CharacterControl extends PhysicsCharacter implements PhysicsControl, JmeCloneable {

    protected Spatial spatial;
    protected boolean enabled = true;
    protected boolean added = false;
    protected PhysicsSpace space = null;
    protected Vector3f viewDirection = new Vector3f(Vector3f.UNIT_Z);
    protected boolean useViewDirection = true;
    protected boolean applyLocal = false;

    public CharacterControl() {
    }

    public CharacterControl(CollisionShape shape, float stepHeight) {
        super(shape, stepHeight);
    }

    public boolean isApplyPhysicsLocal() {
        return applyLocal;
    }

    /**
     * When set to true, the physics coordinates will be applied to the local
     * translation of the Spatial
     * @param applyPhysicsLocal
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        applyLocal = applyPhysicsLocal;
    }

    private Vector3f getSpatialTranslation() {
        if (applyLocal) {
            return spatial.getLocalTranslation();
        }
        return spatial.getWorldTranslation();
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        CharacterControl control = new CharacterControl(collisionShape, stepHeight);
        control.setCcdMotionThreshold(getCcdMotionThreshold());
        control.setCcdSweptSphereRadius(getCcdSweptSphereRadius());
        control.setCollideWithGroups(getCollideWithGroups());
        control.setCollisionGroup(getCollisionGroup());
        control.setFallSpeed(getFallSpeed());
        control.setGravity(getGravity());
        control.setJumpSpeed(getJumpSpeed());
        control.setMaxSlope(getMaxSlope());
        control.setPhysicsLocation(getPhysicsLocation());
        control.setUpAxis(getUpAxis());
        control.setApplyPhysicsLocal(isApplyPhysicsLocal());
        return control;
    }

    @Override
    public Object jmeClone() {
        CharacterControl control = new CharacterControl(collisionShape, stepHeight);
        control.setCcdMotionThreshold(getCcdMotionThreshold());
        control.setCcdSweptSphereRadius(getCcdSweptSphereRadius());
        control.setCollideWithGroups(getCollideWithGroups());
        control.setCollisionGroup(getCollisionGroup());
        control.setFallSpeed(getFallSpeed());
        control.setGravity(getGravity());
        control.setJumpSpeed(getJumpSpeed());
        control.setMaxSlope(getMaxSlope());
        control.setPhysicsLocation(getPhysicsLocation());
        control.setUpAxis(getUpAxis());
        control.setApplyPhysicsLocal(isApplyPhysicsLocal());
        control.spatial = this.spatial;
        control.setEnabled(isEnabled());
        return control;
    }     

    @Override
    public void cloneFields( Cloner cloner, Object original ) { 
        this.spatial = cloner.clone(spatial);
    }
         
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        setUserObject(spatial);
        if (spatial == null) {
            return;
        }
        setPhysicsLocation(getSpatialTranslation());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if (spatial != null) {
                    warp(getSpatialTranslation());
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

    public void setViewDirection(Vector3f vec) {
        viewDirection.set(vec);
    }

    public Vector3f getViewDirection() {
        return viewDirection;
    }

    public boolean isUseViewDirection() {
        return useViewDirection;
    }

    public void setUseViewDirection(boolean viewDirectionEnabled) {
        this.useViewDirection = viewDirectionEnabled;
    }

    public void update(float tpf) {
        if (enabled && spatial != null) {
            Quaternion localRotationQuat = spatial.getLocalRotation();
            Vector3f localLocation = spatial.getLocalTranslation();
            if (!applyLocal && spatial.getParent() != null) {
                getPhysicsLocation(localLocation);
                localLocation.subtractLocal(spatial.getParent().getWorldTranslation());
                localLocation.divideLocal(spatial.getParent().getWorldScale());
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);
                spatial.setLocalTranslation(localLocation);

                if (useViewDirection) {
                    localRotationQuat.lookAt(viewDirection, Vector3f.UNIT_Y);
                    spatial.setLocalRotation(localRotationQuat);
                }
            } else {
                spatial.setLocalTranslation(getPhysicsLocation());
                localRotationQuat.lookAt(viewDirection, Vector3f.UNIT_Y);
                spatial.setLocalRotation(localRotationQuat);
            }
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setPhysicsSpace(PhysicsSpace space) {
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
        oc.write(applyLocal, "applyLocalPhysics", false);
        oc.write(useViewDirection, "viewDirectionEnabled", true);
        oc.write(viewDirection, "viewDirection", new Vector3f(Vector3f.UNIT_Z));
        oc.write(spatial, "spatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        useViewDirection = ic.readBoolean("viewDirectionEnabled", true);
        viewDirection = (Vector3f) ic.readSavable("viewDirection", new Vector3f(Vector3f.UNIT_Z));
        applyLocal = ic.readBoolean("applyLocalPhysics", false);
        spatial = (Spatial) ic.readSavable("spatial", null);
        setUserObject(spatial);
    }
}
