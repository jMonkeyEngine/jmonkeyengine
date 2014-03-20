package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.scene.Geometry;

/**
 * Class that holds information about the mesh.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshContext {
    /** A map between material index and the geometry. */
    private Map<Integer, List<Geometry>>              geometries = new HashMap<Integer, List<Geometry>>();
    /** The vertex reference map. */
    private Map<Integer, Map<Integer, List<Integer>>> vertexReferenceMap;

    /**
     * Adds a geometry for the specified material index.
     * @param materialIndex
     *            the material index
     * @param geometry
     *            the geometry
     */
    public void putGeometry(Integer materialIndex, Geometry geometry) {
        List<Geometry> geomList = geometries.get(materialIndex);
        if (geomList == null) {
            geomList = new ArrayList<Geometry>();
            geometries.put(materialIndex, geomList);
        }
        geomList.add(geometry);
    }

    /**
     * @param materialIndex
     *            the material index
     * @return vertices amount that is used by mesh with the specified material
     */
    public int getVertexCount(int materialIndex) {
        int result = 0;
        for (Geometry geometry : geometries.get(materialIndex)) {
            result += geometry.getVertexCount();
        }
        return result;
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
        for (Entry<Integer, List<Geometry>> entry : geometries.entrySet()) {
            for (Geometry g : entry.getValue()) {
                if (g.equals(geometry)) {
                    return entry.getKey();
                }
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
}
