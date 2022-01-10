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
package com.jme3.animation;

import com.jme3.anim.SkinningControl;
import com.jme3.export.*;
import com.jme3.material.MatParamOverride;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.shader.VarType;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Skeleton control deforms a model according to a skeleton, It handles the
 * computation of the deformation matrices and performs the transformations on
 * the mesh
 *
 * @author Rémy Bouquet Based on AnimControl by Kirill Vainer
 * @deprecated use {@link SkinningControl}
 */
@Deprecated
public class SkeletonControl extends AbstractControl implements Cloneable, JmeCloneable {

    /**
     * The skeleton of the model.
     */
    private Skeleton skeleton;

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

    private MatParamOverride numberOfBonesParam;
    private MatParamOverride boneMatricesParam;

    /**
     * Serialization only. Do not use.
     */
    protected SkeletonControl() {
    }

    private void switchToHardware() {
        numberOfBonesParam.setEnabled(true);
        boneMatricesParam.setEnabled(true);

        // Next full 10 bones (e.g. 30 on 24 bones)
        int numBones = ((skeleton.getBoneCount() / 10) + 1) * 10;
        numberOfBonesParam.setValue(numBones);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(false);
            }
        }
    }

    private void switchToSoftware() {
        numberOfBonesParam.setEnabled(false);
        boneMatricesParam.setEnabled(false);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(true);
            }
        }
    }

    private boolean testHardwareSupported(RenderManager rm) {

        //Only 255 bones max supported with hardware skinning
        if (skeleton.getBoneCount() > 255) {
            return false;
        }

        switchToHardware();

        try {
            rm.preloadScene(spatial);
            return true;
        } catch (RendererException e) {
            Logger.getLogger(SkeletonControl.class.getName()).log(Level.WARNING, "Could not enable HW skinning due to shader compile error:", e);
            return false;
        }
    }

    /**
     * Specifies if hardware skinning is preferred. If it is preferred and
     * supported by GPU, it shall be enabled, if it's not preferred, or not
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
     *
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
     * Creates a skeleton control. The list of targets will be acquired
     * automatically when the control is attached to a node.
     *
     * @param skeleton the skeleton
     */
    public SkeletonControl(Skeleton skeleton) {
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null");
        }
        this.skeleton = skeleton;
        this.numberOfBonesParam = new MatParamOverride(VarType.Int, "NumberOfBones", null);
        this.boneMatricesParam = new MatParamOverride(VarType.Matrix4Array, "BoneMatrices", null);
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
            oldSpatial.removeMatParamOverride(numberOfBonesParam);
            oldSpatial.removeMatParamOverride(boneMatricesParam);
        }

        if (spatial != null) {
            spatial.removeMatParamOverride(numberOfBonesParam);
            spatial.removeMatParamOverride(boneMatricesParam);
            spatial.addMatParamOverride(numberOfBonesParam);
            spatial.addMatParamOverride(boneMatricesParam);
        }
    }

    private void controlRenderSoftware() {
        resetToBind(); // reset morph meshes to bind pose

        offsetMatrices = skeleton.computeSkinningMatrices();

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            // NOTE: This assumes code higher up has
            // already ensured this mesh is animated.
            // Otherwise a crash will happen in skin update.
            softwareSkinUpdate(mesh, offsetMatrices);
        }
    }

    private void controlRenderHardware() {
        offsetMatrices = skeleton.computeSkinningMatrices();
        boneMatricesParam.setValue(offsetMatrices);
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

                    Logger.getLogger(SkeletonControl.class.getName()).log(Level.INFO, "Hardware skinning engaged for {0}", spatial);
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
    }

    //only do this for software updates
    void resetToBind() {
        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                Buffer bwBuff = mesh.getBuffer(Type.BoneWeight).getData();
                Buffer biBuff = mesh.getBuffer(Type.BoneIndex).getData();
                if (!biBuff.hasArray() || !bwBuff.hasArray()) {
                    mesh.prepareForAnim(true); // prepare for software animation
                }
                VertexBuffer bindPos = mesh.getBuffer(Type.BindPosePosition);
                VertexBuffer bindNorm = mesh.getBuffer(Type.BindPoseNormal);
                VertexBuffer pos = mesh.getBuffer(Type.Position);
                VertexBuffer norm = mesh.getBuffer(Type.Normal);
                FloatBuffer pb = (FloatBuffer) pos.getData();
                FloatBuffer nb = (FloatBuffer) norm.getData();
                FloatBuffer bpb = (FloatBuffer) bindPos.getData();
                FloatBuffer bnb = (FloatBuffer) bindNorm.getData();
                pb.clear();
                nb.clear();
                bpb.clear();
                bnb.clear();

                //reset bind tangents if there is a bind tangent buffer
                VertexBuffer bindTangents = mesh.getBuffer(Type.BindPoseTangent);
                if (bindTangents != null) {
                    VertexBuffer tangents = mesh.getBuffer(Type.Tangent);
                    FloatBuffer tb = (FloatBuffer) tangents.getData();
                    FloatBuffer btb = (FloatBuffer) bindTangents.getData();
                    tb.clear();
                    btb.clear();
                    tb.put(btb).clear();
                }

                pb.put(bpb).clear();
                nb.put(bnb).clear();
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

        this.skeleton = cloner.clone(skeleton);

        // If the targets were cloned then this will clone them.  If the targets
        // were shared then this will share them.
        this.targets = cloner.clone(targets);

        this.numberOfBonesParam = cloner.clone(numberOfBonesParam);
        this.boneMatricesParam = cloner.clone(boneMatricesParam);
    }

    /**
     * Access the attachments node of the named bone. If the bone doesn't
     * already have an attachments node, create one and attach it to the scene
     * graph. Models and effects attached to the attachments node will follow
     * the bone's motions.
     *
     * @param boneName the name of the bone
     * @return the attachments node of the bone
     */
    public Node getAttachmentsNode(String boneName) {
        Bone b = skeleton.getBone(boneName);
        if (b == null) {
            throw new IllegalArgumentException("Given bone name does not exist "
                    + "in the skeleton.");
        }

        updateTargetsAndMaterials(spatial);
        int boneIndex = skeleton.getBoneIndex(b);
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
     * returns the skeleton of this control
     *
     * @return the pre-existing instance
     */
    public Skeleton getSkeleton() {
        return skeleton;
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
     * @param mesh then mesh
     * @param offsetMatrices the transformation matrices to apply
     */
    private void softwareSkinUpdate(Mesh mesh, Matrix4f[] offsetMatrices) {

        VertexBuffer tb = mesh.getBuffer(Type.Tangent);
        if (tb == null) {
            //if there are no tangents use the classic skinning
            applySkinning(mesh, offsetMatrices);
        } else {
            //if there are tangents use the skinning with tangents
            applySkinningTangents(mesh, offsetMatrices, tb);
        }
    }

    /**
     * Method to apply skinning transforms to a mesh's buffers
     *
     * @param mesh the mesh
     * @param offsetMatrices the offset matrices to apply
     */
    private void applySkinning(Mesh mesh, Matrix4f[] offsetMatrices) {
        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }
        int fourMinusMaxWeights = 4 - maxWeightsPerVert;

        // NOTE: This code assumes the vertex buffer is in bind pose
        // resetToBind() has been called this frame
        VertexBuffer vb = mesh.getBuffer(Type.Position);
        FloatBuffer fvb = (FloatBuffer) vb.getData();
        fvb.rewind();

        VertexBuffer nb = mesh.getBuffer(Type.Normal);
        FloatBuffer fnb = (FloatBuffer) nb.getData();
        fnb.rewind();

        // get boneIndexes and weights for mesh
        IndexBuffer ib = IndexBuffer.wrapIndexBuffer(mesh.getBuffer(Type.BoneIndex).getData());
        FloatBuffer wb = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        wb.rewind();

        float[] weights = wb.array();
        int idxWeights = 0;

        TempVars vars = TempVars.get();

        float[] posBuf = vars.skinPositions;
        float[] normBuf = vars.skinNormals;

        int iterations = (int) FastMath.ceil(fvb.limit() / ((float) posBuf.length));
        int bufLength = posBuf.length;
        for (int i = iterations - 1; i >= 0; i--) {
            // read next set of positions and normals from native buffer
            bufLength = Math.min(posBuf.length, fvb.remaining());
            fvb.get(posBuf, 0, bufLength);
            fnb.get(normBuf, 0, bufLength);
            int verts = bufLength / 3;
            int idxPositions = 0;

            // iterate vertices and apply skinning transform for each effecting bone
            for (int vert = verts - 1; vert >= 0; vert--) {
                // Skip this vertex if the first weight is zero.
                if (weights[idxWeights] == 0) {
                    idxPositions += 3;
                    idxWeights += 4;
                    continue;
                }

                float nmx = normBuf[idxPositions];
                float vtx = posBuf[idxPositions++];
                float nmy = normBuf[idxPositions];
                float vty = posBuf[idxPositions++];
                float nmz = normBuf[idxPositions];
                float vtz = posBuf[idxPositions++];

                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0;

                for (int w = maxWeightsPerVert - 1; w >= 0; w--) {
                    float weight = weights[idxWeights];
                    Matrix4f mat = offsetMatrices[ib.get(idxWeights++)];

                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;

                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;
                }

                idxWeights += fourMinusMaxWeights;

                idxPositions -= 3;
                normBuf[idxPositions] = rnx;
                posBuf[idxPositions++] = rx;
                normBuf[idxPositions] = rny;
                posBuf[idxPositions++] = ry;
                normBuf[idxPositions] = rnz;
                posBuf[idxPositions++] = rz;
            }

            fvb.position(fvb.position() - bufLength);
            fvb.put(posBuf, 0, bufLength);
            fnb.position(fnb.position() - bufLength);
            fnb.put(normBuf, 0, bufLength);
        }

        vars.release();

        vb.updateData(fvb);
        nb.updateData(fnb);

    }

    /**
     * Specific method for skinning with tangents to avoid cluttering the
     * classic skinning calculation with null checks that would slow down the
     * process even if tangents don't have to be computed. Also the iteration
     * has additional indexes since tangent has 4 components instead of 3 for
     * pos and norm
     *
     * @param mesh the mesh
     * @param offsetMatrices the offset matrices to apply
     * @param tb the tangent vertexBuffer
     */
    private void applySkinningTangents(Mesh mesh, Matrix4f[] offsetMatrices, VertexBuffer tb) {
        int maxWeightsPerVert = mesh.getMaxNumWeights();

        if (maxWeightsPerVert <= 0) {
            throw new IllegalStateException("Max weights per vert is incorrectly set!");
        }

        int fourMinusMaxWeights = 4 - maxWeightsPerVert;

        // NOTE: This code assumes the vertex buffer is in bind pose
        // resetToBind() has been called this frame
        VertexBuffer vb = mesh.getBuffer(Type.Position);
        FloatBuffer fvb = (FloatBuffer) vb.getData();
        fvb.rewind();

        VertexBuffer nb = mesh.getBuffer(Type.Normal);

        FloatBuffer fnb = (FloatBuffer) nb.getData();
        fnb.rewind();

        FloatBuffer ftb = (FloatBuffer) tb.getData();
        ftb.rewind();

        // get boneIndexes and weights for mesh
        IndexBuffer ib = IndexBuffer.wrapIndexBuffer(mesh.getBuffer(Type.BoneIndex).getData());
        FloatBuffer wb = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        wb.rewind();

        float[] weights = wb.array();
        int idxWeights = 0;

        TempVars vars = TempVars.get();

        float[] posBuf = vars.skinPositions;
        float[] normBuf = vars.skinNormals;
        float[] tanBuf = vars.skinTangents;

        int iterations = (int) FastMath.ceil(fvb.limit() / ((float) posBuf.length));
        int bufLength = 0;
        int tanLength = 0;
        for (int i = iterations - 1; i >= 0; i--) {
            // read next set of positions and normals from native buffer
            bufLength = Math.min(posBuf.length, fvb.remaining());
            tanLength = Math.min(tanBuf.length, ftb.remaining());
            fvb.get(posBuf, 0, bufLength);
            fnb.get(normBuf, 0, bufLength);
            ftb.get(tanBuf, 0, tanLength);
            int verts = bufLength / 3;
            int idxPositions = 0;
            // Tangents have their own index because they have 4 components.
            int idxTangents = 0;

            // iterate vertices and apply skinning transform for each effecting bone
            for (int vert = verts - 1; vert >= 0; vert--) {
                // Skip this vertex if the first weight is zero.
                if (weights[idxWeights] == 0) {
                    idxTangents += 4;
                    idxPositions += 3;
                    idxWeights += 4;
                    continue;
                }

                float nmx = normBuf[idxPositions];
                float vtx = posBuf[idxPositions++];
                float nmy = normBuf[idxPositions];
                float vty = posBuf[idxPositions++];
                float nmz = normBuf[idxPositions];
                float vtz = posBuf[idxPositions++];

                float tnx = tanBuf[idxTangents++];
                float tny = tanBuf[idxTangents++];
                float tnz = tanBuf[idxTangents++];

                // skipping the 4th component of the tangent since it doesn't have to be transformed
                idxTangents++;

                float rx = 0, ry = 0, rz = 0, rnx = 0, rny = 0, rnz = 0, rtx = 0, rty = 0, rtz = 0;

                for (int w = maxWeightsPerVert - 1; w >= 0; w--) {
                    float weight = weights[idxWeights];
                    Matrix4f mat = offsetMatrices[ib.get(idxWeights++)];

                    rx += (mat.m00 * vtx + mat.m01 * vty + mat.m02 * vtz + mat.m03) * weight;
                    ry += (mat.m10 * vtx + mat.m11 * vty + mat.m12 * vtz + mat.m13) * weight;
                    rz += (mat.m20 * vtx + mat.m21 * vty + mat.m22 * vtz + mat.m23) * weight;

                    rnx += (nmx * mat.m00 + nmy * mat.m01 + nmz * mat.m02) * weight;
                    rny += (nmx * mat.m10 + nmy * mat.m11 + nmz * mat.m12) * weight;
                    rnz += (nmx * mat.m20 + nmy * mat.m21 + nmz * mat.m22) * weight;

                    rtx += (tnx * mat.m00 + tny * mat.m01 + tnz * mat.m02) * weight;
                    rty += (tnx * mat.m10 + tny * mat.m11 + tnz * mat.m12) * weight;
                    rtz += (tnx * mat.m20 + tny * mat.m21 + tnz * mat.m22) * weight;
                }

                idxWeights += fourMinusMaxWeights;

                idxPositions -= 3;

                normBuf[idxPositions] = rnx;
                posBuf[idxPositions++] = rx;
                normBuf[idxPositions] = rny;
                posBuf[idxPositions++] = ry;
                normBuf[idxPositions] = rnz;
                posBuf[idxPositions++] = rz;

                idxTangents -= 4;

                tanBuf[idxTangents++] = rtx;
                tanBuf[idxTangents++] = rty;
                tanBuf[idxTangents++] = rtz;

                //once again skipping the 4th component of the tangent
                idxTangents++;
            }

            fvb.position(fvb.position() - bufLength);
            fvb.put(posBuf, 0, bufLength);
            fnb.position(fnb.position() - bufLength);
            fnb.put(normBuf, 0, bufLength);
            ftb.position(ftb.position() - tanLength);
            ftb.put(tanBuf, 0, tanLength);
        }

        vars.release();

        vb.updateData(fvb);
        nb.updateData(fnb);
        tb.updateData(ftb);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(skeleton, "skeleton", null);

        oc.write(numberOfBonesParam, "numberOfBonesParam", null);
        oc.write(boneMatricesParam, "boneMatricesParam", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        skeleton = (Skeleton) in.readSavable("skeleton", null);

        numberOfBonesParam = (MatParamOverride) in.readSavable("numberOfBonesParam", null);
        boneMatricesParam = (MatParamOverride) in.readSavable("boneMatricesParam", null);

        if (numberOfBonesParam == null) {
            numberOfBonesParam = new MatParamOverride(VarType.Int, "NumberOfBones", null);
            boneMatricesParam = new MatParamOverride(VarType.Matrix4Array, "BoneMatrices", null);
            getSpatial().addMatParamOverride(numberOfBonesParam);
            getSpatial().addMatParamOverride(boneMatricesParam);
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
