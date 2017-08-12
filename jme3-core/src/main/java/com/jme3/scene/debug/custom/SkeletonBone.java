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

import java.util.Map;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;

import static com.jme3.util.BufferUtils.createFloatBuffer;

import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * The class that displays either wires between the bones' heads if no length
 * data is supplied and full bones' shapes otherwise.
 */
public class SkeletonBone extends Node {

    /**
     * The skeleton to be displayed.
     */
    private Skeleton skeleton;
    /**
     * The map between the bone index and its length.
     */
    private Map<Bone, Node> boneNodes = new HashMap<Bone, Node>();
    private Map<Node, Bone> nodeBones = new HashMap<Node, Bone>();
    private Node selectedNode = null;
    private boolean guessBonesOrientation = false;

    /**
     * Creates a wire with bone lengths data. If the data is supplied then the
     * wires will show each full bone (from head to tail).
     *
     * @param skeleton    the skeleton that will be shown
     * @param boneLengths a map between the bone's index and the bone's length
     */
    public SkeletonBone(Skeleton skeleton, Map<Integer, Float> boneLengths, boolean guessBonesOrientation) {
        this.skeleton = skeleton;
        this.skeleton.reset();
        this.skeleton.updateWorldVectors();
        this.guessBonesOrientation = guessBonesOrientation;

        BoneShape boneShape = new BoneShape(5, 12, 0.02f, 0.07f, 1f, false, false);
        Sphere jointShape = new Sphere(10, 10, 0.1f);
        jointShape.setBuffer(VertexBuffer.Type.Color, 4, createFloatBuffer(jointShape.getVertexCount() * 4));
        FloatBuffer cb = jointShape.getFloatBuffer(VertexBuffer.Type.Color);

        cb.rewind();
        for (int i = 0; i < jointShape.getVertexCount(); i++) {
            cb.put(0.05f).put(0.05f).put(0.05f).put(1f);
        }

        for (Bone bone : skeleton.getRoots()) {
            createSkeletonGeoms(bone, boneShape, jointShape, boneLengths, skeleton, this, guessBonesOrientation);
        }
    }

    protected final void createSkeletonGeoms(Bone bone, Mesh boneShape, Mesh jointShape, Map<Integer, Float> boneLengths, Skeleton skeleton, Node parent, boolean guessBonesOrientation) {

        if (guessBonesOrientation && bone.getName().equalsIgnoreCase("Site")) {
            //BVH skeleton have a useless end point bone named Site
            return;
        }
        Node n = new Node(bone.getName() + "Node");
        Geometry bGeom = new Geometry(bone.getName(), boneShape);
        Geometry jGeom = new Geometry(bone.getName() + "Joint", jointShape);
        n.setLocalTranslation(bone.getLocalPosition());
        n.setLocalRotation(bone.getLocalRotation());

        float boneLength = boneLengths.get(skeleton.getBoneIndex(bone));
        n.setLocalScale(bone.getLocalScale());

        bGeom.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X).normalizeLocal());

        if (guessBonesOrientation) {
            //One child only, the bone direction is from the parent joint to the child joint.
            if (bone.getChildren().size() == 1) {
                Vector3f v = bone.getChildren().get(0).getLocalPosition();
                Quaternion q = new Quaternion();
                q.lookAt(v, Vector3f.UNIT_Z);
                bGeom.setLocalRotation(q);
            }
            //no child, the bone has the same direction as the parent bone.
            if (bone.getChildren().isEmpty()) {
                if (parent.getChildren().size() > 0) {
                    bGeom.setLocalRotation(parent.getChild(0).getLocalRotation());
                } else {
                    //no parent, let's use the bind orientation of the bone
                    bGeom.setLocalRotation(bone.getBindRotation());
                }
            }
        }
        bGeom.setLocalScale(boneLength);
        jGeom.setLocalScale(boneLength);

        n.attachChild(bGeom);
        n.attachChild(jGeom);

        //tip
        if (bone.getChildren().size() != 1) {
            Geometry gt = jGeom.clone();
            gt.scale(0.8f);
            Vector3f v = new Vector3f(0, boneLength, 0);
            if (guessBonesOrientation) {
                if (bone.getChildren().isEmpty()) {
                    if (parent.getChildren().size() > 0) {
                        gt.setLocalTranslation(bGeom.getLocalRotation().mult(parent.getChild(0).getLocalRotation()).mult(v, v));
                    } else {
                        gt.setLocalTranslation(bGeom.getLocalRotation().mult(bone.getBindRotation()).mult(v, v));
                    }
                }
            } else {
                gt.setLocalTranslation(v);
            }

            n.attachChild(gt);
        }


        boneNodes.put(bone, n);
        nodeBones.put(n, bone);
        parent.attachChild(n);
        for (Bone childBone : bone.getChildren()) {
            createSkeletonGeoms(childBone, boneShape, jointShape, boneLengths, skeleton, n, guessBonesOrientation);
        }
    }

    protected Bone select(Geometry g) {
        Node parentNode = g.getParent();

        if (parent != null) {
            Bone b = nodeBones.get(parentNode);
            if (b != null) {
                selectedNode = parentNode;
            }
            return b;
        }
        return null;
    }

    protected Node getSelectedNode() {
        return selectedNode;
    }


//    private Quaternion getRotationBetweenVect(Vector3f v1, Vector3f v2){       
//        Vector3f a =v1.cross(v2);
//        float w = FastMath.sqrt((v1.length() * v1.length()) * (v2.length() * v2.length())) + v1.dot(v2);       
//        return new Quaternion(a.x, a.y, a.z, w).normalizeLocal() ;
//    }

    protected final void updateSkeletonGeoms(Bone bone) {
        if (guessBonesOrientation && bone.getName().equalsIgnoreCase("Site")) {
            return;
        }
        Node n = boneNodes.get(bone);
        n.setLocalTranslation(bone.getLocalPosition());
        n.setLocalRotation(bone.getLocalRotation());
        n.setLocalScale(bone.getLocalScale());

        for (Bone childBone : bone.getChildren()) {
            updateSkeletonGeoms(childBone);
        }
    }

    /**
     * The method updates the geometry according to the poitions of the bones.
     */
    public void updateGeometry() {

        for (Bone bone : skeleton.getRoots()) {
            updateSkeletonGeoms(bone);
        }
    }
}
