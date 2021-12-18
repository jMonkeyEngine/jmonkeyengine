/*
 * Copyright (c) 2017 jMonkeyEngine
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
package com.jme3.collision;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.material.Material;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.system.JmeSystem;
import com.jme3.system.MockJmeSystemDelegate;
import org.junit.Test;

/**
 * Verify that collideWith() works with ignoreTransforms. This was issue #744 at
 * GitHub.
 *
 * @author Stephen Gold
 */
public class CollideIgnoreTransformTest {

    AssetManager assetManager;

    Node rootNode;

    /**
     * Cast a ray at the geometry and check the number of collisions.
     */
    void castRay(Ray ray, int expectedNumResults) {
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);
        int numResults = results.size();
        if (numResults != expectedNumResults) {
            String msg = String.format("Expected %d, got %d.",
                    expectedNumResults, numResults);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Attach a red square in the XY plane with its lower left corner at (0, 0,
     * 0). It is composed of 2 triangles.
     */
    void createRedSquare() {
        Mesh quadMesh = new Quad(1f, 1f);
        Geometry redSquare = new Geometry("red square", quadMesh);
        Material red = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        redSquare.setMaterial(red);
        rootNode.attachChild(redSquare);

        redSquare.setLocalTranslation(0f, 3f, 0f);
        redSquare.setIgnoreTransform(true);
    }

    @Test
    public void testPhantomTriangles() {
        JmeSystem.setSystemDelegate(new MockJmeSystemDelegate());
        assetManager = new DesktopAssetManager();
        assetManager.registerLocator(null, ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3m", "j3md");
        rootNode = new Node();

        createRedSquare();

        rootNode.updateLogicalState(0.01f);
        rootNode.updateGeometricState();
        /*
         * ray in the -Z direction, starting from (0.5, 0.6, 10)
         */
        Ray ray1 = new Ray(/* origin */new Vector3f(0.5f, 0.6f, 10f),
                /* direction */ new Vector3f(0f, 0f, -1f));
        castRay(ray1, 1);
        /*
         * ray in the -Z direction, starting from (0.5, 3, 10)
         */
        Ray ray0 = new Ray(/* origin */new Vector3f(0.5f, 3f, 10f),
                /* direction */ new Vector3f(0f, 0f, -1f));
        castRay(ray0, 0);
    }
}
