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

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.collision.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.Line;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * The class that displays either wires between the bones' heads if no length
 * data is supplied and full bones' shapes otherwise.
 */
public class ArmatureNode extends Node {

    /**
     * The armature to be displayed.
     */
    private Armature armature;
    /**
     * The map between the bone index and its length.
     */
    private Map<Joint, Geometry[]> jointToGeoms = new HashMap<>();
    private Map<Geometry, Joint> geomToJoint = new HashMap<>();
    private Joint selectedJoint = null;
    private Vector3f tmpStart = new Vector3f();
    private Vector3f tmpEnd = new Vector3f();
    ColorRGBA selectedColor = ColorRGBA.Orange;
    ColorRGBA selectedColorJ = ColorRGBA.Yellow;
    ;//new ColorRGBA(0.2f, 1f, 1.0f, 1.0f);
    ColorRGBA baseColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);


    /**
     * Creates a wire with bone lengths data. If the data is supplied then the
     * wires will show each full bone (from head to tail).
     *
     * @param armature the armature that will be shown
     */
    public ArmatureNode(Armature armature, Node joints, Node wires, Node outlines, List<Joint> deformingJoints) {
        this.armature = armature;

        for (Joint joint : armature.getRoots()) {
            createSkeletonGeoms(joint, joints, wires, outlines, deformingJoints);
        }
        this.updateModelBound();

    }

    protected final void createSkeletonGeoms(Joint joint, Node joints, Node wires, Node outlines, List<Joint> deformingJoints) {
        Vector3f start = joint.getModelTransform().getTranslation().clone();
        Vector3f end = null;

        //One child only, the bone direction is from the parent joint to the child joint.
        if (joint.getChildren().size() == 1) {
            end = joint.getChildren().get(0).getModelTransform().getTranslation().clone();
        }

        boolean deforms = deformingJoints.contains(joint);

        Geometry jGeom = new Geometry(joint.getName() + "Joint", new JointShape());
        jGeom.setLocalTranslation(start);
        attach(joints, deforms, jGeom);
        Geometry bGeom = null;
        Geometry bGeomO = null;
        if (end != null) {
            bGeom = new Geometry(joint.getName() + "Bone", new Line(start, end));
            setColor(bGeom, baseColor);
            geomToJoint.put(bGeom, joint);
            bGeomO = new Geometry(joint.getName() + "BoneOutline", new Line(start, end));
            setColor(bGeomO, ColorRGBA.White);
            bGeom.setUserData("start", wires.getWorldTransform().transformVector(start, start));
            bGeom.setUserData("end", wires.getWorldTransform().transformVector(end, end));
            bGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
            attach(wires, deforms, bGeom);
            attach(outlines, deforms, bGeomO);
        }

        jointToGeoms.put(joint, new Geometry[]{jGeom, bGeom, bGeomO});

        for (Joint child : joint.getChildren()) {
            createSkeletonGeoms(child, joints, wires, outlines, deformingJoints);
        }
    }

    private void attach(Node parent, boolean deforms, Geometry geom) {
        if (deforms) {
            parent.attachChild(geom);
        } else {
            ((Node) parent.getChild(0)).attachChild(geom);
        }
    }

    protected Joint select(Geometry g) {
        if (g == null) {
            resetSelection();
            return null;
        }
        Joint j = geomToJoint.get(g);
        if (j != null) {
            if (selectedJoint == j) {
                return null;
            }
            resetSelection();
            selectedJoint = j;
            Geometry[] geomArray = jointToGeoms.get(selectedJoint);
            setColor(geomArray[0], selectedColorJ);
            setColor(geomArray[1], selectedColor);
            setColor(geomArray[2], baseColor);
            return j;
        }
        return null;
    }

    private void resetSelection() {
        if (selectedJoint == null) {
            return;
        }
        Geometry[] geoms = jointToGeoms.get(selectedJoint);
        setColor(geoms[0], ColorRGBA.White);
        setColor(geoms[1], baseColor);
        setColor(geoms[2], ColorRGBA.White);
        selectedJoint = null;
    }

    protected Joint getSelectedJoint() {
        return selectedJoint;
    }


    protected final void updateSkeletonGeoms(Joint joint) {
        Geometry[] geoms = jointToGeoms.get(joint);
        if (geoms != null) {
            Geometry jGeom = geoms[0];
            jGeom.setLocalTranslation(joint.getModelTransform().getTranslation());
            Geometry bGeom = geoms[1];
            if (bGeom != null) {
                tmpStart.set(joint.getModelTransform().getTranslation());
                boolean hasEnd = false;
                if (joint.getChildren().size() == 1) {
                    tmpEnd.set(joint.getChildren().get(0).getModelTransform().getTranslation());
                    hasEnd = true;
                }
                if (hasEnd) {
                    updateBoneMesh(bGeom);
                    Geometry bGeomO = geoms[2];
                    updateBoneMesh(bGeomO);
                    Vector3f start = bGeom.getUserData("start");
                    Vector3f end = bGeom.getUserData("end");
                    bGeom.setUserData("start", bGeom.getParent().getWorldTransform().transformVector(tmpStart, start));
                    bGeom.setUserData("end", bGeom.getParent().getWorldTransform().transformVector(tmpEnd, end));
                }
            }
        }

        for (Joint child : joint.getChildren()) {
            updateSkeletonGeoms(child);
        }
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        if (!(other instanceof Ray)) {
            return 0;
        }
        int nbCol = 0;
        for (Geometry g : geomToJoint.keySet()) {
            float len = MathUtils.raySegmentShortestDistance((Ray) other, (Vector3f) g.getUserData("start"), (Vector3f) g.getUserData("end"));
            if (len > 0 && len < 0.1f) {
                CollisionResult res = new CollisionResult();
                res.setGeometry(g);
                results.addCollision(res);
                nbCol++;
            }
        }
        return nbCol;
    }

    private void updateBoneMesh(Geometry bGeom) {
        VertexBuffer pos = bGeom.getMesh().getBuffer(VertexBuffer.Type.Position);
        FloatBuffer fb = (FloatBuffer) pos.getData();
        fb.rewind();
        fb.put(new float[]{tmpStart.x, tmpStart.y, tmpStart.z,
                tmpEnd.x, tmpEnd.y, tmpEnd.z,});
        pos.updateData(fb);

        bGeom.updateModelBound();
    }

    private void setColor(Geometry g, ColorRGBA color) {
        float[] colors = new float[g.getMesh().getVertexCount() * 4];
        for (int i = 0; i < g.getMesh().getVertexCount() * 4; i += 4) {
            colors[i] = color.r;
            colors[i + 1] = color.g;
            colors[i + 2] = color.b;
            colors[i + 3] = color.a;
        }
        VertexBuffer colorBuff = g.getMesh().getBuffer(VertexBuffer.Type.Color);
        if (colorBuff == null) {
            g.getMesh().setBuffer(VertexBuffer.Type.Color, 4, colors);
        } else {
            FloatBuffer cBuff = (FloatBuffer) colorBuff.getData();
            cBuff.rewind();
            cBuff.put(colors);
            colorBuff.updateData(cBuff);
        }
    }

    /**
     * The method updates the geometry according to the positions of the bones.
     */
    public void updateGeometry() {
        armature.update();
        for (Joint joint : armature.getRoots()) {
            updateSkeletonGeoms(joint);
        }
    }
}
