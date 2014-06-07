package com.jme3.scene.plugins.blender.modifiers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.BoneEnvelope;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.MeshContext;
import com.jme3.scene.plugins.blender.meshes.MeshContext.VertexGroup;
import com.jme3.util.BufferUtils;

/**
 * This modifier allows to add bone animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArmatureModifier extends Modifier {
    private static final Logger LOGGER                     = Logger.getLogger(ArmatureModifier.class.getName());
    private static final int    MAXIMUM_WEIGHTS_PER_VERTEX = 4;                                                 // JME

    private static final int    FLAG_VERTEX_GROUPS         = 0x01;
    private static final int    FLAG_BONE_ENVELOPES        = 0x02;

    private Structure           armatureObject;
    private Skeleton            skeleton;
    private Structure           meshStructure;
    /** The wold transform matrix of the armature object. */
    private Matrix4f            objectWorldMatrix;
    /** Old memory address of the mesh that will have the skeleton applied. */
    private Long                meshOMA;
    /** The variable tells if the vertex groups of the mesh should be used to assign verts to bones. */
    private boolean             useVertexGroups;
    /** The variable tells if the bones' envelopes should be used to assign verts to bones. */
    private boolean             useBoneEnvelopes;

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
                int deformflag = ((Number) modifierStructure.getFieldValue("deformflag")).intValue();
                useVertexGroups = (deformflag & FLAG_VERTEX_GROUPS) != 0;
                useBoneEnvelopes = (deformflag & FLAG_BONE_ENVELOPES) != 0;
                modifying = useBoneEnvelopes || useVertexGroups;
                if (modifying) {// if neither option is used the modifier will not modify anything anyway
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
                    this.meshStructure = meshStructure;

                    // read mesh indexes
                    meshOMA = meshStructure.getOldMemoryAddress();

                    if (useBoneEnvelopes) {
                        ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
                        Spatial object = (Spatial) blenderContext.getLoadedFeature(objectStructure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
                        objectWorldMatrix = constraintHelper.toMatrix(object.getWorldTransform(), new Matrix4f());
                    }
                }
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
            for (Geometry geom : geomList) {
                int materialIndex = meshContext.getMaterialIndex(geom);
                Mesh mesh = geom.getMesh();

                MeshWeightsData buffers = this.readVerticesWeightsData(meshContext, skeleton, materialIndex, mesh, blenderContext);
                if (buffers != null) {
                    mesh.setMaxNumWeights(buffers.maximumWeightsPerVertex);
                    mesh.setBuffer(buffers.verticesWeights);
                    mesh.setBuffer(buffers.verticesWeightsIndices);

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
            }

            AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
            animationHelper.applyAnimations(node, skeleton, blenderContext.getBlenderKey().getAnimationMatchMethod());
            node.updateModelBound();
        }
    }

    /**
     * Reads the vertices data and prepares appropriate buffers to be added to the mesh. There is a bone index buffer and weitghts buffer.
     * 
     * @param meshContext
     *            the mesh context
     * @param skeleton
     *            the current skeleton
     * @param materialIndex
     *            the material index
     * @param mesh
     *            the mesh we create the buffers for
     * @param blenderContext
     *            the blender context
     * @return an instance that aggregates all needed data for the mesh
     */
    private MeshWeightsData readVerticesWeightsData(MeshContext meshContext, Skeleton skeleton, int materialIndex, Mesh mesh, BlenderContext blenderContext) {
        int vertexListSize = meshContext.getVertexCount(materialIndex);
        Map<Integer, List<Integer>> vertexReferenceMap = meshContext.getVertexReferenceMap(materialIndex);

        Map<String, VertexGroup> vertexGroups = new HashMap<String, VertexGroup>();
        Buffer indexes = mesh.getBuffer(Type.Index).getData();
        FloatBuffer positions = mesh.getFloatBuffer(Type.Position);

        int maximumWeightsPerVertex = 0;
        if (useVertexGroups) {
            LOGGER.fine("Attaching verts to bones using vertex groups.");
            for (int boneIndex = 1; boneIndex < skeleton.getBoneCount(); ++boneIndex) {// bone with index 0 is a root bone
                Bone bone = skeleton.getBone(boneIndex);
                VertexGroup vertexGroup = meshContext.getGroup(bone.getName());
                if (vertexGroup != null) {
                    vertexGroup.setBoneIndex(boneIndex);
                    vertexGroups.put(bone.getName(), vertexGroup);
                }
            }
        }

        if (useBoneEnvelopes) {
            LOGGER.fine("Attaching verts to bones using bone envelopes.");
            Vector3f pos = new Vector3f();

            for (int boneIndex = 1; boneIndex < skeleton.getBoneCount(); ++boneIndex) {// bone with index 0 is a root bone
                Bone bone = skeleton.getBone(boneIndex);
                BoneContext boneContext = blenderContext.getBoneContext(bone);
                BoneEnvelope boneEnvelope = boneContext.getBoneEnvelope();
                if (boneEnvelope != null) {
                    VertexGroup vertexGroup = vertexGroups.get(bone.getName());
                    if (vertexGroup == null) {
                        vertexGroup = new VertexGroup();
                        vertexGroups.put(bone.getName(), vertexGroup);
                    }
                    vertexGroup.setBoneIndex(boneIndex);

                    for (Entry<Integer, List<Integer>> entry : vertexReferenceMap.entrySet()) {
                        List<Integer> vertexIndices = entry.getValue();
                        for (int j = 0; j < indexes.limit(); ++j) {
                            int index = indexes instanceof ShortBuffer ? ((ShortBuffer) indexes).get(j) : ((IntBuffer) indexes).get(j);
                            if (vertexIndices.contains(index)) {// current geometry has the index assigned to the current mesh
                                int ii = index * 3;
                                pos.set(positions.get(ii), positions.get(ii + 1), positions.get(ii + 2));
                                // move the vertex to the global space position
                                objectWorldMatrix.mult(pos, pos);// TODO: optimize: check every vertex once and apply its references
                                if (boneEnvelope.isInEnvelope(pos)) {
                                    vertexGroup.addVertex(index, boneEnvelope.getWeight());
                                } else if (boneIndex == 5) {
                                    System.out.println("Si nie zaapa: " + pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<Integer, WeightsAndBoneIndexes> weights = new HashMap<Integer, WeightsAndBoneIndexes>();// [vertex_index; [bone_index; weight]]
        if (vertexGroups.size() > 0) {
            LOGGER.fine("Gathering vertex groups information to prepare the buffers for the mesh.");
            for (VertexGroup vertexGroup : vertexGroups.values()) {
                for (Entry<Integer, Float> entry : vertexGroup.entrySet()) {
                    WeightsAndBoneIndexes vertexWeights = weights.get(entry.getKey());
                    if (vertexWeights == null) {
                        vertexWeights = new WeightsAndBoneIndexes();
                        weights.put(entry.getKey(), vertexWeights);
                    }
                    vertexWeights.put(vertexGroup.getBoneIndex(), entry.getValue());
                }
            }

            LOGGER.log(Level.FINE, "Equalizing the amount of weights per vertex to {0} if any of them has more or less.", MAXIMUM_WEIGHTS_PER_VERTEX);
            for (Entry<Integer, WeightsAndBoneIndexes> entry : weights.entrySet()) {
                maximumWeightsPerVertex = Math.max(maximumWeightsPerVertex, entry.getValue().size());
                entry.getValue().normalize(MAXIMUM_WEIGHTS_PER_VERTEX);
            }

            if (maximumWeightsPerVertex > MAXIMUM_WEIGHTS_PER_VERTEX) {
                LOGGER.log(Level.WARNING, "{0} has vertices with more than 4 weights assigned. The model may not behave as it should.", meshStructure.getName());
                maximumWeightsPerVertex = MAXIMUM_WEIGHTS_PER_VERTEX;// normalization already made at most 'MAXIMUM_WEIGHTS_PER_VERTEX' weights per vertex
            }
        }

        if(maximumWeightsPerVertex == 0) {
            LOGGER.fine("No vertex group data nor bone envelopes found to attach vertices to bones!");
            return null;
        }
        
        LOGGER.fine("Preparing buffers for the mesh.");
        FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
        ByteBuffer indicesData = BufferUtils.createByteBuffer(vertexListSize * MAXIMUM_WEIGHTS_PER_VERTEX);
        for (int i = 0; i < indexes.limit(); ++i) {
            int index = indexes instanceof ShortBuffer ? ((ShortBuffer) indexes).get(i) : ((IntBuffer) indexes).get(i);
            WeightsAndBoneIndexes weightsAndBoneIndexes = weights.get(index);
            if (weightsAndBoneIndexes != null) {
                int count = 0;
                for (Entry<Integer, Float> entry : weightsAndBoneIndexes.entrySet()) {
                    weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + count, entry.getValue());
                    indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + count, entry.getKey().byteValue());
                    ++count;
                }
            } else {
                // if no bone is assigned to this vertex then attach it to the 0-indexed root bone
                weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 1.0f);
                indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
            }
        }
        VertexBuffer verticesWeights = new VertexBuffer(Type.BoneWeight);
        verticesWeights.setupData(Usage.CpuOnly, maximumWeightsPerVertex, Format.Float, weightsFloatData);

        VertexBuffer verticesWeightsIndices = new VertexBuffer(Type.BoneIndex);
        verticesWeightsIndices.setupData(Usage.CpuOnly, maximumWeightsPerVertex, Format.UnsignedByte, indicesData);

        return new MeshWeightsData(maximumWeightsPerVertex, verticesWeights, verticesWeightsIndices);
    }

    /**
     * A class that gathers the data for mesh bone buffers.
     * Added to increase code readability.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class MeshWeightsData {
        public final int          maximumWeightsPerVertex;
        public final VertexBuffer verticesWeights;
        public final VertexBuffer verticesWeightsIndices;

        public MeshWeightsData(int maximumWeightsPerVertex, VertexBuffer verticesWeights, VertexBuffer verticesWeightsIndices) {
            this.maximumWeightsPerVertex = maximumWeightsPerVertex;
            this.verticesWeights = verticesWeights;
            this.verticesWeightsIndices = verticesWeightsIndices;
        }
    }

    /**
     * A map between the bone index and the bone's weight.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class WeightsAndBoneIndexes extends HashMap<Integer, Float> {
        private static final long serialVersionUID = 2754299007299077459L;

        /**
         * The method normalizes the weights and bone indexes data.
         * First it truncates the amount to MAXIMUM_WEIGHTS_PER_VERTEX because this is how many weights JME can handle.
         * Next it normalizes the weights so that the sum of all verts is 1.
         * @param maximumSize
         *            the maximum size that the data will be truncated to (usually: MAXIMUM_WEIGHTS_PER_VERTEX)
         */
        public void normalize(int maximumSize) {
            if (this.size() > maximumSize) {// select only the most significant weights
                float lowestWeight = Float.MAX_VALUE;
                int lowestWeightIndex = -1;
                HashMap<Integer, Float> msw = new HashMap<Integer, Float>(maximumSize);// msw = Most Significant Weight
                for (Entry<Integer, Float> entry : this.entrySet()) {
                    if (msw.size() < maximumSize) {
                        msw.put(entry.getKey(), entry.getValue());
                        if (entry.getValue() < lowestWeight) {
                            lowestWeight = entry.getValue();
                            lowestWeightIndex = entry.getKey();
                        }
                    } else if (entry.getValue() > lowestWeight) {
                        msw.remove(lowestWeightIndex);
                        msw.put(lowestWeightIndex, lowestWeight);

                        // search again for the lowest weight
                        lowestWeight = Float.MAX_VALUE;
                        for (Entry<Integer, Float> e : msw.entrySet()) {
                            if (e.getValue() < lowestWeight) {
                                lowestWeight = e.getValue();
                                lowestWeightIndex = e.getKey();
                            }
                        }
                    }
                }

                // replace current weights with the given ones
                this.clear();
                this.putAll(msw);
            }

            // normalizing the weights so that the sum of the values is equal to '1'
            float sum = 0;
            for (Entry<Integer, Float> entry : this.entrySet()) {
                sum += entry.getValue();
            }

            if (sum != 0 && sum != 1) {
                for (Entry<Integer, Float> entry : this.entrySet()) {
                    entry.setValue(entry.getValue() / sum);
                }
            }
        }
    }
}
