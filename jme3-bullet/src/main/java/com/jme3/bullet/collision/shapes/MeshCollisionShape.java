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
package com.jme3.bullet.collision.shapes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.bullet.util.NativeMeshUtil;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;

/**
 * Basic mesh collision shape
 *
 * @author normenhansen
 */
public class MeshCollisionShape extends CollisionShape {

    private static final String VERTEX_BASE = "vertexBase";
    private static final String TRIANGLE_INDEX_BASE = "triangleIndexBase";
    private static final String TRIANGLE_INDEX_STRIDE = "triangleIndexStride";
    private static final String VERTEX_STRIDE = "vertexStride";
    private static final String NUM_TRIANGLES = "numTriangles";
    private static final String NUM_VERTICES = "numVertices";
    private static final String NATIVE_BVH = "nativeBvh";
    protected int numVertices, numTriangles, vertexStride, triangleIndexStride;
    protected ByteBuffer triangleIndexBase, vertexBase;
    protected long meshId = 0;
    protected long nativeBVHBuffer = 0;
    private boolean memoryOptimized;

    public MeshCollisionShape() {
    }

    /**
     * Creates a collision shape from the given Mesh. 
     * Default behavior, more optimized for memory usage.
     *
     * @param mesh
     */
    public MeshCollisionShape(Mesh mesh) {
        this(mesh, true);
    }

    /**
     * Creates a collision shape from the given Mesh.
     * <code>memoryOptimized</code> determines if optimized instead of 
     * quantized BVH will be used.
     * Internally, <code>memoryOptimized</code> BVH is slower to calculate (~4x) 
     * but also smaller (~0.5x). 
     * It is preferable to use the memory optimized version and then serialize
     * the resulting MeshCollisionshape as this will also save the
     * generated BVH. 
     * An exception can be procedurally / generated collision shapes, where
     * the generation time is more of a concern
     *
     * @param mesh the Mesh to use
     * @param memoryOptimized True to generate a memory optimized BVH,
     * false to generate quantized BVH.
     */
    public MeshCollisionShape(final Mesh mesh, final boolean memoryOptimized) {
        this.memoryOptimized = memoryOptimized;
        this.createCollisionMesh(mesh);
    }

    /**
     * Advanced constructor, usually you don’t want to use this, but the Mesh
     * based one. Passing false values can lead to a crash, use at own risk
     *
     * This constructor bypasses all copy logic normally used, this allows for
     * faster bullet shape generation when using procedurally generated Meshes.
     *
     *
     * @param indices the raw index buffer
     * @param vertices the raw vertex buffer
     * @param memoryOptimized use quantisize BVH, uses less memory, but slower
     */
    public MeshCollisionShape(ByteBuffer indices, ByteBuffer vertices, boolean memoryOptimized) {
        this.triangleIndexBase = indices;
        this.vertexBase = vertices;
        this.numVertices = vertices.limit() / 4 / 3;
        this.numTriangles = this.triangleIndexBase.limit() / 4 / 3;
        this.vertexStride = 12;
        this.triangleIndexStride = 12;
        this.memoryOptimized = memoryOptimized;
        this.createShape(null);
    }
    
    private void createCollisionMesh(Mesh mesh) {
        this.triangleIndexBase = BufferUtils.createByteBuffer(mesh.getTriangleCount() * 3 * 4);
        this.vertexBase = BufferUtils.createByteBuffer(mesh.getVertexCount() * 3 * 4);
        this.numVertices = mesh.getVertexCount();
        this.vertexStride = 12; // 3 verts * 4 bytes per.
        this.numTriangles = mesh.getTriangleCount();
        this.triangleIndexStride = 12; // 3 index entries * 4 bytes each.

        IndexBuffer indices = mesh.getIndicesAsList();
        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        vertices.rewind();

        int verticesLength = mesh.getVertexCount() * 3;
        for (int i = 0; i < verticesLength; i++) {
            float tempFloat = vertices.get();
            vertexBase.putFloat(tempFloat);
        }

        int indicesLength = mesh.getTriangleCount() * 3;
        for (int i = 0; i < indicesLength; i++) {
            triangleIndexBase.putInt(indices.get(i));
        }
        vertices.rewind();
        vertices.clear();

        this.createShape(null);
    }

    @Override
    public void write(final JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(numVertices, MeshCollisionShape.NUM_VERTICES, 0);
        capsule.write(numTriangles, MeshCollisionShape.NUM_TRIANGLES, 0);
        capsule.write(vertexStride, MeshCollisionShape.VERTEX_STRIDE, 0);
        capsule.write(triangleIndexStride, MeshCollisionShape.TRIANGLE_INDEX_STRIDE, 0);

        triangleIndexBase.position(0);
        byte[] triangleIndexBasearray = new byte[triangleIndexBase.limit()];
        triangleIndexBase.get(triangleIndexBasearray);
        capsule.write(triangleIndexBasearray, MeshCollisionShape.TRIANGLE_INDEX_BASE, null);

        vertexBase.position(0);
        byte[] vertexBaseArray = new byte[vertexBase.limit()];
        vertexBase.get(vertexBaseArray);
        capsule.write(vertexBaseArray, MeshCollisionShape.VERTEX_BASE, null);

        if (memoryOptimized) {
            byte[] data = saveBVH(objectId);
            capsule.write(data, MeshCollisionShape.NATIVE_BVH, null);
        }
    }

    @Override
    public void read(final JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        this.numVertices = capsule.readInt(MeshCollisionShape.NUM_VERTICES, 0);
        this.numTriangles = capsule.readInt(MeshCollisionShape.NUM_TRIANGLES, 0);
        this.vertexStride = capsule.readInt(MeshCollisionShape.VERTEX_STRIDE, 0);
        this.triangleIndexStride = capsule.readInt(MeshCollisionShape.TRIANGLE_INDEX_STRIDE, 0);

        this.triangleIndexBase = BufferUtils.createByteBuffer(capsule.readByteArray(MeshCollisionShape.TRIANGLE_INDEX_BASE, null));
        this.vertexBase = BufferUtils.createByteBuffer(capsule.readByteArray(MeshCollisionShape.VERTEX_BASE, null));

        byte[] nativeBvh = capsule.readByteArray(MeshCollisionShape.NATIVE_BVH, null);
        memoryOptimized=nativeBvh != null;
        createShape(nativeBvh);
    }

    private void createShape(byte bvh[]) {
        boolean buildBvh=bvh==null||bvh.length==0;
        this.meshId = NativeMeshUtil.createTriangleIndexVertexArray(this.triangleIndexBase, this.vertexBase, this.numTriangles, this.numVertices, this.vertexStride, this.triangleIndexStride);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Mesh {0}", Long.toHexString(this.meshId));
        this.objectId = createShape(memoryOptimized, buildBvh, this.meshId);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(this.objectId));
        if(!buildBvh)   nativeBVHBuffer = setBVH(bvh, this.objectId);                
        this.setScale(this.scale);
        this.setMargin(this.margin);        
    }

    /**
     * returns the pointer to the native buffer used by the in place
     * de-serialized shape, must be freed when not used anymore!
     */
    private native long setBVH(byte[] buffer, long objectid);
    
    private native byte[] saveBVH(long objectId);
    
    private native long createShape(boolean memoryOptimized, boolean buildBvt, long meshId);

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Mesh {0}", Long.toHexString(this.meshId));
        if (this.meshId > 0) {
            this.finalizeNative(this.meshId, this.nativeBVHBuffer);
        }
    }

    private native void finalizeNative(long objectId, long nativeBVHBuffer);
}