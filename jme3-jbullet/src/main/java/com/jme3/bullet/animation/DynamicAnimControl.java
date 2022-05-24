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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Before adding this control to a spatial, configure it by invoking
 * {@link #link(java.lang.String, float, com.jme3.bullet.animation.RangeOfMotion)}
 * for each bone that should have its own rigid body. Leave unlinked bones near
 * the root of the skeleton to form the torso of the ragdoll.
 * <p>
 * When you add the control to a spatial, it generates a ragdoll consisting of a
 * rigid body for the torso and another for each linked bone. It also creates a
 * SixDofJoint connecting each rigid body to its parent in the link hierarchy.
 * The mass of each rigid body and the range-of-motion of each joint can be
 * reconfigured on the fly.
 * <p>
 * Each link is either dynamic (driven by forces and torques) or kinematic
 * (unperturbed by forces and torques). Transitions from dynamic to kinematic
 * can be immediate or gradual.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class DynamicAnimControl
        extends DacLinks
        implements PhysicsCollisionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger35
            = Logger.getLogger(DynamicAnimControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * calculated total mass
     */
    private float ragdollMass = 0f;
    /**
     * list of registered collision listeners
     */
    private List<RagdollCollisionListener> collisionListeners
            = new SafeArrayList<>(RagdollCollisionListener.class);
    /*
     * center-of-mass actual location (in physics-space coordinates)
     */
    private Vector3f centerLocation = new Vector3f();
    /*
     * center-of-mass estimated velocity (psu/second in physics-space coordinates)
     */
    private Vector3f centerVelocity = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any linked bones (torso only).
     */
    public DynamicAnimControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a collision listener to this control.
     *
     * @param listener (not null, alias created)
     */
    public void addCollisionListener(RagdollCollisionListener listener) {
        collisionListeners.add(listener);
    }

    /**
     * Begin blending the specified link and all its descendants to kinematic
     * animation.
     *
     * @param rootLink the root of the subtree to bind (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    public void animateSubtree(PhysicsLink rootLink, float blendInterval) {
        verifyAddedToSpatial("change modes");
        blendSubtree(rootLink, KinematicSubmode.Animated, blendInterval);
    }

    /**
     * Begin blending all links to purely kinematic mode, driven by animation.
     * TODO callback when the transition completes
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the controlled
     * spatial when the transition completes or null for no change to local
     * transform (unaffected)
     */
    public void blendToKinematicMode(float blendInterval,
            Transform endModelTransform) {
        verifyAddedToSpatial("change modes");

        getTorsoLink().blendToKinematicMode(KinematicSubmode.Animated, blendInterval,
                endModelTransform);
        for (BoneLink boneLink : getBoneLinks()) {
            boneLink.blendToKinematicMode(KinematicSubmode.Animated,
                    blendInterval);
        }
    }

    /**
     * Calculate the ragdoll's total mass and center of mass.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param storeLocation storage for the location of the center (in
     * physics-space coordinates, modified if not null)
     * @param storeVelocity storage for the velocity of the center (psu/second
     * in physics-space coordinates, modified if not null)
     * @return the total mass (&gt;0)
     */
    public float centerOfMass(Vector3f storeLocation, Vector3f storeVelocity) {
        verifyReadyForDynamicMode("calculate the center of mass");

        recalculateCenter();
        if (storeLocation != null) {
            storeLocation.set(centerLocation);
        }
        if (storeVelocity != null) {
            storeVelocity.set(centerVelocity);
        }

        return ragdollMass;
    }

    /**
     * Alter the contact-response setting of the specified link and all its
     * descendants. Note: recursive!
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param rootLink the root of the subtree to modify (not null)
     * @param desiredResponse true for the usual rigid-body response, false for
     * ghost-like response
     */
    public void setContactResponseSubtree(PhysicsLink rootLink,
            boolean desiredResponse) {
        verifyAddedToSpatial("change modes");

        PhysicsRigidBody rigidBody = rootLink.getRigidBody();
        rigidBody.setContactResponse(desiredResponse);

        PhysicsLink[] children = rootLink.listChildren();
        for (PhysicsLink child : children) {
            setContactResponseSubtree(child, desiredResponse);
        }
    }

    /**
     * Immediately put the specified link and all its ancestors (excluding the
     * torso) into dynamic mode. Note: recursive!
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param startLink the start of the chain to modify (not null)
     * @param chainLength the maximum number of links to modify (&ge;0)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamicChain(PhysicsLink startLink, int chainLength,
            Vector3f uniformAcceleration) {
        if (chainLength == 0) {
            return;
        }
        verifyAddedToSpatial("change modes");

        if (startLink instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) startLink;
            boneLink.setDynamic(uniformAcceleration);
        }

        PhysicsLink parent = startLink.getParent();
        if (parent != null && chainLength > 1) {
            setDynamicChain(parent, chainLength - 1, uniformAcceleration);
        }
    }

    /**
     * Immediately put the specified link and all its descendants into dynamic
     * mode. Note: recursive!
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param rootLink the root of the subtree to modify (not null)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamicSubtree(PhysicsLink rootLink,
            Vector3f uniformAcceleration) {
        verifyAddedToSpatial("change modes");

        if (rootLink == getTorsoLink()) {
            getTorsoLink().setDynamic(uniformAcceleration);
        } else if (rootLink instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) rootLink;
            boneLink.setDynamic(uniformAcceleration);
        }

        PhysicsLink[] children = rootLink.listChildren();
        for (PhysicsLink child : children) {
            setDynamicSubtree(child, uniformAcceleration);
        }
    }

    /**
     * Immediately put all links into purely kinematic mode.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void setKinematicMode() {
        verifyAddedToSpatial("set kinematic mode");

        Transform localTransform = getSpatial().getLocalTransform();
        blendToKinematicMode(0f, localTransform);
    }

    /**
     * Immediately put this control into ragdoll mode.
     * <p>
     * Allowed only when the control IS added to a spatial and all links are
     * "ready".
     */
    public void setRagdollMode() {
        verifyReadyForDynamicMode("set ragdoll mode");

        TorsoLink torsoLink = getTorsoLink();
        Vector3f acceleration = gravity(null);
        torsoLink.setDynamic(acceleration);
        for (BoneLink boneLink : getBoneLinks()) {
            boneLink.setDynamic(acceleration);
        }
    }
    // *************************************************************************
    // DacPhysicsLinks methods

    /**
     * Add all managed physics objects to the PhysicsSpace.
     */
    @Override
    protected void addPhysics(PhysicsSpace space) {
        super.addPhysics(space);

        space.addCollisionListener(this);
        space.addTickListener(this);
    }

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

        collisionListeners = cloner.clone(collisionListeners);
        centerLocation = cloner.clone(centerLocation);
        centerVelocity = cloner.clone(centerVelocity);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public DynamicAnimControl jmeClone() {
        try {
            DynamicAnimControl clone = (DynamicAnimControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im the importer (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        // isReady and collisionListeners not read
        ragdollMass = ic.readFloat("ragdollMass", 1f);
        centerLocation
                = (Vector3f) ic.readSavable("centerLocation", new Vector3f());
        centerVelocity
                = (Vector3f) ic.readSavable("centerVelocity", new Vector3f());
    }

    /**
     * Remove all managed physics objects from the PhysicsSpace.
     */
    @Override
    protected void removePhysics(PhysicsSpace space) {
        super.removePhysics(space);

        space.removeCollisionListener(this);
        space.removeTickListener(this);
    }

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex the exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        // isReady and collisionListeners not written
        oc.write(ragdollMass, "ragdollMass", 1f);
        oc.write(centerLocation, "centerLocation", null);
        oc.write(centerVelocity, "centerVelocity", null);
    }
    // *************************************************************************
    // PhysicsCollisionListener methods

    /**
     * For internal use only: callback for collision events.
     *
     * @param event (not null)
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {
        if (event.getNodeA() == null && event.getNodeB() == null) {
            return;
        }
        /*
         * Determine which bone was involved (if any) and also the
         * other collision object involved.
         */
        boolean isThisControlInvolved = false;
        PhysicsLink physicsLink = null;
        PhysicsCollisionObject otherPco = null;
        PhysicsCollisionObject pcoA = event.getObjectA();
        PhysicsCollisionObject pcoB = event.getObjectB();

        Object userA = pcoA.getUserObject();
        Object userB = pcoB.getUserObject();
        if (userA instanceof PhysicsLink) {
            physicsLink = (PhysicsLink) userA;
            DacLinks control = physicsLink.getControl();
            if (control == this) {
                isThisControlInvolved = true;
            }
            otherPco = pcoB;
        }
        if (userB instanceof PhysicsLink) {
            physicsLink = (PhysicsLink) userB;
            DacLinks control = physicsLink.getControl();
            if (control == this) {
                isThisControlInvolved = true;
            }
            otherPco = pcoA;
        }
        /*
         * Discard collisions that don't involve this control.
         */
        if (!isThisControlInvolved) {
            return;
        }
        /*
         * Discard low-impulse collisions.
         */
        float impulseThreshold = eventDispatchImpulseThreshold();
        if (event.getAppliedImpulse() < impulseThreshold) {
            return;
        }
        /*
         * Dispatch an event.
         */
        for (RagdollCollisionListener listener : collisionListeners) {
            listener.collide(physicsLink, otherPco, event);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Begin blending the descendents of the specified link to the specified
     * kinematic submode. Note: recursive!
     *
     * @param rootLink the root of the subtree to blend (not null)
     * @param submode an enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    private void blendDescendants(PhysicsLink rootLink,
            KinematicSubmode submode, float blendInterval) {
        assert rootLink != null;
        assert submode != null;
        assert blendInterval >= 0f : blendInterval;

        PhysicsLink[] children = rootLink.listChildren();
        for (PhysicsLink child : children) {
            if (child instanceof BoneLink) {
                BoneLink boneLink = (BoneLink) child;
                boneLink.blendToKinematicMode(submode, blendInterval);
            }
            blendDescendants(child, submode, blendInterval);
        }
    }

    /**
     * Begin blending the specified link and all its descendants to the
     * specified kinematic submode.
     *
     * @param rootLink the root of the subtree to blend (not null)
     * @param submode the desired submode (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    private void blendSubtree(PhysicsLink rootLink, KinematicSubmode submode,
            float blendInterval) {
        assert rootLink != null;
        assert submode != null;
        assert blendInterval >= 0f : blendInterval;

        blendDescendants(rootLink, submode, blendInterval);

        if (rootLink == getTorsoLink()) {
            getTorsoLink().blendToKinematicMode(submode, blendInterval, null);
        } else if (rootLink instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) rootLink;
            boneLink.blendToKinematicMode(submode, blendInterval);
        }
    }

    /**
     * Recalculate the total mass of the ragdoll. Also updates the location and
     * estimated velocity of the center of mass.
     */
    private void recalculateCenter() {
        double massSum = 0.0;
        Vector3f locationSum = new Vector3f();
        Vector3f velocitySum = new Vector3f();
        Vector3f tmpVector = new Vector3f();
        List<PhysicsLink> links = listLinks(PhysicsLink.class);
        for (PhysicsLink link : links) {
            PhysicsRigidBody rigidBody = link.getRigidBody();
            float mass = rigidBody.getMass();
            massSum += mass;

            rigidBody.getPhysicsLocation(tmpVector);
            tmpVector.multLocal(mass);
            locationSum.addLocal(tmpVector);

            link.velocity(tmpVector);
            tmpVector.multLocal(mass);
            velocitySum.addLocal(tmpVector);
        }

        float invMass = (float) (1.0 / massSum);
        locationSum.mult(invMass, centerLocation);
        velocitySum.mult(invMass, centerVelocity);
        ragdollMass = (float) massSum;
    }
}
