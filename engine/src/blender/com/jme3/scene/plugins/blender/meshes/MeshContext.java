package com.jme3.scene.plugins.blender.meshes;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that holds information about the mesh.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshContext {
	/** The mesh stored here as a list of geometries. */
	private List<Geometry> mesh;
	/** Vertex list that is referenced by all the geometries. */
	private List<Vector3f> vertexList;
	/** The vertex reference map. */
	private Map<Integer, List<Integer>> vertexReferenceMap;
	/** The UV-coordinates for each of the geometries. */
	private Map<Geometry, VertexBuffer> uvCoordinates = new HashMap<Geometry, VertexBuffer>();

	/**
	 * This method returns the referenced mesh.
	 * 
	 * @return the referenced mesh
	 */
	public List<Geometry> getMesh() {
		return mesh;
	}

	/**
	 * This method sets the referenced mesh.
	 * 
	 * @param mesh
	 *            the referenced mesh
	 */
	public void setMesh(List<Geometry> mesh) {
		this.mesh = mesh;
	}

	/**
	 * This method returns the vertex list.
	 * 
	 * @return the vertex list
	 */
	public List<Vector3f> getVertexList() {
		return vertexList;
	}

	/**
	 * This method sets the vertex list.
	 * 
	 * @param vertexList
	 *            the vertex list
	 */
	public void setVertexList(List<Vector3f> vertexList) {
		this.vertexList = vertexList;
	}

	/**
	 * This method returns the vertex reference map.
	 * 
	 * @return the vertex reference map
	 */
	public Map<Integer, List<Integer>> getVertexReferenceMap() {
		return vertexReferenceMap;
	}

	/**
	 * This method sets the vertex reference map.
	 * 
	 * @param vertexReferenceMap
	 *            the vertex reference map
	 */
	public void setVertexReferenceMap(
			Map<Integer, List<Integer>> vertexReferenceMap) {
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
}
