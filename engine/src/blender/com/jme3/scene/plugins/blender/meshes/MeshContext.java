package com.jme3.scene.plugins.blender.meshes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;

/**
 * Class that holds information about the mesh.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshContext {
    /** A map between material index and the geometry. */
    private Map<Integer, Geometry>                    geometries       = new HashMap<Integer, Geometry>();
    /** The vertex reference map. */
    private Map<Integer, Map<Integer, List<Integer>>> vertexReferenceMap;
    /** Bind buffer for vertices is stored here and applied when required. */
    private Map<Integer, VertexBuffer>                bindPoseBuffer   = new HashMap<Integer, VertexBuffer>();
    /** Bind buffer for normals is stored here and applied when required. */
    private Map<Integer, VertexBuffer>                bindNormalBuffer = new HashMap<Integer, VertexBuffer>();

    /**
     * Adds a geometry for the specified material index.
     * @param materialIndex
     *            the material index
     * @param geometry
     *            the geometry
     */
    public void putGeometry(Integer materialIndex, Geometry geometry) {
        geometries.put(materialIndex, geometry);
    }

    /**
     * @param materialIndex
     *            the material index
     * @return vertices amount that is used by mesh with the specified material
     */
    public int getVertexCount(int materialIndex) {
        return geometries.get(materialIndex).getVertexCount();
    }

    /**
     * Returns material index for the geometry.
     * @param geometry
     *            the geometry
     * @return material index
     * @throws IllegalStateException
     *             this exception is thrown when no material is found for the specified geometry
     */
    public int getMaterialIndex(Geometry geometry) {
        for (Entry<Integer, Geometry> entry : geometries.entrySet()) {
            if (entry.getValue().equals(geometry)) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Cannot find material index for the given geometry: " + geometry);
    }

    /**
     * This method returns the vertex reference map.
     * 
     * @return the vertex reference map
     */
    public Map<Integer, List<Integer>> getVertexReferenceMap(int materialIndex) {
        return vertexReferenceMap.get(materialIndex);
    }

    /**
     * This method sets the vertex reference map.
     * 
     * @param vertexReferenceMap
     *            the vertex reference map
     */
    public void setVertexReferenceMap(Map<Integer, Map<Integer, List<Integer>>> vertexReferenceMap) {
        this.vertexReferenceMap = vertexReferenceMap;
    }

    /**
     * This method sets the bind buffer for vertices.
     * 
     * @param materialIndex
     *            the index of the mesh's material
     * @param bindNormalBuffer
     *            the bind buffer for vertices
     */
    public void setBindNormalBuffer(int materialIndex, VertexBuffer bindNormalBuffer) {
        this.bindNormalBuffer.put(materialIndex, bindNormalBuffer);
    }

    /**
     * @param materialIndex
     *            the index of the mesh's material
     * @return the bind buffer for vertices
     */
    public VertexBuffer getBindNormalBuffer(int materialIndex) {
        return bindNormalBuffer.get(materialIndex);
    }

    /**
     * This method sets the bind buffer for normals.
     * 
     * @param materialIndex
     *            the index of the mesh's material
     * @param bindNormalBuffer
     *            the bind buffer for normals
     */
    public void setBindPoseBuffer(int materialIndex, VertexBuffer bindPoseBuffer) {
        this.bindPoseBuffer.put(materialIndex, bindPoseBuffer);
    }

    /**
     * @param materialIndex
     *            the index of the mesh's material
     * @return the bind buffer for normals
     */
    public VertexBuffer getBindPoseBuffer(int materialIndex) {
        return bindPoseBuffer.get(materialIndex);
    }
}
