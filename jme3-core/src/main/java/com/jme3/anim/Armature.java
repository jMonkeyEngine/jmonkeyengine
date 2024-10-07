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

import com.jme3.anim.util.JointModelTransform;
import com.jme3.asset.AssetLoadException;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
            instantiateJointModelTransform(joint);
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
     * @param modelTransformClass which implementation to use
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
            instantiateJointModelTransform(joint);
        }
    }

    private void instantiateJointModelTransform(Joint joint) {
        try {
            joint.setJointModelTransform(modelTransformClass.getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
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
        OutputCapsule output = ex.getCapsule(this);
        output.write(rootJoints, "rootJoints", null);
        output.write(jointList, "jointList", null);
        output.write(modelTransformClass.getCanonicalName(), "modelTransformClass", MatrixJointModelTransform.class.getCanonicalName());
    }
}
