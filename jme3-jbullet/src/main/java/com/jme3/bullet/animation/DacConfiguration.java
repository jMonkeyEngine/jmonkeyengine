/*
 * Copyright (c) 2018-2019 jMonkeyEngine
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
package com.jme3.bullet.animation;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.bullet.control.AbstractPhysicsControl;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configure a DynamicAnimControl and access its configuration.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
abstract public class DacConfiguration extends AbstractPhysicsControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(DacConfiguration.class.getName());
    /**
     * name for the ragdoll's torso, must not be used for any bone
     */
    final public static String torsoName = "";
    // *************************************************************************
    // fields

    /**
     * viscous damping ratio for new rigid bodies (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    private float damping = 0.6f;
    /**
     * minimum applied impulse for a collision event to be dispatched to
     * listeners (default=0)
     */
    private float eventDispatchImpulseThreshold = 0f;
    /**
     * mass for the torso
     */
    private float torsoMass = 1f;
    /**
     * Maps linked bone names to masses.
     */
    private Map<String, Float> blConfigMap = new HashMap<>(50);
    /**
     * Maps linked bone names to ranges of motion for createSpatialData().
     */
    private Map<String, RangeOfMotion> jointMap = new HashMap<>(50);
    /**
     * gravitational acceleration vector for ragdolls (default is 9.8 in the -Y
     * direction, approximating Earth-normal in MKS units)
     */
    private Vector3f gravityVector = new Vector3f(0f, -9.8f, 0f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any linked bones (torso only).
     */
    DacConfiguration() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count the linked bones.
     *
     * @return count (&ge;0)
     */
    public int countLinkedBones() {
        int count = blConfigMap.size();

        assert count == jointMap.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the links.
     *
     * @return count (&ge;0)
     */
    public int countLinks() {
        int result = countLinkedBones() + 1;
        return result;
    }

    /**
     * Read the damping ratio for new rigid bodies.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float damping() {
        assert damping >= 0f : damping;
        return damping;
    }

    /**
     * Read the event-dispatch impulse threshold of this control.
     *
     * @return the threshold value (&ge;0)
     */
    public float eventDispatchImpulseThreshold() {
        assert eventDispatchImpulseThreshold >= 0f;
        return eventDispatchImpulseThreshold;
    }

    /**
     * Access the nominal range of motion for the joint connecting the named
     * linked bone to its parent in the hierarchy.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @return the pre-existing instance (not null)
     */
    public RangeOfMotion getJointLimits(String boneName) {
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + boneName;
            throw new IllegalArgumentException(msg);
        }
        RangeOfMotion result = jointMap.get(boneName);

        assert result != null;
        return result;
    }

    /**
     * Copy this control's gravitational acceleration for Ragdoll mode.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an acceleration vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f gravity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(gravityVector);
        return result;
    }

    /**
     * Test whether a BoneLink exists for the named bone.
     *
     * @param boneName the name of the bone (may be null)
     * @return true if found, otherwise false
     */
    public boolean hasBoneLink(String boneName) {
        boolean result;
        if (boneName == null) {
            result = false;
        } else {
            result = blConfigMap.containsKey(boneName);
        }

        return result;
    }

    /**
     * Link the named bone using the specified mass and range of motion.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to link (not null, not empty)
     * @param mass the desired mass of the bone (&gt;0)
     * @param rom the desired range of motion (not null)
     * @see #setJointLimits(java.lang.String,
     * com.jme3.bullet.animation.RangeOfMotion)
     */
    public void link(String boneName, float mass, RangeOfMotion rom) {
        verifyNotAddedToSpatial("link a bone");
        if (hasBoneLink(boneName)) {
            logger2.log(Level.WARNING, "Bone {0} is already linked.", boneName);
        }

        jointMap.put(boneName, rom);
        blConfigMap.put(boneName, mass);
    }

    /**
     * Enumerate all bones with bone links.
     *
     * @return a new array of bone names (not null, may be empty)
     */
    public String[] listLinkedBoneNames() {
        int size = countLinkedBones();
        String[] result = new String[size];
        Collection<String> names = blConfigMap.keySet();
        names.toArray(result);

        return result;
    }

    /**
     * Read the mass of the named bone/torso.
     *
     * @param boneName the name of the bone/torso (not null)
     * @return the mass (in physics units, &gt;0)
     */
    public float mass(String boneName) {
        float mass;
        if (torsoName.equals(boneName)) {
            mass = torsoMass;
        } else {
            mass = blConfigMap.get(boneName);
        }
        return mass;
    }

    /**
     * Alter the viscous damping ratio for new rigid bodies.
     *
     * @param dampingRatio the desired damping ratio (non-negative, 0&rarr;no
     * damping, 1&rarr;critically damped, default=0.6)
     */
    public void setDamping(float dampingRatio) {
        damping = dampingRatio;
    }

    /**
     * Alter the event-dispatch impulse threshold of this control.
     *
     * @param threshold the desired threshold (&ge;0)
     */
    public void setEventDispatchImpulseThreshold(float threshold) {
        eventDispatchImpulseThreshold = threshold;
    }

    /**
     * Alter this control's gravitational acceleration for Ragdoll mode.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected, default=0,-9.8,0)
     */
    public void setGravity(Vector3f gravity) {
        gravityVector.set(gravity);
    }

    /**
     * Alter the range of motion of the joint connecting the named BoneLink to
     * its parent in the link hierarchy.
     *
     * @param boneName the name of the BoneLink (not null, not empty)
     * @param rom the desired range of motion (not null)
     */
    public void setJointLimits(String boneName, RangeOfMotion rom) {
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + boneName;
            throw new IllegalArgumentException(msg);
        }

        jointMap.put(boneName, rom);
    }

    /**
     * Alter the mass of the named bone/torso.
     *
     * @param boneName the name of the bone, or torsoName (not null)
     * @param mass the desired mass (&gt;0)
     */
    public void setMass(String boneName, float mass) {
        if (torsoName.equals(boneName)) {
            torsoMass = mass;
        } else if (hasBoneLink(boneName)) {
            blConfigMap.put(boneName, mass);
        } else {
            String msg = "No bone/torso named " + boneName;
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Calculate the ragdoll's total mass.
     *
     * @return the total mass (&gt;0)
     */
    public float totalMass() {
        float totalMass = torsoMass;
        for (float mass : blConfigMap.values()) {
            totalMass += mass;
        }

        return totalMass;
    }

    /**
     * Unlink the BoneLink of the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to unlink (not null, not empty)
     */
    public void unlinkBone(String boneName) {
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + boneName;
            throw new IllegalArgumentException(msg);
        }
        verifyNotAddedToSpatial("unlink a bone");

        jointMap.remove(boneName);
        blConfigMap.remove(boneName);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Add unlinked descendants of the specified bone to the specified
     * collection. Note: recursive.
     *
     * @param startBone the starting bone (not null, unaffected)
     * @param addResult the collection of bone names to append to (not null,
     * modified)
     */
    protected void addUnlinkedDescendants(Joint startBone,
            Collection<Joint> addResult) {
        for (Joint childBone : startBone.getChildren()) {
            String childName = childBone.getName();
            if (!hasBoneLink(childName)) {
                addResult.add(childBone);
                addUnlinkedDescendants(childBone, addResult);
            }
        }
    }

    /**
     * Find the manager of the specified bone.
     *
     * @param startBone the bone (not null, unaffected)
     * @return a bone/torso name (not null)
     */
    protected String findManager(Joint startBone) {
        String managerName;
        Joint bone = startBone;
        while (true) {
            String boneName = bone.getName();
            if (hasBoneLink(boneName)) {
                managerName = boneName;
                break;
            }
            bone = bone.getParent();
            if (bone == null) {
                managerName = torsoName;
                break;
            }
        }

        assert managerName != null;
        return managerName;
    }

    /**
     * Create a map from bone indices to the names of the bones that manage
     * them.
     *
     * @param skeleton (not null, unaffected)
     * @return a new array of bone/torso names (not null)
     */
    protected String[] managerMap(Armature skeleton) {
        int numBones = skeleton.getJointCount();
        String[] managerMap = new String[numBones];
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Joint bone = skeleton.getJoint(boneIndex);
            managerMap[boneIndex] = findManager(bone);
        }

        return managerMap;
    }
    // *************************************************************************
    // AbstractPhysicsControl methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null, modified)
     * @param original the control from which this control was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        blConfigMap = cloner.clone(blConfigMap);
        jointMap = cloner.clone(jointMap);
        gravityVector = cloner.clone(gravityVector);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public DacConfiguration jmeClone() {
        try {
            DacConfiguration clone
                    = (DacConfiguration) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
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

        damping = ic.readFloat("damping", 0.6f);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);

        jointMap.clear();
        blConfigMap.clear();
        String[] linkedBoneNames = ic.readStringArray("linkedBoneNames", null);
        Savable[] linkedBoneJoints
                = ic.readSavableArray("linkedBoneJoints", null);
        float[] blConfigs = ic.readFloatArray("blConfigs", null);
        for (int i = 0; i < linkedBoneNames.length; ++i) {
            String boneName = linkedBoneNames[i];
            RangeOfMotion rom = (RangeOfMotion) linkedBoneJoints[i];
            jointMap.put(boneName, rom);
            blConfigMap.put(boneName, blConfigs[i]);
        }

        torsoMass = ic.readFloat("torsoMass", 1f);
        gravityVector = (Vector3f) ic.readSavable("gravity", null);
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
     * Alter whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @param applyPhysicsLocal true&rarr;match local coordinates,
     * false&rarr;match world coordinates (default=false)
     */
    @Override
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        if (applyPhysicsLocal) {
            throw new UnsupportedOperationException(
                    "DynamicAnimControl does not support local physics.");
        }
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

        oc.write(damping, "damping", 0.6f);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);

        int count = countLinkedBones();
        String[] linkedBoneNames = new String[count];
        RangeOfMotion[] roms = new RangeOfMotion[count];
        float[] blConfigs = new float[count];
        int i = 0;
        for (Map.Entry<String, Float> entry : blConfigMap.entrySet()) {
            linkedBoneNames[i] = entry.getKey();
            roms[i] = jointMap.get(entry.getKey());
            blConfigs[i] = entry.getValue();
            ++i;
        }
        oc.write(linkedBoneNames, "linkedBoneNames", null);
        oc.write(roms, "linkedBoneJoints", null);
        oc.write(blConfigs, "blConfigs", null);

        oc.write(torsoMass, "torsoMass", 1f);
        oc.write(gravityVector, "gravity", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Verify that this control is NOT added to a Spatial.
     *
     * @param desiredAction (not null, not empty)
     */
    private void verifyNotAddedToSpatial(String desiredAction) {
        assert desiredAction != null;

        Spatial controlledSpatial = getSpatial();
        if (controlledSpatial != null) {
            String message = "Cannot " + desiredAction
                    + " while the Control is added to a Spatial.";
            throw new IllegalStateException(message);
        }
    }
}
