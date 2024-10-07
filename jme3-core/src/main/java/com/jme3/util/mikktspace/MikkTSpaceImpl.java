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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 *
 * @author Nehon
 */
public class MikkTSpaceImpl implements MikkTSpaceContext {

    Mesh mesh;
    final private IndexBuffer index;

    public MikkTSpaceImpl(Mesh mesh) {
        this.mesh = mesh;

        // If the mesh lacks indices, generate a virtual index buffer.
        this.index = mesh.getIndicesAsList();

        //replacing any existing tangent buffer, if you came here you want them new.
        mesh.clearBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer fb = BufferUtils.createFloatBuffer(mesh.getVertexCount() * 4);
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, fb);
    }

    @Override
    public int getNumFaces() {
        return mesh.getTriangleCount();        
    }

    @Override
    public int getNumVerticesOfFace(int face) {
        return 3;
    }

    @Override
    public void getPosition(float[] posOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer position = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer pos = (FloatBuffer) position.getData();
        pos.position(vertIndex * 3);
        posOut[0] = pos.get();
        posOut[1] = pos.get();
        posOut[2] = pos.get();
    }

    @Override
    public void getNormal(float[] normOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer normal = mesh.getBuffer(VertexBuffer.Type.Normal);
        FloatBuffer norm = (FloatBuffer) normal.getData();
        norm.position(vertIndex * 3);
        normOut[0] = norm.get();
        normOut[1] = norm.get();
        normOut[2] = norm.get();
    }

    @Override
    public void getTexCoord(float[] texOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer texCoord = mesh.getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer tex = (FloatBuffer) texCoord.getData();
        tex.position(vertIndex * 2);
        texOut[0] = tex.get();
        texOut[1] = tex.get();        
    }

    @Override
    public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer tangentBuffer = mesh.getBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer tan = (FloatBuffer) tangentBuffer.getData();
        
        tan.position(vertIndex * 4);
        tan.put(tangent);
        tan.put(sign);
        
        tan.rewind();
        tangentBuffer.setUpdateNeeded();
    }

    @Override
    public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
        //Do nothing
    }

    private int getIndex(int face, int vert) {
        int vertIndex = index.get(face * 3 + vert);
        return vertIndex;
    }

}
