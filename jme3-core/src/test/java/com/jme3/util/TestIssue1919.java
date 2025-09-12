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
package com.jme3.util;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import java.nio.FloatBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies how MikktspaceTangentGenerator handles various mesh modes. This was
 * issue #1919 at GitHub.
 *
 * @author Stephen Gold
 */
public class TestIssue1919 {
    /**
     * The number of axes in a vector.
     */
    private static final int numAxes = 3;

    /**
     * Tests a Hybrid-mode mesh.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testHybrid() {
        Geometry testGeometry = createGeometry(Mesh.Mode.Hybrid);
        MikktspaceTangentGenerator.generate(testGeometry);
    }

    /**
     * Tests a LineLoop-mode mesh.
     */
    @Test
    public void testLineLoop() {
        Geometry testGeometry = createGeometry(Mesh.Mode.LineLoop);
        MikktspaceTangentGenerator.generate(testGeometry);

        Mesh mesh = testGeometry.getMesh();
        VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
        Assert.assertNull(tangents); /// skipped this mesh
    }

    /**
     * Tests a LineStrip-mode mesh.
     */
    @Test
    public void testLineStrip() {
        Geometry testGeometry = createGeometry(Mesh.Mode.LineStrip);
        MikktspaceTangentGenerator.generate(testGeometry);

        Mesh mesh = testGeometry.getMesh();
        VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
        Assert.assertNull(tangents); /// skipped this mesh
    }

    /**
     * Tests a Lines-mode mesh.
     */
    @Test
    public void testLines() {
        Geometry testGeometry = createGeometry(Mesh.Mode.Lines);
        MikktspaceTangentGenerator.generate(testGeometry);

        Mesh mesh = testGeometry.getMesh();
        VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
        Assert.assertNull(tangents); // skipped this mesh
    }

    /**
     * Tests a Patch-mode mesh.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testPatch() {
        Geometry testGeometry = createGeometry(Mesh.Mode.Patch);
        MikktspaceTangentGenerator.generate(testGeometry);
    }

    /**
     * Tests a Points-mode mesh.
     */
    @Test
    public void testPoints() {
        Geometry testGeometry = createGeometry(Mesh.Mode.Points);
        MikktspaceTangentGenerator.generate(testGeometry);

        Mesh mesh = testGeometry.getMesh();
        VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
        Assert.assertNull(tangents); // skipped this mesh
    }

    /**
     * Tests a Triangles-mode mesh.
     */
    @Test
    public void testTriangles() {
        Geometry testGeometry = createGeometry(Mesh.Mode.Triangles);
        MikktspaceTangentGenerator.generate(testGeometry);

        Mesh mesh = testGeometry.getMesh();
        VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
        Assert.assertNotNull(tangents); // generated tangents
    }

    /**
     * Generates a geometry in the X-Z plane with the specified mesh mode.
     *
     * @param mode the desired mode (not null)
     * @return a new geometry
     */
    private Geometry createGeometry(Mesh.Mode mode) {
        FloatBuffer normals = BufferUtils.createFloatBuffer(
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f
        );
        float uvDiameter = 5f;
        FloatBuffer uvs = BufferUtils.createFloatBuffer(
                uvDiameter, uvDiameter,
                0f, 0f,
                uvDiameter, 0f,
                uvDiameter, uvDiameter,
                0f, uvDiameter,
                0f, 0f
        );
        float posRadius = 500f;
        FloatBuffer positions = BufferUtils.createFloatBuffer(
                +posRadius, 0f, +posRadius,
                -posRadius, 0f, -posRadius,
                -posRadius, 0f, +posRadius,
                +posRadius, 0f, +posRadius,
                +posRadius, 0f, -posRadius,
                -posRadius, 0f, -posRadius
        );
        Mesh mesh = new Mesh();
        mesh.setMode(mode);
        mesh.setBuffer(VertexBuffer.Type.Normal, numAxes, normals);
        mesh.setBuffer(VertexBuffer.Type.Position, numAxes, positions);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvs);
        mesh.updateBound();

        Geometry result = new Geometry("testGeometry" + mode, mesh);
        return result;
    }
}
