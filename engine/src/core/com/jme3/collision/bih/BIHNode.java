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
package com.jme3.collision.bih;

import com.jme3.bounding.BoundingBox;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;

/**
 * Bounding Interval Hierarchy.
 * Based on:
 *
 * Instant Ray Tracing: The Bounding Interval Hierarchy
 * By Carsten Wächter and Alexander Keller
 */
public final class BIHNode implements Savable {

    private int leftIndex, rightIndex;
    private BIHNode left;
    private BIHNode right;
    private float leftPlane;
    private float rightPlane;
    private int axis;
    
    //Do not do this: It increases memory usage of each BIHNode by at least 56 bytes!
    //
    //private Triangle tmpTriangle = new Triangle();

    public BIHNode(int l, int r) {
        leftIndex = l;
        rightIndex = r;
        axis = 3; // indicates leaf
    }

    public BIHNode(int axis) {
        this.axis = axis;
    }

    public BIHNode() {
    }

    public BIHNode getLeftChild() {
        return left;
    }

    public void setLeftChild(BIHNode left) {
        this.left = left;
    }

    public float getLeftPlane() {
        return leftPlane;
    }

    public void setLeftPlane(float leftPlane) {
        this.leftPlane = leftPlane;
    }

    public BIHNode getRightChild() {
        return right;
    }

    public void setRightChild(BIHNode right) {
        this.right = right;
    }

    public float getRightPlane() {
        return rightPlane;
    }

