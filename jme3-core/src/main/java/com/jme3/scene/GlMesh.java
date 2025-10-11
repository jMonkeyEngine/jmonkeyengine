/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.scene;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.bih.BIHTree;
import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.GlVertexBuffer.*;
import com.jme3.scene.mesh.*;
import com.jme3.util.*;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.AttributeModifier;
import com.jme3.vulkan.mesh.AccessRate;

import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;

/**
 * <code>Mesh</code> is used to store rendering data.
 * <p>
 * All visible elements in a scene are represented by meshes.
 * Meshes may contain three types of geometric primitives:
 * <ul>
 * <li>Points - Every vertex represents a single point in space.
 * <li>Lines - 2 vertices represent a line segment, with the width specified
 * via {@link com.jme3.material.GlMaterial#getAdditionalRenderState()} and {@link RenderState#setLineWidth(float)}.</li>
 * <li>Triangles - 3 vertices represent a solid triangle primitive. </li>
 * </ul>
 *
 * @author Kirill Vainer
 */
public class GlMesh implements Mesh, Savable, Cloneable, JmeCloneable {

    /**
     * The mode of the Mesh specifies both the type of primitive represented
     * by the mesh and how the data should be interpreted.
     */
    public enum Mode {
        /**
         * A primitive is a single point in space. The size of {@link Mode#Points points} are
         * determined via the vertex shader's <code>gl_PointSize</code> output.
         */
        Points(true),
        /**
         * A primitive is a line segment. Every two vertices specify
         * a single line. {@link com.jme3.material.GlMaterial#getAdditionalRenderState()}
         * and {@link RenderState#setLineWidth(float)} can be used
         * to set the width of the lines.
         */
        Lines(true),
        /**
         * A primitive is a line segment. The first two vertices specify
         * a single line, while subsequent vertices are combined with the
         * previous vertex to make a line. {@link com.jme3.material.GlMaterial#getAdditionalRenderState()}
         * and {@link RenderState#setLineWidth(float)} can
         * be used to set the width of the lines.
         */
        LineStrip(false),
        /**
         * Identical to {@link #LineStrip} except that at the end
         * the last vertex is connected with the first to form a line.
         * {@link com.jme3.material.GlMaterial#getAdditionalRenderState()}
         * and {@link RenderState#setLineWidth(float)} can be used
         * to set the width of the lines.
         */
        LineLoop(false),
        /**
         * A primitive is a triangle. Each 3 vertices specify a single
         * triangle.
         */
        Triangles(true),
        /**
         * Similar to {@link #Triangles}, the first 3 vertices
         * specify a triangle, while subsequent vertices are combined with
         * the previous two to form a triangle.
         */
        TriangleStrip(false),
        /**
         * Similar to {@link #Triangles}, the first 3 vertices
         * specify a triangle, each 2 subsequent vertices are combined
         * with the very first vertex to make a triangle.
         */
        TriangleFan(false),
        /**
         * A combination of various triangle modes. It is best to avoid
         * using this mode as it may not be supported by all renderers.
         * The {@link GlMesh#setModeStart(int[]) mode start points} and
         * {@link GlMesh#setElementLengths(int[]) element lengths} must
         * be specified for this mode.
         */
        Hybrid(false),
        /**
         * Used for Tessellation only. Requires to set the number of vertices
         * for each patch (default is 3 for triangle tessellation)
         */
        Patch(true);

        private boolean listMode = false;

        private Mode(boolean listMode) {
            this.listMode = listMode;
        }

        /**
         * Returns true if the specified mode is a list mode (meaning
         * ,it specifies the indices as a linear list and not some special
         * format).
         * Will return true for the types {@link #Points}, {@link #Lines} and
         * {@link #Triangles}.
         *
         * @return true if the mode is a list type mode
         */
        public boolean isListMode() {
            return listMode;
        }
    }

    /**
     * Default Variables
     */
    private static final int DEFAULT_VERTEX_ARRAY_ID = -1;
    private static final CollisionData DEFAULT_COLLISION_TREE = null;

    private static final float DEFAULT_POINT_SIZE = 1.0f;
    private static final float DEFAULT_LINE_WIDTH = 1.0f;

    private static final int DEFAULT_VERT_COUNT = -1;
    private static final int DEFAULT_ELEMENT_COUNT = -1;
    private static final int DEFAULT_INSTANCE_COUNT = -1;
    private static final int DEFAULT_PATCH_VERTEX_COUNT = 3;
    private static final int DEFAULT_MAX_NUM_WEIGHTS = -1;

    /**
     * The bounding volume that contains the mesh entirely.
     * By default a BoundingBox (AABB).
     */
    private BoundingVolume meshBound = new BoundingBox();

    private CollisionData collisionTree = DEFAULT_COLLISION_TREE;

    private SafeArrayList<GlVertexBuffer> buffersList = new SafeArrayList<>(GlVertexBuffer.class);
    private IntMap<GlVertexBuffer> buffers = new IntMap<>();
    private GlVertexBuffer[] lodLevels;

    private float pointSize = DEFAULT_POINT_SIZE;
    private float lineWidth = DEFAULT_LINE_WIDTH;

    private transient int vertexArrayID = DEFAULT_VERTEX_ARRAY_ID;

    private int vertCount = DEFAULT_VERT_COUNT;
    private int elementCount = DEFAULT_ELEMENT_COUNT;
    private int instanceCount = DEFAULT_INSTANCE_COUNT;
    private int patchVertexCount = DEFAULT_PATCH_VERTEX_COUNT; //only used for tessellation
    private int maxNumWeights = DEFAULT_MAX_NUM_WEIGHTS; // only if using skeletal animation

    private int[] elementLengths;
    private int[] modeStart;

    private Mode mode = Mode.Triangles;
    private SafeArrayList<MorphTarget> morphTargets;

    /**
     * Creates a new mesh with no {@link GlVertexBuffer vertex buffers}.
     */
    public GlMesh() {
    }

