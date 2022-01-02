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
package com.jme3.anim;

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.anim.util.JointModelTransform;
import com.jme3.export.*;
import com.jme3.material.MatParamOverride;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.shader.VarType;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Joint is the basic component of an armature designed to perform skeletal animation
 * Created by Nehon on 15/12/2017.
 */
public class Joint implements Savable, JmeCloneable, HasLocalTransform {

    private String name;
    private int id;
    private Joint parent;
    private SafeArrayList<Joint> children = new SafeArrayList<>(Joint.class);
    private Geometry targetGeometry;

    /**
     * The attachment node.
     */
    private Node attachedNode;

    /**
     * The transform of the joint in local space. Relative to its parent.
     * Or relative to the model's origin for the root joint.
     */
    private Transform localTransform = new Transform();

    /**
     * The initial transform of the joint in local space. Relative to its parent.
     * Or relative to the model's origin for the root joint.
     * this transform is the transform applied when the armature is loaded.
     */
    private Transform initialTransform = new Transform();

    /**
     * The transform of the joint in model space. Relative to the origin of the model.
     * this is either a MatrixJointModelTransform or a SeparateJointModelTransform
     */
    private JointModelTransform jointModelTransform;

    /**
     * The matrix used to transform affected vertices position into the joint model space.
     * Used for skinning.
     */
    private Matrix4f inverseModelBindMatrix = new Matrix4f();

    /**
     * Instantiate a nameless Joint.
     */
    public Joint() {
    }

    /**
     * Instantiate a Joint with the specified name.
     *
     * @param name the desired name
     */
    public Joint(String name) {
        this.name = name;
    }

    /**
     * Updates world transforms for this bone and its children.
     */
    public final void update() {
        this.updateModelTransforms();

        for (Joint child : children.getArray()) {
            child.update();
        }
    }

    /**
     * Updates the model transforms for this bone and for the attachments node
     * if not null.
     * <p>
     * The model transform of this bone is computed by combining the parent's
     * model transform with this bone's local transform.
     */
    public final void updateModelTransforms() {
        jointModelTransform.updateModelTransform(localTransform, parent);
        updateAttachNode();
    }

    /**
     * Update the local transform of the attachments node.
     */
    private void updateAttachNode() {
        if (attachedNode == null) {
            return;
        }
        Node attachParent = attachedNode.getParent();
        if (attachParent == null || targetGeometry == null
                || targetGeometry.getParent() == attachParent
                && targetGeometry.getLocalTransform().isIdentity()) {
            /*
             * The animated meshes are in the same coordinate system as the
             * attachments node: no further transforms are needed.
             */
            attachedNode.setLocalTransform(getModelTransform());

        } else {
            Spatial loopSpatial = targetGeometry;
            Transform combined = getModelTransform().clone();
            /*
             * Climb the scene graph applying local transforms until the
             * attachments node's parent is reached.
             */
            while (loopSpatial != attachParent && loopSpatial != null) {
                Transform localTransform = loopSpatial.getLocalTransform();
                combined.combineWithParent(localTransform);
                loopSpatial = loopSpatial.getParent();
            }
            attachedNode.setLocalTransform(combined);
        }
    }

    /**
     * Stores the skinning transform in the specified Matrix4f.
     * The skinning transform applies the animation of the bone to a vertex.
     * <p>
     * This assumes that the world transforms for the entire bone hierarchy
     * have already been computed, otherwise this method will return undefined
     * results.
     *
     * @param outTransform storage for the result (modified)
     */
    protected void getOffsetTransform(Matrix4f outTransform) {
        jointModelTransform.getOffsetTransform(outTransform, inverseModelBindMatrix);
    }

    /**
     * Sets the current localTransform as the Bind transform.
     */
    protected void saveBindPose() {
        //Note that the whole Armature must be updated before calling this method.
        getModelTransform().toTransformMatrix(inverseModelBindMatrix);
        inverseModelBindMatrix.invertLocal();
    }

    /**
     * Sets the current local transforms as the initial transform.
     */
    protected void saveInitialPose() {
        initialTransform.set(localTransform);
    }

    /**
     * Sets the local transform with the bind transforms
     */
    public void applyBindPose() {
        jointModelTransform.applyBindPose(localTransform, inverseModelBindMatrix, parent);
        updateModelTransforms();

        for (Joint child : children.getArray()) {
            child.applyBindPose();
        }
    }

    /**
     * Sets the local transform with the initial transform
     */
    protected void applyInitialPose() {
        setLocalTransform(initialTransform);
        updateModelTransforms();

        for (Joint child : children.getArray()) {
            child.applyInitialPose();
        }
    }

    /**
     * Access the accumulated model transform.
     *
     * @return the pre-existing instance
     */
    protected JointModelTransform getJointModelTransform() {
        return jointModelTransform;
    }

    /**
     * Replace the accumulated model transform.
     *
     * @param jointModelTransform the transform to use (alias created)
     */
    protected void setJointModelTransform(JointModelTransform jointModelTransform) {
        this.jointModelTransform = jointModelTransform;
    }

    /**
     * Access the local translation vector.
     *
     * @return the pre-existing vector
     */
    public Vector3f getLocalTranslation() {
        return localTransform.getTranslation();
    }

    /**
     * Access the local rotation.
     *
     * @return the pre-existing Quaternion
     */
    public Quaternion getLocalRotation() {
        return localTransform.getRotation();
    }

    /**
     * Access the local scale vector.
     *
     * @return the pre-existing vector
     */
    public Vector3f getLocalScale() {
        return localTransform.getScale();
    }

