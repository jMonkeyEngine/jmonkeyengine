/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.animation.*;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.control.ragdoll.*;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <strong>This control is still a WIP, use it at your own risk</strong><br> To
 * use this control you need a model with an AnimControl and a
 * SkeletonControl.<br> This should be the case if you imported an animated
 * model from Ogre or blender.<br> Note enabling/disabling the control
 * add/removes it from the physics space<br>
 * <p>
 * This control creates collision shapes for all bones in the skeleton when you
 * invoke spatial.addControl(ragdollControl). <ul> <li>The shape is
 * HullCollision shape based on the vertices associated with each bone and based
 * on a tweakable weight threshold (see setWeightThreshold)</li> <li>If you
 * don't want each bone to be a collision shape, you can specify what bone to
 * use by using the addBoneName method<br> By using this method, bone that are
 * not used to create a shape, are "merged" to their parent to create the
 * collision shape. </li>
 * </ul>
 * <p>
 * There are 2 modes for this control : <ul> <li><strong>The kinematic modes
 * :</strong><br> this is the default behavior, this means that the collision
 * shapes of the body are able to interact with physics enabled objects. in this
 * mode physics shapes follow the motion of the animated skeleton (for example
 * animated by a key framed animation) this mode is enabled by calling
 * setKinematicMode(); </li> <li><strong>The ragdoll modes:</strong><br> To
 * enable this behavior, you need to invoke the setRagdollMode() method. In this
 * mode the character is entirely controlled by physics, so it will fall under
 * the gravity and move if any force is applied to it.</li>
 * </ul>
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Normen Hansen and RÃ©my Bouquet (Nehon)
 */
@Deprecated
public class KinematicRagdollControl extends AbstractPhysicsControl implements PhysicsCollisionListener, JmeCloneable {

    /**
     * list of registered collision listeners
     */
    protected static final Logger logger = Logger.getLogger(KinematicRagdollControl.class.getName());
    protected List<RagdollCollisionListener> listeners;
    protected final Set<String> boneList = new TreeSet<>();
    protected final Map<String, PhysicsBoneLink> boneLinks = new HashMap<>();
    protected final Vector3f modelPosition = new Vector3f();
    protected final Quaternion modelRotation = new Quaternion();
    protected final PhysicsRigidBody baseRigidBody;
    /**
     * model being controlled
     */
    protected Spatial targetModel;
    /**
     * skeleton being controlled
     */
    protected Skeleton skeleton;
    protected RagdollPreset preset = new HumanoidRagdollPreset();
    protected Vector3f initScale;
    protected Mode mode = Mode.Kinematic;
    protected boolean debug = false;
    protected boolean blendedControl = false;
    protected float weightThreshold = -1.0f;
    protected float blendStart = 0.0f;
    /**
     * blending interval for animations (in seconds, &ge;0)
     */
    protected float blendTime = 1.0f;
    protected float eventDispatchImpulseThreshold = 10;
    protected float rootMass = 15;
    /**
     * accumulate total mass of ragdoll when control is added to a scene
     */
    protected float totalMass = 0;
    private Map<String, Vector3f> ikTargets = new HashMap<>();
    private Map<String, Integer> ikChainDepth = new HashMap<>();
    /**
     * rotational speed for inverse kinematics (radians per second, default=7)
     */
    private float ikRotSpeed = 7f;
    /**
     * viscous limb-damping ratio (0&rarr;no damping, 1&rarr;critically damped,
     * default=0.6)
     */
    private float limbDampening = 0.6f;
    /**
     * distance threshold for inverse kinematics (default=0.1)
     */
    private float IKThreshold = 0.1f;
    /**
     * Enumerate joint-control modes for this control.
     */
    public static enum Mode {
        /**
         * collision shapes follow the movements of bones in the skeleton
         */
        Kinematic,
        /**
         * skeleton is controlled by Bullet physics (gravity and collisions)
         */
        Ragdoll,
        /**
         * skeleton is controlled by inverse-kinematic targets
         */
        IK
    }

    /**
     * Link a bone to a jointed rigid body.
     */
    public class PhysicsBoneLink implements Savable {

        protected PhysicsRigidBody rigidBody;
        protected Bone bone;
        protected SixDofJoint joint;
        protected Quaternion initalWorldRotation;
        protected Quaternion startBlendingRot = new Quaternion();
        protected Vector3f startBlendingPos = new Vector3f();

        /**
         * Instantiate an uninitialized link.
         */
        public PhysicsBoneLink() {
        }

        /**
         * Access the linked bone.
         *
         * @return the pre-existing instance or null
         */
        public Bone getBone() {
            return bone;
        }

        /**
         * Access the linked body.
         *
         * @return the pre-existing instance or null
         */
        public PhysicsRigidBody getRigidBody() {
            return rigidBody;
        }

