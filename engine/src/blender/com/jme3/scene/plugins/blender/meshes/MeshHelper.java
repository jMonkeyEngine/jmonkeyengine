/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
     */
    public MeshHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion,fixUpAxis);
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
        Vector3f[] vertices = this.getVertices(structure, blenderContext);
        List<byte[]> verticesColors = this.getVerticesColors(structure, blenderContext);
        
        MeshBuilder meshBuilder = new MeshBuilder(vertices, verticesColors, this.areGeneratedTexturesPresent(materials));

        Pointer pMFace = (Pointer) structure.getFieldValue("mface");
        List<Structure> mFaces = null;
        if (pMFace.isNotNull()) {
            mFaces = pMFace.fetchData(blenderContext.getInputStream());
            if (mFaces == null || mFaces.size() == 0) {
                return new ArrayList<Geometry>(0);
            }
        } else{
        	mFaces = new ArrayList<Structure>(0);
        }

        Pointer pMTFace = (Pointer) structure.getFieldValue("mtface");
        List<Structure> mtFaces = null;

        if (pMTFace.isNotNull()) {
            mtFaces = pMTFace.fetchData(blenderContext.getInputStream());
            int facesAmount = ((Number) structure.getFieldValue("totface")).intValue();
            if (mtFaces.size() != facesAmount) {
                throw new BlenderFileException("The amount of faces uv coordinates is not equal to faces amount!");
            }
        }

        // indicates if the material with the specified number should have a texture attached
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

        //reading custom properties
        Properties properties = this.loadProperties(structure, blenderContext);

        // generating meshes
        for (Entry<Integer, List<Integer>> meshEntry : meshBuilder.getMeshesMap().entrySet()) {
        	int materialIndex = meshEntry.getKey();
        	//key is the material index (or -1 if the material has no texture)
        	//value is a list of vertex indices
            Mesh mesh = new Mesh();
            
            // creating vertices indices for this mesh
            List<Integer> indexList = meshEntry.getValue();
            if(meshBuilder.getVerticesAmount(materialIndex) <= Short.MAX_VALUE) {
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
            verticesBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(verticesBuffer.getData()));

            VertexBuffer normalsBuffer = new VertexBuffer(Type.Normal);
            normalsBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(meshBuilder.getNormals(materialIndex)));

            // initial normals position (used with animation)
            VertexBuffer normalsBind = new VertexBuffer(Type.BindPoseNormal);
            normalsBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(normalsBuffer.getData()));
            
            mesh.setBuffer(verticesBuffer);
            meshContext.setBindPoseBuffer(verticesBind);//this is stored in the context and applied when needed (when animation is applied to the mesh)

            // setting vertices colors
            if (verticesColors != null) {
                mesh.setBuffer(Type.Color, 4, meshBuilder.getVertexColorsBuffer(materialIndex));
                mesh.getBuffer(Type.Color).setNormalized(true);
            }

            // setting faces' normals
            mesh.setBuffer(normalsBuffer);
            meshContext.setBindNormalBuffer(normalsBind);//this is stored in the context and applied when needed (when animation is applied to the mesh)

            // creating the result
            Geometry geometry = new Geometry(name + (geometries.size() + 1), mesh);
            if (properties != null && properties.getValue() != null) {
                geometry.setUserData("properties", properties);
            }
            geometries.add(geometry);
            meshContext.putGeometry(materialIndex, geometry);
        }
        
        //store the data in blender context before applying the material
        blenderContext.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
        blenderContext.setMeshContext(structure.getOldMemoryAddress(), meshContext);
        
        //apply materials only when all geometries are in place
        if(materials != null) {
        	for(Geometry geometry : geometries) {
        		int materialNumber = meshContext.getMaterialIndex(geometry);
                if(materials[materialNumber] != null) {
                	List<Vector2f> uvCoordinates = meshBuilder.getUVCoordinates(materialNumber);
	                MaterialContext materialContext = materials[materialNumber];
	                materialContext.applyMaterial(geometry, structure.getOldMemoryAddress(), uvCoordinates, blenderContext);
                }
        	}
        } else {
        	//add UV coordinates if they are defined even if the material is not applied to the model
        	VertexBuffer uvCoordsBuffer = null;
        	if(meshBuilder.hasUVCoordinates()) {
	        	List<Vector2f> uvs = meshBuilder.getUVCoordinates(0);
	        	uvCoordsBuffer = new VertexBuffer(Type.TexCoord);
	            uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(uvs.toArray(new Vector2f[uvs.size()])));
        	}
			
        	for(Geometry geometry : geometries) {
        		geometry.setMaterial(blenderContext.getDefaultMaterial());
        		if(uvCoordsBuffer != null) {
        			geometry.getMesh().setBuffer(uvCoordsBuffer);
        		}
        	}
        }
        
        return geometries;
    }

    /**
	 * @return <b>true</b> if the material has at least one generated component and <b>false</b> otherwise
	 */
    private boolean areGeneratedTexturesPresent(MaterialContext[] materials) {
    	if(materials != null) {
    		for(MaterialContext material : materials) {
    			if(material != null && material.hasGeneratedTextures()) {
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
                byte r = ((Number)color.getFieldValue("r")).byteValue();
                byte g = ((Number)color.getFieldValue("g")).byteValue();
                byte b = ((Number)color.getFieldValue("b")).byteValue();
                byte a = ((Number)color.getFieldValue("a")).byteValue();
                verticesColors.add(new byte[]{b, g, r, a});
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
     * @return a list of vertices colors, each color belongs to a single vertex
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    @SuppressWarnings("unchecked")
    private Vector3f[] getVertices(Structure meshStructure, BlenderContext blenderContext) throws BlenderFileException {
        int verticesAmount = ((Number) meshStructure.getFieldValue("totvert")).intValue();
        Vector3f[] vertices = new Vector3f[verticesAmount];
        if (verticesAmount == 0) {
            return vertices;
        }

        Pointer pMVert = (Pointer) meshStructure.getFieldValue("mvert");
        List<Structure> mVerts = pMVert.fetchData(blenderContext.getInputStream());
        if(this.fixUpAxis) {
        	for (int i = 0; i < verticesAmount; ++i) {
                DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                vertices[i] = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(2).floatValue(), -coordinates.get(1).floatValue());
            }
        } else {
        	for (int i = 0; i < verticesAmount; ++i) {
                DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
                vertices[i] = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(1).floatValue(), coordinates.get(2).floatValue());
            }
        }
        return vertices;
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        return true;
    }
}
