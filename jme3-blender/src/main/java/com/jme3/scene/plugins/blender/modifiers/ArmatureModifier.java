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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.MeshContext;
import com.jme3.util.BufferUtils;

/**
 * This modifier allows to add bone animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArmatureModifier extends Modifier {
    private static final Logger LOGGER                     = Logger.getLogger(ArmatureModifier.class.getName());
    private static final int    MAXIMUM_WEIGHTS_PER_VERTEX = 4;                                                 // JME

    private Structure           armatureObject;
    private Skeleton            skeleton;
    private Structure           objectStructure;
    private Structure           meshStructure;

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
        Structure meshStructure = ((Pointer) objectStructure.getFieldValue("data")).fetchData().get(0);
        if (this.validate(modifierStructure, blenderContext)) {
            Pointer pArmatureObject = (Pointer) modifierStructure.getFieldValue("object");
            if (pArmatureObject.isNotNull()) {
                armatureObject = pArmatureObject.fetchData().get(0);

                // load skeleton
                Structure armatureStructure = ((Pointer) armatureObject.getFieldValue("data")).fetchData().get(0);
                List<Structure> bonebase = ((Structure) armatureStructure.getFieldValue("bonebase")).evaluateListBase();
                List<Bone> bonesList = new ArrayList<Bone>();
                for (int i = 0; i < bonebase.size(); ++i) {
                    this.buildBones(armatureObject.getOldMemoryAddress(), bonebase.get(i), null, bonesList, objectStructure.getOldMemoryAddress(), blenderContext);
                }
                bonesList.add(0, new Bone(""));
                Bone[] bones = bonesList.toArray(new Bone[bonesList.size()]);
                skeleton = new Skeleton(bones);
                blenderContext.setSkeleton(armatureObject.getOldMemoryAddress(), skeleton);
                this.objectStructure = objectStructure;
                this.meshStructure = meshStructure;

                // read mesh indexes
                meshOMA = meshStructure.getOldMemoryAddress();
            } else {
                modifying = false;
            }
        }
    }

    /**
     * This method builds the object's bones structure.
     * 
     * @param armatureObjectOMA
     *            the OMa of the armature node
     * @param boneStructure
     *            the structure containing the bones' data
     * @param parent
     *            the parent bone
     * @param result
     *            the list where the newly created bone will be added
     * @param spatialOMA
     *            the OMA of the spatial that will own the skeleton
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there is problem with the blender
     *             file
     */
    private void buildBones(Long armatureObjectOMA, Structure boneStructure, Bone parent, List<Bone> result, Long spatialOMA, BlenderContext blenderContext) throws BlenderFileException {
        BoneContext bc = new BoneContext(armatureObjectOMA, boneStructure, blenderContext);
        bc.buildBone(result, spatialOMA, blenderContext);
    }

    /**
     * This method returns a map where the key is the object's group index that
     * is used by a bone and the key is the bone index in the armature.
     * 
     * @param defBaseStructure
     *            a bPose structure of the object
     * @return bone group-to-index map
     * @throws BlenderFileException
     *             this exception is thrown when the blender file is somehow
     *             corrupted
     */
    public Map<Integer, Integer> getGroupToBoneIndexMap(Structure defBaseStructure, Skeleton skeleton) throws BlenderFileException {
        Map<Integer, Integer> result = null;
        if (skeleton.getBoneCount() != 0) {
            result = new HashMap<Integer, Integer>();
            List<Structure> deformGroups = defBaseStructure.evaluateListBase();// bDeformGroup
            int groupIndex = 0;
            for (Structure deformGroup : deformGroups) {
                String deformGroupName = deformGroup.getFieldValue("name").toString();
                int boneIndex = skeleton.getBoneIndex(deformGroupName);
                if (boneIndex >= 0) {
                    result.put(groupIndex, boneIndex);
                }
                ++groupIndex;
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
        }// if invalid, animData will be null
        if (skeleton != null) {
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

                        LOGGER.fine("Generating bind pose and normal buffers.");
                        mesh.generateBindPose(true);

                        // change the usage type of vertex and normal buffers from
                        // Static to Stream
                        mesh.getBuffer(Type.Position).setUsage(Usage.Stream);
                        mesh.getBuffer(Type.Normal).setUsage(Usage.Stream);

                        // creating empty buffers for HW skinning
                        // the buffers will be setup if ever used.
                        VertexBuffer verticesWeightsHW = new VertexBuffer(Type.HWBoneWeight);
                        VertexBuffer verticesWeightsIndicesHW = new VertexBuffer(Type.HWBoneIndex);
                        mesh.setBuffer(verticesWeightsHW);
                        mesh.setBuffer(verticesWeightsIndicesHW);
                    }
                } catch (BlenderFileException e) {
                    LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                    invalid = true;
                }
            }
            
            AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
            animationHelper.applyAnimations(node, skeleton, blenderContext.getBlenderKey().getSkeletonAnimationNames(node.getName()));
            node.updateModelBound();
        }
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
        Structure defBase = (Structure) objectStructure.getFieldValue("defbase");
        Map<Integer, Integer> groupToBoneIndexMap = this.getGroupToBoneIndexMap(defBase, skeleton);

        MeshContext meshContext = blenderContext.getMeshContext(meshStructure.getOldMemoryAddress());

        return this.getBoneWeightAndIndexBuffer(meshStructure, meshContext.getVertexCount(materialIndex), bonesGroups, meshContext.getVertexReferenceMap(materialIndex), groupToBoneIndexMap);
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
     * @return arrays of vertices weights and their bone indices and (as an
     *         output parameter) the maximum amount of weights for a vertex
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is
     *             somehow invalid or corrupted
     */
    private VertexBuffer[] getBoneWeightAndIndexBuffer(Structure meshStructure, int vertexListSize, int[] bonesGroups, Map<Integer, List<Integer>> vertexReferenceMap, Map<Integer, Integer> groupToBoneIndexMap) throws BlenderFileException {
        bonesGroups[0] = 0;
        Pointer pDvert = (Pointer) meshStructure.getFieldValue("dvert");// dvert = DeformVERTices
        FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
        ByteBuffer indicesData = BufferUtils.createByteBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);

        if (pDvert.isNotNull()) {// assigning weights and bone indices
            boolean warnAboutTooManyVertexWeights = false;
            // dverts.size() = verticesAmount (one dvert per vertex in blender)
            List<Structure> dverts = pDvert.fetchData();
            int vertexIndex = 0;
            // use tree map to sort weights from the lowest to the highest ones
            TreeMap<Float, Integer> weightToIndexMap = new TreeMap<Float, Integer>();

            for (Structure dvert : dverts) {
                // we fetch the referenced vertices here
                List<Integer> vertexIndices = vertexReferenceMap.get(Integer.valueOf(vertexIndex));
                if (vertexIndices != null) {
                    // total amount of wights assigned to the vertex (max. 4 in JME)
                    int totweight = ((Number) dvert.getFieldValue("totweight")).intValue();
                    Pointer pDW = (Pointer) dvert.getFieldValue("dw");
                    if (totweight > 0 && groupToBoneIndexMap != null) {
                        weightToIndexMap.clear();
                        int weightIndex = 0;
                        List<Structure> dw = pDW.fetchData();
                        for (Structure deformWeight : dw) {
                            Integer boneIndex = groupToBoneIndexMap.get(((Number) deformWeight.getFieldValue("def_nr")).intValue());
                            float weight = ((Number) deformWeight.getFieldValue("weight")).floatValue();
                            // boneIndex == null: it here means that we came
                            // accross group that has no bone attached to, so
                            // simply ignore it
                            // if weight == 0 and weightIndex == 0 then ignore
                            // the weight (do not set weight = 0 as a first
                            // weight)
                            if (boneIndex != null && (weight > 0.0f || weightIndex > 0)) {
                                if (weightIndex < MAXIMUM_WEIGHTS_PER_VERTEX) {
                                    if (weight == 0.0f) {
                                        boneIndex = Integer.valueOf(0);
                                    }
                                    // we apply the weight to all referenced
                                    // vertices
                                    for (Integer index : vertexIndices) {
                                        weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, weight);
                                        indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + weightIndex, boneIndex.byteValue());
                                    }
                                    weightToIndexMap.put(weight, weightIndex);
                                    bonesGroups[0] = Math.max(bonesGroups[0], weightIndex + 1);
                                } else if (weight > 0) {// if weight is zero the
                                                        // simply ignore it
                                    warnAboutTooManyVertexWeights = true;
                                    Entry<Float, Integer> lowestWeightAndIndex = weightToIndexMap.firstEntry();
                                    if (lowestWeightAndIndex != null && lowestWeightAndIndex.getKey() < weight) {
                                        // we apply the weight to all referenced
                                        // vertices
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
                        // 0.0 weight indicates, do not transform this vertex,
                        // but keep it in bind pose.
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
            // assigned and it is used in object animation, so if we come
            // accross object
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
    
//    This method is now not used because it broke animations.
//    Perhaps in the future I will find a solution to this problem.
//    I store it here for future use.
//    
//    private void loadBonePoses() {
//        TempVars tempVars = TempVars.get();
//        try {
//            Pointer pPose = (Pointer) armatureObject.getFieldValue("pose");
//            if (pPose.isNotNull()) {
//                LOGGER.fine("Loading the pose of the armature.");
//                ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
//                ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
//
//                Structure pose = pPose.fetchData().get(0);
//                Structure chanbase = (Structure) pose.getFieldValue("chanbase");
//                List<Structure> chans = chanbase.evaluateListBase();
//                Transform transform = new Transform();
//                for (Structure poseChannel : chans) {
//                    Pointer pBone = (Pointer) poseChannel.getFieldValue("bone");
//                    if (pBone.isNull()) {
//                        throw new BlenderFileException("Cannot find bone for pose channel named: " + poseChannel.getName());
//                    }
//                    BoneContext boneContext = blenderContext.getBoneContext(pBone.getOldMemoryAddress());
//
//                    LOGGER.log(Level.FINEST, "Getting the global pose transformation for bone: {0}", boneContext);
//                    Matrix4f poseMat = objectHelper.getMatrix(poseChannel, "pose_mat", blenderContext.getBlenderKey().isFixUpAxis());
//                    poseMat.multLocal(BoneContext.BONE_ARMATURE_TRANSFORMATION_MATRIX);
//
//                    Matrix4f armatureWorldMat = objectHelper.getMatrix(armatureObject, "obmat", blenderContext.getBlenderKey().isFixUpAxis());
//                    Matrix4f boneWorldMat = armatureWorldMat.multLocal(poseMat);
//
//                    boneWorldMat.toTranslationVector(tempVars.vect1);
//                    boneWorldMat.toRotationQuat(tempVars.quat1);
//                    boneWorldMat.toScaleVector(tempVars.vect2);
//                    transform.setTranslation(tempVars.vect1);
//                    transform.setRotation(tempVars.quat1);
//                    transform.setScale(tempVars.vect2);
//
//                    constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD, transform);
//                }
//            }
//        } catch (BlenderFileException e) {
//            LOGGER.log(Level.WARNING, "Problems occured during pose loading: {0}.", e.getLocalizedMessage());
//        } finally {
//            tempVars.release();
//        }
//    }
}
