 /*
  * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.environment.util;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * <p>A `Circle` is a 2D mesh representing a circular outline (wireframe).
 * It's defined by a specified number of radial samples, which determine its smoothness.</p>
 *
 * <p>The circle is centered at (0,0,0) in its local coordinate space and has a radius of 1.0.</p>
 *
 * @author capdevon
 */
public class Circle extends Mesh {

    // The number of segments used to approximate the circle.
    protected int radialSamples = 256;

    /**
     * Creates a new `Circle` mesh.
     */
    public Circle() {
        setGeometryData();
        setIndexData();
    }

    /**
     * Initializes the vertex buffers for the circle mesh.
     */
    private void setGeometryData() {
        setMode(Mode.Lines);

        int numVertices = radialSamples + 1;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(numVertices);
        FloatBuffer colBuf = BufferUtils.createFloatBuffer(numVertices * 4);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(numVertices);

        // --- Generate Geometry Data ---
        float angleStep = FastMath.TWO_PI / radialSamples;

        // Define the color for the entire circle.
        ColorRGBA color = ColorRGBA.Orange;

        // Populate the position, color, and texture coordinate buffers.
        for (int i = 0; i < numVertices; i++) {
            float angle = angleStep * i;
            float cos = FastMath.cos(angle);
            float sin = FastMath.sin(angle);

            posBuf.put(cos).put(sin).put(0);
            colBuf.put(color.r).put(color.g).put(color.b).put(color.a);
            texBuf.put(i % 2f).put(i % 2f);
        }

        setBuffer(Type.Position, 3, posBuf);
        setBuffer(Type.Color, 4, colBuf);
        setBuffer(Type.TexCoord, 2, texBuf);

        updateBound();
        setStatic();
    }

    /**
     * Initializes the index buffer for the circle mesh.
     */
    private void setIndexData() {
        // allocate connectivity
        int numIndices = radialSamples * 2;

        ShortBuffer idxBuf = BufferUtils.createShortBuffer(numIndices);
        setBuffer(Type.Index, 2, idxBuf);

        // --- Generate Index Data ---
        for (int i = 0; i < radialSamples; i++) {
            idxBuf.put((short) i);         // Start of segment
            idxBuf.put((short) (i + 1));   // End of segment
        }
    }

    /**
     * Creates a {@link Geometry} object representing a dashed wireframe circle.
     *
     * @param assetManager The application's AssetManager to load materials.
     * @param name         The desired name for the Geometry.
     * @return A new Geometry instance with a `Circle` mesh.
     */
    public static Geometry createShape(AssetManager assetManager, String name) {
        Circle mesh = new Circle();
        Geometry geom = new Geometry(name, mesh);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Dashed.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setLineWidth(2f);
        mat.setColor("Color", ColorRGBA.Orange);
        mat.setFloat("DashSize", 0.5f);
        geom.setMaterial(mat);

        return geom;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(radialSamples, "radialSamples", 256);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        radialSamples = ic.readInt("radialSamples", 256);
    }

}