        /**
         * Serialize this bone link, for example when saving to a J3O file.
         *
         * @param ex exporter (not null)
         * @throws IOException from exporter
         */
        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(rigidBody, "rigidBody", null);
            oc.write(bone, "bone", null);
            oc.write(joint, "joint", null);
            oc.write(initalWorldRotation, "initalWorldRotation", null);
            oc.write(startBlendingRot, "startBlendingRot", new Quaternion());
            oc.write(startBlendingPos, "startBlendingPos", new Vector3f());
        }

        /**
         * De-serialize this bone link, for example when loading from a J3O
         * file.
         *
         * @param im importer (not null)
         * @throws IOException from importer
         */
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule ic = im.getCapsule(this);
            rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
            bone = (Bone) ic.readSavable("bone", null);
            joint = (SixDofJoint) ic.readSavable("joint", null);
            initalWorldRotation = (Quaternion) ic.readSavable("initalWorldRotation", null);
            startBlendingRot = (Quaternion) ic.readSavable("startBlendingRot", null);
            startBlendingPos = (Vector3f) ic.readSavable("startBlendingPos", null);
        }
    }

    /**
     * Instantiate an enabled control.
     */
    public KinematicRagdollControl() {
        baseRigidBody = new PhysicsRigidBody(new BoxCollisionShape(Vector3f.UNIT_XYZ.mult(0.1f)), 1);
        baseRigidBody.setKinematic(mode == Mode.Kinematic);
    }

    /**
     * Instantiate an enabled control with the specified weight threshold.
     *
     * @param weightThreshold (&gt;0, &lt;1)
     */
    public KinematicRagdollControl(float weightThreshold) {
        this();
        this.weightThreshold = weightThreshold;
    }

    /**
     * Instantiate an enabled control with the specified preset and weight
     * threshold.
     *
     * @param preset (not null)
     * @param weightThreshold (&gt;0, &lt;1)
     */
    public KinematicRagdollControl(RagdollPreset preset, float weightThreshold) {
        this();
        this.preset = preset;
        this.weightThreshold = weightThreshold;
    }

    /**
     * Instantiate an enabled control with the specified preset.
     *
     * @param preset (not null)
     */
    public KinematicRagdollControl(RagdollPreset preset) {
        this();
        this.preset = preset;
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
        if (!enabled) {
            return;
        }
        if(mode == Mode.IK){
            ikUpdate(tpf);
        } else if (mode == Mode.Ragdoll && targetModel.getLocalTranslation().equals(modelPosition)) {
            //if the ragdoll has the control of the skeleton, we update each bone with its position in physics world space.
            ragDollUpdate(tpf);
        } else {
            kinematicUpdate(tpf);
        }
    }

    /**
     * Update this control in Ragdoll mode, based on Bullet physics.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void ragDollUpdate(float tpf) {
        TempVars vars = TempVars.get();
        Quaternion tmpRot1 = vars.quat1;
        Quaternion tmpRot2 = vars.quat2;

        for (PhysicsBoneLink link : boneLinks.values()) {

            Vector3f position = vars.vect1;

            //retrieving bone position in physics world space
            Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
            //transforming this position with inverse transforms of the model
            targetModel.getWorldTransform().transformInverseVector(p, position);

            //retrieving bone rotation in physics world space
            Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();

            //multiplying this rotation by the initialWorld rotation of the bone,
            //then transforming it with the inverse world rotation of the model
            tmpRot1.set(q).multLocal(link.initalWorldRotation);
            tmpRot2.set(targetModel.getWorldRotation()).inverseLocal().mult(tmpRot1, tmpRot1);
            tmpRot1.normalizeLocal();

            //if the bone is the root bone, we apply the physic's transform to the model, so its position and rotation are correctly updated
            if (link.bone.getParent() == null) {

                //offsetting the physics position/rotation by the root bone inverse model space position/rotation
                modelPosition.set(p).subtractLocal(link.bone.getBindPosition());
                targetModel.getParent().getWorldTransform().transformInverseVector(modelPosition, modelPosition);
                modelRotation.set(q).multLocal(tmpRot2.set(link.bone.getBindRotation()).inverseLocal());


                //applying transforms to the model
                targetModel.setLocalTranslation(modelPosition);

                targetModel.setLocalRotation(modelRotation);

                //Applying computed transforms to the bone
                link.bone.setUserTransformsInModelSpace(position, tmpRot1);

            } else {
                // Some bones in the skeleton might not be associated with a collision shape,
                // so we update them recursively.
                RagdollUtils.setTransform(link.bone, position, tmpRot1, false, boneList);
            }
        }
        vars.release();
    }

    /**
     * Update this control in Kinematic mode, based on bone animation tracks.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void kinematicUpdate(float tpf) {
        //the ragdoll does not have control, so the keyframe animation updates the physics position of the physics bones
        TempVars vars = TempVars.get();
        Quaternion tmpRot1 = vars.quat1;
        Quaternion tmpRot2 = vars.quat2;
        Vector3f position = vars.vect1;
        for (PhysicsBoneLink link : boneLinks.values()) {
//            if(link.usedbyIK){
//                continue;
//            }
            //blended control means keyframe animation is updating the skeleton,
            //but to allow smooth transition, we blend this transformation with the saved position of the ragdoll
            if (blendedControl) {
                Vector3f position2 = vars.vect2;
                //initializing tmp vars with the start position/rotation of the ragdoll
                position.set(link.startBlendingPos);
                tmpRot1.set(link.startBlendingRot);

                //interpolate between ragdoll position/rotation and keyframe position/rotation
                tmpRot2.set(tmpRot1).nlerp(link.bone.getModelSpaceRotation(), blendStart / blendTime);
                position2.set(position).interpolateLocal(link.bone.getModelSpacePosition(), blendStart / blendTime);
                tmpRot1.set(tmpRot2);
                position.set(position2);

                //update bone transforms
                RagdollUtils.setTransform(link.bone, position, tmpRot1, true, boneList);
            }
            //setting skeleton transforms to the ragdoll
            matchPhysicObjectToBone(link, position, tmpRot1);
            modelPosition.set(targetModel.getLocalTranslation());
        }

        //time control for blending
        if (blendedControl) {
            blendStart += tpf;
            if (blendStart > blendTime) {
                blendedControl = false;
            }
        }
        vars.release();
    }
    /**
     * Update this control in IK mode, based on IK targets.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void ikUpdate(float tpf){
        TempVars vars = TempVars.get();

        Quaternion tmpRot1 = vars.quat1;
        Quaternion[] tmpRot2 = new Quaternion[]{vars.quat2, new Quaternion()};

        Iterator<String> it = ikTargets.keySet().iterator();
        float distance;
        Bone bone;
        String boneName;
        while (it.hasNext()) {
            
            boneName = it.next();
            bone = boneLinks.get(boneName).bone;
            if (!bone.hasUserControl()) {
                Logger.getLogger(KinematicRagdollControl.class.getSimpleName()).log(Level.FINE, "{0} doesn't have user control", boneName);
                continue;
            }
            distance = bone.getModelSpacePosition().distance(ikTargets.get(boneName));
            if (distance < IKThreshold) {
                Logger.getLogger(KinematicRagdollControl.class.getSimpleName()).log(Level.FINE, "Distance is close enough");
                continue;
            }
            int depth = 0;
            int maxDepth = ikChainDepth.get(bone.getName());
            updateBone(boneLinks.get(bone.getName()), tpf * FastMath.sqrt(distance), vars, tmpRot1, tmpRot2, bone, ikTargets.get(boneName), depth, maxDepth);

            Vector3f position = vars.vect1;
            
            for (PhysicsBoneLink link : boneLinks.values()) {
                matchPhysicObjectToBone(link, position, tmpRot1);
            }
        }
        vars.release();
    }
    
    /**
     * Update a bone and its ancestors in IK mode. Note: recursive!
     *
     * @param link the bone link for the affected bone (may be null)
     * @param tpf the time interval between frames (in seconds, &ge;0)
     * @param vars unused
     * @param tmpRot1 temporary storage used in calculations (not null)
     * @param tmpRot2 temporary storage used in calculations (not null)
     * @param tipBone (not null)
     * @param target the location target in model space (not null, unaffected)
     * @param depth depth of the recursion (&ge;0)
     * @param maxDepth recursion limit (&ge;0)
     */
    public void updateBone(PhysicsBoneLink link, float tpf, TempVars vars, Quaternion tmpRot1, Quaternion[] tmpRot2, Bone tipBone, Vector3f target, int depth, int maxDepth) {
        if (link == null || link.bone.getParent() == null) {
            return;
        }
        Quaternion preQuat = link.bone.getLocalRotation();
        Vector3f vectorAxis;
        
        float[] measureDist = new float[]{Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        for (int dirIndex = 0; dirIndex < 3; dirIndex++) {
            if (dirIndex == 0) {
                vectorAxis = Vector3f.UNIT_Z;
            } else if (dirIndex == 1) {
                vectorAxis = Vector3f.UNIT_X;
            } else {
                vectorAxis = Vector3f.UNIT_Y;
            }

            for (int posOrNeg = 0; posOrNeg < 2; posOrNeg++) {
                float rot = ikRotSpeed * tpf / (link.rigidBody.getMass() * 2);

                rot = FastMath.clamp(rot, link.joint.getRotationalLimitMotor(dirIndex).getLoLimit(), link.joint.getRotationalLimitMotor(dirIndex).getHiLimit());
                tmpRot1.fromAngleAxis(rot, vectorAxis);
//                tmpRot1.fromAngleAxis(rotSpeed * tpf / (link.rigidBody.getMass() * 2), vectorAxis);
                
                
                tmpRot2[posOrNeg] = link.bone.getLocalRotation().mult(tmpRot1);
                tmpRot2[posOrNeg].normalizeLocal();

                ikRotSpeed = -ikRotSpeed;
               
                link.bone.setLocalRotation(tmpRot2[posOrNeg]);
                link.bone.update();
                measureDist[posOrNeg] = tipBone.getModelSpacePosition().distance(target);
                link.bone.setLocalRotation(preQuat);
            }

            if (measureDist[0] < measureDist[1]) {
                link.bone.setLocalRotation(tmpRot2[0]);
            } else if (measureDist[0] > measureDist[1]) {
                link.bone.setLocalRotation(tmpRot2[1]);
            }

        }
        link.bone.getLocalRotation().normalizeLocal();

        link.bone.update();
//        link.usedbyIK = true;
        if (link.bone.getParent() != null && depth < maxDepth) {
            
            updateBone(boneLinks.get(link.bone.getParent().getName()), tpf * limbDampening, vars, tmpRot1, tmpRot2, tipBone, target, depth + 1, maxDepth);
        }
    }

    /**
     * Alter the transforms of a rigidBody to match the transforms of a bone.
     * This is used to make the ragdoll follow animated motion in Kinematic mode
     *
     * @param link the bone link connecting the bone and the rigidBody
     * @param position temporary storage used in calculations (not null)
     * @param tmpRot1 temporary storage used in calculations (not null)
     */
    protected void matchPhysicObjectToBone(PhysicsBoneLink link, Vector3f position, Quaternion tmpRot1) {
        //computing position from rotation and scale
        targetModel.getWorldTransform().transformVector(link.bone.getModelSpacePosition(), position);

        //computing rotation
        tmpRot1.set(link.bone.getModelSpaceRotation()).multLocal(link.bone.getModelBindInverseRotation());
        targetModel.getWorldRotation().mult(tmpRot1, tmpRot1);
        tmpRot1.normalizeLocal();

        //updating physics location/rotation of the physics bone
        link.rigidBody.setPhysicsLocation(position);
        link.rigidBody.setPhysicsRotation(tmpRot1);

    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale on the ragdoll
     * after it was initialized. Same as re-attaching.
     */
    public void reBuild() {
        if (spatial == null) {
            return;
        }
        removeSpatialData(spatial);
        createSpatialData(spatial);
    }

    /**
     * Create spatial-dependent data. Invoked when this control is added to a
     * scene.
     *
     * @param model the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial model) {
        targetModel = model;
        Node parent = model.getParent();


        Vector3f initPosition = model.getLocalTranslation().clone();
        Quaternion initRotation = model.getLocalRotation().clone();
        initScale = model.getLocalScale().clone();

        model.removeFromParent();
        model.setLocalTranslation(Vector3f.ZERO);
        model.setLocalRotation(Quaternion.IDENTITY);
        model.setLocalScale(1);
        //HACK ALERT change this
        // I remove the SkeletonControl and re-add it to the Spatial to make sure it's after the ragdoll control in the stack.
        //Find a proper way to order the controls.
        SkeletonControl sc = model.getControl(SkeletonControl.class);
        if(sc == null){
            throw new IllegalArgumentException("The root node of the model should have a SkeletonControl. Make sure the control is there and that it's not on a sub node.");
        }
        model.removeControl(sc);
        model.addControl(sc);

        if (boneList.isEmpty()) {
            // add all bones to the list
            skeleton = sc.getSkeleton();
            for (int boneI = 0; boneI < skeleton.getBoneCount(); boneI++) {
                String boneName = skeleton.getBone(boneI).getName();
                boneList.add(boneName);
            }
        }
        // filter out bones without vertices
        filterBoneList(sc);

        if (boneList.isEmpty()) {
            throw new IllegalArgumentException(
                    "No suitable bones were found in the model's skeleton.");
        }

        // put into bind pose and compute bone transforms in model space
        // maybe don't reset to ragdoll out of animations?
        scanSpatial(model);


        if (parent != null) {
            parent.attachChild(model);

        }
        model.setLocalTranslation(initPosition);
        model.setLocalRotation(initRotation);
        model.setLocalScale(initScale);

        if (added) {
            addPhysics(space);
        }
        logger.log(Level.FINE, "Created physics ragdoll for skeleton {0}", skeleton);
    }

    /**
     * Remove any bones without vertices from the boneList, so that every hull
     * shape will contain at least 1 vertex.
     */
    private void filterBoneList(SkeletonControl skeletonControl) {
        Mesh[] targets = skeletonControl.getTargets();
        Skeleton skel = skeletonControl.getSkeleton();
        for (int boneI = 0; boneI < skel.getBoneCount(); boneI++) {
            String boneName = skel.getBone(boneI).getName();
            if (boneList.contains(boneName)) {
                boolean hasVertices = RagdollUtils.hasVertices(boneI, targets,
                        weightThreshold);
                if (!hasVertices) {
                    boneList.remove(boneName);
                }
            }
        }
    }

    /**
     * Destroy spatial-dependent data. Invoked when this control is removed from
     * a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics(space);
        }
        boneLinks.clear();
    }

    /**
     * Add a bone name to this control. Repeated invocations of this method can
     * be used to specify which bones to use when generating collision shapes.
     * <p>
     * Not allowed after attaching the control.
     *
     * @param name the name of the bone to add
     */
    public void addBoneName(String name) {
        boneList.add(name);
    }

    /**
     * Generate physics shapes and bone links for the skeleton.
     *
     * @param model the spatial with the model's SkeletonControl (not null)
     */
    protected void scanSpatial(Spatial model) {
        AnimControl animControl = model.getControl(AnimControl.class);
        Map<Integer, List<Float>> pointsMap = null;
        if (weightThreshold == -1.0f) {
            pointsMap = RagdollUtils.buildPointMap(model);
        }

        skeleton = animControl.getSkeleton();
        skeleton.resetAndUpdate();
        for (int i = 0; i < skeleton.getRoots().length; i++) {
            Bone childBone = skeleton.getRoots()[i];
            if (childBone.getParent() == null) {
                logger.log(Level.FINE, "Found root bone in skeleton {0}", skeleton);
                boneRecursion(model, childBone, baseRigidBody, 1, pointsMap);
            }
        }
    }

    /**
     * Generate a physics shape and bone links for the specified bone. Note:
     * recursive!
     *
     * @param model the spatial with the model's SkeletonControl (not null)
     * @param bone the bone to be linked (not null)
     * @param parent the body linked to the parent bone (not null)
     * @param recursionCount depth of the recursion (&ge;1)
     * @param pointsMap (not null)
     */
    protected void boneRecursion(Spatial model, Bone bone, PhysicsRigidBody parent, int recursionCount, Map<Integer, List<Float>> pointsMap) {
        PhysicsRigidBody parentShape = parent;
        if (boneList.contains(bone.getName())) {

            PhysicsBoneLink link = new PhysicsBoneLink();
            link.bone = bone;

            //create the collision shape
            HullCollisionShape shape = null;
            if (pointsMap != null) {
                //build a shape for the bone, using the vertices that are most influenced by this bone
                shape = RagdollUtils.makeShapeFromPointMap(pointsMap, RagdollUtils.getBoneIndices(link.bone, skeleton, boneList), initScale, link.bone.getModelSpacePosition());
            } else {
                //build a shape for the bone, using the vertices associated with this bone with a weight above the threshold
                shape = RagdollUtils.makeShapeFromVerticeWeights(model, RagdollUtils.getBoneIndices(link.bone, skeleton, boneList), initScale, link.bone.getModelSpacePosition(), weightThreshold);
            }

            PhysicsRigidBody shapeNode = new PhysicsRigidBody(shape, rootMass / recursionCount);

            shapeNode.setKinematic(mode == Mode.Kinematic);
            totalMass += rootMass / recursionCount;

            link.rigidBody = shapeNode;
            link.initalWorldRotation = bone.getModelSpaceRotation().clone();

            if (parent != null) {
                //get joint position for parent
                Vector3f posToParent = new Vector3f();
                if (bone.getParent() != null) {
                    bone.getModelSpacePosition().subtract(bone.getParent().getModelSpacePosition(), posToParent).multLocal(initScale);
                }

                SixDofJoint joint = new SixDofJoint(parent, shapeNode, posToParent, new Vector3f(0, 0, 0f), true);
                preset.setupJointForBone(bone.getName(), joint);

                link.joint = joint;
                joint.setCollisionBetweenLinkedBodys(false);
            }
            boneLinks.put(bone.getName(), link);
            shapeNode.setUserObject(link);
            parentShape = shapeNode;
        }

        for (Iterator<Bone> it = bone.getChildren().iterator(); it.hasNext();) {
            Bone childBone = it.next();
            boneRecursion(model, childBone, parentShape, recursionCount + 1, pointsMap);
        }
    }

    /**
     * Alter the limits of the joint connecting the specified bone to its
     * parent. Can only be invoked after adding the control to a spatial.
     *
     * @param boneName the name of the bone
     * @param maxX the maximum rotation on the X axis (in radians)
     * @param minX the minimum rotation on the X axis (in radians)
     * @param maxY the maximum rotation on the Y axis (in radians)
     * @param minY the minimum rotation on the Y axis (in radians)
     * @param maxZ the maximum rotation on the Z axis (in radians)
     * @param minZ the minimum rotation on the Z axis (in radians)
     */
    public void setJointLimit(String boneName, float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            RagdollUtils.setJointLimit(link.joint, maxX, minX, maxY, minY, maxZ, minZ);
        } else {
            logger.log(Level.WARNING, "Not joint was found for bone {0}. make sure you call spatial.addControl(ragdoll) before setting joints limit", boneName);
        }
    }

    /**
     * Return the joint between the specified bone and its parent. This return
     * null if it's invoked before adding the control to a spatial
     *
     * @param boneName the name of the bone
     * @return the joint between the given bone and its parent
     */
    public SixDofJoint getJoint(String boneName) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            return link.joint;
        } else {
            logger.log(Level.WARNING, "Not joint was found for bone {0}. make sure you call spatial.addControl(ragdoll) before setting joints limit", boneName);
            return null;
        }
    }

    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        if (baseRigidBody != null) {
            baseRigidBody.setPhysicsLocation(vec);
        }
    }

    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        if (baseRigidBody != null) {
            baseRigidBody.setPhysicsRotation(quat);
        }
    }

    @Override
    protected void addPhysics(PhysicsSpace space) {
        if (baseRigidBody != null) {
            space.add(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.rigidBody != null) {
                space.add(physicsBoneLink.rigidBody);
                if (physicsBoneLink.joint != null) {
                    space.add(physicsBoneLink.joint);

                }
            }
        }
        space.addCollisionListener(this);
    }

    @Override
    protected void removePhysics(PhysicsSpace space) {
        if (baseRigidBody != null) {
            space.remove(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.joint != null) {
                space.remove(physicsBoneLink.joint);
                if (physicsBoneLink.rigidBody != null) {
                    space.remove(physicsBoneLink.rigidBody);
                }
            }
        }
        space.removeCollisionListener(this);
    }

    /**
     * For internal use only: callback for collision event
     *
     * @param event (not null)
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {
        PhysicsCollisionObject objA = event.getObjectA();
        PhysicsCollisionObject objB = event.getObjectB();

        //excluding collisions that involve 2 parts of the ragdoll
        if (event.getNodeA() == null && event.getNodeB() == null) {
            return;
        }

        //discarding low impulse collision
        if (event.getAppliedImpulse() < eventDispatchImpulseThreshold) {
            return;
        }

        boolean hit = false;
        Bone hitBone = null;
        PhysicsCollisionObject hitObject = null;

        //Computing which bone has been hit
        if (objA.getUserObject() instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) objA.getUserObject();
            if (link != null) {
                hit = true;
                hitBone = link.bone;
                hitObject = objB;
            }
        }

        if (objB.getUserObject() instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) objB.getUserObject();
            if (link != null) {
                hit = true;
                hitBone = link.bone;
                hitObject = objA;

            }
        }

        //dispatching the event if the ragdoll has been hit
        if (hit && listeners != null) {
            for (RagdollCollisionListener listener : listeners) {
                listener.collide(hitBone, hitObject, event);
            }
        }

    }

    /**
     * Enable or disable the ragdoll behaviour. if ragdollEnabled is true, the
     * character motion will only be powered by physics else, the character will
     * be animated by the keyframe animation, but will be able to physically
     * interact with its physics environment
     *
     * @param mode an enum value (not null)
     */
    protected void setMode(Mode mode) {
        this.mode = mode;
        AnimControl animControl = targetModel.getControl(AnimControl.class);
        animControl.setEnabled(mode == Mode.Kinematic);

        baseRigidBody.setKinematic(mode == Mode.Kinematic);
        if (mode != Mode.IK) {
            TempVars vars = TempVars.get();

            for (PhysicsBoneLink link : boneLinks.values()) {
                link.rigidBody.setKinematic(mode == Mode.Kinematic);
                if (mode == Mode.Ragdoll) {
                    Quaternion tmpRot1 = vars.quat1;
                    Vector3f position = vars.vect1;
                    //making sure that the ragdoll is at the correct place.
                    matchPhysicObjectToBone(link, position, tmpRot1);
                }

            }
            vars.release();
        }

        if(mode != Mode.IK){
            for (Bone bone : skeleton.getRoots()) {
                RagdollUtils.setUserControl(bone, mode == Mode.Ragdoll);
            }
        }
        
    }

    /**
     * Smoothly blend from Ragdoll mode to Kinematic mode This is useful to
     * blend ragdoll actual position to a keyframe animation for example
     *
     * @param blendTime the blending time between ragdoll to anim.
     */
    public void blendToKinematicMode(float blendTime) {
        if (mode == Mode.Kinematic) {
            return;
        }
        blendedControl = true;
        this.blendTime = blendTime;
        mode = Mode.Kinematic;
        AnimControl animControl = targetModel.getControl(AnimControl.class);
        animControl.setEnabled(true);


        TempVars vars = TempVars.get();
        for (PhysicsBoneLink link : boneLinks.values()) {

            Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
            Vector3f position = vars.vect1;

            targetModel.getWorldTransform().transformInverseVector(p, position);

            Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();
            Quaternion q2 = vars.quat1;
            Quaternion q3 = vars.quat2;

            q2.set(q).multLocal(link.initalWorldRotation).normalizeLocal();
            q3.set(targetModel.getWorldRotation()).inverseLocal().mult(q2, q2);
            q2.normalizeLocal();
            link.startBlendingPos.set(position);
            link.startBlendingRot.set(q2);
            link.rigidBody.setKinematic(true);
        }
        vars.release();

        for (Bone bone : skeleton.getRoots()) {
            RagdollUtils.setUserControl(bone, false);
        }

        blendStart = 0;
    }

    /**
     * Put the control into Kinematic mode. In this mode, the collision shapes
     * follow the movements of the skeleton while interacting with the physics
     * environment.
     */
    public void setKinematicMode() {
        if (mode != Mode.Kinematic) {
            setMode(Mode.Kinematic);
        }
    }

    /**
     * Sets the control into Ragdoll mode The skeleton is entirely controlled by
     * physics.
     */
    public void setRagdollMode() {
        if (mode != Mode.Ragdoll) {
            setMode(Mode.Ragdoll);
        }
    }

    /**
     * Sets the control into Inverse Kinematics mode. The affected bones are
     * affected by IK. physics.
     */
    public void setIKMode() {
        if (mode != Mode.IK) {
            setMode(Mode.IK);
        }
    }
    
    /**
     * returns the mode of this control
     *
     * @return an enum value
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Add a collision listener to this control.
     *
     * @param listener (not null, alias created)
     */
    public void addCollisionListener(RagdollCollisionListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<RagdollCollisionListener>();
        }
        listeners.add(listener);
    }

    /**
     * Alter the ragdoll's root mass.
     *
     * @param rootMass the desired mass (&ge;0)
     */
    public void setRootMass(float rootMass) {
        this.rootMass = rootMass;
    }

    /**
     * Read the ragdoll's total mass.
     *
     * @return mass (&ge;0)
     */
    public float getTotalMass() {
        return totalMass;
    }

    /**
     * Read the ragdoll's weight threshold.
     *
     * @return threshold
     */
    public float getWeightThreshold() {
        return weightThreshold;
    }

    /**
     * Alter the ragdoll's weight threshold.
     *
     * @param weightThreshold the desired threshold
     */
    public void setWeightThreshold(float weightThreshold) {
        this.weightThreshold = weightThreshold;
    }

    /**
     * Read the ragdoll's event-dispatch impulse threshold.
     *
     * @return threshold
     */
    public float getEventDispatchImpulseThreshold() {
        return eventDispatchImpulseThreshold;
    }

    /**
     * Alter the ragdoll's event-dispatch impulse threshold.
     *
     * @param eventDispatchImpulseThreshold desired threshold
     */
    public void setEventDispatchImpulseThreshold(float eventDispatchImpulseThreshold) {
        this.eventDispatchImpulseThreshold = eventDispatchImpulseThreshold;
    }

    /**
     * Alter the CcdMotionThreshold of all rigid bodies in the ragdoll.
     *
     * @see PhysicsRigidBody#setCcdMotionThreshold(float)
     * @param value the desired threshold value (velocity, &gt;0) or zero to
     * disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float value) {
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.rigidBody.setCcdMotionThreshold(value);
        }
    }

    /**
     * Alter the CcdSweptSphereRadius of all rigid bodies in the ragdoll.
     *
     * @see PhysicsRigidBody#setCcdSweptSphereRadius(float)
     * @param value the desired radius of the sphere used for continuous
     * collision detection (&ge;0)
     */
    public void setCcdSweptSphereRadius(float value) {
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.rigidBody.setCcdSweptSphereRadius(value);
        }
    }

    /**
     * Access the rigidBody associated with the named bone.
     *
     * @param boneName the name of the bone
     * @return the associated rigidBody.
     */
    public PhysicsRigidBody getBoneRigidBody(String boneName) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            return link.rigidBody;
        }
        return null;
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
     * Create a shallow clone for the JME cloner.
     *
     * @return a new control (not null)
     */
    @Override   
    public Object jmeClone() {
        KinematicRagdollControl control = new KinematicRagdollControl(preset, weightThreshold);        
        control.setMode(mode);
        control.setRootMass(rootMass);
        control.setWeightThreshold(weightThreshold);
        control.setApplyPhysicsLocal(applyLocal);
        control.spatial = this.spatial;
        return control;
    }     

    /**
     * Add a target for inverse kinematics.
     *
     * @param bone which bone the IK applies to (not null)
     * @param worldPos the world coordinates of the goal (not null)
     * @param chainLength number of bones in the chain
     * @return a new instance (not null, already added to ikTargets)
     */
    public Vector3f setIKTarget(Bone bone, Vector3f worldPos, int chainLength) {
        Vector3f target = worldPos.subtract(targetModel.getWorldTranslation());
        ikTargets.put(bone.getName(), target);
        ikChainDepth.put(bone.getName(), chainLength);
        int i = 0;
        while (i < chainLength+2 && bone.getParent() != null) {
            if (!bone.hasUserControl()) {
                bone.setUserControl(true);
            }
            bone = bone.getParent();
            i++;
        }


//        setIKMode();
        return target;
    }

    /**
     * Remove the inverse-kinematics target for the specified bone.
     *
     * @param bone which bone has the target (not null, modified)
     */
    public void removeIKTarget(Bone bone) {
        int depth = ikChainDepth.remove(bone.getName());
        int i = 0;
        while (i < depth+2 && bone.getParent() != null) {
            if (bone.hasUserControl()) {
//                matchPhysicObjectToBone(boneLinks.get(bone.getName()), position, tmpRot1);
                bone.setUserControl(false);
            }
            bone = bone.getParent();
            i++;
        }
    }
    
    /**
     * Remove all inverse-kinematics targets.
     */
    public void removeAllIKTargets(){
        ikTargets.clear();
        ikChainDepth.clear();
        applyUserControl();
    }

    /**
     * Ensure that user control is enabled for any bones used by inverse
     * kinematics and disabled for any other bones.
     */
    public void applyUserControl() {
        for (Bone bone : skeleton.getRoots()) {
            RagdollUtils.setUserControl(bone, false);
        }

        if (ikTargets.isEmpty()) {
            setKinematicMode();
        } else {
            Iterator iterator = ikTargets.keySet().iterator();

            TempVars vars = TempVars.get();

            while (iterator.hasNext()) {
                Bone bone = (Bone) iterator.next();
                while (bone.getParent() != null) {

                    Quaternion tmpRot1 = vars.quat1;
                    Vector3f position = vars.vect1;
                    matchPhysicObjectToBone(boneLinks.get(bone.getName()), position, tmpRot1);
                    bone.setUserControl(true);
                    bone = bone.getParent();
                }
            }
            vars.release();
        }
    }

    /**
     * Read the rotation speed for inverse kinematics.
     *
     * @return speed (&ge;0)
     */
    public float getIkRotSpeed() {
        return ikRotSpeed;
    }

    /**
     * Alter the rotation speed for inverse kinematics.
     *
     * @param ikRotSpeed the desired speed (&ge;0, default=7)
     */
    public void setIkRotSpeed(float ikRotSpeed) {
        this.ikRotSpeed = ikRotSpeed;
    }

    /**
     * Read the distance threshold for inverse kinematics.
     *
     * @return distance threshold
     */
    public float getIKThreshold() {
        return IKThreshold;
    }

    /**
     * Alter the distance threshold for inverse kinematics.
     *
     * @param IKThreshold the desired distance threshold (default=0.1)
     */
    public void setIKThreshold(float IKThreshold) {
        this.IKThreshold = IKThreshold;
    }

    /**
     * Read the limb damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getLimbDampening() {
        return limbDampening;
    }

    /**
     * Alter the limb damping.
     *
     * @param limbDampening the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=0.6)
     */
    public void setLimbDampening(float limbDampening) {
        this.limbDampening = limbDampening;
    }
    
    /**
     * Access the named bone.
     *
     * @param name which bone to access
     * @return the pre-existing instance, or null if not found
     */
    public Bone getBone(String name){
        return skeleton.getBone(name);
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
        oc.write(boneList.toArray(new String[boneList.size()]), "boneList", new String[0]);
        oc.write(boneLinks.values().toArray(new PhysicsBoneLink[boneLinks.size()]), "boneLinks", new PhysicsBoneLink[0]);
        oc.write(modelPosition, "modelPosition", new Vector3f());
        oc.write(modelRotation, "modelRotation", new Quaternion());
        oc.write(targetModel, "targetModel", null);
        oc.write(skeleton, "skeleton", null);
//        oc.write(preset, "preset", null);//TODO
        oc.write(initScale, "initScale", null);
        oc.write(mode, "mode", null);
        oc.write(blendedControl, "blendedControl", false);
        oc.write(weightThreshold, "weightThreshold", -1.0f);
        oc.write(blendStart, "blendStart", 0.0f);
        oc.write(blendTime, "blendTime", 1.0f);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold", 10);
        oc.write(rootMass, "rootMass", 15);
        oc.write(totalMass, "totalMass", 0);
        oc.write(ikRotSpeed, "rotSpeed", 7f);
        oc.write(limbDampening, "limbDampening", 0.6f);
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
        String[] loadedBoneList = ic.readStringArray("boneList", new String[0]);
        boneList.addAll(Arrays.asList(loadedBoneList));
        PhysicsBoneLink[] loadedBoneLinks = (PhysicsBoneLink[]) ic.readSavableArray("boneList", new PhysicsBoneLink[0]);
        for (PhysicsBoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.bone.getName(), physicsBoneLink);
        }
        modelPosition.set((Vector3f) ic.readSavable("modelPosition", new Vector3f()));
        modelRotation.set((Quaternion) ic.readSavable("modelRotation", new Quaternion()));
        targetModel = (Spatial) ic.readSavable("targetModel", null);
        skeleton = (Skeleton) ic.readSavable("skeleton", null);
//        preset //TODO
        initScale = (Vector3f) ic.readSavable("initScale", null);
        mode = ic.readEnum("mode", Mode.class, Mode.Kinematic);
        blendedControl = ic.readBoolean("blendedControl", false);
        weightThreshold = ic.readFloat("weightThreshold", -1.0f);
        blendStart = ic.readFloat("blendStart", 0.0f);
        blendTime = ic.readFloat("blendTime", 1.0f);
        eventDispatchImpulseThreshold = ic.readFloat("eventDispatchImpulseThreshold", 10);
        rootMass = ic.readFloat("rootMass", 15);
        totalMass = ic.readFloat("totalMass", 0);
    }
}
