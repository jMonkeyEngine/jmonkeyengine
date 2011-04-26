/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.animation.AnimChannel;
import com.jme3.bullet.control.ragdoll.RagdollPreset;
import com.jme3.bullet.control.ragdoll.HumanoidRagdollPreset;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class RagdollControl implements PhysicsControl, PhysicsCollisionListener {

    protected static final Logger logger = Logger.getLogger(RagdollControl.class.getName());
    protected Map<String, PhysicsBoneLink> boneLinks = new HashMap<String, PhysicsBoneLink>();
    protected Skeleton skeleton;
    protected PhysicsSpace space;
    protected boolean enabled = true;
    protected boolean debug = false;
    protected PhysicsRigidBody baseRigidBody;
    protected float weightThreshold = 1.0f;
    protected Spatial targetModel;
    protected Vector3f initScale;
    protected boolean control = false;
    protected boolean blendedControl = false;
    protected float blendTime = 1.0f;
    protected float blendStart = 0.0f;
    protected List<RagdollCollisionListener> listeners;
    protected float eventDispatchImpulseThreshold = 10;
    protected float eventDiscardImpulseThreshold = 3;
    protected RagdollPreset preset = new HumanoidRagdollPreset();
    protected List<String> boneList = new LinkedList<String>();
    protected Vector3f modelPosition = new Vector3f();
    protected Quaternion modelRotation = new Quaternion();
    protected float rootMass = 15;
    private float totalMass = 0;

    public RagdollControl() {
    }

    public RagdollControl(float weightThreshold) {
        this.weightThreshold = weightThreshold;
    }

    public RagdollControl(RagdollPreset preset, float weightThreshold) {
        this.preset = preset;
        this.weightThreshold = weightThreshold;
    }

    public RagdollControl(RagdollPreset preset) {
        this.preset = preset;
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        TempVars vars = TempVars.get();
        assert vars.lock();
        Quaternion q2 = vars.quat1;
        Quaternion q3 = vars.quat2;

        if (control) {


            for (PhysicsBoneLink link : boneLinks.values()) {

                Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
                Vector3f position = vars.vect1;

                targetModel.getWorldTransform().transformInverseVector(p, position);

                Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();

                q2.set(q).multLocal(link.initalWorldRotation).normalize();
                q3.set(targetModel.getWorldRotation()).inverseLocal().mult(q2, q2);
                q2.normalize();
                if (link.bone.getParent() == null) {
                    modelPosition.set(p).subtractLocal(link.bone.getInitialPos());
                    modelRotation.set(q).multLocal(link.bone.getInitialRot().inverse());
                    targetModel.setLocalTranslation(modelPosition);
                    targetModel.setLocalRotation(modelRotation);
                    link.bone.setUserControl(true);
                    link.bone.setUserTransformsWorld(position, q2);

                } else {
                    if (boneList.isEmpty()) {
                        link.bone.setUserControl(true);
                        link.bone.setUserTransformsWorld(position, q2);
                    } else {
                        setTransform(link.bone, position, q2);
                    }
                }
            }
        } else {


            for (PhysicsBoneLink link : boneLinks.values()) {
                //the ragdoll does not control the skeleton
                link.bone.setUserControl(false);
                if (!link.rigidBody.isKinematic()) {
                    link.rigidBody.setKinematic(true);
                }

                if (blendedControl) {



                    Vector3f position = vars.vect1.set(link.startBlendingPos);
                    Vector3f position2 = vars.vect2;

                    q2.set(link.startBlendingRot);

                    q3.set(q2).slerp(link.bone.getModelSpaceRotation(), blendStart / blendTime);
                    position2.set(position).interpolate(link.bone.getModelSpacePosition(), blendStart / blendTime);
                    q2.set(q3);
                    position.set(position2);
                    if (boneList.isEmpty()) {
                        link.bone.setUserControl(true);
                        link.bone.setUserTransformsWorld(position, q2);
                    } else {
                        setTransform(link.bone, position, q2);
                    }

                }





                Vector3f position = vars.vect1;
                Quaternion rotation = vars.quat1;

                //computing position from rotation and scale
                targetModel.getWorldTransform().transformVector(link.bone.getModelSpacePosition(), position);

                //computing rotation
                rotation.set(link.bone.getModelSpaceRotation()).multLocal(link.bone.getWorldBindInverseRotation());
                targetModel.getWorldRotation().mult(rotation, rotation);
                rotation.normalize();

                link.rigidBody.setPhysicsLocation(position);
                link.rigidBody.setPhysicsRotation(rotation);
            }
            if (blendedControl) {
                blendStart += tpf;

                if (blendStart > blendTime) {
                    blendedControl = false;

                }
            }
        }


        assert vars.unlock();

    }

    private void setTransform(Bone bone, Vector3f pos, Quaternion rot) {
        bone.setUserControl(true);
        bone.setUserTransformsWorld(pos, rot);
        for (Bone childBone : bone.getChildren()) {
            if (!boneList.contains(childBone.getName())) {
                Transform t = childBone.getCombinedTransform(pos, rot);
                setTransform(childBone, t.getTranslation(), t.getRotation());
            }
        }
        bone.setUserControl(false);
    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSpatial(Spatial model) {
        targetModel = model;
        Node parent = model.getParent();


        Vector3f initPosition = model.getLocalTranslation().clone();
        Quaternion initRotation = model.getLocalRotation().clone();
        initScale = model.getLocalScale().clone();

        model.removeFromParent();
        model.setLocalTranslation(Vector3f.ZERO);
        model.setLocalRotation(Quaternion.ZERO);
        model.setLocalScale(1);
        //HACK ALERT change this
        //I remove the skeletonControl and readd it to the spatial to make sure it's after the ragdollControl in the stack
        //Find a proper way to order the controls.
        SkeletonControl sc = model.getControl(SkeletonControl.class);
        model.removeControl(sc);
        model.addControl(sc);
        //---- 

        removeFromPhysicsSpace();
        clearData();
        // put into bind pose and compute bone transforms in model space
        // maybe dont reset to ragdoll out of animations?
        scanSpatial(model);


        if (parent != null) {
            parent.attachChild(model);

        }
        model.setLocalTranslation(initPosition);
        model.setLocalRotation(initRotation);
        model.setLocalScale(initScale);

        logger.log(Level.INFO, "Create physics ragdoll for skeleton {0}", skeleton);
    }

    public void addBoneName(String name) {
        boneList.add(name);
    }

    private void scanSpatial(Spatial model) {
        AnimControl animControl = model.getControl(AnimControl.class);

        skeleton = animControl.getSkeleton();
        skeleton.resetAndUpdate();
        for (int i = 0; i < skeleton.getRoots().length; i++) {
            Bone childBone = skeleton.getRoots()[i];
            if (childBone.getParent() == null) {
                logger.log(Level.INFO, "Found root bone in skeleton {0}", skeleton);
                baseRigidBody = new PhysicsRigidBody(new BoxCollisionShape(Vector3f.UNIT_XYZ.mult(0.1f)), 1);
                boneRecursion(model, childBone, baseRigidBody, 1);
            }
        }


    }

    private void boneRecursion(Spatial model, Bone bone, PhysicsRigidBody parent, int reccount) {
        PhysicsRigidBody parentShape = parent;
        if (boneList.isEmpty() || boneList.contains(bone.getName())) {

            PhysicsBoneLink link = new PhysicsBoneLink();
            link.bone = bone;
            //creating the collision shape from the bone's associated vertices
            PhysicsRigidBody shapeNode = new PhysicsRigidBody(makeShape(link, model), rootMass / (float) reccount);
            totalMass += rootMass / (float) reccount;

            link.rigidBody = shapeNode;
            link.initalWorldRotation = bone.getModelSpaceRotation().clone();

            if (parent != null) {
                //get joint position for parent
                Vector3f posToParent = new Vector3f();
                if (bone.getParent() != null) {
                    bone.getModelSpacePosition().subtract(bone.getParent().getModelSpacePosition(), posToParent).multLocal(initScale);
                }

                //Joint local position from parent
                link.pivotA = posToParent;
                //joint local position from current bone
                link.pivotB = new Vector3f(0, 0, 0f);

                SixDofJoint joint = new SixDofJoint(parent, shapeNode, link.pivotA, link.pivotB, true);
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
            boneRecursion(model, childBone, parentShape, reccount + 1);
        }


    }

    /**
     * Set the joint limits for the joint between the given bone and its parent.
     * This method can't work before attaching the control to a spatial
     * @param boneName the name of the bone
     * @param maxX the maximum rotation on the x axis (in radians)
     * @param minX the minimum rotation on the x axis (in radians)
     * @param maxY the maximum rotation on the y axis (in radians)
     * @param minY the minimum rotation on the z axis (in radians)
     * @param maxZ the maximum rotation on the z axis (in radians)
     * @param minZ the minimum rotation on the z axis (in radians)
     */
    public void setJointLimit(String boneName, float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            setJointLimit(link.joint, maxX, minX, maxY, minY, maxZ, minZ);
        } else {
            logger.log(Level.WARNING, "Not joint was found for bone {0}. make sure you call spatial.addControl(ragdoll) before setting joints limit", boneName);
        }
    }

    /**
     * Return the joint between the given bone and its parent.
     * This return null if it's called before attaching the control to a spatial
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

    private void setJointLimit(SixDofJoint joint, float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {

        joint.getRotationalLimitMotor(0).setHiLimit(maxX);
        joint.getRotationalLimitMotor(0).setLoLimit(minX);
        joint.getRotationalLimitMotor(1).setHiLimit(maxY);
        joint.getRotationalLimitMotor(1).setLoLimit(minY);
        joint.getRotationalLimitMotor(2).setHiLimit(maxZ);
        joint.getRotationalLimitMotor(2).setLoLimit(minZ);
    }

    private void clearData() {
        boneLinks.clear();
        baseRigidBody = null;
    }

    private void addToPhysicsSpace() {
        if (baseRigidBody != null) {
            space.add(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.rigidBody != null) {
                space.add(physicsBoneLink.rigidBody);
            }
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.joint != null) {
                space.add(physicsBoneLink.joint);
            }
        }
    }

    private HullCollisionShape makeShape(PhysicsBoneLink link, Spatial model) {
        Bone bone = link.bone;
        List<Integer> boneIndices = null;
        if (boneList.isEmpty()) {
            boneIndices = new LinkedList<Integer>();
            boneIndices.add(skeleton.getBoneIndex(bone));
        } else {
            boneIndices = getBoneIndices(bone, skeleton);
        }


        ArrayList<Float> points = new ArrayList<Float>();
        if (model instanceof Geometry) {
            Geometry g = (Geometry) model;
            for (Integer index : boneIndices) {
                points.addAll(getPoints(g.getMesh(), index, bone.getModelSpacePosition(), link));
            }
        } else if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial s : node.getChildren()) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry) s;
                    for (Integer index : boneIndices) {
                        points.addAll(getPoints(g.getMesh(), index, bone.getModelSpacePosition(), link));
                    }

                }
            }
        }
        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }

        return new HullCollisionShape(p);
    }

    private List<Integer> getBoneIndices(Bone bone, Skeleton skeleton) {
        List<Integer> list = new LinkedList<Integer>();
        list.add(skeleton.getBoneIndex(bone));
        for (Bone chilBone : bone.getChildren()) {
            if (!boneList.contains(chilBone.getName())) {
                list.addAll(getBoneIndices(chilBone, skeleton));
            }
        }
        return list;
    }

    protected List<Float> getPoints(Mesh mesh, int boneIndex, Vector3f offset, PhysicsBoneLink link) {

        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        ByteBuffer boneIndices = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
        FloatBuffer boneWeight = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        vertices.rewind();
        boneIndices.rewind();
        boneWeight.rewind();

        ArrayList<Float> results = new ArrayList<Float>();

        int vertexComponents = mesh.getVertexCount() * 3;

        for (int i = 0; i < vertexComponents; i += 3) {
            int k;
            boolean add = false;
            int start = i / 3 * 4;
            for (k = start; k < start + 4; k++) {
                if (boneIndices.get(k) == boneIndex && boneWeight.get(k) >= weightThreshold) {
                    add = true;
                    break;
                }
            }
            if (add) {

                Vector3f pos = new Vector3f();
                pos.x = vertices.get(i);
                pos.y = vertices.get(i + 1);
                pos.z = vertices.get(i + 2);
                pos.subtractLocal(offset).multLocal(initScale);
                results.add(pos.x);
                results.add(pos.y);
                results.add(pos.z);

            }
        }

        return results;
    }

    private void removeFromPhysicsSpace() {
        if (baseRigidBody != null) {
            space.remove(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.joint != null) {
                space.remove(physicsBoneLink.joint);
            }
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.rigidBody != null) {
                space.remove(physicsBoneLink.rigidBody);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (!enabled && space != null) {
            removeFromPhysicsSpace();
        } else if (enabled && space != null) {
            addToPhysicsSpace();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void attachDebugShape(AssetManager manager) {
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            physicsBoneLink.rigidBody.attachDebugShape(manager);
        }
        debug = true;
    }

    public void detachDebugShape() {
        for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            physicsBoneLink.rigidBody.detachDebugShape();
        }
        debug = false;
    }

    public void render(RenderManager rm, ViewPort vp) {
        if (enabled && space != null && space.getDebugManager() != null) {
            if (!debug) {
                attachDebugShape(space.getDebugManager());
            }
            for (Iterator<PhysicsBoneLink> it = boneLinks.values().iterator(); it.hasNext();) {
                PhysicsBoneLink physicsBoneLink = it.next();
                Spatial debugShape = physicsBoneLink.rigidBody.debugShape();
                if (debugShape != null) {
                    debugShape.setLocalTranslation(physicsBoneLink.rigidBody.getMotionState().getWorldLocation());
                    debugShape.setLocalRotation(physicsBoneLink.rigidBody.getMotionState().getWorldRotationQuat());
                    debugShape.updateGeometricState();
                    rm.renderScene(debugShape, vp);
                }
            }
        }
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        if (space == null) {
            removeFromPhysicsSpace();
            this.space = space;
        } else {
            if (this.space == space) {
                return;
            }
            this.space = space;
            addToPhysicsSpace();
        }
        this.space.addCollisionListener(this);
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void collision(PhysicsCollisionEvent event) {
        PhysicsCollisionObject objA = event.getObjectA();
        PhysicsCollisionObject objB = event.getObjectB();

        if (event.getNodeA() == null && event.getNodeB() == null) {
            return;
        }

        if (event.getAppliedImpulse() < eventDiscardImpulseThreshold) {
            return;
        }

        boolean hit = false;
        Bone hitBone = null;
        PhysicsCollisionObject hitObject = null;


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

        if (hit && event.getAppliedImpulse() > eventDispatchImpulseThreshold) {
            for (RagdollCollisionListener listener : listeners) {
                listener.collide(hitBone, hitObject, event);
            }
        }

    }

    public void setControl(boolean control) {

        AnimControl animControl = targetModel.getControl(AnimControl.class);
        animControl.setEnabled(!control);

        this.control = control;
        for (PhysicsBoneLink link : boneLinks.values()) {
            //  link.bone.setUserControl(control);
            link.rigidBody.setKinematic(!control);
        }


        for (Bone bone : skeleton.getRoots()) {
            setUserControl(bone, control);
        }

    }

    public void blendControlToAnim(String anim, AnimChannel channel) {
        blendedControl = true;
        control = false;
        AnimControl animControl = targetModel.getControl(AnimControl.class);
        animControl.setEnabled(true);
        channel.setAnim(anim);
        channel.setLoopMode(LoopMode.DontLoop);

        TempVars vars = TempVars.get();
        assert vars.lock();
        //this.control = control;
        for (PhysicsBoneLink link : boneLinks.values()) {

            Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
            Vector3f position = vars.vect1;

            targetModel.getWorldTransform().transformInverseVector(p, position);

            Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();
            Quaternion q2 = vars.quat1;
            Quaternion q3 = vars.quat2;

            q2.set(q).multLocal(link.initalWorldRotation).normalize();
            q3.set(targetModel.getWorldRotation()).inverseLocal().mult(q2, q2);
            q2.normalize();
            link.startBlendingPos.set(position);
            link.startBlendingRot.set(q2);
            link.rigidBody.setKinematic(true);
        }
        assert vars.unlock();

        for (Bone bone : skeleton.getRoots()) {
            setUserControl(bone, false);
        }

        blendStart = 0;
    }

    private void setUserControl(Bone bone, boolean bool) {
        bone.setUserControl(bool);
        for (Bone child : bone.getChildren()) {
            setUserControl(child, bool);
        }
    }

    public boolean hasControl() {

        return control;

    }

    public void addCollisionListener(RagdollCollisionListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<RagdollCollisionListener>();
        }
        listeners.add(listener);
    }

    protected static class PhysicsBoneLink {

        Bone bone;
        Quaternion initalWorldRotation;
        SixDofJoint joint;
        PhysicsRigidBody rigidBody;
        Vector3f pivotA;
        Vector3f pivotB;
        Quaternion startBlendingRot = new Quaternion();
        Vector3f startBlendingPos = new Vector3f();
    }

    public void setRootMass(float rootMass) {
        this.rootMass = rootMass;
    }

    public float getTotalMass() {
        return totalMass;
    }

    public float getWeightThreshold() {
        return weightThreshold;
    }

    public void setWeightThreshold(float weightThreshold) {
        this.weightThreshold = weightThreshold;
    }

    public float getEventDiscardImpulseThreshold() {
        return eventDiscardImpulseThreshold;
    }

    public void setEventDiscardImpulseThreshold(float eventDiscardImpulseThreshold) {
        this.eventDiscardImpulseThreshold = eventDiscardImpulseThreshold;
    }

    public float getEventDispatchImpulseThreshold() {
        return eventDispatchImpulseThreshold;
    }

    public void setEventDispatchImpulseThreshold(float eventDispatchImpulseThreshold) {
        this.eventDispatchImpulseThreshold = eventDispatchImpulseThreshold;
    }
}
