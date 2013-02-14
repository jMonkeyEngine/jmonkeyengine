/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.objects.Properties;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER = Logger.getLogger(MeshHelper.class.getName());

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *            a variable that indicates if the Y asxis is the UP axis or not
     */
    public MeshHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
    }

    /**
     * This method reads converts the given structure into mesh. The given structure needs to be filled with the appropriate data.
     * 
     * @param structure
     *            the structure we read the mesh from
     * @return the mesh feature
     * @throws BlenderFileException
     */
    @SuppressWarnings("unchecked")
    public List<Geometry> toMesh(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(structure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (geometries != null) {
            List<Geometry> copiedGeometries = new ArrayList<Geometry>(geometries.size());
            for (Geometry geometry : geometries) {
                copiedGeometries.add(geometry.clone());
            }
            return copiedGeometries;
        }

        // reading mesh data
        String name = structure.getName();
        MeshContext meshContext = new MeshContext();

        // reading materials
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        MaterialContext[] materials = null;
        if ((blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.MATERIALS) != 0) {
            materials = materialHelper.getMaterials(structure, blenderContext);
        }

        // reading vertices and their colors
        Vector3f[][] verticesAndNormals = this.getVerticesAndNormals(structure, blenderContext);
        List<byte[]> verticesColors = this.getVerticesColors(structure, blenderContext);

        MeshBuilder meshBuilder = new MeshBuilder(verticesAndNormals, verticesColors, this.areGeneratedTexturesPresent(materials));

        if (this.isBMeshCompatible(structure)) {
            this.readBMesh(meshBuilder, structure, blenderContext);
        } else {
            this.readTraditionalFaces(meshBuilder, structure, blenderContext);
        }

        if (meshBuilder.isEmpty()) {
            geometries = new ArrayList<Geometry>(0);
            blenderContext.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
            blenderContext.setMeshContext(structure.getOldMemoryAddress(), meshContext);
            return geometries;
        }

        meshContext.setVertexReferenceMap(meshBuilder.getVertexReferenceMap());

        // reading vertices groups (from the parent)
        Structure parent = blenderContext.peekParent();
        Structure defbase = (Structure) parent.getFieldValue("defbase");
        List<Structure> defs = defbase.evaluateListBase(blenderContext);
        String[] verticesGroups = new String[defs.size()];
        int defIndex = 0;
        for (Structure def : defs) {
            verticesGroups[defIndex++] = def.getFieldValue("name").toString();
        }

        // creating the result meshes
        geometries = new ArrayList<Geometry>(meshBuilder.getMeshesPartAmount());

        // reading custom properties
        Properties properties = this.loadProperties(structure, blenderContext);

        // generating meshes
        for (Entry<Integer, List<Integer>> meshEntry : meshBuilder.getMeshesMap().entrySet()) {
            int materialIndex = meshEntry.getKey();
            // key is the material index (or -1 if the material has no texture)
            // value is a list of vertex indices
            Mesh mesh = new Mesh();

            // creating vertices indices for this mesh
            List<Integer> indexList = meshEntry.getValue();
            if (meshBuilder.getVerticesAmount(materialIndex) <= Short.MAX_VALUE) {
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

            VertexBuffer verticesBuffer = new VertexBuffer(Type.Position);
            verticesBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(meshBuilder.getVertices(materialIndex)));

            // initial vertex position (used with animation)
            VertexBuffer verticesBind = new VertexBuffer(Type.BindPosePosition);
            verticesBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.createFloatBuffer(meshBuilder.getVertices(materialIndex)));

            VertexBuffer normalsBuffer = new VertexBuffer(Type.Normal);
            normalsBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(meshBuilder.getNormals(materialIndex)));

            // initial normals position (used with animation)
            VertexBuffer normalsBind = new VertexBuffer(Type.BindPoseNormal);
            normalsBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.createFloatBuffer(meshBuilder.getNormals(materialIndex)));

            mesh.setBuffer(verticesBuffer);
            meshContext.setBindPoseBuffer(materialIndex, verticesBind);// this is stored in the context and applied when needed (when animation is applied to the mesh)

            // setting vertices colors
            if (verticesColors != null) {
                mesh.setBuffer(Type.Color, 4, meshBuilder.getVertexColorsBuffer(materialIndex));
                mesh.getBuffer(Type.Color).setNormalized(true);
            }

            // setting faces' normals
            mesh.setBuffer(normalsBuffer);
            meshContext.setBindNormalBuffer(materialIndex, normalsBind);// this is stored in the context and applied when needed (when animation is applied to the mesh)

            // creating the result
            Geometry geometry = new Geometry(name + (geometries.size() + 1), mesh);
            if (properties != null && properties.getValue() != null) {
                this.applyProperties(geometry, properties);
            }
            geometries.add(geometry);
            meshContext.putGeometry(materialIndex, geometry);
        }

        // store the data in blender context before applying the material
        blenderContext.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
        blenderContext.setMeshContext(structure.getOldMemoryAddress(), meshContext);

        // apply materials only when all geometries are in place
        if (materials != null) {
            for (Geometry geometry : geometries) {
                int materialNumber = meshContext.getMaterialIndex(geometry);
                if (materials[materialNumber] != null) {
                    List<Vector2f> uvCoordinates = meshBuilder.getUVCoordinates(materialNumber);
                    MaterialContext materialContext = materials[materialNumber];
                    materialContext.applyMaterial(geometry, structure.getOldMemoryAddress(), uvCoordinates, blenderContext);
                } else {
                    geometry.setMaterial(blenderContext.getDefaultMaterial());
                    LOGGER.warning("The importer came accross mesh that points to a null material. Default material is used to prevent loader from crashing, " + "but the model might look not the way it should. Sometimes blender does not assign materials properly. " + "Enter the edit mode and assign materials once more to your faces.");
                }
            }
        } else {
            // add UV coordinates if they are defined even if the material is not applied to the model
            VertexBuffer uvCoordsBuffer = null;
            if (meshBuilder.hasUVCoordinates()) {
                List<Vector2f> uvs = meshBuilder.getUVCoordinates(0);
                uvCoordsBuffer = new VertexBuffer(Type.TexCoord);
                uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(uvs.toArray(new Vector2f[uvs.size()])));
            }

            for (Geometry geometry : geometries) {
                geometry.setMaterial(blenderContext.getDefaultMaterial());
                if (uvCoordsBuffer != null) {
                    geometry.getMesh().setBuffer(uvCoordsBuffer);
                }
            }
        }

        return geometries;
    }

    /**
     * Tells if the given mesh structure supports BMesh.
     * 
     * @param meshStructure
     *            the mesh structure
     * @return <b>true</b> if BMesh is supported and <b>false</b> otherwise
     */
    private boolean isBMeshCompatible(Structure meshStructure) {
        Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
        Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");
        return pMLoop != null && pMPoly != null && pMLoop.isNotNull() && pMPoly.isNotNull();
    }

    /**
     * This method reads the mesh from the new BMesh system.
     * 
     * @param meshBuilder
     *            the mesh builder
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the
     *             blender file
     */
    @SuppressWarnings("unchecked")
    private void readBMesh(MeshBuilder meshBuilder, Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
        Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");
        Pointer pMEdge = (Pointer) meshStructure.getFieldValue("medge");
        Pointer pMLoopUV = (Pointer) meshStructure.getFieldValue("mloopuv");
        Vector2f[] uvCoordinatesForFace = new Vector2f[3];

        if (pMPoly.isNotNull() && pMLoop.isNotNull() && pMEdge.isNotNull()) {
            int faceIndex = 0;
            List<Structure> polys = pMPoly.fetchData(blenderContext.getInputStream());
            List<Structure> loops = pMLoop.fetchData(blenderContext.getInputStream());
            List<Structure> loopuvs = pMLoopUV.isNotNull() ? pMLoopUV.fetchData(blenderContext.getInputStream()) : null;
            for (Structure poly : polys) {
                int materialNumber = ((Number) poly.getFieldValue("mat_nr")).intValue();
                int loopStart = ((Number) poly.getFieldValue("loopstart")).intValue();
                int totLoop = ((Number) poly.getFieldValue("totloop")).intValue();
                boolean smooth = (((Number) poly.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
                int[] vertexIndexes = new int[totLoop];
                Vector2f[] uvs = loopuvs != null ? new Vector2f[totLoop] : null;

                for (int i = loopStart; i < loopStart + totLoop; ++i) {
                    vertexIndexes[i - loopStart] = ((Number) loops.get(i).getFieldValue("v")).intValue();
                    if (uvs != null) {
                        DynamicArray<Number> loopUVS = (DynamicArray<Number>) loopuvs.get(i).getFieldValue("uv");
                        uvs[i - loopStart] = new Vector2f(loopUVS.get(0).floatValue(), loopUVS.get(1).floatValue());
                    }
                }

                int i = 0;
                while (i < totLoop - 2) {
                    int v1 = vertexIndexes[0];
                    int v2 = vertexIndexes[i + 1];
                    int v3 = vertexIndexes[i + 2];

                    if (uvs != null) {// uvs always must be added wheater we
                                      // have texture or not
                        uvCoordinatesForFace[0] = uvs[0];
                        uvCoordinatesForFace[1] = uvs[i + 1];
                        uvCoordinatesForFace[2] = uvs[i + 2];
                    }

                    meshBuilder.appendFace(v1, v2, v3, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, false, faceIndex);

                    ++i;
                }
                ++faceIndex;
            }
        }
    }

    /**
     * This method reads the mesh from traditional triangle/quad storing
     * structures.
     * 
     * @param meshBuilder
     *            the mesh builder
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the
     *             blender file
     */
    @SuppressWarnings("unchecked")
    private void readTraditionalFaces(MeshBuilder meshBuilder, Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        Pointer pMFace = (Pointer) meshStructure.getFieldValue("mface");
        List<Structure> mFaces = pMFace.isNotNull() ? pMFace.fetchData(blenderContext.getInputStream()) : null;
        if (mFaces != null && mFaces.size() > 0) {
            Pointer pMTFace = (Pointer) meshStructure.getFieldValue("mtface");
            List<Structure> mtFaces = null;

            if (pMTFace.isNotNull()) {
                mtFaces = pMTFace.fetchData(blenderContext.getInputStream());
                int facesAmount = ((Number) meshStructure.getFieldValue("totface")).intValue();
                if (mtFaces.size() != facesAmount) {
                    throw new BlenderFileException("The amount of faces uv coordinates is not equal to faces amount!");
                }
            }

            // indicates if the material with the specified number should have a
            // texture attached
            Vector2f[] uvCoordinatesForFace = new Vector2f[3];
            for (int i = 0; i < mFaces.size(); ++i) {
                Structure mFace = mFaces.get(i);
                int materialNumber = ((Number) mFace.getFieldValue("mat_nr")).intValue();
                boolean smooth = (((Number) mFace.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
                DynamicArray<Number> uvs = null;

                if (mtFaces != null) {
                    Structure mtFace = mtFaces.get(i);
                    // uvs always must be added wheater we have texture or not
                    uvs = (DynamicArray<Number>) mtFace.getFieldValue("uv");
                    uvCoordinatesForFace[0] = new Vector2f(uvs.get(0, 0).floatValue(), uvs.get(0, 1).floatValue());
                    uvCoordinatesForFace[1] = new Vector2f(uvs.get(1, 0).floatValue(), uvs.get(1, 1).floatValue());
                    uvCoordinatesForFace[2] = new Vector2f(uvs.get(2, 0).floatValue(), uvs.get(2, 1).floatValue());
                }

                int v1 = ((Number) mFace.getFieldValue("v1")).intValue();
                int v2 = ((Number) mFace.getFieldValue("v2")).intValue();
                int v3 = ((Number) mFace.getFieldValue("v3")).intValue();
                int v4 = ((Number) mFace.getFieldValue("v4")).intValue();

                meshBuilder.appendFace(v1, v2, v3, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, false, i);
                if (v4 > 0) {
                    if (uvs != null) {
                        uvCoordinatesForFace[0] = new Vector2f(uvs.get(0, 0).floatValue(), uvs.get(0, 1).floatValue());
                        uvCoordinatesForFace[1] = new Vector2f(uvs.get(2, 0).floatValue(), uvs.get(2, 1).floatValue());
                        uvCoordinatesForFace[2] = new Vector2f(uvs.get(3, 0).floatValue(), uvs.get(3, 1).floatValue());
                    }
                    meshBuilder.appendFace(v1, v3, v4, smooth, materialNumber, uvs == null ? null : uvCoordinatesForFace, true, i);
                }
            }
        } else {
            Pointer pMEdge = (Pointer) meshStructure.getFieldValue("medge");
            List<Structure> mEdges = pMEdge.isNotNull() ? pMEdge.fetchData(blenderContext.getInputStream()) : null;
            if (mEdges != null && mEdges.size() > 0) {
                for (int i = 0; i < mEdges.size(); ++i) {
                    Structure mEdge = mEdges.get(i);
                    boolean smooth = (((Number) mEdge.getFieldValue("flag")).byteValue() & 0x01) != 0x00;

                    int v1 = ((Number) mEdge.getFieldValue("v1")).intValue();
                    int v2 = ((Number) mEdge.getFieldValue("v2")).intValue();

                    meshBuilder.appendEdge(v1, v2, smooth);
                }
            }
        }
    }

    /**
     * @return <b>true</b> if the material has at least one generated component and <b>false</b> otherwise
     */
    private boolean areGeneratedTexturesPresent(MaterialContext[] materials) {
        if (materials != null) {
            for (MaterialContext material : materials) {
                if (material != null && material.hasGeneratedTextures()) {
                    return true;
                }
            }
        }
        return false;
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
    public List<byte[]> getVerticesColors(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        Pointer pMCol = (Pointer) meshStructure.getFieldValue("mcol");
        List<byte[]> verticesColors = null;
        List<Structure> mCol = null;
        if (pMCol.isNotNull()) {
            verticesColors = new ArrayList<byte[]>();
            mCol = pMCol.fetchData(blenderContext.getInputStream());
            for (Structure color : mCol) {
                byte r = ((Number) color.getFieldValue("r")).byteValue();
                byte g = ((Number) color.getFieldValue("g")).byteValue();
                byte b = ((Number) color.getFieldValue("b")).byteValue();
                byte a = ((Number) color.getFieldValue("a")).byteValue();
                verticesColors.add(new byte[] { b, g, r, a });
            }
        }
        return verticesColors;
    }

    /**
     * This method returns the vertices.
     * 
     * @param meshStructure
     *            the structure containing the mesh data
     * @param blenderContext
     *            the blender context
     * @return a list of two - element arrays, the first element is the vertex and the second - its normal
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    @SuppressWarnings("unchecked")
    private Vector3f[][] getVerticesAndNormals(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        int count = ((Number) meshStructure.getFieldValue("totvert")).intValue();
        Vector3f[][] result = new Vector3f[count][2];
        if (count == 0) {
            return result;
        }

        Pointer pMVert = (Pointer) meshStructure.getFieldValue("mvert");
        List<Structure> mVerts = pMVert.fetchData(blenderContext.getInputStream());
        if (this.fixUpAxis) {
            for (int i = 0; i < count; ++i) {
                DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                result[i][0] = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(2).floatValue(), -coordinates.get(1).floatValue());

                DynamicArray<Number> normals = (DynamicArray<Number>) mVerts.get(i).getFieldValue("no");
                result[i][1] = new Vector3f(normals.get(0).shortValue() / 32767.0f, normals.get(2).shortValue() / 32767.0f, -normals.get(1).shortValue() / 32767.0f);
            }
        } else {
            for (int i = 0; i < count; ++i) {
                DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                result[i][0] = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(1).floatValue(), coordinates.get(2).floatValue());

                DynamicArray<Number> normals = (DynamicArray<Number>) mVerts.get(i).getFieldValue("no");
                result[i][1] = new Vector3f(normals.get(0).shortValue() / 32767.0f, normals.get(1).shortValue() / 32767.0f, normals.get(2).shortValue() / 32767.0f);
            }
        }
        return result;
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        return true;
    }
}
