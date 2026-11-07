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
import com.jme3.shader.VarType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that extending a batch does not register the same batch again.
 */
public class BatchBookkeepingTest {

    @Test
    public void extendingBatchReusesItsEntry() {
        checkIncrementalBatch(new BatchNode("batch"));
    }

    @Test
    public void extendingSimpleBatchReusesItsEntry() {
        checkIncrementalBatch(new SimpleBatchNode("batch"));
    }

    private void checkIncrementalBatch(BatchNode node) {
        Material material = material();
        addGeometry(node, material);
        BatchNode.Batch original = node.batches.get(0);
        for (int i = 2; i <= 8; i++) {
            addGeometry(node, material);
            assertEquals(1, node.batches.size());
            assertSame(original, node.batches.get(0));
            assertEquals(i * 2, original.getGeometry().getMesh().getTriangleCount());
        }
    }

    @Test
    public void equalMaterialsReuseTheirEntry() {
        BatchNode node = new BatchNode("batch");
        Material material = material();
        addGeometry(node, material);
        addGeometry(node, material.clone());
        assertEquals(1, node.batches.size());
        assertEquals(4, node.batches.get(0).getGeometry().getMesh().getTriangleCount());
    }

    @Test
    public void differentMaterialsKeepSeparateEntries() {
        BatchNode node = new BatchNode("batch");
        Material first = material();
        Material second = first.clone();
        second.setFloat("Value", 1);
        addGeometry(node, first);
        addGeometry(node, second);
        assertEquals(2, node.batches.size());
        addGeometry(node, first);
        addGeometry(node, second);
        assertEquals(2, node.batches.size());
        for (BatchNode.Batch batch : node.batches) {
            assertEquals(4, batch.getGeometry().getMesh().getTriangleCount());
        }
    }

    @Test
    public void rebatchingWithoutNewGeometryKeepsItsEntry() {
        BatchNode node = new BatchNode("batch");
        addGeometry(node, material());
        BatchNode.Batch original = node.batches.get(0);
        node.batch();
        node.updateGeometricState();
        assertEquals(1, node.batches.size());
        assertSame(original, node.batches.get(0));
    }

    @Test
    public void fullRebatchAfterRemovalKeepsOnlyCurrentEntries() {
        BatchNode node = new BatchNode("batch");
        Material material = material();
        Geometry first = addGeometry(node, material);
        addGeometry(node, material);
        node.detachChild(first);
        node.batch();
        node.updateGeometricState();
        assertEquals(1, node.batches.size());
        assertEquals(2, node.batches.get(0).getGeometry().getMesh().getTriangleCount());
    }

    private Geometry addGeometry(BatchNode node, Material material) {
        Geometry geometry = new Geometry("source", new Quad(1, 1));
        geometry.setMaterial(material);
        node.attachChild(geometry);
        node.batch();
        node.updateGeometricState();
        return geometry;
    }

    private Material material() {
        MaterialDef def = new MaterialDef(new DesktopAssetManager(), "test");
        def.addMaterialParam(VarType.Float, "Value", 0f);
        return new Material(def);
    }
}
