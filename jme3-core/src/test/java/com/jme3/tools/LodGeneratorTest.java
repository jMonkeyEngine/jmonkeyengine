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

package com.jme3.tools;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.TestUtil;

import jme3tools.optimize.LodGenerator;

/**
 * Tests the result of the LodGenerator.
 *
 * @author Melvyn Linke
 */
public class LodGeneratorTest {
    AssetManager assetManager = TestUtil.createAssetManager();

    float[] REDUCTION_VALUES = { 0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f, 0.80f };

    /**
     * Tests the construction of the LodGenerator.
     */
    @Test
    public void testInit() {
        LodGenerator lod = new LodGenerator(sphere());
        assert true;
    }

    /**
     * Returns a List of the sizes of the VertexBuff.
     */
    private int[] getBufferSizes(VertexBuffer[] buffers) {
        int[] result = new int[buffers.length];

        for (int i = 0; i < buffers.length; i++) {
            result[i] = buffers[i].getNumElements();
        }

        return result;
    }

    /**
     * Tests the LodGenerator with proportional reduction on a sphere(see sphere()).
     */
    @Test
    public void testSphereReductionProportional() {
        LodGenerator lod = new LodGenerator(sphere());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL,
                REDUCTION_VALUES);

        int[] expected = { 240, 120, 108, 96, 84, 72, 60, 48 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    /**
     * Tests the LodGenerator with collapse cost reduction on a sphere(see sphere()).
     */
    @Test
    public void testSphereReductionCollapsCost() {
        LodGenerator lod = new LodGenerator(sphere());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST,
                REDUCTION_VALUES);

        int[] expected = { 240, 6, 2, 1 };
        int[] actual = getBufferSizes(buffer);
        assert buffer != null;
        assertArrayEquals(expected, actual);

    }

    /**
     * Returns the mesh of a node.
     */
    private Mesh getMesh(Node node) {
        Mesh m = null;
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                m = ((Geometry) spatial).getMesh();
                if (m.getVertexCount() == 5108) {

                }

            }
        }
        return m;
    }

    /**
     * Returns the Monkey mesh used in the TestLodGeneration stresstest. Note: Doesn't work durring gradle
     * build.
     */
    private Mesh monkey() {
        Node model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        return getMesh(model);
    }

    /**
     * Returns a 12x12 Sphere mesh.
     */
    private Mesh sphere() {
        return new Sphere(12, 12, 1, false, false);
    }

    /**
     * Tests the LodGenerator with constnat reduction on a monkey(see monkey()).
     */
    // @Test
    public void testMonkeyReductionConstant() {

        LodGenerator lod = new LodGenerator(monkey());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.CONSTANT,
                REDUCTION_VALUES);

        int[] expected = { 5108 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    /**
     * Tests the LodGenerator with proportional reduction on a sphere(see sphere()).
     */
    // @Test
    public void testMonkeyReductionProportional() {

        LodGenerator lod = new LodGenerator(monkey());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL,
                REDUCTION_VALUES);

        int[] expected = { 5108, 2553, 2298, 2043, 1787, 1531, 1276, 1021 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    /**
     * Tests the LodGenerator with collapse cost reduction on a monkey(see monkey()).
     */
    // @Test
    public void testMonkeyReductionCollapsCost() {
        LodGenerator lod = new LodGenerator(monkey());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST,
                REDUCTION_VALUES);

        int[] expected = { 5108, 16 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }
}