/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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

import com.jme3.anim.util.JointModelTransform;
import com.jme3.asset.AssetLoadException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Matrix4f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An `Armature` represents a skeletal structure composed of {@link Joint} objects.
 * It manages the hierarchy of joints, their transformations, and provides methods
 * for updating the armature's pose and computing skinning matrices.
 *
 * @author Nehon
 */
public class Armature implements JmeCloneable, Savable {

    /**
     * The array of root joints in this armature. These are joints that have no parent.
     */
    private Joint[] rootJoints;
    /**
     * A flat list of all joints managed by this armature, indexed by their ID.
     */
    private Joint[] jointList;
    /**
     * Contains the skinning matrices. Multiplying a vertex affected by a joint
     * by its corresponding skinning matrix will transform the vertex to the
     * animated position.
     */
    private transient Matrix4f[] skinningMatrices;
    /**
     * The class used to instantiate {@link JointModelTransform} instances for each joint.
     * Defaults to {@link SeparateJointModelTransform}. This allows for customization
     * of how joint model transformations are handled (e.g., separate scale/rotation/translation
     * or combined in a single matrix).
     */
    private Class<? extends JointModelTransform> modelTransformClass = SeparateJointModelTransform.class;

    /**
     * Serialization only
     */
    protected Armature() {
    }

    /**
     * Creates an `Armature` from a given list of joints.
     * The root joints are automatically identified based on their parent-child relationships.
     * Each joint is assigned an ID corresponding to its index in the `jointList`.
     * <p>
     * Note that calling this constructor will cause the bind pose of the joints
     * in the list to be recomputed based on their initial local transforms.
     * The initial pose is also applied after construction.
     * </p>
     *
     * @param jointList An array of {@link Joint} objects that will be managed by this `Armature`.
     */
    public Armature(Joint[] jointList) {
        this.jointList = jointList;

        List<Joint> rootJointList = new ArrayList<>();
        for (int i = jointList.length - 1; i >= 0; i--) {
            Joint joint = jointList[i];
            joint.setId(i);
            instantiateJointModelTransform(joint);
            if (joint.getParent() == null) {
                rootJointList.add(joint);
            }
        }
        rootJoints = rootJointList.toArray(new Joint[0]);

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

    /**
     * Initializes or re-initializes the array of skinning matrices.
     * Each matrix in this array corresponds to a joint in the `jointList`
     * and is used to transform vertices affected by that joint into their
     * animated position.
     */
    private void createSkinningMatrices() {
        skinningMatrices = new Matrix4f[jointList.length];
        for (int i = 0; i < skinningMatrices.length; i++) {
            skinningMatrices[i] = new Matrix4f();
        }
    }

    /**
     * Sets the {@link JointModelTransform} implementation to be used by all joints
     * in this `Armature`. This allows customizing how joint transformations
     * (scale, rotation, translation) are managed internally.
     * <p>
     * By default, {@link SeparateJointModelTransform} is used.
     *
     * @param aClass The {@link Class} object representing the desired
     *                            {@link JointModelTransform} implementation.
     * @see JointModelTransform
     * @see MatrixJointModelTransform
     * @see SeparateJointModelTransform
     */
    public void setModelTransformClass(Class<? extends JointModelTransform> aClass) {
        this.modelTransformClass = aClass;
        // Only re-instantiate if jointList is already populated
        if (jointList != null) {
            for (Joint joint : jointList) {
                instantiateJointModelTransform(joint);
            }
        }
    }

    /**
     * Instantiates a new {@link JointModelTransform} object of the type
     * specified by `modelTransformClass` and sets it on the given joint.
     *
     * @param joint The {@link Joint} for which to instantiate and set the
     * {@link JointModelTransform}.
     */
    private void instantiateJointModelTransform(Joint joint) {
        try {
            JointModelTransform transform = modelTransformClass.getDeclaredConstructor().newInstance();
            joint.setJointModelTransform(transform);

        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException ex) {
            throw new IllegalArgumentException("Failed to instantiate JointModelTransform for joint: " + joint.getName(), ex);
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

    /**
     * Access all joints in this Armature.
     *
     * @return a new list of pre-existing joints
     */
    public List<Joint> getJointList() {
        return Arrays.asList(jointList);
    }

    /**
     * return a joint for the given index
     *
     * @param index a zero-based joint index (&ge;0)
     * @return the pre-existing instance
     */
    public Joint getJoint(int index) {
        return jointList[index];
    }

    /**
     * returns the joint with the given name
     *
     * @param name the name to search for
     * @return the pre-existing instance or null if not found
     */
    public Joint getJoint(String name) {
        for (Joint joint : jointList) {
            if (joint.getName().equals(name)) {
                return joint;
            }
        }
        return null;
    }

    /**
     * returns the bone index of the given bone
     *
     * @param joint the Joint to search for
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
     * @param name the name to search for
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
     * This method sets this armature to its bind pose (aligned with the mesh to deform).
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
     * Computes the skinning matrices for each joint in the `Armature`.
     * These matrices are essential for skinning (vertex deformation), as they
     * transform vertices from model space to the animated joint's space.
     *
     * @return the pre-existing array
     */
    public Matrix4f[] computeSkinningMatrices() {
        for (int i = 0; i < jointList.length; i++) {
            jointList[i].getOffsetTransform(skinningMatrices[i]);
        }
        return skinningMatrices;
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
        this.skinningMatrices = cloner.clone(skinningMatrices);
        for (Joint joint : jointList) {
            instantiateJointModelTransform(joint);
        }
    }

    /**
     * De-serialize this Armature from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to read from (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        Savable[] rootJointsSavable = ic.readSavableArray("rootJoints", null);
        rootJoints = new Joint[rootJointsSavable.length];
        System.arraycopy(rootJointsSavable, 0, rootJoints, 0, rootJointsSavable.length);

        Savable[] jointListSavable = ic.readSavableArray("jointList", null);
        jointList = new Joint[jointListSavable.length];
        System.arraycopy(jointListSavable, 0, jointList, 0, jointListSavable.length);

        String className = ic.readString("modelTransformClass", MatrixJointModelTransform.class.getCanonicalName());
        try {
            modelTransformClass = (Class<? extends JointModelTransform>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssetLoadException("Cannot find class for name '" + className + "' specified for JointModelTransform.", e);
        }

        int i = 0;
        for (Joint joint : jointList) {
            joint.setId(i++);
            instantiateJointModelTransform(joint);
        }
        createSkinningMatrices();

        for (Joint rootJoint : rootJoints) {
            rootJoint.update();
        }
        applyInitialPose();
    }

    /**
     * Serialize this Armature to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param ex the exporter to write to (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(rootJoints, "rootJoints", null);
        oc.write(jointList, "jointList", null);
        oc.write(modelTransformClass.getCanonicalName(), "modelTransformClass", MatrixJointModelTransform.class.getCanonicalName());
    }
}
