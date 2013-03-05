package com.jme3.scene.plugins.blender.modifiers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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
    private static final Logger LOGGER                     = Logger.getLogger(ArmatureModifier.class.getName());
    private static final int    MAXIMUM_WEIGHTS_PER_VERTEX = 4;                                                  // JME limitation

    private Skeleton            skeleton;
    private Structure           objectStructure;
    private Structure           meshStructure;

    /** Loaded animation data. */
    private AnimData            animData;
    /** Old memory address of the mesh that will have the skeleton applied. */
    private Long                meshOMA;

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
        Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices

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
                    armatureHelper.buildBones(armatureObject.getOldMemoryAddress(), bonebase.get(i), null, bonesList, objectToArmatureTransformation, bonesPoseChannels, blenderContext);
                }
                bonesList.add(0, new Bone(""));
                Bone[] bones = bonesList.toArray(new Bone[bonesList.size()]);
                skeleton = new Skeleton(bones);
                blenderContext.setSkeleton(armatureObject.getOldMemoryAddress(), skeleton);
                this.objectStructure = objectStructure;
                this.meshStructure = meshStructure;

                // read mesh indexes
                this.meshOMA = meshStructure.getOldMemoryAddress();

                // read animations
                ArrayList<Animation> animations = new ArrayList<Animation>();
                List<FileBlockHeader> actionHeaders = blenderContext.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
                if (actionHeaders != null) {// it may happen that the model has armature with no actions
                    for (FileBlockHeader header : actionHeaders) {
                        Structure actionStructure = header.getStructure(blenderContext);
                        String actionName = actionStructure.getName();

                        BoneTrack[] tracks = armatureHelper.getTracks(actionStructure, skeleton, blenderContext);
                        if (tracks != null && tracks.length > 0) {
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
                // fetching action defined in object
                Pointer pAction = (Pointer) objectStructure.getFieldValue("action");
                if (pAction.isNotNull()) {
                    Structure actionStructure = pAction.fetchData(blenderContext.getInputStream()).get(0);
                    String actionName = actionStructure.getName();

                    BoneTrack[] tracks = armatureHelper.getTracks(actionStructure, skeleton, blenderContext);
                    if (tracks != null && tracks.length > 0) {
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

                animData = new AnimData(skeleton, animations);

                // store the animation data for each bone
                for (Bone bone : bones) {
                    Long boneOma = armatureHelper.getBoneOMA(bone);
                    if (boneOma != null) {
                        blenderContext.setAnimData(boneOma, animData);
                    }
                }
            } else {
                modifying = false;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Node apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
        }// if invalid, animData will be null
        if (animData == null || skeleton == null) {
            return node;
        }

        // setting weights for bones
        List<Geometry> geomList = (List<Geometry>) blenderContext.getLoadedFeature(meshOMA, LoadedFeatureDataType.LOADED_FEATURE);
        MeshContext meshContext = blenderContext.getMeshContext(meshOMA);
        int[] bonesGroups = new int[] { 0 };
        for (Geometry geom : geomList) {
            int materialIndex = meshContext.getMaterialIndex(geom);
            Mesh mesh = geom.getMesh();

            try {
                VertexBuffer[] buffers = this.readVerticesWeightsData(objectStructure, meshStructure, skeleton, materialIndex, bonesGroups, blenderContext);
                if (buffers != null) {
                    mesh.setMaxNumWeights(bonesGroups[0]);
                    mesh.setBuffer(buffers[0]);
                    mesh.setBuffer(buffers[1]);

                    VertexBuffer bindNormalBuffer = (meshContext.getBindNormalBuffer(materialIndex));
                    if (bindNormalBuffer != null) {
                        mesh.setBuffer(bindNormalBuffer);
                    }
                    VertexBuffer bindPoseBuffer = (meshContext.getBindPoseBuffer(materialIndex));
                    if (bindPoseBuffer != null) {
                        mesh.setBuffer(bindPoseBuffer);
                    }
                    // change the usage type of vertex and normal buffers from Static to Stream
                    mesh.getBuffer(Type.Position).setUsage(Usage.Stream);
                    mesh.getBuffer(Type.Normal).setUsage(Usage.Stream);
                }
            } catch (BlenderFileException e) {
                LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                this.invalid = true;
                return node;
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
    private VertexBuffer[] readVerticesWeightsData(Structure objectStructure, Structure meshStructure, Skeleton skeleton, int materialIndex, int[] bonesGroups, BlenderContext blenderContext) throws BlenderFileException {
        ArmatureHelper armatureHelper = blenderContext.getHelper(ArmatureHelper.class);
        Structure defBase = (Structure) objectStructure.getFieldValue("defbase");
        Map<Integer, Integer> groupToBoneIndexMap = armatureHelper.getGroupToBoneIndexMap(defBase, skeleton, blenderContext);

        MeshContext meshContext = blenderContext.getMeshContext(meshStructure.getOldMemoryAddress());

        return this.getBoneWeightAndIndexBuffer(meshStructure, meshContext.getVertexCount(materialIndex), bonesGroups, meshContext.getVertexReferenceMap(materialIndex), groupToBoneIndexMap, blenderContext);
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
    private VertexBuffer[] getBoneWeightAndIndexBuffer(Structure meshStructure, int vertexListSize, int[] bonesGroups, Map<Integer, List<Integer>> vertexReferenceMap, Map<Integer, Integer> groupToBoneIndexMap, BlenderContext blenderContext) throws BlenderFileException {
        bonesGroups[0] = 0;
        Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices
        FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
        ByteBuffer indicesData = BufferUtils.createByteBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);

        if (pDvert.isNotNull()) {// assigning weights and bone indices
            boolean warnAboutTooManyVertexWeights = false;
            List<Structure> dverts = pDvert.fetchData(blenderContext.getInputStream());// dverts.size() == verticesAmount (one dvert per vertex in blender)
            int vertexIndex = 0;
            // use tree map to sort weights from the lowest to the highest ones
            TreeMap<Float, Integer> weightToIndexMap = new TreeMap<Float, Integer>();

            for (Structure dvert : dverts) {
                List<Integer> vertexIndices = vertexReferenceMap.get(Integer.valueOf(vertexIndex));// we fetch the referenced vertices here
                if (vertexIndices != null) {
                    int totweight = ((Number) dvert.getFieldValue("totweight")).intValue();// total amount of weights assignet to the vertex (max. 4 in JME)
                    Pointer pDW = (Pointer) dvert.getFieldValue("dw");
                    if (totweight > 0 && groupToBoneIndexMap != null) {
                        weightToIndexMap.clear();
                        int weightIndex = 0;
                        List<Structure> dw = pDW.fetchData(blenderContext.getInputStream());
                        for (Structure deformWeight : dw) {
                            Integer boneIndex = groupToBoneIndexMap.get(((Number) deformWeight.getFieldValue("def_nr")).intValue());
                            float weight = ((Number) deformWeight.getFieldValue("weight")).floatValue();
                            // boneIndex == null: it here means that we came accross group that has no bone attached to, so simply ignore it
                            // if weight == 0 and weightIndex == 0 then ignore the weight (do not set weight = 0 as a first weight)
                            if (boneIndex != null && (weight > 0.0f || weightIndex > 0)) {
                                if (weightIndex < MAXIMUM_WEIGHTS_PER_VERTEX) {
                                    if (weight == 0.0f) {
                                        boneIndex = Integer.valueOf(0);
                                    }
                                    // we apply the weight to all referenced vertices
                                    for (Integer index : vertexIndices) {
                                        weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, weight);
                                        indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, boneIndex.byteValue());
                                    }
                                    weightToIndexMap.put(weight, weightIndex);
                                    bonesGroups[0] = Math.max(bonesGroups[0], weightIndex + 1);
                                } else if (weight > 0) {// if weight is zero the simply ignore it
                                    warnAboutTooManyVertexWeights = true;
                                    Entry<Float, Integer> lowestWeightAndIndex = weightToIndexMap.firstEntry();
                                    if (lowestWeightAndIndex != null && lowestWeightAndIndex.getKey() < weight) {
                                        // we apply the weight to all referenced vertices
                                        for (Integer index : vertexIndices) {
                                            weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + lowestWeightAndIndex.getValue(), weight);
                                            indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + lowestWeightAndIndex.getValue(), boneIndex.byteValue());
                                        }
                                        weightToIndexMap.remove(lowestWeightAndIndex.getKey());
                                        weightToIndexMap.put(weight, lowestWeightAndIndex.getValue());
                                    }
                                }
                                ++weightIndex;
                            }
                        }
                    } else {
                        // 0.0 weight indicates, do not transform this vertex, but keep it in bind pose.
                        for (Integer index : vertexIndices) {
                            weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 0.0f);
                            indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
                        }
                    }
                }
                ++vertexIndex;
            }

            if (warnAboutTooManyVertexWeights) {
                LOGGER.log(Level.WARNING, "{0} has vertices with more than 4 weights assigned. The model may not behave as it should.", meshStructure.getName());
            }
        } else {
            // always bind all vertices to 0-indexed bone
            // this bone makes the model look normally if vertices have no bone
            // assigned and it is used in object animation, so if we come accross object
            // animation we can use the 0-indexed bone for this
            for (List<Integer> vertexIndexList : vertexReferenceMap.values()) {
                // we apply the weight to all referenced vertices
                for (Integer index : vertexIndexList) {
                    weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 1.0f);
                    indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
                }
            }
        }

        bonesGroups[0] = Math.max(bonesGroups[0], 1);

        this.endBoneAssigns(vertexListSize, weightsFloatData);
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
    private void endBoneAssigns(int vertCount, FloatBuffer weightsFloatData) {
        weightsFloatData.rewind();
        float[] weights = new float[MAXIMUM_WEIGHTS_PER_VERTEX];
        for (int v = 0; v < vertCount; ++v) {
            float sum = 0;
            for (int i = 0; i < MAXIMUM_WEIGHTS_PER_VERTEX; ++i) {
                weights[i] = weightsFloatData.get();
                sum += weights[i];
            }
            if (sum != 1f && sum != 0.0f) {
                weightsFloatData.position(weightsFloatData.position() - MAXIMUM_WEIGHTS_PER_VERTEX);
                // compute new vals based on sum
                float sumToB = 1f / sum;
                for (int i = 0; i < MAXIMUM_WEIGHTS_PER_VERTEX; ++i) {
                    weightsFloatData.put(weights[i] * sumToB);
                }
            }
        }
        weightsFloatData.rewind();
    }

    @Override
    public String getType() {
        return Modifier.ARMATURE_MODIFIER_DATA;
    }
}
