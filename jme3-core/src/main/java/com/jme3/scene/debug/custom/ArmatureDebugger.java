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
     * The lines of the bones or the wires between their heads.
     */
    private ArmatureNode armatureNode;

    private Armature armature;

    private Node joints;
    private Node outlines;
    private Node wires;
    /**
     * The dotted lines between a bone's tail and the had of its children. Not
     * available if the length data was not provided.
     */
    private ArmatureInterJointsWire interJointWires;

    public ArmatureDebugger() {
    }

    /**
     * Creates a debugger with no length data. The wires will be a connection
     * between the bones' heads only. The points will show the bones' heads only
     * and no dotted line of inter bones connection will be visible.
     *
     * @param name     the name of the debugger's node
     * @param armature the armature that will be shown
     * @param deformingJoints a list of joints
     */
    public ArmatureDebugger(String name, Armature armature, List<Joint> deformingJoints) {
        super(name);
        this.armature = armature;
        armature.update();

        joints = new Node("joints");
        outlines = new Node("outlines");
        wires = new Node("bones");
        this.attachChild(joints);
        this.attachChild(outlines);
        this.attachChild(wires);
        Node ndJoints = new Node("non deforming Joints");
        Node ndOutlines = new Node("non deforming Joints outlines");
        Node ndWires = new Node("non deforming Joints wires");
        joints.attachChild(ndJoints);
        outlines.attachChild(ndOutlines);
        wires.attachChild(ndWires);
        Node outlineDashed = new Node("Outlines Dashed");
        Node wiresDashed = new Node("Wires Dashed");
        wiresDashed.attachChild(new Node("dashed non defrom"));
        outlineDashed.attachChild(new Node("dashed non defrom"));
        outlines.attachChild(outlineDashed);
        wires.attachChild(wiresDashed);

        armatureNode = new ArmatureNode(armature, joints, wires, outlines, deformingJoints);

        this.attachChild(armatureNode);

        displayNonDeformingJoint(false);
    }

    public void displayNonDeformingJoint(boolean display) {
        joints.getChild(0).setCullHint(display ? CullHint.Dynamic : CullHint.Always);
        outlines.getChild(0).setCullHint(display ? CullHint.Dynamic : CullHint.Always);
        wires.getChild(0).setCullHint(display ? CullHint.Dynamic : CullHint.Always);
        ((Node) outlines.getChild(1)).getChild(0).setCullHint(display ? CullHint.Dynamic : CullHint.Always);
        ((Node) wires.getChild(1)).getChild(0).setCullHint(display ? CullHint.Dynamic : CullHint.Always);
    }

    public void initialize(AssetManager assetManager, Camera camera) {

        armatureNode.setCamera(camera);

        Material matJoints = new Material(assetManager, "Common/MatDefs/Misc/Billboard.j3md");
        Texture t = assetManager.loadTexture("Common/Textures/dot.png");
        matJoints.setTexture("Texture", t);
        matJoints.getAdditionalRenderState().setDepthTest(false);
        matJoints.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        joints.setQueueBucket(RenderQueue.Bucket.Translucent);
        joints.setMaterial(matJoints);

        Material matWires = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWires.setBoolean("VertexColor", true);
        matWires.getAdditionalRenderState().setLineWidth(1f);
        wires.setMaterial(matWires);

        Material matOutline = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matOutline.setBoolean("VertexColor", true);
        matOutline.getAdditionalRenderState().setLineWidth(1f);
        outlines.setMaterial(matOutline);

        Material matOutline2 = new Material(assetManager, "Common/MatDefs/Misc/DashedLine.j3md");
        matOutline2.getAdditionalRenderState().setLineWidth(1);
        outlines.getChild(1).setMaterial(matOutline2);

        Material matWires2 = new Material(assetManager, "Common/MatDefs/Misc/DashedLine.j3md");
        matWires2.getAdditionalRenderState().setLineWidth(1);
        wires.getChild(1).setMaterial(matWires2);

    }

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

    protected Joint select(Geometry g) {
        return armatureNode.select(g);
    }

    /**
     * @return the armature wires
     */
    public ArmatureNode getBoneShapes() {
        return armatureNode;
    }

    /**
     * @return the dotted line between bones (can be null)
     */
    public ArmatureInterJointsWire getInterJointWires() {
        return interJointWires;
    }
}