package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.Edge;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.plugins.blender.textures.TexturePixel;

/**
 * A modifier that subdivides the mesh using either simple or catmull-clark subdivision.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SubdivisionSurfaceModifier extends Modifier {
    private static final Logger LOGGER                  = Logger.getLogger(SubdivisionSurfaceModifier.class.getName());

    private static final int    TYPE_CATMULLCLARK       = 0;
    private static final int    TYPE_SIMPLE             = 1;

    private static final int    FLAG_SUBDIVIDE_UVS      = 0x8;

    /** The subdivision type. */
    private int                 subdivType;
    /** The amount of subdivision levels. */
    private int                 levels;
    /** Indicates if the UV's should also be subdivided. */
    private boolean             subdivideUVS;

    private List<Integer>       verticesOnOriginalEdges = new ArrayList<Integer>();

    /**
     * Constructor loads all neccessary modifier data.
     * @param modifierStructure
     *            the modifier structure
     * @param blenderContext
     *            the blender context
     */
    public SubdivisionSurfaceModifier(Structure modifierStructure, BlenderContext blenderContext) {
        if (this.validate(modifierStructure, blenderContext)) {
            subdivType = ((Number) modifierStructure.getFieldValue("subdivType")).intValue();
            levels = ((Number) modifierStructure.getFieldValue("levels")).intValue();
            int flag = ((Number) modifierStructure.getFieldValue("flags")).intValue();
            subdivideUVS = (flag & FLAG_SUBDIVIDE_UVS) != 0 && subdivType == TYPE_CATMULLCLARK;

            if (subdivType != TYPE_CATMULLCLARK && subdivType != TYPE_SIMPLE) {
                LOGGER.log(Level.SEVERE, "Unknown subdivision type: {0}.", subdivType);
                invalid = true;
            }
            if (levels < 0) {
                LOGGER.severe("The amount of subdivision levels cannot be negative.");
                invalid = true;
            }
        }
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Subdivision surface modifier is invalid! Cannot be applied to: {0}", node.getName());
        } else if (levels > 0) {// no need to do anything if the levels is set to zero
            TemporalMesh temporalMesh = this.getTemporalMesh(node);
            if (temporalMesh != null) {
                LOGGER.log(Level.FINE, "Applying subdivision surface modifier to: {0}", temporalMesh);

                for (Edge edge : temporalMesh.getEdges()) {
                    verticesOnOriginalEdges.add(edge.getFirstIndex());
                    verticesOnOriginalEdges.add(edge.getSecondIndex());
                }

                if (subdivType == TYPE_CATMULLCLARK) {
                    for (int i = 0; i < levels; ++i) {
                        this.subdivideSimple(temporalMesh);// first do simple subdivision ...
                        this.subdivideCatmullClark(temporalMesh);// ... and then apply Catmull-Clark algorithm
                        if (subdivideUVS) {// UV's can be subdivided only for Catmull-Clark subdivision algorithm
                            this.subdivideUVs(temporalMesh);
                        }
                    }
                } else {
                    for (int i = 0; i < levels; ++i) {
                        this.subdivideSimple(temporalMesh);
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
            }
        }
    }

    /**
     * Catmull-Clark subdivision. It assumes that the mesh was already simple-subdivided.
     * @param temporalMesh
     *            the mesh whose vertices will be transformed to form Catmull-Clark subdivision
     */
    private void subdivideCatmullClark(TemporalMesh temporalMesh) {
        Set<Integer> boundaryVertices = new HashSet<Integer>();
        for (Face face : temporalMesh.getFaces()) {
            for (Integer index : face.getIndexes()) {
                if (temporalMesh.isBoundary(index)) {
                    boundaryVertices.add(index);
                }
            }
        }

        List<CreasePoint> creasePoints = new ArrayList<CreasePoint>(temporalMesh.getVertexCount());
        for (int i = 0; i < temporalMesh.getVertexCount(); ++i) {
            // finding adjacent edges that were created by dividing original edges
            List<Edge> adjacentOriginalEdges = new ArrayList<Edge>();
            for (Edge edge : temporalMesh.getAdjacentEdges(i)) {
                if (verticesOnOriginalEdges.contains(edge.getFirstIndex()) || verticesOnOriginalEdges.contains(edge.getSecondIndex())) {
                    adjacentOriginalEdges.add(edge);
                }
            }

            creasePoints.add(new CreasePoint(i, boundaryVertices.contains(i), adjacentOriginalEdges, temporalMesh));
        }

        Vector3f[] averageVert = new Vector3f[temporalMesh.getVertexCount()];
        int[] averageCount = new int[temporalMesh.getVertexCount()];

        for (Face face : temporalMesh.getFaces()) {
            Vector3f centroid = face.computeCentroid();

            for (Integer index : face.getIndexes()) {
                if (boundaryVertices.contains(index)) {
                    Edge edge = this.findEdge(temporalMesh, index, face.getIndexes().getNextIndex(index));
                    if (temporalMesh.isBoundary(edge)) {
                        averageVert[index] = averageVert[index] == null ? edge.computeCentroid() : averageVert[index].addLocal(edge.computeCentroid());
                        averageCount[index] += 1;
                    }
                    edge = this.findEdge(temporalMesh, face.getIndexes().getPreviousIndex(index), index);
                    if (temporalMesh.isBoundary(edge)) {
                        averageVert[index] = averageVert[index] == null ? edge.computeCentroid() : averageVert[index].addLocal(edge.computeCentroid());
                        averageCount[index] += 1;
                    }
                } else {
                    averageVert[index] = averageVert[index] == null ? centroid.clone() : averageVert[index].addLocal(centroid);
                    averageCount[index] += 1;
                }
            }
        }
        for (Edge edge : temporalMesh.getEdges()) {
            if (!edge.isInFace()) {
                Vector3f centroid = temporalMesh.getVertices().get(edge.getFirstIndex()).add(temporalMesh.getVertices().get(edge.getSecondIndex())).divideLocal(2);

                averageVert[edge.getFirstIndex()] = averageVert[edge.getFirstIndex()] == null ? centroid.clone() : averageVert[edge.getFirstIndex()].addLocal(centroid);
                averageVert[edge.getSecondIndex()] = averageVert[edge.getSecondIndex()] == null ? centroid.clone() : averageVert[edge.getSecondIndex()].addLocal(centroid);
                averageCount[edge.getSecondIndex()] += 2;
            }
        }

        for (int i = 0; i < averageVert.length; ++i) {
            Vector3f v = temporalMesh.getVertices().get(i);
            averageVert[i].divideLocal(averageCount[i]);

            // computing translation vector
            Vector3f t = averageVert[i].subtract(v);
            if (!boundaryVertices.contains(i)) {
                t.multLocal(4 / (float) averageCount[i]);
            }

            // moving the vertex
            v.addLocal(t);

            // applying crease weight if neccessary
            CreasePoint creasePoint = creasePoints.get(i);
            if (creasePoint.getTarget() != null && creasePoint.getWeight() != 0) {
                t = creasePoint.getTarget().subtractLocal(v).multLocal(creasePoint.getWeight());
                v.addLocal(t);
            }
        }
    }

    /**
     * The method performs a simple subdivision of the mesh.
     * 
     * @param temporalMesh
     *            the mesh to be subdivided
     */
    @SuppressWarnings("unchecked")
    private void subdivideSimple(TemporalMesh temporalMesh) {
        Map<Edge, Integer> edgePoints = new HashMap<Edge, Integer>();
        Map<Face, Integer> facePoints = new HashMap<Face, Integer>();
        List<Face> newFaces = new ArrayList<Face>();
        List<Edge> newEdges = new ArrayList<Edge>(temporalMesh.getEdges().size() * 2);

        int originalFacesCount = temporalMesh.getFaces().size();

        List<Map<String, Float>> vertexGroups = temporalMesh.getVertexGroups();
        // the result vertex array will have verts in the following order [[original_verts], [face_verts], [edge_verts]]
        List<Vector3f> vertices = temporalMesh.getVertices();
        List<Vector3f> edgeVertices = new ArrayList<Vector3f>();
        List<Vector3f> faceVertices = new ArrayList<Vector3f>();
        // the same goes for normals
        List<Vector3f> normals = temporalMesh.getNormals();
        List<Vector3f> edgeNormals = new ArrayList<Vector3f>();
        List<Vector3f> faceNormals = new ArrayList<Vector3f>();

        List<Face> faces = temporalMesh.getFaces();
        for (Face face : faces) {
            Map<String, List<Vector2f>> uvSets = face.getUvSets();

            Vector3f facePoint = face.computeCentroid();
            Integer facePointIndex = vertices.size() + faceVertices.size();
            facePoints.put(face, facePointIndex);
            faceVertices.add(facePoint);
            faceNormals.add(this.computeFaceNormal(face));
            Map<String, Vector2f> faceUV = this.computeFaceUVs(face);
            byte[] faceVertexColor = this.computeFaceVertexColor(face);
            Map<String, Float> faceVertexGroups = this.computeFaceVertexGroups(face);
            if (vertexGroups.size() > 0) {
                vertexGroups.add(faceVertexGroups);
            }

            for (int i = 0; i < face.getIndexes().size(); ++i) {
                int vIndex = face.getIndexes().get(i);
                int vPrevIndex = i == 0 ? face.getIndexes().get(face.getIndexes().size() - 1) : face.getIndexes().get(i - 1);
                int vNextIndex = i == face.getIndexes().size() - 1 ? face.getIndexes().get(0) : face.getIndexes().get(i + 1);

                Edge prevEdge = this.findEdge(temporalMesh, vPrevIndex, vIndex);
                Edge nextEdge = this.findEdge(temporalMesh, vIndex, vNextIndex);
                int vPrevEdgeVertIndex = edgePoints.containsKey(prevEdge) ? edgePoints.get(prevEdge) : -1;
                int vNextEdgeVertIndex = edgePoints.containsKey(nextEdge) ? edgePoints.get(nextEdge) : -1;

                Vector3f v = temporalMesh.getVertices().get(vIndex);
                if (vPrevEdgeVertIndex < 0) {
                    vPrevEdgeVertIndex = vertices.size() + originalFacesCount + edgeVertices.size();
                    verticesOnOriginalEdges.add(vNextEdgeVertIndex);
                    edgeVertices.add(vertices.get(vPrevIndex).add(v).divideLocal(2));
                    edgeNormals.add(normals.get(vPrevIndex).add(normals.get(vIndex)).normalizeLocal());
                    edgePoints.put(prevEdge, vPrevEdgeVertIndex);
                    if (vertexGroups.size() > 0) {
                        vertexGroups.add(this.interpolateVertexGroups(Arrays.asList(vertexGroups.get(vPrevIndex), vertexGroups.get(vIndex))));
                    }
                }
                if (vNextEdgeVertIndex < 0) {
                    vNextEdgeVertIndex = vertices.size() + originalFacesCount + edgeVertices.size();
                    verticesOnOriginalEdges.add(vPrevEdgeVertIndex);
                    edgeVertices.add(vertices.get(vNextIndex).add(v).divideLocal(2));
                    edgeNormals.add(normals.get(vNextIndex).add(normals.get(vIndex)).normalizeLocal());
                    edgePoints.put(nextEdge, vNextEdgeVertIndex);
                    if (vertexGroups.size() > 0) {
                        vertexGroups.add(this.interpolateVertexGroups(Arrays.asList(vertexGroups.get(vNextIndex), vertexGroups.get(vIndex))));
                    }
                }

                Integer[] indexes = new Integer[] { vIndex, vNextEdgeVertIndex, facePointIndex, vPrevEdgeVertIndex };

                Map<String, List<Vector2f>> newUVSets = null;
                if (uvSets != null) {
                    newUVSets = new HashMap<String, List<Vector2f>>(uvSets.size());
                    for (Entry<String, List<Vector2f>> uvset : uvSets.entrySet()) {
                        int indexOfvIndex = i;
                        int indexOfvPrevIndex = face.getIndexes().indexOf(vPrevIndex);
                        int indexOfvNextIndex = face.getIndexes().indexOf(vNextIndex);

                        Vector2f uv1 = uvset.getValue().get(indexOfvIndex);
                        Vector2f uv2 = uvset.getValue().get(indexOfvNextIndex).add(uv1).divideLocal(2);
                        Vector2f uv3 = faceUV.get(uvset.getKey());
                        Vector2f uv4 = uvset.getValue().get(indexOfvPrevIndex).add(uv1).divideLocal(2);
                        List<Vector2f> uvList = Arrays.asList(uv1, uv2, uv3, uv4);
                        newUVSets.put(uvset.getKey(), new ArrayList<Vector2f>(uvList));
                    }
                }

                List<byte[]> vertexColors = null;
                if (face.getVertexColors() != null) {

                    int indexOfvIndex = i;
                    int indexOfvPrevIndex = face.getIndexes().indexOf(vPrevIndex);
                    int indexOfvNextIndex = face.getIndexes().indexOf(vNextIndex);

                    byte[] vCol1 = face.getVertexColors().get(indexOfvIndex);
                    byte[] vCol2 = this.interpolateVertexColors(face.getVertexColors().get(indexOfvNextIndex), vCol1);
                    byte[] vCol3 = faceVertexColor;
                    byte[] vCol4 = this.interpolateVertexColors(face.getVertexColors().get(indexOfvPrevIndex), vCol1);
                    vertexColors = new ArrayList<byte[]>(Arrays.asList(vCol1, vCol2, vCol3, vCol4));
                }

                newFaces.add(new Face(indexes, face.isSmooth(), face.getMaterialNumber(), newUVSets, vertexColors, temporalMesh));

                newEdges.add(new Edge(vIndex, vNextEdgeVertIndex, nextEdge.getCrease(), true, temporalMesh));
                newEdges.add(new Edge(vNextEdgeVertIndex, facePointIndex, 0, true, temporalMesh));
                newEdges.add(new Edge(facePointIndex, vPrevEdgeVertIndex, 0, true, temporalMesh));
                newEdges.add(new Edge(vPrevEdgeVertIndex, vIndex, prevEdge.getCrease(), true, temporalMesh));
            }
        }

        vertices.addAll(faceVertices);
        vertices.addAll(edgeVertices);
        normals.addAll(faceNormals);
        normals.addAll(edgeNormals);

        for (Edge edge : temporalMesh.getEdges()) {
            if (!edge.isInFace()) {
                int newVertexIndex = vertices.size();
                vertices.add(vertices.get(edge.getFirstIndex()).add(vertices.get(edge.getSecondIndex())).divideLocal(2));
                normals.add(normals.get(edge.getFirstIndex()).add(normals.get(edge.getSecondIndex())).normalizeLocal());

                newEdges.add(new Edge(edge.getFirstIndex(), newVertexIndex, 0, false, temporalMesh));
                newEdges.add(new Edge(newVertexIndex, edge.getSecondIndex(), 0, false, temporalMesh));
            }
        }

        temporalMesh.getFaces().clear();
        temporalMesh.getFaces().addAll(newFaces);
        temporalMesh.getEdges().clear();
        temporalMesh.getEdges().addAll(newEdges);

        temporalMesh.rebuildIndexesMappings();
    }

    /**
     * The method subdivides mesh's UV coordinates. It actually performs only Catmull-Clark modifications because if any UV's are present then they are
     * automatically subdivided by the simple algorithm.
     * @param temporalMesh
     *            the mesh whose UV coordinates will be applied Catmull-Clark algorithm
     */
    private void subdivideUVs(TemporalMesh temporalMesh) {
        List<Face> faces = temporalMesh.getFaces();
        Map<String, UvCoordsSubdivideTemporalMesh> subdividedUVS = new HashMap<String, UvCoordsSubdivideTemporalMesh>();
        for (Face face : faces) {
            if (face.getUvSets() != null) {
                for (Entry<String, List<Vector2f>> uvset : face.getUvSets().entrySet()) {
                    UvCoordsSubdivideTemporalMesh uvCoordsSubdivideTemporalMesh = subdividedUVS.get(uvset.getKey());
                    if (uvCoordsSubdivideTemporalMesh == null) {
                        try {
                            uvCoordsSubdivideTemporalMesh = new UvCoordsSubdivideTemporalMesh(temporalMesh.getBlenderContext());
                        } catch (BlenderFileException e) {
                            assert false : "Something went really wrong! The UvCoordsSubdivideTemporalMesh class should NOT throw exceptions here!";
                        }
                        subdividedUVS.put(uvset.getKey(), uvCoordsSubdivideTemporalMesh);
                    }
                    uvCoordsSubdivideTemporalMesh.addFace(uvset.getValue());
                }
            }
        }

        for (Entry<String, UvCoordsSubdivideTemporalMesh> entry : subdividedUVS.entrySet()) {
            entry.getValue().rebuildIndexesMappings();
            this.subdivideCatmullClark(entry.getValue());

            for (int i = 0; i < faces.size(); ++i) {
                List<Vector2f> uvs = faces.get(i).getUvSets().get(entry.getKey());
                if (uvs != null) {
                    uvs.clear();
                    uvs.addAll(entry.getValue().faceToUVs(i));
                }
            }
        }
    }

    /**
     * The method computes the face's normal vector.
     * @param face
     *            the face of the mesh
     * @return face's normal vector
     */
    private Vector3f computeFaceNormal(Face face) {
        Vector3f result = new Vector3f();
        for (Integer index : face.getIndexes()) {
            result.addLocal(face.getTemporalMesh().getNormals().get(index));
        }
        result.divideLocal(face.getIndexes().size());
        return result;
    }

    /**
     * The method computes the UV coordinates of the face middle point.
     * @param face
     *            the face of the mesh
     * @return a map whose key is the name of the UV set and value is the UV coordinate of the face's middle point
     */
    private Map<String, Vector2f> computeFaceUVs(Face face) {
        Map<String, Vector2f> result = null;

        Map<String, List<Vector2f>> uvSets = face.getUvSets();
        if (uvSets != null && uvSets.size() > 0) {
            result = new HashMap<String, Vector2f>(uvSets.size());

            for (Entry<String, List<Vector2f>> entry : uvSets.entrySet()) {
                Vector2f faceUV = new Vector2f();
                for (Vector2f uv : entry.getValue()) {
                    faceUV.addLocal(uv);
                }
                faceUV.divideLocal(entry.getValue().size());
                result.put(entry.getKey(), faceUV);
            }
        }

        return result;
    }

    /**
     * The mesh interpolates the values of vertex groups weights for new vertices.
     * @param vertexGroups
     *            the vertex groups
     * @return interpolated weights of given vertex groups' weights
     */
    private Map<String, Float> interpolateVertexGroups(List<Map<String, Float>> vertexGroups) {
        Map<String, Float> weightSums = new HashMap<String, Float>();
        if (vertexGroups.size() > 0) {
            for (Map<String, Float> vGroup : vertexGroups) {
                for (Entry<String, Float> entry : vGroup.entrySet()) {
                    if (weightSums.containsKey(entry.getKey())) {
                        weightSums.put(entry.getKey(), weightSums.get(entry.getKey()) + entry.getValue());
                    } else {
                        weightSums.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        Map<String, Float> result = new HashMap<String, Float>(weightSums.size());
        for (Entry<String, Float> entry : weightSums.entrySet()) {
            result.put(entry.getKey(), entry.getValue() / vertexGroups.size());
        }

        return result;
    }

    /**
     * The method computes the vertex groups values for face's middle point.
     * @param face
     *            the face of the mesh
     * @return face's middle point interpolated vertex groups' weights
     */
    private Map<String, Float> computeFaceVertexGroups(Face face) {
        if (face.getTemporalMesh().getVertexGroups().size() > 0) {
            List<Map<String, Float>> vertexGroups = new ArrayList<Map<String, Float>>(face.getIndexes().size());
            for (Integer index : face.getIndexes()) {
                vertexGroups.add(face.getTemporalMesh().getVertexGroups().get(index));
            }
            return this.interpolateVertexGroups(vertexGroups);
        }
        return new HashMap<String, Float>();
    }

    /**
     * The method computes face's middle point vertex color.
     * @param face
     *            the face of the mesh
     * @return face's middle point vertex color
     */
    private byte[] computeFaceVertexColor(Face face) {
        if (face.getVertexColors() != null) {
            return this.interpolateVertexColors(face.getVertexColors().toArray(new byte[face.getVertexColors().size()][]));
        }
        return null;
    }

    /**
     * The method computes the average value for the given vertex colors.
     * @param colors
     *            the vertex colors
     * @return vertex colors' average value
     */
    private byte[] interpolateVertexColors(byte[]... colors) {
        TexturePixel pixel = new TexturePixel();
        TexturePixel temp = new TexturePixel();
        for (int i = 0; i < colors.length; ++i) {
            temp.fromARGB8(colors[i][3], colors[i][0], colors[i][1], colors[i][2]);
            pixel.add(temp);
        }
        pixel.divide(colors.length);
        byte[] result = new byte[4];
        pixel.toRGBA8(result);
        return result;
    }

    /**
     * The method finds an edge between the given vertices in the mesh.
     * @param temporalMesh
     *            the mesh
     * @param index1
     *            first index of the edge
     * @param index2
     *            second index of the edge
     * @return found edge or null
     */
    private Edge findEdge(TemporalMesh temporalMesh, int index1, int index2) {
        for (Edge edge : temporalMesh.getEdges()) {
            if (edge.getFirstIndex() == index1 && edge.getSecondIndex() == index2 || edge.getFirstIndex() == index2 && edge.getSecondIndex() == index1) {
                return edge;
            }
        }
        return null;
    }

    /**
     * This is a helper class for UV coordinates subdivision. UV's form a mesh that is being applied the same algorithms as a regular mesh.
     * This way one code handles two issues. After applying Catmull-Clark algorithm the UV-mesh is transformed back into UV coordinates.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class UvCoordsSubdivideTemporalMesh extends TemporalMesh {
        private static final Vector3f NORMAL = new Vector3f(0, 0, 1);

        public UvCoordsSubdivideTemporalMesh(BlenderContext blenderContext) throws BlenderFileException {
            super(null, blenderContext, false);
        }

        /**
         * Adds a UV-face to the mesh.
         * @param uvs
         *            the UV coordinates
         */
        public void addFace(List<Vector2f> uvs) {
            Integer[] indexes = new Integer[uvs.size()];
            int i = 0;

            for (Vector2f uv : uvs) {
                Vector3f v = new Vector3f(uv.x, uv.y, 0);
                int index = vertices.indexOf(v);
                if (index >= 0) {
                    indexes[i++] = index;
                } else {
                    indexes[i++] = vertices.size();
                    vertices.add(v);
                    normals.add(NORMAL);
                }
            }
            faces.add(new Face(indexes, false, 0, null, null, this));
            for (i = 1; i < indexes.length; ++i) {
                edges.add(new Edge(indexes[i - 1], indexes[i], 0, true, this));
            }
            edges.add(new Edge(indexes[indexes.length - 1], indexes[0], 0, true, this));
        }

        /**
         * Converts the mesh back into UV coordinates for the given face.
         * @param faceIndex
         *            the index of the face
         * @return UV coordinates
         */
        public List<Vector2f> faceToUVs(int faceIndex) {
            Face face = faces.get(faceIndex);
            List<Vector2f> result = new ArrayList<Vector2f>(face.getIndexes().size());
            for (Integer index : face.getIndexes()) {
                Vector3f v = vertices.get(index);
                result.add(new Vector2f(v.x, v.y));
            }
            return result;
        }
    }

    /**
     * A point computed for each vertex before applying CC subdivision and after simple subdivision.
     * This class has a target where the vertices will be drawn to with a proper strength (value from 0 to 1).
     * 
     * The algorithm of computing the target point was made by observing how blender behaves.
     * If a vertex has one or less creased edges (means edges that have non zero crease value) the target will not exist.
     * If a vertex is a border vertex and has two creased edges - the target will be the original simple subdivided vertex.
     * If a vertex is not a border vertex and have two creased edges - then it will be drawned to the plane defined by those
     * two edges.
     * If a vertex has 3 or more creased edges it will be drawn to its original vertex before CC subdivision with average strength
     * computed from edges' crease values.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class CreasePoint {
        private Vector3f target = new Vector3f();
        private float    weight;

        public CreasePoint(int index, boolean borderIndex, List<Edge> creaseEdges, TemporalMesh temporalMesh) {
            if (creaseEdges == null || creaseEdges.size() <= 1) {
                target = null;// crease is used when vertex belongs to at least 2 creased edges
            } else {
                int creasedEdgesCount = 0;
                for (Edge edge : creaseEdges) {
                    if (edge.getCrease() > 0) {
                        ++creasedEdgesCount;
                        weight += edge.getCrease();
                        target.addLocal(temporalMesh.getVertices().get(edge.getOtherIndex(index)));
                    }
                }

                if (creasedEdgesCount <= 1) {
                    target = null;// crease is used when vertex belongs to at least 2 creased edges
                } else if (creasedEdgesCount == 2) {
                    if (borderIndex) {
                        target.set(temporalMesh.getVertices().get(index));
                    } else {
                        target.divideLocal(creasedEdgesCount);
                    }
                } else {
                    target.set(temporalMesh.getVertices().get(index));
                }
                if (creasedEdgesCount > 0) {
                    weight /= creasedEdgesCount;
                }
            }
        }

        public Vector3f getTarget() {
            return target;
        }

        public float getWeight() {
            return weight;
        }
    }
}
