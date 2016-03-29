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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * AbstractPhysicsControl manages the lifecycle of a physics object that is
 * attached to a spatial in the SceneGraph.
 *
 * @author normenhansen
 */
public abstract class AbstractPhysicsControl implements PhysicsControl, JmeCloneable {

    private final Quaternion tmp_inverseWorldRotation = new Quaternion();
    protected Spatial spatial;
    protected boolean enabled = true;
    protected boolean added = false;
    protected PhysicsSpace space = null;
    protected boolean applyLocal = false;

    /**
     * Called when the control is added to a new spatial, create any
     * spatial-dependent data here.
     *
     * @param spat The new spatial, guaranteed not to be null
     */
    protected abstract void createSpatialData(Spatial spat);

    /**
     * Called when the control is removed from a spatial, remove any
     * spatial-dependent data here.
     *
     * @param spat The old spatial, guaranteed not to be null
     */
    protected abstract void removeSpatialData(Spatial spat);

    /**
     * Called when the physics object is supposed to move to the spatial
     * position.
     *
     * @param vec
     */
    protected abstract void setPhysicsLocation(Vector3f vec);

    /**
     * Called when the physics object is supposed to move to the spatial
     * rotation.
     *
     * @param quat
     */
    protected abstract void setPhysicsRotation(Quaternion quat);

    /**
     * Called when the physics object is supposed to add all objects it needs to
     * manage to the physics space.
     *
     * @param space
     */
    protected abstract void addPhysics(PhysicsSpace space);

    /**
     * Called when the physics object is supposed to remove all objects added to
     * the physics space.
     *
     * @param space
     */
    protected abstract void removePhysics(PhysicsSpace space);

    public boolean isApplyPhysicsLocal() {
        return applyLocal;
    }

    /**
     * When set to true, the physics coordinates will be applied to the local
     * translation of the Spatial
     *
     * @param applyPhysicsLocal
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        applyLocal = applyPhysicsLocal;
    }

    protected Vector3f getSpatialTranslation() {
        if (applyLocal) {
            return spatial.getLocalTranslation();
        }
        return spatial.getWorldTranslation();
    }

    protected Quaternion getSpatialRotation() {
        if (applyLocal) {
            return spatial.getLocalRotation();
        }
        return spatial.getWorldRotation();
    }

    /**
     * Applies a physics transform to the spatial
     *
     * @param worldLocation
     * @param worldRotation
     */
    protected void applyPhysicsTransform(Vector3f worldLocation, Quaternion worldRotation) {
        if (enabled && spatial != null) {
            Vector3f localLocation = spatial.getLocalTranslation();
            Quaternion localRotationQuat = spatial.getLocalRotation();
            if (!applyLocal && spatial.getParent() != null) {
                localLocation.set(worldLocation).subtractLocal(spatial.getParent().getWorldTranslation());
                localLocation.divideLocal(spatial.getParent().getWorldScale());
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);
                localRotationQuat.set(worldRotation);
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);

                spatial.setLocalTranslation(localLocation);
                spatial.setLocalRotation(localRotationQuat);
            } else {
                spatial.setLocalTranslation(worldLocation);
                spatial.setLocalRotation(worldRotation);
            }
        }

    }
    
    @Override   
    public void cloneFields( Cloner cloner, Object original ) { 
        this.spatial = cloner.clone(spatial);
        createSpatialData(this.spatial);
    }
         
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && this.spatial != spatial) {
            removeSpatialData(this.spatial);
        } else if (this.spatial == spatial) {
            return;
        }
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        createSpatialData(this.spatial);
        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if (spatial != null) {
                    setPhysicsLocation(getSpatialTranslation());
                    setPhysicsRotation(getSpatialRotation());
                }
                addPhysics(space);
                added = true;
            } else if (!enabled && added) {
                removePhysics(space);
                added = false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        if (space == null) {
            if (this.space != null) {
                removePhysics(this.space);
                added = false;
            }
        } else {
            if (this.space == space) {
                return;
            } else if (this.space != null) {
                removePhysics(this.space);
            }
            addPhysics(space);
            added = true;
        }
        this.space = space;
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(applyLocal, "applyLocalPhysics", false);
        oc.write(spatial, "spatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        applyLocal = ic.readBoolean("applyLocalPhysics", false);
    }
}
