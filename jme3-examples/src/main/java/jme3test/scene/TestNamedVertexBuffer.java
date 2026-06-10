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
package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Demonstrates a named vertex buffer consumed by a shader attribute.
 */
public class TestNamedVertexBuffer extends SimpleApplication {

    private static final String HEAT_ATTRIBUTE = "inHeat";

    private VertexBuffer heatBuffer;
    private FloatBuffer heatData;
    private float time;

    public static void main(String[] args) {
        TestNamedVertexBuffer app = new TestNamedVertexBuffer();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Named VertexBuffer Shader Attribute");
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(0f, 0f, 4f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(
                -1.4f, -1.0f, 0f,
                 1.4f, -1.0f, 0f,
                 1.4f,  1.0f, 0f,
                -1.4f,  1.0f, 0f));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createShortBuffer(
                (short) 0, (short) 1, (short) 2,
                (short) 0, (short) 2, (short) 3));

        heatData = BufferUtils.createFloatBuffer(0f, 0.35f, 1f, 0.65f);
        mesh.setBuffer(HEAT_ATTRIBUTE, 1, VertexBuffer.Format.Float, heatData);
        heatBuffer = mesh.getBuffer(HEAT_ATTRIBUTE);
        mesh.updateBound();

        Geometry geometry = new Geometry("Named vertex buffer quad", mesh);
        geometry.setMaterial(new Material(assetManager, "jme3test/vertexbuffer/NamedVertexBuffer.j3md"));
        rootNode.attachChild(geometry);
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        for (int i = 0; i < heatData.limit(); i++) {
            heatData.put(i, 0.5f + 0.5f * FastMath.sin(time + i * FastMath.HALF_PI));
        }
        heatBuffer.markElementsDirty(0, heatData.limit());
    }
}
