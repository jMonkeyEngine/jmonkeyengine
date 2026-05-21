/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.IdxBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.VertexBuffer;
import com.jme3.vulkan.mesh.attributes.SingleAttrStruct;

/**
 * A box with solid (filled) faces.
 * 
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Box extends AbstractBox {

    private static final int VERTICES = 24;
    
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
        super(VERTICES, 1);
        updateGeometry(Vector3f.ZERO, x, y, z);
    }

    /**
     * Creates a new box.
     * <p>
     * The box has the given center and extends in the out from the center by
     * the given amount in <em>each</em> direction. So, for example, a box
     * with extent of 0.5 would be the unit cube.
     * 
     * @deprecated Due to constant confusion of geometry centers and the center
     * of the box mesh this method has been deprecated.
     * 
     * @param center the center of the box.
     * @param x the size of the box along the x axis, in both directions.
     * @param y the size of the box along the y axis, in both directions.
     * @param z the size of the box along the z axis, in both directions.
     */
    @Deprecated
    public Box(Vector3f center, float x, float y, float z) {
        super(VERTICES, 1);
        updateGeometry(center, x, y, z);
    }

    /**
     * Constructor instantiates a new <code>Box</code> object.
     * <p>
     * The minimum and maximum point are provided, these two points define the
     * shape and size of the box but not its orientation or position. You should
     * use the {@link com.jme3.scene.Spatial#setLocalTranslation(com.jme3.math.Vector3f) }
     * and {@link com.jme3.scene.Spatial#setLocalRotation(com.jme3.math.Quaternion) }
     * methods to define those properties.
     * 
     * @param min the minimum point that defines the box.
     * @param max the maximum point that defines the box.
     */
    public Box(Vector3f min, Vector3f max) {
        super(24, 1);
        updateGeometry(min, max);
    }

    /**
     * Empty constructor for serialization only. Do not use.
     */
    protected Box(){
        super(24, 1);
    }

    /**
     * Creates a clone of this box.
     * <p>
     * The cloned box will have '_clone' appended to its name, but all other
     * properties will be the same as this box.
     */
    @Override
    public Box clone() {
        return new Box(center.clone(), xExtent, yExtent, zExtent);
    }

    @Override
    protected void doUpdateGeometryIndices() {
        IdxBuffer buf = new IdxBuffer(IndexType.UInt16, JmePlatform.allocateStandardBuffer(
                GEOMETRY_INDICES_DATA.length * Short.BYTES, BufferUsage.Index, UpdateHint.Static));
        try (BufferMapping m = buf.map()) {
            m.getShorts().put(GEOMETRY_INDICES_DATA);
            m.stage();
        }
        setBaseIndexBuffer(buf);
    }

    @Override
    protected void doUpdateGeometryNormals() {
        // todo: fix: does not replace existing normal buffer (ditto for other buffers)
        VertexBuffer buf = new VertexBuffer(InputRate.Vertex, new SingleAttrStruct<>("Normal", new Vector3f()),
                JmePlatform.allocateStandardBuffer(1, BufferUsage.Vertex, UpdateHint.Static));
        try (BufferMapping m = buf.getBuffer().map()) {
            m.getFloats().put(GEOMETRY_NORMALS_DATA);
            m.stage();
        }
        addVertexBuffer(buf);
    }

    @Override
    protected void doUpdateGeometryTextures() {
        VertexBuffer buf = new VertexBuffer(InputRate.Vertex, new SingleAttrStruct<>("TexCoord", new Vector2f()),
                JmePlatform.allocateStandardBuffer(1, BufferUsage.Vertex, UpdateHint.Static));
        try (BufferMapping m = buf.getBuffer().map()) {
            m.getFloats().put(GEOMETRY_TEXTURE_DATA);
            m.stage();
        }
        addVertexBuffer(buf);
    }

    @Override
    protected void doUpdateGeometryVertices() {
        VertexBuffer buf = new VertexBuffer(InputRate.Vertex, new SingleAttrStruct<>("Position", new Vector3f()),
                JmePlatform.allocateStandardBuffer(1, BufferUsage.Vertex, UpdateHint.Static));
        try (BufferMapping m = buf.getBuffer().map()) {
            Vector3f[] v = computeVertices();
            m.getFloats().put(rearrangeToFloats(v, 0, 1, 2, 3, 1, 4, 6, 2, 4, 5, 7, 6, 5, 0, 3, 7, 2, 6, 7, 3, 0, 5, 4, 1));
            m.stage();
        }
        addVertexBuffer(buf);
    }

    private float[] rearrangeToFloats(Vector3f[] vecs, int... indices) {
        float[] floats = new float[indices.length * 3];
        int j = 0;
        for (int i : indices) {
            floats[j++] = vecs[i].x;
            floats[j++] = vecs[i].y;
            floats[j++] = vecs[i].z;
        }
        return floats;
    }

}