    /**
     * Alter the local translation vector.
     *
     * @param translation the desired offset vector (not null, unaffected)
     */
    public void setLocalTranslation(Vector3f translation) {
        localTransform.setTranslation(translation);
    }

    /**
     * Alter the local rotation.
     *
     * @param rotation the desired rotation (not null, unaffected)
     */
    public void setLocalRotation(Quaternion rotation) {
        localTransform.setRotation(rotation);
    }

    /**
     * Alter the local scale vector.
     *
     * @param scale the desired scale factors (not null, unaffected)
     */
    public void setLocalScale(Vector3f scale) {
        localTransform.setScale(scale);
    }

    /**
     * Add the specified Joint as a child.
     *
     * @param child the Joint to add (not null, modified)
     */
    public void addChild(Joint child) {
        children.add(child);
        child.parent = this;
    }

    /**
     * Alter the name.
     *
     * @param name the desired name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Alter the local transform.
     *
     * @param localTransform the desired Transform (not null, unaffected)
     */
    @Override
    public void setLocalTransform(Transform localTransform) {
        this.localTransform.set(localTransform);
    }

    /**
     * Replace the inverse model bind matrix.
     *
     * @param inverseModelBindMatrix the matrix to use (alias created)
     */
    public void setInverseModelBindMatrix(Matrix4f inverseModelBindMatrix) {
        this.inverseModelBindMatrix = inverseModelBindMatrix;
    }

    /**
     * Determine the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Access the parent joint.
     *
     * @return the pre-existing instance, or null if this is a root joint
     */
    public Joint getParent() {
        return parent;
    }

    /**
     * Access the list of child joints.
     *
     * @return the pre-existing list
     */
    public List<Joint> getChildren() {
        return children;
    }

    /**
     * Access the attachments node of this joint. If this joint doesn't already
     * have an attachments node, create one. Models and effects attached to the
     * attachments node will follow this bone's motions.
     *
     * @param jointIndex this bone's index in its armature (&ge;0)
     * @param targets    a list of geometries animated by this bone's skeleton (not
     *                   null, unaffected)
     * @return the attachments node (not null)
     */
    protected Node getAttachmentsNode(int jointIndex, SafeArrayList<Geometry> targets) {
        targetGeometry = null;
        /*
         * Search for a geometry animated by this particular bone.
         */
        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimatedByJoint(jointIndex)) {
                targetGeometry = geometry;
                break;
            }
        }

        if (attachedNode == null) {
            attachedNode = new Node(name + "_attachnode");
            attachedNode.setUserData("AttachedBone", this);
            //We don't want the node to have a numBone set by a parent node so we force it to null
            attachedNode.addMatParamOverride(new MatParamOverride(VarType.Int, "NumberOfBones", null));
        }

        return attachedNode;
    }

    /**
     * Access the initial transform.
     *
     * @return the pre-existing instance
     */
    public Transform getInitialTransform() {
        return initialTransform;
    }

    /**
     * Access the local transform.
     *
     * @return the pre-existing instance
     */
    @Override
    public Transform getLocalTransform() {
        return localTransform;
    }

    /**
     * Determine the model transform.
     *
     * @return a shared instance
     */
    public Transform getModelTransform() {
        return jointModelTransform.getModelTransform();
    }

    /**
     * Determine the inverse model bind matrix.
     *
     * @return the pre-existing instance
     */
    public Matrix4f getInverseModelBindMatrix() {
        return inverseModelBindMatrix;
    }

    /**
     * Determine this joint's index in the Armature that contains it.
     *
     * @return an index (&ge;0)
     */
    public int getId() {
        return id;
    }

    /**
     * Alter this joint's index in the Armature that contains it.
     *
     * @param id the desired index (&ge;0)
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public Object jmeClone() {
        try {
            Joint clone = (Joint) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned Joint into a deep-cloned one, using the specified Cloner
     * and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this Joint (not null)
     * @param original the instance from which this Joint was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.children = cloner.clone(children);
        this.parent = cloner.clone(parent);
        this.attachedNode = cloner.clone(attachedNode);
        this.targetGeometry = cloner.clone(targetGeometry);
        this.localTransform = cloner.clone(localTransform);
        this.initialTransform = cloner.clone(initialTransform);
        this.inverseModelBindMatrix = cloner.clone(inverseModelBindMatrix);
    }

    /**
     * De-serialize this Joint from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule input = im.getCapsule(this);

        name = input.readString("name", null);
        attachedNode = (Node) input.readSavable("attachedNode", null);
        targetGeometry = (Geometry) input.readSavable("targetGeometry", null);
        initialTransform = (Transform) input.readSavable("initialTransform", new Transform());
        inverseModelBindMatrix = (Matrix4f) input.readSavable("inverseModelBindMatrix", inverseModelBindMatrix);

        ArrayList<Joint> childList = input.readSavableArrayList("children", null);
        for (int i = childList.size() - 1; i >= 0; i--) {
            this.addChild(childList.get(i));
        }
    }

    /**
     * Serialize this Joint to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param ex the exporter to write to (not null)
     * @throws IOException from the exporter
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);

        output.write(name, "name", null);
        output.write(attachedNode, "attachedNode", null);
        output.write(targetGeometry, "targetGeometry", null);
        output.write(initialTransform, "initialTransform", new Transform());
        output.write(inverseModelBindMatrix, "inverseModelBindMatrix", new Matrix4f());
        output.writeSavableArrayList(new ArrayList(children), "children", null);
    }

}
