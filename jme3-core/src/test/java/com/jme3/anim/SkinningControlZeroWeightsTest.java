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
 * documentation and/or other materials provided with the distribution.
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
package com.jme3.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

/**
 * Tests software skinning when a mesh has no bone-weight influences.
 */
public class SkinningControlZeroWeightsTest {

    @Test
    public void zeroWeightMeshWithoutTangentsRemainsInBindPose() {
        assertZeroWeightMeshRemainsInBindPose(false);
    }

    @Test
    public void zeroWeightMeshWithTangentsRemainsInBindPose() {
        assertZeroWeightMeshRemainsInBindPose(true);
    }

    private void assertZeroWeightMeshRemainsInBindPose(boolean withTangents) {
        Mesh mesh = new Mesh();
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(1f, 2f, 3f);
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(0f, 0f, 1f);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, positionBuffer);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
        mesh.setBuffer(VertexBuffer.Type.BoneIndex, 4, BufferUtils.createByteBuffer(4));
        mesh.setBuffer(VertexBuffer.Type.BoneWeight, 4, BufferUtils.createFloatBuffer(4));
        mesh.setMaxNumWeights(0);

        FloatBuffer tangentBuffer = null;
        if (withTangents) {
            tangentBuffer = BufferUtils.createFloatBuffer(1f, 0f, 0f, 1f);
            mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tangentBuffer);
        }
        mesh.generateBindPose();

        positionBuffer.put(0, 9f);
        positionBuffer.put(1, 9f);
        positionBuffer.put(2, 9f);
        normalBuffer.put(0, 1f);
        normalBuffer.put(1, 0f);
        normalBuffer.put(2, 0f);
        if (tangentBuffer != null) {
            tangentBuffer.put(0, 0f);
            tangentBuffer.put(1, 1f);
            tangentBuffer.put(2, 0f);
            tangentBuffer.put(3, 1f);
        }

        Geometry geometry = new Geometry("zero-weight", mesh);
        SkinningControl control = new SkinningControl(new Armature(new Joint[]{new Joint("root")}));
        control.setHardwareSkinningPreferred(false);
        control.setSpatial(geometry);

        control.controlRender(null, null);

        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
        assertEquals(1f, positions.get(0), 0f);
        assertEquals(2f, positions.get(1), 0f);
        assertEquals(3f, positions.get(2), 0f);
        FloatBuffer normals = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Normal).getData();
        assertEquals(0f, normals.get(0), 0f);
        assertEquals(0f, normals.get(1), 0f);
        assertEquals(1f, normals.get(2), 0f);
        if (withTangents) {
            FloatBuffer tangents = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Tangent).getData();
            assertEquals(1f, tangents.get(0), 0f);
            assertEquals(0f, tangents.get(1), 0f);
            assertEquals(0f, tangents.get(2), 0f);
            assertEquals(1f, tangents.get(3), 0f);
        }
    }
}