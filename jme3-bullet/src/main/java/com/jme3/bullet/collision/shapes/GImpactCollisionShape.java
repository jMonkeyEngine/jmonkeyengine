/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A mesh collision shape based on Bullet's btGImpactMeshShape.
 *
 * @author normenhansen
 */
public class GImpactCollisionShape extends CollisionShape {

//    protected Vector3f worldScale;
    protected int numVertices, numTriangles, vertexStride, triangleIndexStride;
    protected ByteBuffer triangleIndexBase, vertexBase;
    /**
     * Unique identifier of the Bullet mesh. The constructor sets this to a
     * non-zero value.
     */
    protected long meshId = 0;
//    protected IndexedMesh bulletMesh;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public GImpactCollisionShape() {
    }

    /**
     * Instantiate a shape based on the specified JME mesh.
     *
     * @param mesh the Mesh to use
     */
    public GImpactCollisionShape(Mesh mesh) {
        createCollisionMesh(mesh);
    }

    private void createCollisionMesh(Mesh mesh) {
        triangleIndexBase = BufferUtils.createByteBuffer(mesh.getTriangleCount() * 3 * 4);
        vertexBase = BufferUtils.createByteBuffer(mesh.getVertexCount() * 3 * 4); 
//        triangleIndexBase = ByteBuffer.allocate(mesh.getTriangleCount() * 3 * 4);
//        vertexBase = ByteBuffer.allocate(mesh.getVertexCount() * 3 * 4);
        numVertices = mesh.getVertexCount();
        vertexStride = 12; //3 verts * 4 bytes per.
        numTriangles = mesh.getTriangleCount();
        triangleIndexStride = 12; //3 index entries * 4 bytes each.

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

        createShape();
    }

//    /**
//     * creates a jme mesh from the collision shape, only needed for debugging
//     */
//    public Mesh createJmeMesh() {
//        return Converter.convert(bulletMesh);
//    }
    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
//        capsule.write(worldScale, "worldScale", new Vector3f(1, 1, 1));
        capsule.write(numVertices, "numVertices", 0);
        capsule.write(numTriangles, "numTriangles", 0);
        capsule.write(vertexStride, "vertexStride", 0);
        capsule.write(triangleIndexStride, "triangleIndexStride", 0);

        capsule.write(triangleIndexBase.array(), "triangleIndexBase", new byte[0]);
        capsule.write(vertexBase.array(), "vertexBase", new byte[0]);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
//        worldScale = (Vector3f) capsule.readSavable("worldScale", new Vector3f(1, 1, 1));
        numVertices = capsule.readInt("numVertices", 0);
        numTriangles = capsule.readInt("numTriangles", 0);
        vertexStride = capsule.readInt("vertexStride", 0);
        triangleIndexStride = capsule.readInt("triangleIndexStride", 0);

        triangleIndexBase = ByteBuffer.wrap(capsule.readByteArray("triangleIndexBase", new byte[0]));
        vertexBase = ByteBuffer.wrap(capsule.readByteArray("vertexBase", new byte[0]));
        createShape();
    }
    
    /**
     * Alter the scaling factors of this shape.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) changes can have unintended consequences.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=(1,1,1))
     */
    @Override
    public void setScale(Vector3f scale) {
        super.setScale(scale);
        recalcAabb(objectId);
    }

    native private void recalcAabb(long shapeId);

    /**
     * Instantiate the configured shape in Bullet.
     */
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
//        objectId = new GImpactMeshShape(tiv);
//        objectId.setLocalScaling(Converter.convert(worldScale));
//        ((GImpactMeshShape)objectId).updateBound();
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        meshId = NativeMeshUtil.createTriangleIndexVertexArray(triangleIndexBase, vertexBase, numTriangles, numVertices, vertexStride, triangleIndexStride);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Mesh {0}", Long.toHexString(meshId));
        objectId = createShape(meshId);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(long meshId);

    /**
     * Finalize this shape just before it is destroyed. Should be invoked only
     * by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Mesh {0}", Long.toHexString(meshId));
        finalizeNative(meshId);
    }

    private native void finalizeNative(long objectId);
}
