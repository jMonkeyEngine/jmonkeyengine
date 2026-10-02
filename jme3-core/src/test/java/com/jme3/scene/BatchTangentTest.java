/*
 * Copyright (c) 2026 jMonkeyEngine
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

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.shape.Quad;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that extending an existing batch preserves its tangent basis.
 */
public class BatchTangentTest {

    @Test
    public void addingToBatchPreservesTangentHandedness() {
        checkIncrementalBatch(new BatchNode("batch"));
    }

    @Test
    public void addingToSimpleBatchPreservesTangentHandedness() {
        checkIncrementalBatch(new SimpleBatchNode("batch"));
    }

    private void checkIncrementalBatch(BatchNode node) {
        Material material = new Material(new MaterialDef(new DesktopAssetManager(), "test"));
        Geometry first = geometry(material, -1);
        first.rotate(0, 0, 0.5f);
        first.move(2, 3, 4);
        node.attachChild(first);
        node.batch();
        node.updateGeometricState();
        Mesh initial = getBatchMesh(node);
        checkHandedness(initial, 0, first.getVertexCount(), -1);
        float[] initialPositions = copyBuffer(initial, VertexBuffer.Type.Position);
        float[] initialNormals = copyBuffer(initial, VertexBuffer.Type.Normal);
        float[] initialTangents = copyBuffer(initial, VertexBuffer.Type.Tangent);

        Geometry second = geometry(material, 1);
        node.attachChild(second);
        node.batch();
        node.updateGeometricState();
        Mesh extended = getBatchMesh(node);
        checkHandedness(extended, first.getVertexCount(), second.getVertexCount(), 1);
        checkHandedness(extended, 0, first.getVertexCount(), -1);
        checkPrefix(extended, VertexBuffer.Type.Position, initialPositions);
        checkPrefix(extended, VertexBuffer.Type.Normal, initialNormals);
        checkPrefix(extended, VertexBuffer.Type.Tangent, initialTangents);

        // A third batch pass must retain both the old and recently added signs.
        Geometry third = geometry(material, -1);
        node.attachChild(third);
        node.batch();
        node.updateGeometricState();
        Mesh repeated = getBatchMesh(node);
        checkHandedness(repeated, 0, first.getVertexCount(), -1);
        checkHandedness(repeated, first.getVertexCount(), second.getVertexCount(), 1);
        checkHandedness(repeated, first.getVertexCount() + second.getVertexCount(), third.getVertexCount(), -1);
    }

    private Geometry geometry(Material material, float sign) {
        Mesh mesh = new Quad(1, 1);
        float[] tangent = new float[mesh.getVertexCount() * 4];
        for (int i = 0; i < tangent.length; i += 4) {
            tangent[i] = 1;
            tangent[i + 3] = sign;
        }
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tangent);
        Geometry geometry = new Geometry("source", mesh);
        geometry.setMaterial(material);
        return geometry;
    }

    private Mesh getBatchMesh(BatchNode node) {
        Mesh result = null;
        for (Spatial child : node.getChildren()) {
            if (node.isBatch(child)) {
                result = ((Geometry) child).getMesh();
            }
        }
        assertNotNull(result);
        return result;
    }

    private float[] copyBuffer(Mesh mesh, VertexBuffer.Type type) {
        FloatBuffer buffer = mesh.getFloatBuffer(type);
        float[] result = new float[buffer.limit()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    private void checkPrefix(Mesh mesh, VertexBuffer.Type type, float[] expected) {
        FloatBuffer buffer = mesh.getFloatBuffer(type);
        float[] actual = new float[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = buffer.get(i);
        }
        assertArrayEquals(expected, actual);
    }

    private void checkHandedness(Mesh mesh, int start, int count, float expected) {
        FloatBuffer tangent = mesh.getFloatBuffer(VertexBuffer.Type.Tangent);
        for (int i = start; i < start + count; i++) {
            assertEquals(expected, tangent.get(i * 4 + 3));
        }
    }
}
