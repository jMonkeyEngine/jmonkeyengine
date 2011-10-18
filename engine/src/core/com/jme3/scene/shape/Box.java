/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

// $Id: Box.java 4131 2009-03-19 20:15:28Z blaine.dev $
package com.jme3.scene.shape;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 * A box with solid (filled) faces.
 * 
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Box extends AbstractBox {
    
    private static final short[] GEOMETRY_INDICES_DATA = {
         2,  1,  0,  3,  2,  0, // back
         6,  5,  4,  7,  6,  4, // right
        10,  9,  8, 11, 10,  8, // front
        14, 13, 12, 15, 14, 12, // left
        18, 17, 16, 19, 18, 16, // top
        22, 21, 20, 23, 22, 20  // bottom
    };

    private static final float[] GEOMETRY_NORMALS_DATA = {
        0,  0, -1,  0,  0, -1,  0,  0, -1,  0,  0, -1, // back
        1,  0,  0,  1,  0,  0,  1,  0,  0,  1,  0,  0, // right
        0,  0,  1,  0,  0,  1,  0,  0,  1,  0,  0,  1, // front
       -1,  0,  0, -1,  0,  0, -1,  0,  0, -1,  0,  0, // left
        0,  1,  0,  0,  1,  0,  0,  1,  0,  0,  1,  0, // top
        0, -1,  0,  0, -1,  0,  0, -1,  0,  0, -1,  0  // bottom
    };

    private static final float[] GEOMETRY_TEXTURE_DATA = {
        1, 0, 0, 0, 0, 1, 1, 1, // back
        1, 0, 0, 0, 0, 1, 1, 1, // right
        1, 0, 0, 0, 0, 1, 1, 1, // front
        1, 0, 0, 0, 0, 1, 1, 1, // left
        1, 0, 0, 0, 0, 1, 1, 1, // top
        1, 0, 0, 0, 0, 1, 1, 1  // bottom
    };

    /**
     * Creates a new box.
     * <p>
     * The box has a center of 0,0,0 and extends in the out from the center by
     * the given amount in <em>each</em> direction. So, for example, a box
     * with extent of 0.5 would be the unit cube.
     *
     * @param x the size of the box along the x axis, in both directions.
     * @param y the size of the box along the y axis, in both directions.
     * @param z the size of the box along the z axis, in both directions.
     */
    public Box(float x, float y, float z) {
        super();
        updateGeometry(Vector3f.ZERO, x, y, z);
    }

    /**
     * Creates a new box.
     * <p>
     * The box has the given center and extends in the out from the center by
     * the given amount in <em>each</em> direction. So, for example, a box
     * with extent of 0.5 would be the unit cube.
     * 
     * @param center the center of the box.
     * @param x the size of the box along the x axis, in both directions.
     * @param y the size of the box along the y axis, in both directions.
     * @param z the size of the box along the z axis, in both directions.
     */
    public Box(Vector3f center, float x, float y, float z) {
        super();
        updateGeometry(center, x, y, z);
    }

    /**
     * Constructor instantiates a new <code>Box</code> object.
     * <p>
     * The minimum and maximum point are provided, these two points define the
     * shape and size of the box but not it's orientation or position. You should
     * use the {@link #setLocalTranslation()} and {@link #setLocalRotation()}
     * methods to define those properties.
     * 
     * @param min the minimum point that defines the box.
     * @param max the maximum point that defines the box.
     */
    public Box(Vector3f min, Vector3f max) {
        super();
        updateGeometry(min, max);
    }

    /**
     * Empty constructor for serialization only. Do not use.
     */
    public Box(){
        super();
    }

    /**
     * Creates a clone of this box.
     * <p>
     * The cloned box will have '_clone' appended to it's name, but all other
     * properties will be the same as this box.
     */
    @Override
    public Box clone() {
        return new Box(center.clone(), xExtent, yExtent, zExtent);
    }

    protected void duUpdateGeometryIndices() {
        if (getBuffer(Type.Index) == null){
            setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(GEOMETRY_INDICES_DATA));
        }
    }

    protected void duUpdateGeometryNormals() {
        if (getBuffer(Type.Normal) == null){
            setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(GEOMETRY_NORMALS_DATA));
        }
    }

    protected void duUpdateGeometryTextures() {
        if (getBuffer(Type.TexCoord) == null){
            setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(GEOMETRY_TEXTURE_DATA));
        }
    }

    protected void duUpdateGeometryVertices() {
        FloatBuffer fpb = BufferUtils.createVector3Buffer(24);
        Vector3f[] v = computeVertices();
        fpb.put(new float[] {
                v[0].x, v[0].y, v[0].z, v[1].x, v[1].y, v[1].z, v[2].x, v[2].y, v[2].z, v[3].x, v[3].y, v[3].z, // back
                v[1].x, v[1].y, v[1].z, v[4].x, v[4].y, v[4].z, v[6].x, v[6].y, v[6].z, v[2].x, v[2].y, v[2].z, // right
                v[4].x, v[4].y, v[4].z, v[5].x, v[5].y, v[5].z, v[7].x, v[7].y, v[7].z, v[6].x, v[6].y, v[6].z, // front
                v[5].x, v[5].y, v[5].z, v[0].x, v[0].y, v[0].z, v[3].x, v[3].y, v[3].z, v[7].x, v[7].y, v[7].z, // left
                v[2].x, v[2].y, v[2].z, v[6].x, v[6].y, v[6].z, v[7].x, v[7].y, v[7].z, v[3].x, v[3].y, v[3].z, // top
                v[0].x, v[0].y, v[0].z, v[5].x, v[5].y, v[5].z, v[4].x, v[4].y, v[4].z, v[1].x, v[1].y, v[1].z  // bottom
        });
        setBuffer(Type.Position, 3, fpb);
        updateBound();
    }

}