/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.material.RenderState;
import com.jme3.math.Matrix4f;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.*;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.SafeArrayList;
import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;

/**
 * <code>Mesh</code> is used to store rendering data.
 * <p>
 * All visible elements in a scene are represented by meshes.
 * Meshes may contain three types of geometric primitives:
 * <ul>
 * <li>Points - Every vertex represents a single point in space, 
 * the size of each point is specified via {@link Mesh#setPointSize(float) }.
 * Points can also be used for {@link RenderState#setPointSprite(boolean) point
 * sprite} mode.</li>
 * <li>Lines - 2 vertices represent a line segment, with the width specified
 * via {@link Mesh#setLineWidth(float) }.</li>
 * <li>Triangles - 3 vertices represent a solid triangle primitive. </li>
 * </ul>
 * 
 * @author Kirill Vainer
 */
public class Mesh implements Savable, Cloneable {

    /**
     * The mode of the Mesh specifies both the type of primitive represented
     * by the mesh and how the data should be interpreted.
     */
    public enum Mode {
        /**
         * A primitive is a single point in space. The size of the points 
         * can be specified with {@link Mesh#setPointSize(float) }.
         */
        Points(true),
        
        /**
         * A primitive is a line segment. Every two vertices specify
         * a single line. {@link Mesh#setLineWidth(float) } can be used 
         * to set the width of the lines.
         */
        Lines(true),
        
        /**
         * A primitive is a line segment. The first two vertices specify
         * a single line, while subsequent vertices are combined with the 
         * previous vertex to make a line. {@link Mesh#setLineWidth(float) } can 
         * be used to set the width of the lines.
         */
        LineStrip(false),
        
        /**
         * Identical to {@link #LineStrip} except that at the end
         * the last vertex is connected with the first to form a line.
         * {@link Mesh#setLineWidth(float) } can be used 
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
         * The {@link Mesh#setModeStart(int[]) mode start points} and
         * {@link Mesh#setElementLengths(int[]) element lengths} must 
         * be specified for this mode.
         */
        Hybrid(false);
        
        private boolean listMode = false;
        
        private Mode(boolean listMode){
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
        public boolean isListMode(){
            return listMode;
        }
    }

    /**
     * The bounding volume that contains the mesh entirely.
     * By default a BoundingBox (AABB).
     */
    private BoundingVolume meshBound =  new BoundingBox();

    private CollisionData collisionTree = null;

    private SafeArrayList<VertexBuffer> buffersList = new SafeArrayList<VertexBuffer>(VertexBuffer.class);
    private IntMap<VertexBuffer> buffers = new IntMap<VertexBuffer>();
    private VertexBuffer[] lodLevels;
    private float pointSize = 1;
    private float lineWidth = 1;

    private transient int vertexArrayID = -1;

    private int vertCount = -1;
    private int elementCount = -1;
    private int maxNumWeights = -1; // only if using skeletal animation

    private int[] elementLengths;
    private int[] modeStart;

    private Mode mode = Mode.Triangles;

    /**
     * Creates a new mesh with no {@link VertexBuffer vertex buffers}.
     */
    public Mesh(){
    }

