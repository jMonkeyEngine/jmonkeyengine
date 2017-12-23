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
import com.jme3.animation.Bone;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

import java.util.ArrayList;
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

    Node joints;
    Node outlines;
    Node wires;

    /**
     * The dotted lines between a bone's tail and the had of its children. Not
     * available if the length data was not provided.
     */
    private ArmatureInterJointsWire interJointWires;
    //private Geometry wires;
    private List<Bone> selectedJoints = new ArrayList<Bone>();

    public ArmatureDebugger() {
    }

    /**
     * Creates a debugger with no length data. The wires will be a connection
     * between the bones' heads only. The points will show the bones' heads only
     * and no dotted line of inter bones connection will be visible.
     *
     * @param name     the name of the debugger's node
     * @param armature the armature that will be shown
     */
    public ArmatureDebugger(String name, Armature armature) {
        super(name);
        this.armature = armature;
        armature.update();


        joints = new Node("joints");
        outlines = new Node("outlines");
        wires = new Node("bones");
        this.attachChild(joints);
        this.attachChild(outlines);
        this.attachChild(wires);

        armatureNode = new ArmatureNode(armature, joints, wires, outlines);

        this.attachChild(armatureNode);

        //interJointWires = new ArmatureInterJointsWire(armature, bonesLength, guessJointsOrientation);
        //wires = new Geometry(name + "_interwires", interJointWires);
        //       this.attachChild(wires);
    }

    protected void initialize(AssetManager assetManager) {
        Material matWires = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWires.setBoolean("VertexColor", true);
        matWires.getAdditionalRenderState().setLineWidth(3);
        wires.setMaterial(matWires);

        Material matOutline = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matOutline.setBoolean("VertexColor", true);
        //matOutline.setColor("Color", ColorRGBA.White);
        matOutline.getAdditionalRenderState().setLineWidth(5);
        outlines.setMaterial(matOutline);

        Material matJoints = new Material(assetManager, "Common/MatDefs/Misc/Billboard.j3md");
        Texture t = assetManager.loadTexture("Common/Textures/dot.png");
//        matJoints.setBoolean("VertexColor", true);
//        matJoints.setTexture("ColorMap", t);
        matJoints.setTexture("Texture", t);
        matJoints.getAdditionalRenderState().setDepthTest(false);
        matJoints.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        joints.setQueueBucket(RenderQueue.Bucket.Translucent);
        joints.setMaterial(matJoints);

    }

    public Armature getArmature() {
        return armature;
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        armatureNode.updateGeometry();
        if (interJointWires != null) {
            //        interJointWires.updateGeometry();
        }
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