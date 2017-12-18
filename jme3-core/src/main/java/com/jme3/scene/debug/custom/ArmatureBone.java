package com.jme3.scene.debug.custom;

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

import com.jme3.animation.Armature;
import com.jme3.animation.Joint;
import com.jme3.bounding.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.shape.Sphere;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.jme3.util.BufferUtils.createFloatBuffer;

/**
 * The class that displays either wires between the bones' heads if no length
 * data is supplied and full bones' shapes otherwise.
 */
public class ArmatureBone extends Node {

    /**
     * The armature to be displayed.
     */
    private Armature armature;
    /**
     * The map between the bone index and its length.
     */
    private Map<Joint, Node> jointNode = new HashMap<>();
    private Map<Node, Joint> nodeJoint = new HashMap<>();
    private Node selectedNode = null;
    private boolean guessJointsOrientation = false;

    /**
     * Creates a wire with bone lengths data. If the data is supplied then the
     * wires will show each full bone (from head to tail).
     *
     * @param armature    the armature that will be shown
     * @param boneLengths a map between the bone's index and the bone's length
     */
    public ArmatureBone(Armature armature, Map<Integer, Float> boneLengths, boolean guessJointsOrientation) {
        this.armature = armature;
        this.guessJointsOrientation = guessJointsOrientation;

        BoneShape boneShape = new BoneShape();
        Sphere jointShape = new Sphere(16, 16, 0.05f);
        jointShape.setBuffer(VertexBuffer.Type.Color, 4, createFloatBuffer(jointShape.getVertexCount() * 4));
        FloatBuffer cb = jointShape.getFloatBuffer(VertexBuffer.Type.Color);

        cb.rewind();
        for (int i = 0; i < jointShape.getVertexCount(); i++) {
            cb.put(0.05f).put(0.05f).put(0.05f).put(1f);
        }

        for (Joint joint : armature.getRoots()) {
            createSkeletonGeoms(joint, boneShape, jointShape, boneLengths, armature, this, guessJointsOrientation);
        }
        this.updateModelBound();


        Sphere originShape = new Sphere(16, 16, 0.02f);
        originShape.setBuffer(VertexBuffer.Type.Color, 4, createFloatBuffer(originShape.getVertexCount() * 4));
        cb = originShape.getFloatBuffer(VertexBuffer.Type.Color);
        cb.rewind();
        for (int i = 0; i < jointShape.getVertexCount(); i++) {
            cb.put(0.4f).put(0.4f).put(0.05f).put(1f);
        }

        Geometry origin = new Geometry("origin", originShape);
        BoundingVolume bv = this.getWorldBound();
        float scale = 1;
        if (bv.getType() == BoundingVolume.Type.AABB) {
            BoundingBox bb = (BoundingBox) bv;
            scale = (bb.getXExtent() + bb.getYExtent() + bb.getZExtent()) / 3f;
        } else if (bv.getType() == BoundingVolume.Type.Sphere) {
            BoundingSphere bs = (BoundingSphere) bv;
            scale = bs.getRadius();
        }
        origin.scale(scale);
        attachChild(origin);
    }

    protected final void createSkeletonGeoms(Joint joint, Mesh boneShape, Mesh jointShape, Map<Integer, Float> boneLengths, Armature armature, Node parent, boolean guessBonesOrientation) {

        Node n = new Node(joint.getName() + "Node");
        Geometry bGeom = new Geometry(joint.getName() + "Bone", boneShape);
        Geometry jGeom = new Geometry(joint.getName() + "Joint", jointShape);
        n.setLocalTransform(joint.getLocalTransform());

        float boneLength = boneLengths.get(armature.getJointIndex(joint));

        if (guessBonesOrientation) {
            //One child only, the bone direction is from the parent joint to the child joint.
            if (joint.getChildren().size() == 1) {
                Vector3f v = joint.getChildren().get(0).getLocalTranslation();
                Quaternion q = new Quaternion();
                q.lookAt(v, Vector3f.UNIT_Z);
                bGeom.setLocalRotation(q);
            }
            //no child, the bone has the same direction as the parent bone.
            if (joint.getChildren().isEmpty()) {
                //no parent, let's use the bind orientation of the bone
                Spatial s = parent.getChild(0);
                if (s != null) {
                    bGeom.setLocalRotation(s.getLocalRotation());
                }
            }
        }

        float boneScale = boneLength * 0.8f;
        float scale = boneScale / 8f;
        bGeom.setLocalScale(new Vector3f(scale, scale, boneScale));
        Vector3f offset = new Vector3f(0, 0, boneLength * 0.1f);
        bGeom.getLocalRotation().multLocal(offset);
        bGeom.setLocalTranslation(offset);
        jGeom.setLocalScale(boneLength);

        if (joint.getChildren().size() <= 1) {
            n.attachChild(bGeom);
        }
        n.attachChild(jGeom);

        jointNode.put(joint, n);
        nodeJoint.put(n, joint);
        parent.attachChild(n);
        for (Joint child : joint.getChildren()) {
            createSkeletonGeoms(child, boneShape, jointShape, boneLengths, armature, n, guessBonesOrientation);
        }
    }

    protected Joint select(Geometry g) {
        Node parentNode = g.getParent();

        if (parent != null) {
            Joint j = nodeJoint.get(parentNode);
            if (j != null) {
                selectedNode = parentNode;
            }
            return j;
        }
        return null;
    }

    protected Node getSelectedNode() {
        return selectedNode;
    }


    protected final void updateSkeletonGeoms(Joint joint) {
        Node n = jointNode.get(joint);
        n.setLocalTransform(joint.getLocalTransform());

        for (Joint child : joint.getChildren()) {
            updateSkeletonGeoms(child);
        }
    }

    /**
     * The method updates the geometry according to the positions of the bones.
     */
    public void updateGeometry() {
        for (Joint joint : armature.getRoots()) {
            updateSkeletonGeoms(joint);
        }
    }
}
