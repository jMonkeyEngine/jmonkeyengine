/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.bullet.control.ragdoll;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;

/**
 * Utility methods used by KinematicRagdollControl.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Nehon
 */
public class RagdollUtils {

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RagdollUtils() {
    }

    /**
     * Alter the limits of the specified 6-DOF joint.
     *
     * @param joint which joint to alter (not null)
     * @param maxX the maximum rotation on the X axis (in radians)
     * @param minX the minimum rotation on the X axis (in radians)
     * @param maxY the maximum rotation on the Y axis (in radians)
     * @param minY the minimum rotation on the Y axis (in radians)
     * @param maxZ the maximum rotation on the Z axis (in radians)
     * @param minZ the minimum rotation on the Z axis (in radians)
     */
    public static void setJointLimit(SixDofJoint joint, float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {

        joint.getRotationalLimitMotor(0).setHiLimit(maxX);
        joint.getRotationalLimitMotor(0).setLoLimit(minX);
        joint.getRotationalLimitMotor(1).setHiLimit(maxY);
        joint.getRotationalLimitMotor(1).setLoLimit(minY);
        joint.getRotationalLimitMotor(2).setHiLimit(maxZ);
        joint.getRotationalLimitMotor(2).setLoLimit(minZ);
    }

    /**
     * Build a map of mesh vertices in a subtree of the scene graph.
     *
     * @param model the root of the subtree (may be null)
     * @return a new map (not null)
     */
    public static Map<Integer, List<Float>> buildPointMap(Spatial model) {
        Map<Integer, List<Float>> map = new HashMap<>();

        SkeletonControl skeletonCtrl = model.getControl(SkeletonControl.class);
        Mesh[] targetMeshes = skeletonCtrl.getTargets();
        for (Mesh mesh : targetMeshes) {
            buildPointMapForMesh(mesh, map);
        }

        return map;
    }

    private static Map<Integer, List<Float>> buildPointMapForMesh(Mesh mesh, Map<Integer, List<Float>> map) {

        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        ByteBuffer boneIndices = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
        FloatBuffer boneWeight = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        vertices.rewind();
        boneIndices.rewind();
        boneWeight.rewind();

        int vertexComponents = mesh.getVertexCount() * 3;
        int k, start, index;
        float maxWeight = 0;

        for (int i = 0; i < vertexComponents; i += 3) {


            start = i / 3 * 4;
            index = 0;
            maxWeight = -1;
            for (k = start; k < start + 4; k++) {
                float weight = boneWeight.get(k);
                if (weight > maxWeight) {
                    maxWeight = weight;
                    index = boneIndices.get(k);
                }
            }
            List<Float> points = map.get(index);
            if (points == null) {
                points = new ArrayList<Float>();
                map.put(index, points);
            }
            points.add(vertices.get(i));
            points.add(vertices.get(i + 1));
            points.add(vertices.get(i + 2));
        }
        return map;
    }

    /**
     * Create a hull collision shape from linked vertices to this bone. Vertices
     * must have previously been gathered using buildPointMap().
     *
     * @param pointsMap map from bone indices to coordinates (not null,
     * unaffected)
     * @param boneIndices (not null, unaffected)
     * @param initialScale scale factors (not null, unaffected)
     * @param initialPosition location (not null, unaffected)
     * @return a new shape (not null)
     */
    public static HullCollisionShape makeShapeFromPointMap(Map<Integer, List<Float>> pointsMap, List<Integer> boneIndices, Vector3f initialScale, Vector3f initialPosition) {

        ArrayList<Float> points = new ArrayList<>();
        for (Integer index : boneIndices) {
            List<Float> l = pointsMap.get(index);
            if (l != null) {

                for (int i = 0; i < l.size(); i += 3) {
                    Vector3f pos = new Vector3f();
                    pos.x = l.get(i);
                    pos.y = l.get(i + 1);
                    pos.z = l.get(i + 2);
                    pos.subtractLocal(initialPosition).multLocal(initialScale);
                    points.add(pos.x);
                    points.add(pos.y);
                    points.add(pos.z);
                }
            }
        }

        assert !points.isEmpty();
        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }

        return new HullCollisionShape(p);
    }

    /**
     * Enumerate the bone indices of the specified bone and all its descendents.
     *
     * @param bone the input bone (not null)
     * @param skeleton the skeleton containing the bone (not null)
     * @param boneList a set of bone names (not null, unaffected)
     *
     * @return a new list (not null)
     */
    public static List<Integer> getBoneIndices(Bone bone, Skeleton skeleton, Set<String> boneList) {
        List<Integer> list = new LinkedList<>();
        if (boneList.isEmpty()) {
            list.add(skeleton.getBoneIndex(bone));
        } else {
            list.add(skeleton.getBoneIndex(bone));
            for (Bone childBone : bone.getChildren()) {
                if (!boneList.contains(childBone.getName())) {
                    list.addAll(getBoneIndices(childBone, skeleton, boneList));
                }
            }
        }
        return list;
    }

