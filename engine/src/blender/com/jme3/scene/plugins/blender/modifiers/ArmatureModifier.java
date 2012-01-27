package com.jme3.scene.plugins.blender.modifiers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.constraints.Constraint;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.MeshContext;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.ogre.AnimData;
import com.jme3.util.BufferUtils;

/**
 * This modifier allows to add bone animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArmatureModifier extends Modifier {
	private static final Logger	LOGGER						= Logger.getLogger(ArmatureModifier.class.getName());
	private static final int	MAXIMUM_WEIGHTS_PER_VERTEX	= 4;
	// @Marcin it was an Ogre limitation, but as long as we use a MaxNumWeight
	// variable in mesh,
	// i guess this limitation has no sense for the blender loader...so i guess
	// it's up to you. You'll have to deternine the max weight according to the
	// provided blend file
	// I added a check to avoid crash when loading a model that has more than 4
	// weight per vertex on line 258
	// If you decide to remove this limitation, remove this code.
	// Rémy

	/** Loaded animation data. */
	private AnimData			animData;
	/** Old memory address of the mesh that will have the skeleton applied. */
	private Long				meshOMA;
	/**
	 * The maxiumum amount of bone groups applied to a single vertex (max =
	 * MAXIMUM_WEIGHTS_PER_VERTEX).
	 */
	private int					boneGroups;
	/** The weights of vertices. */
	private VertexBuffer		verticesWeights;
	/** The indexes of bones applied to vertices. */
	private VertexBuffer		verticesWeightsIndices;

	/**
	 * This constructor reads animation data from the object structore. The
	 * stored data is the AnimData and additional data is armature's OMA.
	 * 
	 * @param objectStructure
	 *            the structure of the object
	 * @param modifierStructure
	 *            the structure of the modifier
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ArmatureModifier(Structure objectStructure, Structure modifierStructure, BlenderContext blenderContext) throws BlenderFileException {
		Structure meshStructure = ((Pointer) objectStructure.getFieldValue("data")).fetchData(blenderContext.getInputStream()).get(0);
		Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert
																		// =
																		// DeformVERTices

		// if pDvert==null then there are not vertex groups and no need to load
		// skeleton (untill bone envelopes are supported)
		if (this.validate(modifierStructure, blenderContext) && pDvert.isNotNull()) {
			Pointer pArmatureObject = (Pointer) modifierStructure.getFieldValue("object");
			if (pArmatureObject.isNotNull()) {
				ArmatureHelper armatureHelper = blenderContext.getHelper(ArmatureHelper.class);

				Structure armatureObject = pArmatureObject.fetchData(blenderContext.getInputStream()).get(0);

				// load skeleton
				Structure armatureStructure = ((Pointer) armatureObject.getFieldValue("data")).fetchData(blenderContext.getInputStream()).get(0);

				Structure pose = ((Pointer) armatureObject.getFieldValue("pose")).fetchData(blenderContext.getInputStream()).get(0);
				List<Structure> chanbase = ((Structure) pose.getFieldValue("chanbase")).evaluateListBase(blenderContext);

				Map<Long, Structure> bonesPoseChannels = new HashMap<Long, Structure>(chanbase.size());
				for (Structure poseChannel : chanbase) {
					Pointer pBone = (Pointer) poseChannel.getFieldValue("bone");
					bonesPoseChannels.put(pBone.getOldMemoryAddress(), poseChannel);
				}

				ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
				Matrix4f armatureObjectMatrix = objectHelper.getMatrix(armatureObject, "obmat", true);
				Matrix4f inverseMeshObjectMatrix = objectHelper.getMatrix(objectStructure, "obmat", true).invertLocal();
				Matrix4f objectToArmatureTransformation = armatureObjectMatrix.multLocal(inverseMeshObjectMatrix);

				List<Structure> bonebase = ((Structure) armatureStructure.getFieldValue("bonebase")).evaluateListBase(blenderContext);
				List<Bone> bonesList = new ArrayList<Bone>();
				for (int i = 0; i < bonebase.size(); ++i) {
					armatureHelper.buildBones(bonebase.get(i), null, bonesList, objectToArmatureTransformation, bonesPoseChannels, blenderContext);
				}
				bonesList.add(0, new Bone(""));
				Bone[] bones = bonesList.toArray(new Bone[bonesList.size()]);
				Skeleton skeleton = new Skeleton(bones);

				// read mesh indexes
				this.meshOMA = meshStructure.getOldMemoryAddress();
				this.readVerticesWeightsData(objectStructure, meshStructure, skeleton, blenderContext);

				// read animations
				ArrayList<Animation> animations = new ArrayList<Animation>();
				List<FileBlockHeader> actionHeaders = blenderContext.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
				if (actionHeaders != null) {// it may happen that the model has
											// armature with no actions
					for (FileBlockHeader header : actionHeaders) {
						Structure actionStructure = header.getStructure(blenderContext);
						String actionName = actionStructure.getName();

						BoneTrack[] tracks = armatureHelper.getTracks(actionStructure, skeleton, blenderContext);
						if(tracks != null && tracks.length > 0) {
							// determining the animation time
							float maximumTrackLength = 0;
							for (BoneTrack track : tracks) {
								float length = track.getLength();
								if (length > maximumTrackLength) {
									maximumTrackLength = length;
								}
							}

							Animation boneAnimation = new Animation(actionName, maximumTrackLength);
							boneAnimation.setTracks(tracks);
							animations.add(boneAnimation);
						}
					}
				}
				animData = new AnimData(skeleton, animations);

				// store the animation data for each bone
				for (Bone bone : bones) {
					Long boneOma = armatureHelper.getBoneOMA(bone);
					if (boneOma != null) {
						blenderContext.setAnimData(boneOma, animData);
					}
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Node apply(Node node, BlenderContext blenderContext) {
		if (invalid) {
			LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
		}// if invalid, animData will be null
		if (animData == null) {
			return node;
		}

		// setting weights for bones
		List<Geometry> geomList = (List<Geometry>) blenderContext.getLoadedFeature(this.meshOMA, LoadedFeatureDataType.LOADED_FEATURE);
		for (Geometry geom : geomList) {
			Mesh mesh = geom.getMesh();
			if (this.verticesWeights != null) {
				mesh.setMaxNumWeights(this.boneGroups);
				mesh.setBuffer(this.verticesWeights);
				mesh.setBuffer(this.verticesWeightsIndices);
			}
		}

		// applying constraints to Bones
		ArmatureHelper armatureHelper = blenderContext.getHelper(ArmatureHelper.class);
		for (int i = 0; i < animData.skeleton.getBoneCount(); ++i) {
			Long boneOMA = armatureHelper.getBoneOMA(animData.skeleton.getBone(i));
			List<Constraint> constraints = blenderContext.getConstraints(boneOMA);
			if (constraints != null && constraints.size() > 0) {
				for (Constraint constraint : constraints) {
					constraint.bake();
				}
			}
		}

		// applying animations
		AnimControl control = new AnimControl(animData.skeleton);
		ArrayList<Animation> animList = animData.anims;
		if (animList != null && animList.size() > 0) {
			HashMap<String, Animation> anims = new HashMap<String, Animation>(animList.size());
			for (int i = 0; i < animList.size(); ++i) {
				Animation animation = animList.get(i);
				anims.put(animation.getName(), animation);
			}
			control.setAnimations(anims);
		}
		node.addControl(control);
		node.addControl(new SkeletonControl(animData.skeleton));

		return node;
	}

	/**
	 * This method reads mesh indexes
	 * 
	 * @param objectStructure
	 *            structure of the object that has the armature modifier applied
	 * @param meshStructure
	 *            the structure of the object's mesh
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blend file structure is
	 *             somehow invalid or corrupted
	 */
	private void readVerticesWeightsData(Structure objectStructure, Structure meshStructure, Skeleton skeleton, BlenderContext blenderContext) throws BlenderFileException {
		ArmatureHelper armatureHelper = blenderContext.getHelper(ArmatureHelper.class);
		Structure defBase = (Structure) objectStructure.getFieldValue("defbase");
		Map<Integer, Integer> groupToBoneIndexMap = armatureHelper.getGroupToBoneIndexMap(defBase, skeleton, blenderContext);

		int[] bonesGroups = new int[] { 0 };
		MeshContext meshContext = blenderContext.getMeshContext(meshStructure.getOldMemoryAddress());

		VertexBuffer[] boneWeightsAndIndex = this.getBoneWeightAndIndexBuffer(meshStructure, meshContext.getVertexList().size(), bonesGroups, meshContext.getVertexReferenceMap(), groupToBoneIndexMap, blenderContext);
		this.verticesWeights = boneWeightsAndIndex[0];
		this.verticesWeightsIndices = boneWeightsAndIndex[1];
		this.boneGroups = bonesGroups[0];
	}

	/**
	 * This method returns an array of size 2. The first element is a vertex
	 * buffer holding bone weights for every vertex in the model. The second
	 * element is a vertex buffer holding bone indices for vertices (the indices
	 * of bones the vertices are assigned to).
	 * 
	 * @param meshStructure
	 *            the mesh structure object
	 * @param vertexListSize
	 *            a number of vertices in the model
	 * @param bonesGroups
	 *            this is an output parameter, it should be a one-sized array;
	 *            the maximum amount of weights per vertex (up to
	 *            MAXIMUM_WEIGHTS_PER_VERTEX) is stored there
	 * @param vertexReferenceMap
	 *            this reference map allows to map the original vertices read
	 *            from blender to vertices that are really in the model; one
	 *            vertex may appear several times in the result model
	 * @param groupToBoneIndexMap
	 *            this object maps the group index (to which a vertices in
	 *            blender belong) to bone index of the model
	 * @param blenderContext
	 *            the blender context
	 * @return arrays of vertices weights and their bone indices and (as an
	 *         output parameter) the maximum amount of weights for a vertex
	 * @throws BlenderFileException
	 *             this exception is thrown when the blend file structure is
	 *             somehow invalid or corrupted
	 */
	private VertexBuffer[] getBoneWeightAndIndexBuffer(Structure meshStructure, int vertexListSize, int[] bonesGroups, Map<Integer, List<Integer>> vertexReferenceMap, Map<Integer, Integer> groupToBoneIndexMap, BlenderContext blenderContext)
			throws BlenderFileException {
		Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices
		FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
		ByteBuffer indicesData = BufferUtils.createByteBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
		if (pDvert.isNotNull()) {// assigning weights and bone indices
			List<Structure> dverts = pDvert.fetchData(blenderContext.getInputStream());// dverts.size() == verticesAmount (one dvert per
																						// vertex in blender)
			int vertexIndex = 0;
			for (Structure dvert : dverts) {
				int totweight = ((Number) dvert.getFieldValue("totweight")).intValue();// total amount of weights assignet to the vertex
																						// (max. 4 in JME)
				Pointer pDW = (Pointer) dvert.getFieldValue("dw");
				List<Integer> vertexIndices = vertexReferenceMap.get(Integer.valueOf(vertexIndex));// we fetch the referenced vertices here
				if (totweight > 0 && pDW.isNotNull() && groupToBoneIndexMap!=null) {// pDW should never be null here, but I check it just in case :)
					int weightIndex = 0;
					List<Structure> dw = pDW.fetchData(blenderContext.getInputStream());
					for (Structure deformWeight : dw) {
						Integer boneIndex = groupToBoneIndexMap.get(((Number) deformWeight.getFieldValue("def_nr")).intValue());

						// Remove this code if 4 weights limitation is removed
						if (weightIndex == 4) {
							LOGGER.log(Level.WARNING, "{0} has more than 4 weight on bone index {1}", new Object[] { meshStructure.getName(), boneIndex });
							break;
						}

						// null here means that we came accross group that has no bone attached to
						if (boneIndex != null) {
							float weight = ((Number) deformWeight.getFieldValue("weight")).floatValue();
							if (weight == 0.0f) {
								weight = 1;
								boneIndex = Integer.valueOf(0);
							}
							// we apply the weight to all referenced vertices
							for (Integer index : vertexIndices) {
								weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, weight);
								indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, boneIndex.byteValue());
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
			// this bone makes the model look normally if vertices have no bone
			// assigned
			// and it is used in object animation, so if we come accross object
			// animation
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
	 * Normalizes weights if needed and finds largest amount of weights used for
	 * all vertices in the buffer.
	 * 
	 * @param vertCount
	 *            amount of vertices
	 * @param weightsFloatData
	 *            weights for vertices
	 */
	private int endBoneAssigns(int vertCount, FloatBuffer weightsFloatData) {
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
		return maxWeightsPerVert;
	}

	@Override
	public String getType() {
		return Modifier.ARMATURE_MODIFIER_DATA;
	}
}
