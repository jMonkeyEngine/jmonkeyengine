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
     * Constructor. Initializes the emitter shape with a list of meshes.
     * The vertices and normals for all triangles of these meshes are
     * extracted and stored internally.
     *
     * @param meshes a list of {@link Mesh} objects that will define the
     * shape from which particles are emitted.
     */
    public EmitterMeshFaceShape(List<Mesh> meshes) {
        super(meshes);
    }

    /**
     * Sets the meshes for this emitter shape. This method extracts all
     * triangle vertices and computes their normals, storing them internally
     * for subsequent particle emission.
     *
     * @param meshes a list of {@link Mesh} objects to set as the emitter's shape.
     */
    @Override
    public void setMeshes(List<Mesh> meshes) {
        this.vertices = new ArrayList<List<Vector3f>>(meshes.size());
        this.normals = new ArrayList<List<Vector3f>>(meshes.size());

        for (Mesh mesh : meshes) {
            Vector3f[] vertexTable = BufferUtils.getVector3Array(mesh.getFloatBuffer(Type.Position));
            int[] indices = new int[3];
            List<Vector3f> meshVertices = new ArrayList<>(mesh.getTriangleCount() * 3);
            List<Vector3f> meshNormals = new ArrayList<>(mesh.getTriangleCount());

            for (int i = 0; i < mesh.getTriangleCount(); ++i) {
                mesh.getTriangle(i, indices);

                Vector3f v1 = vertexTable[indices[0]];
                Vector3f v2 = vertexTable[indices[1]];
                Vector3f v3 = vertexTable[indices[2]];

                // Add all three vertices of the triangle
                meshVertices.add(v1);
                meshVertices.add(v2);
                meshVertices.add(v3);

                // Compute and add the normal for the current triangle face
                meshNormals.add(FastMath.computeNormal(v1, v2, v3));
            }
            this.vertices.add(meshVertices);
            this.normals.add(meshNormals);
        }
    }

    /**
     * Randomly selects a point on a random face of one of the stored meshes.
     * The point is generated using barycentric coordinates to ensure uniform
     * distribution within the selected triangle.
     *
     * @param store a {@link Vector3f} object where the coordinates of the
     *              selected point will be stored.
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        List<Vector3f> currVertices = vertices.get(meshIndex);
        int numVertices = currVertices.size();

        // the index of the first vertex of a face (must be dividable by 3)
        int faceIndex = FastMath.nextRandomInt(0, numVertices / 3 - 1);
        int vertIndex = faceIndex * 3;

        // Generate the random point on the triangle
        generateRandomPointOnTriangle(currVertices, vertIndex, store);
    }

    /**
     * Randomly selects a point on a random face of one of the stored meshes,
     * and also sets the normal of that selected face.
     * The point is generated using barycentric coordinates for uniform distribution.
     *
     * @param store  a {@link Vector3f} object where the coordinates of the
     *               selected point will be stored.
     * @param normal a {@link Vector3f} object where the normal of the
     *               selected face will be stored.
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        List<Vector3f> currVertices = vertices.get(meshIndex);
        int numVertices = currVertices.size();

        // the index of the first vertex of a face (must be dividable by 3)
        int faceIndex = FastMath.nextRandomInt(0, numVertices / 3 - 1);
        int vertIndex = faceIndex * 3;

        // Generate the random point on the triangle
        generateRandomPointOnTriangle(currVertices, vertIndex, store);
        // Set the normal from the pre-computed normals list for the selected face
        normal.set(normals.get(meshIndex).get(faceIndex));
    }

    /**
     * Internal method to generate a random point within a specific triangle
     * using barycentric coordinates.
     *
     * @param currVertices The list of vertices for the current mesh.
     * @param vertIndex    The starting index of the triangle's first vertex
     *                     within the {@code currVertices} list.
     * @param store        A {@link Vector3f} object where the calculated point will be stored.
     */
    private void generateRandomPointOnTriangle(List<Vector3f> currVertices, int vertIndex, Vector3f store) {

        Vector3f v1 = currVertices.get(vertIndex);
        Vector3f v2 = currVertices.get(vertIndex + 1);
        Vector3f v3 = currVertices.get(vertIndex + 2);

        // Generate random barycentric coordinates
        float u = FastMath.nextRandomFloat();
        float v = FastMath.nextRandomFloat();

        if ((u + v) > 1) {
            u = 1 - u;
            v = 1 - v;
        }

        // P = v1 + u * (v2 - v1) + v * (v3 - v1)
        store.x = v1.x + u * (v2.x - v1.x) + v * (v3.x - v1.x);
        store.y = v1.y + u * (v2.y - v1.y) + v * (v3.y - v1.y);
        store.z = v1.z + u * (v2.z - v1.z) + v * (v3.z - v1.z);
    }

}
