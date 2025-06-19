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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.MatParamOverride;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
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
 * The `SkinningControl` deforms a 3D model according to an {@link Armature}. It manages the
 * computation of deformation matrices and applies these transformations to the mesh,
 * supporting both software and hardware-accelerated skinning.
 *
 * <p>
 * **Software Skinning:** Performed on the CPU, offering broader compatibility but
 * potentially lower performance for complex models.
 * <p>
 * **Hardware Skinning:** Utilizes the GPU for deformation, providing significantly
 * better performance but requiring shader support and having a limit on the number
 * of bones (typically 255 in common shaders).
 *
 * @author Nehon
 */
public class SkinningControl extends AbstractControl implements JmeCloneable {

    private static final Logger logger = Logger.getLogger(SkinningControl.class.getName());

    /**
     * The maximum number of bones supported for hardware skinning in common shaders.
     */
    private static final int MAX_BONES_HW_SKINNING_SUPPORT = 255;

    /**
     * The armature of the model.
     */
    private Armature armature;

    /**
     * A list of geometries that this control will deform.
     */
    private SafeArrayList<Geometry> targets = new SafeArrayList<>(Geometry.class);

    /**
     * Used to track when a mesh needs to be updated. Meshes are only updated if they
     * are visible in at least one camera.
     */
    private boolean meshUpdateRequired = true;

    /**
     * Indicates whether hardware skinning is preferred. If `true` and the GPU
     * supports it, hardware skinning will be enabled.
     */
    private transient boolean hwSkinningPreferred = true;

    /**
     * Indicates if hardware skinning is currently active and being used.
     */
    private transient boolean hwSkinningEnabled = false;

    /**
     * Flag indicating whether hardware skinning compatibility has been tested
     * on the current GPU. Results are stored in {@link #hwSkinningSupported}.
     */
    private transient boolean hwSkinningTested = false;

    /**
     * Stores the result of the hardware skinning compatibility test. `true` if
     * supported, `false` otherwise. This is only valid after
     * {@link #hwSkinningTested} is `true`.
     */
    private transient boolean hwSkinningSupported = false;

    /**
     * Bone offset matrices, computed each frame to deform the mesh based on
     * the armature's current pose.
     */
    private transient Matrix4f[] boneOffsetMatrices;

    private MatParamOverride numberOfJointsParam = new MatParamOverride(VarType.Int, "NumberOfBones", null);
    private MatParamOverride jointMatricesParam = new MatParamOverride(VarType.Matrix4Array, "BoneMatrices", null);

    /**
     * Serialization only. Do not use.
     */
    protected SkinningControl() {
    }

    /**
     * Creates a new `SkinningControl` for the given armature.
     *
     * @param armature The armature that drives the deformation (not null).
     */
    public SkinningControl(Armature armature) {
        if (armature == null) {
            throw new IllegalArgumentException("armature cannot be null.");
        }
        this.armature = armature;
    }

