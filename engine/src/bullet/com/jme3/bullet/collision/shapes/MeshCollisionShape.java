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
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.util.NativeMeshUtil;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic mesh collision shape
 * @author normenhansen
 */
public class MeshCollisionShape extends CollisionShape {

    protected int numVertices, numTriangles, vertexStride, triangleIndexStride;
    protected ByteBuffer triangleIndexBase, vertexBase;
    protected long meshId = 0;

    public MeshCollisionShape() {
    }

    /**
     * creates a collision shape from the given TriMesh
     * @param mesh the TriMesh to use
     */
    public MeshCollisionShape(Mesh mesh) {
        createCollisionMesh(mesh);
    }

    private void createCollisionMesh(Mesh mesh) {
        triangleIndexBase = BufferUtils.createByteBuffer(mesh.getTriangleCount() * 3 * 4);
        vertexBase = BufferUtils.createByteBuffer(mesh.getVertexCount() * 3 * 4);
        numVertices = mesh.getVertexCount();
        vertexStride = 12; //3 verts * 4 bytes per.
        numTriangles = mesh.getTriangleCount();
        triangleIndexStride = 12; //3 index entries * 4 bytes each.

        IndexBuffer indices = mesh.getIndexBuffer();
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

        createShape();
    }

    /**
     * creates a jme mesh from the collision shape, only needed for debugging
     */
//    public Mesh createJmeMesh(){
//        return Converter.convert(bulletMesh);
//    }
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(numVertices, "numVertices", 0);
        capsule.write(numTriangles, "numTriangles", 0);
        capsule.write(vertexStride, "vertexStride", 0);
        capsule.write(triangleIndexStride, "triangleIndexStride", 0);

        capsule.write(triangleIndexBase.array(), "triangleIndexBase", new byte[0]);
        capsule.write(vertexBase.array(), "vertexBase", new byte[0]);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        numVertices = capsule.readInt("numVertices", 0);
        numTriangles = capsule.readInt("numTriangles", 0);
        vertexStride = capsule.readInt("vertexStride", 0);
        triangleIndexStride = capsule.readInt("triangleIndexStride", 0);

        triangleIndexBase = ByteBuffer.wrap(capsule.readByteArray("triangleIndexBase", new byte[0]));
        vertexBase = ByteBuffer.wrap(capsule.readByteArray("vertexBase", new byte[0])).order(ByteOrder.nativeOrder());
        createShape();
    }

    protected void createShape() {
//        bulletMesh = new IndexedMesh();
//        bulletMesh.numVertices = numVertices;
//        bulletMesh.numTriangles = numTriangles;
//        bulletMesh.vertexStride = vertexStride;
//        bulletMesh.triangleIndexStride = triangleIndexStride;
//        bulletMesh.triangleIndexBase = triangleIndexBase;
//        bulletMesh.vertexBase = vertexBase;
//        bulletMesh.triangleIndexBase = triangleIndexBase;
//        TriangleIndexVertexArray tiv = new TriangleIndexVertexArray(numTriangles, triangleIndexBase, triangleIndexStride, numVertices, vertexBase, vertexStride);
//        objectId = new BvhTriangleMeshShape(tiv, true);
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        meshId = NativeMeshUtil.createTriangleIndexVertexArray(triangleIndexBase, vertexBase, numTriangles, numVertices, vertexStride, triangleIndexStride);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Mesh {0}", Long.toHexString(meshId));
        objectId = createShape(meshId);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(long meshId);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finalizing Mesh {0}", Long.toHexString(meshId));
        finalizeNative(meshId);
    }

    private native void finalizeNative(long objectId);
}
