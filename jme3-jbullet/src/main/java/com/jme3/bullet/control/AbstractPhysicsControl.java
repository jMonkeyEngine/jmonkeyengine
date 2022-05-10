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
 * Manage the life cycle of a physics object linked to a spatial in a scene
 * graph.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public abstract class AbstractPhysicsControl implements PhysicsControl, JmeCloneable {

    /**
     * temporary storage during calculations
     */
    private final Quaternion tmp_inverseWorldRotation = new Quaternion();
    /**
     * spatial to which this control is added, or null if none
     */
    protected Spatial spatial;
    /**
     * true&rarr;control is enabled, false&rarr;control is disabled
     */
    protected boolean enabled = true;
    /**
     * true&rarr;body is added to the physics space, false&rarr;not added
     */
    protected boolean added = false;
    /**
     * space to which the physics object is (or would be) added
     */
    protected PhysicsSpace space = null;
    /**
     * true &rarr; physics coordinates match local transform, false &rarr;
     * physics coordinates match world transform
     */
    protected boolean applyLocal = false;

    /**
     * Create spatial-dependent data. Invoked when this control is added to a
     * spatial.
     *
     * @param spat the controlled spatial (not null)
     */
    protected abstract void createSpatialData(Spatial spat);

    /**
     * Destroy spatial-dependent data. Invoked when this control is removed from
     * a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    protected abstract void removeSpatialData(Spatial spat);

    /**
     * Translate the physics object to the specified location.
     *
     * @param vec desired location (not null, unaffected)
     */
    protected abstract void setPhysicsLocation(Vector3f vec);

    /**
     * Rotate the physics object to the specified orientation.
     *
     * @param quat desired orientation (not null, unaffected)
     */
    protected abstract void setPhysicsRotation(Quaternion quat);

    /**
     * Add all managed physics objects to the specified space.
     *
     * @param space which physics space to add to (not null)
     */
    protected abstract void addPhysics(PhysicsSpace space);

    /**
     * Remove all managed physics objects from the specified space.
     *
     * @param space which physics space to remove from (not null)
     */
    protected abstract void removePhysics(PhysicsSpace space);

    /**
     * Test whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @return true if matching local coordinates, false if matching world
     * coordinates
     */
    public boolean isApplyPhysicsLocal() {
        return applyLocal;
    }

    /**
     * Alter whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @param applyPhysicsLocal true&rarr;match local coordinates,
     * false&rarr;match world coordinates (default=false)
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        applyLocal = applyPhysicsLocal;
    }

    /**
     * Access whichever spatial translation corresponds to the physics location.
     *
     * @return the pre-existing location vector (in physics-space coordinates,
     * not null)
     */
    protected Vector3f getSpatialTranslation() {
        if (applyLocal) {
            return spatial.getLocalTranslation();
        }
        return spatial.getWorldTranslation();
    }

    /**
     * Access whichever spatial rotation corresponds to the physics rotation.
     *
     * @return the pre-existing quaternion (in physics-space coordinates, not
     * null)
     */
    protected Quaternion getSpatialRotation() {
        if (applyLocal) {
            return spatial.getLocalRotation();
        }
        return spatial.getWorldRotation();
    }

    /**
     * Apply a physics transform to the spatial.
     *
     * @param worldLocation location vector (in physics-space coordinates, not
     * null, unaffected)
     * @param worldRotation orientation (in physics-space coordinates, not null,
     * unaffected)
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

    @Deprecated
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
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
        createSpatialData(this.spatial);
    }
         
    /**
     * Alter which spatial is controlled. Invoked when the control is added to
     * or removed from a spatial. Should be invoked only by a subclass or from
     * Spatial. Do not invoke directly from user code.
     *
     * @param spatial the spatial to control (or null)
     */
    @Override
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

    /**
     * @return returns the spatial the control is added to, or null if the control is not attached to a spatial yet.
     */
    public Spatial getSpatial(){
        return this.spatial;
    }

    /**
     * Enable or disable this control.
     * <p>
     * When the control is disabled, the physics object is removed from physics
     * space. When the control is enabled again, the physics object is moved to
     * the spatial's location and then added to the physics space.
     *
     * @param enabled true&rarr;enable the control, false&rarr;disable it
     */
    @Override
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

    /**
     * Test whether this control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * If enabled, add this control's physics object to the specified physics
     * space. If not enabled, alter where the object would be added. The object
     * is removed from any other space it's in.
     *
     * @param newSpace where to add, or null to simply remove
     */
    @Override
    public void setPhysicsSpace(PhysicsSpace newSpace) {
        if (space == newSpace) {
            return;
        }
        if (added) {
            removePhysics(space);
            added = false;
        }
        if (newSpace != null && isEnabled()) {
            addPhysics(newSpace);
            added = true;
        }
        /*
         * If this control isn't enabled, its physics object will be
         * added to the new space when the control becomes enabled.
         */
        space = newSpace;
    }

    /**
     * Access the physics space to which the object is (or would be) added.
     *
     * @return the pre-existing space, or null for none
     */
    @Override
    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    /**
     * Serialize this object, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(applyLocal, "applyLocalPhysics", false);
        oc.write(spatial, "spatial", null);
    }

    /**
     * De-serialize this control from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        applyLocal = ic.readBoolean("applyLocalPhysics", false);
    }
}
