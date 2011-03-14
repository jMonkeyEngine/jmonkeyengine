/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexByteBuffer;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.bih.BIHTree;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Matrix4f;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer.*;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class Mesh implements Savable, Cloneable {

    // TODO: Document this enum
    public enum Mode {
        Points,
        Lines,
        LineLoop,
        LineStrip,
        Triangles,
        TriangleStrip,
        TriangleFan,
        Hybrid
    }

//    private static final int BUFFERS_SIZE = VertexBuffer.Type.BoneIndex.ordinal() + 1;

    /**
     * The bounding volume that contains the mesh entirely.
     * By default a BoundingBox (AABB).
     */
    private BoundingVolume meshBound =  new BoundingBox();

    private CollisionData collisionTree = null;

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

    public Mesh(){
    }

    @Override
    public Mesh clone(){
        try{
            Mesh clone = (Mesh) super.clone();
            clone.meshBound = meshBound.clone();
            clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.buffers = buffers.clone();
            clone.vertexArrayID = -1;
            if (elementLengths != null)
                clone.elementLengths = elementLengths.clone();
            if (modeStart != null)
                clone.modeStart = modeStart.clone();
            return clone;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    public Mesh deepClone(){
        try{
            Mesh clone = (Mesh) super.clone();
            clone.meshBound = meshBound != null ? meshBound.clone() : null;
            clone.collisionTree = collisionTree != null ? collisionTree : null;
            clone.buffers = new IntMap<VertexBuffer>();
            for (Entry<VertexBuffer> ent : buffers){
                clone.buffers.put(ent.getKey(), ent.getValue().clone());
            }
            clone.vertexArrayID = -1;
            clone.vertCount = -1;
            clone.elementCount = -1;
            clone.maxNumWeights = -1;
            clone.elementLengths = elementLengths != null ? elementLengths.clone() : null;
            clone.modeStart = modeStart != null ? modeStart.clone() : null;
            return clone;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

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
            }
        }
        return clone;
    }

    public void prepareForAnim(boolean swAnim){
        if (swAnim){
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

    public VertexBuffer getLodLevel(int lod){
        return lodLevels[lod];
    }
    
    public int[] getElementLengths() {
        return elementLengths;
    }

    public void setElementLengths(int[] elementLengths) {
        this.elementLengths = elementLengths;
    }

    public int[] getModeStart() {
        return modeStart;
    }

    public void setModeStart(int[] modeStart) {
        this.modeStart = modeStart;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        updateCounts();
    }

    public int getMaxNumWeights() {
        return maxNumWeights;
    }

    public void setMaxNumWeights(int maxNumWeights) {
        this.maxNumWeights = maxNumWeights;
    }

    public float getPointSize() {
        return pointSize;
    }

    public void setPointSize(float pointSize) {
        this.pointSize = pointSize;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Locks the mesh so it cannot be modified anymore, thus
     * optimizing its data.
     */
    public void setStatic() {
        for (Entry<VertexBuffer> entry : buffers){
            entry.getValue().setUsage(Usage.Static);
        }
    }

    /**
     * Unlocks the mesh so it can be modified, this
     * will un-optimize the data!
     */
    public void setDynamic() {
        for (Entry<VertexBuffer> entry : buffers){
            entry.getValue().setUsage(Usage.Dynamic);
        }
    }

    public void setStreamed(){
        for (Entry<VertexBuffer> entry : buffers){
            entry.getValue().setUsage(Usage.Stream);
        }
    }

    public void setInterleaved(){
        ArrayList<VertexBuffer> vbs = new ArrayList<VertexBuffer>();
        for (Entry<VertexBuffer> entry : buffers){
            vbs.add(entry.getValue());
        }
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
        allData.setupData(Usage.Static, -1, Format.UnsignedByte, dataBuf);
        // adding buffer directly so that no update counts is forced
        buffers.put(Type.InterleavedData.ordinal(), allData);

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
                }
            }
        }

        int offset = 0;
        for (VertexBuffer vb : vbs){
            vb.setOffset(offset);
            vb.setStride(stride);
            
            // discard old buffer
            vb.setupData(vb.usage, vb.components, vb.format, null);
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

    public int getTriangleCount(){
        return elementCount;
    }

    public int getVertexCount(){
        return vertCount;
    }

    /**
     *
     * @param count
     * @deprecated Use {@link Mesh#updateCounts() } to update the counts after
     * updating the buffers.
     */
    @Deprecated
    public void setTriangleCount(int count){
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     *
     * @param count
     * @deprecated Use {@link Mesh#updateCounts() } to update the counts after
     * updating the buffers.
     */
    @Deprecated
    public void setVertexCount(int count){
        throw new UnsupportedOperationException("Deprecated");
    }

    public void getTriangle(int index, Vector3f v1, Vector3f v2, Vector3f v3){
        VertexBuffer pb = getBuffer(Type.Position);
        VertexBuffer ib = getBuffer(Type.Index);

        if (pb.getFormat() == Format.Float){
            FloatBuffer fpb = (FloatBuffer) pb.getData();

            if (ib.getFormat() == Format.UnsignedShort){
                // accepted format for buffers
                ShortBuffer sib = (ShortBuffer) ib.getData();

                // aquire triangle's vertex indices
                int vertIndex = index * 3;
                int vert1 = sib.get(vertIndex);
                int vert2 = sib.get(vertIndex+1);
                int vert3 = sib.get(vertIndex+2);

                BufferUtils.populateFromBuffer(v1, fpb, vert1);
                BufferUtils.populateFromBuffer(v2, fpb, vert2);
                BufferUtils.populateFromBuffer(v3, fpb, vert3);
            }
        }
    }
    
    public void getTriangle(int index, Triangle tri){
        getTriangle(index, tri.get1(), tri.get2(), tri.get3());
        tri.setIndex(index);
    }

    public void getTriangle(int index, int[] indices){
        VertexBuffer ib = getBuffer(Type.Index);
        if (ib.getFormat() == Format.UnsignedShort){
            // accepted format for buffers
            ShortBuffer sib = (ShortBuffer) ib.getData();

            // aquire triangle's vertex indices
            int vertIndex = index * 3;
            indices[0] = sib.get(vertIndex);
            indices[1] = sib.get(vertIndex+1);
            indices[2] = sib.get(vertIndex+2);
        }
    }

    public int getId(){
        return vertexArrayID;
    }

    public void setId(int id){
        if (vertexArrayID != -1)
            throw new IllegalStateException("ID has already been set.");
        
        vertexArrayID = id;
    }

    public void createCollisionData(){
        BIHTree tree = new BIHTree(this);
        tree.construct();
        collisionTree = tree;
    }

    public int collideWith(Collidable other, 
                           Matrix4f worldMatrix,
                           BoundingVolume worldBound,
                           CollisionResults results){

        if (collisionTree == null){
            createCollisionData();
        }
        
        return collisionTree.collideWith(other, worldMatrix, worldBound, results);
    }

//    public void setLodData(ShortBuffer[] lodData){
//        this.lodData = lodData;
//    }

    public void setBuffer(Type type, int components, FloatBuffer buf) {
//        VertexBuffer vb = buffers.get(type);
        VertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null){
            if (buf == null)
                return;

            vb = new VertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, Format.Float, buf);
//            buffers.put(type, vb);
            buffers.put(type.ordinal(), vb);
        }else{
            vb.setupData(Usage.Dynamic, components, Format.Float, buf);
        }
        updateCounts();
    }

    public void setBuffer(Type type, int components, float[] buf){
        setBuffer(type, components, BufferUtils.createFloatBuffer(buf));
    }

    public void setBuffer(Type type, int components, IntBuffer buf) {
        VertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null){
            vb = new VertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, Format.UnsignedInt, buf);
            buffers.put(type.ordinal(), vb);
            updateCounts();
        }
    }

    public void setBuffer(Type type, int components, int[] buf){
        setBuffer(type, components, BufferUtils.createIntBuffer(buf));
    }

    public void setBuffer(Type type, int components, ShortBuffer buf) {
        VertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null){
            vb = new VertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, Format.UnsignedShort, buf);
            buffers.put(type.ordinal(), vb);
            updateCounts();
        }
    }

    public void setBuffer(Type type, int components, byte[] buf){
        setBuffer(type, components, BufferUtils.createByteBuffer(buf));
    }

    public void setBuffer(Type type, int components, ByteBuffer buf) {
        VertexBuffer vb = buffers.get(type.ordinal());
        if (vb == null){
            vb = new VertexBuffer(type);
            vb.setupData(Usage.Dynamic, components, Format.UnsignedByte, buf);
            buffers.put(type.ordinal(), vb);
            updateCounts();
        }
    }

    public void setBuffer(VertexBuffer vb){
        if (buffers.containsKey(vb.getBufferType().ordinal()))
            throw new IllegalArgumentException("Buffer type already set: "+vb.getBufferType());

        buffers.put(vb.getBufferType().ordinal(), vb);
        updateCounts();
    }

    public void clearBuffer(VertexBuffer.Type type){
        buffers.remove(type.ordinal());
        updateCounts();
    }

    public void setBuffer(Type type, int components, short[] buf){
        setBuffer(type, components, BufferUtils.createShortBuffer(buf));
    }

    public VertexBuffer getBuffer(Type type){
        return buffers.get(type.ordinal());
    }

    public FloatBuffer getFloatBuffer(Type type) {
        VertexBuffer vb = getBuffer(type);
        if (vb == null)
            return null;

        return (FloatBuffer) vb.getData();
    }
    
    public ShortBuffer getShortBuffer(Type type) {
        VertexBuffer vb = getBuffer(type);
        if (vb == null)
            return null;

        return (ShortBuffer) vb.getData();
    }

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

    public void updateBound(){
        VertexBuffer posBuf = getBuffer(VertexBuffer.Type.Position);
        if (meshBound != null && posBuf != null){
            meshBound.computeFromPoints((FloatBuffer)posBuf.getData());
        }
    }

    public BoundingVolume getBound() {
        return meshBound;
    }

    public void setBound(BoundingVolume modelBound) {
        meshBound = modelBound;
    }

    public IntMap<VertexBuffer> getBuffers(){
        return buffers;
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
        Savable[] lodLevelsSavable = in.readSavableArray("lodLevels", null);
        if(lodLevelsSavable!=null){
            lodLevels = new VertexBuffer[lodLevelsSavable.length];
            for (int i = 0; i < lodLevels.length; i++){
                lodLevels[i] = (VertexBuffer) lodLevelsSavable[i];
            }
        }
    }

}
