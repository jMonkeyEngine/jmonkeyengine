package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A class that represents a single face in the mesh. The face is a polygon. Its minimum count of
 * vertices is = 3.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class Face implements Comparator<Integer> {
    private static final Logger         LOGGER = Logger.getLogger(Face.class.getName());

    /** The indexes loop of the face. */
    private IndexesLoop                 indexes;

    private List<IndexesLoop>           triangulatedFaces;
    /** Indicates if the face is smooth or solid. */
    private boolean                     smooth;
    /** The material index of the face. */
    private int                         materialNumber;
    /** UV coordinate sets attached to the face. The key is the set name and value are the UV coords. */
    private Map<String, List<Vector2f>> faceUVCoords;
    /** The vertex colors of the face. */
    private List<byte[]>                vertexColors;
    /** The temporal mesh the face belongs to. */
    private TemporalMesh                temporalMesh;

    /**
     * Creates a complete face with all available data.
     * @param indexes
     *            the indexes of the face (required)
     * @param smooth
     *            indicates if the face is smooth or solid
     * @param materialNumber
     *            the material index of the face
     * @param faceUVCoords
     *            UV coordinate sets of the face (optional)
     * @param vertexColors
     *            the vertex colors of the face (optional)
     * @param temporalMesh
     *            the temporal mesh the face belongs to (required)
     */
    public Face(Integer[] indexes, boolean smooth, int materialNumber, Map<String, List<Vector2f>> faceUVCoords, List<byte[]> vertexColors, TemporalMesh temporalMesh) {
        this.setTemporalMesh(temporalMesh);
        this.indexes = new IndexesLoop(indexes);
        this.smooth = smooth;
        this.materialNumber = materialNumber;
        this.faceUVCoords = faceUVCoords;
        this.temporalMesh = temporalMesh;
        this.vertexColors = vertexColors;
    }

    /**
     * Default constructor. Used by the clone method.
     */
    private Face() {
    }

    @Override
    public Face clone() {
        Face result = new Face();
        result.indexes = indexes.clone();
        result.smooth = smooth;
        result.materialNumber = materialNumber;
        if (faceUVCoords != null) {
            result.faceUVCoords = new HashMap<String, List<Vector2f>>(faceUVCoords.size());
            for (Entry<String, List<Vector2f>> entry : faceUVCoords.entrySet()) {
                List<Vector2f> uvs = new ArrayList<Vector2f>(entry.getValue().size());
                for (Vector2f v : entry.getValue()) {
                    uvs.add(v.clone());
                }
                result.faceUVCoords.put(entry.getKey(), uvs);
            }
        }
        if (vertexColors != null) {
            result.vertexColors = new ArrayList<byte[]>(vertexColors.size());
            for (byte[] colors : vertexColors) {
                result.vertexColors.add(colors.clone());
            }
        }
        result.temporalMesh = temporalMesh;
        return result;
    }

    /**
     * Returns the index at the given position in the index loop. If the given position is negative or exceeds
     * the amount of vertices - it is being looped properly so that it always hits an index.
     * For example getIndex(-1) will return the index before the 0 - in this case it will be the last one.
     * @param indexPosition
     *            the index position
     * @return index value at the given position
     */
    private Integer getIndex(int indexPosition) {
        if (indexPosition >= indexes.size()) {
            indexPosition = indexPosition % indexes.size();
        } else if (indexPosition < 0) {
            indexPosition = indexes.size() - -indexPosition % indexes.size();
        }
        return indexes.get(indexPosition);
    }

    /**
     * @return the mesh this face belongs to
     */
    public TemporalMesh getTemporalMesh() {
        return temporalMesh;
    }

    /**
     * @return the original indexes of the face
     */
    public IndexesLoop getIndexes() {
        return indexes;
    }

    /**
     * @return the centroid of the face
     */
    public Vector3f computeCentroid() {
        Vector3f result = new Vector3f();
        List<Vector3f> vertices = temporalMesh.getVertices();
        for (Integer index : indexes) {
            result.addLocal(vertices.get(index));
        }
        return result.divideLocal(indexes.size());
    }

    /**
     * @return current indexes of the face (if it is already triangulated then more than one index group will be in the result list)
     */
    public List<List<Integer>> getCurrentIndexes() {
        if (triangulatedFaces == null) {
            return Arrays.asList(indexes.getAll());
        }
        List<List<Integer>> result = new ArrayList<List<Integer>>(triangulatedFaces.size());
        for (IndexesLoop loop : triangulatedFaces) {
            result.add(loop.getAll());
        }
        return result;
    }

    /**
     * The method detaches the triangle from the face. This method keeps the indexes loop normalized - every index
     * has only two neighbours. So if detaching the triangle causes a vertex to have more than two neighbours - it is
     * also detached and returned as a result.
     * The result is an empty list if no such situation happens.
     * @param triangleIndexes
     *            the indexes of a triangle to be detached
     * @return a list of faces that need to be detached as well in order to keep them normalized
     * @throws BlenderFileException
     *             an exception is thrown when vertices of a face create more than one loop; this is found during path finding
     */
    private List<Face> detachTriangle(Integer[] triangleIndexes) throws BlenderFileException {
        LOGGER.fine("Detaching triangle.");
        if (triangleIndexes.length != 3) {
            throw new IllegalArgumentException("Cannot detach triangle with that does not have 3 indexes!");
        }
        MeshHelper meshHelper = temporalMesh.getBlenderContext().getHelper(MeshHelper.class);
        List<Face> detachedFaces = new ArrayList<Face>();
        List<Integer> path = new ArrayList<Integer>(indexes.size());

        boolean[] edgeRemoved = new boolean[] { indexes.removeEdge(triangleIndexes[0], triangleIndexes[1]), indexes.removeEdge(triangleIndexes[0], triangleIndexes[2]), indexes.removeEdge(triangleIndexes[1], triangleIndexes[2]) };
        Integer[][] indexesPairs = new Integer[][] { new Integer[] { triangleIndexes[0], triangleIndexes[1] }, new Integer[] { triangleIndexes[0], triangleIndexes[2] }, new Integer[] { triangleIndexes[1], triangleIndexes[2] } };

        for (int i = 0; i < 3; ++i) {
            if (!edgeRemoved[i]) {
                indexes.findPath(indexesPairs[i][0], indexesPairs[i][1], path);
                if (path.size() == 0) {
                    indexes.findPath(indexesPairs[i][1], indexesPairs[i][0], path);
                }
                if (path.size() == 0) {
                    throw new IllegalStateException("Triangulation failed. Cannot find path between two indexes. Please apply triangulation in Blender as a workaround.");
                }
                if (detachedFaces.size() == 0 && path.size() < indexes.size()) {
                    Integer[] indexesSublist = path.toArray(new Integer[path.size()]);
                    detachedFaces.add(new Face(indexesSublist, smooth, materialNumber, meshHelper.selectUVSubset(this, indexesSublist), meshHelper.selectVertexColorSubset(this, indexesSublist), temporalMesh));
                    for (int j = 0; j < path.size() - 1; ++j) {
                        indexes.removeEdge(path.get(j), path.get(j + 1));
                    }
                    indexes.removeEdge(path.get(path.size() - 1), path.get(0));
                } else {
                    indexes.addEdge(path.get(path.size() - 1), path.get(0));
                }
            }
        }

        return detachedFaces;
    }

    /**
     * Sets the temporal mesh for the face. The given mesh cannot be null.
     * @param temporalMesh
     *            the temporal mesh of the face
     * @throws IllegalArgumentException
     *             thrown if given temporal mesh is null
     */
    public void setTemporalMesh(TemporalMesh temporalMesh) {
        if (temporalMesh == null) {
            throw new IllegalArgumentException("No temporal mesh for the face given!");
        }
        this.temporalMesh = temporalMesh;
    }

    /**
     * Flips the order of the indexes.
     */
    public void flipIndexes() {
        indexes.reverse();
        if (faceUVCoords != null) {
            for (Entry<String, List<Vector2f>> entry : faceUVCoords.entrySet()) {
                Collections.reverse(entry.getValue());
            }
        }
    }

    /**
     * Flips UV coordinates.
     * @param u
     *            indicates if U coords should be flipped
     * @param v
     *            indicates if V coords should be flipped
     */
    public void flipUV(boolean u, boolean v) {
        if (faceUVCoords != null) {
            for (Entry<String, List<Vector2f>> entry : faceUVCoords.entrySet()) {
                for (Vector2f uv : entry.getValue()) {
                    uv.set(u ? 1 - uv.x : uv.x, v ? 1 - uv.y : uv.y);
                }
            }
        }
    }

    /**
     * @return the UV sets of the face
     */
    public Map<String, List<Vector2f>> getUvSets() {
        return faceUVCoords;
    }

    /**
     * @return current vertex count of the face
     */
    public int vertexCount() {
        return indexes.size();
    }

    /**
     * The method triangulates the face.
     */
    public TriangulationWarning triangulate() {
        LOGGER.fine("Triangulating face.");
        assert indexes.size() >= 3 : "Invalid indexes amount for face. 3 is the required minimum!";
        triangulatedFaces = new ArrayList<IndexesLoop>(indexes.size() - 2);
        Integer[] indexes = new Integer[3];
        TriangulationWarning warning = TriangulationWarning.NONE;
        
        try {
            List<Face> facesToTriangulate = new ArrayList<Face>(Arrays.asList(this.clone()));
            while (facesToTriangulate.size() > 0 && warning == TriangulationWarning.NONE) {
                Face face = facesToTriangulate.remove(0);
                // two special cases will improve the computations speed
                if(face.getIndexes().size() == 3) {
                	triangulatedFaces.add(face.getIndexes().clone());
                } else {
                	int previousIndex1 = -1, previousIndex2 = -1, previousIndex3 = -1;
                    while (face.vertexCount() > 0) {
                        indexes[0] = face.getIndex(0);
                        indexes[1] = face.findClosestVertex(indexes[0], -1);
                        indexes[2] = face.findClosestVertex(indexes[0], indexes[1]);

                        LOGGER.finer("Veryfying improper triangulation of the temporal mesh.");
                        if (indexes[0] < 0 || indexes[1] < 0 || indexes[2] < 0) {
                            warning = TriangulationWarning.CLOSEST_VERTS;
                            break;
                        }
                        if (previousIndex1 == indexes[0] && previousIndex2 == indexes[1] && previousIndex3 == indexes[2]) {
                            warning = TriangulationWarning.INFINITE_LOOP;
                            break;
                        }
                        previousIndex1 = indexes[0];
                        previousIndex2 = indexes[1];
                        previousIndex3 = indexes[2];

                        Arrays.sort(indexes, this);
                        facesToTriangulate.addAll(face.detachTriangle(indexes));
                        triangulatedFaces.add(new IndexesLoop(indexes));
                    }
                }
            }
        } catch (BlenderFileException e) {
            LOGGER.log(Level.WARNING, "Errors occurred during face triangulation: {0}. The face will be triangulated with the most direct algorithm, but the results might not be identical to blender.", e.getLocalizedMessage());
            warning = TriangulationWarning.UNKNOWN;
        }
        if(warning != TriangulationWarning.NONE) {
            LOGGER.finest("Triangulation the face using the most direct algorithm.");
            indexes[0] = this.getIndex(0);
            for (int i = 1; i < this.vertexCount() - 1; ++i) {
                indexes[1] = this.getIndex(i);
                indexes[2] = this.getIndex(i + 1);
                triangulatedFaces.add(new IndexesLoop(indexes));
            }
        }
        return warning;
    }
    
    /**
     * A warning that indicates a problem with face triangulation. The warnings are collected and displayed once for each type for a mesh to
     * avoid multiple warning loggings during triangulation. The amount of iterations can be really huge and logging every single failure would
     * really slow down the importing process and make logs unreadable.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static enum TriangulationWarning {
        NONE(null), 
        CLOSEST_VERTS("Unable to find two closest vertices while triangulating face."), 
        INFINITE_LOOP("Infinite loop detected during triangulation."), 
        UNKNOWN("There was an unknown problem with face triangulation. Please see log for details.");

        private String description;

        private TriangulationWarning(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * @return <b>true</b> if the face is smooth and <b>false</b> otherwise
     */
    public boolean isSmooth() {
        return smooth;
    }

    /**
     * @return the material index of the face
     */
    public int getMaterialNumber() {
        return materialNumber;
    }

    /**
     * @return the vertices colord of the face
     */
    public List<byte[]> getVertexColors() {
        return vertexColors;
    }

    @Override
    public String toString() {
        return "Face " + indexes;
    }

	/**
	 * The method finds the closest vertex to the one specified by <b>index</b>.
	 * If the vertexToIgnore is positive than it will be ignored in the result.
	 * The closest vertex must be able to create an edge that is fully contained
	 * within the face and does not cross any other edges. Also if the
	 * vertexToIgnore is not negative then the condition that the edge between
	 * the found index and the one to ignore is inside the face must also be
	 * met.
	 * 
	 * @param index
	 *            the index of the vertex that needs to have found the nearest
	 *            neighbour
	 * @param indexToIgnore
	 *            the index to ignore in the result (pass -1 if none is to be
	 *            ignored)
	 * @return the index of the closest vertex to the given one
	 */
    private int findClosestVertex(int index, int indexToIgnore) {
        int result = -1;
        List<Vector3f> vertices = temporalMesh.getVertices();
        Vector3f v1 = vertices.get(index);
        float distance = Float.MAX_VALUE;
        for (int i : indexes) {
            if (i != index && i != indexToIgnore) {
                Vector3f v2 = vertices.get(i);
                float d = v2.distance(v1);
                if (d < distance && this.contains(new Edge(index, i, 0, true, temporalMesh)) && (indexToIgnore < 0 || this.contains(new Edge(indexToIgnore, i, 0, true, temporalMesh)))) {
                    result = i;
                    distance = d;
                }
            }
        }
        return result;
    }

    /**
     * The method verifies if the edge is contained within the face.
     * It means it cannot cross any other edge and it must be inside the face and not outside of it.
     * @param edge
     *            the edge to be checked
     * @return <b>true</b> if the given edge is contained within the face and <b>false</b> otherwise
     */
    private boolean contains(Edge edge) {
        int index1 = edge.getFirstIndex();
        int index2 = edge.getSecondIndex();
        // check if the line between the vertices is not a border edge of the face
        if (!indexes.areNeighbours(index1, index2)) {
            for (int i = 0; i < indexes.size(); ++i) {
                int i1 = this.getIndex(i - 1);
                int i2 = this.getIndex(i);
                // check if the edges have no common verts (because if they do, they cannot cross)
                if (i1 != index1 && i1 != index2 && i2 != index1 && i2 != index2) {
                    if (edge.cross(new Edge(i1, i2, 0, false, temporalMesh))) {
                        return false;
                    }
                }
            }

            // computing the edge's middle point
            Vector3f edgeMiddlePoint = edge.computeCentroid();
            // computing the edge that is perpendicular to the given edge and has a length of 1 (length actually does not matter)
            Vector3f edgeVector = edge.getSecondVertex().subtract(edge.getFirstVertex());
            Vector3f edgeNormal = temporalMesh.getNormals().get(index1).cross(edgeVector).normalizeLocal();
            Edge e = new Edge(edgeMiddlePoint, edgeNormal.add(edgeMiddlePoint));
            // compute the vectors from the middle point to the crossing between the extended edge 'e' and other edges of the face
            List<Vector3f> crossingVectors = new ArrayList<Vector3f>();
            for (int i = 0; i < indexes.size(); ++i) {
                int i1 = this.getIndex(i);
                int i2 = this.getIndex(i + 1);
            	Vector3f crossPoint = e.getCrossPoint(new Edge(i1, i2, 0, false, temporalMesh), true, false);
                if(crossPoint != null) {
                	crossingVectors.add(crossPoint.subtractLocal(edgeMiddlePoint));
                }
            }
            if(crossingVectors.size() == 0) {
            	return false;// edges do not cross
            }
            
            // use only distinct vertices (doubles may appear if the crossing point is a vertex)
            List<Vector3f> distinctCrossingVectors = new ArrayList<Vector3f>();
            for(Vector3f cv : crossingVectors) {
        		double minDistance = Double.MAX_VALUE;
        		for(Vector3f dcv : distinctCrossingVectors) {
        			minDistance = Math.min(minDistance, dcv.distance(cv));
        		}
        		if(minDistance > FastMath.FLT_EPSILON) {
        			distinctCrossingVectors.add(cv);
        		}
            }
            
            if(distinctCrossingVectors.size() == 0) {
            	throw new IllegalStateException("There MUST be at least 2 crossing vertices!");
            }
            // checking if all crossing vectors point to the same direction (if yes then the edge is outside the face)
            float direction = Math.signum(distinctCrossingVectors.get(0).dot(edgeNormal));// if at least one vector has different direction that this - it means that the edge is inside the face
            for(int i=1;i<distinctCrossingVectors.size();++i) {
            	if(direction != Math.signum(distinctCrossingVectors.get(i).dot(edgeNormal))) {
            		return true;
            	}
            }
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + indexes.hashCode();
        result = prime * result + temporalMesh.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Face)) {
            return false;
        }
        Face other = (Face) obj;
        if (!indexes.equals(other.indexes)) {
            return false;
        }
        return temporalMesh.equals(other.temporalMesh);
    }

    /**
     * Loads all faces of a given mesh.
     * @param meshStructure
     *            the mesh structure we read the faces from
     * @param userUVGroups
     *            UV groups defined by the user
     * @param verticesColors
     *            the vertices colors of the mesh
     * @param temporalMesh
     *            the temporal mesh the faces will belong to
     * @param blenderContext
     *            the blender context
     * @return list of faces read from the given mesh structure
     * @throws BlenderFileException
     *             an exception is thrown when problems with file reading occur
     */
    public static List<Face> loadAll(Structure meshStructure, Map<String, List<Vector2f>> userUVGroups, List<byte[]> verticesColors, TemporalMesh temporalMesh, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading all faces from mesh: {0}", meshStructure.getName());
        List<Face> result = new ArrayList<Face>();
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
        if (meshHelper.isBMeshCompatible(meshStructure)) {
            LOGGER.fine("Reading BMesh.");
            Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
            Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");

            if (pMPoly.isNotNull() && pMLoop.isNotNull()) {
                List<Structure> polys = pMPoly.fetchData();
                List<Structure> loops = pMLoop.fetchData();
                for (Structure poly : polys) {
                    int materialNumber = ((Number) poly.getFieldValue("mat_nr")).intValue();
                    int loopStart = ((Number) poly.getFieldValue("loopstart")).intValue();
                    int totLoop = ((Number) poly.getFieldValue("totloop")).intValue();
                    boolean smooth = (((Number) poly.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
                    Integer[] vertexIndexes = new Integer[totLoop];

                    for (int i = loopStart; i < loopStart + totLoop; ++i) {
                        vertexIndexes[i - loopStart] = ((Number) loops.get(i).getFieldValue("v")).intValue();
                    }

                    // uvs always must be added wheater we have texture or not
                    Map<String, List<Vector2f>> uvCoords = new HashMap<String, List<Vector2f>>();
                    for (Entry<String, List<Vector2f>> entry : userUVGroups.entrySet()) {
                        List<Vector2f> uvs = entry.getValue().subList(loopStart, loopStart + totLoop);
                        uvCoords.put(entry.getKey(), new ArrayList<Vector2f>(uvs));
                    }

                    List<byte[]> vertexColors = null;
                    if (verticesColors != null && verticesColors.size() > 0) {
                        vertexColors = new ArrayList<byte[]>(totLoop);
                        for (int i = loopStart; i < loopStart + totLoop; ++i) {
                            vertexColors.add(verticesColors.get(i));
                        }
                    }

                    result.add(new Face(vertexIndexes, smooth, materialNumber, uvCoords, vertexColors, temporalMesh));
                }
            }
        } else {
            LOGGER.fine("Reading traditional faces.");
            Pointer pMFace = (Pointer) meshStructure.getFieldValue("mface");
            List<Structure> mFaces = pMFace.isNotNull() ? pMFace.fetchData() : null;
            if (mFaces != null && mFaces.size() > 0) {
                // indicates if the material with the specified number should have a texture attached
                for (int i = 0; i < mFaces.size(); ++i) {
                    Structure mFace = mFaces.get(i);
                    int materialNumber = ((Number) mFace.getFieldValue("mat_nr")).intValue();
                    boolean smooth = (((Number) mFace.getFieldValue("flag")).byteValue() & 0x01) != 0x00;

                    int v1 = ((Number) mFace.getFieldValue("v1")).intValue();
                    int v2 = ((Number) mFace.getFieldValue("v2")).intValue();
                    int v3 = ((Number) mFace.getFieldValue("v3")).intValue();
                    int v4 = ((Number) mFace.getFieldValue("v4")).intValue();

                    int vertCount = v4 == 0 ? 3 : 4;

                    // uvs always must be added wheater we have texture or not
                    Map<String, List<Vector2f>> faceUVCoords = new HashMap<String, List<Vector2f>>();
                    for (Entry<String, List<Vector2f>> entry : userUVGroups.entrySet()) {
                        List<Vector2f> uvCoordsForASingleFace = new ArrayList<Vector2f>(vertCount);
                        for (int j = 0; j < vertCount; ++j) {
                            uvCoordsForASingleFace.add(entry.getValue().get(i * 4 + j));
                        }
                        faceUVCoords.put(entry.getKey(), uvCoordsForASingleFace);
                    }

                    List<byte[]> vertexColors = null;
                    if (verticesColors != null && verticesColors.size() > 0) {
                        vertexColors = new ArrayList<byte[]>(vertCount);

                        vertexColors.add(verticesColors.get(v1));
                        vertexColors.add(verticesColors.get(v2));
                        vertexColors.add(verticesColors.get(v3));
                        if (vertCount == 4) {
                            vertexColors.add(verticesColors.get(v4));
                        }
                    }

                    result.add(new Face(vertCount == 4 ? new Integer[] { v1, v2, v3, v4 } : new Integer[] { v1, v2, v3 }, smooth, materialNumber, faceUVCoords, vertexColors, temporalMesh));
                }
            }
        }
        LOGGER.log(Level.FINE, "Loaded {0} faces.", result.size());
        return result;
    }

    @Override
    public int compare(Integer index1, Integer index2) {
        return indexes.indexOf(index1) - indexes.indexOf(index2);
    }
}
