package com.jme3.scene.debug.custom;

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

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.collision.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
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

    public static final float PIXEL_BOX = 10f;
    /**
     * The armature to be displayed.
     */
    final private Armature armature;
    /**
     * The map between the bone index and its length.
     */
    final private Map<Joint, Geometry[]> jointToGeoms = new HashMap<>();
    final private Map<Geometry, Joint> geomToJoint = new HashMap<>();
    private Joint selectedJoint = null;
    final private Vector3f tmp = new Vector3f();
    final private Vector2f tmpv2 = new Vector2f();
    private final static ColorRGBA selectedColor = ColorRGBA.Orange;
    private final static ColorRGBA selectedColorJ = ColorRGBA.Yellow;
    private final static ColorRGBA outlineColor = ColorRGBA.LightGray;
    private final static ColorRGBA baseColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);

    private Camera camera;


    /**
     * Creates a wire with bone lengths data. If the data is supplied then the
     * wires will show each full bone (from head to tail).
     *
     * @param armature the armature that will be shown
     * @param joints the Node to visualize joints
     * @param wires the Node to visualize wires
     * @param outlines the Node to visualize outlines
     * @param deformingJoints a list of joints
     */
    public ArmatureNode(Armature armature, Node joints, Node wires, Node outlines, List<Joint> deformingJoints) {
        this.armature = armature;

        Geometry origin = new Geometry("Armature Origin", new JointShape());
        setColor(origin, ColorRGBA.Green);
        attach(joints, true, origin);

        for (Joint joint : armature.getRoots()) {
            createSkeletonGeoms(joint, joints, wires, outlines, deformingJoints);
        }
        this.updateModelBound();

    }

    protected final void createSkeletonGeoms(Joint joint, Node joints, Node wires, Node outlines, List<Joint> deformingJoints) {
        Vector3f start = joint.getModelTransform().getTranslation().clone();

        Vector3f[] ends = null;
        if (!joint.getChildren().isEmpty()) {
            ends = new Vector3f[joint.getChildren().size()];
        }

        for (int i = 0; i < joint.getChildren().size(); i++) {
            ends[i] = joint.getChildren().get(i).getModelTransform().getTranslation().clone();
        }

        boolean deforms = deformingJoints.contains(joint);

        Geometry jGeom = new Geometry(joint.getName() + "Joint", new JointShape());
        jGeom.setLocalTranslation(start);
        attach(joints, deforms, jGeom);
        Geometry bGeom = null;
        Geometry bGeomO = null;
        if (ends == null) {
            geomToJoint.put(jGeom, joint);
        } else {
            Mesh m = null;
            Mesh mO = null;
            Node wireAttach = wires;
            Node outlinesAttach = outlines;
            if (ends.length == 1) {
                m = new Line(start, ends[0]);
                mO = new Line(start, ends[0]);
            } else {
                m = new ArmatureInterJointsWire(start, ends);
                mO = new ArmatureInterJointsWire(start, ends);
                wireAttach = (Node) wires.getChild(1);
                outlinesAttach = null;
            }
            bGeom = new Geometry(joint.getName() + "Bone", m);
            setColor(bGeom, outlinesAttach == null ? outlineColor : baseColor);
            geomToJoint.put(bGeom, joint);
            bGeom.setUserData("start", getWorldTransform().transformVector(start, start));
            for (int i = 0; i < ends.length; i++) {
                getWorldTransform().transformVector(ends[i], ends[i]);
            }
            bGeom.setUserData("end", ends);
            bGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
            attach(wireAttach, deforms, bGeom);
            if (outlinesAttach != null) {
                bGeomO = new Geometry(joint.getName() + "BoneOutline", mO);
                setColor(bGeomO, outlineColor);
                attach(outlinesAttach, deforms, bGeomO);
            }
        }
        jointToGeoms.put(joint, new Geometry[]{jGeom, bGeom, bGeomO});

        for (Joint child : joint.getChildren()) {
            createSkeletonGeoms(child, joints, wires, outlines, deformingJoints);
        }
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
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

            if (geomArray[1] != null) {
                setColor(geomArray[1], selectedColor);
            }

            if (geomArray[2] != null) {
                setColor(geomArray[2], baseColor);
            }
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
        if (geoms[1] != null) {
            setColor(geoms[1], geoms[2] == null ? outlineColor : baseColor);
        }
        if (geoms[2] != null) {
            setColor(geoms[2], outlineColor);
        }
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
                Vector3f start = bGeom.getUserData("start");
                Vector3f[] ends = bGeom.getUserData("end");
                start.set(joint.getModelTransform().getTranslation());
                if (ends != null) {
                    for (int i = 0; i < joint.getChildren().size(); i++) {
                        ends[i].set(joint.getChildren().get(i).getModelTransform().getTranslation());
                    }
                    updateBoneMesh(bGeom, start, ends);
                    Geometry bGeomO = geoms[2];
                    if (bGeomO != null) {
                        updateBoneMesh(bGeomO, start, ends);
                    }
                    bGeom.setUserData("start", getWorldTransform().transformVector(start, start));
                    for (int i = 0; i < ends.length; i++) {
                        getWorldTransform().transformVector(ends[i], ends[i]);
                    }
                    bGeom.setUserData("end", ends);

                }
            }
        }

        for (Joint child : joint.getChildren()) {
            updateSkeletonGeoms(child);
        }
    }

    public int pick(Vector2f cursor, CollisionResults results) {

        for (Geometry g : geomToJoint.keySet()) {
            if (g.getMesh() instanceof JointShape) {
                camera.getScreenCoordinates(g.getWorldTranslation(), tmp);
                if (cursor.x <= tmp.x + PIXEL_BOX && cursor.x >= tmp.x - PIXEL_BOX
                        && cursor.y <= tmp.y + PIXEL_BOX && cursor.y >= tmp.y - PIXEL_BOX) {
                    CollisionResult res = new CollisionResult();
                    res.setGeometry(g);
                    results.addCollision(res);
                }
            }
        }
        return 0;
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        if (!(other instanceof Ray)) {
            return 0;
        }

        // first try a 2D pick;
        camera.getScreenCoordinates(((Ray)other).getOrigin(),tmp);
        tmpv2.x = tmp.x;
        tmpv2.y = tmp.y;
        int nbHit = pick(tmpv2, results);
        if (nbHit > 0) {
            return nbHit;
        }

        for (Geometry g : geomToJoint.keySet()) {
            if (g.getMesh() instanceof JointShape) {
                continue;
            }
            Vector3f start = g.getUserData("start");
            Vector3f[] ends = g.getUserData("end");
            for (int i = 0; i < ends.length; i++) {
                float len = MathUtils.raySegmentShortestDistance((Ray) other, start, ends[i], camera);
                if (len > 0 && len < PIXEL_BOX) {
                    CollisionResult res = new CollisionResult();
                    res.setGeometry(g);
                    results.addCollision(res);
                    nbHit++;
                }
            }
        }
        return nbHit;
    }

    private void updateBoneMesh(Geometry geom, Vector3f start, Vector3f[] ends) {
        if (geom.getMesh() instanceof ArmatureInterJointsWire) {
            ((ArmatureInterJointsWire) geom.getMesh()).updatePoints(start, ends);
        } else if (geom.getMesh() instanceof Line) {
            ((Line) geom.getMesh()).updatePoints(start, ends[0]);
        }
        geom.updateModelBound();
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
