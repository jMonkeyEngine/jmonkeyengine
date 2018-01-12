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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.objects.Properties;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER                   = Logger.getLogger(MeshHelper.class.getName());

    /** A type of UV data layer in traditional faced mesh (triangles or quads). */
    public static final int     UV_DATA_LAYER_TYPE_FMESH = 5;
    /** A type of UV data layer in bmesh type. */
    public static final int     UV_DATA_LAYER_TYPE_BMESH = 16;

    /** A material used for single lines and points. */
    private Material            blackUnshadedMaterial;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public MeshHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * Converts the mesh structure into temporal mesh.
     * The temporal mesh is stored in blender context and here always a clone is being returned because the mesh might
     * be modified by modifiers.
     * 
     * @param meshStructure
     *            the mesh structure
     * @param blenderContext
     *            the blender context
     * @return temporal mesh read from the given structure
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading blend file occur
     */
    public TemporalMesh toTemporalMesh(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading temporal mesh named: {0}.", meshStructure.getName());
        TemporalMesh temporalMesh = (TemporalMesh) blenderContext.getLoadedFeature(meshStructure.getOldMemoryAddress(), LoadedDataType.TEMPORAL_MESH);
        if (temporalMesh != null) {
            LOGGER.fine("The mesh is already loaded. Returning its clone.");
            return temporalMesh.clone();
        }

        if ("ID".equals(meshStructure.getType())) {
            LOGGER.fine("Loading mesh from external blend file.");
            return (TemporalMesh) this.loadLibrary(meshStructure);
        }

        String name = meshStructure.getName();
        LOGGER.log(Level.FINE, "Reading mesh: {0}.", name);
        temporalMesh = new TemporalMesh(meshStructure, blenderContext);

        LOGGER.fine("Loading materials.");
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        temporalMesh.setMaterials(materialHelper.getMaterials(meshStructure, blenderContext));

        LOGGER.fine("Reading custom properties.");
        Properties properties = this.loadProperties(meshStructure, blenderContext);
        temporalMesh.setProperties(properties);

        blenderContext.addLoadedFeatures(meshStructure.getOldMemoryAddress(), LoadedDataType.STRUCTURE, meshStructure);
        blenderContext.addLoadedFeatures(meshStructure.getOldMemoryAddress(), LoadedDataType.TEMPORAL_MESH, temporalMesh);
        return temporalMesh.clone();
    }

    /**
     * Tells if the given mesh structure supports BMesh.
     * 
     * @param meshStructure
     *            the mesh structure
     * @return <b>true</b> if BMesh is supported and <b>false</b> otherwise
     */
    public boolean isBMeshCompatible(Structure meshStructure) {
        Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
        Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");
        return pMLoop != null && pMPoly != null && pMLoop.isNotNull() && pMPoly.isNotNull();
    }

    /**
     * This method returns the vertices.
     * 
     * @param meshStructure
     *            the structure containing the mesh data
     * @return a list of two - element arrays, the first element is the vertex and the second - its normal
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    @SuppressWarnings("unchecked")
    public void loadVerticesAndNormals(Structure meshStructure, List<Vector3f> vertices, List<Vector3f> normals) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading vertices and normals from mesh: {0}.", meshStructure.getName());
        int count = ((Number) meshStructure.getFieldValue("totvert")).intValue();
        if (count > 0) {
            Pointer pMVert = (Pointer) meshStructure.getFieldValue("mvert");
            List<Structure> mVerts = pMVert.fetchData();
            Vector3f co = null, no = null;
            if (fixUpAxis) {
                for (int i = 0; i < count; ++i) {
                    DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                    co = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(2).floatValue(), -coordinates.get(1).floatValue());
                    vertices.add(co);

                    DynamicArray<Number> norm = (DynamicArray<Number>) mVerts.get(i).getFieldValue("no");
                    no = new Vector3f(norm.get(0).shortValue() / 32767.0f, norm.get(2).shortValue() / 32767.0f, -norm.get(1).shortValue() / 32767.0f);
                    normals.add(no);
                }
            } else {
                for (int i = 0; i < count; ++i) {
                    DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                    co = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(1).floatValue(), coordinates.get(2).floatValue());
                    vertices.add(co);

                    DynamicArray<Number> norm = (DynamicArray<Number>) mVerts.get(i).getFieldValue("no");
                    no = new Vector3f(norm.get(0).shortValue() / 32767.0f, norm.get(1).shortValue() / 32767.0f, norm.get(2).shortValue() / 32767.0f);
                    normals.add(no);
                }
            }
        }
        LOGGER.log(Level.FINE, "Loaded {0} vertices and normals.", vertices.size());
    }

    /**
     * This method returns the vertices colors. Each vertex is stored in byte[4] array.
     * 
     * @param meshStructure
     *            the structure containing the mesh data
     * @param blenderContext
     *            the blender context
     * @return a list of vertices colors, each color belongs to a single vertex or empty list of colors are not specified
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    public List<byte[]> loadVerticesColors(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading vertices colors from mesh: {0}.", meshStructure.getName());
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
        Pointer pMCol = (Pointer) meshStructure.getFieldValue(meshHelper.isBMeshCompatible(meshStructure) ? "mloopcol" : "mcol");
        List<byte[]> verticesColors = new ArrayList<byte[]>();
        // it was likely a bug in blender untill version 2.63 (the blue and red factors were misplaced in their structure)
        // so we need to put them right
        boolean useBGRA = blenderContext.getBlenderVersion() < 263;
        if (pMCol.isNotNull()) {
            List<Structure> mCol = pMCol.fetchData();
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
     * The method loads the UV coordinates. The result is a map where the key is the user's UV set name and the values are UV coordinates.
     * But depending on the mesh type (triangle/quads or bmesh) the lists in the map have different meaning.
     * For bmesh they are enlisted just like they are stored in the blend file (in loops).
     * For traditional faces every 4 UV's should be assigned for a single face.
     * @param meshStructure
     *            the mesh structure
     * @return a map that sorts UV coordinates between different UV sets
     * @throws BlenderFileException
     *             an exception is thrown when problems with blend file occur
     */
    @SuppressWarnings("unchecked")
    public LinkedHashMap<String, List<Vector2f>> loadUVCoordinates(Structure meshStructure) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading UV coordinates from mesh: {0}.", meshStructure.getName());
        LinkedHashMap<String, List<Vector2f>> result = new LinkedHashMap<String, List<Vector2f>>();
        if (this.isBMeshCompatible(meshStructure)) {
            // in this case the UV's are assigned to vertices (an array is the same length as the vertex array)
            Structure loopData = (Structure) meshStructure.getFieldValue("ldata");
            Pointer pLoopDataLayers = (Pointer) loopData.getFieldValue("layers");
            List<Structure> loopDataLayers = pLoopDataLayers.fetchData();
            for (Structure structure : loopDataLayers) {
                Pointer p = (Pointer) structure.getFieldValue("data");
                if (p.isNotNull() && ((Number) structure.getFieldValue("type")).intValue() == MeshHelper.UV_DATA_LAYER_TYPE_BMESH) {
                    String uvSetName = structure.getFieldValue("name").toString();
                    List<Structure> uvsStructures = p.fetchData();
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
            if (pFacesDataLayers.isNotNull()) {
                List<Structure> facesDataLayers = pFacesDataLayers.fetchData();
                for (Structure structure : facesDataLayers) {
                    Pointer p = (Pointer) structure.getFieldValue("data");
                    if (p.isNotNull() && ((Number) structure.getFieldValue("type")).intValue() == MeshHelper.UV_DATA_LAYER_TYPE_FMESH) {
                        String uvSetName = structure.getFieldValue("name").toString();
                        List<Structure> uvsStructures = p.fetchData();
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
        }
        return result;
    }

    /**
     * Loads all vertices groups.
     * @param meshStructure
     *            the mesh structure
     * @return a list of vertex groups for every vertex in the mesh
     * @throws BlenderFileException
     *             an exception is thrown when problems with blend file occur
     */
    public List<Map<String, Float>> loadVerticesGroups(Structure meshStructure) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading vertices groups from mesh: {0}.", meshStructure.getName());
        List<Map<String, Float>> result = new ArrayList<Map<String, Float>>();

        Structure parent = blenderContext.peekParent();
        if(parent != null) {
        	// the mesh might be saved without its parent (it is then unused)
        	Structure defbase = (Structure) parent.getFieldValue("defbase");
            List<String> groupNames = new ArrayList<String>();
            List<Structure> defs = defbase.evaluateListBase();
            
            if(!defs.isEmpty()) {
                for (Structure def : defs) {
                    groupNames.add(def.getFieldValue("name").toString());
                }

                Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices
                if (pDvert.isNotNull()) {// assigning weights and bone indices
                    List<Structure> dverts = pDvert.fetchData();
                    for (Structure dvert : dverts) {
                        Map<String, Float> weightsForVertex = new HashMap<String, Float>();
                        Pointer pDW = (Pointer) dvert.getFieldValue("dw");
                        if (pDW.isNotNull()) {
                            List<Structure> dw = pDW.fetchData();
                            for (Structure deformWeight : dw) {
                                int groupIndex = ((Number) deformWeight.getFieldValue("def_nr")).intValue();
                                float weight = ((Number) deformWeight.getFieldValue("weight")).floatValue();
                                String groupName = groupNames.get(groupIndex);

                                weightsForVertex.put(groupName, weight);
                            }
                        }
                        result.add(weightsForVertex);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Selects the proper subsets of UV coordinates for the given sublist of indexes.
     * @param face
     *            the face with the original UV sets
     * @param indexesSublist
     *            the sub list of indexes
     * @return a map of UV coordinates subsets
     */
    public Map<String, List<Vector2f>> selectUVSubset(Face face, Integer... indexesSublist) {
        Map<String, List<Vector2f>> result = null;
        if (face.getUvSets() != null) {
            result = new HashMap<String, List<Vector2f>>();
            for (Entry<String, List<Vector2f>> entry : face.getUvSets().entrySet()) {
                List<Vector2f> uvs = new ArrayList<Vector2f>(indexesSublist.length);
                for (Integer index : indexesSublist) {
                    uvs.add(entry.getValue().get(face.getIndexes().indexOf(index)));
                }
                result.put(entry.getKey(), uvs);
            }
        }
        return result;
    }

    /**
     * Selects the proper subsets of vertex colors for the given sublist of indexes.
     * @param face
     *            the face with the original vertex colors
     * @param indexesSublist
     *            the sub list of indexes
     * @return a sublist of vertex colors
     */
    public List<byte[]> selectVertexColorSubset(Face face, Integer... indexesSublist) {
        List<byte[]> result = null;
        List<byte[]> vertexColors = face.getVertexColors();
        if (vertexColors != null) {
            result = new ArrayList<byte[]>(indexesSublist.length);
            for (Integer index : indexesSublist) {
                result.add(vertexColors.get(face.getIndexes().indexOf(index)));
            }
        }
        return result;
    }

    /**
     * Returns the black unshaded material. It is used for lines and points because that is how blender
     * renders it.
     * @param blenderContext
     *            the blender context
     * @return black unshaded material
     */
    public synchronized Material getBlackUnshadedMaterial(BlenderContext blenderContext) {
        if (blackUnshadedMaterial == null) {
            blackUnshadedMaterial = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            blackUnshadedMaterial.setColor("Color", ColorRGBA.Black);
        }
        return blackUnshadedMaterial;
    }
}
