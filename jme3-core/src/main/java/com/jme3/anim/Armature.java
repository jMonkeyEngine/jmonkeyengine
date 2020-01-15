package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.asset.AssetLoadException;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.*;

/**
 * Created by Nehon on 15/12/2017.
 */
public class Armature implements JmeCloneable, Savable {

    private Joint[] rootJoints;
    private Joint[] jointList;

    /**
     * Contains the skinning matrices, multiplying it by a vertex effected by a bone
     * will cause it to go to the animated position.
     */
    private transient Matrix4f[] skinningMatrixes;
    private Class<? extends JointModelTransform> modelTransformClass = SeparateJointModelTransform.class;

    /**
     * Serialization only
     */
    protected Armature() {
    }

    /**
     * Creates an armature from a joint list.
     * The root joints are found automatically.
     * <p>
     * Note that using this constructor will cause the joints in the list
     * to have their bind pose recomputed based on their local transforms.
     *
     * @param jointList The list of joints to manage by this Armature
     */
    public Armature(Joint[] jointList) {
        this.jointList = jointList;

        List<Joint> rootJointList = new ArrayList<>();
        for (int i = jointList.length - 1; i >= 0; i--) {
            Joint joint = jointList[i];
            joint.setId(i);
            instanciateJointModelTransform(joint);
            if (joint.getParent() == null) {
                rootJointList.add(joint);
            }
        }
        rootJoints = rootJointList.toArray(new Joint[rootJointList.size()]);

        createSkinningMatrices();

        for (int i = rootJoints.length - 1; i >= 0; i--) {
            Joint rootJoint = rootJoints[i];
            rootJoint.update();
        }
    }

    /**
     * Update all joints in this Armature.
     */
    public void update() {
        for (Joint rootJoint : rootJoints) {
            rootJoint.update();
        }
    }

    private void createSkinningMatrices() {
        skinningMatrixes = new Matrix4f[jointList.length];
        for (int i = 0; i < skinningMatrixes.length; i++) {
            skinningMatrixes[i] = new Matrix4f();
        }
    }

    /**
     * Sets the JointModelTransform implementation
     * Default is {@link MatrixJointModelTransform}
     *
     * @param modelTransformClass
     * @see JointModelTransform
     * @see MatrixJointModelTransform
     * @see SeparateJointModelTransform
     */
    public void setModelTransformClass(Class<? extends JointModelTransform> modelTransformClass) {
        this.modelTransformClass = modelTransformClass;
        if (jointList == null) {
            return;
        }
        for (Joint joint : jointList) {
            instanciateJointModelTransform(joint);
        }
    }

    private void instanciateJointModelTransform(Joint joint) {
        try {
            joint.setJointModelTransform(modelTransformClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * returns the array of all root joints of this Armature
     *
     * @return the pre-existing array
     */
    public Joint[] getRoots() {
        return rootJoints;
    }

    public List<Joint> getJointList() {
        return Arrays.asList(jointList);
    }

    /**
     * return a joint for the given index
     *
     * @param index
     * @return the pre-existing instance
     */
    public Joint getJoint(int index) {
        return jointList[index];
    }

    /**
     * returns the joint with the given name
     *
     * @param name
     * @return the pre-existing instance or null if not found
     */
    public Joint getJoint(String name) {
        for (int i = 0; i < jointList.length; i++) {
            if (jointList[i].getName().equals(name)) {
                return jointList[i];
            }
        }
        return null;
    }

    /**
     * returns the bone index of the given bone
     *
     * @param joint
     * @return the index (&ge;0) or -1 if not found
     */
    public int getJointIndex(Joint joint) {
        for (int i = 0; i < jointList.length; i++) {
            if (jointList[i] == joint) {
                return i;
            }
        }

        return -1;
    }

    /**
     * returns the joint index of the joint that has the given name
     *
     * @param name
     * @return the index (&ge;0) or -1 if not found
     */
    public int getJointIndex(String name) {
        for (int i = 0; i < jointList.length; i++) {
            if (jointList[i].getName().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Saves the current Armature state as its bind pose.
     * Note that the bind pose is supposed to be the one where the armature is aligned with the mesh to deform.
     * Saving this pose will affect how skinning works.
     */
    public void saveBindPose() {
        //make sure all bones are updated
        update();
        //Save the current pose as bind pose
        for (Joint joint : jointList) {
            joint.saveBindPose();
        }
    }

    /**
     * This methods sets this armature in its bind pose (aligned with the mesh to deform)
     * Note that this is only useful for debugging purpose.
     */
    public void applyBindPose() {
        for (Joint joint : rootJoints) {
            joint.applyBindPose();
        }
    }

    /**
     * Saves the current local transform as the initial transform.
     * Initial transform is the one applied to the armature when loaded.
     */
    public void saveInitialPose() {
        for (Joint joint : jointList) {
            joint.saveInitialPose();
        }
    }

    /**
     * Applies the initial pose to this armature
     */
    public void applyInitialPose() {
        for (Joint rootJoint : rootJoints) {
            rootJoint.applyInitialPose();
        }
    }

    /**
     * Compute the skinning matrices for each bone of the armature that would be used to transform vertices of associated meshes
     *
     * @return the pre-existing array
     */
    public Matrix4f[] computeSkinningMatrices() {
        for (int i = 0; i < jointList.length; i++) {
            jointList[i].getOffsetTransform(skinningMatrixes[i]);
        }
        return skinningMatrixes;
    }

    /**
     * returns the number of joints of this armature
     *
     * @return the count (&ge;0)
     */
    public int getJointCount() {
        return jointList.length;
    }

    @Override
    public Object jmeClone() {
        try {
            Armature clone = (Armature) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.rootJoints = cloner.clone(rootJoints);
        this.jointList = cloner.clone(jointList);
        this.skinningMatrixes = cloner.clone(skinningMatrixes);
        for (Joint joint : jointList) {
            instanciateJointModelTransform(joint);
        }
    }


    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule input = im.getCapsule(this);

        Savable[] jointRootsAsSavable = input.readSavableArray("rootJoints", null);
        rootJoints = new Joint[jointRootsAsSavable.length];
        System.arraycopy(jointRootsAsSavable, 0, rootJoints, 0, jointRootsAsSavable.length);

        Savable[] jointListAsSavable = input.readSavableArray("jointList", null);
        jointList = new Joint[jointListAsSavable.length];
        System.arraycopy(jointListAsSavable, 0, jointList, 0, jointListAsSavable.length);

        String className = input.readString("modelTransformClass", MatrixJointModelTransform.class.getCanonicalName());
        try {
            modelTransformClass = (Class<? extends JointModelTransform>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssetLoadException("Cannot find class for name " + className);
        }

        int i = 0;
        for (Joint joint : jointList) {
            joint.setId(i++);
            instanciateJointModelTransform(joint);
        }
        createSkinningMatrices();

        for (Joint rootJoint : rootJoints) {
            rootJoint.update();
        }
        applyInitialPose();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);
        output.write(rootJoints, "rootJoints", null);
        output.write(jointList, "jointList", null);
        output.write(modelTransformClass.getCanonicalName(), "modelTransformClass", MatrixJointModelTransform.class.getCanonicalName());
    }
}
