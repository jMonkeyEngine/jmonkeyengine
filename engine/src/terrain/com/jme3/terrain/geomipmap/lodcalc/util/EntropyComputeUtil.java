/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.terrain.geomipmap.lodcalc.util;

import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Matrix4f;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Computes the entropy value δ (delta) for a given terrain block and
 * LOD level.
 * See the geomipmapping paper section
 * "2.3.1 Choosing the appropriate GeoMipMap level"
 *
 * @author Kirill Vainer
 */
public class EntropyComputeUtil {

    public static float computeLodEntropy(Mesh terrainBlock, Buffer lodIndices){
        // Bounding box for the terrain block
        BoundingBox bbox = (BoundingBox) terrainBlock.getBound();

        // Vertex positions for the block
        FloatBuffer positions = terrainBlock.getFloatBuffer(Type.Position);

        // Prepare to cast rays
        Vector3f pos = new Vector3f();
        Vector3f dir = new Vector3f(0, -1, 0);
        Ray ray = new Ray(pos, dir);

        // Prepare collision results
        CollisionResults results = new CollisionResults();

        // Set the LOD indices on the block
        VertexBuffer originalIndices = terrainBlock.getBuffer(Type.Index);

        terrainBlock.clearBuffer(Type.Index);
        if (lodIndices instanceof IntBuffer)
            terrainBlock.setBuffer(Type.Index, 3, (IntBuffer)lodIndices);
        else if (lodIndices instanceof ShortBuffer) {
            terrainBlock.setBuffer(Type.Index, 3, (ShortBuffer) lodIndices);
        }

        // Recalculate collision mesh
        terrainBlock.createCollisionData();

        float entropy = 0;
        for (int i = 0; i < positions.limit() / 3; i++){
            BufferUtils.populateFromBuffer(pos, positions, i);

            float realHeight = pos.y;

            pos.addLocal(0, bbox.getYExtent(), 0);
            ray.setOrigin(pos);

            results.clear();
            terrainBlock.collideWith(ray, Matrix4f.IDENTITY, bbox, results);

            if (results.size() > 0){
                Vector3f contactPoint = results.getClosestCollision().getContactPoint();
                float delta = Math.abs(realHeight - contactPoint.y);
                entropy = Math.max(delta, entropy);
            }
        }

        // Restore original indices
        terrainBlock.clearBuffer(Type.Index);
        terrainBlock.setBuffer(originalIndices);

        return entropy;
    }

}
