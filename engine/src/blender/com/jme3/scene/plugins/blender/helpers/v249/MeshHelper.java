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
package com.jme3.scene.plugins.blender.helpers.v249;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.scene.plugins.blender.utils.Properties;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshHelper extends AbstractBlenderHelper {
	protected static final int	MAXIMUM_WEIGHTS_PER_VERTEX	= 4;	// have no idea why 4, could someone please explain ?

	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
	 * versions.
	 * 
	 * @param blenderVersion
	 *            the version read from the blend file
	 */
	public MeshHelper(String blenderVersion) {
		super(blenderVersion);
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
	public List<Geometry> toMesh(Structure structure, DataRepository dataRepository) throws BlenderFileException {
		List<Geometry> geometries = (List<Geometry>) dataRepository.getLoadedFeature(structure.getOldMemoryAddress(),
				LoadedFeatureDataType.LOADED_FEATURE);
		if (geometries != null) {
			List<Geometry> copiedGeometries = new ArrayList<Geometry>(geometries.size());
			for (Geometry geometry : geometries) {
				copiedGeometries.add(geometry.clone());
			}
			return copiedGeometries;
		}

		// helpers
		TextureHelper textureHelper = dataRepository.getHelper(TextureHelper.class);

		// reading mesh data
		String name = structure.getName();

		// reading vertices
		Vector3f[] vertices = this.getVertices(structure, dataRepository);
		int verticesAmount = vertices.length;

		// vertices Colors
		List<float[]> verticesColors = this.getVerticesColors(structure, dataRepository);

		// reading faces
		// the following map sorts faces by material number (because in jme Mesh can have only one material)
		Map<Integer, List<Integer>> meshesMap = new HashMap<Integer, List<Integer>>();
		Pointer pMFace = (Pointer) structure.getFieldValue("mface");
		List<Structure> mFaces = pMFace.fetchData(dataRepository.getInputStream());

		Pointer pMTFace = (Pointer) structure.getFieldValue("mtface");
		List<Vector2f> uvCoordinates = null;
		List<Structure> mtFaces = null;

		if (pMTFace.isNotNull()) {
			mtFaces = pMTFace.fetchData(dataRepository.getInputStream());
			int facesAmount = ((Number) structure.getFieldValue("totface")).intValue();
			if (mtFaces.size() != facesAmount) {
				throw new BlenderFileException("The amount of faces uv coordinates is not equal to faces amount!");
			}
			uvCoordinates = new ArrayList<Vector2f>();// TODO: calculate the amount of coordinates if possible
		}

		// normalMap merges normals of faces that will be rendered smooth
		Map<Vector3f, Vector3f> normalMap = new HashMap<Vector3f, Vector3f>(verticesAmount);

		List<Vector3f> normalList = new ArrayList<Vector3f>();
		List<Vector3f> vertexList = new ArrayList<Vector3f>();
		// indicates if the material with the specified number should have a texture attached
		Map<Integer, Texture> materialNumberToTexture = new HashMap<Integer, Texture>();
		// this map's key is the vertex index from 'vertices 'table and the value are indices from 'vertexList'
		// positions (it simply tells which vertex is referenced where in the result list)
		Map<Integer, List<Integer>> vertexReferenceMap = new HashMap<Integer, List<Integer>>(verticesAmount);
		int vertexColorIndex = 0;
		for (int i = 0; i < mFaces.size(); ++i) {
			Structure mFace = mFaces.get(i);
			boolean smooth = (((Number) mFace.getFieldValue("flag")).byteValue() & 0x01) != 0x00;
			DynamicArray<Number> uvs = null;
			boolean materialWithoutTextures = false;
			Pointer pImage = null;
			if (mtFaces != null) {
				Structure mtFace = mtFaces.get(i);
				pImage = (Pointer) mtFace.getFieldValue("tpage");
				materialWithoutTextures = pImage.isNull();
				// uvs always must be added wheater we have texture or not
				uvs = (DynamicArray<Number>) mtFace.getFieldValue("uv");
				uvCoordinates.add(new Vector2f(uvs.get(0, 0).floatValue(), 1.0f - uvs.get(0, 1).floatValue()));
				uvCoordinates.add(new Vector2f(uvs.get(1, 0).floatValue(), 1.0f - uvs.get(1, 1).floatValue()));
				uvCoordinates.add(new Vector2f(uvs.get(2, 0).floatValue(), 1.0f - uvs.get(2, 1).floatValue()));
			}
			int matNr = ((Number) mFace.getFieldValue("mat_nr")).intValue();
			Integer materialNumber = Integer.valueOf(materialWithoutTextures ? -1 * matNr - 1 : matNr);
			List<Integer> indexList = meshesMap.get(materialNumber);
			if (indexList == null) {
				indexList = new ArrayList<Integer>();
				meshesMap.put(materialNumber, indexList);
			}
			
			// attaching image to texture (face can have UV's and image whlie its material may have no texture attached)
			if (pImage != null && pImage.isNotNull() && !materialNumberToTexture.containsKey(materialNumber)) {
				Texture texture = textureHelper.getTextureFromImage(pImage.fetchData(dataRepository.getInputStream()).get(0),
						dataRepository);
				if (texture != null) {
					materialNumberToTexture.put(materialNumber, texture);
				}
			}

			int v1 = ((Number) mFace.getFieldValue("v1")).intValue();
			int v2 = ((Number) mFace.getFieldValue("v2")).intValue();
			int v3 = ((Number) mFace.getFieldValue("v3")).intValue();
			int v4 = ((Number) mFace.getFieldValue("v4")).intValue();

			Vector3f n = FastMath.computeNormal(vertices[v1], vertices[v2], vertices[v3]);
			this.addNormal(n, normalMap, smooth, vertices[v1], vertices[v2], vertices[v3]);
			normalList.add(normalMap.get(vertices[v1]));
			normalList.add(normalMap.get(vertices[v2]));
			normalList.add(normalMap.get(vertices[v3]));

			this.appendVertexReference(v1, vertexList.size(), vertexReferenceMap);
			indexList.add(vertexList.size());
			vertexList.add(vertices[v1]);

			this.appendVertexReference(v2, vertexList.size(), vertexReferenceMap);
			indexList.add(vertexList.size());
			vertexList.add(vertices[v2]);

			this.appendVertexReference(v3, vertexList.size(), vertexReferenceMap);
			indexList.add(vertexList.size());
			vertexList.add(vertices[v3]);

			if (v4 > 0) {
				if (uvs != null) {
					uvCoordinates.add(new Vector2f(uvs.get(0, 0).floatValue(), 1.0f - uvs.get(0, 1).floatValue()));
					uvCoordinates.add(new Vector2f(uvs.get(2, 0).floatValue(), 1.0f - uvs.get(2, 1).floatValue()));
					uvCoordinates.add(new Vector2f(uvs.get(3, 0).floatValue(), 1.0f - uvs.get(3, 1).floatValue()));
				}
				this.appendVertexReference(v1, vertexList.size(), vertexReferenceMap);
				indexList.add(vertexList.size());
				vertexList.add(vertices[v1]);

				this.appendVertexReference(v3, vertexList.size(), vertexReferenceMap);
				indexList.add(vertexList.size());
				vertexList.add(vertices[v3]);

				this.appendVertexReference(v4, vertexList.size(), vertexReferenceMap);
				indexList.add(vertexList.size());
				vertexList.add(vertices[v4]);

				this.addNormal(n, normalMap, smooth, vertices[v4]);
				normalList.add(normalMap.get(vertices[v1]));
				normalList.add(normalMap.get(vertices[v3]));
				normalList.add(normalMap.get(vertices[v4]));

				if (verticesColors != null) {
					verticesColors.add(vertexColorIndex + 3, verticesColors.get(vertexColorIndex));
					verticesColors.add(vertexColorIndex + 4, verticesColors.get(vertexColorIndex + 2));
				}
				vertexColorIndex += 6;
			} else {
				if (verticesColors != null) {
					verticesColors.remove(vertexColorIndex + 3);
					vertexColorIndex += 3;
				}
			}
		}
		Vector3f[] normals = normalList.toArray(new Vector3f[normalList.size()]);

		// reading vertices groups (from the parent)
		Structure parent = dataRepository.peekParent();
		Structure defbase = (Structure) parent.getFieldValue("defbase");
		List<Structure> defs = defbase.evaluateListBase(dataRepository);
		String[] verticesGroups = new String[defs.size()];
		int defIndex = 0;
		for (Structure def : defs) {
			verticesGroups[defIndex++] = def.getFieldValue("name").toString();
		}

		// vertices bone weights and indices
		ArmatureHelper armatureHelper = dataRepository.getHelper(ArmatureHelper.class);
		Structure defBase = (Structure) parent.getFieldValue("defbase");
		Map<Integer, Integer> groupToBoneIndexMap = armatureHelper.getGroupToBoneIndexMap(defBase, dataRepository);

		VertexBuffer verticesWeights = null, verticesWeightsIndices = null;
		int[] bonesGroups = new int[] { 0 };
		VertexBuffer[] boneWeightsAndIndex = this.getBoneWeightAndIndexBuffer(structure, vertexList.size(), bonesGroups,
				vertexReferenceMap, groupToBoneIndexMap, dataRepository);
		verticesWeights = boneWeightsAndIndex[0];
		verticesWeightsIndices = boneWeightsAndIndex[1];

		// reading materials
		MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
		Material[] materials = null;
		Material[] nonTexturedMaterials = null;
		if ((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.MATERIALS) != 0) {
			materials = materialHelper.getMaterials(structure, dataRepository);
			nonTexturedMaterials = materials == null ? null : new Material[materials.length];// fill it when needed
		}

		// creating the result meshes
		geometries = new ArrayList<Geometry>(meshesMap.size());

		VertexBuffer verticesBuffer = new VertexBuffer(Type.Position);
		verticesBuffer.setupData(Usage.Stream, 3, Format.Float,
				BufferUtils.createFloatBuffer(vertexList.toArray(new Vector3f[vertexList.size()])));

		// initial vertex position (used with animation)
		VertexBuffer verticesBind = new VertexBuffer(Type.BindPosePosition);
		verticesBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(verticesBuffer.getData()));

		VertexBuffer normalsBuffer = new VertexBuffer(Type.Normal);
		normalsBuffer.setupData(Usage.Stream, 3, Format.Float, BufferUtils.createFloatBuffer(normals));

		// initial normals position (used with animation)
		VertexBuffer normalsBind = new VertexBuffer(Type.BindPoseNormal);
		normalsBind.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(normalsBuffer.getData()));

		VertexBuffer uvCoordsBuffer = null;
		if (uvCoordinates != null) {
			uvCoordsBuffer = new VertexBuffer(Type.TexCoord);
			uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float,
					BufferUtils.createFloatBuffer(uvCoordinates.toArray(new Vector2f[uvCoordinates.size()])));
		}

		//reading custom properties
		Properties properties = this.loadProperties(structure, dataRepository);
		
		// generating meshes
		FloatBuffer verticesColorsBuffer = this.createFloatBuffer(verticesColors);
		for (Entry<Integer, List<Integer>> meshEntry : meshesMap.entrySet()) {
			Mesh mesh = new Mesh();

			// creating vertices indices for this mesh
			List<Integer> indexList = meshEntry.getValue();
			short[] indices = new short[indexList.size()];//TODO: check if the model doesn't have more than 32767 vertices
			for (int i = 0; i < indexList.size(); ++i) {//if yes then mesh.getVertices method must be changed to accept other than ShortBuffer
				indices[i] = indexList.get(i).shortValue();
			}

			// setting vertices
			mesh.setBuffer(Type.Index, 1, BufferUtils.createShortBuffer(indices));
			mesh.setBuffer(verticesBuffer);
			mesh.setBuffer(verticesBind);

			// setting vertices colors
			if (verticesColorsBuffer != null) {
				mesh.setBuffer(Type.Color, 4, verticesColorsBuffer);
			}

			// setting weights for bones
			if (verticesWeights != null) {
				mesh.setMaxNumWeights(bonesGroups[0]);
				mesh.setBuffer(verticesWeights);
				mesh.setBuffer(verticesWeightsIndices);
			}

			// setting faces' normals
			mesh.setBuffer(normalsBuffer);
			mesh.setBuffer(normalsBind);

			// setting uvCoords
			if (uvCoordsBuffer != null) {
				mesh.setBuffer(uvCoordsBuffer);
			}

			// creating the result
			Geometry geometry = new Geometry(name + (geometries.size() + 1), mesh);
			if (materials != null) {
				int materialNumber = meshEntry.getKey().intValue();
				Material material;
				if (materialNumber >= 0) {
					material = materials[materialNumber];
					if (materialNumberToTexture.containsKey(Integer.valueOf(materialNumber))) {
						if (material.getMaterialDef().getAssetName().contains("Lighting")) {
							if (!materialHelper.hasTexture(material, MaterialHelper.TEXTURE_TYPE_DIFFUSE)) {
								material = material.clone();
								material.setTexture(MaterialHelper.TEXTURE_TYPE_DIFFUSE,
										materialNumberToTexture.get(Integer.valueOf(materialNumber)));
							}
						} else {
							if (!materialHelper.hasTexture(material, MaterialHelper.TEXTURE_TYPE_COLOR)) {
								material = material.clone();
								material.setTexture(MaterialHelper.TEXTURE_TYPE_COLOR,
										materialNumberToTexture.get(Integer.valueOf(materialNumber)));
							}
						}
					}
				} else {
					materialNumber = -1 * (materialNumber + 1);
					if (nonTexturedMaterials[materialNumber] == null) {
						nonTexturedMaterials[materialNumber] = materialHelper.getNonTexturedMaterial(materials[materialNumber],
								TextureHelper.TEX_IMAGE);
					}
					material = nonTexturedMaterials[materialNumber];
				}
				geometry.setMaterial(material);
			} else {
				geometry.setMaterial(dataRepository.getDefaultMaterial());
			}
			if(properties != null && properties.getValue() != null) {
				geometry.setUserData("properties", properties);
			}
			geometries.add(geometry);
		}
		dataRepository.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
		return geometries;
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
	protected void addNormal(Vector3f normalToAdd, Map<Vector3f, Vector3f> normalMap, boolean smooth, Vector3f... vertices) {
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
	protected void appendVertexReference(int basicVertexIndex, int resultIndex, Map<Integer, List<Integer>> vertexReferenceMap) {
		List<Integer> referenceList = vertexReferenceMap.get(Integer.valueOf(basicVertexIndex));
		if (referenceList == null) {
			referenceList = new ArrayList<Integer>();
			vertexReferenceMap.put(Integer.valueOf(basicVertexIndex), referenceList);
		}
		referenceList.add(Integer.valueOf(resultIndex));
	}

	/**
	 * This method returns the vertices colors. Each vertex is stored in float[4] array.
	 * 
	 * @param meshStructure
	 *            the structure containing the mesh data
	 * @param dataRepository
	 *            the data repository
	 * @return a list of vertices colors, each color belongs to a single vertex
	 * @throws BlenderFileException
	 *             this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public List<float[]> getVerticesColors(Structure meshStructure, DataRepository dataRepository) throws BlenderFileException {
		Pointer pMCol = (Pointer) meshStructure.getFieldValue("mcol");
		List<float[]> verticesColors = null;
		List<Structure> mCol = null;
		if (pMCol.isNotNull()) {
			verticesColors = new LinkedList<float[]>();
			mCol = pMCol.fetchData(dataRepository.getInputStream());
			for (Structure color : mCol) {
				float r = ((Number) color.getFieldValue("r")).byteValue() / 256.0f;
				float g = ((Number) color.getFieldValue("g")).byteValue() / 256.0f;
				float b = ((Number) color.getFieldValue("b")).byteValue() / 256.0f;
				float a = ((Number) color.getFieldValue("a")).byteValue() / 256.0f;
				verticesColors.add(new float[] { b, g, r, a });
			}
		}
		return verticesColors;
	}

	/**
	 * This method returns the vertices.
	 * 
	 * @param meshStructure
	 *            the structure containing the mesh data
	 * @param dataRepository
	 *            the data repository
	 * @return a list of vertices colors, each color belongs to a single vertex
	 * @throws BlenderFileException
	 *             this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	@SuppressWarnings("unchecked")
	public Vector3f[] getVertices(Structure meshStructure, DataRepository dataRepository) throws BlenderFileException {
		int verticesAmount = ((Number) meshStructure.getFieldValue("totvert")).intValue();
		Vector3f[] vertices = new Vector3f[verticesAmount];
		Pointer pMVert = (Pointer) meshStructure.getFieldValue("mvert");
		List<Structure> mVerts = pMVert.fetchData(dataRepository.getInputStream());
		for (int i = 0; i < verticesAmount; ++i) {
			DynamicArray<Number> coordinates = (DynamicArray<Number>) mVerts.get(i).getFieldValue("co");
			vertices[i] = new Vector3f(coordinates.get(0).floatValue(), coordinates.get(1).floatValue(), coordinates.get(2).floatValue());
		}
		return vertices;
	}

	/**
	 * This method returns an array of size 2. The first element is a vertex buffer holding bone weights for every vertex in the model. The
	 * second element is a vertex buffer holding bone indices for vertices (the indices of bones the vertices are assigned to).
	 * 
	 * @param meshStructure
	 *            the mesh structure object
	 * @param vertexListSize
	 *            a number of vertices in the model
	 * @param bonesGroups
	 *            this is an output parameter, it should be a one-sized array; the maximum amount of weights per vertex (up to
	 *            MAXIMUM_WEIGHTS_PER_VERTEX) is stored there
	 * @param vertexReferenceMap
	 *            this reference map allows to map the original vertices read from blender to vertices that are really in the model; one
	 *            vertex may appear several times in the result model
	 * @param groupToBoneIndexMap
	 *            this object maps the group index (to which a vertices in blender belong) to bone index of the model
	 * @param dataRepository
	 *            the data repository
	 * @return arrays of vertices weights and their bone indices and (as an outpot parameter) the maximum amount of weights for a vertex
	 * @throws BlenderFileException
	 *             this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public VertexBuffer[] getBoneWeightAndIndexBuffer(Structure meshStructure, int vertexListSize, int[] bonesGroups,
			Map<Integer, List<Integer>> vertexReferenceMap, Map<Integer, Integer> groupToBoneIndexMap, DataRepository dataRepository)
			throws BlenderFileException {
		Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices
		FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
		ByteBuffer indicesData = BufferUtils.createByteBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
		if (pDvert.isNotNull()) {// assigning weights and bone indices
			List<Structure> dverts = pDvert.fetchData(dataRepository.getInputStream());// dverts.size() == verticesAmount (one dvert per
																						// vertex in blender)
			int vertexIndex = 0;
			for (Structure dvert : dverts) {
				int totweight = ((Number) dvert.getFieldValue("totweight")).intValue();// total amount of weights assignet to the vertex
																						// (max. 4 in JME)
				Pointer pDW = (Pointer) dvert.getFieldValue("dw");
				List<Integer> vertexIndices = vertexReferenceMap.get(Integer.valueOf(vertexIndex));// we fetch the referenced vertices here
				if (totweight > 0 && pDW.isNotNull()) {// pDW should never be null here, but I check it just in case :)
					int weightIndex = 0;
					List<Structure> dw = pDW.fetchData(dataRepository.getInputStream());
					for (Structure deformWeight : dw) {
						Integer boneIndex = groupToBoneIndexMap.get(((Number) deformWeight.getFieldValue("def_nr")).intValue());
						if (boneIndex != null) {// null here means that we came accross group that has no bone attached to
							float weight = ((Number) deformWeight.getFieldValue("weight")).floatValue();
							if (weight == 0.0f) {
								weight = 1;
								boneIndex = Integer.valueOf(0);
							}
							// we apply the weight to all referenced vertices
							for (Integer index : vertexIndices) {
								// all indices are always assigned to 0-indexed bone
								// weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, 1.0f);
								// indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, (byte)0);
								// if(weight != 0.0f) {
								weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, weight);
								indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, boneIndex.byteValue());
								// }
							}
						}
						++weightIndex;
					}
				} else {
					for (Integer index : vertexIndices) {
						weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 1.0f);
						indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
					}
				}
				++vertexIndex;
			}
		} else {
			// always bind all vertices to 0-indexed bone
			// this bone makes the model look normally if vertices have no bone assigned
			// and it is used in object animation, so if we come accross object animation
			// we can use the 0-indexed bone for this
			for (List<Integer> vertexIndexList : vertexReferenceMap.values()) {
				// we apply the weight to all referenced vertices
				for (Integer index : vertexIndexList) {
					weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 1.0f);
					indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
				}
			}
		}

		bonesGroups[0] = this.endBoneAssigns(vertexListSize, weightsFloatData);
		VertexBuffer verticesWeights = new VertexBuffer(Type.BoneWeight);
		verticesWeights.setupData(Usage.CpuOnly, bonesGroups[0], Format.Float, weightsFloatData);

		VertexBuffer verticesWeightsIndices = new VertexBuffer(Type.BoneIndex);
		verticesWeightsIndices.setupData(Usage.CpuOnly, bonesGroups[0], Format.UnsignedByte, indicesData);
		return new VertexBuffer[] { verticesWeights, verticesWeightsIndices };
	}

	/**
	 * Normalizes weights if needed and finds largest amount of weights used for all vertices in the buffer.
	 */
	protected int endBoneAssigns(int vertCount, FloatBuffer weightsFloatData) {
		int maxWeightsPerVert = 0;
		weightsFloatData.rewind();
		for (int v = 0; v < vertCount; ++v) {
			float w0 = weightsFloatData.get(), w1 = weightsFloatData.get(), w2 = weightsFloatData.get(), w3 = weightsFloatData.get();

			if (w3 != 0) {
				maxWeightsPerVert = Math.max(maxWeightsPerVert, 4);
			} else if (w2 != 0) {
				maxWeightsPerVert = Math.max(maxWeightsPerVert, 3);
			} else if (w1 != 0) {
				maxWeightsPerVert = Math.max(maxWeightsPerVert, 2);
			} else if (w0 != 0) {
				maxWeightsPerVert = Math.max(maxWeightsPerVert, 1);
			}

			float sum = w0 + w1 + w2 + w3;
			if (sum != 1f && sum != 0.0f) {
				weightsFloatData.position(weightsFloatData.position() - 4);
				// compute new vals based on sum
				float sumToB = 1f / sum;
				weightsFloatData.put(w0 * sumToB);
				weightsFloatData.put(w1 * sumToB);
				weightsFloatData.put(w2 * sumToB);
				weightsFloatData.put(w3 * sumToB);
			}
		}
		weightsFloatData.rewind();

		// mesh.setMaxNumWeights(maxWeightsPerVert);
		return maxWeightsPerVert;
	}
}