    /**
     * Create a shallow clone of this Mesh. The {@link GlVertexBuffer vertex
     * buffers} are shared between this and the clone mesh, the rest
     * of the data is cloned.
     *
     * @return A shallow clone of the mesh
     */
    @Override
    public GlMesh clone() {
        try {
            GlMesh clone = (GlMesh) super.clone();
            clone.meshBound = meshBound.clone();
            clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.buffers = buffers.clone();
            clone.buffersList = new SafeArrayList<>(GlVertexBuffer.class, buffersList);
            clone.vertexArrayID = DEFAULT_VERTEX_ARRAY_ID;
            if (elementLengths != null) {
                clone.elementLengths = elementLengths.clone();
            }
            if (modeStart != null) {
                clone.modeStart = modeStart.clone();
            }
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Creates a deep clone of this mesh.
     * The {@link GlVertexBuffer vertex buffers} and the data inside them
     * is cloned.
     *
     * @return a deep clone of this mesh.
     */
    public GlMesh deepClone() {
        try {
            GlMesh clone = (GlMesh) super.clone();
            clone.meshBound = meshBound != null ? meshBound.clone() : null;

            // TODO: Collision tree cloning
            //clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.collisionTree = DEFAULT_COLLISION_TREE; // it will get re-generated in any case

            clone.buffers = new IntMap<>();
            clone.buffersList = new SafeArrayList<>(GlVertexBuffer.class);
            for (GlVertexBuffer vb : buffersList.getArray()) {
                GlVertexBuffer bufClone = vb.clone();
                clone.buffers.put(vb.getBufferType().ordinal(), bufClone);
                clone.buffersList.add(bufClone);
            }

            clone.vertexArrayID = DEFAULT_VERTEX_ARRAY_ID;
            clone.vertCount = vertCount;
            clone.elementCount = elementCount;
            clone.instanceCount = instanceCount;

            // although this could change
            // if the bone weight/index buffers are modified
            clone.maxNumWeights = maxNumWeights;

            clone.elementLengths = elementLengths != null ? elementLengths.clone() : null;
            clone.modeStart = modeStart != null ? modeStart.clone() : null;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Clone the mesh for animation use.
     * This creates a shallow clone of the mesh, sharing most
     * of the {@link GlVertexBuffer vertex buffer} data, however the
     * {@link Type#Position}, {@link Type#Normal}, and {@link Type#Tangent} buffers
     * are deeply cloned.
     *
     * @return A clone of the mesh for animation use.
     */
    public GlMesh cloneForAnim() {
        GlMesh clone = clone();
        if (getBuffer(Type.BindPosePosition) != null) {
            GlVertexBuffer oldPos = getBuffer(Type.Position);

            // NOTE: creates deep clone
            GlVertexBuffer newPos = oldPos.clone();
            clone.clearBuffer(Type.Position);
            clone.setBuffer(newPos);

            if (getBuffer(Type.BindPoseNormal) != null) {
                GlVertexBuffer oldNorm = getBuffer(Type.Normal);
                GlVertexBuffer newNorm = oldNorm.clone();
                clone.clearBuffer(Type.Normal);
                clone.setBuffer(newNorm);

                if (getBuffer(Type.BindPoseTangent) != null) {
                    GlVertexBuffer oldTang = getBuffer(Type.Tangent);
                    GlVertexBuffer newTang = oldTang.clone();
                    clone.clearBuffer(Type.Tangent);
                    clone.setBuffer(newTang);
                }
            }
        }
        return clone;
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public GlMesh jmeClone() {
        try {
            GlMesh clone = (GlMesh) super.clone();
            clone.vertexArrayID = DEFAULT_VERTEX_ARRAY_ID;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        // Probably could clone this now but it will get regenerated anyway.
        this.collisionTree = DEFAULT_COLLISION_TREE;

        this.meshBound = cloner.clone(meshBound);
        this.buffersList = cloner.clone(buffersList);
        this.buffers = cloner.clone(buffers);
        this.lodLevels = cloner.clone(lodLevels);
        this.elementLengths = cloner.clone(elementLengths);
        this.modeStart = cloner.clone(modeStart);
    }

    /**
     * @param forSoftwareAnim ignored
     * @deprecated use generateBindPose();
     */
    @Deprecated
    public void generateBindPose(boolean forSoftwareAnim) {
        generateBindPose();
    }

    /**
     * Generates the {@link Type#BindPosePosition}, {@link Type#BindPoseNormal},
     * and {@link Type#BindPoseTangent}
     * buffers for this mesh by duplicating them based on the position and normal
     * buffers already set on the mesh.
     * This method does nothing if the mesh has no bone weight or index
     * buffers.
     */
    public void generateBindPose() {
        GlVertexBuffer pos = getBuffer(Type.Position);
        if (pos == null || getBuffer(Type.BoneIndex) == null) {
            // ignore, this mesh doesn't have positional data
            // or it doesn't have bone-vertex assignments, so it's not animated
            return;
        }

        GlVertexBuffer bindPos = new GlVertexBuffer(Type.BindPosePosition);
        bindPos.setupData(Usage.CpuOnly,
                pos.getNumComponents(),
                pos.getFormat(),
                BufferUtils.clone(pos.getData()));
        setBuffer(bindPos);

        // XXX: note that this method also sets stream mode
        // so that animation is faster. this is not needed for hardware skinning
        pos.setUsage(Usage.Stream);

        GlVertexBuffer norm = getBuffer(Type.Normal);
        if (norm != null) {
            GlVertexBuffer bindNorm = new GlVertexBuffer(Type.BindPoseNormal);
            bindNorm.setupData(Usage.CpuOnly,
                    norm.getNumComponents(),
                    norm.getFormat(),
                    BufferUtils.clone(norm.getData()));
            setBuffer(bindNorm);
            norm.setUsage(Usage.Stream);
        }

        GlVertexBuffer tangents = getBuffer(Type.Tangent);
        if (tangents != null) {
            GlVertexBuffer bindTangents = new GlVertexBuffer(Type.BindPoseTangent);
            bindTangents.setupData(Usage.CpuOnly,
                    tangents.getNumComponents(),
                    tangents.getFormat(),
                    BufferUtils.clone(tangents.getData()));
            setBuffer(bindTangents);
            tangents.setUsage(Usage.Stream);
        }// else hardware setup does nothing, mesh already in bind pose

    }

    /**
     * Prepares the mesh for software skinning by converting the bone index
     * and weight buffers to heap buffers.
     *
     * @param forSoftwareAnim Should be true to enable the conversion.
     */
    public void prepareForAnim(boolean forSoftwareAnim) {
        if (forSoftwareAnim) {
            // convert indices to ubytes on the heap
            GlVertexBuffer indices = getBuffer(Type.BoneIndex);
            if (!indices.getData().hasArray()) {
                if (indices.getFormat() == Format.UnsignedByte) {
                    ByteBuffer originalIndex = (ByteBuffer) indices.getData();
                    ByteBuffer arrayIndex = ByteBuffer.allocate(originalIndex.capacity());
                    originalIndex.clear();
                    arrayIndex.put(originalIndex);
                    indices.updateData(arrayIndex);
                } else {
                    //bone indices can be stored in an UnsignedShort buffer
                    ShortBuffer originalIndex = (ShortBuffer) indices.getData();
                    ShortBuffer arrayIndex = ShortBuffer.allocate(originalIndex.capacity());
                    originalIndex.clear();
                    arrayIndex.put(originalIndex);
                    indices.updateData(arrayIndex);
                }
            }
            indices.setUsage(Usage.CpuOnly);

            // convert weights on the heap
            GlVertexBuffer weights = getBuffer(Type.BoneWeight);
            if (!weights.getData().hasArray()) {
                FloatBuffer originalWeight = (FloatBuffer) weights.getData();
                FloatBuffer arrayWeight = FloatBuffer.allocate(originalWeight.capacity());
                originalWeight.clear();
                arrayWeight.put(originalWeight);
                weights.updateData(arrayWeight);
            }
            weights.setUsage(Usage.CpuOnly);
            // position, normal, and tangent buffers to be in "Stream" mode
            GlVertexBuffer positions = getBuffer(Type.Position);
            GlVertexBuffer normals = getBuffer(Type.Normal);
            GlVertexBuffer tangents = getBuffer(Type.Tangent);
            positions.setUsage(Usage.Stream);
            if (normals != null) {
                normals.setUsage(Usage.Stream);
            }
            if (tangents != null) {
                tangents.setUsage(Usage.Stream);
            }
        } else {
            //if HWBoneIndex and HWBoneWeight are empty, we setup them as direct
            //buffers with software anim buffers data
            GlVertexBuffer indicesHW = getBuffer(Type.HWBoneIndex);
            Buffer result;
            if (indicesHW.getData() == null) {
                GlVertexBuffer indices = getBuffer(Type.BoneIndex);
                if (indices.getFormat() == Format.UnsignedByte) {
                    ByteBuffer originalIndex = (ByteBuffer) indices.getData();
                    ByteBuffer directIndex
                            = BufferUtils.createByteBuffer(originalIndex.capacity());
                    originalIndex.clear();
                    directIndex.put(originalIndex);
                    result = directIndex;
                } else {
                    //bone indices can be stored in an UnsignedShort buffer
                    ShortBuffer originalIndex = (ShortBuffer) indices.getData();
                    ShortBuffer directIndex
                            = BufferUtils.createShortBuffer(originalIndex.capacity());
                    originalIndex.clear();
                    directIndex.put(originalIndex);
                    result = directIndex;
                }
                indicesHW.setupData(Usage.Static, indices.getNumComponents(),
                        indices.getFormat(), result);
            }

            GlVertexBuffer weightsHW = getBuffer(Type.HWBoneWeight);
            if (weightsHW.getData() == null) {
                GlVertexBuffer weights = getBuffer(Type.BoneWeight);
                FloatBuffer originalWeight = (FloatBuffer) weights.getData();
                FloatBuffer directWeight
                        = BufferUtils.createFloatBuffer(originalWeight.capacity());
                originalWeight.clear();
                directWeight.put(originalWeight);
                weightsHW.setupData(Usage.Static, weights.getNumComponents(),
                        weights.getFormat(), directWeight);
            }

            // position, normal, and tangent buffers to be in "Static" mode
            GlVertexBuffer positions = getBuffer(Type.Position);
            GlVertexBuffer normals = getBuffer(Type.Normal);
            GlVertexBuffer tangents = getBuffer(Type.Tangent);

            GlVertexBuffer positionsBP = getBuffer(Type.BindPosePosition);
            GlVertexBuffer normalsBP = getBuffer(Type.BindPoseNormal);
            GlVertexBuffer tangentsBP = getBuffer(Type.BindPoseTangent);

            positions.setUsage(Usage.Static);
            positionsBP.copyElements(0, positions, 0, positionsBP.getNumElements());
            positions.setUpdateNeeded();

            if (normals != null) {
                normals.setUsage(Usage.Static);
                normalsBP.copyElements(0, normals, 0, normalsBP.getNumElements());
                normals.setUpdateNeeded();
            }

            if (tangents != null) {
                tangents.setUsage(Usage.Static);
                tangentsBP.copyElements(0, tangents, 0, tangentsBP.getNumElements());
                tangents.setUpdateNeeded();
            }
        }
    }

    /**
     * Set the LOD (level of detail) index buffers on this mesh.
     *
     * @param lodLevels The LOD levels to set
     */
    public void setLodLevels(GlVertexBuffer[] lodLevels) {
        this.lodLevels = lodLevels;
    }

    /**
     * @return The number of LOD levels set on this mesh, including the main
     * index buffer, returns zero if there are no lod levels.
     */
    @Override
    public int getNumLodLevels() {
        return lodLevels != null ? lodLevels.length : 0;
    }

    /**
     * Returns the lod level at the given index.
     *
     * @param lod The lod level index, this does not include
     * the main index buffer.
     * @return The LOD index buffer at the index
     *
     * @throws IndexOutOfBoundsException If the index is outside of the
     * range [0, {@link #getNumLodLevels()}].
     *
     * @see #setLodLevels(GlVertexBuffer[])
     */
    public GlVertexBuffer getLodLevel(int lod) {
        return lodLevels[lod];
    }

    /**
     * Get the element lengths for {@link Mode#Hybrid} mesh mode.
     *
     * @return element lengths
     */
    public int[] getElementLengths() {
        return elementLengths;
    }

    /**
     * Set the element lengths for {@link Mode#Hybrid} mesh mode.
     *
     * @param elementLengths The element lengths to set
     */
    public void setElementLengths(int[] elementLengths) {
        this.elementLengths = elementLengths;
    }

    /**
     * Set the mode start indices for {@link Mode#Hybrid} mesh mode.
     *
     * @return mode start indices
     */
    public int[] getModeStart() {
        return modeStart;
    }

    /**
     * Get the mode start indices for {@link Mode#Hybrid} mesh mode.
     *
     * @param modeStart the pre-existing array
     */
    public void setModeStart(int[] modeStart) {
        this.modeStart = modeStart;
    }

    /**
     * Returns the mesh mode
     *
     * @return the mesh mode
     *
     * @see #setMode(GlMesh.Mode)
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Change the Mesh's mode. By default the mode is {@link Mode#Triangles}.
     *
     * @param mode The new mode to set
     *
     * @see Mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        updateCounts();
    }

    /**
     * Returns the maximum number of weights per vertex on this mesh.
     *
     * @return maximum number of weights per vertex
     *
     * @see #setMaxNumWeights(int)
     */
    public int getMaxNumWeights() {
        return maxNumWeights;
    }

    /**
     * Set the maximum number of weights per vertex on this mesh.
     * Only relevant if this mesh has bone index/weight buffers.
     * This value should be between 0 and 4.
     *
     * @param maxNumWeights the desired number (between 0 and 4, inclusive)
     */
    public void setMaxNumWeights(int maxNumWeights) {
        this.maxNumWeights = maxNumWeights;
    }

    /**
     * @deprecated Always returns <code>1.0</code> since point size is
     * determined in the vertex shader.
     *
     * @return <code>1.0</code>
     */
    @Deprecated
    public float getPointSize() {
        return DEFAULT_POINT_SIZE;
    }

    /**
     * Returns the line width for line meshes.
     *
     * @return the line width
     * @deprecated use {@link com.jme3.material.GlMaterial#getAdditionalRenderState()}
     *             and {@link RenderState#getLineWidth()}
     */
    @Deprecated
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Specify the line width for meshes of the line modes, such
     * as {@link Mode#Lines}. The line width is specified as on-screen pixels,
     * the default value is 1.0.
     *
     * @param lineWidth The line width
     * @deprecated use {@link com.jme3.material.GlMaterial#getAdditionalRenderState()}
     *             and {@link RenderState#setLineWidth(float)}
     */
    @Deprecated
    public void setLineWidth(float lineWidth) {
        if (lineWidth < 1f) {
            throw new IllegalArgumentException("lineWidth must be greater than or equal to 1.0");
        }
        this.lineWidth = lineWidth;
    }

    /**
     * Indicates to the GPU that this mesh will not be modified (a hint).
     * Sets the usage mode to {@link Usage#Static}
     * for all {@link GlVertexBuffer vertex buffers} on this Mesh.
     */
    public void setStatic() {
        for (GlVertexBuffer vb : buffersList.getArray()) {
            vb.setUsage(Usage.Static);
        }
    }

    /**
     * Indicates to the GPU that this mesh will be modified occasionally (a hint).
     * Sets the usage mode to {@link Usage#Dynamic}
     * for all {@link GlVertexBuffer vertex buffers} on this Mesh.
     */
    public void setDynamic() {
        for (GlVertexBuffer vb : buffersList.getArray()) {
            vb.setUsage(Usage.Dynamic);
        }
    }

    /**
     * Indicates to the GPU that this mesh will be modified every frame (a hint).
     * Sets the usage mode to {@link Usage#Stream}
     * for all {@link GlVertexBuffer vertex buffers} on this Mesh.
     */
    public void setStreamed() {
        for (GlVertexBuffer vb : buffersList.getArray()) {
            vb.setUsage(Usage.Stream);
        }
    }

    /**
     * Interleaves the data in this mesh. This operation cannot be reversed.
     * Some GPUs may prefer the data in this format, however it is a good idea
     * to <em>avoid</em> using this method as it disables some engine features.
     */
    @Deprecated
    public void setInterleaved() {
        ArrayList<GlVertexBuffer> vbs = new ArrayList<>();
        vbs.addAll(buffersList);

//        ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>(buffers.values());
        // index buffer not included when interleaving
        vbs.remove(getBuffer(Type.Index));

        int stride = 0; // aka bytes per vertex
        for (int i = 0; i < vbs.size(); i++) {
            GlVertexBuffer vb = vbs.get(i);
//            if (vb.getFormat() != Format.Float){
//                throw new UnsupportedOperationException("Cannot interleave vertex buffer.\n" +
//                                                        "Contains not-float data.");
//            }
            stride += vb.componentsLength;
            vb.getData().clear(); // reset position & limit (used later)
        }

        GlVertexBuffer allData = new GlVertexBuffer(Type.InterleavedData);
        ByteBuffer dataBuf = BufferUtils.createByteBuffer(stride * getVertexCount());
        allData.setupData(Usage.Static, 1, Format.UnsignedByte, dataBuf);

        // adding buffer directly so that no update counts is forced
        buffers.put(Type.InterleavedData.ordinal(), allData);
        buffersList.add(allData);

        for (int vert = 0; vert < getVertexCount(); vert++) {
            for (int i = 0; i < vbs.size(); i++) {
                GlVertexBuffer vb = vbs.get(i);
                switch (vb.getFormat()) {
                    case Float:
                        FloatBuffer fb = (FloatBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++) {
                            dataBuf.putFloat(fb.get());
                        }
                        break;
                    case Byte:
                    case UnsignedByte:
                        ByteBuffer bb = (ByteBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++) {
                            dataBuf.put(bb.get());
                        }
                        break;
                    case Half:
                    case Short:
                    case UnsignedShort:
                        ShortBuffer sb = (ShortBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++) {
                            dataBuf.putShort(sb.get());
                        }
                        break;
                    case Int:
                    case UnsignedInt:
                        IntBuffer ib = (IntBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++) {
                            dataBuf.putInt(ib.get());
                        }
                        break;
                    case Double:
                        DoubleBuffer db = (DoubleBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++) {
                            dataBuf.putDouble(db.get());
                        }
                        break;
                }
            }
        }

        int offset = 0;
        for (GlVertexBuffer vb : vbs) {
            vb.setOffset(offset);
            vb.setStride(stride);

            vb.updateData(null);
            //vb.setupData(vb.usage, vb.components, vb.format, null);
            offset += vb.componentsLength;
        }
    }

    private int computeNumElements(int bufSize) {
        switch (mode) {
            case Triangles:
                return bufSize / 3;
            case TriangleFan:
            case TriangleStrip:
                return bufSize - 2;
            case Points:
                return bufSize;
            case Lines:
                return bufSize / 2;
            case LineLoop:
                return bufSize;
            case LineStrip:
                return bufSize - 1;
            case Patch:
                return bufSize / patchVertexCount;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private int computeInstanceCount() {
        // Whatever the max of the base instance counts
        int max = 0;
        for (GlVertexBuffer vb : buffersList) {
            if (vb.getBaseInstanceCount() > max) {
                max = vb.getBaseInstanceCount();
            }
        }
        return max;
    }

    /**
     * Update the {@link #getVertexCount() vertex} and
     * {@link #getTriangleCount() triangle} counts for this mesh
     * based on the current data. This method should be called
     * after the {@link Buffer#capacity() capacities} of the mesh's
     * {@link GlVertexBuffer vertex buffers} has been altered.
     *
     * @throws IllegalStateException If this mesh is in
     * {@link #setInterleaved() interleaved} format.
     */
    public void updateCounts() {
        if (getBuffer(Type.InterleavedData) != null) {
            throw new IllegalStateException("Should update counts before interleave");
        }

        GlVertexBuffer pb = getBuffer(Type.Position);
        GlVertexBuffer ib = getBuffer(Type.Index);
        if (pb != null) {
            vertCount = pb.getData().limit() / pb.getNumComponents();
        }
        if (ib != null) {
            elementCount = computeNumElements(ib.getData().limit());
        } else {
            elementCount = computeNumElements(vertCount);
        }
        instanceCount = computeInstanceCount();
    }

    /**
     * Returns the triangle count for the given LOD level.
     *
     * @param lod The lod level to look up
     * @return The triangle count for that LOD level
     */
    @Override
    public int getTriangleCount(int lod) {
        if (lodLevels != null) {
            if (lod < 0) {
                throw new IllegalArgumentException("LOD level cannot be < 0");
            }

            if (lod >= lodLevels.length) {
                throw new IllegalArgumentException("LOD level " + lod + " does not exist!");
            }

            return computeNumElements(lodLevels[lod].getData().limit());
        } else if (lod == 0) {
            return elementCount;
        } else {
            throw new IllegalArgumentException("There are no LOD levels on the mesh!");
        }
    }

    /**
     * Returns how many triangles or elements are on this Mesh.
     * This value is only updated when {@link #updateCounts() } is called.
     * If the mesh mode is not a triangle mode, then this returns the
     * number of elements/primitives, e.g. how many lines or how many points,
     * instead of how many triangles.
     *
     * @return how many triangles/elements are on this Mesh.
     */
    public int getTriangleCount() {
        return elementCount;
    }

    @Override
    public void render(RenderManager renderManager, CommandBuffer cmd, Geometry geometry, Material material) {

    }

    @Override
    public AttributeModifier modify(String attributeName) {
        return null;
    }

    @Override
    public void setAccessFrequency(String attributeName, AccessRate hint) {

    }

    @Override
    public void setVertexCount(int vertices) {

    }

    @Override
    public void setTriangleCount(int lod, int triangles) {

    }

    @Override
    public void setInstanceCount(int instances) {

    }

    /**
     * Returns the number of vertices on this mesh.
     * The value is computed based on the position buffer, which
     * must be set on all meshes.
     *
     * @return Number of vertices on the mesh
     */
    @Override
    public int getVertexCount() {
        return vertCount;
    }

    /**
     * Returns the number of instances this mesh contains.  The instance
     * count is based on any VertexBuffers with instancing set.
     *
     * @return the number of instances
     */
    @Override
    public int getInstanceCount() {
        return instanceCount;
    }

    @Override
    public int collideWith(Collidable other, Geometry geometry, CollisionResults results) {
        return 0;
    }

    /**
     * Gets the triangle vertex positions at the given triangle index
     * and stores them into the v1, v2, v3 arguments.
     *
     * @param index The index of the triangle.
     * Should be between 0 and {@link #getTriangleCount()}.
     *
     * @param v1 Vector to contain first vertex position
     * @param v2 Vector to contain second vertex position
     * @param v3 Vector to contain third vertex position
     */
    public void getTriangle(int index, Vector3f v1, Vector3f v2, Vector3f v3) {
        GlVertexBuffer pb = getBuffer(Type.Position);
        IndexBuffer ib = getIndicesAsList();
        if (pb != null && pb.getFormat() == Format.Float && pb.getNumComponents() == 3) {
            FloatBuffer fpb = (FloatBuffer) pb.getData();

            // acquire triangle's vertex indices
            int vertIndex = index * 3;
            int vert1 = ib.get(vertIndex);
            int vert2 = ib.get(vertIndex + 1);
            int vert3 = ib.get(vertIndex + 2);

            BufferUtils.populateFromBuffer(v1, fpb, vert1);
            BufferUtils.populateFromBuffer(v2, fpb, vert2);
            BufferUtils.populateFromBuffer(v3, fpb, vert3);
        } else {
            throw new UnsupportedOperationException("Position buffer not set or "
                    + " has incompatible format");
        }
    }

    /**
     * Gets the triangle vertex positions at the given triangle index
     * and stores them into the {@link Triangle} argument.
     * Also sets the triangle index to the <code>index</code> argument.
     *
     * @param index The index of the triangle.
     * Should be between 0 and {@link #getTriangleCount()}.
     *
     * @param tri The triangle to store the positions in
     */
    public void getTriangle(int index, Triangle tri) {
        getTriangle(index, tri.get1(), tri.get2(), tri.get3());
        tri.setIndex(index);
        tri.setCenter(null); // invalidate previously cached centroid, if any
        tri.setNormal(null);
    }

    /**
     * Gets the triangle vertex indices at the given triangle index
     * and stores them into the given int array.
     *
     * @param index The index of the triangle.
     * Should be between 0 and {@link #getTriangleCount()}.
     *
     * @param indices Indices of the triangle's vertices
     */
    public void getTriangle(int index, int[] indices) {
        IndexBuffer ib = getIndicesAsList();

        // acquire triangle's vertex indices
        int vertIndex = index * 3;
        indices[0] = ib.get(vertIndex);
        indices[1] = ib.get(vertIndex + 1);
        indices[2] = ib.get(vertIndex + 2);
    }

    /**
     * Returns the mesh's VAO ID. Internal use only.
     *
     * @return the array ID
     */
    public int getId() {
        return vertexArrayID;
    }

    /**
     * Sets the mesh's VAO ID. Internal use only.
     *
     * @param id the array ID
     */
    public void setId(int id) {
        if (vertexArrayID != DEFAULT_VERTEX_ARRAY_ID) {
            throw new IllegalStateException("ID has already been set.");
        }

        vertexArrayID = id;
    }

    /**
     * Generates a collision tree for the mesh.
     * Called automatically by {@link #collideWith(com.jme3.collision.Collidable,
     * com.jme3.math.Matrix4f,
     * com.jme3.bounding.BoundingVolume,
     * com.jme3.collision.CollisionResults) }.
     */
    public void createCollisionData() {
        throw new UnsupportedOperationException("Collision tree not supported.");
//        BIHTree tree = new BIHTree(this);
//        tree.construct();
//        collisionTree = tree;
    }

    /**
     * Clears any previously generated collision data.  Use this if
     * the mesh has changed in some way that invalidates any previously
     * generated BIHTree.
     */
    public void clearCollisionData() {
        collisionTree = DEFAULT_COLLISION_TREE;
    }

    /**
     * Handles collision detection, internal use only.
     * User code should only use collideWith() on scene
     * graph elements such as {@link Spatial}s.
     *
     * @param other the other Collidable
     * @param worldMatrix the world matrix
     * @param worldBound the world bound
     * @param results storage for the results
     * @return the number of collisions detected (&ge;0)
     */
    public int collideWith(Collidable other,
                           Matrix4f worldMatrix,
                           BoundingVolume worldBound,
                           CollisionResults results) {

        switch (mode) {
            case Points:
            case Lines:
            case LineStrip:
            case LineLoop:
                /*
                 * Collisions can be detected only with triangles,
                 * and there are no triangles in this mesh.
                 */
                return 0;
        }

        if (getVertexCount() == 0) {
            return 0;
        }

        if (collisionTree == null) {
            createCollisionData();
        }

        return collisionTree.collideWith(other, worldMatrix, worldBound, results);
    }

    /**
     * Sets the {@link GlVertexBuffer} on the mesh.
     * This will update the vertex/triangle counts if needed.
     *
     * @param vb The buffer to set
     * @throws IllegalArgumentException If the buffer type is already set
     */
    public void setBuffer(GlVertexBuffer vb) {
        if (buffers.containsKey(vb.getBufferType().ordinal())) {
            throw new IllegalArgumentException("Buffer type already set: " + vb.getBufferType());
        }

        buffers.put(vb.getBufferType().ordinal(), vb);
        buffersList.add(vb);
        updateCounts();
    }

    /**
     * Unsets the {@link GlVertexBuffer} set on this mesh
     * with the given type. Does nothing if the vertex buffer type is not set
     * initially.
     *
     * @param type The buffer type to remove
     */
    public void clearBuffer(GlVertexBuffer.Type type) {
        GlVertexBuffer vb = buffers.remove(type.ordinal());
        if (vb != null) {
            buffersList.remove(vb);
            updateCounts();
        }
    }

    /**
     * Creates a {@link GlVertexBuffer} for the mesh or modifies
     * the existing one per the parameters given.
     *
     * @param type The type of the buffer
     * @param components Number of components
     * @param format Data format
     * @param buf The buffer data
     *
     * @throws UnsupportedOperationException If the buffer already set is
     * incompatible with the parameters given.
     */
    public void setBuffer(Type type, int components, Format format, Buffer buf) {
        GlVertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null) {
            vb = new GlVertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, format, buf);
            setBuffer(vb);
        } else {
            if (vb.getNumComponents() != components || vb.getFormat() != format) {
                throw new UnsupportedOperationException("The buffer already set "
                        + "is incompatible with the given parameters");
            }
            vb.updateData(buf);
            updateCounts();
        }
    }

    /**
     * Set a floating point {@link GlVertexBuffer} on the mesh.
     *
     * @param type The type of {@link GlVertexBuffer},
     * e.g. {@link Type#Position}, {@link Type#Normal}, etc.
     *
     * @param components Number of components on the vertex buffer, should
     * be between 1 and 4.
     *
     * @param buf The floating point data to contain
     */
    public void setBuffer(Type type, int components, FloatBuffer buf) {
        setBuffer(type, components, Format.Float, buf);
    }

    public void setBuffer(Type type, int components, float[] buf) {
        setBuffer(type, components, BufferUtils.createFloatBuffer(buf));
    }

    public void setBuffer(Type type, int components, IntBuffer buf) {
        setBuffer(type, components, Format.UnsignedInt, buf);
    }

    public void setBuffer(Type type, int components, int[] buf) {
        setBuffer(type, components, BufferUtils.createIntBuffer(buf));
    }

    public void setBuffer(Type type, int components, ShortBuffer buf) {
        setBuffer(type, components, Format.UnsignedShort, buf);
    }

    public void setBuffer(Type type, int components, byte[] buf) {
        setBuffer(type, components, BufferUtils.createByteBuffer(buf));
    }

    public void setBuffer(Type type, int components, ByteBuffer buf) {
        setBuffer(type, components, Format.UnsignedByte, buf);
    }

    public void setBuffer(Type type, int components, short[] buf) {
        setBuffer(type, components, BufferUtils.createShortBuffer(buf));
    }

    /**
     * Get the {@link GlVertexBuffer} stored on this mesh with the given
     * type.
     *
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set
     */
    public GlVertexBuffer getBuffer(Type type) {
        return buffers.get(type.ordinal());
    }

    /**
     * Get the {@link GlVertexBuffer} data stored on this mesh in float
     * format.
     *
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set
     */
    public FloatBuffer getFloatBuffer(Type type) {
        GlVertexBuffer vb = getBuffer(type);
        if (vb == null) {
            return null;
        }

        return (FloatBuffer) vb.getData();
    }

    /**
     * Get the {@link GlVertexBuffer} data stored on this mesh in short
     * format.
     *
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set
     */
    public ShortBuffer getShortBuffer(Type type) {
        GlVertexBuffer vb = getBuffer(type);
        if (vb == null) {
            return null;
        }

        return (ShortBuffer) vb.getData();
    }

    /**
     * Acquires an index buffer that will read the vertices on the mesh
     * as a list.
     *
     * @return A virtual or wrapped index buffer to read the data as a list
     */
    public IndexBuffer getIndicesAsList() {
        if (mode == Mode.Hybrid) {
            throw new UnsupportedOperationException("Hybrid mode not supported");
        }

        IndexBuffer ib = getIndexBuffer();
        if (ib != null) {
            if (mode.isListMode()) {
                // already in list mode
                return ib;
            } else {
                // not in list mode but it does have an index buffer
                // wrap it so the data is converted to list format
                return new WrappedIndexBuffer(this);
            }
        } else {
            // return a virtual index buffer that will supply
            // "fake" indices in list format
            return new VirtualIndexBuffer(vertCount, mode);
        }
    }

    /**
     * Get the index buffer for this mesh.
     * Will return <code>null</code> if no index buffer is set.
     *
     * @return The index buffer of this mesh.
     *
     * @see Type#Index
     */
    public IndexBuffer getIndexBuffer() {
        GlVertexBuffer vb = getBuffer(Type.Index);
        if (vb == null) {
            return null;
        }

        return IndexBuffer.wrapIndexBuffer(vb.getData());
    }

    /**
     * Extracts the vertex attributes from the given mesh into
     * this mesh, by using this mesh's {@link #getIndexBuffer() index buffer}
     * to index into the attributes of the other mesh.
     * Note that this will also change this mesh's index buffer so that
     * the references to the vertex data match the new indices.
     *
     * @param other The mesh to extract the vertex data from
     */
    public void extractVertexData(GlMesh other) {
        // Determine the number of unique vertices need to
        // be created. Also determine the mappings
        // between old indices to new indices (since we avoid duplicating
        // vertices, this is a map and not an array).
        GlVertexBuffer oldIdxBuf = getBuffer(Type.Index);
        IndexBuffer indexBuf = getIndexBuffer();
        int numIndices = indexBuf.size();

        IntMap<Integer> oldIndicesToNewIndices = new IntMap<>(numIndices);
        ArrayList<Integer> newIndicesToOldIndices = new ArrayList<>();
        int newIndex = 0;

        for (int i = 0; i < numIndices; i++) {
            int oldIndex = indexBuf.get(i);

            if (!oldIndicesToNewIndices.containsKey(oldIndex)) {
                // this vertex has not been added, so allocate a
                // new index for it and add it to the map
                oldIndicesToNewIndices.put(oldIndex, newIndex);
                newIndicesToOldIndices.add(oldIndex);

                // increment to have the next index
                newIndex++;
            }
        }

        // Number of unique verts to be created now available
        int newNumVerts = newIndicesToOldIndices.size();

        if (newIndex != newNumVerts) {
            throw new AssertionError();
        }

        // Create the new index buffer.
        // Do not overwrite the old one because we might be able to
        // convert from int index buffer to short index buffer
        IndexBuffer newIndexBuf;
        if (newNumVerts >= 65536) {
            newIndexBuf = new IndexIntBuffer(BufferUtils.createIntBuffer(numIndices));
        } else {
            newIndexBuf = new IndexShortBuffer(BufferUtils.createShortBuffer(numIndices));
        }

        for (int i = 0; i < numIndices; i++) {
            // Map the old indices to the new indices
            int oldIndex = indexBuf.get(i);
            newIndex = oldIndicesToNewIndices.get(oldIndex);

            newIndexBuf.put(i, newIndex);
        }

        GlVertexBuffer newIdxBuf = new GlVertexBuffer(Type.Index);
        newIdxBuf.setupData(oldIdxBuf.getUsage(),
                oldIdxBuf.getNumComponents(),
                newIndexBuf instanceof IndexIntBuffer ? Format.UnsignedInt : Format.UnsignedShort,
                newIndexBuf.getBuffer());
        clearBuffer(Type.Index);
        setBuffer(newIdxBuf);

        // Now, create the vertex buffers
        SafeArrayList<GlVertexBuffer> oldVertexData = other.getBufferList();
        for (GlVertexBuffer oldVb : oldVertexData) {
            if (oldVb.getBufferType() == GlVertexBuffer.Type.Index) {
                // ignore the index buffer
                continue;
            }

            GlVertexBuffer newVb = new GlVertexBuffer(oldVb.getBufferType());
            newVb.setNormalized(oldVb.isNormalized());
            //check for data before copying, some buffers are just empty shells
            //for caching purpose (HW skinning buffers), and will be filled when
            //needed
            if (oldVb.getData() != null) {
                // Create a new vertex buffer with similar configuration, but
                // with the capacity of number of unique vertices
                Buffer buffer = GlVertexBuffer.createBuffer(oldVb.getFormat(),
                        oldVb.getNumComponents(), newNumVerts);
                newVb.setupData(oldVb.getUsage(), oldVb.getNumComponents(),
                        oldVb.getFormat(), buffer);

                // Copy the vertex data from the old buffer into the new buffer
                for (int i = 0; i < newNumVerts; i++) {
                    int oldIndex = newIndicesToOldIndices.get(i);

                    // Copy the vertex attribute from the old index
                    // to the new index
                    oldVb.copyElement(oldIndex, newVb, i);
                }
            }

            // Set the buffer on the mesh
            clearBuffer(newVb.getBufferType());
            setBuffer(newVb);
        }

        // Copy max weights per vertex as well
        setMaxNumWeights(other.getMaxNumWeights());

        // The data has been copied over, update information
        updateCounts();
        updateBound();
    }

    /**
     * Scales the texture coordinate buffer on this mesh by the given scale
     * factor.
     * <p>
     * Note that values above 1 will cause the
     * texture to tile, while values below 1 will cause the texture
     * to stretch.
     * </p>
     *
     * @param scaleFactor The scale factor to scale by. Every texture
     * coordinate is multiplied by this vector to get the result.
     *
     * @throws IllegalStateException If there's no texture coordinate
     * buffer on the mesh
     * @throws UnsupportedOperationException If the texture coordinate
     * buffer is not in 2D float format.
     */
    public void scaleTextureCoordinates(Vector2f scaleFactor) {
        GlVertexBuffer tc = getBuffer(Type.TexCoord);
        if (tc == null) {
            throw new IllegalStateException("The mesh has no texture coordinates");
        }

        if (tc.getFormat() != GlVertexBuffer.Format.Float) {
            throw new UnsupportedOperationException("Only float texture coord format is supported");
        }

        if (tc.getNumComponents() != 2) {
            throw new UnsupportedOperationException("Only 2D texture coords are supported");
        }

        FloatBuffer fb = (FloatBuffer) tc.getData();
        fb.clear();
        for (int i = 0; i < fb.limit() / 2; i++) {
            float x = fb.get();
            float y = fb.get();
            fb.position(fb.position() - 2);
            x *= scaleFactor.getX();
            y *= scaleFactor.getY();
            fb.put(x).put(y);
        }
        fb.clear();
        tc.updateData(fb);
    }

    /**
     * Updates the bounding volume of this mesh.
     * The method does nothing if the mesh has no {@link Type#Position} buffer.
     * It is expected that the position buffer is a float buffer with 3 components.
     */
    @Override
    public void updateBound() {
        GlVertexBuffer posBuf = getBuffer(GlVertexBuffer.Type.Position);
        if (meshBound != null && posBuf != null) {
            meshBound.computeFromPoints((FloatBuffer) posBuf.getData());
        }
    }

    /**
     * Returns the {@link BoundingVolume} of this Mesh.
     * By default the bounding volume is a {@link BoundingBox}.
     *
     * @return the bounding volume of this mesh
     */
    @Override
    public BoundingVolume getBound() {
        return meshBound;
    }

    /**
     * Sets the {@link BoundingVolume} for this Mesh.
     * The bounding volume is recomputed by calling {@link #updateBound() }.
     *
     * @param modelBound The model bound to set
     */
    @Override
    public void setBound(BoundingVolume modelBound) {
        meshBound = modelBound;
    }

    /**
     * Returns a map of all {@link GlVertexBuffer vertex buffers} on this Mesh.
     * The integer key for the map is the {@link Enum#ordinal() ordinal}
     * of the vertex buffer's {@link Type}.
     * Note that the returned map is a reference to the map used internally,
     * modifying it will cause undefined results.
     *
     * @return map of vertex buffers on this mesh.
     */
    public IntMap<GlVertexBuffer> getBuffers() {
        return buffers;
    }

    /**
     * Returns a list of all {@link GlVertexBuffer vertex buffers} on this Mesh.
     * Using a list instead an IntMap via the {@link #getBuffers() } method is
     * better for iteration as there's no need to create an iterator instance.
     * Note that the returned list is a reference to the list used internally,
     * modifying it will cause undefined results.
     *
     * @return list of vertex buffers on this mesh.
     */
    public SafeArrayList<GlVertexBuffer> getBufferList() {
        return buffersList;
    }

    /**
     * Determines if the mesh uses bone animation.
     *
     * A mesh uses bone animation if it has bone index / weight buffers
     * such as {@link Type#BoneIndex} or {@link Type#HWBoneIndex}.
     *
     * @return true if the mesh uses bone animation, false otherwise
     */
    public boolean isAnimated() {
        return getBuffer(Type.BoneIndex) != null
                || getBuffer(Type.HWBoneIndex) != null;
    }

    /**
     * @deprecated use isAnimatedByJoint
     * @param boneIndex the bone's index in its skeleton
     * @return true if animated by that bone, otherwise false
     */
    @Deprecated
    public boolean isAnimatedByBone(int boneIndex) {
        return isAnimatedByJoint(boneIndex);
    }

    /**
     * Test whether the specified bone animates this mesh.
     *
     * @param jointIndex the bone's index in its skeleton
     * @return true if the specified bone animates this mesh, otherwise false
     */
    @Override
    public boolean isAnimatedByJoint(int jointIndex) {
        GlVertexBuffer biBuf = getBuffer(GlVertexBuffer.Type.BoneIndex);
        GlVertexBuffer wBuf = getBuffer(GlVertexBuffer.Type.BoneWeight);
        if (biBuf == null || wBuf == null) {
            return false; // no bone animation data
        }

        IndexBuffer boneIndexBuffer = IndexBuffer.wrapIndexBuffer(biBuf.getData());
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getData();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;
        /*
         * Test each vertex to determine whether the bone affects it.
         */
        int biByte = jointIndex;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                int bIndex = boneIndexBuffer.get();
                float weight = weightBuffer.get();
                if (wIndex < maxNumWeights && bIndex == biByte && weight != 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets the count of vertices used for each tessellation patch
     *
     * @param patchVertexCount the desired count
     */
    public void setPatchVertexCount(int patchVertexCount) {
        this.patchVertexCount = patchVertexCount;
    }

    /**
     * Gets the amount of vertices used for each patch;
     *
     * @return the count (&ge;0)
     */
    public int getPatchVertexCount() {
        return patchVertexCount;
    }

    public void addMorphTarget(MorphTarget target) {
        if (morphTargets == null) {
            morphTargets = new SafeArrayList<>(MorphTarget.class);
        }
        morphTargets.add(target);
    }

    /**
     * Remove the given MorphTarget from the Mesh
     * @param target The MorphTarget to remove
     * @return If the MorphTarget was removed
     */
    public boolean removeMorphTarget(MorphTarget target) {
        return morphTargets != null ? morphTargets.remove(target) : false;
    }

    /**
     * Remove the MorphTarget from the Mesh at the given index
     * @throws IndexOutOfBoundsException if the index outside the number of morph targets
     * @param index Index of the MorphTarget to remove
     * @return The MorphTarget that was removed
     */
    public MorphTarget removeMorphTarget(int index) {
        if (morphTargets == null) {
            throw new IndexOutOfBoundsException("Index:" + index + ", Size:0");
        }
        return morphTargets.remove(index);
    }

    /**
     * Get the MorphTarget at the given index
     * @throws IndexOutOfBoundsException if the index outside the number of morph targets
     * @param index The index of the morph target to get
     * @return The MorphTarget at the index
     */
    public MorphTarget getMorphTarget(int index) {
        if (morphTargets == null) {
            throw new IndexOutOfBoundsException("Index:" + index + ", Size:0");
        }
        return morphTargets.get(index);
    }

    public MorphTarget[] getMorphTargets() {
        if (morphTargets == null) {
            return new MorphTarget[0];
        } else {
            return morphTargets.getArray();
        }
    }

    /**
     * Get the name of all morphs in order.
     * Morphs without names will be null
     * @return an array
     */
    public String[] getMorphTargetNames() {
        MorphTarget[] nbMorphTargets = getMorphTargets();
        if (nbMorphTargets.length == 0) {
            return new String[0];
        }
        String[] targets = new String[nbMorphTargets.length];

        for (int index = 0; index < nbMorphTargets.length; index++) {
            targets[index] = nbMorphTargets[index].getName();
        }
        return targets;
    }

    public boolean hasMorphTargets() {
        return morphTargets != null && !morphTargets.isEmpty();
    }

    /**
     * Get the index of the morph that has the given name.
     *
     * @param morphName The name of the morph to search for
     * @return The index of the morph, or -1 if not found.
     */
    public int getMorphIndex(String morphName) {
        int index = -1;
        MorphTarget[] nbMorphTargets = getMorphTargets();
        for (int i = 0; i < nbMorphTargets.length; i++) {
            if (nbMorphTargets[i].getName().equals(morphName)) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);

        out.write(meshBound, "modelBound", null);
        out.write(vertCount, "vertCount", DEFAULT_VERT_COUNT);
        out.write(elementCount, "elementCount", DEFAULT_ELEMENT_COUNT);
        out.write(instanceCount, "instanceCount", DEFAULT_INSTANCE_COUNT);
        out.write(maxNumWeights, "max_num_weights", DEFAULT_MAX_NUM_WEIGHTS);
        out.write(mode, "mode", Mode.Triangles);
        out.write(collisionTree, "collisionTree", DEFAULT_COLLISION_TREE);
        out.write(elementLengths, "elementLengths", null);
        out.write(modeStart, "modeStart", null);
        out.write(pointSize, "pointSize", DEFAULT_POINT_SIZE);

        //Removing HW skinning buffers to not save them
        GlVertexBuffer hwBoneIndex = null;
        GlVertexBuffer hwBoneWeight = null;
        hwBoneIndex = getBuffer(Type.HWBoneIndex);
        if (hwBoneIndex != null) {
            buffers.remove(Type.HWBoneIndex.ordinal());
        }
        hwBoneWeight = getBuffer(Type.HWBoneWeight);
        if (hwBoneWeight != null) {
            buffers.remove(Type.HWBoneWeight.ordinal());
        }

        out.writeIntSavableMap(buffers, "buffers", null);

        //restoring Hw skinning buffers.
        if (hwBoneIndex != null) {
            buffers.put(hwBoneIndex.getBufferType().ordinal(), hwBoneIndex);
        }
        if (hwBoneWeight != null) {
            buffers.put(hwBoneWeight.getBufferType().ordinal(), hwBoneWeight);
        }

        out.write(lodLevels, "lodLevels", null);
        if (morphTargets != null) {
            out.writeSavableArrayList(new ArrayList(morphTargets), "morphTargets", null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        meshBound = (BoundingVolume) in.readSavable("modelBound", null);
        vertCount = in.readInt("vertCount", DEFAULT_VERT_COUNT);
        elementCount = in.readInt("elementCount", DEFAULT_ELEMENT_COUNT);
        instanceCount = in.readInt("instanceCount", DEFAULT_INSTANCE_COUNT);
        maxNumWeights = in.readInt("max_num_weights", DEFAULT_MAX_NUM_WEIGHTS);
        mode = in.readEnum("mode", Mode.class, Mode.Triangles);
        elementLengths = in.readIntArray("elementLengths", null);
        modeStart = in.readIntArray("modeStart", null);
        collisionTree = (BIHTree) in.readSavable("collisionTree", DEFAULT_COLLISION_TREE);
        elementLengths = in.readIntArray("elementLengths", null);
        modeStart = in.readIntArray("modeStart", null);
        pointSize = in.readFloat("pointSize", DEFAULT_POINT_SIZE);

//        in.readStringSavableMap("buffers", null);
        buffers = (IntMap<GlVertexBuffer>) in.readIntSavableMap("buffers", null);
        for (Entry<GlVertexBuffer> entry : buffers) {
            buffersList.add(entry.getValue());
        }

        //creating hw animation buffers empty so that they are put in the cache
        if (isAnimated()) {
            GlVertexBuffer hwBoneIndex = new GlVertexBuffer(Type.HWBoneIndex);
            hwBoneIndex.setUsage(Usage.CpuOnly);
            setBuffer(hwBoneIndex);
            GlVertexBuffer hwBoneWeight = new GlVertexBuffer(Type.HWBoneWeight);
            hwBoneWeight.setUsage(Usage.CpuOnly);
            setBuffer(hwBoneWeight);
        }

        Savable[] lodLevelsSavable = in.readSavableArray("lodLevels", null);
        if (lodLevelsSavable != null) {
            lodLevels = new GlVertexBuffer[lodLevelsSavable.length];
            System.arraycopy(lodLevelsSavable, 0, lodLevels, 0, lodLevels.length);
        }

        ArrayList<Savable> l = in.readSavableArrayList("morphTargets", null);
        if (l != null) {
            morphTargets = new SafeArrayList(MorphTarget.class, l);
        }
    }
}
