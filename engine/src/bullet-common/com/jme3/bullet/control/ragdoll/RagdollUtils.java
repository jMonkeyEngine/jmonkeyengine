/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.control.ragdoll;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

/**
 *
 * @author Nehon
 */
public class RagdollUtils {

    public static void setJointLimit(SixDofJoint joint, float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {

        joint.getRotationalLimitMotor(0).setHiLimit(maxX);
        joint.getRotationalLimitMotor(0).setLoLimit(minX);
        joint.getRotationalLimitMotor(1).setHiLimit(maxY);
        joint.getRotationalLimitMotor(1).setLoLimit(minY);
        joint.getRotationalLimitMotor(2).setHiLimit(maxZ);
        joint.getRotationalLimitMotor(2).setLoLimit(minZ);
    }

    public static Map<Integer, List<Float>> buildPointMap(Spatial model) {


        Map<Integer, List<Float>> map = new HashMap<Integer, List<Float>>();
        if (model instanceof Geometry) {
            Geometry g = (Geometry) model;
            buildPointMapForMesh(g.getMesh(), map);
        } else if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial s : node.getChildren()) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry) s;
                    buildPointMapForMesh(g.getMesh(), map);
                }
            }
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
     * Create a hull collision shape from linked vertices to this bone.
     * Vertices have to be previoulsly gathered in a map using buildPointMap method
     * @param link
     * @param model
     * @return 
     */
    public static HullCollisionShape makeShapeFromPointMap(Map<Integer, List<Float>> pointsMap, List<Integer> boneIndices, Vector3f initialScale, Vector3f initialPosition) {

        ArrayList<Float> points = new ArrayList<Float>();
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

        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }


        return new HullCollisionShape(p);
    }

    //retruns the list of bone indices of the given bone and its child(if they are not in the boneList)
    public static List<Integer> getBoneIndices(Bone bone, Skeleton skeleton, Set<String> boneList) {
        List<Integer> list = new LinkedList<Integer>();
        if (boneList.isEmpty()) {
            list.add(skeleton.getBoneIndex(bone));
        } else {
            list.add(skeleton.getBoneIndex(bone));
            for (Bone chilBone : bone.getChildren()) {
                if (!boneList.contains(chilBone.getName())) {
                    list.addAll(getBoneIndices(chilBone, skeleton, boneList));
                }
            }
        }
        return list;
    }

    /**
     * Create a hull collision shape from linked vertices to this bone.
     * 
     * @param link
     * @param model
     * @return 
     */
    public static HullCollisionShape makeShapeFromVerticeWeights(Spatial model, List<Integer> boneIndices, Vector3f initialScale, Vector3f initialPosition, float weightThreshold) {

        ArrayList<Float> points = new ArrayList<Float>();
        if (model instanceof Geometry) {
            Geometry g = (Geometry) model;
            for (Integer index : boneIndices) {
                points.addAll(getPoints(g.getMesh(), index, initialScale, initialPosition, weightThreshold));
            }
        } else if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial s : node.getChildren()) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry) s;
                    for (Integer index : boneIndices) {
                        points.addAll(getPoints(g.getMesh(), index, initialScale, initialPosition, weightThreshold));
                    }

                }
            }
        }
        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }


        return new HullCollisionShape(p);
    }

    /**
     * returns a list of points for the given bone
     * @param mesh
     * @param boneIndex
     * @param offset
     * @param link
     * @return 
     */
    private static List<Float> getPoints(Mesh mesh, int boneIndex, Vector3f initialScale, Vector3f offset, float weightThreshold) {

        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        ByteBuffer boneIndices = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
        FloatBuffer boneWeight = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        vertices.rewind();
        boneIndices.rewind();
        boneWeight.rewind();

        ArrayList<Float> results = new ArrayList<Float>();

        int vertexComponents = mesh.getVertexCount() * 3;

        for (int i = 0; i < vertexComponents; i += 3) {
            int k;
            boolean add = false;
            int start = i / 3 * 4;
            for (k = start; k < start + 4; k++) {
                if (boneIndices.get(k) == boneIndex && boneWeight.get(k) >= weightThreshold) {
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
     * Updates a bone position and rotation.
     * if the child bones are not in the bone list this means, they are not associated with a physic shape.
     * So they have to be updated
     * @param bone the bone
     * @param pos the position
     * @param rot the rotation
     */
    public static void setTransform(Bone bone, Vector3f pos, Quaternion rot, boolean restoreBoneControl, Set<String> boneList) {
        //we ensure that we have the control
        if (restoreBoneControl) {
            bone.setUserControl(true);
        }
        //we set te user transforms of the bone
        bone.setUserTransformsWorld(pos, rot);
        for (Bone childBone : bone.getChildren()) {
            //each child bone that is not in the list is updated
            if (!boneList.contains(childBone.getName())) {
                Transform t = childBone.getCombinedTransform(pos, rot);
                setTransform(childBone, t.getTranslation(), t.getRotation(), restoreBoneControl, boneList);
            }
        }
        //we give back the control to the keyframed animation
        if (restoreBoneControl) {
            bone.setUserControl(false);
        }
    }

    public static void setUserControl(Bone bone, boolean bool) {
        bone.setUserControl(bool);
        for (Bone child : bone.getChildren()) {
            setUserControl(child, bool);
        }
    }
}
