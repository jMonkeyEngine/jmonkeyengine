package com.jme3.scene.plugins.blender.meshes.builders;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.textures.UserUVCollection;
import com.jme3.util.BufferUtils;

/**
 * A builder class for meshes made of triangles (faces).
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class FaceMeshBuilder {
    private static final Logger                       LOGGER           = Logger.getLogger(FaceMeshBuilder.class.getName());

    /** An array of reference vertices. */
    private Vector3f[][]                              verticesAndNormals;
    /** An list of vertices colors. */
    private List<byte[]>                              verticesColors;
    /** A variable that indicates if the model uses generated textures. */
    private boolean                                   usesGeneratedTextures;
    /**
     * This map's key is the vertex index from 'vertices 'table and the value are indices from 'vertexList'
     * positions (it simply tells which vertex is referenced where in the result list).
     */
    private Map<Integer, Map<Integer, List<Integer>>> globalVertexReferenceMap;
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>>              normalMap        = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts vertices by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Vector3f>>              vertexMap        = new HashMap<Integer, List<Vector3f>>();
    /** The following map sorts vertices colors by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<byte[]>>                vertexColorsMap  = new HashMap<Integer, List<byte[]>>();
    /** The following map sorts indexes by material number (because in jme Mesh can have only one material). */
    private Map<Integer, List<Integer>>               indexMap         = new HashMap<Integer, List<Integer>>();
    /** A collection of user defined UV coordinates (one mesh can have more than one such mappings). */
    private UserUVCollection                          userUVCollection = new UserUVCollection();

    /**
     * Constructor. Stores the given array (not copying it).
     * The second argument describes if the model uses generated textures. If yes then no vertex amount optimisation is applied.
     * The amount of vertices is always faceCount * 3.
     * @param verticesAndNormals
     *            the reference vertices and normals array
     * @param usesGeneratedTextures
     *            a variable that indicates if the model uses generated textures or not
     */
    public FaceMeshBuilder(Vector3f[][] verticesAndNormals, boolean usesGeneratedTextures) {
        this.verticesAndNormals = verticesAndNormals;
        this.usesGeneratedTextures = usesGeneratedTextures;
        globalVertexReferenceMap = new HashMap<Integer, Map<Integer, List<Integer>>>(verticesAndNormals.length);
    }

    public void readMesh(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        verticesColors = this.getVerticesColors(structure, blenderContext);
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);

        if (meshHelper.isBMeshCompatible(structure)) {
            this.readBMesh(structure, blenderContext);
        } else {
            this.readTraditionalFaces(structure, blenderContext);
        }
    }

    /**
     * Builds the meshes.
     * @return a map between material index and the mesh
     */
    public Map<Integer, Mesh> buildMeshes() {
        Map<Integer, Mesh> result = new HashMap<Integer, Mesh>(indexMap.size());

        for (Entry<Integer, List<Integer>> meshEntry : indexMap.entrySet()) {
            int materialIndex = meshEntry.getKey();
            // key is the material index
            // value is a list of vertex indices
            Mesh mesh = new Mesh();

            // creating vertices indices for this mesh
            List<Integer> indexList = meshEntry.getValue();
            if (this.getVerticesAmount(materialIndex) <= Short.MAX_VALUE) {
                short[] indices = new short[indexList.size()];
                for (int i = 0; i < indexList.size(); ++i) {
                    indices[i] = indexList.get(i).shortValue();
                }
                mesh.setBuffer(Type.Index, 1, indices);
            } else {
                int[] indices = new int[indexList.size()];
                for (int i = 0; i < indexList.size(); ++i) {
                    indices[i] = indexList.get(i).intValue();
                }
                mesh.setBuffer(Type.Index, 1, indices);
            }

            LOGGER.fine("Creating vertices buffer.");
            VertexBuffer verticesBuffer = new VertexBuffer(Type.Position);
            verticesBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(this.getVertices(materialIndex)));
            mesh.setBuffer(verticesBuffer);

            LOGGER.fine("Creating normals buffer.");
            VertexBuffer normalsBuffer = new VertexBuffer(Type.Normal);
            normalsBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(this.getNormals(materialIndex)));
            mesh.setBuffer(normalsBuffer);

            if (verticesColors != null) {
                LOGGER.fine("Setting vertices colors.");
                mesh.setBuffer(Type.Color, 4, this.getVertexColorsBuffer(materialIndex));
                mesh.getBuffer(Type.Color).setNormalized(true);
            }

            result.put(materialIndex, mesh);
        }

        return result;
    }

    /**
     * This method reads the mesh from the new BMesh system.
     * 
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the
     *             blender file
     */
    private void readBMesh(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.fine("Reading BMesh.");
        Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
        Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");
        Map<String, Vector2f[]> uvCoordinatesForFace = new HashMap<String, Vector2f[]>();

        if (pMPoly.isNotNull() && pMLoop.isNotNull()) {
            Map<String, List<Vector2f>> uvs = this.loadUVCoordinates(meshStructure, true, blenderContext);
            List<Structure> polys = pMPoly.fetchData(blenderContext.getInputStream());
            List<Structure> loops = pMLoop.fetchData(blenderContext.getInputStream());
            int[] vertexColorIndex = verticesColors == null ? null : new int[3];
            for (Structure poly : polys) {
                int materialNumber = ((Number) poly.getFieldValue("mat_nr")).intValue();
                int loopStart = ((Number) poly.getFieldValue("loopstart")).intValue();
                int totLoop = ((Number) poly.getFieldValue("totloop")).intValue();
                boolean smooth = (((Number) poly.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
                int[] vertexIndexes = new int[totLoop];

                for (int i = loopStart; i < loopStart + totLoop; ++i) {
                    vertexIndexes[i - loopStart] = ((Number) loops.get(i).getFieldValue("v")).intValue();
                }

                int i = 0;
                while (i < totLoop - 2) {
                    int v1 = vertexIndexes[0];
                    int v2 = vertexIndexes[i + 1];
                    int v3 = vertexIndexes[i + 2];
                    if (vertexColorIndex != null) {
                        vertexColorIndex[0] = loopStart;
                        vertexColorIndex[1] = loopStart + i + 1;
                        vertexColorIndex[2] = loopStart + i + 2;
                    }

                    if (uvs != null) {
                        // uvs always must be added wheater we have texture or not
                        for (Entry<String, List<Vector2f>> entry : uvs.entrySet()) {
                            Vector2f[] uvCoordsForASingleFace = new Vector2f[3];
                            uvCoordsForASingleFace[0] = entry.getValue().get(loopStart);
                            uvCoordsForASingleFace[1] = entry.getValue().get(loopStart + i + 1);
                            uvCoordsForASingleFace[2] = entry.getValue().get(loopStart + i + 2);
                            uvCoordinatesForFace.put(entry.getKey(), uvCoordsForASingleFace);
                        }
                    }

                    this.appendFace(v1, v2, v3, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, vertexColorIndex);
                    uvCoordinatesForFace.clear();
                    ++i;
                }
            }
        }
    }

    /**
     * This method reads the mesh from traditional triangle/quad storing
     * structures.
     * 
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the
     *             blender file
     */
    private void readTraditionalFaces(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.fine("Reading traditional faces.");
        Pointer pMFace = (Pointer) meshStructure.getFieldValue("mface");
        List<Structure> mFaces = pMFace.isNotNull() ? pMFace.fetchData(blenderContext.getInputStream()) : null;
        if (mFaces != null && mFaces.size() > 0) {
            // indicates if the material with the specified number should have a texture attached
            Map<String, List<Vector2f>> uvs = this.loadUVCoordinates(meshStructure, false, blenderContext);
            Map<String, Vector2f[]> uvCoordinatesForFace = new HashMap<String, Vector2f[]>();
            int[] vertexColorIndex = verticesColors == null ? null : new int[3];
            for (int i = 0; i < mFaces.size(); ++i) {
                Structure mFace = mFaces.get(i);
                int materialNumber = ((Number) mFace.getFieldValue("mat_nr")).intValue();
                boolean smooth = (((Number) mFace.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
                if (uvs != null) {
                    // uvs always must be added wheater we have texture or not
                    for (Entry<String, List<Vector2f>> entry : uvs.entrySet()) {
                        Vector2f[] uvCoordsForASingleFace = new Vector2f[3];
                        uvCoordsForASingleFace[0] = entry.getValue().get(i * 4);
                        uvCoordsForASingleFace[1] = entry.getValue().get(i * 4 + 1);
                        uvCoordsForASingleFace[2] = entry.getValue().get(i * 4 + 2);
                        uvCoordinatesForFace.put(entry.getKey(), uvCoordsForASingleFace);
                    }
                }

                int v1 = ((Number) mFace.getFieldValue("v1")).intValue();
                int v2 = ((Number) mFace.getFieldValue("v2")).intValue();
                int v3 = ((Number) mFace.getFieldValue("v3")).intValue();
                int v4 = ((Number) mFace.getFieldValue("v4")).intValue();
                if (vertexColorIndex != null) {
                    vertexColorIndex[0] = i * 4;
                    vertexColorIndex[1] = i * 4 + 1;
                    vertexColorIndex[2] = i * 4 + 2;
                }

                this.appendFace(v1, v2, v3, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, vertexColorIndex);
                uvCoordinatesForFace.clear();
                if (v4 > 0) {
                    if (uvs != null) {
                        // uvs always must be added wheater we have texture or not
                        for (Entry<String, List<Vector2f>> entry : uvs.entrySet()) {
                            Vector2f[] uvCoordsForASingleFace = new Vector2f[3];
                            uvCoordsForASingleFace[0] = entry.getValue().get(i * 4);
                            uvCoordsForASingleFace[1] = entry.getValue().get(i * 4 + 2);
                            uvCoordsForASingleFace[2] = entry.getValue().get(i * 4 + 3);
                            uvCoordinatesForFace.put(entry.getKey(), uvCoordsForASingleFace);
                        }
                    }
                    if (vertexColorIndex != null) {
                        vertexColorIndex[0] = i * 4;
                        vertexColorIndex[1] = i * 4 + 2;
                        vertexColorIndex[2] = i * 4 + 3;
                    }
                    this.appendFace(v1, v3, v4, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, vertexColorIndex);
                    uvCoordinatesForFace.clear();
                }
            }
        }
    }

    /**
     * This method adds a face to the mesh.
     * @param v1
     *            index of the 1'st vertex from the reference vertex table
     * @param v2
     *            index of the 2'nd vertex from the reference vertex table
     * @param v3
     *            index of the 3'rd vertex from the reference vertex table
     * @param smooth
     *            indicates if this face should have smooth shading or flat shading
     * @param materialNumber
     *            the material number for this face
     * @param uvsForFace
     *            a 3-element array of vertices UV coordinates mapped to the UV's set name
     * @param vertexColorIndex
     *            a table of 3 elements that indicates the verts' colors indexes
     */
    private void appendFace(int v1, int v2, int v3, boolean smooth, int materialNumber, Map<String, Vector2f[]> uvsForFace, int[] vertexColorIndex) {
        if (uvsForFace != null && uvsForFace.size() > 0) {
            for (Entry<String, Vector2f[]> entry : uvsForFace.entrySet()) {
                if (entry.getValue().length != 3) {
                    throw new IllegalArgumentException("UV coordinates must be a 3-element array!" + (entry.getKey() != null ? " (UV set name: " + entry.getKey() + ')' : ""));
                }
            }
        }

        // getting the required lists
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
        if (vertexColorsList == null && vertexColorsMap != null) {
            vertexColorsList = new ArrayList<byte[]>();
            vertexColorsMap.put(materialNumber, vertexColorsList);
        }
        List<Vector3f> normalList = normalMap.get(materialNumber);
        if (normalList == null) {
            normalList = new ArrayList<Vector3f>();
            normalMap.put(materialNumber, normalList);
        }
        Map<Integer, List<Integer>> vertexReferenceMap = globalVertexReferenceMap.get(materialNumber);
        if (vertexReferenceMap == null) {
            vertexReferenceMap = new HashMap<Integer, List<Integer>>();
            globalVertexReferenceMap.put(materialNumber, vertexReferenceMap);
        }

        // creating faces
        Integer[] index = new Integer[] { v1, v2, v3 };
        if (smooth && !usesGeneratedTextures) {
            for (int i = 0; i < 3; ++i) {
                if (!vertexReferenceMap.containsKey(index[i])) {
                    // if this index is not yet used then create another face
                    this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
                    if (uvsForFace != null) {
                        for (Entry<String, Vector2f[]> entry : uvsForFace.entrySet()) {
                            userUVCollection.addUV(materialNumber, entry.getKey(), entry.getValue()[i], vertexList.size());
                        }
                    }

                    vertexList.add(verticesAndNormals[index[i]][0]);
                    if (verticesColors != null) {
                        vertexColorsList.add(verticesColors.get(vertexColorIndex[i]));
                    }
                    normalList.add(verticesAndNormals[index[i]][1]);

                    index[i] = vertexList.size() - 1;
                } else if (uvsForFace != null) {
                    // if the index is used then check if the vertexe's UV coordinates match, if yes then the vertex doesn't have separate UV's
                    // in different faces so we can use it here as well, if UV's are different in separate faces the we need to add this vert
                    // because in jme one vertex can have only on UV coordinate
                    boolean vertexAlreadyUsed = false;
                    for (Integer vertexIndex : vertexReferenceMap.get(index[i])) {
                        int vertexUseCounter = 0;
                        for (Entry<String, Vector2f[]> entry : uvsForFace.entrySet()) {
                            if (entry.getValue()[i].equals(userUVCollection.getUVForVertex(entry.getKey(), vertexIndex))) {
                                ++vertexUseCounter;
                            }
                        }
                        if (vertexUseCounter == uvsForFace.size()) {
                            vertexAlreadyUsed = true;
                            index[i] = vertexIndex;
                            break;
                        }
                    }

                    if (!vertexAlreadyUsed) {
                        // treat this face as a new one because its vertices have separate UV's
                        this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
                        for (Entry<String, Vector2f[]> entry : uvsForFace.entrySet()) {
                            userUVCollection.addUV(materialNumber, entry.getKey(), entry.getValue()[i], vertexList.size());
                        }
                        vertexList.add(verticesAndNormals[index[i]][0]);
                        if (verticesColors != null) {
                            vertexColorsList.add(verticesColors.get(vertexColorIndex[i]));
                        }
                        normalList.add(verticesAndNormals[index[i]][1]);
                        index[i] = vertexList.size() - 1;
                    }
                } else {
                    // use this index again
                    index[i] = vertexList.indexOf(verticesAndNormals[index[i]][0]);
                }
                indexList.add(index[i]);
            }
        } else {
            Vector3f n = smooth ? null : FastMath.computeNormal(verticesAndNormals[v1][0], verticesAndNormals[v2][0], verticesAndNormals[v3][0]);
            for (int i = 0; i < 3; ++i) {
                indexList.add(vertexList.size());
                this.appendVertexReference(index[i], vertexList.size(), vertexReferenceMap);
                if (uvsForFace != null) {
                    for (Entry<String, Vector2f[]> entry : uvsForFace.entrySet()) {
                        userUVCollection.addUV(materialNumber, entry.getKey(), entry.getValue()[i], vertexList.size());
                    }
                }
                vertexList.add(verticesAndNormals[index[i]][0]);
                if (verticesColors != null) {
                    vertexColorsList.add(verticesColors.get(vertexColorIndex[i]));
                }
                normalList.add(smooth ? verticesAndNormals[index[i]][1] : n);
            }
        }
    }

    /**
     * The method loads the UV coordinates. The result is a map where the key is the user's UV set name and the values are UV coordinates.
     * But depending on the mesh type (triangle/quads or bmesh) the lists in the map have different meaning.
     * For bmesh they are enlisted just like they are stored in the blend file (in loops).
     * For traditional faces every 4 UV's should be assigned for a single face.
     * @param meshStructure
     *            the mesh structure
     * @param useBMesh
     *            tells if we should load the coordinates from loops of from faces
     * @param blenderContext
     *            the blender context
     * @return a map that sorts UV coordinates between different UV sets
     * @throws BlenderFileException
     *             an exception is thrown when problems with blend file occur
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Vector2f>> loadUVCoordinates(Structure meshStructure, boolean useBMesh, BlenderContext blenderContext) throws BlenderFileException {
        Map<String, List<Vector2f>> result = new HashMap<String, List<Vector2f>>();
        if (useBMesh) {
            // in this case the UV's are assigned to vertices (an array is the same length as the vertex array)
            Structure loopData = (Structure) meshStructure.getFieldValue("ldata");
            Pointer pLoopDataLayers = (Pointer) loopData.getFieldValue("layers");
            List<Structure> loopDataLayers = pLoopDataLayers.fetchData(blenderContext.getInputStream());
            for (Structure structure : loopDataLayers) {
                Pointer p = (Pointer) structure.getFieldValue("data");
                if (p.isNotNull() && ((Number) structure.getFieldValue("type")).intValue() == MeshHelper.UV_DATA_LAYER_TYPE_BMESH) {
                    String uvSetName = structure.getFieldValue("name").toString();
                    List<Structure> uvsStructures = p.fetchData(blenderContext.getInputStream());
                    List<Vector2f> uvs = new ArrayList<Vector2f>(uvsStructures.size());
                    for (Structure uvStructure : uvsStructures) {
                        DynamicArray<Number> loopUVS = (DynamicArray<Number>) uvStructure.getFieldValue("uv");
                        uvs.add(new Vector2f(loopUVS.get(0).floatValue(), loopUVS.get(1).floatValue()));
                    }
                    result.put(uvSetName, uvs);
                }
            }
        } else {
            // in this case UV's are assigned to faces (the array has the same legnth as the faces count)
            Structure facesData = (Structure) meshStructure.getFieldValue("fdata");
            Pointer pFacesDataLayers = (Pointer) facesData.getFieldValue("layers");
            List<Structure> facesDataLayers = pFacesDataLayers.fetchData(blenderContext.getInputStream());
            for (Structure structure : facesDataLayers) {
                Pointer p = (Pointer) structure.getFieldValue("data");
                if (p.isNotNull() && ((Number) structure.getFieldValue("type")).intValue() == MeshHelper.UV_DATA_LAYER_TYPE_FMESH) {
                    String uvSetName = structure.getFieldValue("name").toString();
                    List<Structure> uvsStructures = p.fetchData(blenderContext.getInputStream());
                    List<Vector2f> uvs = new ArrayList<Vector2f>(uvsStructures.size());
                    for (Structure uvStructure : uvsStructures) {
                        DynamicArray<Number> mFaceUVs = (DynamicArray<Number>) uvStructure.getFieldValue("uv");
                        uvs.add(new Vector2f(mFaceUVs.get(0).floatValue(), mFaceUVs.get(1).floatValue()));
                        uvs.add(new Vector2f(mFaceUVs.get(2).floatValue(), mFaceUVs.get(3).floatValue()));
                        uvs.add(new Vector2f(mFaceUVs.get(4).floatValue(), mFaceUVs.get(5).floatValue()));
                        uvs.add(new Vector2f(mFaceUVs.get(6).floatValue(), mFaceUVs.get(7).floatValue()));
                    }
                    result.put(uvSetName, uvs);
                }
            }
        }
        return result;
    }

    /**
     * This method returns the vertices colors. Each vertex is stored in byte[4] array.
     * 
     * @param meshStructure
     *            the structure containing the mesh data
     * @param blenderContext
     *            the blender context
     * @return a list of vertices colors, each color belongs to a single vertex
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    private List<byte[]> getVerticesColors(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
        Pointer pMCol = (Pointer) meshStructure.getFieldValue(meshHelper.isBMeshCompatible(meshStructure) ? "mloopcol" : "mcol");
        List<byte[]> verticesColors = null;
        // it was likely a bug in blender untill version 2.63 (the blue and red factors were misplaced in their structure)
        // so we need to put them right
        boolean useBGRA = blenderContext.getBlenderVersion() < 263;
        if (pMCol.isNotNull()) {
            List<Structure> mCol = pMCol.fetchData(blenderContext.getInputStream());
            verticesColors = new ArrayList<byte[]>(mCol.size());
            for (Structure color : mCol) {
                byte r = ((Number) color.getFieldValue("r")).byteValue();
                byte g = ((Number) color.getFieldValue("g")).byteValue();
                byte b = ((Number) color.getFieldValue("b")).byteValue();
                byte a = ((Number) color.getFieldValue("a")).byteValue();
                verticesColors.add(useBGRA ? new byte[] { b, g, r, a } : new byte[] { r, g, b, a });
            }
        }
        return verticesColors;
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
    private Vector3f[] getVertices(int materialNumber) {
        return vertexMap.get(materialNumber).toArray(new Vector3f[vertexMap.get(materialNumber).size()]);
    }

    /**
     * @param materialNumber
     *            the material index
     * @return the amount of result vertices
     */
    private int getVerticesAmount(int materialNumber) {
        return vertexMap.get(materialNumber).size();
    }

    /**
     * @param materialNumber
     *            the material index
     * @return normals result array
     */
    private Vector3f[] getNormals(int materialNumber) {
        return normalMap.get(materialNumber).toArray(new Vector3f[normalMap.get(materialNumber).size()]);
    }

    /**
     * @param materialNumber
     *            the material index
     * @return the vertices colors buffer or null if no vertex colors is set
     */
    private ByteBuffer getVertexColorsBuffer(int materialNumber) {
        ByteBuffer result = null;
        if (verticesColors != null && vertexColorsMap.get(materialNumber) != null) {
            List<byte[]> data = vertexColorsMap.get(materialNumber);
            result = BufferUtils.createByteBuffer(4 * data.size());
            for (byte[] v : data) {
                if (v != null) {
                    result.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
                } else {
                    result.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
            result.flip();
        }
        return result;
    }

    /**
     * @param materialNumber
     *            the material number that is appied to the mesh
     * @return UV coordinates of vertices that belong to the required mesh part
     */
    public LinkedHashMap<String, List<Vector2f>> getUVCoordinates(int materialNumber) {
        return userUVCollection.getUVCoordinates(materialNumber);
    }

    /**
     * @return indicates if the mesh has UV coordinates
     */
    public boolean hasUVCoordinates() {
        return userUVCollection.hasUVCoordinates();
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
