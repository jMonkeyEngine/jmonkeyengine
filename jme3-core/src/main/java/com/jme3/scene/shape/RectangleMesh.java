/*
 * Copyright (c) 2021 jMonkeyEngine
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

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Rectangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;

/**
 * A static, indexed, Triangle-mode mesh that renders a rectangle or
 * parallelogram, with customizable normals and texture coordinates.
 *
 * <p>It uses a {@link com.jme3.math.Rectangle} to locate its vertices, which
 * are named as follows:
 *
 * <pre>
 *     C +----+ D
 *       |\   |
 *       | \  |
 *       |  \ |
 *       |   \|
 *     A +----+ B
 * </pre>
 *
 * <p>In the vertex buffers, the order of the vertices is A, B, D, then C.
 *
 * <p>The default texture coordinates have:<ul>
 * <li>U=0 at vertices A and C</li>
 * <li>U=1 at vertices B and D</li>
 * <li>V=0 at vertices A and B</li>
 * <li>V=1 at vertices C and D</li></ul>
 *
 * @author Francivan Bezerra
 */
public class RectangleMesh extends Mesh {

    /**
     * Used to locate the vertices and calculate a default normal.
     */
    private Rectangle rectangle;

    /**
     * Texture coordinates in A-B-D-C order.
     */
    private Vector2f[] texCoords;

    /**
     * Normal direction for all 4 vertices.
     */
    private Vector3f normal;

    /**
     * Used to indicate whether this mesh is flipped.
     */
    private boolean flipped;

    /**
     * Instantiates a unit-square mesh in the X-Y plane, centered at (0.5, 0.5),
     * with normals in the +Z direction.
     *
     */
    public RectangleMesh() {
        this(new Rectangle(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)));
    }

    /**
     * Instantiates a rectangle or parallelogram mesh based on the specified
     * {@link com.jme3.math.Rectangle}.
     *
     * @param rectangle to locate the vertices and set the normals (not null,
     *     alias created)
     */
    public RectangleMesh(Rectangle rectangle) {
        this.rectangle = rectangle;
        this.texCoords = new Vector2f[] {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        };
        flipped = false;
        updateMesh();
    }

    /**
     * Instantiates a rectangle or parallelogram mesh based on 3 specified
     * vertex positions.
     *
     * @param a the mesh position of vertex A (not null, alias created)
     * @param b the mesh position of vertex B (not null, alias created)
     * @param c the mesh position of vertex C (not null, alias created)
     */
    public RectangleMesh(Vector3f a, Vector3f b, Vector3f c) {
        this(new Rectangle(a, b, c));
    }

    /**
     * Provides access to the internal {@link com.jme3.math.Rectangle} on which
     * the mesh is based.
     *
     * @return the pre-existing instance (do not modify!)
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * Sets the {@link com.jme3.math.Rectangle} and updates the mesh
     * accordingly.
     *
     * @param rectangle the desired Rectangle (not null, alias created)
     */
    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
        updateMesh();
    }

    /**
     * Provides access to the internal texture-coordinate array.
     *
     * @return the pre-existing array of length 4 (do not modify!)
     */
    public Vector2f[] getTexCoords() {
        return texCoords;
    }

    /**
     * Sets the texture coordinates and updates the mesh accordingly.
     *
     * @param texCoords the desired texture coordinates for each vertex (not
     *     null, alias created)
     * @throws IllegalArgumentException if the array length isn't exactly 4
     */
    public void setTexCoords(Vector2f[] texCoords) throws IllegalArgumentException {
        if (texCoords.length != 4) {
            throw new IllegalArgumentException(
                    "Texture coordinates are 4 vertices, therefore a Vector2f array of length 4 must be provided.");
        }
        this.texCoords = texCoords;
        updateMesh();
    }

    /**
     * Provides access to the internal normal-direction vector.
     *
     * @return the pre-existing vector (do not modify!)
     */
    public Vector3f getNormal() {
        return normal;
    }

    /**
     * Flips this mesh by reversing its normal vector direction and
     * setting the {@code flipped} variable accordingly. This variable
     * will be used by the {@code updateMesh()} method to rearrange
     * the index buffer.
     */
    public void flip() {
        normal.negateLocal();
        flipped = !flipped;
        updateMesh();
    }

    protected void updateMesh() {
        Vector3f a = rectangle.getA();
        Vector3f b = rectangle.getB();
        Vector3f c = rectangle.getC();
        Vector3f d = rectangle.calculateD();
        setBuffer(Type.Position, 3, new float[] {
                a.x, a.y, a.z,
                b.x, b.y, b.z,
                d.x, d.y, d.z,
                c.x, c.y, c.z
        });

        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));

        if (normal == null) {
            normal = rectangle.calculateNormal(null);
        }
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normal, normal, normal, normal));

        if (flipped) {
            setBuffer(Type.Index, 3, new short[]{1, 0, 3, 3, 2, 1});
        } else {
            setBuffer(Type.Index, 3, new short[]{3, 0, 1, 1, 2, 3});
        }

        updateBound();
        setStatic();
    }

    /**
     * Called internally by com.jme3.util.clone.Cloner. Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        this.rectangle = cloner.clone(rectangle);
        this.texCoords = cloner.clone(texCoords);
        this.normal = cloner.clone(normal);
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        final InputCapsule capsule = importer.getCapsule(this);
        rectangle = (Rectangle) capsule.readSavable("rectangle", new Rectangle(
                new Vector3f(),
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0)));
        texCoords = (Vector2f[]) capsule.readSavableArray("texCoords", new Vector2f[] {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1) });
        normal = (Vector3f) capsule.readSavable("normal", null);
        flipped = capsule.readBoolean("flipped", false);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(rectangle, "rectangle", new Rectangle(
                new Vector3f(),
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0)));
        capsule.write(texCoords, "texCoords", new Vector2f[] {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        });
        capsule.write(normal, "normal", null);
        capsule.write(flipped, "flipped", false);
    }
}
