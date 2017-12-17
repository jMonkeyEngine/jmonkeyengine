package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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


    /**
     * Serialization only
     */
    public Armature() {
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
            Joint b = jointList[i];
            if (b.getParent() == null) {
                rootJointList.add(b);
            }
        }
        rootJoints = rootJointList.toArray(new Joint[rootJointList.size()]);

        createSkinningMatrices();

        for (int i = rootJoints.length - 1; i >= 0; i--) {
            Joint rootJoint = rootJoints[i];
            rootJoint.update();
        }
    }

//
//    /**
//     * Special-purpose copy constructor.
//     * <p>
//     * Shallow copies bind pose data from the source skeleton, does not
//     * copy any other data.
//     *
//     * @param source The source Skeleton to copy from
//     */
//    public Armature(Armature source) {
//        Joint[] sourceList = source.jointList;
//        jointList = new Joint[sourceList.length];
//        for (int i = 0; i < sourceList.length; i++) {
//            jointList[i] = new Joint(sourceList[i]);
//        }
//
//        rootJoints = new Bone[source.rootJoints.length];
//        for (int i = 0; i < rootJoints.length; i++) {
//            rootJoints[i] = recreateBoneStructure(source.rootJoints[i]);
//        }
//        createSkinningMatrices();
//
//        for (int i = rootJoints.length - 1; i >= 0; i--) {
//            rootJoints[i].update();
//        }
//    }

    /**
     * Update all joints sin this Amature.
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
     * returns the array of all root joints of this Armatire
     *
     * @return
     */
    public Joint[] getRoots() {
        return rootJoints;
    }

    /**
     * return a joint for the given index
     *
     * @param index
     * @return
     */
    public Joint getJoint(int index) {
        return jointList[index];
    }

    /**
     * returns the joint with the given name
     *
     * @param name
     * @return
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
     * @return
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
     * @return
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
     * Saves the current Armature state as it's bind pose.
     */
    public void setBindPose() {
        //make sure all bones are updated
        update();
        //Save the current pose as bind pose
        for (Joint rootJoint : rootJoints) {
            rootJoint.setBindPose();
        }
    }

    /**
     * Compute the skining matrices for each bone of the armature that would be used to transform vertices of associated meshes
     *
     * @return
     */
    public Matrix4f[] computeSkinningMatrices() {
        TempVars vars = TempVars.get();
        for (int i = 0; i < jointList.length; i++) {
            jointList[i].getOffsetTransform(skinningMatrixes[i], vars.quat1, vars.vect1, vars.vect2, vars.tempMat3);
        }
        vars.release();
        return skinningMatrixes;
    }

    /**
     * returns the number of joints of this armature
     *
     * @return
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

        createSkinningMatrices();

        for (Joint rootJoint : rootJoints) {
            rootJoint.update();
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);
        output.write(rootJoints, "rootJoints", null);
        output.write(jointList, "jointList", null);
    }

}
