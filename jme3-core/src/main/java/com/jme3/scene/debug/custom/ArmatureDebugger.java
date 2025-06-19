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
import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

import java.util.List;

/**
 * The class that creates a mesh to display how bones behave. If it is supplied
 * with the bones' lengths it will show exactly how the bones look like on the
 * scene. If not then only connections between each bone heads will be shown.
 */
public class ArmatureDebugger extends Node {

    /**
     * The node responsible for rendering the bones/wires and their outlines.
     */
    private ArmatureNode armatureNode;
    /**
     * The {@link Armature} instance being debugged.
     */
    private Armature armature;
    /**
     * A node containing all {@link Geometry} objects representing the joint points.
     */
    private Node joints;
    /**
     * A node containing all {@link Geometry} objects representing the bone outlines.
     */
    private Node outlines;
    /**
     * A node containing all {@link Geometry} objects representing the bone wires/lines.
     */
    private Node wires;
    /**
     * The dotted lines between a bone's tail and the had of its children. Not
     * available if the length data was not provided.
     */
    private ArmatureInterJointsWire interJointWires;
    /**
     * Default constructor for `ArmatureDebugger`.
     * Use {@link #ArmatureDebugger(String, Armature, List)} for a functional instance.
     */
    public ArmatureDebugger() {
    }

    /**
     * Convenience constructor that creates an {@code ArmatureDebugger} and immediately
     * initializes its materials based on the provided {@code AssetManager}
     * and {@code SkinningControl}.
     *
     * @param assetManager The {@link AssetManager} used to load textures and materials
     *                     for the debug visualization.
     * @param skControl    The {@link SkinningControl} from which to extract the
     *                     {@link Armature} and its associated joints.
     */
    public ArmatureDebugger(AssetManager assetManager, SkinningControl skControl) {
        this(null, skControl.getArmature(), skControl.getArmature().getJointList());
        initialize(assetManager, null);
    }

    /**
     * Creates an `ArmatureDebugger` instance without explicit bone length data.
     * In this configuration, the visual representation will consist of wires
     * connecting the bone heads, and points representing the bone heads.
     * No dotted lines for inter-bone connections will be visible.
     *
     * @param name            The name of this debugger's root node.
     * @param armature        The {@link Armature} to be visualized.
     * @param deformingJoints A {@link List} of {@link Joint} objects that are
     *                        considered deforming joints.
     */
    public ArmatureDebugger(String name, Armature armature, List<Joint> deformingJoints) {
        super(name);
        this.armature = armature;
        // Ensure the armature's world transforms are up-to-date before visualization.
        armature.update();

        // Initialize the main container nodes for different visual elements.
        joints = new Node("joints");
        outlines = new Node("outlines");
        wires = new Node("bones");
        this.attachChild(joints);
        this.attachChild(outlines);
        this.attachChild(wires);

        // Create child nodes specifically for non-deforming joints' visualization
        joints.attachChild(new Node("NonDeformingJoints"));
        outlines.attachChild(new Node("NonDeformingOutlines"));
        wires.attachChild(new Node("NonDeformingWires"));

        Node outlineDashed = new Node("DashedOutlines");
        outlineDashed.attachChild(new Node("DashedNonDeformingOutlines"));
        outlines.attachChild(outlineDashed);

        Node wiresDashed = new Node("DashedWires");
        wiresDashed.attachChild(new Node("DashedNonDeformingWires"));
        wires.attachChild(wiresDashed);

        // Initialize the core ArmatureNode which handles the actual mesh generation.
        armatureNode = new ArmatureNode(armature, joints, wires, outlines, deformingJoints);
        this.attachChild(armatureNode);

        // By default, non-deforming joints are hidden.
        displayNonDeformingJoint(false);
    }

