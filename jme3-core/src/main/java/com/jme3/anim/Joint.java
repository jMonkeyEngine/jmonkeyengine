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


    public Joint() {
    }

    public Joint(String name) {
        this.name = name;
    }

    /**
     * Updates world transforms for this bone and it's children.
     */
    public final void update() {
        this.updateModelTransforms();

        for (Joint child : children.getArray()) {
            child.update();
        }
    }

    /**
     * Updates the model transforms for this bone, and, possibly the attach node
     * if not null.
     * <p>
     * The model transform of this bone is computed by combining the parent's
     * model transform with this bones' local transform.
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
     * @param outTransform
     */
    void getOffsetTransform(Matrix4f outTransform) {
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

    protected JointModelTransform getJointModelTransform() {
        return jointModelTransform;
    }

    protected void setJointModelTransform(JointModelTransform jointModelTransform) {
        this.jointModelTransform = jointModelTransform;
    }

    public Vector3f getLocalTranslation() {
        return localTransform.getTranslation();
    }

    public Quaternion getLocalRotation() {
        return localTransform.getRotation();
    }

    public Vector3f getLocalScale() {
        return localTransform.getScale();
    }

    public void setLocalTranslation(Vector3f translation) {
        localTransform.setTranslation(translation);
    }

    public void setLocalRotation(Quaternion rotation) {
        localTransform.setRotation(rotation);
    }

    public void setLocalScale(Vector3f scale) {
        localTransform.setScale(scale);
    }

    public void addChild(Joint child) {
        children.add(child);
        child.parent = this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocalTransform(Transform localTransform) {
        this.localTransform.set(localTransform);
    }

    public void setInverseModelBindMatrix(Matrix4f inverseModelBindMatrix) {
        this.inverseModelBindMatrix = inverseModelBindMatrix;
    }

    public String getName() {
        return name;
    }

    public Joint getParent() {
        return parent;
    }

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
     */
    Node getAttachmentsNode(int jointIndex, SafeArrayList<Geometry> targets) {
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

    public Transform getInitialTransform() {
        return initialTransform;
    }

    public Transform getLocalTransform() {
        return localTransform;
    }

    public Transform getModelTransform() {
        return jointModelTransform.getModelTransform();
    }

    public Matrix4f getInverseModelBindMatrix() {
        return inverseModelBindMatrix;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Object jmeClone() {
        try {
            Joint clone = (Joint) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.children = cloner.clone(children);
        this.parent = cloner.clone(parent);
        this.attachedNode = cloner.clone(attachedNode);
        this.targetGeometry = cloner.clone(targetGeometry);
        this.localTransform = cloner.clone(localTransform);
        this.inverseModelBindMatrix = cloner.clone(inverseModelBindMatrix);
    }


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

    @Override
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
