/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.util.mikktspace;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;

import java.util.Objects;

/**
 *
 * @author Nehon
 */
public class MikkTSpaceImpl implements MikkTSpaceContext {

    private final IndexBuffer index;
    private final Attribute<Vector3f> positions, normals;
    private final Attribute<Vector2f> texCoords;
    private final Attribute<Vector4f> tangents;
    private final int triangleCount;
    private final Vector2f tempVec2 = new Vector2f();
    private final Vector3f tempVec3 = new Vector3f();
    private final Vector4f tempVec4 = new Vector4f();

    public MikkTSpaceImpl(Mesh mesh) {

        // todo: if the mesh lacks indices, generate a virtual index buffer.
        this.index = mesh.getIndexBuffer().mapIndices();
        this.triangleCount = mesh.getIndexBuffer().size().getElements() / 3;

        positions = Objects.requireNonNull(mesh.mapAttribute(GlVertexBuffer.Type.Position),
                "Position attribute required to generate tangents.");
        texCoords = Objects.requireNonNull(mesh.mapAttribute(GlVertexBuffer.Type.TexCoord),
                "TexCoord attribute required to generate tangents.");
        normals = Objects.requireNonNull(mesh.mapAttribute(GlVertexBuffer.Type.Normal),
                "Normal attribute required to generate tangents.");
        tangents = Objects.requireNonNull(mesh.mapAttribute(GlVertexBuffer.Type.Tangent),
                "Tangent attribute required to generate tangents.");

    }

    @Override
    public int getNumFaces() {
        return triangleCount;
    }

    @Override
    public int getNumVerticesOfFace(int face) {
        return 3;
    }

    @Override
    public void getPosition(float[] posOut, int face, int vert) {
        positions.get(getIndex(face, vert), tempVec3);
        posOut[0] = tempVec3.x;
        posOut[1] = tempVec3.y;
        posOut[2] = tempVec3.z;
    }

    @Override
    public void getNormal(float[] normOut, int face, int vert) {
        normals.get(getIndex(face, vert), tempVec3);
        normOut[0] = tempVec3.x;
        normOut[1] = tempVec3.y;
        normOut[2] = tempVec3.z;
    }

    @Override
    public void getTexCoord(float[] texOut, int face, int vert) {
        texCoords.get(getIndex(face, vert), tempVec2);
        texOut[0] = tempVec2.x;
        texOut[1] = tempVec2.y;
    }

    @Override
    public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
        tangents.set(getIndex(face, vert), tempVec4.set(tangent[0], tangent[1], tangent[2], sign));
    }

    @Override
    public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
        //Do nothing
    }

    @Override
    public void close() {
        tangents.push();
        positions.unmap();
        texCoords.unmap();
        normals.unmap();
        tangents.unmap();
    }

    private int getIndex(int face, int vert) {
        return index.get(face * 3 + vert);
    }

}