    /**
     * Sets the visibility of non-deforming joints and their associated outlines and wires.
     *
     * @param display `true` to make non-deforming joints visible, `false` to hide them.
     */
    public void displayNonDeformingJoint(boolean display) {
        CullHint cullHint = display ? CullHint.Dynamic : CullHint.Always;

        joints.getChild(0).setCullHint(cullHint);
        outlines.getChild(0).setCullHint(cullHint);
        wires.getChild(0).setCullHint(cullHint);

        ((Node) outlines.getChild(1)).getChild(0).setCullHint(cullHint);
        ((Node) wires.getChild(1)).getChild(0).setCullHint(cullHint);
    }

    /**
     * Initializes the materials and camera for the debugger's visual components.
     * This method should be called after the `ArmatureDebugger` is added to a scene graph
     * and an {@link AssetManager} and {@link Camera} are available.
     *
     * @param assetManager The {@link AssetManager} to load textures and materials.
     * @param camera       The scene's primary {@link Camera}, used by the `ArmatureNode`
     * for billboard rendering of joint points.
     */
    public void initialize(AssetManager assetManager, Camera camera) {

        armatureNode.setCamera(camera);

        // Material for joint points (billboard dots).
        Material matJoints = getJointMaterial(assetManager);
        joints.setQueueBucket(RenderQueue.Bucket.Translucent);
        joints.setMaterial(matJoints);

        // Material for bone wires/lines (unshaded, vertex colored).
        Material matWires = getUnshadedMaterial(assetManager);
        wires.setMaterial(matWires);

        // Material for dashed wires ("DashedLine.j3md" shader).
        Material matWires2 = getDashedMaterial(assetManager);
        wires.getChild(1).setMaterial(matWires2);

        // Material for bone outlines (unshaded, vertex colored).
        Material matOutline = getUnshadedMaterial(assetManager);
        outlines.setMaterial(matOutline);

        // Material for dashed outlines ("DashedLine.j3md" shader).
        Material matOutline2 = getDashedMaterial(assetManager);
        outlines.getChild(1).setMaterial(matOutline2);
    }

    private Material getJointMaterial(AssetManager asm) {
        Material mat = new Material(asm, "Common/MatDefs/Misc/Billboard.j3md");
        Texture tex = asm.loadTexture("Common/Textures/dot.png");
        mat.setTexture("Texture", tex);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return mat;
    }

    private Material getUnshadedMaterial(AssetManager asm) {
        Material mat = new Material(asm, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setBoolean("VertexColor", true);
        mat.getAdditionalRenderState().setDepthTest(false);
        return mat;
    }

    private Material getDashedMaterial(AssetManager asm) {
        Material mat = new Material(asm, "Common/MatDefs/Misc/DashedLine.j3md");
        mat.getAdditionalRenderState().setDepthTest(false);
        return mat;
    }

    /**
     * Returns the {@link Armature} instance associated with this debugger.
     *
     * @return The {@link Armature} being debugged.
     */
    public Armature getArmature() {
        return armature;
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        armatureNode.updateGeometry();
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        return armatureNode.collideWith(other, results);
    }

    /**
     * Selects and returns the {@link Joint} associated with a given {@link Geometry}.
     * This is an internal helper method, likely used for picking operations.
     *
     * @param geo The {@link Geometry} representing a part of a joint.
     * @return The {@link Joint} corresponding to the geometry, or `null` if not found.
     */
    protected Joint select(Geometry geo) {
        return armatureNode.select(geo);
    }

    /**
     * Returns the {@link ArmatureNode} which is responsible for generating and
     * managing the visual mesh of the bones and wires.
     *
     * @return The {@link ArmatureNode} instance.
     */
    public ArmatureNode getBoneShapes() {
        return armatureNode;
    }

    /**
     * Returns the {@link ArmatureInterJointsWire} instance, which represents the
     * dotted lines connecting a bone's tail to the head of its children.
     * This will be `null` if the debugger was created without bone length data.
     *
     * @return The {@link ArmatureInterJointsWire} instance, or `null` if not present.
     */
    public ArmatureInterJointsWire getInterJointWires() {
        return interJointWires;
    }
}
