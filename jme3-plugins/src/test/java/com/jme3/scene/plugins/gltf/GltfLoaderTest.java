/*
 * Copyright (c) 2017-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.plugin.TestMaterialWrite;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeSystem;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Created by Nehon on 07/08/2017.
 */
public class GltfLoaderTest {

    private final static String indentString = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

    private AssetManager assetManager;

    @Before
    public void init() {
        assetManager = JmeSystem.newAssetManager(
                TestMaterialWrite.class.getResource("/com/jme3/asset/Desktop.cfg"));

    }

    @Test
    public void testLoad() {
        Spatial scene = assetManager.loadModel("gltf/box/box.gltf");
        dumpScene(scene, 0);
//        scene = assetManager.loadModel("gltf/hornet/scene.gltf");
//        dumpScene(scene, 0);
    }

    @Test
    public void testLoadEmptyScene() {
        try {
            Spatial scene = assetManager.loadModel("gltf/box/boxWithEmptyScene.gltf");
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assert.fail("Failed to import gltf model with empty scene");
        }
    }

    @Test
    public void testLightsPunctualExtension() {
        try {
            Spatial scene = assetManager.loadModel("gltf/lights/lights.gltf");
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assert.fail("Failed to import gltf model with lights punctual extension");
        }
    }

    private void dumpScene(Spatial s, int indent) {
        System.err.print(indentString.substring(0, indent) + s.getName() + " (" + s.getClass().getSimpleName() + ") / " +
                s.getLocalTransform().getTranslation().toString() + ", " +
                s.getLocalTransform().getRotation().toString() + ", " +
                s.getLocalTransform().getScale().toString());
        if (s instanceof Geometry) {
            System.err.print(" / " + ((Geometry) s).getMaterial());
        }
        System.err.println();
        for (Light light : s.getLocalLightList()) {
            System.err.print(indentString.substring(0, indent + 1) + " (" + light.getClass().getSimpleName() + ")");
            if (light instanceof SpotLight) {
                Vector3f pos = ((SpotLight) light).getPosition();
                Vector3f dir = ((SpotLight) light).getDirection();
                System.err.println(" " + pos.toString() + ", " + dir.toString());
            } else if (light instanceof PointLight) {
                Vector3f pos = ((PointLight) light).getPosition();
                System.err.println(" " + pos.toString());
            } else if (light instanceof DirectionalLight) {
                Vector3f dir = ((DirectionalLight) light).getDirection();
                System.err.println(" " + dir.toString());
            } else {
                System.err.println();
            }
        }

        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                dumpScene(spatial, indent + 1);
            }
        }
    }
}