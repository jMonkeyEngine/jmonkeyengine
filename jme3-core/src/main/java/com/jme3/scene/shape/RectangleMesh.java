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
 * {@code RectangleMesh} is a rectangle in space similar to
 * {@link com.jme3.scene.shape.Quad}. It uses a {@link com.jme3.math.Rectangle}
 * to position its vertices.
 * 
 * @author Francivan Bezerra
 */
public class RectangleMesh extends Mesh {

    private Rectangle rectangle;

    private Vector2f[] texCoords;

    private Vector3f normal;

    /**
     * Creates a new rectangular mesh with sides of equal length (a.k.a. a square).
     * 
     */
    public RectangleMesh() {
        this(new Rectangle(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)));
    }

    /**
     * Creates a mesh with the given rectangle.
     * 
     * @param rectangle the rectangle used to build this mesh.
     */
    public RectangleMesh(Rectangle rectangle) {
        this.rectangle = rectangle;
        this.texCoords = new Vector2f[] {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        };
        updateMesh();
    }

    /**
     * Creates a mesh where points A, B and C define the area of the rectangle.
     * 
     * @param a the first corner of the rectangle.
     * @param b the second corner of the rectangle.
     * @param c the third corner of the rectangle.
     */
    public RectangleMesh(Vector3f a, Vector3f b, Vector3f c) {
        this(new Rectangle(a, b, c));
    }

    /**
     * Returns the rectangle used to build this mesh.
     * 
     * @return the rectangle used to build this mesh.
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * Sets the rectangle that will be used to build this mesh.
     * 
     * @param rectangle the rectangle used to build this mesh.
     */
    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
        updateMesh();
    }

    /**
     * Returns the texture coordinates.
     * 
     * @return the texture coordinates
     */
    public Vector2f[] getTexCoords() {
        return texCoords;
    }

    /**
     * Sets the texture coordinates.
     * 
     * @param texCoords a {@link Vector2f} array containing the texture coordinates.
     * @throws IllegalArgumentException if the array length is not equal to 4.
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
     * Returns the normal vector of this mesh.
     * 
     * @return the normal vector of this mesh.
     */
    public Vector3f getNormal() {
        return normal;
    }

    /**
     * Sets the normal vector of this mesh.
     * 
     * @param normal the normal vector of this mesh.
     */
    public void setNormal(Vector3f normal) {
        this.normal = normal;
        updateMesh();
    }

    public void updateMesh() {
        Vector3f a = rectangle.getA();
        Vector3f b = rectangle.getB();
        Vector3f c = rectangle.getC();
        Vector3f d = rectangle.getD();
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

        setBuffer(Type.Index, 3, new short[] { 3, 0, 1, 1, 2, 3 });

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
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        capsule.readSavable("rectangle", rectangle);
        capsule.readSavableArray("texCoords", texCoords);
        capsule.readSavable("normal", normal);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(rectangle, "rectangle", new Rectangle());
        capsule.write(texCoords, "texCoords", new Vector2f[] {
                new Vector2f(0, 0),
                new Vector2f(1, 0),
                new Vector2f(1, 1),
                new Vector2f(0, 1)
        });
        capsule.write(normal, "normal", null);
    }
}
