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
	private Map<Integer, Geometry> geometries = new HashMap<Integer, Geometry>();
	/** The vertex reference map. */
	private Map<Integer, Map<Integer, List<Integer>>>	vertexReferenceMap;
	/** The UV-coordinates for each of the geometries. */
	private Map<Geometry, VertexBuffer>	uvCoordinates	= new HashMap<Geometry, VertexBuffer>();
	/** Bind buffer for vertices is stored here and applied when required. */
	private VertexBuffer				bindPoseBuffer;
	/** Bind buffer for normals is stored here and applied when required. */
	private VertexBuffer				bindNormalBuffer;

	/**
	 * Adds a geometry for the specified material index.
	 * @param materialIndex the material index
	 * @param geometry the geometry
	 */
	public void putGeometry(Integer materialIndex, Geometry geometry) {
		geometries.put(materialIndex, geometry);
	}
	
	/**
	 * @param materialIndex the material index
	 * @return vertices amount that is used by mesh with the specified material
	 */
	public int getVertexCount(int materialIndex) {
		return geometries.get(materialIndex).getVertexCount();
	}
	
	/**
	 * Returns material index for the geometry.
	 * @param geometry the geometry
	 * @return material index
	 * @throws IllegalStateException this exception is thrown when no material is found for the specified geometry
	 */
	public int getMaterialIndex(Geometry geometry) {
		for(Entry<Integer, Geometry> entry : geometries.entrySet()) {
			if(entry.getValue().equals(geometry)) {
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
	 * This method adds the mesh's UV-coordinates.
	 * 
	 * @param geometry
	 *            the mesh that has the UV-coordinates
	 * @param vertexBuffer
	 *            the mesh's UV-coordinates
	 */
	public void addUVCoordinates(Geometry geometry, VertexBuffer vertexBuffer) {
		uvCoordinates.put(geometry, vertexBuffer);
	}

	/**
	 * This method returns the mesh's UV-coordinates.
	 * 
	 * @param geometry
	 *            the mesh
	 * @return the mesh's UV-coordinates
	 */
	public VertexBuffer getUVCoordinates(Geometry geometry) {
		return uvCoordinates.get(geometry);
	}

	/**
	 * This method sets the bind buffer for vertices.
	 * 
	 * @param bindNormalBuffer
	 *            the bind buffer for vertices
	 */
	public void setBindNormalBuffer(VertexBuffer bindNormalBuffer) {
		this.bindNormalBuffer = bindNormalBuffer;
	}

	/**
	 * @return the bind buffer for vertices
	 */
	public VertexBuffer getBindNormalBuffer() {
		return bindNormalBuffer;
	}

	/**
	 * This method sets the bind buffer for normals.
	 * 
	 * @param bindNormalBuffer
	 *            the bind buffer for normals
	 */
	public void setBindPoseBuffer(VertexBuffer bindPoseBuffer) {
		this.bindPoseBuffer = bindPoseBuffer;
	}

	/**
	 * @return the bind buffer for normals
	 */
	public VertexBuffer getBindPoseBuffer() {
		return bindPoseBuffer;
	}
}
