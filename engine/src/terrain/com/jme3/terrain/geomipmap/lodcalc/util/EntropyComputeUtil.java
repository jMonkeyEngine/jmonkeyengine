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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Computes the entropy value δ (delta) for a given terrain block and
 * LOD level.
 * See the geomipmapping paper section
 * "2.3.1 Choosing the appropriate GeoMipMap level"
 *
 * @author Kirill Vainer
 */
public class EntropyComputeUtil {

    public static float computeLodEntropy(Mesh terrainBlock, IntBuffer lodIndices){
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
        terrainBlock.setBuffer(Type.Index, 3, lodIndices);

        // Recalculate collision mesh
        terrainBlock.createCollisionData();

        float entropy = 0;
        for (int i = 0; i < positions.capacity() / 3; i++){
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
