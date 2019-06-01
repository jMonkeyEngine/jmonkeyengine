package com.jme3.scene.plugins.blender.meshes;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.meshes.Face.TriangulationWarning;
import com.jme3.scene.plugins.blender.meshes.MeshBuffers.BoneBuffersData;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.blender.objects.Properties;

/**
 * The class extends Geometry so that it can be temporalily added to the object's node.
 * Later each such node's child will be transformed into a list of geometries.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class TemporalMesh extends Geometry {
    private static final Logger        LOGGER                    = Logger.getLogger(TemporalMesh.class.getName());
    /** A minimum weight value. */
    private static final double 	   MINIMUM_BONE_WEIGHT 		 = FastMath.DBL_EPSILON;
    
    /** The blender context. */
    protected final BlenderContext     blenderContext;

    /** The mesh's structure. */
    protected final Structure          meshStructure;

    /** Loaded vertices. */
    protected List<Vector3f>           vertices                  = new ArrayList<Vector3f>();
    /** Loaded normals. */
    protected List<Vector3f>           normals                   = new ArrayList<Vector3f>();
    /** Loaded vertex groups. */
    protected List<Map<String, Float>> vertexGroups              = new ArrayList<Map<String, Float>>();
    /** Loaded vertex colors. */
    protected List<byte[]>             verticesColors            = new ArrayList<byte[]>();

    /** Materials used by the mesh. */
    protected MaterialContext[]        materials;
    /** The properties of the mesh. */
    protected Properties               properties;
    /** The bone indexes. */
    protected Map<String, Integer>     boneIndexes               = new HashMap<String, Integer>();
    /** The modifiers that should be applied after the mesh has been created. */
    protected List<Modifier>           postMeshCreationModifiers = new ArrayList<Modifier>();

    /** The faces of the mesh. */
    protected List<Face>               faces                     = new ArrayList<Face>();
    /** The edges of the mesh. */
    protected List<Edge>               edges                     = new ArrayList<Edge>();
    /** The points of the mesh. */
    protected List<Point>              points                    = new ArrayList<Point>();
    /** A map between index and faces that contain it (for faster index - face queries). */
    protected Map<Integer, Set<Face>>  indexToFaceMapping        = new HashMap<Integer, Set<Face>>();
    /** A map between index and edges that contain it (for faster index - edge queries). */
    protected Map<Integer, Set<Edge>>  indexToEdgeMapping        = new HashMap<Integer, Set<Edge>>();

    /** The bounding box of the temporal mesh. */
    protected BoundingBox              boundingBox;

    /**
     * Creates a temporal mesh based on the given mesh structure.
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when problems with file reading occur
     */
    public TemporalMesh(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        this(meshStructure, blenderContext, true);
    }

    /**
     * Creates a temporal mesh based on the given mesh structure.
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @param loadData
     *            tells if the data should be loaded from the mesh structure
     * @throws BlenderFileException
     *             an exception is thrown when problems with file reading occur
     */
    protected TemporalMesh(Structure meshStructure, BlenderContext blenderContext, boolean loadData) throws BlenderFileException {
        this.blenderContext = blenderContext;
        this.meshStructure = meshStructure;

        if (loadData) {
            name = meshStructure.getName();

            MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);

            meshHelper.loadVerticesAndNormals(meshStructure, vertices, normals);
            verticesColors = meshHelper.loadVerticesColors(meshStructure, blenderContext);
            LinkedHashMap<String, List<Vector2f>> userUVGroups = meshHelper.loadUVCoordinates(meshStructure);
            vertexGroups = meshHelper.loadVerticesGroups(meshStructure);

            faces = Face.loadAll(meshStructure, userUVGroups, verticesColors, this, blenderContext);
            edges = Edge.loadAll(meshStructure, this);
            points = Point.loadAll(meshStructure);

            this.rebuildIndexesMappings();
        }
    }

    /**
     * @return the blender context
     */
    public BlenderContext getBlenderContext() {
        return blenderContext;
    }

    /**
     * @return the vertices of the mesh
     */
    public List<Vector3f> getVertices() {
        return vertices;
    }

    /**
     * @return the normals of the mesh
     */
    public List<Vector3f> getNormals() {
        return normals;
    }

    /**
     * @return all faces
     */
    public List<Face> getFaces() {
        return faces;
    }

    /**
     * @return all edges
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * @return all points (do not mistake it with vertices)
     */
    public List<Point> getPoints() {
        return points;
    }

    /**
     * @return all vertices colors
     */
    public List<byte[]> getVerticesColors() {
        return verticesColors;
    }

    /**
     * @return all vertex groups for the vertices (each map has groups for the proper vertex)
     */
    public List<Map<String, Float>> getVertexGroups() {
        return vertexGroups;
    }

    /**
     * @return the faces that contain the given index or null if none contain it
     */
    public Collection<Face> getAdjacentFaces(Integer index) {
        return indexToFaceMapping.get(index);
    }

    /**
     * @param edge the edge of the mesh
     * @return a list of faces that contain the given edge or an empty list
     */
    public Collection<Face> getAdjacentFaces(Edge edge) {
        List<Face> result = new ArrayList<Face>(indexToFaceMapping.get(edge.getFirstIndex()));
        Set<Face> secondIndexAdjacentFaces = indexToFaceMapping.get(edge.getSecondIndex());
        if (secondIndexAdjacentFaces != null) {
            result.retainAll(indexToFaceMapping.get(edge.getSecondIndex()));
        }
        return result;
    }

    /**
     * @param index the index of the mesh
     * @return a list of edges that contain the index
     */
    public Collection<Edge> getAdjacentEdges(Integer index) {
        return indexToEdgeMapping.get(index);
    }

    /**
     * Tells if the given edge is a boundary edge. The boundary edge means that it belongs to a single
     * face or to none.
     * @param edge the edge of the mesh
     * @return <b>true</b> if the edge is a boundary one and <b>false</b> otherwise
     */
    public boolean isBoundary(Edge edge) {
        return this.getAdjacentFaces(edge).size() <= 1;
    }

    /**
     * The method tells if the given index is a boundary index. A boundary index belongs to at least
     * one boundary edge.
     * @param index
     *            the index of the mesh
     * @return <b>true</b> if the index is a boundary one and <b>false</b> otherwise
     */
    public boolean isBoundary(Integer index) {
        Collection<Edge> adjacentEdges = this.getAdjacentEdges(index);
        for (Edge edge : adjacentEdges) {
            if (this.isBoundary(edge)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TemporalMesh clone() {
        try {
            TemporalMesh result = new TemporalMesh(meshStructure, blenderContext, false);
            result.name = name;
            for (Vector3f v : vertices) {
                result.vertices.add(v.clone());
            }
            for (Vector3f n : normals) {
                result.normals.add(n.clone());
            }
            for (Map<String, Float> group : vertexGroups) {
                result.vertexGroups.add(new HashMap<String, Float>(group));
            }
            for (byte[] vertColor : verticesColors) {
                result.verticesColors.add(vertColor.clone());
            }
            result.materials = materials;
            result.properties = properties;
            result.boneIndexes.putAll(boneIndexes);
            result.postMeshCreationModifiers.addAll(postMeshCreationModifiers);
            for (Face face : faces) {
                result.faces.add(face.clone());
            }
            for (Edge edge : edges) {
                result.edges.add(edge.clone());
            }
            for (Point point : points) {
                result.points.add(point.clone());
            }
            result.rebuildIndexesMappings();
            return result;
        } catch (BlenderFileException e) {
            LOGGER.log(Level.SEVERE, "Error while cloning the temporal mesh: {0}. Returning null.", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * The method rebuilds the mappings between faces and edges. Should be called after
     * every major change of the temporal mesh done outside it.
     * <p>
     * Note: I will remove this method soon and cause the mappings to be done
     * automatically when the mesh is modified.
     */
    public void rebuildIndexesMappings() {
        indexToEdgeMapping.clear();
        indexToFaceMapping.clear();
        for (Face face : faces) {
            for (Integer index : face.getIndexes()) {
                Set<Face> faces = indexToFaceMapping.get(index);
                if (faces == null) {
                    faces = new HashSet<Face>();
                    indexToFaceMapping.put(index, faces);
                }
                faces.add(face);
            }
        }
        for (Edge edge : edges) {
            Set<Edge> edges = indexToEdgeMapping.get(edge.getFirstIndex());
            if (edges == null) {
                edges = new HashSet<Edge>();
                indexToEdgeMapping.put(edge.getFirstIndex(), edges);
            }
            edges.add(edge);
            edges = indexToEdgeMapping.get(edge.getSecondIndex());
            if (edges == null) {
                edges = new HashSet<Edge>();
                indexToEdgeMapping.put(edge.getSecondIndex(), edges);
            }
            edges.add(edge);
        }
    }

    @Override
    public void updateModelBound() {
        if (boundingBox == null) {
            boundingBox = new BoundingBox();
        }
        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        for (Vector3f v : vertices) {
            min.set(Math.min(min.x, v.x), Math.min(min.y, v.y), Math.min(min.z, v.z));
            max.set(Math.max(max.x, v.x), Math.max(max.y, v.y), Math.max(max.z, v.z));
        }
        boundingBox.setMinMax(min, max);
    }

    @Override
    public BoundingVolume getModelBound() {
        this.updateModelBound();
        return boundingBox;
    }

    @Override
    public BoundingVolume getWorldBound() {
        this.updateModelBound();
        Node parent = this.getParent();
        if (parent != null) {
            BoundingVolume bv = boundingBox.clone();
            bv.setCenter(parent.getWorldTranslation());
            return bv;
        } else {
            return boundingBox;
        }
    }

    /**
     * Triangulates the mesh.
     */
    public void triangulate() {
        Set<TriangulationWarning> warnings = new HashSet<>(TriangulationWarning.values().length - 1);
        LOGGER.fine("Triangulating temporal mesh.");
        for (Face face : faces) {
            TriangulationWarning warning = face.triangulate();
            if(warning != TriangulationWarning.NONE) {
                warnings.add(warning);
            }
        }
        
        if(warnings.size() > 0 && LOGGER.isLoggable(Level.WARNING)) {
            StringBuilder sb = new StringBuilder(512);
            sb.append("There were problems with triangulating the faces of a mesh: ").append(name);
            for(TriangulationWarning w : warnings) {
                sb.append("\n\t").append(w);
            }
            LOGGER.warning(sb.toString());
        }
    }

    /**
     * The method appends the given mesh to the current one. New faces and vertices and indexes are added.
     * @param mesh
     *            the mesh to be appended
     */
    public void append(TemporalMesh mesh) {
        if (mesh != null) {
            // we need to shift the indexes in faces, lines and points
            int shift = vertices.size();
            if (shift > 0) {
                for (Face face : mesh.faces) {
                    face.getIndexes().shiftIndexes(shift, null);
                    face.setTemporalMesh(this);
                }
                for (Edge edge : mesh.edges) {
                    edge.shiftIndexes(shift, null);
                }
                for (Point point : mesh.points) {
                    point.shiftIndexes(shift, null);
                }
            }

            faces.addAll(mesh.faces);
            edges.addAll(mesh.edges);
            points.addAll(mesh.points);

            vertices.addAll(mesh.vertices);
            normals.addAll(mesh.normals);
            vertexGroups.addAll(mesh.vertexGroups);
            verticesColors.addAll(mesh.verticesColors);
            boneIndexes.putAll(mesh.boneIndexes);

            this.rebuildIndexesMappings();
        }
    }

    /**
     * Sets the properties of the mesh.
     * @param properties
     *            the properties of the mesh
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Sets the materials of the mesh.
     * @param materials
     *            the materials of the mesh
     */
    public void setMaterials(MaterialContext[] materials) {
        this.materials = materials;
    }

    /**
     * Adds bone index to the mesh.
     * @param boneName
     *            the name of the bone
     * @param boneIndex
     *            the index of the bone
     */
    public void addBoneIndex(String boneName, Integer boneIndex) {
        boneIndexes.put(boneName, boneIndex);
    }

    /**
     * The modifier to be applied after the geometries are created.
     * @param modifier
     *            the modifier to be applied
     */
    public void applyAfterMeshCreate(Modifier modifier) {
        postMeshCreationModifiers.add(modifier);
    }

    @Override
    public int getVertexCount() {
        return vertices.size();
    }

    /**
     * Removes all vertices from the mesh.
     */
    public void clear() {
        vertices.clear();
        normals.clear();
        vertexGroups.clear();
        verticesColors.clear();
        faces.clear();
        edges.clear();
        points.clear();
        indexToEdgeMapping.clear();
        indexToFaceMapping.clear();
    }

    /**
     * The mesh builds geometries from the mesh. The result is stored in the blender context
     * under the mesh's OMA.
     */
    public void toGeometries() {
        LOGGER.log(Level.FINE, "Converting temporal mesh {0} to jme geometries.", name);
        List<Geometry> result = new ArrayList<Geometry>();
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
        Node parent = this.getParent();
        parent.detachChild(this);

        this.prepareFacesGeometry(result, meshHelper);
        this.prepareLinesGeometry(result, meshHelper);
        this.preparePointsGeometry(result, meshHelper);

        blenderContext.addLoadedFeatures(meshStructure.getOldMemoryAddress(), LoadedDataType.FEATURE, result);

        for (Geometry geometry : result) {
            parent.attachChild(geometry);
        }

        for (Modifier modifier : postMeshCreationModifiers) {
            modifier.postMeshCreationApply(parent, blenderContext);
        }
    }

    /**
     * The method creates geometries from faces.
     * @param result
     *            the list where new geometries will be appended
     * @param meshHelper
     *            the mesh helper
     */
    protected void prepareFacesGeometry(List<Geometry> result, MeshHelper meshHelper) {
        LOGGER.fine("Preparing faces geometries.");
        this.triangulate();

        Vector3f[] tempVerts = new Vector3f[3];
        Vector3f[] tempNormals = new Vector3f[3];
        byte[][] tempVertColors = new byte[3][];
        List<Map<Float, Integer>> boneBuffers = new ArrayList<Map<Float, Integer>>(3);

        LOGGER.log(Level.FINE, "Appending {0} faces to mesh buffers.", faces.size());
        Map<Integer, MeshBuffers> faceMeshes = new HashMap<Integer, MeshBuffers>();
        for (Face face : faces) {
            MeshBuffers meshBuffers = faceMeshes.get(face.getMaterialNumber());
            if (meshBuffers == null) {
                meshBuffers = new MeshBuffers(face.getMaterialNumber());
                faceMeshes.put(face.getMaterialNumber(), meshBuffers);
            }

            List<List<Integer>> triangulatedIndexes = face.getCurrentIndexes();
            List<byte[]> vertexColors = face.getVertexColors();

            for (List<Integer> indexes : triangulatedIndexes) {
                assert indexes.size() == 3 : "The mesh has not been properly triangulated!";
                
                Vector3f normal = null;
                if(!face.isSmooth()) {
                    normal = FastMath.computeNormal(vertices.get(indexes.get(0)), vertices.get(indexes.get(1)), vertices.get(indexes.get(2)));
                }
                
                boneBuffers.clear();
                for (int i = 0; i < 3; ++i) {
                    int vertIndex = indexes.get(i);
                    tempVerts[i] = vertices.get(vertIndex);
                    tempNormals[i] = normal != null ? normal : normals.get(vertIndex);
                    tempVertColors[i] = vertexColors != null ? vertexColors.get(face.getIndexes().indexOf(vertIndex)) : null;

                    if (boneIndexes.size() > 0 && vertexGroups.size() > 0) {
                        Map<Float, Integer> boneBuffersForVertex = new HashMap<Float, Integer>();
                        Map<String, Float> vertexGroupsForVertex = vertexGroups.get(vertIndex);
                        for (Entry<String, Integer> entry : boneIndexes.entrySet()) {
                            if (vertexGroupsForVertex.containsKey(entry.getKey())) {
                                float weight = vertexGroupsForVertex.get(entry.getKey());
                                if (weight > MINIMUM_BONE_WEIGHT) {
                                	// only values of weight greater than MINIMUM_BONE_WEIGHT are used
                                	// if all non zero weights were used, and they were samm enough, problems with normalisation would occur
                                	// because adding a very small value to 1.0 will give 1.0
                                	// so in order to avoid such errors, which can cause severe animation artifacts we need to use some minimum weight value
                                    boneBuffersForVertex.put(weight, entry.getValue());
                                }
                            }
                        }
                        if (boneBuffersForVertex.size() == 0) {// attach the vertex to zero-indexed bone so that it does not collapse to (0, 0, 0)
                            boneBuffersForVertex.put(1.0f, 0);
                        }
                        boneBuffers.add(boneBuffersForVertex);
                    }
                }

                Map<String, List<Vector2f>> uvs = meshHelper.selectUVSubset(face, indexes.toArray(new Integer[indexes.size()]));
                meshBuffers.append(face.isSmooth(), tempVerts, tempNormals, uvs, tempVertColors, boneBuffers);
            }
        }

        LOGGER.fine("Converting mesh buffers to geometries.");
        Map<Geometry, MeshBuffers> geometryToBuffersMap = new HashMap<Geometry, MeshBuffers>();
        for (Entry<Integer, MeshBuffers> entry : faceMeshes.entrySet()) {
            MeshBuffers meshBuffers = entry.getValue();

            Mesh mesh = new Mesh();

            if (meshBuffers.isShortIndexBuffer()) {
                mesh.setBuffer(Type.Index, 1, (ShortBuffer) meshBuffers.getIndexBuffer());
            } else {
                mesh.setBuffer(Type.Index, 1, (IntBuffer) meshBuffers.getIndexBuffer());
            }
            mesh.setBuffer(meshBuffers.getPositionsBuffer());
            mesh.setBuffer(meshBuffers.getNormalsBuffer());
            if (meshBuffers.areVertexColorsUsed()) {
                mesh.setBuffer(Type.Color, 4, meshBuffers.getVertexColorsBuffer());
                mesh.getBuffer(Type.Color).setNormalized(true);
            }

            BoneBuffersData boneBuffersData = meshBuffers.getBoneBuffers();
            if (boneBuffersData != null) {
                mesh.setMaxNumWeights(boneBuffersData.maximumWeightsPerVertex);
                mesh.setBuffer(boneBuffersData.verticesWeights);
                mesh.setBuffer(boneBuffersData.verticesWeightsIndices);

                LOGGER.fine("Generating bind pose and normal buffers.");
                mesh.generateBindPose(true);

                // change the usage type of vertex and normal buffers from Static to Stream
                mesh.getBuffer(Type.Position).setUsage(Usage.Stream);
                mesh.getBuffer(Type.Normal).setUsage(Usage.Stream);

                // creating empty buffers for HW skinning; the buffers will be setup if ever used
                VertexBuffer verticesWeightsHW = new VertexBuffer(Type.HWBoneWeight);
                VertexBuffer verticesWeightsIndicesHW = new VertexBuffer(Type.HWBoneIndex);
                mesh.setBuffer(verticesWeightsHW);
                mesh.setBuffer(verticesWeightsIndicesHW);
            }

            Geometry geometry = new Geometry(name + (result.size() + 1), mesh);
            if (properties != null && properties.getValue() != null) {
                meshHelper.applyProperties(geometry, properties);
            }
            result.add(geometry);

            geometryToBuffersMap.put(geometry, meshBuffers);
        }

        LOGGER.fine("Applying materials to geometries.");
        for (Entry<Geometry, MeshBuffers> entry : geometryToBuffersMap.entrySet()) {
            int materialIndex = entry.getValue().getMaterialIndex();
            Geometry geometry = entry.getKey();
            if (materialIndex >= 0 && materials != null && materials.length > materialIndex && materials[materialIndex] != null) {
                materials[materialIndex].applyMaterial(geometry, meshStructure.getOldMemoryAddress(), entry.getValue().getUvCoords(), blenderContext);
            } else {
                Material defaultMaterial = blenderContext.getDefaultMaterial().clone();
                defaultMaterial.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
                geometry.setMaterial(defaultMaterial);
            }
        }
    }

    /**
     * The method creates geometries from lines.
     * @param result
     *            the list where new geometries will be appended
     * @param meshHelper
     *            the mesh helper
     */
    protected void prepareLinesGeometry(List<Geometry> result, MeshHelper meshHelper) {
        if (edges.size() > 0) {
            LOGGER.fine("Preparing lines geometries.");

            List<List<Integer>> separateEdges = new ArrayList<List<Integer>>();
            List<Edge> edges = new ArrayList<Edge>(this.edges.size());
            for (Edge edge : this.edges) {
                if (!edge.isInFace()) {
                    edges.add(edge);
                }
            }
            while (edges.size() > 0) {
                boolean edgeAppended = false;
                int edgeIndex = 0;
                for (List<Integer> list : separateEdges) {
                    for (edgeIndex = 0; edgeIndex < edges.size() && !edgeAppended; ++edgeIndex) {
                        Edge edge = edges.get(edgeIndex);
                        if (list.get(0).equals(edge.getFirstIndex())) {
                            list.add(0, edge.getSecondIndex());
                            --edgeIndex;
                            edgeAppended = true;
                        } else if (list.get(0).equals(edge.getSecondIndex())) {
                            list.add(0, edge.getFirstIndex());
                            --edgeIndex;
                            edgeAppended = true;
                        } else if (list.get(list.size() - 1).equals(edge.getFirstIndex())) {
                            list.add(edge.getSecondIndex());
                            --edgeIndex;
                            edgeAppended = true;
                        } else if (list.get(list.size() - 1).equals(edge.getSecondIndex())) {
                            list.add(edge.getFirstIndex());
                            --edgeIndex;
                            edgeAppended = true;
                        }
                    }
                    if (edgeAppended) {
                        break;
                    }
                }
                Edge edge = edges.remove(edgeAppended ? edgeIndex : 0);
                if (!edgeAppended) {
                    separateEdges.add(new ArrayList<Integer>(Arrays.asList(edge.getFirstIndex(), edge.getSecondIndex())));
                }
            }

            for (List<Integer> list : separateEdges) {
                MeshBuffers meshBuffers = new MeshBuffers(0);
                for (int index : list) {
                    meshBuffers.append(vertices.get(index), normals.get(index));
                }
                Mesh mesh = new Mesh();
                mesh.setLineWidth(blenderContext.getBlenderKey().getLinesWidth());
                mesh.setMode(Mode.LineStrip);
                if (meshBuffers.isShortIndexBuffer()) {
                    mesh.setBuffer(Type.Index, 1, (ShortBuffer) meshBuffers.getIndexBuffer());
                } else {
                    mesh.setBuffer(Type.Index, 1, (IntBuffer) meshBuffers.getIndexBuffer());
                }
                mesh.setBuffer(meshBuffers.getPositionsBuffer());
                mesh.setBuffer(meshBuffers.getNormalsBuffer());

                Geometry geometry = new Geometry(meshStructure.getName() + (result.size() + 1), mesh);
                geometry.setMaterial(meshHelper.getBlackUnshadedMaterial(blenderContext));
                if (properties != null && properties.getValue() != null) {
                    meshHelper.applyProperties(geometry, properties);
                }
                result.add(geometry);
            }
        }
    }

    /**
     * The method creates geometries from points.
     * @param result
     *            the list where new geometries will be appended
     * @param meshHelper
     *            the mesh helper
     */
    protected void preparePointsGeometry(List<Geometry> result, MeshHelper meshHelper) {
        if (points.size() > 0) {
            LOGGER.fine("Preparing point geometries.");

            MeshBuffers pointBuffers = new MeshBuffers(0);
            for (Point point : points) {
                pointBuffers.append(vertices.get(point.getIndex()), normals.get(point.getIndex()));
            }
            Mesh pointsMesh = new Mesh();
            pointsMesh.setMode(Mode.Points);
            pointsMesh.setPointSize(blenderContext.getBlenderKey().getPointsSize());
            if (pointBuffers.isShortIndexBuffer()) {
                pointsMesh.setBuffer(Type.Index, 1, (ShortBuffer) pointBuffers.getIndexBuffer());
            } else {
                pointsMesh.setBuffer(Type.Index, 1, (IntBuffer) pointBuffers.getIndexBuffer());
            }
            pointsMesh.setBuffer(pointBuffers.getPositionsBuffer());
            pointsMesh.setBuffer(pointBuffers.getNormalsBuffer());

            Geometry pointsGeometry = new Geometry(meshStructure.getName() + (result.size() + 1), pointsMesh);
            pointsGeometry.setMaterial(meshHelper.getBlackUnshadedMaterial(blenderContext));
            if (properties != null && properties.getValue() != null) {
                meshHelper.applyProperties(pointsGeometry, properties);
            }
            result.add(pointsGeometry);
        }
    }

    @Override
    public String toString() {
        return "TemporalMesh [name=" + name + ", vertices.size()=" + vertices.size() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (meshStructure == null ? 0 : meshStructure.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TemporalMesh)) {
            return false;
        }
        TemporalMesh other = (TemporalMesh) obj;
        return meshStructure.getOldMemoryAddress().equals(other.meshStructure.getOldMemoryAddress());
    }
}
