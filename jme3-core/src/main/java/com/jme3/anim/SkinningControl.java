/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.material.MatParamOverride;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.GlVertexBuffer.Type;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Skinning control deforms a model according to an armature, It handles the
 * computation of the deformation matrices and performs the transformations on
 * the mesh
 * <p>
 * It can perform software skinning or Hardware skinning
 *
 * @author Rémy Bouquet Based on SkeletonControl by Kirill Vainer
 */
public class SkinningControl extends AbstractControl implements Cloneable, JmeCloneable {

    private static final Logger logger = Logger.getLogger(SkinningControl.class.getName());

    /**
     * The armature of the model.
     */
    private Armature armature;

    /**
     * List of geometries affected by this control.
     */
    private SafeArrayList<Geometry> targets = new SafeArrayList<>(Geometry.class);

    /**
     * Used to track when a mesh was updated. Meshes are only updated if they
     * are visible in at least one camera.
     */
    private boolean wasMeshUpdated = false;

    /**
     * User wishes to use hardware skinning if available.
     */
    private transient boolean hwSkinningDesired = true;

    /**
     * Hardware skinning is currently being used.
     */
    private transient boolean hwSkinningEnabled = false;

    /**
     * Hardware skinning was tested on this GPU, results
     * are stored in {@link #hwSkinningSupported} variable.
     */
    private transient boolean hwSkinningTested = false;

    /**
     * If hardware skinning was {@link #hwSkinningTested tested}, then
     * this variable will be set to true if supported, and false if otherwise.
     */
    private transient boolean hwSkinningSupported = false;

    /**
     * Bone offset matrices, recreated each frame
     */
    private transient Matrix4f[] offsetMatrices;


    private MatParamOverride numberOfJointsParam;
    private MatParamOverride jointMatricesParam;

    /**
     * Serialization only. Do not use.
     */
    protected SkinningControl() {
    }

    /**
     * Creates an armature control. The list of targets will be acquired
     * automatically when the control is attached to a node.
     *
     * @param armature the armature
     */
    public SkinningControl(Armature armature) {
        if (armature == null) {
            throw new IllegalArgumentException("armature cannot be null");
        }
        this.armature = armature;
        this.numberOfJointsParam = new MatParamOverride(VarType.Int, "NumberOfBones", null);
        this.jointMatricesParam = new MatParamOverride(VarType.Matrix4Array, "BoneMatrices", null);
    }


