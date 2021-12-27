/*
 * Copyright (c) 2018-2021 jMonkeyEngine
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
import com.jme3.anim.SkinningControl;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access a DynamicAnimControl at the PhysicsLink level once it's been added to
 * a Spatial.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class DacLinks
        extends DacConfiguration
        implements PhysicsTickListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger3
            = Logger.getLogger(DacLinks.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // fields

    /**
     * false until the 1st physics tick, true thereafter, indicating that all
     * links are ready for dynamic mode
     */
    private boolean isReady = false;
    /**
     * bone links in a pre-order, depth-first traversal of the link hierarchy
     */
    private List<BoneLink> boneLinkList = null;
    /**
     * map bone names to bone links
     */
    private Map<String, BoneLink> boneLinks = new HashMap<>(32);
    /**
     * skeleton being controlled
     */
    private Armature skeleton = null;
    /**
     * spatial that provides the mesh-coordinate transform
     */
    private Spatial transformer = null;
    /**
     * torso link for this control
     */
    private TorsoLink torsoLink = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any linked bones (torso only).
     */
    DacLinks() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the named bone.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param boneName the name of the skeleton bone to access
     * @return the pre-existing instance, or null if not found
     */
    public Joint findBone(String boneName) {
        verifyAddedToSpatial("access a bone");
        Joint result = skeleton.getJoint(boneName);
        return result;
    }

    /**
     * Access the BoneLink for the named bone. Returns null if bone is not
     * linked, or if the control is not added to a spatial.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @return the pre-existing BoneLink, or null if not found
     */
    public BoneLink findBoneLink(String boneName) {
        BoneLink boneLink = boneLinks.get(boneName);
        return boneLink;
    }

    /**
     * Access the named link. Returns null if the name is invalid, or if the
     * control is not added to a spatial.
     *
     * @param linkName the name of the link (not null, not empty)
     * @return the pre-existing link, or null if not found
     */
    public PhysicsLink findLink(String linkName) {
        PhysicsLink link;
        if (linkName.startsWith("Bone:")) {
            String boneName = linkName.substring(5);
            link = findBoneLink(boneName);
        } else {
            assert linkName.equals("Torso:");
            link = torsoLink;
        }

        return link;
    }

    /**
     * Access the skeleton. Returns null if the control is not added to a
     * spatial.
     *
     * @return the pre-existing skeleton, or null
     */
    public Armature getSkeleton() {
        return skeleton;
    }

    /**
     * Access the TorsoLink. Returns null if the control is not added to a
     * spatial.
     *
     * @return the pre-existing TorsoLink, or null
     */
    public TorsoLink getTorsoLink() {
        return torsoLink;
    }

    /**
     * Access the spatial with the mesh-coordinate transform. Returns null if
     * the control is not added to a spatial.
     *
     * @return the pre-existing spatial, or null
     */
    Spatial getTransformer() {
        return transformer;
    }

    /**
     * Test whether this control is ready for dynamic mode.
     *
     * @return true if ready, otherwise false
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Enumerate all physics links of the specified type managed by this
     * control.
     *
     * @param <T> subclass of PhysicsLink
     * @param linkType the subclass of PhysicsLink to search for (not null)
     * @return a new array of links (not null, not empty)
     */
    @SuppressWarnings("unchecked")
    public <T extends PhysicsLink> List<T> listLinks(Class<T> linkType) {
        int numLinks = countLinks();
        List<T> result = new ArrayList<>(numLinks);

        if (torsoLink != null
                && linkType.isAssignableFrom(torsoLink.getClass())) {
            result.add((T) torsoLink);
        }
        for (BoneLink link : boneLinkList) {
            if (linkType.isAssignableFrom(link.getClass())) {
                result.add((T) link);
            }
        }

        return result;
    }

    /**
     * Enumerate all managed bones of the named link, in a pre-order,
     * depth-first traversal of the skeleton, such that child bones never
     * precede their ancestors.
     *
     * @param managerName the name of the managing link (not null)
     * @return a new array of managed bones, including the manager if it is not
     * the torso
     */
    Joint[] listManagedBones(String managerName) {
        List<Joint> list = new ArrayList<>(8);

        if (torsoName.equals(managerName)) {
            Joint[] roots = skeleton.getRoots();
            for (Joint rootBone : roots) {
                list.add(rootBone);
                addUnlinkedDescendants(rootBone, list);
            }

        } else {
            BoneLink manager = findBoneLink(managerName);
            if (manager == null) {
                String msg = "No link named " + managerName;
                throw new IllegalArgumentException(msg);
            }
            Joint managerBone = manager.getBone();
            list.add(managerBone);
            addUnlinkedDescendants(managerBone, list);
        }
        /*
         * Convert the list to an array.
         */
        int numManagedBones = list.size();
        Joint[] array = new Joint[numManagedBones];
        list.toArray(array);

        return array;
    }

    /**
     * Enumerate all rigid bodies managed by this control.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @return a new array of pre-existing rigid bodies (not null, not empty)
     */
    public PhysicsRigidBody[] listRigidBodies() {
        verifyAddedToSpatial("enumerate rigid bodies");

        int numLinks = countLinks();
        PhysicsRigidBody[] result = new PhysicsRigidBody[numLinks];

        int linkIndex = 0;
        if (torsoLink != null) {
            result[0] = torsoLink.getRigidBody();
            ++linkIndex;
        }
        for (BoneLink boneLink : boneLinkList) {
            result[linkIndex] = boneLink.getRigidBody();
            ++linkIndex;
        }
        assert linkIndex == numLinks;

        return result;
    }

    /**
     * Copy the model's mesh-to-world transform.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the model's mesh transform (in world coordinates, either
     * storeResult or a new transform, not null)
     */
    Transform meshTransform(Transform storeResult) {
        Transform result = transformer.getWorldTransform().clone();
        return result;
    }

    /**
     * Calculate the physics transform to match the specified skeleton bone.
     *
     * @param bone the skeleton bone to match (not null, unaffected)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated physics transform (either storeResult or a new
     * transform, not null)
     */
    Transform physicsTransform(Joint bone, Vector3f localOffset,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        /*
         * Start with the body's transform in the bone's local coordinates.
         */
        result.setTranslation(localOffset);
        result.setRotation(rotateIdentity);
        result.setScale(1f);
        /*
         * Convert to mesh coordinates.
         */
        Transform localToMesh = bone.getModelTransform();
        result.combineWithParent(localToMesh);
        /*
         * Convert to world (physics-space) coordinates.
         */
        Transform meshToWorld = meshTransform(null);
        result.combineWithParent(meshToWorld);

        return result;
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale to the model
     * after it was initialized.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void rebuild() {
        verifyAddedToSpatial("rebuild the ragdoll");

        Map<String, BoneLink> saveBones = new HashMap<>(boneLinks);
        TorsoLink saveTorso = torsoLink;

        Spatial controlledSpatial = getSpatial();
        removeSpatialData(controlledSpatial);
        createSpatialData(controlledSpatial);

        for (Map.Entry<String, BoneLink> entry : boneLinks.entrySet()) {
            String name = entry.getKey();
            BoneLink newLink = entry.getValue();
            BoneLink oldLink = saveBones.get(name);
            newLink.postRebuild(oldLink);
        }
        if (torsoLink != null) {
            torsoLink.postRebuild(saveTorso);
        }
    }

    /**
     * Alter the mass of the specified link.
     *
     * @param link the link to modify (not null)
     * @param mass the desired mass (&gt;0)
     */
    public void setMass(PhysicsLink link, float mass) {
        if (link instanceof BoneLink) {
            String boneName = link.boneName();
            setMass(boneName, mass);
        } else {
            assert link instanceof TorsoLink;
            setMass(torsoName, mass);
        }
    }

    /**
     * Verify that this control is ready for dynamic mode, which implies that it
     * is added to a Spatial.
     *
     * @param desiredAction (not null, not empty)
     */
    public void verifyReadyForDynamicMode(String desiredAction) {
        assert desiredAction != null;

        verifyAddedToSpatial(desiredAction);

        if (!isReady) {
            String message = "Cannot " + desiredAction
                    + " until the physics has been stepped.";
            throw new IllegalStateException(message);
        }
    }
    // *************************************************************************
    // new protected methods

    /**
     * Access the list of bone links in a pre-order, depth-first traversal of
     * the link hierarchy.
     *
     * @return the pre-existing list (not null)
     */
    protected List<BoneLink> getBoneLinks() {
        assert boneLinkList != null;
        return boneLinkList;
    }

    /**
     * Verify that this control is added to a Spatial.
     *
     * @param desiredAction (not null, not empty)
     */
    protected void verifyAddedToSpatial(String desiredAction) {
        assert desiredAction != null;

        Spatial controlledSpatial = getSpatial();
        if (controlledSpatial == null) {
            String message = "Cannot " + desiredAction
                    + " unless the Control is added to a Spatial.";
            throw new IllegalStateException(message);
        }
    }
    // *************************************************************************
    // DacConfiguration methods

    /**
     * Add all managed physics objects to the PhysicsSpace.
     */
    @Override
    protected void addPhysics(PhysicsSpace space) {
        Vector3f gravity = gravity(null);

        PhysicsRigidBody rigidBody;
        if (torsoLink != null) {
            rigidBody = torsoLink.getRigidBody();
            space.add(rigidBody);
            rigidBody.setGravity(gravity);
        }

        for (BoneLink boneLink : boneLinkList) {
            rigidBody = boneLink.getRigidBody();
            space.add(rigidBody);
            rigidBody.setGravity(gravity);

            PhysicsJoint joint = boneLink.getJoint();
            space.add(joint);
        }
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
        DacLinks originalDac = (DacLinks) original;

        boneLinkList = cloner.clone(boneLinkList);

        boneLinks = new HashMap<>(32);
        for (Map.Entry<String, BoneLink> entry
                : originalDac.boneLinks.entrySet()) {
            String boneName = entry.getKey();
            BoneLink link = entry.getValue();
            BoneLink copyLink = cloner.clone(link);
            boneLinks.put(boneName, copyLink);
        }

        skeleton = cloner.clone(skeleton);
        transformer = cloner.clone(transformer);
        torsoLink = cloner.clone(torsoLink);
    }

    /**
     * Create spatial-dependent data. Invoked each time the control is added to
     * a spatial. Also invoked by {@link #rebuild()}.
     *
     * @param spatial the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial spatial) {
        RagUtils.validate(spatial);

        SkinningControl skeletonControl
                = spatial.getControl(SkinningControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "The controlled spatial must have a SkinningControl. "
                    + "Make sure the control is there and not on a subnode.");
        }
        sortControls(skeletonControl);
        skeletonControl.setHardwareSkinningPreferred(false);
        /*
         * Analyze the model's skeleton.
         */
        skeleton = skeletonControl.getArmature();
        validateSkeleton();
        String[] tempManagerMap = managerMap(skeleton);
        int numBones = skeleton.getJointCount();
        /*
         * Temporarily set all bones' local translations and rotations to bind.
         */
        Transform[] savedTransforms = new Transform[numBones];
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Joint bone = skeleton.getJoint(boneIndex);
            savedTransforms[boneIndex] = bone.getLocalTransform().clone();
            bone.applyBindPose(); // TODO adjust the scale?
        }
        skeleton.update();
        /*
         * Find the target meshes and choose the transform spatial.
         */
        List<Mesh> targetList = RagUtils.listAnimatedMeshes(spatial, null);
        Mesh[] targets = new Mesh[targetList.size()];
        targetList.toArray(targets);
        transformer = RagUtils.findAnimatedGeometry(spatial);
        if (transformer == null) {
            transformer = spatial;
        }
        /*
         * Enumerate mesh-vertex coordinates and assign them to managers.
         */
        Map<String, VectorSet> coordsMap
                = RagUtils.coordsMap(targets, tempManagerMap);
        /*
         * Create the torso link.
         */
        VectorSet vertexLocations = coordsMap.get(torsoName);
        createTorsoLink(vertexLocations, targets);
        /*
         * Create bone links without joints.
         */
        String[] linkedBoneNames = listLinkedBoneNames();
        for (String boneName : linkedBoneNames) {
            vertexLocations = coordsMap.get(boneName);
            createBoneLink(boneName, vertexLocations);
        }
        int numLinkedBones = countLinkedBones();
        assert boneLinks.size() == numLinkedBones;
        /*
         * Add joints to connect each BoneLink rigid body with its parent in the
         * link hierarchy.  Also initialize the boneLinkList.
         */
        boneLinkList = new ArrayList<>(numLinkedBones);
        addJoints(torsoLink);
        assert boneLinkList.size() == numLinkedBones : boneLinkList.size();
        /*
         * Restore the skeleton's pose.
         */
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Joint bone = skeleton.getJoint(boneIndex);
            bone.setLocalTransform(savedTransforms[boneIndex]);
        }
        skeleton.update();

        if (added) {
            addPhysics(space);
        }

        logger3.log(Level.FINE, "Created ragdoll for skeleton.");
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public DacLinks jmeClone() {
        try {
            DacLinks clone = (DacLinks) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Read the mass of the named bone/torso.
     *
     * @param boneName the name of the bone/torso (not null)
     * @return the mass (&gt;0) or NaN if undetermined
     */
    @Override
    public float mass(String boneName) {
        float mass;
        if (getSpatial() == null) {
            mass = super.mass(boneName);
        } else if (torsoName.equals(boneName)) {
            PhysicsRigidBody rigidBody = torsoLink.getRigidBody();
            mass = rigidBody.getMass();
        } else if (boneLinks.containsKey(boneName)) {
            BoneLink link = boneLinks.get(boneName);
            PhysicsRigidBody rigidBody = link.getRigidBody();
            mass = rigidBody.getMass();
        } else {
            String msg = "No bone/torso named " + boneName;
            throw new IllegalArgumentException(msg);
        }

        return mass;
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        boneLinkList
                = ic.readSavableArrayList("boneLinkList", null);
        for (BoneLink link : boneLinkList) {
            String name = link.boneName();
            boneLinks.put(name, link);
        }

        skeleton = (Armature) ic.readSavable("skeleton", null);
        transformer = (Spatial) ic.readSavable("transformer", null);
        torsoLink = (TorsoLink) ic.readSavable("torsoLink", null);
    }

    /**
     * Remove all managed physics objects from the PhysicsSpace.
     */
    @Override
    protected void removePhysics(PhysicsSpace space) {
        assert added;

        PhysicsRigidBody rigidBody;
        if (torsoLink != null) {
            rigidBody = torsoLink.getRigidBody();
            space.remove(rigidBody);
        }

        for (BoneLink boneLink : boneLinks.values()) {
            rigidBody = boneLink.getRigidBody();
            space.remove(rigidBody);

            PhysicsJoint joint = boneLink.getJoint();
            space.remove(joint);
        }
    }

    /**
     * Remove spatial-dependent data. Invoked each time this control is rebuilt
     * or removed from a spatial.
     *
     * @param spat the previously controlled spatial (unused)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics(space);
        }

        skeleton = null;

        boneLinks.clear();
        boneLinkList = null;
        torsoLink = null;
        transformer = null;
    }

    /**
     * Alter the viscous damping ratio for all rigid bodies, including new ones.
     *
     * @param dampingRatio the desired damping ratio (non-negative, 0&rarr;no
     * damping, 1&rarr;critically damped, default=0.6)
     */
    @Override
    public void setDamping(float dampingRatio) {
        super.setDamping(dampingRatio);

        if (getSpatial() != null) {
            PhysicsRigidBody[] bodies = listRigidBodies();
            for (PhysicsRigidBody rigidBody : bodies) {
                rigidBody.setDamping(dampingRatio, dampingRatio);
            }
        }
    }

    /**
     * Alter this control's gravitational acceleration for Ragdoll mode.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected, default=0,-9.8,0)
     */
    @Override
    public void setGravity(Vector3f gravity) {
        super.setGravity(gravity);

        if (getSpatial() != null) { // TODO make sure it's in ragdoll mode
            PhysicsRigidBody[] bodies = listRigidBodies();
            for (PhysicsRigidBody rigidBody : bodies) {
                rigidBody.setGravity(gravity);
            }
        }
    }

    /**
     * Alter the range of motion of the joint connecting the named BoneLink to
     * its parent in the link hierarchy.
     *
     * @param boneName the name of the BoneLink (not null, not empty)
     * @param rom the desired range of motion (not null)
     */
    @Override
    public void setJointLimits(String boneName, RangeOfMotion rom) {
        if (!hasBoneLink(boneName)) {
            String msg = "No linked bone named " + boneName;
            throw new IllegalArgumentException(msg);
        }

        super.setJointLimits(boneName, rom);

        if (getSpatial() != null) {
            BoneLink boneLink = findBoneLink(boneName);
            SixDofJoint joint = (SixDofJoint) boneLink.getJoint();
            rom.setupJoint(joint);
        }
    }

    /**
     * Alter the mass of the named bone/torso.
     *
     * @param boneName the name of the bone, or torsoName (not null)
     * @param mass the desired mass (&gt;0)
     */
    @Override
    public void setMass(String boneName, float mass) {
        super.setMass(boneName, mass);

        if (getSpatial() != null) {
            PhysicsRigidBody rigidBody;
            if (torsoName.equals(boneName)) {
                rigidBody = torsoLink.getRigidBody();
            } else {
                BoneLink link = findBoneLink(boneName);
                rigidBody = link.getRigidBody();
            }
            rigidBody.setMass(mass);
        }
    }

    /**
     * Translate the torso to the specified location.
     *
     * @param vec desired location (not null, unaffected)
     */
    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        torsoLink.getRigidBody().setPhysicsLocation(vec);
    }

    /**
     * Rotate the torso to the specified orientation.
     *
     * @param quat desired orientation (not null, unaffected)
     */
    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        torsoLink.getRigidBody().setPhysicsRotation(quat);
    }

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is added to a scene. Do not invoke directly
     * from user code.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        verifyAddedToSpatial("update the control");
        if (!isEnabled()) {
            return;
        }

        if (torsoLink != null) {
            torsoLink.update(tpf);
        }
        for (BoneLink boneLink : boneLinkList) {
            boneLink.update(tpf);
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

        int count = countLinkedBones();
        Savable[] savableArray = new Savable[count];
        boneLinkList.toArray(savableArray);
        oc.write(savableArray, "boneLinkList", null);

        oc.write(skeleton, "skeleton", null);
        oc.write(transformer, "transformer", null);
        oc.write(torsoLink, "torsoLink", null);
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just after the physics has been stepped.
     * Used to re-activate any deactivated rigid bodies.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        assert space == getPhysicsSpace();

        torsoLink.postTick();
        for (BoneLink boneLink : boneLinkList) {
            boneLink.postTick();
        }

        isReady = true;
    }

    /**
     * Callback from Bullet, invoked just before the physics is stepped. A good
     * time to clear/apply forces.
     *
     * @param space the space that is about to be stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        assert space == getPhysicsSpace();

        torsoLink.preTick(timeStep);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.preTick(timeStep);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add joints to connect the named bone/torso link with each of its
     * children. Also fill in the boneLinkList. Note: recursive!
     *
     * @param parentLink the parent bone/torso link (not null)
     */
    private void addJoints(PhysicsLink parentLink) {
        List<String> childNames = childNames(parentLink);
        for (String childName : childNames) {
            /*
             * Add the joint and configure its range of motion.
             * Also initialize the BoneLink's parent and its array
             * of managed bones.
             */
            BoneLink childLink = findBoneLink(childName);
            childLink.addJoint(parentLink);
            /*
             * Add the BoneLink to the pre-order list.
             */
            boneLinkList.add(childLink);

            addJoints(childLink);
        }
    }

    /**
     * Enumerate all immediate child BoneLinks of the specified bone/torso link.
     *
     * @param link the bone/torso link (not null)
     * @return a new list of bone names
     */
    private List<String> childNames(PhysicsLink link) {
        assert link != null;

        String linkName;
        if (link == torsoLink) {
            linkName = torsoName;
        } else {
            linkName = link.boneName();
        }

        List<String> result = new ArrayList<>(8);
        for (String childName : listLinkedBoneNames()) {
            Joint bone = findBone(childName);
            Joint parent = bone.getParent();
            if (parent != null && findManager(parent).equals(linkName)) {
                result.add(childName);
            }
        }

        return result;
    }

    /**
     * Create a jointless BoneLink for the named bone, and add it to the
     * boneLinks map.
     *
     * @param boneName the name of the bone to be linked (not null)
     * @param vertexLocations the set of vertex locations (not null, not empty)
     */
    private void createBoneLink(String boneName, VectorSet vertexLocations) {
        Joint bone = findBone(boneName);
        Transform boneToMesh = bone.getModelTransform();
        Transform meshToBone = boneToMesh.invert();
        //logger3.log(Level.INFO, "meshToBone = {0}", meshToBone);
        /*
         * Create the CollisionShape and locate the center of mass.
         */
        CollisionShape shape;
        Vector3f center;
        if (vertexLocations == null || vertexLocations.numVectors() == 0) {
            throw new IllegalStateException("no vertex for " + boneName);
        } else {
            center = vertexLocations.mean(null);
            center.subtractLocal(bone.getModelTransform().getTranslation());
            shape = createShape(meshToBone, center, vertexLocations);
        }

        meshToBone.getTranslation().zero();
        float mass = super.mass(boneName);
        Vector3f offset = meshToBone.transformVector(center, null);
        BoneLink link = new BoneLink(this, bone, shape, mass, offset);
        boneLinks.put(boneName, link);
    }

    /**
     * Create a CollisionShape for the specified transform, center, and vertex
     * locations.
     *
     * @param vertexToShape the transform from vertex coordinates to de-scaled
     * shape coordinates (not null, unaffected)
     * @param center the location of the shape's center, in vertex coordinates
     * (not null, unaffected)
     * @param vertexLocations the set of vertex locations (not null, not empty,
     * TRASHED)
     * @return a new CollisionShape
     */
    private CollisionShape createShape(Transform vertexToShape, Vector3f center,
            VectorSet vertexLocations) {
        int numVectors = vertexLocations.numVectors();
        assert numVectors > 0 : numVectors;

        Vector3f tempLocation = new Vector3f();
        int numPoints = vertexLocations.numVectors();
        float points[] = new float[3 * numPoints];
        FloatBuffer buffer = vertexLocations.toBuffer();
        buffer.rewind();
        int floatIndex = 0;
        while (buffer.hasRemaining()) {
            tempLocation.x = buffer.get();
            tempLocation.y = buffer.get();
            tempLocation.z = buffer.get();
            /*
             * Translate so that vertex coordinates are relative to
             * the shape's center.
             */
            tempLocation.subtractLocal(center);
            /*
             * Transform vertex coordinates to de-scaled shape coordinates.
             */
            vertexToShape.transformVector(tempLocation, tempLocation);
            points[floatIndex] = tempLocation.x;
            points[floatIndex + 1] = tempLocation.y;
            points[floatIndex + 2] = tempLocation.z;
            floatIndex += 3;
        }

        CollisionShape result = new HullCollisionShape(points);

        return result;
    }

    /**
     * Create the TorsoLink.
     *
     * @param vertexLocations the set of vertex locations (not null, not empty)
     * @param meshes array of animated meshes to use (not null, unaffected)
     */
    private void createTorsoLink(VectorSet vertexLocations, Mesh[] meshes) {
        if (vertexLocations == null || vertexLocations.numVectors() == 0) {
            throw new IllegalArgumentException(
                    "No mesh vertices for the torso."
                    + " Make sure the root bone is not linked.");
        }
        /*
         * Create the CollisionShape.
         */
        Joint bone = RagUtils.findMainBone(skeleton, meshes);
        assert bone.getParent() == null;
        Transform boneToMesh = bone.getModelTransform();
        Transform meshToBone = boneToMesh.invert();
        Vector3f center = vertexLocations.mean(null);
        center.subtractLocal(boneToMesh.getTranslation());
        CollisionShape shape = createShape(meshToBone, center, vertexLocations);

        meshToBone.getTranslation().zero();
        Vector3f offset = meshToBone.transformVector(center, null);

        Transform meshToModel;
        Spatial cgm = getSpatial();
        if (cgm instanceof Node) {
            Transform modelToMesh
                    = RagUtils.relativeTransform(transformer, (Node) cgm, null);
            meshToModel = modelToMesh.invert();
        } else {
            meshToModel = transformIdentity;
        }

        float mass = super.mass(torsoName);
        torsoLink = new TorsoLink(this, bone, shape, mass, meshToModel, offset);
    }

    /**
     * Sort the controls of the controlled spatial, such that this control will
     * come BEFORE the specified SkinningControl.
     *
     * @param skinningControl (not null)
     */
    private void sortControls(SkinningControl skinningControl) {
        assert skinningControl != null;

        int dacIndex = RagUtils.findIndex(spatial, this);
        assert dacIndex != -1;
        int scIndex = RagUtils.findIndex(spatial, skinningControl);
        assert scIndex != -1;
        assert dacIndex != scIndex;

        if (dacIndex > scIndex) {
            /*
             * Remove the SkinningControl and re-add it to make sure it will get
             * updated *after* this control.
             */
            spatial.removeControl(skinningControl);
            spatial.addControl(skinningControl);

            dacIndex = RagUtils.findIndex(spatial, this);
            assert dacIndex != -1;
            scIndex = RagUtils.findIndex(spatial, skinningControl);
            assert scIndex != -1;
            assert dacIndex < scIndex;
        }
    }

    /**
     * Validate the model's skeleton.
     */
    private void validateSkeleton() {
        RagUtils.validate(skeleton);

        for (String boneName : listLinkedBoneNames()) {
            Joint bone = findBone(boneName);
            if (bone == null) {
                String msg = String.format(
                        "Linked bone %s not found in skeleton.", boneName);
                throw new IllegalArgumentException(msg);
            }
            if (bone.getParent() == null) {
                logger3.log(Level.WARNING, "Linked bone {0} is a root bone.",
                        boneName);
            }
        }
    }
}
