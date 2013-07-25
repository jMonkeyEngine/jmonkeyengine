package com.jme3.scene.plugins.blender.textures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.Vector2f;

/**
 * A collection of UV coordinates. The coords are stored in groups defined by the material index and their UV set name.
 * 
 * @author Kaelthas (Marcin Roguski)
 */
public class UserUVCollection {
    /** A map between material number and UV coordinates of mesh that has this material applied. */
    private Map<Integer, LinkedHashMap<String, List<Vector2f>>> uvCoordinates = new HashMap<Integer, LinkedHashMap<String, List<Vector2f>>>();
    /** A map between vertex index and its UV coordinates. */
    private Map<String, Map<Integer, Vector2f>>                 uvsMap        = new HashMap<String, Map<Integer, Vector2f>>();

    /**
     * Adds a single UV coordinates for a specified vertex index.
     * @param materialIndex
     *            the material index
     * @param uvSetName
     *            the UV set name
     * @param uv
     *            the added UV coordinates
     * @param jmeVertexIndex
     *            the index of the vertex in result jme mesh
     */
    public void addUV(int materialIndex, String uvSetName, Vector2f uv, int jmeVertexIndex) {
        // first get all UV sets for the specified material ...
        LinkedHashMap<String, List<Vector2f>> uvsForMaterial = uvCoordinates.get(materialIndex);
        if (uvsForMaterial == null) {
            uvsForMaterial = new LinkedHashMap<String, List<Vector2f>>();
            uvCoordinates.put(materialIndex, uvsForMaterial);
        }

        // ... then fetch the UVS for the specified UV set name ...
        List<Vector2f> uvsForName = uvsForMaterial.get(uvSetName);
        if (uvsForName == null) {
            uvsForName = new ArrayList<Vector2f>();
            uvsForMaterial.put(uvSetName, uvsForName);
        }

        // ... add the UV coordinates to the proper list ...
        uvsForName.add(uv);

        // ... and add the mapping of the UV coordinates to a vertex index for the specified UV set
        Map<Integer, Vector2f> uvToVertexIndexMapping = uvsMap.get(uvSetName);
        if (uvToVertexIndexMapping == null) {
            uvToVertexIndexMapping = new HashMap<Integer, Vector2f>();
            uvsMap.put(uvSetName, uvToVertexIndexMapping);
        }
        uvToVertexIndexMapping.put(jmeVertexIndex, uv);
    }

    /**
     * @param uvSetName
     *            the name of the UV set
     * @param vertexIndex
     *            the vertex index corresponds to the index in jme mesh and not the original one in blender
     * @return
     */
    public Vector2f getUVForVertex(String uvSetName, int vertexIndex) {
        return uvsMap.get(uvSetName).get(vertexIndex);
    }

    /**
     * @param materialNumber
     *            the material number that is appied to the mesh
     * @return UV coordinates of vertices that belong to the required mesh part
     */
    public LinkedHashMap<String, List<Vector2f>> getUVCoordinates(int materialNumber) {
        return uvCoordinates.get(materialNumber);
    }

    /**
     * @return indicates if the mesh has UV coordinates
     */
    public boolean hasUVCoordinates() {
        return uvCoordinates.size() > 0;
    }
}