    private void switchToHardware() {
        numberOfJointsParam.setEnabled(true);
        jointMatricesParam.setEnabled(true);

        // Next full 10 bones (e.g. 30 on 24 bones)
        int numBones = ((armature.getJointCount() / 10) + 1) * 10;
        numberOfJointsParam.setValue(numBones);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(false);
            }
        }
    }

    private void switchToSoftware() {
        numberOfJointsParam.setEnabled(false);
        jointMatricesParam.setEnabled(false);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(true);
            }
        }
    }

    private boolean testHardwareSupported(RenderManager rm) {

        //Only 255 bones max supported with hardware skinning
        if (armature.getJointCount() > 255) {
            return false;
        }

        switchToHardware();

        try {
            rm.preloadScene(spatial);
            return true;
        } catch (RendererException e) {
            logger.log(Level.WARNING, "Could not enable HW skinning due to shader compile error:", e);
            return false;
        }
    }

    /**
     * Specifies if hardware skinning is preferred. If it is preferred and
     * supported by GPU, it shall be enabled.  If it's not preferred, or not
     * supported by GPU, then it shall be disabled.
     *
     * @param preferred true to prefer hardware skinning, false to prefer 
     * software skinning (default=true)
     * @see #isHardwareSkinningUsed()
     */
    public void setHardwareSkinningPreferred(boolean preferred) {
        hwSkinningDesired = preferred;
    }

    /**
     * @return True if hardware skinning is preferable to software skinning.
     * Set to false by default.
     * @see #setHardwareSkinningPreferred(boolean)
     */
    public boolean isHardwareSkinningPreferred() {
        return hwSkinningDesired;
    }

    /**
     * @return True is hardware skinning is activated and is currently used, false otherwise.
     */
    public boolean isHardwareSkinningUsed() {
        return hwSkinningEnabled;
    }


    /**
     * If specified the geometry has an animated mesh, add its mesh and material
     * to the lists of animation targets.
     */
    private void findTargets(Geometry geometry) {
        Mesh mesh = geometry.getMesh();
        if (mesh != null && mesh.isAnimated()) {
            targets.add(geometry);
        }

    }

    private void findTargets(Node node) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                findTargets((Geometry) child);
            } else if (child instanceof Node) {
                findTargets((Node) child);
            }
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        Spatial oldSpatial = this.spatial;
        super.setSpatial(spatial);
        updateTargetsAndMaterials(spatial);

        if (oldSpatial != null) {
            oldSpatial.removeMatParamOverride(numberOfJointsParam);
            oldSpatial.removeMatParamOverride(jointMatricesParam);
        }

        if (spatial != null) {
            spatial.removeMatParamOverride(numberOfJointsParam);
            spatial.removeMatParamOverride(jointMatricesParam);
            spatial.addMatParamOverride(numberOfJointsParam);
            spatial.addMatParamOverride(jointMatricesParam);
        }
    }

    private void controlRenderSoftware() {
        resetToBind(); // reset morph meshes to bind pose

        offsetMatrices = armature.computeSkinningMatrices();

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            // NOTE: This assumes code higher up has
            // already ensured this mesh is animated.
            // Otherwise a crash will happen in skin update.
            softwareSkinUpdate(mesh, offsetMatrices);
        }
    }

    private void controlRenderHardware() {
        offsetMatrices = armature.computeSkinningMatrices();
        jointMatricesParam.setValue(offsetMatrices);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        if (!wasMeshUpdated) {
            updateTargetsAndMaterials(spatial);

            // Prevent illegal cases. These should never happen.
            assert hwSkinningTested || (!hwSkinningTested && !hwSkinningSupported && !hwSkinningEnabled);
            assert !hwSkinningEnabled || (hwSkinningEnabled && hwSkinningTested && hwSkinningSupported);

            if (hwSkinningDesired && !hwSkinningTested) {
                hwSkinningTested = true;
                hwSkinningSupported = testHardwareSupported(rm);

                if (hwSkinningSupported) {
                    hwSkinningEnabled = true;

                    Logger.getLogger(SkinningControl.class.getName()).log(Level.INFO, "Hardware skinning engaged for {0}", spatial);
                } else {
                    switchToSoftware();
                }
            } else if (hwSkinningDesired && hwSkinningSupported && !hwSkinningEnabled) {
                switchToHardware();
                hwSkinningEnabled = true;
            } else if (!hwSkinningDesired && hwSkinningEnabled) {
                switchToSoftware();
                hwSkinningEnabled = false;
            }

            if (hwSkinningEnabled) {
                controlRenderHardware();
            } else {
                controlRenderSoftware();
            }

            wasMeshUpdated = true;
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        wasMeshUpdated = false;
        armature.update();
    }

    //only do this for software updates
    void resetToBind() {
        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                try (AttributeModifier pb = mesh.modify(Type.Position);
                        AttributeModifier bpb = mesh.modify(Type.BindPosePosition)) {
                    pb.putReader(bpb, 0, 0, 0, 0, mesh.getVertexCount(), 3);
                }
                if (mesh.attributeExists(Type.BindPoseNormal)) {
                    try (AttributeModifier nb = mesh.modify(Type.Normal);
                            AttributeModifier bnb = mesh.modify(Type.BindPoseNormal)) {
                        nb.putReader(bnb, 0, 0, 0, 0, mesh.getVertexCount(), 3);
                    }
                }
                if (mesh.attributeExists(Type.BindPoseTangent)) {
                    try (AttributeModifier tb = mesh.modify(Type.Tangent);
                            AttributeModifier btb = mesh.modify(Type.BindPoseTangent)) {
                        tb.putReader(btb, 0, 0, 0, 0, mesh.getVertexCount(), 4);
                    }
                }

                // todo: replace prepareForAnim
//                Buffer bwBuff = mesh.getBuffer(Type.BoneWeight).getData();
//                Buffer biBuff = mesh.getBuffer(Type.BoneIndex).getData();
//                if (!biBuff.hasArray() || !bwBuff.hasArray()) {
//                    mesh.prepareForAnim(true); // prepare for software animation
//                }
            }
        }
    }

    @Override
    public Object jmeClone() {
        return super.jmeClone();
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        this.armature = cloner.clone(armature);

        // If the targets were cloned then this will clone them.  If the targets
        // were shared then this will share them.
        this.targets = cloner.clone(targets);

        this.numberOfJointsParam = cloner.clone(numberOfJointsParam);
        this.jointMatricesParam = cloner.clone(jointMatricesParam);
    }

    /**
     * Access the attachments node of the named bone. If the bone doesn't
     * already have an attachments node, create one and attach it to the scene
     * graph. Models and effects attached to the attachments node will follow
     * the bone's motions.
     *
     * @param jointName the name of the joint
     * @return the attachments node of the joint
     */
    public Node getAttachmentsNode(String jointName) {
        Joint b = armature.getJoint(jointName);
        if (b == null) {
            throw new IllegalArgumentException("Given bone name does not exist "
                    + "in the armature.");
        }

        updateTargetsAndMaterials(spatial);
        int boneIndex = armature.getJointIndex(b);
        Node n = b.getAttachmentsNode(boneIndex, targets);
        /*
         * Select a node to parent the attachments node.
         */
        Node parent;
        if (spatial instanceof Node) {
            parent = (Node) spatial; // the usual case
        } else {
            parent = spatial.getParent();
        }
        parent.attachChild(n);

        return n;
    }

    /**
     * returns the armature of this control
     *
     * @return the pre-existing instance
     */
    public Armature getArmature() {
        return armature;
    }

    /**
     * Enumerate the target meshes of this control.
     *
     * @return a new array
     */
    public Mesh[] getTargets() {
        Mesh[] result = new Mesh[targets.size()];
        int i = 0;
        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            result[i] = mesh;
            i++;
        }

        return result;
    }

    /**
     * Update the mesh according to the given transformation matrices
     *
     * @param mesh           then mesh
     * @param offsetMatrices the transformation matrices to apply
     */
    private void softwareSkinUpdate(Mesh mesh, Matrix4f[] offsetMatrices) {

        if (!mesh.attributeExists(Type.Tangent)) {
            //if there are no tangents use the classic skinning
            applySkinning(mesh, offsetMatrices);
        } else {
            //if there are tangents use the skinning with tangents
            applySkinningTangents(mesh, offsetMatrices);
        }


    }

    /**
     * Method to apply skinning transforms to a mesh's buffers
     *
     * @param mesh           the mesh
     * @param offsetMatrices the offset matrices to apply
     */
    private void applySkinning(Mesh mesh, Matrix4f[] offsetMatrices) {

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }

        try (AttributeModifier positionBuffer = mesh.modify(Type.Position);
             AttributeModifier normalBuffer = mesh.modify(Type.Normal);
             AttributeModifier boneIndexBuffer = mesh.modify(Type.BoneIndex);
             AttributeModifier boneWeightBuffer = mesh.modify(Type.BoneWeight)) {

            for (int v = 0; v < mesh.getVertexCount(); v++) {
                if (boneWeightBuffer.getFloat(v, 0) == 0f) {
                    continue; // skip if first weight is zero
                }
                float vtx = positionBuffer.getFloat(v, 0);
                float vty = positionBuffer.getFloat(v, 1);
                float vtz = positionBuffer.getFloat(v, 2);
                float nmx = normalBuffer.getFloat(v, 0);
                float nmy = normalBuffer.getFloat(v, 1);
                float nmz = normalBuffer.getFloat(v, 2);
                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0;
                for (int w = 0; w < maxWeightsPerVert; w++) {
                    float weight = boneWeightBuffer.getFloat(v, w);
                    Matrix4f mat = offsetMatrices[boneIndexBuffer.getInt(v, w)];
                    // transform position
                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;
                    // transform normal
                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;
                }
                positionBuffer.putVector3(v, 0, rx, ry, rz);
                normalBuffer.putVector3(v, 0, rnx, rny, rnz);
            }

            positionBuffer.setUpdateNeeded();
            normalBuffer.setUpdateNeeded();

        }

    }

    /**
     * Specific method for skinning with tangents to avoid cluttering the
     * classic skinning calculation with null checks that would slow down the
     * process even if tangents don't have to be computed. Also the iteration
     * has additional indexes since tangent has 4 components instead of 3 for
     * pos and norm
     *
     * @param mesh           the mesh
     * @param offsetMatrices the offsetMatrices to apply
     */
    private void applySkinningTangents(Mesh mesh, Matrix4f[] offsetMatrices) {

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }

        try (AttributeModifier positionBuffer = mesh.modify(Type.Position);
                AttributeModifier normalBuffer = mesh.modify(Type.Normal);
                AttributeModifier tangentBuffer = mesh.modify(Type.Tangent);
                AttributeModifier boneIndexBuffer = mesh.modify(Type.BoneIndex);
                AttributeModifier boneWeightBuffer = mesh.modify(Type.BoneWeight)) {

            for (int v = 0; v < mesh.getVertexCount(); v++) {
                if (boneWeightBuffer.getFloat(v, 0) == 0f) {
                    continue; // skip if first weight is zero
                }
                float vtx = positionBuffer.getFloat(v, 0);
                float vty = positionBuffer.getFloat(v, 1);
                float vtz = positionBuffer.getFloat(v, 2);
                float nmx = normalBuffer.getFloat(v, 0);
                float nmy = normalBuffer.getFloat(v, 1);
                float nmz = normalBuffer.getFloat(v, 2);
                float tnx = tangentBuffer.getFloat(v, 0);
                float tny = tangentBuffer.getFloat(v, 1);
                float tnz = tangentBuffer.getFloat(v, 2);
                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0, rtx = 0, rty = 0, rtz = 0;
                for (int w = 0; w < maxWeightsPerVert; w++) {
                    float weight = boneWeightBuffer.getFloat(v, w);
                    Matrix4f mat = offsetMatrices[boneIndexBuffer.getInt(v, w)];
                    // transform position
                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;
                    // transform normal
                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;
                    // transform tangent (xyz only)
                    rtx += (tnx * mat.m00 + tny * mat.m01 + tnz * mat.m02) * weight;
                    rty += (tnx * mat.m10 + tny * mat.m11 + tnz * mat.m12) * weight;
                    rtz += (tnx * mat.m20 + tny * mat.m21 + tnz * mat.m22) * weight;
                }
                positionBuffer.putVector3(v, 0, rx, ry, rz);
                normalBuffer.putVector3(v, 0, rnx, rny, rnz);
                tangentBuffer.putVector3(v, 0, rtx, rty, rtz);
            }

            positionBuffer.setUpdateNeeded();
            normalBuffer.setUpdateNeeded();
            tangentBuffer.setUpdateNeeded();

        }

    }

    /**
     * Serialize this Control to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param ex the exporter to write to (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(armature, "armature", null);

        oc.write(numberOfJointsParam, "numberOfBonesParam", null);
        oc.write(jointMatricesParam, "boneMatricesParam", null);
    }

    /**
     * De-serialize this Control from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to read from (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        armature = (Armature) in.readSavable("armature", null);

        numberOfJointsParam = (MatParamOverride) in.readSavable("numberOfBonesParam", null);
        jointMatricesParam = (MatParamOverride) in.readSavable("boneMatricesParam", null);

        if (numberOfJointsParam == null) {
            numberOfJointsParam = new MatParamOverride(VarType.Int, "NumberOfBones", null);
            jointMatricesParam = new MatParamOverride(VarType.Matrix4Array, "BoneMatrices", null);
            getSpatial().addMatParamOverride(numberOfJointsParam);
            getSpatial().addMatParamOverride(jointMatricesParam);
        }
    }

    /**
     * Update the lists of animation targets.
     *
     * @param spatial the controlled spatial
     */
    private void updateTargetsAndMaterials(Spatial spatial) {
        targets.clear();

        if (spatial instanceof Node) {
            findTargets((Node) spatial);
        } else if (spatial instanceof Geometry) {
            findTargets((Geometry) spatial);
        }
    }
}
