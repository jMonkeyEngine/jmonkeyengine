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
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;

/**
 * {@code RectangleMesh} is a rectangle in space similar to
 * {@link com.jme3.scene.shape.Quad}. It uses a
 * {@link com.jme3.math.Rectangle} to position its vertices.
 * 
 * @author Francivan Bezerra
 */
public class RectangleMesh extends Mesh {

    private Rectangle rectangle;

    public RectangleMesh() {
        rectangle = new Rectangle();
    }

    /**
     * Creates a mesh with the given rectangle.
     * 
     * @param rectangle the rectangle used to build this mesh.
     */
    public RectangleMesh(Rectangle rectangle) {
        this.rectangle = rectangle;
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

    public void updateMesh() {
        updateMesh(false);
    }

    public void updateMesh(boolean flipCoords) {
        // the fourth point defining the rectangle (C - B) + A.
        final Vector3f fourthPoint = rectangle.getC().subtract(rectangle.getB()).addLocal(rectangle.getA());

        setBuffer(Type.Position, 3,
                new float[] {
                        rectangle.getA().x, rectangle.getA().y, rectangle.getA().z,
                        rectangle.getB().x, rectangle.getB().y, rectangle.getB().z,
                        rectangle.getC().x, rectangle.getC().y, rectangle.getC().z,
                        fourthPoint.x, fourthPoint.y, fourthPoint.z
                });

        if (flipCoords) {
            setBuffer(Type.TexCoord, 2, new float[] {
                    0, 1,
                    1, 1,
                    1, 0,
                    0, 0
            });
        } else {
            setBuffer(Type.TexCoord, 2, new float[] {
                    0, 0,
                    1, 0,
                    1, 1,
                    0, 1
            });
        }

        setBuffer(Type.Normal, 3, new float[] {
                0, 0,
                1, 0,
                0, 1,
                0, 0,
                1, 0,
                0, 1
        });

        setBuffer(Type.Index, 3, new short[] {
                0, 1, 2,
                0, 2, 3
        });

        updateBound();
        setStatic();
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        capsule.readSavable("rectangle", rectangle);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(rectangle, "rectangle", new Rectangle());
    }
}
