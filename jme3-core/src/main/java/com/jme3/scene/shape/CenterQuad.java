/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.io.IOException;

/**
 * A static, indexed, Triangles-mode mesh for an axis-aligned rectangle in the
 * X-Y plane.
 *
 * <p>The rectangle extends from (-width/2, -height/2, 0) to
 * (width/2, height/2, 0) with normals set to (0,0,1).
 *
 * <p>This differs from {@link com.jme3.scene.shape.Quad} because it puts
 * (0,0,0) at the rectangle's center instead of in a corner.
 *
 * @author Kirill Vainer
 */
public class CenterQuad extends Mesh {

    private float width;
    private float height;

    /**
     * For serialization only. Do not use.
     */
    protected CenterQuad() {
    }

    /**
     * Instantiate an unflipped quad in the X-Y plane with the specified width
     * and height.
     *
     * @param width the desired X extent or width
     * @param height the desired Y extent or height
     */
    public CenterQuad(float width, float height) {
        updateGeometry(width, height, false);
    }

    /**
     * Instantiate a quad in the X-Y plane with the specified width and height.
     *
     * @param width the desired X extent or width
     * @param height the desired Y extent or height
     * @param flipCoords true to flip the texture coordinates (v=0 when
     *     y=height/2) or false to leave them unflipped (v=1 when y=height/2)
     */
    public CenterQuad(float width, float height, boolean flipCoords) {
        updateGeometry(width, height, flipCoords);
    }

    /**
     * Returns the height (or Y extent).
     *
     * @return the height
     */
    public float getHeight() {
        return height;
    }

    /**
     * Returns the width (or X extent).
     *
     * @return the width
     */
    public float getWidth() {
        return width;
    }

    /**
     * De-serializes from the specified importer, for example when loading from
     * a J3O file.
     *
     * @param importer the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        width = capsule.readFloat("width", 0f);
        height = capsule.readFloat("height", 0f);
    }

    /**
     * Serializes to the specified exporter, for example when saving to a J3O
     * file. The current instance is unaffected.
     *
     * @param exporter the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(width, "width", 0f);
        capsule.write(height, "height", 0f);
    }

    private void updateGeometry(float width, float height, boolean flipCoords) {
        this.width = width;
        this.height = height;

        float x = width / 2;
        float y = height / 2;
        setBuffer(Type.Position, 3, new float[]{
            -x, -y, 0f,
            +x, -y, 0f,
            +x, +y, 0f,
            -x, +y, 0f
        });

        if (flipCoords) {
            setBuffer(Type.TexCoord, 2, new float[]{
                0f, 1f,
                1f, 1f,
                1f, 0f,
                0f, 0f
            });
        } else {
            setBuffer(Type.TexCoord, 2, new float[]{
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            });
        }

        setBuffer(Type.Normal, 3, new float[]{
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
        });

        if (width * height < 0f) {
            setBuffer(Type.Index, 3, new byte[]{
                0, 2, 1,
                0, 3, 2
            });
        } else {
            setBuffer(Type.Index, 3, new byte[]{
                0, 1, 2,
                0, 2, 3
            });
        }

        updateBound();
        setStatic();
    }
}
