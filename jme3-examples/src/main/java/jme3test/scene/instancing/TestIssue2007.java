/*
 * Copyright (c) 2023 jMonkeyEngine
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
package jme3test.scene.instancing;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.water.WaterFilter;

/**
 * Test for JMonkeyEngine issue #2007: instanced objects are culled when using
 * WaterFilter.
 * <p>
 * If the issue is resolved, rotating the camera up and down shouldn't cause
 * test boxes to pop in and out of existence.
 * <p>
 * Based on code submitted by foxcc2021.
 */
public class TestIssue2007 extends SimpleApplication {
    /**
     * Main entry point for the HelloIssue2007 application.
     *
     * @param args array of command-line arguments (not null)
     */
    public static void main(String[] args) {
        TestIssue2007 test = new TestIssue2007();
        test.start();
    }

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1, -1, -1));
        rootNode.addLight(light);

        // Create a material for instancing:
        Material mat = new Material(
                assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseInstancing", true);

        // Create a Mesh for test boxes:
        Mesh mesh = new Box(0.5f, 0.5f, 0.5f);

        // Apply a WaterFilter to the main viewport:
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        WaterFilter waterFilter = new WaterFilter();
        waterFilter.setReflectionScene(rootNode);
        fpp.addFilter(waterFilter);
        viewPort.addProcessor(fpp);

        InstancedNode instanceNode = new InstancedNode("TestInstancedNode");
        rootNode.attachChild(instanceNode);

        // Attach 1000 instanced test boxes to the scene:
        for (int i = 0; i < 1000; i++) {
            Geometry obj = new Geometry("TestBox" + i, mesh);
            obj.setLocalTranslation(0, 10, 0);
            obj.setLocalScale(1, 1, 1);
            obj.setMaterial(mat);
            obj.setLocalTranslation(i, i, 0);
            instanceNode.attachChild(obj);
        }
        instanceNode.instance();
    }
}