    /**
     * Create a shallow clone of this Mesh. The {@link VertexBuffer vertex
     * buffers} are shared between this and the clone mesh, the rest
     * of the data is cloned.
     * 
     * @return A shallow clone of the mesh
     */
    @Override
    public Mesh clone() {
        try {
            Mesh clone = (Mesh) super.clone();
            clone.meshBound = meshBound.clone();
            clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.buffers = buffers.clone();
            clone.buffersList = new SafeArrayList<VertexBuffer>(VertexBuffer.class,buffersList);
            clone.vertexArrayID = -1;
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
     * The {@link VertexBuffer vertex buffers} and the data inside them
     * is cloned.
     * 
     * @return a deep clone of this mesh.
     */
    public Mesh deepClone(){
        try{
            Mesh clone = (Mesh) super.clone();
            clone.meshBound = meshBound != null ? meshBound.clone() : null;

            // TODO: Collision tree cloning
            //clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.collisionTree = null; // it will get re-generated in any case

            clone.buffers = new IntMap<VertexBuffer>();
            clone.buffersList = new SafeArrayList<VertexBuffer>(VertexBuffer.class);
            for (VertexBuffer vb : buffersList.getArray()){
                VertexBuffer bufClone = vb.clone();
                clone.buffers.put(vb.getBufferType().ordinal(), bufClone);
                clone.buffersList.add(bufClone);
            }
            
            clone.vertexArrayID = -1;
            clone.vertCount = -1;
            clone.elementCount = -1;
            
            // although this could change
            // if the bone weight/index buffers are modified
            clone.maxNumWeights = maxNumWeights; 
            
            clone.elementLengths = elementLengths != null ? elementLengths.clone() : null;
            clone.modeStart = modeStart != null ? modeStart.clone() : null;
            return clone;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    /**
     * Clone the mesh for animation use.
     * This creates a shallow clone of the mesh, sharing most
     * of the {@link VertexBuffer vertex buffer} data, however the
     * {@link Type#Position}, {@link Type#Normal}, and {@link Type#Tangent} buffers
     * are deeply cloned.
     * 
     * @return A clone of the mesh for animation use.
     */
    public Mesh cloneForAnim(){
        Mesh clone = clone();
        if (getBuffer(Type.BindPosePosition) != null){
            VertexBuffer oldPos = getBuffer(Type.Position);
            
            // NOTE: creates deep clone
            VertexBuffer newPos = oldPos.clone();
            clone.clearBuffer(Type.Position);
            clone.setBuffer(newPos);

            if (getBuffer(Type.BindPoseNormal) != null){
                VertexBuffer oldNorm = getBuffer(Type.Normal);
                VertexBuffer newNorm = oldNorm.clone();
                clone.clearBuffer(Type.Normal);
                clone.setBuffer(newNorm);
                
                if (getBuffer(Type.BindPoseTangent) != null){
                    VertexBuffer oldTang = getBuffer(Type.Tangent);
                    VertexBuffer newTang = oldTang.clone();
                    clone.clearBuffer(Type.Tangent);
                    clone.setBuffer(newTang);
                }
            }
        }
        return clone;
    }

    /**
     * Generates the {@link Type#BindPosePosition}, {@link Type#BindPoseNormal},
     * and {@link Type#BindPoseTangent} 
     * buffers for this mesh by duplicating them based on the position and normal
     * buffers already set on the mesh.
     * This method does nothing if the mesh has no bone weight or index
     * buffers.
     * 
     * @param forSoftwareAnim Should be true if the bind pose is to be generated.
     */
    public void generateBindPose(boolean forSoftwareAnim){
        if (forSoftwareAnim){
            VertexBuffer pos = getBuffer(Type.Position);
            if (pos == null || getBuffer(Type.BoneIndex) == null) {
                // ignore, this mesh doesn't have positional data
                // or it doesn't have bone-vertex assignments, so its not animated
                return;
            }

            VertexBuffer bindPos = new VertexBuffer(Type.BindPosePosition);
            bindPos.setupData(Usage.CpuOnly,
                    3,
                    Format.Float,
                    BufferUtils.clone(pos.getData()));
            setBuffer(bindPos);

            // XXX: note that this method also sets stream mode
            // so that animation is faster. this is not needed for hardware skinning
            pos.setUsage(Usage.Stream);

            VertexBuffer norm = getBuffer(Type.Normal);
            if (norm != null) {
                VertexBuffer bindNorm = new VertexBuffer(Type.BindPoseNormal);
                bindNorm.setupData(Usage.CpuOnly,
                        3,
                        Format.Float,
                        BufferUtils.clone(norm.getData()));
                setBuffer(bindNorm);
                norm.setUsage(Usage.Stream);
            }
            
            VertexBuffer tangents = getBuffer(Type.Tangent);
            if (tangents != null) {
                VertexBuffer bindTangents = new VertexBuffer(Type.BindPoseTangent);
                bindTangents.setupData(Usage.CpuOnly,
                        4,
                        Format.Float,
                        BufferUtils.clone(tangents.getData()));
                setBuffer(bindTangents);
                tangents.setUsage(Usage.Stream);
            }
        }
    }

    /**
     * Prepares the mesh for software skinning by converting the bone index
     * and weight buffers to heap buffers. 
     * 
     * @param forSoftwareAnim Should be true to enable the conversion.
     */
    public void prepareForAnim(boolean forSoftwareAnim){
        if (forSoftwareAnim){
            // convert indices
            VertexBuffer indices = getBuffer(Type.BoneIndex);
            ByteBuffer originalIndex = (ByteBuffer) indices.getData();
            ByteBuffer arrayIndex = ByteBuffer.allocate(originalIndex.capacity());
            originalIndex.clear();
            arrayIndex.put(originalIndex);
            indices.updateData(arrayIndex);

            // convert weights
            VertexBuffer weights = getBuffer(Type.BoneWeight);
            FloatBuffer originalWeight = (FloatBuffer) weights.getData();
            FloatBuffer arrayWeight = FloatBuffer.allocate(originalWeight.capacity());
            originalWeight.clear();
            arrayWeight.put(originalWeight);
            weights.updateData(arrayWeight);
        }
    }

    /**
     * Set the LOD (level of detail) index buffers on this mesh.
     * 
     * @param lodLevels The LOD levels to set
     */
    public void setLodLevels(VertexBuffer[] lodLevels){
        this.lodLevels = lodLevels;
    }

    /**
     * @return The number of LOD levels set on this mesh, including the main
     * index buffer, returns zero if there are no lod levels.
     */
    public int getNumLodLevels(){
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
     * @see #setLodLevels(com.jme3.scene.VertexBuffer[]) 
     */
    public VertexBuffer getLodLevel(int lod){
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
     * @return mode start indices
     */
    public void setModeStart(int[] modeStart) {
        this.modeStart = modeStart;
    }

    /**
     * Returns the mesh mode
     * 
     * @return the mesh mode
     * 
     * @see #setMode(com.jme3.scene.Mesh.Mode) 
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
     * @param maxNumWeights 
     */
    public void setMaxNumWeights(int maxNumWeights) {
        this.maxNumWeights = maxNumWeights;
    }

    /**
     * Returns the size of points for point meshes
     * 
     * @return the size of points
     * 
     * @see #setPointSize(float) 
     */
    public float getPointSize() {
        return pointSize;
    }

    /**
     * Set the size of points for meshes of mode {@link Mode#Points}. 
     * The point size is specified as on-screen pixels, the default
     * value is 1.0. The point size
     * does nothing if {@link RenderState#setPointSprite(boolean) point sprite}
     * render state is enabled, in that case, the vertex shader must specify the 
     * point size by writing to <code>gl_PointSize</code>.
     * 
     * @param pointSize The size of points
     */
    public void setPointSize(float pointSize) {
        this.pointSize = pointSize;
    }

    /**
     * Returns the line width for line meshes.
     * 
     * @return the line width
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Specify the line width for meshes of the line modes, such
     * as {@link Mode#Lines}. The line width is specified as on-screen pixels, 
     * the default value is 1.0.
     * 
     * @param lineWidth The line width
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Indicates to the GPU that this mesh will not be modified (a hint). 
     * Sets the usage mode to {@link Usage#Static}
     * for all {@link VertexBuffer vertex buffers} on this Mesh.
     */
    public void setStatic() {
        for (VertexBuffer vb : buffersList.getArray()){
            vb.setUsage(Usage.Static);
        }
    }

    /**
     * Indicates to the GPU that this mesh will be modified occasionally (a hint).
     * Sets the usage mode to {@link Usage#Dynamic}
     * for all {@link VertexBuffer vertex buffers} on this Mesh.
     */
    public void setDynamic() {
        for (VertexBuffer vb : buffersList.getArray()){
            vb.setUsage(Usage.Dynamic);
        }
    }

    /**
     * Indicates to the GPU that this mesh will be modified every frame (a hint).
     * Sets the usage mode to {@link Usage#Stream}
     * for all {@link VertexBuffer vertex buffers} on this Mesh.
     */
    public void setStreamed(){
        for (VertexBuffer vb : buffersList.getArray()){
            vb.setUsage(Usage.Stream);
        }
    }

    /**
     * Interleaves the data in this mesh. This operation cannot be reversed.
     * Some GPUs may prefer the data in this format, however it is a good idea
     * to <em>avoid</em> using this method as it disables some engine features.
     */
    @Deprecated
    public void setInterleaved(){
        ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>();
        vbs.addAll(buffersList);
        
//        ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>(buffers.values());
        // index buffer not included when interleaving
        vbs.remove(getBuffer(Type.Index));

        int stride = 0; // aka bytes per vertex
        for (int i = 0; i < vbs.size(); i++){
            VertexBuffer vb = vbs.get(i);
//            if (vb.getFormat() != Format.Float){
//                throw new UnsupportedOperationException("Cannot interleave vertex buffer.\n" +
//                                                        "Contains not-float data.");
//            }
            stride += vb.componentsLength;
            vb.getData().clear(); // reset position & limit (used later)
        }

        VertexBuffer allData = new VertexBuffer(Type.InterleavedData);
        ByteBuffer dataBuf = BufferUtils.createByteBuffer(stride * getVertexCount());
        allData.setupData(Usage.Static, 1, Format.UnsignedByte, dataBuf);
        
        // adding buffer directly so that no update counts is forced
        buffers.put(Type.InterleavedData.ordinal(), allData);
        buffersList.add(allData);

        for (int vert = 0; vert < getVertexCount(); vert++){
            for (int i = 0; i < vbs.size(); i++){
                VertexBuffer vb = vbs.get(i);
                switch (vb.getFormat()){
                    case Float:
                        FloatBuffer fb = (FloatBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++){
                            dataBuf.putFloat(fb.get());
                        }
                        break;
                    case Byte:
                    case UnsignedByte:
                        ByteBuffer bb = (ByteBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++){
                            dataBuf.put(bb.get());
                        }
                        break;
                    case Half:
                    case Short:
                    case UnsignedShort:
                        ShortBuffer sb = (ShortBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++){
                            dataBuf.putShort(sb.get());
                        }
                        break;
                    case Int:
                    case UnsignedInt:
                        IntBuffer ib = (IntBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++){
                            dataBuf.putInt(ib.get());
                        }
                        break;
                    case Double:
                        DoubleBuffer db = (DoubleBuffer) vb.getData();
                        for (int comp = 0; comp < vb.components; comp++){
                            dataBuf.putDouble(db.get());
                        }
                        break;
                }
            }
        }

        int offset = 0;
        for (VertexBuffer vb : vbs){
            vb.setOffset(offset);
            vb.setStride(stride);
            
            vb.updateData(null);
            //vb.setupData(vb.usage, vb.components, vb.format, null);
            offset += vb.componentsLength;
        }
    }

    private int computeNumElements(int bufSize){
        switch (mode){
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
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Update the {@link #getVertexCount() vertex} and 
     * {@link #getTriangleCount() triangle} counts for this mesh
     * based on the current data. This method should be called
     * after the {@link Buffer#capacity() capacities} of the mesh's
     * {@link VertexBuffer vertex buffers} has been altered.
     * 
     * @throws IllegalStateException If this mesh is in 
     * {@link #setInterleaved() interleaved} format.
     */
    public void updateCounts(){
        if (getBuffer(Type.InterleavedData) != null)
            throw new IllegalStateException("Should update counts before interleave");

        VertexBuffer pb = getBuffer(Type.Position);
        VertexBuffer ib = getBuffer(Type.Index);
        if (pb != null){
            vertCount = pb.getData().capacity() / pb.getNumComponents();
        }
        if (ib != null){
            elementCount = computeNumElements(ib.getData().capacity());
        }else{
            elementCount = computeNumElements(vertCount);
        }
    }

    /**
     * Returns the triangle count for the given LOD level.
     * 
     * @param lod The lod level to look up
     * @return The triangle count for that LOD level
     */
    public int getTriangleCount(int lod){
        if (lodLevels != null){
            if (lod < 0)
                throw new IllegalArgumentException("LOD level cannot be < 0");

            if (lod >= lodLevels.length)
                throw new IllegalArgumentException("LOD level "+lod+" does not exist!");

            return computeNumElements(lodLevels[lod].getData().capacity());
        }else if (lod == 0){
            return elementCount;
        }else{
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
    public int getTriangleCount(){
        return elementCount;
    }

    /**
     * Returns the number of vertices on this mesh.
     * The value is computed based on the position buffer, which 
     * must be set on all meshes.
     * 
     * @return Number of vertices on the mesh
     */
    public int getVertexCount(){
        return vertCount;
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
    public void getTriangle(int index, Vector3f v1, Vector3f v2, Vector3f v3){
        VertexBuffer pb = getBuffer(Type.Position);
        IndexBuffer ib = getIndicesAsList();
        if (pb != null && pb.getFormat() == Format.Float && pb.getNumComponents() == 3){
            FloatBuffer fpb = (FloatBuffer) pb.getData();

            // aquire triangle's vertex indices
            int vertIndex = index * 3;
            int vert1 = ib.get(vertIndex);
            int vert2 = ib.get(vertIndex+1);
            int vert3 = ib.get(vertIndex+2);

            BufferUtils.populateFromBuffer(v1, fpb, vert1);
            BufferUtils.populateFromBuffer(v2, fpb, vert2);
            BufferUtils.populateFromBuffer(v3, fpb, vert3);
        }else{
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
    public void getTriangle(int index, Triangle tri){
        getTriangle(index, tri.get1(), tri.get2(), tri.get3());
        tri.setIndex(index);
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
    public void getTriangle(int index, int[] indices){
        IndexBuffer ib = getIndicesAsList();

        // acquire triangle's vertex indices
        int vertIndex = index * 3;
        indices[0] = ib.get(vertIndex);
        indices[1] = ib.get(vertIndex+1);
        indices[2] = ib.get(vertIndex+2);
    }

    /**
     * Returns the mesh's VAO ID. Internal use only.
     */
    public int getId(){
        return vertexArrayID;
    }

    /**
     * Sets the mesh's VAO ID. Internal use only.
     */
    public void setId(int id){
        if (vertexArrayID != -1)
            throw new IllegalStateException("ID has already been set.");
        
        vertexArrayID = id;
    }

    /**
     * Generates a collision tree for the mesh.
     * Called automatically by {@link #collideWith(com.jme3.collision.Collidable, 
     * com.jme3.math.Matrix4f, 
     * com.jme3.bounding.BoundingVolume, 
     * com.jme3.collision.CollisionResults) }.
     */
    public void createCollisionData(){
        BIHTree tree = new BIHTree(this);
        tree.construct();
        collisionTree = tree;
    }

    /**
     * Handles collision detection, internal use only.
     * User code should only use collideWith() on scene
     * graph elements such as {@link Spatial}s.
     */
    public int collideWith(Collidable other, 
                           Matrix4f worldMatrix,
                           BoundingVolume worldBound,
                           CollisionResults results){

        if (collisionTree == null){
            createCollisionData();
        }
        
        return collisionTree.collideWith(other, worldMatrix, worldBound, results);
    }

    /**
     * Sets the {@link VertexBuffer} on the mesh.
     * This will update the vertex/triangle counts if needed.
     * 
     * @param vb The buffer to set
     * @throws IllegalArgumentException If the buffer type is already set
     */
    public void setBuffer(VertexBuffer vb){
        if (buffers.containsKey(vb.getBufferType().ordinal()))
            throw new IllegalArgumentException("Buffer type already set: "+vb.getBufferType());

        buffers.put(vb.getBufferType().ordinal(), vb);
        buffersList.add(vb);
        updateCounts();
    }
    
    /**
     * Unsets the {@link VertexBuffer} set on this mesh
     * with the given type. Does nothing if the vertex buffer type is not set 
     * initially.
     * 
     * @param type The buffer type to remove
     */
    public void clearBuffer(VertexBuffer.Type type){
        VertexBuffer vb = buffers.remove(type.ordinal());
        if (vb != null){
            buffersList.remove(vb);
            updateCounts();
        }
    }
    
    /**
     * Creates a {@link VertexBuffer} for the mesh or modifies
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
    public void setBuffer(Type type, int components, Format format, Buffer buf){
        VertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null){
            vb = new VertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, format, buf);
            setBuffer(vb);
        }else{
            if (vb.getNumComponents() != components || vb.getFormat() != format){
                throw new UnsupportedOperationException("The buffer already set "
                        + "is incompatible with the given parameters");
            }
            vb.updateData(buf);
            updateCounts();
        }
    }
    
    /**
     * Set a floating point {@link VertexBuffer} on the mesh. 
     * 
     * @param type The type of {@link VertexBuffer}, 
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

    public void setBuffer(Type type, int components, float[] buf){
        setBuffer(type, components, BufferUtils.createFloatBuffer(buf));
    }

    public void setBuffer(Type type, int components, IntBuffer buf) {
        setBuffer(type, components, Format.UnsignedInt, buf);
    }

    public void setBuffer(Type type, int components, int[] buf){
        setBuffer(type, components, BufferUtils.createIntBuffer(buf));
    }

    public void setBuffer(Type type, int components, ShortBuffer buf) {
        setBuffer(type, components, Format.UnsignedShort, buf);
    }

    public void setBuffer(Type type, int components, byte[] buf){
        setBuffer(type, components, BufferUtils.createByteBuffer(buf));
    }

    public void setBuffer(Type type, int components, ByteBuffer buf) {
        setBuffer(type, components, Format.UnsignedByte, buf);
    }

    public void setBuffer(Type type, int components, short[] buf){
        setBuffer(type, components, BufferUtils.createShortBuffer(buf));
    }

    /**
     * Get the {@link VertexBuffer} stored on this mesh with the given
     * type.
     * 
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set 
     */
    public VertexBuffer getBuffer(Type type){
        return buffers.get(type.ordinal());
    }

    /**
     * Get the {@link VertexBuffer} data stored on this mesh in float
     * format.
     * 
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set
     */
    public FloatBuffer getFloatBuffer(Type type) {
        VertexBuffer vb = getBuffer(type);
        if (vb == null)
            return null;

        return (FloatBuffer) vb.getData();
    }
    
    /**
     * Get the {@link VertexBuffer} data stored on this mesh in short
     * format.
     * 
     * @param type The type of VertexBuffer
     * @return the VertexBuffer data, or null if not set
     */
    public ShortBuffer getShortBuffer(Type type) {
        VertexBuffer vb = getBuffer(type);
        if (vb == null)
            return null;

        return (ShortBuffer) vb.getData();
    }

    /**
     * Acquires an index buffer that will read the vertices on the mesh
     * as a list.
     * 
     * @return A virtual or wrapped index buffer to read the data as a list
     */
    public IndexBuffer getIndicesAsList(){
        if (mode == Mode.Hybrid)
            throw new UnsupportedOperationException("Hybrid mode not supported");
        
        IndexBuffer ib = getIndexBuffer();
        if (ib != null){
            if (mode.isListMode()){
                // already in list mode
                return ib; 
            }else{
                // not in list mode but it does have an index buffer
                // wrap it so the data is converted to list format
                return new WrappedIndexBuffer(this);
            }
        }else{
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
        VertexBuffer vb = getBuffer(Type.Index);
        if (vb == null)
            return null;
        
        Buffer buf = vb.getData();
        if (buf instanceof ByteBuffer) {
            return new IndexByteBuffer((ByteBuffer) buf);
        } else if (buf instanceof ShortBuffer) {
            return new IndexShortBuffer((ShortBuffer) buf);
        } else if (buf instanceof IntBuffer) {
            return new IndexIntBuffer((IntBuffer) buf);
        } else {
            throw new UnsupportedOperationException("Index buffer type unsupported: "+ buf.getClass());
        }
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
    public void extractVertexData(Mesh other) {
        // Determine the number of unique vertices need to
        // be created. Also determine the mappings
        // between old indices to new indices (since we avoid duplicating
        // vertices, this is a map and not an array).
        VertexBuffer oldIdxBuf = getBuffer(Type.Index);
        IndexBuffer indexBuf = getIndexBuffer();
        int numIndices = indexBuf.size();

        IntMap<Integer> oldIndicesToNewIndices = new IntMap<Integer>(numIndices);
        ArrayList<Integer> newIndicesToOldIndices = new ArrayList<Integer>();
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
        
        VertexBuffer newIdxBuf = new VertexBuffer(Type.Index);
        newIdxBuf.setupData(oldIdxBuf.getUsage(), 
                            oldIdxBuf.getNumComponents(), 
                            newIndexBuf instanceof IndexIntBuffer ? Format.UnsignedInt : Format.UnsignedShort,
                            newIndexBuf.getBuffer());
        clearBuffer(Type.Index);
        setBuffer(newIdxBuf);

        // Now, create the vertex buffers
        SafeArrayList<VertexBuffer> oldVertexData = other.getBufferList();
        for (VertexBuffer oldVb : oldVertexData) {
            if (oldVb.getBufferType() == VertexBuffer.Type.Index) {
                // ignore the index buffer
                continue;
            }

            // Create a new vertex buffer with similar configuration, but
            // with the capacity of number of unique vertices
            Buffer buffer = VertexBuffer.createBuffer(oldVb.getFormat(), oldVb.getNumComponents(), newNumVerts);

            VertexBuffer newVb = new VertexBuffer(oldVb.getBufferType());
            newVb.setNormalized(oldVb.isNormalized());
            newVb.setupData(oldVb.getUsage(), oldVb.getNumComponents(), oldVb.getFormat(), buffer);

            // Copy the vertex data from the old buffer into the new buffer
            for (int i = 0; i < newNumVerts; i++) {
                int oldIndex = newIndicesToOldIndices.get(i);

                // Copy the vertex attribute from the old index
                // to the new index
                oldVb.copyElement(oldIndex, newVb, i);
            }
            
            // Set the buffer on the mesh
            clearBuffer(newVb.getBufferType());
            setBuffer(newVb);
        }
        
        // Copy max weights per vertex as well
        setMaxNumWeights(other.getMaxNumWeights());
        
        // The data has been copied over, update informations
        updateCounts();
        updateBound();
    }
    
    /**
     * Scales the texture coordinate buffer on this mesh by the given
     * scale factor. 
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
    public void scaleTextureCoordinates(Vector2f scaleFactor){
        VertexBuffer tc = getBuffer(Type.TexCoord);
        if (tc == null)
            throw new IllegalStateException("The mesh has no texture coordinates");

        if (tc.getFormat() != VertexBuffer.Format.Float)
            throw new UnsupportedOperationException("Only float texture coord format is supported");

        if (tc.getNumComponents() != 2)
            throw new UnsupportedOperationException("Only 2D texture coords are supported");

        FloatBuffer fb = (FloatBuffer) tc.getData();
        fb.clear();
        for (int i = 0; i < fb.capacity() / 2; i++){
            float x = fb.get();
            float y = fb.get();
            fb.position(fb.position()-2);
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
    public void updateBound(){
        VertexBuffer posBuf = getBuffer(VertexBuffer.Type.Position);
        if (meshBound != null && posBuf != null){
            meshBound.computeFromPoints((FloatBuffer)posBuf.getData());
        }
    }

    /**
     * Returns the {@link BoundingVolume} of this Mesh.
     * By default the bounding volume is a {@link BoundingBox}.
     * 
     * @return the bounding volume of this mesh
     */
    public BoundingVolume getBound() {
        return meshBound;
    }

    /**
     * Sets the {@link BoundingVolume} for this Mesh.
     * The bounding volume is recomputed by calling {@link #updateBound() }.
     * 
     * @param modelBound The model bound to set
     */
    public void setBound(BoundingVolume modelBound) {
        meshBound = modelBound;
    }

    /**
     * Returns a map of all {@link VertexBuffer vertex buffers} on this Mesh.
     * The integer key for the map is the {@link Enum#ordinal() ordinal}
     * of the vertex buffer's {@link Type}.
     * Note that the returned map is a reference to the map used internally, 
     * modifying it will cause undefined results.
     * 
     * @return map of vertex buffers on this mesh.
     */
    public IntMap<VertexBuffer> getBuffers(){
        return buffers;
    }
    
    /**
     * Returns a list of all {@link VertexBuffer vertex buffers} on this Mesh.
     * Using a list instead an IntMap via the {@link #getBuffers() } method is
     * better for iteration as there's no need to create an iterator instance.
     * Note that the returned list is a reference to the list used internally,
     * modifying it will cause undefined results.
     * 
     * @return list of vertex buffers on this mesh.
     */
    public SafeArrayList<VertexBuffer> getBufferList(){
        return buffersList;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);

//        HashMap<String, VertexBuffer> map = new HashMap<String, VertexBuffer>();
//        for (Entry<VertexBuffer> buf : buffers){
//            if (buf.getValue() != null)
//                map.put(buf.getKey()+"a", buf.getValue());
//        }
//        out.writeStringSavableMap(map, "buffers", null);

        out.write(meshBound, "modelBound", null);
        out.write(vertCount, "vertCount", -1);
        out.write(elementCount, "elementCount", -1);
        out.write(maxNumWeights, "max_num_weights", -1);
        out.write(mode, "mode", Mode.Triangles);
        out.write(collisionTree, "collisionTree", null);
        out.write(elementLengths, "elementLengths", null);
        out.write(modeStart, "modeStart", null);
        out.write(pointSize, "pointSize", 1f);

        out.writeIntSavableMap(buffers, "buffers", null);
        out.write(lodLevels, "lodLevels", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        meshBound = (BoundingVolume) in.readSavable("modelBound", null);
        vertCount = in.readInt("vertCount", -1);
        elementCount = in.readInt("elementCount", -1);
        maxNumWeights = in.readInt("max_num_weights", -1);
        mode = in.readEnum("mode", Mode.class, Mode.Triangles);
        elementLengths = in.readIntArray("elementLengths", null);
        modeStart = in.readIntArray("modeStart", null);
        collisionTree = (BIHTree) in.readSavable("collisionTree", null);
        elementLengths = in.readIntArray("elementLengths", null);
        modeStart = in.readIntArray("modeStart", null);
        pointSize = in.readFloat("pointSize", 1f);

//        in.readStringSavableMap("buffers", null);
        buffers = (IntMap<VertexBuffer>) in.readIntSavableMap("buffers", null);
        for (Entry<VertexBuffer> entry : buffers){
            buffersList.add(entry.getValue());
        }
        
        Savable[] lodLevelsSavable = in.readSavableArray("lodLevels", null);
        if (lodLevelsSavable != null) {
            lodLevels = new VertexBuffer[lodLevelsSavable.length];
            System.arraycopy( lodLevelsSavable, 0, lodLevels, 0, lodLevels.length);
        }
    }

}
