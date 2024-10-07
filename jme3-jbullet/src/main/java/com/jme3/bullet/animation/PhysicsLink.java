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

import com.jme3.anim.Joint;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract base class used by DynamicAnimControl to link pieces of a JME
 * model to their corresponding collision objects in a ragdoll. Subclasses
 * include BoneLink and TorsoLink. The links in each DynamicAnimControl form a
 * hierarchy with the TorsoLink at its root.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
abstract public class PhysicsLink
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * immediate children in the link hierarchy (not null)
     */
    private ArrayList<PhysicsLink> children = new ArrayList<>(8);
    /**
     * corresponding bone in the skeleton (not null)
     */
    private Joint bone;
    /**
     * scene-graph control that manages this link (not null)
     */
    private DacLinks control;
    /**
     * duration of the most recent blend interval (in seconds, &ge;0)
     */
    private float blendInterval = 1f;
    /**
     * weighting of kinematic movement (&ge;0, &le;1, 0=purely dynamic, 1=purely
     * kinematic, default=1, progresses from 0 to 1 during the blend interval)
     */
    private float kinematicWeight = 1f;
    /**
     * joint between the rigid body and the parent's rigid body, or null if not
     * yet created
     */
    private PhysicsJoint joint = null;
    /**
     * parent/manager in the link hierarchy, or null if none
     */
    private PhysicsLink parent = null;
    /**
     * linked rigid body in the ragdoll (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * transform of the rigid body as of the most recent update (in
     * physics-space coordinates, updated in kinematic mode only)
     */
    private Transform kpTransform = new Transform();
    /**
     * estimate of the body's linear velocity as of the most recent update
     * (psu/sec in physics-space coordinates, kinematic mode only)
     */
    private Vector3f kpVelocity = new Vector3f();
    /**
     * location of the rigid body's center (in the skeleton bone's local
     * coordinates)
     */
    private Vector3f localOffset;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected PhysicsLink() {
    }

    /**
     * Instantiate a purely kinematic link between the specified skeleton bone
     * and the specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param bone the corresponding bone (not null, alias created)
     * @param collisionShape the desired shape (not null, alias created)
     * @param mass the mass (in physics-mass units)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     */
    PhysicsLink(DacLinks control, Joint bone, CollisionShape collisionShape,
            float mass, Vector3f localOffset) {
        assert control != null;
        assert bone != null;
        assert collisionShape != null;
        assert localOffset != null;

        this.control = control;
        this.bone = bone;
        rigidBody = createRigidBody(mass, collisionShape);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Creating link for bone {0} with mass={1}",
                    new Object[]{bone.getName(), rigidBody.getMass()});
        }

        this.localOffset = localOffset.clone();
        updateKPTransform();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the name of the corresponding bone.
     *
     * @return the bone name (not null)
     */
    public String boneName() {
        String boneName = bone.getName();

        assert boneName != null;
        return boneName;
    }

    /**
     * Count this link's immediate children in the link hierarchy.
     *
     * @return the count (&ge;0)
     */
    public int countChildren() {
        int numChildren = children.size();
        return numChildren;
    }

    /**
     * Access the corresponding bone.
     *
     * @return the pre-existing instance (not null)
     */
    final public Joint getBone() {
        assert bone != null;
        return bone;
    }

    /**
     * Access the control that manages this link.
     *
     * @return the pre-existing instance (not null)
     */
    public DacLinks getControl() {
        assert control != null;
        return control;
    }

    /**
     * Access the joint between this link's rigid body and that of its parent.
     *
     * @return the pre-existing instance, or null for the torso
     */
    public PhysicsJoint getJoint() {
        return joint;
    }

    /**
     * Access this link's parent/manager in the link hierarchy.
     *
     * @return the link, or null if none
     */
    public PhysicsLink getParent() {
        return parent;
    }

    /**
     * Access the linked rigid body.
     *
     * @return the pre-existing instance (not null)
     */
    public PhysicsRigidBody getRigidBody() {
        assert rigidBody != null;
        return rigidBody;
    }

    /**
     * Test whether the link is in kinematic mode.
     *
     * @return true if kinematic, or false if purely dynamic
     */
    public boolean isKinematic() {
        if (kinematicWeight > 0f) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read the kinematic weight of this link.
     *
     * @return 0 if purely dynamic, 1 if purely kinematic
     */
    public float kinematicWeight() {
        assert kinematicWeight >= 0f : kinematicWeight;
        assert kinematicWeight <= 1f : kinematicWeight;
        return kinematicWeight;
    }

    /**
     * Enumerate this link's immediate children in the link hierarchy.
     *
     * @return a new array (not null)
     */
    public PhysicsLink[] listChildren() {
        int numChildren = children.size();
        PhysicsLink[] result = new PhysicsLink[numChildren];
        children.toArray(result);

        return result;
    }

    /**
     * Unambiguously identify this link by name, within its DynamicAnimControl.
     *
     * @return a text string (not null, not empty)
     */
    abstract public String name();

    /**
     * Calculate a physics transform for the rigid body (to match the skeleton
     * bone).
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated transform (in physics-space coordinates, either
     * storeResult or a new transform, not null)
     */
    public Transform physicsTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        if (isKinematic()) {
            result.set(kpTransform);
        } else {
            rigidBody.getPhysicsLocation(result.getTranslation());
            rigidBody.getPhysicsRotation(result.getRotation());
            result.setScale(rigidBody.getCollisionShape().getScale());
        }

        return result;
    }

    /**
     * Copy animation data from the specified link, which must correspond to the
     * same bone.
     *
     * @param oldLink the link to copy from (not null, unaffected)
     */
    void postRebuild(PhysicsLink oldLink) {
        assert oldLink != null;
        assert oldLink.bone == bone;

        if (oldLink.isKinematic()) {
            blendInterval = oldLink.blendInterval;
            kinematicWeight = oldLink.kinematicWeight;
        } else {
            blendInterval = 0f;
            kinematicWeight = 1f;
        }
    }

    /**
     * Internal callback, invoked just AFTER the physics is stepped.
     */
    void postTick() {
        if (!isKinematic()) {
            rigidBody.activate();
        }
    }

    /**
     * Internal callback, invoked just BEFORE the physics is stepped.
     *
     * @param timeStep the physics time step (in seconds, &ge;0)
     */
    void preTick(float timeStep) {
        if (isKinematic()) {
            rigidBody.setPhysicsLocation(kpTransform.getTranslation());
            rigidBody.setPhysicsRotation(kpTransform.getRotation());
            rigidBody.getCollisionShape().setScale(kpTransform.getScale());
        }
    }

    /**
     * Immediately put this link into dynamic mode. The control must be "ready".
     *
     * @param uniformAcceleration the uniform acceleration vector to apply (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamic(Vector3f uniformAcceleration) {
        control.verifyReadyForDynamicMode("put link into dynamic mode");

        setKinematicWeight(0f);
        rigidBody.setGravity(uniformAcceleration);
    }

    /**
     * Internal callback, invoked once per frame during the logical-state
     * update, provided the control is added to a scene.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    void update(float tpf) {
        assert tpf >= 0f : tpf;

        if (kinematicWeight > 0f) {
            kinematicUpdate(tpf);
        } else {
            dynamicUpdate();
        }
    }

    /**
     * Copy the body's linear velocity, or an estimate thereof.
     *
     * @param storeResult (modified if not null)
     * @return a new velocity vector (psu/sec in physics-space coordinates)
     */
    Vector3f velocity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        if (isKinematic()) {
            result.set(kpVelocity);
        } else {
            assert !rigidBody.isKinematic();
            rigidBody.getLinearVelocity(result);
        }

        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Begin blending this link to a purely kinematic mode.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    protected void blendToKinematicMode(float blendInterval) {
        assert blendInterval >= 0f : blendInterval;

        this.blendInterval = blendInterval;
        setKinematicWeight(Float.MIN_VALUE); // non-zero to trigger blending
    }

    /**
     * Update this link in Dynamic mode, setting the linked bone's transform
     * based on the transform of the rigid body.
     */
    abstract protected void dynamicUpdate();

    /**
     * Update this link in blended Kinematic mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void kinematicUpdate(float tpf) {
        assert tpf >= 0f : tpf;
        assert rigidBody.isKinematic();
        /*
         * If blending, increase the kinematic weight.
         */
        if (blendInterval == 0f) {
            setKinematicWeight(1f); // done blending
        } else {
            setKinematicWeight(kinematicWeight + tpf / blendInterval);
        }
        /*
         * If we didn't need kpVelocity, we could defer this
         * calculation until the preTick().
         */
        Vector3f previousLocation = kpTransform.getTranslation(null);
        updateKPTransform();
        if (tpf > 0f) {
            kpTransform.getTranslation().subtract(previousLocation, kpVelocity);
            kpVelocity.divideLocal(tpf);
        }
    }

    /**
     * Copy the local offset of this link.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the offset (in bone local coordinates, either storeResult or a
     * new vector, not null)
     */
    protected Vector3f localOffset(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(localOffset);
        return result;
    }

    /**
     * Assign a physics joint to this link, or cancel the assigned joint.
     *
     * @param joint (may be null, alias created)
     */
    final protected void setJoint(PhysicsJoint joint) {
        this.joint = joint;
    }

    /**
     * Assign a parent/manager for this link.
     *
     * @param parent (not null, alias created)
     */
    final protected void setParent(PhysicsLink parent) {
        assert parent != null;
        assert this.parent == null;
        this.parent = parent;
        parent.children.add(this);
    }

    /**
     * Alter the rigid body for this link.
     *
     * @param body the desired rigid body (not null, alias created)
     */
    protected void setRigidBody(PhysicsRigidBody body) {
        assert body != null;
        assert rigidBody != null;
        rigidBody = body;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned link into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this link (not null)
     * @param original the instance from which this link was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        bone = cloner.clone(bone);
        control = cloner.clone(control);
        children = cloner.clone(children);
        joint = cloner.clone(joint);
        parent = cloner.clone(parent);
        rigidBody = cloner.clone(rigidBody);
        kpTransform = cloner.clone(kpTransform);
        kpVelocity = cloner.clone(kpVelocity);
        localOffset = cloner.clone(localOffset);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public PhysicsLink jmeClone() {
        try {
            PhysicsLink clone = (PhysicsLink) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this link, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        children = ic.readSavableArrayList("children", new ArrayList(1));
        bone = (Joint) ic.readSavable("bone", null);
        control = (DacLinks) ic.readSavable("control", null);
        blendInterval = ic.readFloat("blendInterval", 1f);
        kinematicWeight = ic.readFloat("kinematicWeight", 1f);
        joint = (PhysicsJoint) ic.readSavable("joint", null);
        parent = (PhysicsLink) ic.readSavable("parent", null);
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        kpTransform
                = (Transform) ic.readSavable("kpTransform", new Transform());
        kpVelocity = (Vector3f) ic.readSavable("kpVelocity", new Vector3f());
        localOffset = (Vector3f) ic.readSavable("offset", new Vector3f());
    }

    /**
     * Serialize this link, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);

        oc.writeSavableArrayList(children, "children", null);
        oc.write(bone, "bone", null);
        oc.write(control, "control", null);
        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
        oc.write(joint, "joint", null);
        oc.write(parent, "parent", null);
        oc.write(rigidBody, "rigidBody", null);
        oc.write(kpTransform, "kpTransform", null);
        oc.write(kpVelocity, "kpVelocity", null);
        oc.write(localOffset, "offset", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Create and configure a rigid body for this link.
     *
     * @param mass the desired mass (&gt;0)
     * @param collisionShape the desired shape (not null, alias created)
     * @return a new instance, not in any PhysicsSpace
     */
    private PhysicsRigidBody createRigidBody(float mass,
            CollisionShape collisionShape) {
        assert collisionShape != null;

        PhysicsRigidBody body = new PhysicsRigidBody(collisionShape, mass);

        float viscousDamping = control.damping();
        body.setDamping(viscousDamping, viscousDamping);

        body.setKinematic(true);
        body.setUserObject(this);

        return body;
    }

    /**
     * Alter the kinematic weight and copy the physics transform and velocity
     * info as needed.
     *
     * @param weight (&ge;0)
     */
    private void setKinematicWeight(float weight) {
        assert weight >= 0f : weight;

        boolean wasKinematic = (kinematicWeight > 0f);
        kinematicWeight = (weight > 1f) ? 1f : weight;
        boolean isKinematic = (kinematicWeight > 0f);

        if (wasKinematic && !isKinematic) {
            rigidBody.setKinematic(false);
            rigidBody.setPhysicsLocation(kpTransform.getTranslation());
            rigidBody.setPhysicsRotation(kpTransform.getRotation());
            rigidBody.getCollisionShape().setScale(kpTransform.getScale());
            rigidBody.setLinearVelocity(kpVelocity);
        } else if (isKinematic && !wasKinematic) {
            rigidBody.getPhysicsLocation(kpTransform.getTranslation());
            rigidBody.getPhysicsRotation(kpTransform.getRotation());
            Vector3f scale = rigidBody.getCollisionShape().getScale();
            kpTransform.setScale(scale);
            rigidBody.getLinearVelocity(kpVelocity);
            rigidBody.setKinematic(true);
        }
    }

    /**
     * Update the kinematic physics transform.
     */
    private void updateKPTransform() {
        control.physicsTransform(bone, localOffset, kpTransform);
    }
}