    /**
     * Configures the material parameters and meshes for hardware skinning.
     */
    private void enableHardwareSkinning() {
        numberOfJointsParam.setEnabled(true);
        jointMatricesParam.setEnabled(true);

        // Calculate the number of bones rounded up to the nearest multiple of 10.
        // This is often required by shaders for array uniform declarations.
        int numBones = ((armature.getJointCount() / 10) + 1) * 10;
        numberOfJointsParam.setValue(numBones);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(false);
            }
        }
    }

    /**
     * Configures the material parameters and meshes for software skinning.
     */
    private void enableSoftwareSkinning() {
        numberOfJointsParam.setEnabled(false);
        jointMatricesParam.setEnabled(false);

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                mesh.prepareForAnim(true);
            }
        }
    }

    /**
     * Tests if hardware skinning is supported by the GPU for the current spatial.
     *
     * @param renderManager the RenderManager instance
     * @return true if hardware skinning is supported, false otherwise
     */
    private boolean testHardwareSupported(RenderManager renderManager) {
        // Only 255 bones max supported with hardware skinning in common shaders.
        if (armature.getJointCount() > MAX_BONES_HW_SKINNING_SUPPORT) {
            logger.log(Level.INFO, "Hardware skinning not supported for {0}: Too many bones ({1} > 255).",
                    new Object[]{spatial, armature.getJointCount()});
            return false;
        }

        // Temporarily enable hardware skinning to test shader compilation.
        enableHardwareSkinning();
        boolean hwSkinningEngaged = false;

        try {
            renderManager.preloadScene(spatial);
            logger.log(Level.INFO, "Hardware skinning engaged for {0}", spatial);
            hwSkinningEngaged = true;

        } catch (RendererException ex) {
            logger.log(Level.WARNING, "Could not enable HW skinning due to shader compile error: ", ex);
        }

        return hwSkinningEngaged;
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
        hwSkinningPreferred = preferred;
    }

    /**
     * @return True if hardware skinning is preferable to software skinning.
     * Set to false by default.
     * @see #setHardwareSkinningPreferred(boolean)
     */
    public boolean isHardwareSkinningPreferred() {
        return hwSkinningPreferred;
    }

    /**
     * @return True is hardware skinning is activated and is currently used, false otherwise.
     */
    public boolean isHardwareSkinningUsed() {
        return hwSkinningEnabled;
    }

    /**
     * Recursively finds and adds animated geometries to the targets list.
     *
     * @param sp The spatial to search within.
     */
    private void collectAnimatedGeometries(Spatial sp) {
        if (sp instanceof Geometry) {
            Geometry geo = (Geometry) sp;
            Mesh mesh = geo.getMesh();
            if (mesh != null && mesh.isAnimated()) {
                targets.add(geo);
            }
        } else if (sp instanceof Node) {
            for (Spatial child : ((Node) sp).getChildren()) {
                collectAnimatedGeometries(child);
            }
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        Spatial oldSpatial = this.spatial;
        super.setSpatial(spatial);
        updateAnimationTargets(spatial);

        if (oldSpatial != null) {
            // Ensure parameters are removed from the old spatial to prevent memory leaks
            oldSpatial.removeMatParamOverride(numberOfJointsParam);
            oldSpatial.removeMatParamOverride(jointMatricesParam);
        }

        if (spatial != null) {
            // Add parameters to the new spatial. No need to remove first if they are not already present.
            spatial.addMatParamOverride(numberOfJointsParam);
            spatial.addMatParamOverride(jointMatricesParam);
        }
    }

    /**
     * Performs software skinning updates.
     */
    private void controlRenderSoftware() {
        resetToBind(); // reset morph meshes to bind pose

        boneOffsetMatrices = armature.computeSkinningMatrices();

        for (Geometry geometry : targets) {
            Mesh mesh = geometry.getMesh();
            // NOTE: This assumes code higher up has already ensured this mesh is animated.
            // Otherwise, a crash will happen in skin update.
            applySoftwareSkinning(mesh, boneOffsetMatrices);
        }
    }

    /**
     * Prepares parameters for hardware skinning.
     */
    private void controlRenderHardware() {
        boneOffsetMatrices = armature.computeSkinningMatrices();
        jointMatricesParam.setValue(boneOffsetMatrices);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        if (meshUpdateRequired) {
            updateAnimationTargets(spatial);

            // Prevent illegal cases. These should never happen.
            assert hwSkinningTested || (!hwSkinningTested && !hwSkinningSupported && !hwSkinningEnabled);
            assert !hwSkinningEnabled || (hwSkinningEnabled && hwSkinningTested && hwSkinningSupported);

            if (hwSkinningPreferred && !hwSkinningTested) {
                // If hardware skinning is preferred and hasn't been tested yet, test it.
                hwSkinningTested = true;
                hwSkinningSupported = testHardwareSupported(rm);

                if (hwSkinningSupported) {
                    hwSkinningEnabled = true;
                } else {
                    enableSoftwareSkinning();
                }
            } else if (hwSkinningPreferred && hwSkinningSupported && !hwSkinningEnabled) {
                // If hardware skinning is preferred, supported, but not yet enabled, enable it.
                enableHardwareSkinning();
                hwSkinningEnabled = true;

            } else if (!hwSkinningPreferred && hwSkinningEnabled) {
                // If hardware skinning is no longer preferred but is enabled, switch to software.
                enableSoftwareSkinning();
                hwSkinningEnabled = false;
            }

            if (hwSkinningEnabled) {
                controlRenderHardware();
            } else {
                controlRenderSoftware();
            }

            meshUpdateRequired = false; // Reset flag after update
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        meshUpdateRequired = true; // Mark for mesh update on next render pass
        armature.update();
    }

    /**
     * Resets the vertex, normal, and tangent buffers of animated meshes to their
     * original bind pose. This is crucial for software skinning to ensure
     * transformations are applied from a consistent base.
     * This method is only applied when performing software updates.
     */
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
                FloatBuffer pb = (FloatBuffer) pos.getData();
                FloatBuffer bpb = (FloatBuffer) bindPos.getData();
                pb.clear();
                bpb.clear();

                // reset bind normals if there is a BindPoseNormal buffer
                if (bindNorm != null) {
                    VertexBuffer norm = mesh.getBuffer(Type.Normal);
                    FloatBuffer nb = (FloatBuffer) norm.getData();
                    FloatBuffer bnb = (FloatBuffer) bindNorm.getData();
                    nb.clear();
                    bnb.clear();
                    nb.put(bnb).clear();
                }

                //resetting bind tangents if there is a bind tangent buffer
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
     * Provides access to the attachment node for a specific joint in the armature.
     * If an attachment node does not already exist for the named joint, one will be
     * created and attached to the scene graph. Models or effects attached to this
     * node will follow the motion of the corresponding bone.
     *
     * @param jointName the name of the joint
     * @return the attachments node of the joint
     */
    public Node getAttachmentsNode(String jointName) {
        Joint joint = armature.getJoint(jointName);
        if (joint == null) {
            throw new IllegalArgumentException(
                    "Given joint name '" + jointName + "' does not exist in the armature.");
        }

        updateAnimationTargets(spatial);
        int jointIndex = armature.getJointIndex(joint);
        Node attachNode = joint.getAttachmentsNode(jointIndex, targets);

        // Determine the appropriate parent for the attachment node.
        Node parent;
        if (spatial instanceof Node) {
            parent = (Node) spatial; // the usual case
        } else {
            parent = spatial.getParent();
        }
        parent.attachChild(attachNode);

        return attachNode;
    }

    /**
     * Returns the armature associated with this skinning control.
     *
     * @return The pre-existing `Armature` instance.
     */
    public Armature getArmature() {
        return armature;
    }

    /**
     * Returns an array containing all the target meshes that this control
     * is currently affecting.
     *
     * @return A new array of `Mesh` objects.
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
     * Applies software skinning transformations to the given mesh using the
     * provided bone offset matrices.
     *
     * @param mesh           The mesh to deform.
     * @param offsetMatrices The array of transformation matrices for each bone.
     */
    private void applySoftwareSkinning(Mesh mesh, Matrix4f[] offsetMatrices) {

        VertexBuffer tb = mesh.getBuffer(Type.Tangent);
        if (tb == null) {
            // if there are no tangents use the classic skinning
            applySkinning(mesh, offsetMatrices);
        } else {
            // if there are tangents use the skinning with tangents
            applySkinningTangents(mesh, offsetMatrices, tb);
        }
    }

    /**
     * Applies skinning transformations to a mesh's position and normal buffers.
     * This method iterates through each vertex, applies the weighted sum of
     * bone transformations, and updates the vertex buffers.
     *
     * @param mesh           The mesh to apply skinning to.
     * @param offsetMatrices The bone offset matrices to use for transformation.
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
     * Applies skinning transformations to a mesh's position, normal, and tangent buffers.
     * This method is specifically designed for meshes that include tangent data,
     * ensuring proper deformation of tangents alongside positions and normals.
     *
     * @param mesh           The mesh to apply skinning to.
     * @param offsetMatrices The bone offset matrices to use for transformation.
     * @param tb             The tangent `VertexBuffer`.
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

        FloatBuffer fnb = (nb == null) ? null : (FloatBuffer) nb.getData();
        if (fnb != null) {
            fnb.rewind();
        }

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
            if (fnb != null) {
                fnb.get(normBuf, 0, bufLength);
            }
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
            if (fnb != null) {
                fnb.position(fnb.position() - bufLength);
                fnb.put(normBuf, 0, bufLength);
            }
            ftb.position(ftb.position() - tanLength);
            ftb.put(tanBuf, 0, tanLength);
        }

        vars.release();

        vb.updateData(fvb);
        if (nb != null) {
            nb.updateData(fnb);
        }
        tb.updateData(ftb);
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
    }

    /**
     * Update the lists of animation targets.
     *
     * @param spatial the controlled spatial
     */
    private void updateAnimationTargets(Spatial spatial) {
        targets.clear();
        collectAnimatedGeometries(spatial);
    }

}
