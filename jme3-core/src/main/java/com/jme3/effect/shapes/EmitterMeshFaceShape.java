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
package com.jme3.effect.shapes;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This emitter shape emits the particles from the given shape's faces.
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshFaceShape extends EmitterMeshVertexShape {

    /**
     * Empty constructor. Sets nothing.
     */
    public EmitterMeshFaceShape() {
    }

    /**
     * Constructor. It stores a copy of vertex list of all meshes.
     * @param meshes
     *        a list of meshes that will form the emitter's shape
     */
    public EmitterMeshFaceShape(List<Mesh> meshes) {
        super(meshes);
    }

    @Override
    public void setMeshes(List<Mesh> meshes) {
        this.vertices = new ArrayList<List<Vector3f>>(meshes.size());
        this.normals = new ArrayList<List<Vector3f>>(meshes.size());
        for (Mesh mesh : meshes) {
            Vector3f[] vertexTable = BufferUtils.getVector3Array(mesh.getFloatBuffer(Type.Position));
            int[] indices = new int[3];
            List<Vector3f> vertices = new ArrayList<>(mesh.getTriangleCount() * 3);
            List<Vector3f> normals = new ArrayList<>(mesh.getTriangleCount());
            for (int i = 0; i < mesh.getTriangleCount(); ++i) {
                mesh.getTriangle(i, indices);
                vertices.add(vertexTable[indices[0]]);
                vertices.add(vertexTable[indices[1]]);
                vertices.add(vertexTable[indices[2]]);
                normals.add(FastMath.computeNormal(vertexTable[indices[0]], vertexTable[indices[1]], vertexTable[indices[2]]));
            }
            this.vertices.add(vertices);
            this.normals.add(normals);
        }
    }

    /**
     * Randomly selects a point on a random face.
     *
     * @param store
     *        storage for the coordinates of the selected point
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        // the index of the first vertex of a face (must be dividable by 3)
        int vertIndex = FastMath.nextRandomInt(0, vertices.get(meshIndex).size() / 3 - 1) * 3;
        // put the point somewhere between the first and the second vertex of a face
        float moveFactor = FastMath.nextRandomFloat();
        store.set(Vector3f.ZERO);
        store.addLocal(vertices.get(meshIndex).get(vertIndex));
        store.addLocal((vertices.get(meshIndex).get(vertIndex + 1).x - vertices.get(meshIndex).get(vertIndex).x) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 1).y - vertices.get(meshIndex).get(vertIndex).y) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 1).z - vertices.get(meshIndex).get(vertIndex).z) * moveFactor);
        // move the result towards the last face vertex
        moveFactor = FastMath.nextRandomFloat();
        store.addLocal((vertices.get(meshIndex).get(vertIndex + 2).x - store.x) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 2).y - store.y) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 2).z - store.z) * moveFactor);
    }

    /**
     * Randomly selects a point on a random face.
     * The {@code normal} argument is set to the normal of the selected face.
     *
     * @param store
     *        storage for the coordinates of the selected point
     * @param normal
     *        storage for the normal of the selected face
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        // the index of the first vertex of a face (must be dividable by 3)
        int faceIndex = FastMath.nextRandomInt(0, vertices.get(meshIndex).size() / 3 - 1);
        int vertIndex = faceIndex * 3;
        // put the point somewhere between the first and the second vertex of a face
        float moveFactor = FastMath.nextRandomFloat();
        store.set(Vector3f.ZERO);
        store.addLocal(vertices.get(meshIndex).get(vertIndex));
        store.addLocal((vertices.get(meshIndex).get(vertIndex + 1).x - vertices.get(meshIndex).get(vertIndex).x) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 1).y - vertices.get(meshIndex).get(vertIndex).y) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 1).z - vertices.get(meshIndex).get(vertIndex).z) * moveFactor);
        // move the result towards the last face vertex
        moveFactor = FastMath.nextRandomFloat();
        store.addLocal((vertices.get(meshIndex).get(vertIndex + 2).x - store.x) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 2).y - store.y) * moveFactor, (vertices.get(meshIndex).get(vertIndex + 2).z - store.z) * moveFactor);
        normal.set(normals.get(meshIndex).get(faceIndex));
    }
}
