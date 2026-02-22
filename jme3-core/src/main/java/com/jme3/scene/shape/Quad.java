/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.GlVertexBuffer.Type;
import com.jme3.vulkan.mesh.AdaptiveMesh;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.attribute.Attribute;

import java.io.IOException;

/**
 * <code>Quad</code> represents a rectangular plane in space
 * defined by 4 vertices. The quad's lower-left side is contained
 * at the local space origin (0, 0, 0), while the upper-right
 * side is located at the width/height coordinates (width, height, 0).
 *
 * @author Kirill Vainer
 */
public class Quad extends AdaptiveMesh {

    private float width;
    private float height;

    protected Quad(MeshLayout layout) {
        super(layout, 4, 1);
        
    }

    /**
     * Create a quad with the given width and height. The quad
     * is always created in the XY plane.
     *
     * @param width The X extent or width
     * @param height The Y extent or width
     */
    public Quad(MeshLayout layout, float width, float height) {
        this(layout);
        updateGeometry(width, height);
    }

    /**
     * Create a quad with the given width and height. The quad
     * is always created in the XY plane.
     *
     * @param width The X extent or width
     * @param height The Y extent or width
     * @param flipCoords If true, the texture coordinates will be flipped
     * along the Y axis.
     */
    public Quad(MeshLayout layout, float width, float height, boolean flipCoords) {
        this(layout);
        updateGeometry(width, height, flipCoords);
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public void updateGeometry(float width, float height) {
        updateGeometry(width, height, false);
    }

    public void updateGeometry(float width, float height, boolean flipCoords) {
        this.width = width;
        this.height = height;
        try (Attribute<Vector3f> pos = mapAttribute(Type.Position)) {
            Vector3f temp = pos.createStorageObject(null);
            pos.set(0, temp.set(0, 0, 0));
            pos.set(1, temp.set(width, 0, 0));
            pos.set(2, temp.set(width, height, 0));
            pos.set(3, temp.set(0, height, 0));
        }
        try (Attribute<Vector2f> texCoord = mapAttribute(Type.TexCoord)) {
            Vector2f temp = texCoord.createStorageObject(null);
            texCoord.set(0, temp.set(0, flipCoords ? 1 : 0));
            texCoord.set(1, temp.set(1, flipCoords ? 1 : 0));
            texCoord.set(2, temp.set(1, flipCoords ? 0 : 1));
            texCoord.set(3, temp.set(0, flipCoords ? 0 : 1));
        }
        try (Attribute<Vector3f> normal = mapAttribute(Type.Normal)) {
            for (Vector3f n : normal.write(null)) {
                n.set(0, 0, 1);
            }
        }

        if (height < 0) {
            setBuffer(Type.Index, 3, new short[]{0, 2, 1,
                                                 0, 3, 2});
        } else {
            setBuffer(Type.Index, 3, new short[]{0, 1, 2,
                                                 0, 2, 3});
        }
        indexBuffers.add(0, new );

        updateBound();
        setStatic();
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        width = capsule.readFloat("width", 0);
        height = capsule.readFloat("height", 0);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(width, "width", 0);
        capsule.write(height, "height", 0);
    }

}
