/*
 * Copyright (c) 2018-2021 jMonkeyEngine
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
package com.jme3.bullet.animation;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.export.InputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Utility methods used by DynamicAnimControl and associated classes.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class RagUtils {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RagUtils.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RagUtils() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Assign each mesh vertex to a bone/torso link and add its location (mesh
     * coordinates in bind pose) to that link's list.
     *
     * @param meshes array of animated meshes to use (not null, unaffected)
     * @param managerMap a map from bone indices to managing link names (not
     * null, unaffected)
     * @return a new map from bone/torso names to sets of vertex coordinates
     */
    static Map<String, VectorSet> coordsMap(Mesh[] meshes,
            String[] managerMap) {
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Vector3f bindPosition = new Vector3f();
        Map<String, VectorSet> coordsMap = new HashMap<>(32);
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; ++vertexI) {
                String managerName = findManager(mesh, vertexI, iArray, wArray,
                        managerMap);
                VectorSet set = coordsMap.get(managerName);
                if (set == null) {
                    set = new VectorSet(1);
                    coordsMap.put(managerName, set);
                }
                vertexVector3f(mesh, VertexBuffer.Type.BindPosePosition,
                        vertexI, bindPosition);
                set.add(bindPosition);
            }
        }

        return coordsMap;
    }

    /**
     * Find an animated geometry in the specified subtree of the scene graph.
     * Note: recursive!
     *
     * @param subtree where to search (not null, unaffected)
     * @return a pre-existing instance, or null if none
     */
    static Geometry findAnimatedGeometry(Spatial subtree) {
        Geometry result = null;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            boolean hasIndices = indices != null;
            VertexBuffer weights = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            boolean hasWeights = weights != null;
            if (hasIndices && hasWeights) {
                result = geometry;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findAnimatedGeometry(child);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find the index of the specified scene-graph control in the specified
     * spatial.
     *
     * @param spatial the spatial to search (not null, unaffected)
     * @param sgc the control to search for (not null, unaffected)
     * @return the index (&ge;0) or -1 if not found
     */
    static int findIndex(Spatial spatial, Control sgc) {
        int numControls = spatial.getNumControls();
        int result = -1;
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
            Control control = spatial.getControl(controlIndex);
            if (control == sgc) {
                result = controlIndex;
                break;
            }
        }

        return result;
    }

    /**
     * Find the main root bone of a skeleton, based on its total bone weight.
     *
     * @param skeleton the skeleton (not null, unaffected)
     * @param targetMeshes an array of animated meshes to provide bone weights
     * (not null)
     * @return a root bone, or null if none found
     */
    static Joint findMainBone(Armature skeleton, Mesh[] targetMeshes) {
        Joint[] rootBones = skeleton.getRoots();

        Joint result;
        if (rootBones.length == 1) {
            result = rootBones[0];
        } else {
            result = null;
            float[] totalWeights = totalWeights(targetMeshes, skeleton);
            float greatestTotalWeight = Float.NEGATIVE_INFINITY;
            for (Joint rootBone : rootBones) {
                int boneIndex = skeleton.getJointIndex(rootBone);
                float weight = totalWeights[boneIndex];
                if (weight > greatestTotalWeight) {
                    result = rootBone;
                    greatestTotalWeight = weight;
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all animated meshes in the specified subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree which subtree (aliases created)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    static List<Mesh> listAnimatedMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            boolean hasIndices = indices != null;
            VertexBuffer weights = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            boolean hasWeights = weights != null;
            if (hasIndices && hasWeights && !storeResult.contains(mesh)) {
                storeResult.add(mesh);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listAnimatedMeshes(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Convert a transform from the mesh coordinate system to the local
     * coordinate system of the specified bone.
     *
     * @param parentBone (not null)
     * @param transform the transform to convert (not null, modified)
     */
    static void meshToLocal(Joint parentBone, Transform transform) {
        Vector3f location = transform.getTranslation();
        Quaternion orientation = transform.getRotation();
        Vector3f scale = transform.getScale();

        Transform pmx = parentBone.getModelTransform();
        Vector3f pmTranslate = pmx.getTranslation();
        Quaternion pmRotInv = pmx.getRotation().inverse();
        Vector3f pmScale = pmx.getScale();

        location.subtractLocal(pmTranslate);
        location.divideLocal(pmScale);
        pmRotInv.mult(location, location);
        scale.divideLocal(pmScale);
        pmRotInv.mult(orientation, orientation);
    }

    /**
     * Read an array of transforms from an input capsule.
     *
     * @param capsule the input capsule (not null)
     * @param fieldName the name of the field to read (not null)
     * @return a new array or null
     * @throws IOException from capsule
     */
    static Transform[] readTransformArray(InputCapsule capsule,
            String fieldName) throws IOException {
        Savable[] tmp = capsule.readSavableArray(fieldName, null);
        Transform[] result;
        if (tmp == null) {
            result = null;
        } else {
            result = new Transform[tmp.length];
            for (int i = 0; i < tmp.length; ++i) {
                result[i] = (Transform) tmp[i];
            }
        }

        return result;
    }

    /**
     * Calculate a coordinate transform for the specified spatial relative to a
     * specified ancestor node. The result incorporates the transform of the
     * starting spatial, but not that of the ancestor.
     *
     * @param startSpatial the starting spatial (not null, unaffected)
     * @param ancestorNode the ancestor node (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return a coordinate transform (either storeResult or a new vector, not
     * null)
     */
    static Transform relativeTransform(Spatial startSpatial,
            Node ancestorNode, Transform storeResult) {
        assert startSpatial.hasAncestor(ancestorNode);
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        result.loadIdentity();
        Spatial loopSpatial = startSpatial;
        while (loopSpatial != ancestorNode) {
            Transform localTransform = loopSpatial.getLocalTransform();
            result.combineWithParent(localTransform);
            loopSpatial = loopSpatial.getParent();
        }

        return result;
    }

    /**
     * Validate a skeleton for use with DynamicAnimControl.
     *
     * @param skeleton the skeleton to validate (not null, unaffected)
     */
    static void validate(Armature skeleton) {
        int numBones = skeleton.getJointCount();
        if (numBones < 0) {
            throw new IllegalArgumentException("Bone count is negative!");
        }

        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Joint bone = skeleton.getJoint(boneIndex);
            if (bone == null) {
                String msg = String.format("Bone %d in skeleton is null!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            }
            String boneName = bone.getName();
            if (boneName == null) {
                String msg = String.format("Bone %d in skeleton has null name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (boneName.equals(DynamicAnimControl.torsoName)) {
                String msg = String.format(
                        "Bone %d in skeleton has a reserved name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (nameSet.contains(boneName)) {
                String msg = "Duplicate bone name in skeleton: " + boneName;
                throw new IllegalArgumentException(msg);
            }
            nameSet.add(boneName);
        }
    }

    /**
     * Validate a model for use with DynamicAnimControl.
     *
     * @param model the model to validate (not null, unaffected)
     */
    static void validate(Spatial model) {
        List<Geometry> geometries = listGeometries(model, null);
        if (geometries.isEmpty()) {
            throw new IllegalArgumentException("No meshes in the model.");
        }
        for (Geometry geometry : geometries) {
            if (geometry.isIgnoreTransform()) {
                throw new IllegalArgumentException(
                        "A model geometry ignores transforms.");
            }
        }
    }
    // *************************************************************************
    // private methods

    private static void addPreOrderJoints(Joint bone, List<Joint> addResult) {
        assert bone != null;
        addResult.add(bone);
        List<Joint> children = bone.getChildren();
        for (Joint child : children) {
            addPreOrderJoints(child, addResult);
        }
    }

    /**
     * Add the vertex weights of each bone in the specified mesh to an array of
     * total weights.
     *
     * @param mesh animated mesh to analyze (not null, unaffected)
     * @param totalWeights (not null, modified)
     */
    private static void addWeights(Mesh mesh, float[] totalWeights) {
        assert totalWeights != null;

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        for (int vIndex = 0; vIndex < numVertices; ++vIndex) {
            for (int wIndex = 0; wIndex < 4; ++wIndex) {
                float weight = weightBuffer.get();
                int boneIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert) {
                    totalWeights[boneIndex] += FastMath.abs(weight);
                }
            }
        }
    }

    /**
     * Determine which physics link should manage the specified mesh vertex.
     *
     * @param mesh the mesh containing the vertex (not null, unaffected)
     * @param vertexIndex the vertex index in the mesh (&ge;0)
     * @param iArray temporary storage for bone indices (not null, modified)
     * @param wArray temporary storage for bone weights (not null, modified)
     * @param managerMap a map from bone indices to bone/torso names (not null,
     * unaffected)
     * @return a bone/torso name
     */
    private static String findManager(Mesh mesh, int vertexIndex, int[] iArray,
            float[] wArray, String[] managerMap) {
        vertexBoneIndices(mesh, vertexIndex, iArray);
        vertexBoneWeights(mesh, vertexIndex, wArray);
        Map<String, Float> weightMap = weightMap(iArray, wArray, managerMap);

        float bestTotalWeight = Float.NEGATIVE_INFINITY;
        String bestName = null;
        for (Map.Entry<String, Float> entry : weightMap.entrySet()) {
            float totalWeight = entry.getValue();
            if (totalWeight >= bestTotalWeight) {
                bestTotalWeight = totalWeight;
                bestName = entry.getKey();
            }
        }

        return bestName;
    }

    /**
     * Enumerate all geometries in the specified subtree of a scene graph. Note:
     * recursive!
     *
     * @param subtree (not null, aliases created)
     * @param addResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    private static List<Geometry> listGeometries(Spatial subtree,
            List<Geometry> addResult) {
        List<Geometry> result = (addResult == null) ? new ArrayList<>(50) : addResult;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            if (!result.contains(geometry)) {
                result.add(geometry);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listGeometries(child, result);
            }
        }

        return result;
    }

    /**
     * Enumerate all bones in a pre-order, depth-first traversal of the
     * skeleton, such that child bones never precede their ancestors.
     *
     * @param skeleton the skeleton to traverse (not null, unaffected)
     * @return a new list of bones
     */
    private static List<Joint> preOrderJoints(Armature skeleton) {
        int numBones = skeleton.getJointCount();
        List<Joint> result = new ArrayList<>(numBones);
        Joint[] rootBones = skeleton.getRoots();
        for (Joint rootBone : rootBones) {
            addPreOrderJoints(rootBone, result);
        }

        assert result.size() == numBones : result.size();
        return result;
    }

    /**
     * Read an index from a buffer.
     *
     * @param buffer a buffer of bytes or shorts (not null)
     * @return index (&ge;0)
     */
    private static int readIndex(Buffer buffer) {
        int result;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) buffer;
            byte b = byteBuffer.get();
            result = 0xff & b;
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer shortBuffer = (ShortBuffer) buffer;
            short s = shortBuffer.get();
            result = 0xffff & s;
        } else {
            throw new IllegalArgumentException();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Calculate the total bone weight animated by each bone in the specified
     * meshes.
     *
     * @param meshes the animated meshes to analyze (not null, unaffected)
     * @param skeleton (not null, unaffected)
     * @return a map from bone indices to total bone weight
     */
    private static float[] totalWeights(Mesh[] meshes, Armature skeleton) {
        int numBones = skeleton.getJointCount();
        float[] result = new float[numBones];
        for (Mesh mesh : meshes) {
            RagUtils.addWeights(mesh, result);
        }

        List<Joint> bones = preOrderJoints(skeleton);
        Collections.reverse(bones);
        for (Joint childBone : bones) {
            int childIndex = skeleton.getJointIndex(childBone);
            Joint parent = childBone.getParent();
            if (parent != null) {
                int parentIndex = skeleton.getJointIndex(parent);
                result[parentIndex] += result[childIndex];
            }
        }

        return result;
    }

    /**
     * Copy the bone indices for the indexed vertex.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    private static int[] vertexBoneIndices(Mesh mesh,
            int vertexIndex, int[] storeResult) {
        if (storeResult == null) {
            storeResult = new int[4];
        } else {
            assert storeResult.length >= 4 : storeResult.length;
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.position(4 * vertexIndex);
        for (int wIndex = 0; wIndex < maxWeightsPerVert; ++wIndex) {
            int boneIndex = readIndex(boneIndexBuffer);
            storeResult[wIndex] = boneIndex;
        }
        /*
         * Fill with -1s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; ++wIndex) {
            storeResult[wIndex] = -1;
        }

        return storeResult;
    }

    /**
     * Copy the bone weights for the indexed vertex.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    private static float[] vertexBoneWeights(Mesh mesh,
            int vertexIndex, float[] storeResult) {
        if (storeResult == null) {
            storeResult = new float[4];
        } else {
            assert storeResult.length >= 4 : storeResult.length;
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.position(4 * vertexIndex);
        for (int wIndex = 0; wIndex < maxWeightsPerVert; ++wIndex) {
            storeResult[wIndex] = weightBuffer.get();
        }
        /*
         * Fill with 0s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; ++wIndex) {
            storeResult[wIndex] = 0f;
        }

        return storeResult;
    }

    /**
     * Copy Vector3f data for the indexed vertex from the specified vertex
     * buffer.
     * <p>
     * A software skin update is required BEFORE reading vertex
     * positions/normals/tangents from an animated mesh
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (5 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    private static Vector3f vertexVector3f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector3f storeResult) {
        assert bufferType == VertexBuffer.Type.BindPoseNormal
                || bufferType == VertexBuffer.Type.BindPosePosition
                || bufferType == VertexBuffer.Type.Binormal
                || bufferType == VertexBuffer.Type.Normal
                || bufferType == VertexBuffer.Type.Position : bufferType;
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(3 * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();
        storeResult.z = floatBuffer.get();

        return storeResult;
    }

    /**
     * Tabulate the total bone weight associated with each bone/torso link in a
     * ragdoll.
     *
     * @param biArray the array of bone indices (not null, unaffected)
     * @param bwArray the array of bone weights (not null, unaffected)
     * @param managerMap a map from bone indices to managing link names (not
     * null, unaffected)
     * @return a new map from link names to total weight
     */
    private static Map<String, Float> weightMap(int[] biArray,
            float[] bwArray, String[] managerMap) {
        assert biArray.length == 4;
        assert bwArray.length == 4;

        Map<String, Float> weightMap = new HashMap<>(4);
        for (int j = 0; j < 4; ++j) {
            int boneIndex = biArray[j];
            if (boneIndex != -1) {
                String managerName = managerMap[boneIndex];
                if (weightMap.containsKey(managerName)) {
                    float oldWeight = weightMap.get(managerName);
                    float newWeight = oldWeight + bwArray[j];
                    weightMap.put(managerName, newWeight);
                } else {
                    weightMap.put(managerName, bwArray[j]);
                }
            }
        }

        return weightMap;
    }
}
