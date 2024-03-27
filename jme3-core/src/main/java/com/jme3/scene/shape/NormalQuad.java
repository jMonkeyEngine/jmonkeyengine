/*
 * Copyright (c) 2024 jMonkeyEngine
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

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * Represents a rectangular finite plane in space defined by a normal vector.
 * 
 * @author codex
 */
public class NormalQuad extends Mesh {
    
    private static final float[] uvs = {1, 1, 1, 0, 0, 0, 0, 1};
    private static final short[] index = {0, 2, 1, 0, 3, 2};
    
    private final Vector3f normal;
    private final Vector3f up;
    private float w;
    private float h;
    private float px;
    private float py;
    private boolean updateFlag = true;
    
    /**
     * Creates a NormalQuad from the arguments
     * 
     * @param normal face normals
     * @param up up direction
     * @param w mesh width
     * @param h mesh height
     * @param px percentage offset x
     * @param py percentage offset y
     * @see #setNormal(com.jme3.math.Vector3f) 
     * @see #setUp(com.jme3.math.Vector3f) 
     * @see #setWidth(float) 
     * @see #setHeight(float) 
     * @see #setPercentOffsetX(float) 
     * @see #setPercentOffsetY(float) 
     */
    public NormalQuad(Vector3f normal, Vector3f up, float w, float h, float px, float py) {
        this.normal = new Vector3f(normal);
        this.up = new Vector3f(up);
        this.w = w;
        this.h = h;
        this.px = px;
        this.py = py;
        updateMesh();
    }
    
    /**
     * Updates the mesh.
     * <p>
     * Only occurs if mesh parameters have been changed since last update.
     */
    public final void updateMesh() {
        if (!updateFlag) return;
        Vector3f x = getLocalX(null);
        Vector3f y = getLocalY(x, null);
        Vector3f temp = new Vector3f();
        Vector3f[] verts = {
            x.mult(-w *       px ).addLocal(y.mult(-h *       py , temp)),
            x.mult(-w *       px ).addLocal(y.mult( h * (1f - py), temp)),
            x.mult( w * (1f - px)).addLocal(y.mult( h * (1f - py), temp)),
            x.mult( w * (1f - px)).addLocal(y.mult(-h *       py , temp)),
        };
        Vector3f[] normals = {normal, normal, normal, normal};
        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createShortBuffer(index));
        updateBound();
        setStatic();
        updateFlag = false;
    }
    
    /**
     * Sets the normal direction.
     * <p>
     * The input vector should be normalized.
     * 
     * @param normal 
     */
    public void setNormal(Vector3f normal) {
        this.normal.set(normal);
        updateFlag = true;
    }
    
    /**
     * Sets the up direction.
     * <p>
     * The input vector should be normalized and not equal to the normal vector.
     * 
     * @param up 
     */
    public void setUp(Vector3f up) {
        this.up.set(up);
        updateFlag = true;
    }
    
    /**
     * Sets the width of the mesh along the local X axis.
     * <p>
     * The local X axis is defined by {@code normal.cross(up)}.
     * 
     * @param w 
     */
    public void setWidth(float w) {
        this.w = w;
        updateFlag = true;
    }
    
    /**
     * Sets the height of the mesh along the local Y axis.
     * <p>
     * The local Y axis is defined by {@code normal.cross(up).cross(normal)}.
     * 
     * @param h 
     */
    public void setHeight(float h) {
        this.h = h;
        updateFlag = true;
    }
    
    /**
     * Sets the percent offset according to the width of the mesh on the local X axis.
     * <p>
     * 0 places the origin on the lower side of the mesh.<br>
     * 1 places the origin on the upper side of the mesh.<br>
     * 0.5 places the origin in the middle of the mesh.
     * 
     * @param px 
     */
    public void setPercentOffsetX(float px) {
        this.px = px;
        updateFlag = true;
    }
    
    /**
     * Sets the percent offset according to the height of the mesh on the local Y axis.
     * <p>
     * 0 places the origin on the lower side of the mesh.<br>
     * 1 places the origin on the upper side of the mesh.<br>
     * 0.5 places the origin in the middle of the mesh.
     * 
     * @param py 
     */
    public void setPercentOffsetY(float py) {
        this.py = py;
        updateFlag = true;
    }

    /**
     * Gets the normal direction.
     * 
     * @return 
     * @see #setNormal(com.jme3.math.Vector3f) 
     */
    public Vector3f getNormal() {
        return normal;
    }
    
    /**
     * Gets the up direction.
     * 
     * @return 
     * @see #setUp(com.jme3.math.Vector3f) 
     */
    public Vector3f getUp() {
        return up;
    }
    
    /**
     * Gets the width of the mesh along the local X axis.
     * 
     * @return 
     * @see #setWidth(float) 
     */
    public float getWidth() {
        return w;
    }
    
    /**
     * Gets the height of the mesh along the local Y axis.
     * 
     * @return 
     * @see #setHeight(float) 
     */
    public float getHeight() {
        return h;
    }
    
    /**
     * Gets the percentage offset according to the width along the local X axis.
     * 
     * @return 
     * @see #setPercentOffsetX(float) 
     */
    public float getPercentOffsetX() {
        return px;
    }
    
    /**
     * Gets the percentage offset according to the height along the local Y axis.
     * 
     * @return 
     * @see #setPercentOffsetY(float) 
     */
    public float getPercentOffsetY() {
        return py;
    }
    
    /**
     * Gets the local X axis.
     * <p>
     * The axis is calculated by {@code normal.cross(up)}.
     * 
     * @param store stores result, or null to create new instance
     * @return local X axis
     */
    public Vector3f getLocalX(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return normal.cross(up, store);
    }
    
    /**
     * Gets the local Y axis.
     * <p>
     * The axis is calculed by {@code normal.cross(up).cross(normal)}.
     * 
     * @param store stores result, or null to create new instance
     * @return local Y axis
     */
    public Vector3f getLocalY(Vector3f store) {
        return getLocalY(getLocalX(store), store);
    }
    
    /**
     * Gets the local Y axis relative to the local X axis.
     * <p>
     * The axis is calculed by {@code localX.cross(normal)}.
     * 
     * @param localX local X axis
     * @param store stores result, or null to create new instance
     * @return local Y axis
     */
    public Vector3f getLocalY(Vector3f localX, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return localX.cross(normal, store);
    }
    
}
