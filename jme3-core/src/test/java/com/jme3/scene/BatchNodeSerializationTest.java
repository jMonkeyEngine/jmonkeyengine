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

import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.scene.shape.Quad;
import com.jme3.system.TestUtil;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that batch nodes fail clearly when serialization is requested.
 */
public class BatchNodeSerializationTest {

    private static final AssetManager assetManager = TestUtil.createAssetManager();

    @Test
    public void batchNodeRejectsSerialization() throws Exception {
        assertSerializationUnsupported(new BatchNode("batch"));
    }

    @Test
    public void simpleBatchNodeRejectsSerialization() throws Exception {
        assertSerializationUnsupported(new SimpleBatchNode("batch"));
    }

    private void assertSerializationUnsupported(BatchNode node) throws Exception {
        Geometry geometry = new Geometry("source", new Quad(1, 1));
        geometry.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        node.attachChild(geometry);
        node.batch();

        assertEquals(1, node.batches.size());
        assertThrows(UnsupportedOperationException.class,
                () -> BinaryExporter.getInstance().save(node, new ByteArrayOutputStream()));
        assertThrows(UnsupportedOperationException.class, () -> node.read(null));
    }
}
