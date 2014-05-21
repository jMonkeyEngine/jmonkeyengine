package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.Geometry;

/**
 * Class that holds information about the mesh.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshContext {
    private static final Logger                       LOGGER       = Logger.getLogger(MeshContext.class.getName());

    /** A map between material index and the geometry. */
    private Map<Integer, List<Geometry>>              geometries   = new HashMap<Integer, List<Geometry>>();
    /** The vertex reference map. */
    private Map<Integer, Map<Integer, List<Integer>>> vertexReferenceMap;
    /**
     * A vertex group map. The key is the vertex group name and the value is the set of vertex groups.
     * Linked hash map is used because the insertion order is important.
     */
    private LinkedHashMap<String, VertexGroup>        vertexGroups = new LinkedHashMap<String, VertexGroup>();

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

    /**
     * Adds a new empty vertex group to the mesh context.
     * @param name
     *            the name of the vertex group
     */
    public void addVertexGroup(String name) {
        if (!vertexGroups.containsKey(name)) {
            vertexGroups.put(name, new VertexGroup());
        } else {
            LOGGER.log(Level.WARNING, "Vertex group already added: {0}", name);
        }
    }

    /**
     * Adds a vertex to the vertex group with specified index (the index is the order of adding a group).
     * @param vertexIndex
     *            the vertex index
     * @param weight
     *            the vertex weight
     * @param vertexGroupIndex
     *            the index of a vertex group
     */
    public void addVertexToGroup(int vertexIndex, float weight, int vertexGroupIndex) {
        if (vertexGroupIndex < 0 || vertexGroupIndex >= vertexGroups.size()) {
            throw new IllegalArgumentException("Invalid group index: " + vertexGroupIndex);
        }
        int counter = 0;
        for (Entry<String, VertexGroup> vg : vertexGroups.entrySet()) {
            if (vertexGroupIndex == counter) {
                vg.getValue().addVertex(vertexIndex, weight);
                return;
            }
            ++counter;
        }
    }

    /**
     * Returns a group with given name of null if such group does not exist.
     * @param groupName
     *            the name of a vertex group
     * @return vertex group with the given name or null
     */
    public VertexGroup getGroup(String groupName) {
        return vertexGroups.get(groupName);
    }

    /**
     * A vertex group class that maps vertex index to its weight in a single group.
     * The group will need to be set a bone index in order to prepare proper buffers for the jme mesh.
     * But that information is available after the skeleton is loaded.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static class VertexGroup extends HashMap<Integer, Float> {
        private static final long serialVersionUID = 5601646768279643957L;

        /** The index of the bone f the vertex group is to be used for attaching vertices to bones. */
        private int               boneIndex;

        /**
         * Adds a mapping between vertex index and its weight.
         * @param index
         *            the index of the vertex (in JME mesh)
         * @param weight
         *            the weight of the vertex
         */
        public void addVertex(int index, float weight) {
            this.put(index, weight);
        }

        /**
         * The method sets the bone index for the current vertex group.
         * @param boneIndex
         *            the index of the bone
         */
        public void setBoneIndex(int boneIndex) {
            this.boneIndex = boneIndex;
        }

        /**
         * @return the index of the bone
         */
        public int getBoneIndex() {
            return boneIndex;
        }
    }
}
