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
package com.jme3.scene.debug;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.shadow.ShadowUtil;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 * A specialized Mesh that renders a camera frustum as a wireframe.
 * This class extends jME3's Mesh and is designed to visually represent
 * the viewing volume of a camera, which can be useful for debugging
 * or visualization purposes.
 * <p>
 * The frustum is defined by eight points: four for the near plane
 * and four for the far plane. These points are connected by lines
 * to form a wireframe cube-like structure.
 */
public class WireFrustum extends Mesh {

    /**
     * For Serialization only. Do not use.
     */
    protected WireFrustum() {
    }

    /**
     * Constructs a new `WireFrustum` mesh using the specified frustum corner points.
     * The points should represent the 8 corners of the frustum.
     * The expected order of points is typically:
     * 0-3: Near plane (e.g., bottom-left, bottom-right, top-right, top-left)
     * 4-7: Far plane (e.g., bottom-left, bottom-right, top-right, top-left)
     *
     * @param points An array of 8 `Vector3f` objects representing the frustum's corners.
     * If the array is null or does not contain 8 points, an
     * `IllegalArgumentException` will be thrown.
     */
    public WireFrustum(Vector3f[] points) {
        if (points == null || points.length != 8) {
            throw new IllegalArgumentException("Frustum points array must not be null and must contain 8 points.");
        }
        setGeometryData(points);
    }

    /**
     * Initializes the mesh's geometric data, setting up the vertex positions and indices.
     * This method is called during the construction of the `WireFrustum`.
     *
     * @param points The 8 `Vector3f` points defining the frustum's corners.
     */
    private void setGeometryData(Vector3f[] points) {
        // Set vertex positions
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(points));

        // Set indices to draw lines connecting the frustum corners
        // The indices define 12 lines: 4 for near plane, 4 for far plane, and 4 connecting near to far.
        setBuffer(Type.Index, 2,
                new short[]{
                        // Near plane
                        0, 1,
                        1, 2,
                        2, 3,
                        3, 0,

                        // Far plane
                        4, 5,
                        5, 6,
                        6, 7,
                        7, 4,

                        // Connecting lines (near to far)
                        0, 4,
                        1, 5,
                        2, 6,
                        3, 7,
                }
        );
        getBuffer(Type.Index).setUsage(Usage.Static);
        setMode(Mode.Lines);
        updateBound();
    }

    /**
     * Updates the vertex positions of the existing `WireFrustum` mesh.
     * This is more efficient than creating a new `WireFrustum` instance
     * if only the frustum's position or orientation changes.
     *
     * @param points An array of 8 `Vector3f` objects representing the new frustum's corners.
     * If the array is null or does not contain 8 points, an
     * `IllegalArgumentException` will be thrown.
     */
    public void update(Vector3f[] points) {
        if (points == null || points.length != 8) {
            throw new IllegalArgumentException("Frustum points array must not be null and must contain 8 points.");
        }

        VertexBuffer vb = getBuffer(Type.Position);
        if (vb == null) {
            // If for some reason the position buffer is missing, re-create it.
            // This case should ideally not happen if the object is constructed properly.
            setGeometryData(points);
            return;
        }

        // Create a new FloatBuffer from the updated points
        FloatBuffer newBuff = BufferUtils.createFloatBuffer(points);
        // Get the existing FloatBuffer from the VertexBuffer
        FloatBuffer currBuff = (FloatBuffer) vb.getData();

        currBuff.clear();       // Clear
        currBuff.put(newBuff);  // Copy
        currBuff.rewind();      // Rewind

        // Update the VertexBuffer with the modified FloatBuffer data
        vb.updateData(currBuff);

        // Update the mesh's bounding volume to reflect the new vertex positions
        updateBound();
    }

    /**
     * A static factory method to create a new `WireFrustum` mesh.
     * This method provides a cleaner way to instantiate a `WireFrustum`.
     *
     * @param points An array of 8 `Vector3f` objects representing the frustum's corners.
     * @return A new `WireFrustum` instance.
     */
    public static Mesh makeFrustum(Vector3f[] points) {
        return new WireFrustum(points);
    }

    /**
     * Creates a `Geometry` object representing the wireframe frustum of a given camera.
     * The frustum points are calculated based on the camera's current view settings.
     * The returned `Geometry` can be directly attached to a scene graph.
     *
     * @param camera The `Camera` whose frustum is to be visualized.
     * @return A `Geometry` object containing the `WireFrustum` mesh.
     */
    public static Geometry makeGeometry(Camera camera) {
        Vector3f[] frustumCorners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            frustumCorners[i] = new Vector3f();
        }

        Camera tempCam = camera.clone();
        tempCam.setLocation(new Vector3f(0, 0, 0));
        tempCam.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        ShadowUtil.updateFrustumPoints2(tempCam, frustumCorners);

        WireFrustum mesh = new WireFrustum(frustumCorners);
        return new Geometry("Viewing Frustum", mesh);
    }

}