    public void setRightPlane(float rightPlane) {
        this.rightPlane = rightPlane;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(leftIndex, "left_index", 0);
        oc.write(rightIndex, "right_index", 0);
        oc.write(leftPlane, "left_plane", 0);
        oc.write(rightPlane, "right_plane", 0);
        oc.write(axis, "axis", 0);
        oc.write(left, "left_node", null);
        oc.write(right, "right_node", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        leftIndex = ic.readInt("left_index", 0);
        rightIndex = ic.readInt("right_index", 0);
        leftPlane = ic.readFloat("left_plane", 0);
        rightPlane = ic.readFloat("right_plane", 0);
        axis = ic.readInt("axis", 0);
        left = (BIHNode) ic.readSavable("left_node", null);
        right = (BIHNode) ic.readSavable("right_node", null);
    }

    public static final class BIHStackData {

        private final BIHNode node;
        private final float min, max;

        BIHStackData(BIHNode node, float min, float max) {
            this.node = node;
            this.min = min;
            this.max = max;
        }
    }

    public final int intersectWhere(Collidable col,
            BoundingBox box,
            Matrix4f worldMatrix,
            BIHTree tree,
            CollisionResults results) {

        TempVars vars = TempVars.get();
        ArrayList<BIHStackData> stack = vars.bihStack;
        stack.clear();

        float[] minExts = {box.getCenter().x - box.getXExtent(),
            box.getCenter().y - box.getYExtent(),
            box.getCenter().z - box.getZExtent()};

        float[] maxExts = {box.getCenter().x + box.getXExtent(),
            box.getCenter().y + box.getYExtent(),
            box.getCenter().z + box.getZExtent()};

        stack.add(new BIHStackData(this, 0, 0));

        Triangle t = new Triangle();
        int cols = 0;

        stackloop:
        while (stack.size() > 0) {
            BIHNode node = stack.remove(stack.size() - 1).node;

            while (node.axis != 3) {
                int a = node.axis;

                float maxExt = maxExts[a];
                float minExt = minExts[a];

                if (node.leftPlane < node.rightPlane) {
                    // means there's a gap in the middle
                    // if the box is in that gap, we stop there
                    if (minExt > node.leftPlane
                            && maxExt < node.rightPlane) {
                        continue stackloop;
                    }
                }

                if (maxExt < node.rightPlane) {
                    node = node.left;
                } else if (minExt > node.leftPlane) {
                    node = node.right;
                } else {
                    stack.add(new BIHStackData(node.right, 0, 0));
                    node = node.left;
                }
//                if (maxExt < node.leftPlane
//                 && maxExt < node.rightPlane){
//                    node = node.left;
//                }else if (minExt > node.leftPlane
//                       && minExt > node.rightPlane){
//                    node = node.right;
//                }else{

//                }
            }

            for (int i = node.leftIndex; i <= node.rightIndex; i++) {
                tree.getTriangle(i, t.get1(), t.get2(), t.get3());
                if (worldMatrix != null) {
                    worldMatrix.mult(t.get1(), t.get1());
                    worldMatrix.mult(t.get2(), t.get2());
                    worldMatrix.mult(t.get3(), t.get3());
                }

                int added = col.collideWith(t, results);

                if (added > 0) {
                    int index = tree.getTriangleIndex(i);
                    int start = results.size() - added;

                    for (int j = start; j < results.size(); j++) {
                        CollisionResult cr = results.getCollisionDirect(j);
                        cr.setTriangleIndex(index);
                    }

                    cols += added;
                }
            }
        }
        vars.release();
        return cols;
    }

    public final int intersectBrute(Ray r,
            Matrix4f worldMatrix,
            BIHTree tree,
            float sceneMin,
            float sceneMax,
            CollisionResults results) {
        float tHit = Float.POSITIVE_INFINITY;
        
        TempVars vars = TempVars.get();

        Vector3f v1 = vars.vect1,
                v2 = vars.vect2,
                v3 = vars.vect3;

        int cols = 0;

        ArrayList<BIHStackData> stack = vars.bihStack;
        stack.clear();
        stack.add(new BIHStackData(this, 0, 0));
        stackloop:
        while (stack.size() > 0) {

            BIHStackData data = stack.remove(stack.size() - 1);
            BIHNode node = data.node;

            leafloop:
            while (node.axis != 3) { // while node is not a leaf
                BIHNode nearNode, farNode;
                nearNode = node.left;
                farNode = node.right;

                stack.add(new BIHStackData(farNode, 0, 0));
                node = nearNode;
            }

            // a leaf
            for (int i = node.leftIndex; i <= node.rightIndex; i++) {
                tree.getTriangle(i, v1, v2, v3);

                if (worldMatrix != null) {
                    worldMatrix.mult(v1, v1);
                    worldMatrix.mult(v2, v2);
                    worldMatrix.mult(v3, v3);
                }

                float t = r.intersects(v1, v2, v3);
                if (t < tHit) {
                    tHit = t;
                    Vector3f contactPoint = new Vector3f(r.direction).multLocal(tHit).addLocal(r.origin);
                    CollisionResult cr = new CollisionResult(contactPoint, tHit);
                    cr.setTriangleIndex(tree.getTriangleIndex(i));
                    results.addCollision(cr);
                    cols++;
                }
            }
        }
        vars.release();
        return cols;
    }

    public final int intersectWhere(Ray r,
            Matrix4f worldMatrix,
            BIHTree tree,
            float sceneMin,
            float sceneMax,
            CollisionResults results) {

        TempVars vars = TempVars.get();
        ArrayList<BIHStackData> stack = vars.bihStack;
        stack.clear();

//        float tHit = Float.POSITIVE_INFINITY;

        Vector3f o = vars.vect1.set(r.getOrigin());
        Vector3f d =  vars.vect2.set(r.getDirection());

        Matrix4f inv =vars.tempMat4.set(worldMatrix).invertLocal();

        inv.mult(r.getOrigin(), r.getOrigin());

        // Fixes rotation collision bug
        inv.multNormal(r.getDirection(), r.getDirection());
//        inv.multNormalAcross(r.getDirection(), r.getDirection());

        float[] origins = {r.getOrigin().x,
            r.getOrigin().y,
            r.getOrigin().z};

        float[] invDirections = {1f / r.getDirection().x,
            1f / r.getDirection().y,
            1f / r.getDirection().z};

        r.getDirection().normalizeLocal();

        Vector3f v1 = vars.vect3,
                v2 = vars.vect4,
                v3 = vars.vect5;
        int cols = 0;

        stack.add(new BIHStackData(this, sceneMin, sceneMax));
        stackloop:
        while (stack.size() > 0) {

            BIHStackData data = stack.remove(stack.size() - 1);
            BIHNode node = data.node;
            float tMin = data.min,
                    tMax = data.max;

            if (tMax < tMin) {
                continue;
            }

            leafloop:
            while (node.axis != 3) { // while node is not a leaf
                int a = node.axis;

                // find the origin and direction value for the given axis
                float origin = origins[a];
                float invDirection = invDirections[a];

                float tNearSplit, tFarSplit;
                BIHNode nearNode, farNode;

                tNearSplit = (node.leftPlane - origin) * invDirection;
                tFarSplit = (node.rightPlane - origin) * invDirection;
                nearNode = node.left;
                farNode = node.right;

                if (invDirection < 0) {
                    float tmpSplit = tNearSplit;
                    tNearSplit = tFarSplit;
                    tFarSplit = tmpSplit;

                    BIHNode tmpNode = nearNode;
                    nearNode = farNode;
                    farNode = tmpNode;
                }

                if (tMin > tNearSplit && tMax < tFarSplit) {
                    continue stackloop;
                }

                if (tMin > tNearSplit) {
                    tMin = max(tMin, tFarSplit);
                    node = farNode;
                } else if (tMax < tFarSplit) {
                    tMax = min(tMax, tNearSplit);
                    node = nearNode;
                } else {
                    stack.add(new BIHStackData(farNode, max(tMin, tFarSplit), tMax));
                    tMax = min(tMax, tNearSplit);
                    node = nearNode;
                }
            }

//            if ( (node.rightIndex - node.leftIndex) > minTrisPerNode){
//                // on demand subdivision
//                node.subdivide();
//                stack.add(new BIHStackData(node, tMin, tMax));
//                continue stackloop;
//            }

            // a leaf
            for (int i = node.leftIndex; i <= node.rightIndex; i++) {
                tree.getTriangle(i, v1, v2, v3);

                float t = r.intersects(v1, v2, v3);
                if (!Float.isInfinite(t)) {
                    if (worldMatrix != null) {
                        worldMatrix.mult(v1, v1);
                        worldMatrix.mult(v2, v2);
                        worldMatrix.mult(v3, v3);
                        float t_world = new Ray(o, d).intersects(v1, v2, v3);
                        t = t_world;
                    }

                    Vector3f contactNormal = Triangle.computeTriangleNormal(v1, v2, v3, null);
                    Vector3f contactPoint = new Vector3f(d).multLocal(t).addLocal(o);
                    float worldSpaceDist = o.distance(contactPoint);

                    CollisionResult cr = new CollisionResult(contactPoint, worldSpaceDist);
                    cr.setContactNormal(contactNormal);
                    cr.setTriangleIndex(tree.getTriangleIndex(i));
                    results.addCollision(cr);
                    cols++;
                }
            }
        }
        vars.release();
        r.setOrigin(o);
        r.setDirection(d);

        return cols;
    }
}