    /**
     * Create a hull collision shape from linked vertices to this bone.
     *
     * @param model the model on which to base the shape
     * @param boneIndices indices of relevant bones (not null, unaffected)
     * @param initialScale scale factors
     * @param initialPosition location
     * @param weightThreshold minimum weight for inclusion
     * @return a new shape
     */
    public static HullCollisionShape makeShapeFromVerticeWeights(Spatial model,
            List<Integer> boneIndices, Vector3f initialScale,
            Vector3f initialPosition, float weightThreshold) {
        List<Float> points = new ArrayList<>(100);

        SkeletonControl skeletonCtrl = model.getControl(SkeletonControl.class);
        Mesh[] targetMeshes = skeletonCtrl.getTargets();
        for (Mesh mesh : targetMeshes) {
            for (Integer index : boneIndices) {
                List<Float> bonePoints = getPoints(mesh, index, initialScale,
                        initialPosition, weightThreshold);
                points.addAll(bonePoints);
            }
        }

        assert !points.isEmpty();
        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }

        return new HullCollisionShape(p);
    }

    /**
     * Enumerate vertices that meet the weight threshold for the indexed bone.
     *
     * @param mesh the mesh to analyze (not null)
     * @param boneIndex the index of the bone (&ge;0)
     * @param initialScale a scale applied to vertex positions (not null,
     * unaffected)
     * @param offset an offset subtracted from vertex positions (not null,
     * unaffected)
     * @param weightThreshold the minimum bone weight for inclusion in the
     * result (&ge;0, &le;1)
     * @return a new list of vertex coordinates (not null, length a multiple of
     * 3)
     */
    private static List<Float> getPoints(Mesh mesh, int boneIndex, Vector3f initialScale, Vector3f offset, float weightThreshold) {

        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndices = biBuf.getDataReadOnly();
        FloatBuffer boneWeight = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        vertices.rewind();
        boneIndices.rewind();
        boneWeight.rewind();

        ArrayList<Float> results = new ArrayList<>();

        int vertexComponents = mesh.getVertexCount() * 3;

        for (int i = 0; i < vertexComponents; i += 3) {
            int k;
            boolean add = false;
            int start = i / 3 * 4;
            for (k = start; k < start + 4; k++) {
                if (readIndex(boneIndices, k) == boneIndex
                        && boneWeight.get(k) >= weightThreshold) {
                    add = true;
                    break;
                }
            }
            if (add) {

                Vector3f pos = new Vector3f();
                pos.x = vertices.get(i);
                pos.y = vertices.get(i + 1);
                pos.z = vertices.get(i + 2);
                pos.subtractLocal(offset).multLocal(initialScale);
                results.add(pos.x);
                results.add(pos.y);
                results.add(pos.z);

            }
        }

        return results;
    }

    /**
     * Updates a bone position and rotation. if the child bones are not in the
     * bone list this means, they are not associated with a physics shape. So
     * they have to be updated
     *
     * @param bone the bone
     * @param pos the position
     * @param rot the rotation
     * @param restoreBoneControl true &rarr; user-control flag should be set
     * @param boneList the names of all bones without collision shapes (not
     * null, unaffected)
     */
    public static void setTransform(Bone bone, Vector3f pos, Quaternion rot, boolean restoreBoneControl, Set<String> boneList) {
        //we ensure that we have the control
        if (restoreBoneControl) {
            bone.setUserControl(true);
        }
        //we set te user transforms of the bone
        bone.setUserTransformsInModelSpace(pos, rot);
        for (Bone childBone : bone.getChildren()) {
            //each child bone that is not in the list is updated
            if (!boneList.contains(childBone.getName())) {
                Transform t = childBone.getCombinedTransform(pos, rot);
                setTransform(childBone, t.getTranslation(), t.getRotation(), restoreBoneControl, boneList);
            }
        }
        // return control to the keyframe animation
        if (restoreBoneControl) {
            bone.setUserControl(false);
        }
    }

    /**
     * Alter the user-control flags of a bone and all its descendents.
     *
     * @param bone the ancestor bone (not null, modified)
     * @param bool true to enable user control, false to disable
     */
    public static void setUserControl(Bone bone, boolean bool) {
        bone.setUserControl(bool);
        for (Bone child : bone.getChildren()) {
            setUserControl(child, bool);
        }
    }

    /**
     * Test whether the indexed bone has at least one vertex in the specified
     * meshes with a weight greater than the specified threshold.
     *
     * @param boneIndex the index of the bone (&ge;0)
     * @param targets the meshes to search (not null, no null elements)
     * @param weightThreshold the threshold (&ge;0, &le;1)
     * @return true if at least 1 vertex found, otherwise false
     */
    public static boolean hasVertices(int boneIndex, Mesh[] targets,
            float weightThreshold) {
        for (Mesh mesh : targets) {
            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndices = biBuf.getDataReadOnly();
            FloatBuffer boneWeight
                    = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

            boneIndices.rewind();
            boneWeight.rewind();

            int vertexComponents = mesh.getVertexCount() * 3;
            for (int i = 0; i < vertexComponents; i += 3) {
                int start = i / 3 * 4;
                for (int k = start; k < start + 4; k++) {
                    if (readIndex(boneIndices, k) == boneIndex
                            && boneWeight.get(k) >= weightThreshold) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Read an index from a buffer.
     *
     * @param buffer a buffer of bytes or shorts (not null)
     * @param k the position from which the index will be read
     * @return the index value (&ge;0)
     */
    public static int readIndex(Buffer buffer, int k) {
        int result;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) buffer;
            byte b = byteBuffer.get(k);
            result = 0xff & b;
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer shortBuffer = (ShortBuffer) buffer;
            short s = shortBuffer.get(k);
            result = 0xffff & s;
        } else {
            throw new IllegalArgumentException(buffer.getClass().getName());
        }

        assert result >= 0 : result;
        return result;
    }
}
