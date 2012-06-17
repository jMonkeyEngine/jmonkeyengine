package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/*package*/ class MeshBuilder {
	/** An array of reference vertices. */
	private Vector3f[] vertices;
	/** A variable that indicates if the model uses generated textures. */
	private boolean usesGeneratedTextures;
	
	/** This map's key is the vertex index from 'vertices 'table and the value are indices from 'vertexList'
    positions (it simply tells which vertex is referenced where in the result list). */
    private Map<Integer, Map<Integer, List<Integer>>> globalVertexReferenceMap;
    /** A map between vertex and its normal vector. */
    private Map<Vector3f, Vector3f> globalNormalMap = new HashMap<Vector3f, Vector3f>();
    
    /** A map between vertex index and its UV coordinates. */
    private Map<Integer, Vector2f> uvsMap = new HashMap<Integer, Vector2f>();
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>> normalMap = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>> vertexMap = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts indexes by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Integer>> indexMap = new HashMap<Integer, List<Integer>>();
    /** A map between material number and UV coordinates of mesh that has this material applied. */
    private Map<Integer, List<Vector2f>> uvCoordinates = new HashMap<Integer, List<Vector2f>>();//<material_number; list of uv coordinates for mesh's vertices>
    
    /**
     * Constructor. Stores the given array (not copying it).
     * The second argument describes if the model uses generated textures. If yes then no vertex amount optimisation is applied.
     * The amount of vertices is always faceCount * 3.
     * @param vertices the reference vertices array
     * @param usesGeneratedTextures a variable that indicates if the model uses generated textures or not
     */
	public MeshBuilder(Vector3f[] vertices, boolean usesGeneratedTextures) {
		if(vertices == null || vertices.length == 0) {
			throw new IllegalArgumentException("No vertices loaded to build mesh.");
		}
		this.vertices = vertices;
		this.usesGeneratedTextures = usesGeneratedTextures;
		globalVertexReferenceMap = new HashMap<Integer, Map<Integer, List<Integer>>>(vertices.length);
	}
	
	/**
	 * This method adds a face to the mesh.
	 * @param v1 index of the 1'st vertex from the reference vertex table
	 * @param v2 index of the 2'nd vertex from the reference vertex table
	 * @param v3 index of the 3'rd vertex from the reference vertex table
	 * @param smooth indicates if this face should have smooth shading or flat shading
	 * @param materialNumber the material number for this face
	 * @param uvs a 3-element array of vertices UV coordinates
	 */
	public void appendFace(int v1, int v2, int v3, boolean smooth, int materialNumber, Vector2f[] uvs) {
		if(uvs != null && uvs.length != 3) {
			throw new IllegalArgumentException("UV coordinates must be a 3-element array!");
		}
		List<Integer> indexList = indexMap.get(materialNumber);
        if (indexList == null) {
            indexList = new ArrayList<Integer>();
            indexMap.put(materialNumber, indexList);
        }
        List<Vector3f> vertexList = vertexMap.get(materialNumber);
        if (vertexList == null) {
            vertexList = new ArrayList<Vector3f>();
            vertexMap.put(materialNumber, vertexList);
        }
        List<Vector3f> normalList = normalMap.get(materialNumber);
        if (normalList == null) {
        	normalList = new ArrayList<Vector3f>();
        	normalMap.put(materialNumber, normalList);
        }
        Map<Integer, List<Integer>> vertexReferenceMap = globalVertexReferenceMap.get(materialNumber);
        if(vertexReferenceMap == null) {
        	vertexReferenceMap = new HashMap<Integer, List<Integer>>();
        	globalVertexReferenceMap.put(materialNumber, vertexReferenceMap);
        }
        
        List<Vector2f> uvCoordinatesList = null;
        if(uvs != null) {
	        uvCoordinatesList = uvCoordinates.get(Integer.valueOf(materialNumber));
	        if(uvCoordinatesList == null) {
	        	uvCoordinatesList = new ArrayList<Vector2f>();
	        	uvCoordinates.put(Integer.valueOf(materialNumber), uvCoordinatesList);
	        }
        }
        
        Integer[] index = new Integer[] {v1, v2, v3};
		Vector3f n = FastMath.computeNormal(vertices[v1], vertices[v2], vertices[v3]);
        this.addNormal(n, globalNormalMap, smooth, vertices[v1], vertices[v2], vertices[v3]);
        if(smooth && !usesGeneratedTextures) {
			for (int i = 0; i < 3; ++i) {
        		if(!vertexReferenceMap.containsKey(index[i])) {
            		this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
            		vertexList.add(vertices[index[i]]);
            		normalList.add(globalNormalMap.get(vertices[index[i]]));
            		if(uvCoordinatesList != null) {
            			uvsMap.put(vertexList.size(), uvs[i]);
            			uvCoordinatesList.add(uvs[i]);
            		}
            		index[i] = vertexList.size() - 1;
            	} else if(uvCoordinatesList != null) {
            		boolean vertexAlreadyUsed = false;
            		for(Integer vertexIndex : vertexReferenceMap.get(index[i])) {
            			if(uvs[i].equals(uvsMap.get(vertexIndex))) {
            				vertexAlreadyUsed = true;
            				index[i] = vertexIndex;
            				break;
            			}
            		}
            		if(!vertexAlreadyUsed) {
            			this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
            			uvsMap.put(vertexList.size(), uvs[i]);
            			vertexList.add(vertices[index[i]]);
                		normalList.add(globalNormalMap.get(vertices[index[i]]));
            			uvCoordinatesList.add(uvs[i]);
            			index[i] = vertexList.size() - 1;
            		}
            	} else {
            		index[i] = vertexList.indexOf(vertices[index[i]]);
            	}
        		indexList.add(index[i]);
        	}
        } else {
        	for (int i = 0; i < 3; ++i) {
        		indexList.add(vertexList.size());
        		this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
        		if(uvCoordinatesList != null) {
        			uvCoordinatesList.add(uvs[i]);
        			uvsMap.put(vertexList.size(), uvs[i]);
        		}
        		vertexList.add(vertices[index[i]]);
        		normalList.add(globalNormalMap.get(vertices[index[i]]));
        	}
        }
	}
	
	/**
	 * @return a map that maps vertex index from reference array to its indices in the result list
	 */
	public Map<Integer, Map<Integer, List<Integer>>> getVertexReferenceMap() {
		return globalVertexReferenceMap;
	}
	
	/**
	 * @return result vertices array
	 */
	public Vector3f[] getVertices(int materialNumber) {
		return vertexMap.get(materialNumber).toArray(new Vector3f[vertexMap.get(materialNumber).size()]);
	}
	
	/**
	 * @return the amount of result vertices
	 */
	public int getVerticesAmount(int materialNumber) {
		return vertexMap.get(materialNumber).size();
	}
	
	/**
	 * @return normals result array
	 */
	public Vector3f[] getNormals(int materialNumber) {
		return normalMap.get(materialNumber).toArray(new Vector3f[normalMap.get(materialNumber).size()]);
	}
	
	/**
	 * @return a map between material number and the mesh part vertices indices
	 */
	public Map<Integer, List<Integer>> getMeshesMap() {
		return indexMap;
	}
	
	/**
	 * @return the amount of meshes the source mesh was split into (depends on the applied materials count)
	 */
	public int getMeshesPartAmount() {
		return indexMap.size();
	}
	
	/**
	 * @param materialNumber the material number that is appied to the mesh
	 * @return UV coordinates of vertices that belong to the required mesh part
	 */
	public List<Vector2f> getUVCoordinates(int materialNumber) {
		return uvCoordinates.get(materialNumber);
	}
	
	/**
	 * @return indicates if the mesh has UV coordinates
	 */
	public boolean hasUVCoordinates() {
		return uvCoordinates.size() > 0;
	}
	
	/**
     * This method adds a normal to a normals' map. This map is used to merge normals of a vertor that should be rendered smooth.
     * 
     * @param normalToAdd
     *            a normal to be added
     * @param normalMap
     *            merges normals of faces that will be rendered smooth; the key is the vertex and the value - its normal vector
     * @param smooth
     *            the variable that indicates wheather to merge normals (creating the smooth mesh) or not
     * @param vertices
     *            a list of vertices read from the blender file
     */
    private void addNormal(Vector3f normalToAdd, Map<Vector3f, Vector3f> normalMap, boolean smooth, Vector3f... vertices) {
        for (Vector3f v : vertices) {
            Vector3f n = normalMap.get(v);
            if (!smooth || n == null) {
                normalMap.put(v, normalToAdd.clone());
            } else {
                n.addLocal(normalToAdd).normalizeLocal();
            }
        }
    }
    
    /**
     * This method fills the vertex reference map. The vertices are loaded once and referenced many times in the model. This map is created
     * to tell where the basic vertices are referenced in the result vertex lists. The key of the map is the basic vertex index, and its key
     * - the reference indices list.
     * 
     * @param basicVertexIndex
     *            the index of the vertex from its basic table
     * @param resultIndex
     *            the index of the vertex in its result vertex list
     * @param vertexReferenceMap
     *            the reference map
     */
    private void appendVertexReference(int basicVertexIndex, int resultIndex, Map<Integer, List<Integer>> vertexReferenceMap) {
        List<Integer> referenceList = vertexReferenceMap.get(Integer.valueOf(basicVertexIndex));
        if (referenceList == null) {
            referenceList = new ArrayList<Integer>();
            vertexReferenceMap.put(Integer.valueOf(basicVertexIndex), referenceList);
        }
        referenceList.add(Integer.valueOf(resultIndex));
    }
}
