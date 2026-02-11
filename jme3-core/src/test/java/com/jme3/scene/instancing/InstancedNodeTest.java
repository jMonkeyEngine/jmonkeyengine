/*
 * Copyright (c) 2025 jMonkeyEngine
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
package com.jme3.scene.instancing;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link InstancedNode} class works correctly.
 *
 * @author copilot
 */
public class InstancedNodeTest {

    private static final AssetManager assetManager = new DesktopAssetManager();

    /**
     * Test that InstancedNode serialization preserves the InstanceNodeControl.
     */
    @Test
    public void testSerializationPreservesControl() throws Exception {
        // Create an InstancedNode
        InstancedNode instancedNode = new InstancedNode("test_instanced_node");

        // Create a material with instancing enabled
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setBoolean("UseInstancing", true);
        material.setColor("Color", ColorRGBA.Red);

        // Add some instanced geometries
        for (int i = 0; i < 5; i++) {
            Box box = new Box(1, 1, 1);
            Geometry geo = new Geometry("box_" + i, box);
            geo.setMaterial(material);
            geo.setLocalTranslation(new Vector3f(i * 3, 0, 0));
            instancedNode.attachChild(geo);
        }

        // Instance the geometries
        instancedNode.instance();

        // Verify the control exists before serialization
        Assert.assertEquals("InstancedNode should have 1 control before serialization", 
                            1, instancedNode.getNumControls());

        // Serialize and deserialize
        InstancedNode loaded = (InstancedNode) BinaryExporter.saveAndLoad(assetManager, instancedNode);

        // Verify the control exists after deserialization
        Assert.assertNotNull("Loaded InstancedNode should not be null", loaded);
        Assert.assertEquals("InstancedNode should have 1 control after deserialization", 
                            1, loaded.getNumControls());
        
        // Verify the control is the right type
        Assert.assertNotNull("Control should not be null", loaded.getControl(0));
    }

    /**
     * Test that an empty InstancedNode can be serialized.
     */
    @Test
    public void testEmptyInstancedNodeSerialization() throws Exception {
        InstancedNode instancedNode = new InstancedNode("empty_instanced_node");

        // Verify the control exists before serialization
        Assert.assertEquals("Empty InstancedNode should have 1 control before serialization", 
                            1, instancedNode.getNumControls());

        // Serialize and deserialize
        InstancedNode loaded = (InstancedNode) BinaryExporter.saveAndLoad(assetManager, instancedNode);

        // Verify the control exists after deserialization
        Assert.assertNotNull("Loaded empty InstancedNode should not be null", loaded);
        Assert.assertEquals("Empty InstancedNode should have 1 control after deserialization", 
                            1, loaded.getNumControls());
    }
}
