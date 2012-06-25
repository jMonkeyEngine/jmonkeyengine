package com.jme3.scene.plugins.blender.meshes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;

/*package*/ class MeshBuilder {
	/** An array of reference vertices. */
	private Vector3f[][] verticesAndNormals;
	/** An list of vertices colors. */
	private List<byte[]> verticesColors;
	/** A variable that indicates if the model uses generated textures. */
	private boolean usesGeneratedTextures;
	
	/** This map's key is the vertex index from 'vertices 'table and the value are indices from 'vertexList'
    positions (it simply tells which vertex is referenced where in the result list). */
    private Map<Integer, Map<Integer, List<Integer>>> globalVertexReferenceMap;
    
    /** A map between vertex index and its UV coordinates. */
    private Map<Integer, Vector2f> uvsMap = new HashMap<Integer, Vector2f>();
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>> normalMap = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>> vertexMap = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts vertices colors by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<byte[]>> vertexColorsMap = new HashMap<Integer, List<byte[]>>();
    /** The following map sorts indexes by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Integer>> indexMap = new HashMap<Integer, List<Integer>>();
    /** A map between material number and UV coordinates of mesh that has this material applied. */
    private Map<Integer, List<Vector2f>> uvCoordinates = new HashMap<Integer, List<Vector2f>>();//<material_number; list of uv coordinates for mesh's vertices>
    
    /**
     * Constructor. Stores the given array (not copying it).
     * The second argument describes if the model uses generated textures. If yes then no vertex amount optimisation is applied.
     * The amount of vertices is always faceCount * 3.
     * @param verticesAndNormals the reference vertices and normals array
     * @param usesGeneratedTextures a variable that indicates if the model uses generated textures or not
     */
	public MeshBuilder(Vector3f[][] verticesAndNormals, List<byte[]> verticesColors, boolean usesGeneratedTextures) {
		if(verticesAndNormals == null || verticesAndNormals.length == 0) {
			throw new IllegalArgumentException("No vertices loaded to build mesh.");
		}
		this.verticesAndNormals = verticesAndNormals;
		this.verticesColors = verticesColors;
		this.usesGeneratedTextures = usesGeneratedTextures;
		globalVertexReferenceMap = new HashMap<Integer, Map<Integer, List<Integer>>>(verticesAndNormals.length);
	}
	
	/**
	 * This method adds a face to the mesh.
	 * @param v1 index of the 1'st vertex from the reference vertex table
	 * @param v2 index of the 2'nd vertex from the reference vertex table
	 * @param v3 index of the 3'rd vertex from the reference vertex table
	 * @param smooth indicates if this face should have smooth shading or flat shading
	 * @param materialNumber the material number for this face
	 * @param uvs a 3-element array of vertices UV coordinates
	 * @param quad indicates if the appended face is a part of a quad face (used for creating vertex colors buffer)
	 * @param faceIndex the face index (used for creating vertex colors buffer)
	 */
	public void appendFace(int v1, int v2, int v3, boolean smooth, int materialNumber, Vector2f[] uvs, boolean quad, int faceIndex) {
		if(uvs != null && uvs.length != 3) {
			throw new IllegalArgumentException("UV coordinates must be a 3-element array!");
		}
		
		//getting the required lists
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
        List<byte[]> vertexColorsList = vertexColorsMap != null ? vertexColorsMap.get(materialNumber) : null;
        int[] vertexColorIndex = new int[] { 0, 1, 2 };
        if(vertexColorsList == null && vertexColorsMap != null) {
        	vertexColorsList = new ArrayList<byte[]>();
        	vertexColorsMap.put(materialNumber, vertexColorsList);
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
        
        faceIndex *= 4;
        if(quad) {
        	vertexColorIndex[1] = 2;
        	vertexColorIndex[2] = 3;
        }
        
        //creating faces
        Integer[] index = new Integer[] {v1, v2, v3};
        if(smooth && !usesGeneratedTextures) {
			for (int i = 0; i < 3; ++i) {
        		if(!vertexReferenceMap.containsKey(index[i])) {
            		this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
            		vertexList.add(verticesAndNormals[index[i]][0]);
            		if(verticesColors != null) {
            			vertexColorsList.add(verticesColors.get(faceIndex + vertexColorIndex[i]));
            		}
            		normalList.add(verticesAndNormals[index[i]][1]);
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
            			vertexList.add(verticesAndNormals[index[i]][0]);
            			if(verticesColors != null) {
            				vertexColorsList.add(verticesColors.get(faceIndex + vertexColorIndex[i]));
            			}
            			normalList.add(verticesAndNormals[index[i]][1]);
            			uvCoordinatesList.add(uvs[i]);
            			index[i] = vertexList.size() - 1;
            		}
            	} else {
            		index[i] = vertexList.indexOf(verticesAndNormals[index[i]]);
            	}
        		indexList.add(index[i]);
        	}
        } else {
        	Vector3f n = FastMath.computeNormal(verticesAndNormals[v1][0], verticesAndNormals[v2][0], verticesAndNormals[v3][0]);
        	for (int i = 0; i < 3; ++i) {
        		indexList.add(vertexList.size());
        		this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
        		if(uvCoordinatesList != null) {
        			uvCoordinatesList.add(uvs[i]);
        			uvsMap.put(vertexList.size(), uvs[i]);
        		}
        		vertexList.add(verticesAndNormals[index[i]][0]);
        		if(verticesColors != null) {
        			vertexColorsList.add(verticesColors.get(faceIndex + vertexColorIndex[i]));
        		}
        		normalList.add(n);
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
	 * @param materialNumber
	 *            the material index
	 * @return result vertices array
	 */
	public Vector3f[] getVertices(int materialNumber) {
		return vertexMap.get(materialNumber).toArray(new Vector3f[vertexMap.get(materialNumber).size()]);
	}
	
	/**
	 * @param materialNumber
	 *            the material index
	 * @return the amount of result vertices
	 */
	public int getVerticesAmount(int materialNumber) {
		return vertexMap.get(materialNumber).size();
	}
	
	/**
	 * @param materialNumber
	 *            the material index
	 * @return normals result array
	 */
	public Vector3f[] getNormals(int materialNumber) {
		return normalMap.get(materialNumber).toArray(new Vector3f[normalMap.get(materialNumber).size()]);
	}
	
	/**
	 * @param materialNumber
	 *            the material index
	 * @return the vertices colors buffer or null if no vertex colors is set
	 */
	public ByteBuffer getVertexColorsBuffer(int materialNumber) {
		ByteBuffer result = null;
		if (verticesColors != null && vertexColorsMap.get(materialNumber) != null) {
			List<byte[]> data = vertexColorsMap.get(materialNumber);
			result = BufferUtils.createByteBuffer(4 * data.size());
			for (byte[] v : data) {
				if (v != null) {
					result.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
				} else {
					result.put((byte)0).put((byte)0).put((byte)0).put((byte)0);
				}
			}
			result.flip();
		}
		return result;
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
	 * @param materialNumber
	 *            the material number that is appied to the mesh
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
	 * @return <b>true</b> if the mesh has no vertices and <b>false</b> otherwise
	 */
	public boolean isEmpty() {
		return vertexMap.size() == 0;
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
