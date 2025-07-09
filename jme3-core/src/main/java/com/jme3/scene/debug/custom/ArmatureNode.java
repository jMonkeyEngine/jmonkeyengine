/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.scene.debug.custom;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.ColorRGBA;
import com.jme3.math.MathUtils;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Line;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders an {@link Armature} for debugging purposes. It can display either
 * wires connecting the heads of bones (if no length data is available) or
 * full bone shapes (from head to tail) when length data is supplied.
 */
public class ArmatureNode extends Node {

    /**
     * The size of the picking box in pixels for joint selection.
     */
    public static final float PIXEL_BOX = 10f;
    /**
     * The armature to be displayed.
     */
    private final Armature armature;
    /**
     * Maps a {@link Joint} to its corresponding {@link Geometry} array.
     * The array typically contains [jointGeometry, boneWireGeometry, boneOutlineGeometry].
     */
    private final Map<Joint, Geometry[]> jointToGeoms = new HashMap<>();
    /**
     * Maps a {@link Geometry} to its associated {@link Joint}. Used for picking.
     */
    private final Map<Geometry, Joint> geomToJoint = new HashMap<>();
    /**
     * The currently selected joint.
     */
    private Joint selectedJoint = null;

    // Temporary vectors for calculations to avoid repeated allocations
    private final Vector3f tempVec3f = new Vector3f();
    private final Vector2f tempVec2f = new Vector2f();

    // Color constants for rendering
    private static final ColorRGBA selectedColor = ColorRGBA.Orange;
    private static final ColorRGBA selectedColorJoint = ColorRGBA.Yellow;
    private static final ColorRGBA outlineColor = ColorRGBA.LightGray;
    private static final ColorRGBA baseColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);

    /**
     * The camera used for 2D picking calculations.
     */
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

        // Recursively create geometries for all joints and bones in the armature
        for (Joint joint : armature.getRoots()) {
            createSkeletonGeoms(joint, joints, wires, outlines, deformingJoints);
        }
        this.updateModelBound();
    }

    /**
     * Recursively creates the geometries for a given joint and its children.
     *
     * @param joint           The current joint for which to create geometries.
     * @param joints          The node for joint geometries.
     * @param wires           The node for bone wire geometries.
     * @param outlines        The node for bone outline geometries.
     * @param deformingJoints A list of deforming joints.
     */
    protected final void createSkeletonGeoms(Joint joint, Node joints, Node wires, Node outlines, List<Joint> deformingJoints) {
        Vector3f start = joint.getModelTransform().getTranslation().clone();

        Vector3f[] ends = null;
        if (!joint.getChildren().isEmpty()) {
            ends = new Vector3f[joint.getChildren().size()];
            for (int i = 0; i < ends.length; i++) {
                ends[i] = joint.getChildren().get(i).getModelTransform().getTranslation().clone();
            }
        }

        boolean deforms = deformingJoints.contains(joint);

        // Create geometry for the joint head
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
            for (Vector3f end : ends) {
                getWorldTransform().transformVector(end, end);
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

        // Recursively call for children
        for (Joint child : joint.getChildren()) {
            createSkeletonGeoms(child, joints, wires, outlines, deformingJoints);
        }
    }

    /**
     * Sets the camera to be used for 2D picking calculations.
     *
     * @param camera The camera to set.
     */
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

    /**
     * Selects a joint based on its associated geometry.
     * If the selected geometry is already the current selection, no change occurs.
     * Resets the selection if {@code geometry} is null.
     *
     * @param geo The geometry representing the joint or bone to select.
     * @return The newly selected {@link Joint}, or null if no joint was selected or the selection was reset.
     */
    protected Joint select(Geometry geo) {
        if (geo == null) {
            resetSelection();
            return null;
        }
        Joint jointToSelect = geomToJoint.get(geo);
        if (jointToSelect != null) {
            if (selectedJoint == jointToSelect) {
                return null;
            }
            resetSelection();
            selectedJoint = jointToSelect;
            Geometry[] geomArray = jointToGeoms.get(selectedJoint);
            // Color the joint head
            setColor(geomArray[0], selectedColorJoint);

            // Color the bone wire
            if (geomArray[1] != null) {
                setColor(geomArray[1], selectedColor);
            }

            // Restore outline color if present (as it's often the base color when bone is selected)
            if (geomArray[2] != null) {
                setColor(geomArray[2], baseColor);
            }
            return jointToSelect;
        }
        return null;
    }

    /**
     * Resets the color of the currently selected joint and bone geometries to their default colors
     * and clears the {@code selectedJoint}.
     */
    private void resetSelection() {
        if (selectedJoint == null) {
            return;
        }
        Geometry[] geoms = jointToGeoms.get(selectedJoint);
        // Reset joint head color
        setColor(geoms[0], ColorRGBA.White);

        // Reset bone wire color (depends on whether it has an outline)
        if (geoms[1] != null) {
            setColor(geoms[1], geoms[2] == null ? outlineColor : baseColor);
        }

        // Reset bone outline color
        if (geoms[2] != null) {
            setColor(geoms[2], outlineColor);
        }
        selectedJoint = null;
    }

    /**
     * Returns the currently selected joint.
     *
     * @return The {@link Joint} that is currently selected, or null if no joint is selected.
     */
    protected Joint getSelectedJoint() {
        return selectedJoint;
    }

    /**
     * Updates the geometries associated with a given joint and its children to reflect their
     * current model transforms. This method is called recursively.
     *
     * @param joint The joint to update.
     */
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
                    for (Vector3f end : ends) {
                        getWorldTransform().transformVector(end, end);
                    }
                    bGeom.setUserData("end", ends);
                }
            }
        }

        // Recursively update children
        for (Joint child : joint.getChildren()) {
            updateSkeletonGeoms(child);
        }
    }

    /**
     * Sets the color of the head geometry for a specific joint.
     *
     * @param joint The joint whose head color is to be set.
     * @param color The new color for the joint head.
     */
    public void setHeadColor(Joint joint, ColorRGBA color) {
        Geometry[] geomArray = jointToGeoms.get(joint);
        setColor(geomArray[0], color);
    }

    /**
     * Sets the color of all joint head geometries.
     *
     * @param color The new color for all joint heads.
     */
    public void setHeadColor(ColorRGBA color) {
        for (Geometry[] geomArray : jointToGeoms.values()) {
            setColor(geomArray[0], color);
        }
    }

    /**
     * Sets the color of all bone line geometries.
     *
     * @param color The new color for all bone lines.
     */
    public void setLineColor(ColorRGBA color) {
        for (Geometry[] geomArray : jointToGeoms.values()) {
            if (geomArray[1] != null) {
                setColor(geomArray[1], color);
            }
        }
    }

    /**
     * Performs a 2D pick operation to find joints or bones near the given cursor position.
     * This method primarily checks for joint heads within a {@link #PIXEL_BOX} box
     * around the cursor, and then checks for bone wires.
     *
     * @param cursor  The 2D screen coordinates of the pick ray origin.
     * @param results The {@link CollisionResults} to store the pick results.
     * @return The number of collisions found.
     */
    public int pick(Vector2f cursor, CollisionResults results) {
        if (camera == null) {
            return 0;
        }

        int collisions = 0;
        for (Geometry geo : geomToJoint.keySet()) {
            if (geo.getMesh() instanceof JointShape) {
                camera.getScreenCoordinates(geo.getWorldTranslation(), tempVec3f);
                if (cursor.x <= tempVec3f.x + PIXEL_BOX && cursor.x >= tempVec3f.x - PIXEL_BOX
                        && cursor.y <= tempVec3f.y + PIXEL_BOX && cursor.y >= tempVec3f.y - PIXEL_BOX) {
                    CollisionResult res = new CollisionResult();
                    res.setGeometry(geo);
                    results.addCollision(res);
                    collisions++;
                }
            }
        }
        return collisions;
    }

    /**
     * Collides this {@code ArmatureNode} with a {@link Collidable} object, typically a {@link Ray}.
     * It prioritizes 2D picking for joint heads and then performs a distance-based check for bone wires.
     *
     * @param other   The {@link Collidable} object to collide with.
     * @param results The {@link CollisionResults} to store the collision information.
     * @return The number of collisions found.
     */
    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        if (!(other instanceof Ray) || camera == null) {
            return 0;
        }

        // First, try a 2D pick for joint heads
        camera.getScreenCoordinates(((Ray) other).getOrigin(), tempVec3f);
        tempVec2f.x = tempVec3f.x;
        tempVec2f.y = tempVec3f.y;
        int hitCount = pick(tempVec2f, results);

        // If 2D pick found hits, return them. Otherwise, proceed with bone wire collision.
        if (hitCount > 0) {
            return hitCount;
        }

        // Check for bone wire collisions
        for (Geometry g : geomToJoint.keySet()) {
            if (g.getMesh() instanceof JointShape) {
                // Skip joint heads, already handled by 2D pick
                continue;
            }

            Vector3f start = g.getUserData("start");
            Vector3f[] ends = g.getUserData("end");

            for (Vector3f end : ends) {
                // Calculate the shortest distance from ray to bone segment
                float dist = MathUtils.raySegmentShortestDistance((Ray) other, start, end, camera);
                if (dist > 0 && dist < PIXEL_BOX) {
                    CollisionResult res = new CollisionResult();
                    res.setGeometry(g);
                    results.addCollision(res);
                    hitCount++;
                }
            }
        }
        return hitCount;
    }

    /**
     * Updates the mesh of a bone geometry (either {@link ArmatureInterJointsWire} or {@link Line})
     * with new start and end points.
     *
     * @param geom  The bone geometry whose mesh needs updating.
     * @param start The new starting point of the bone.
     * @param ends  The new ending points of the bone (can be multiple for {@link ArmatureInterJointsWire}).
     */
    private void updateBoneMesh(Geometry geom, Vector3f start, Vector3f[] ends) {
        if (geom.getMesh() instanceof ArmatureInterJointsWire) {
            ((ArmatureInterJointsWire) geom.getMesh()).updatePoints(start, ends);
        } else if (geom.getMesh() instanceof Line) {
            ((Line) geom.getMesh()).updatePoints(start, ends[0]);
        }
        geom.updateModelBound();
    }

    /**
     * Sets the color of a given geometry's vertex buffer.
     * This method creates a new color buffer or updates an existing one with the specified color.
     *
     * @param geo   The geometry whose color is to be set.
     * @param color The {@link ColorRGBA} to apply.
     */
    private void setColor(Geometry geo, ColorRGBA color) {
        Mesh mesh = geo.getMesh();
        int vertexCount = mesh.getVertexCount();

        float[] colors = new float[vertexCount * 4];
        for (int i = 0; i < colors.length; i += 4) {
            colors[i] = color.r;
            colors[i + 1] = color.g;
            colors[i + 2] = color.b;
            colors[i + 3] = color.a;
        }

        VertexBuffer colorBuff = geo.getMesh().getBuffer(VertexBuffer.Type.Color);
        if (colorBuff == null) {
            // If no color buffer exists, create a new one
            geo.getMesh().setBuffer(VertexBuffer.Type.Color, 4, colors);
        } else {
            // If a color buffer exists, update its data
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
