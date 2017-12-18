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

import com.jme3.animation.*;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * The class that creates a mesh to display how bones behave. If it is supplied
 * with the bones' lengths it will show exactly how the bones look like on the
 * scene. If not then only connections between each bone heads will be shown.
 */
public class ArmatureDebugger extends BatchNode {

    /**
     * The lines of the bones or the wires between their heads.
     */
    private ArmatureBone bones;

    private Armature armature;
    /**
     * The dotted lines between a bone's tail and the had of its children. Not
     * available if the length data was not provided.
     */
    private ArmatureInterJointsWire interJointWires;
    private Geometry wires;
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
    public ArmatureDebugger(String name, Armature armature, boolean guessJointsOrientation) {
        super(name);
        this.armature = armature;
//        armature.reset();
        armature.update();
        //Joints have no length we want to display the as bones so we compute their length
        Map<Integer, Float> bonesLength = new HashMap<Integer, Float>();

        for (Joint joint : armature.getRoots()) {
            computeLength(joint, bonesLength, armature);
        }

        bones = new ArmatureBone(armature, bonesLength, guessJointsOrientation);

        this.attachChild(bones);

        interJointWires = new ArmatureInterJointsWire(armature, bonesLength, guessJointsOrientation);
        wires = new Geometry(name + "_interwires", interJointWires);
        this.attachChild(wires);
    }

    protected void initialize(AssetManager assetManager) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/fakeLighting.j3md");
        mat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 1));
        setMaterial(mat);

        Material matWires = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWires.setColor("Color", ColorRGBA.Black);
        wires.setMaterial(matWires);
        //wires.setQueueBucket(RenderQueue.Bucket.Transparent);
//        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat2.setBoolean("VertexColor", true);
//        bones.setMaterial(mat2);
//        batch();
    }

    @Override
    public final void setMaterial(Material material) {
        if (batches.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                children.get(i).setMaterial(material);
            }
        } else {
            super.setMaterial(material);
        }
    }

    public Armature getArmature() {
        return armature;
    }


    private void computeLength(Joint joint, Map<Integer, Float> jointsLength, Armature armature) {
        if (joint.getChildren().isEmpty()) {
            if (joint.getParent() != null) {
                jointsLength.put(armature.getJointIndex(joint), jointsLength.get(armature.getJointIndex(joint.getParent())) * 0.75f);
            } else {
                jointsLength.put(armature.getJointIndex(joint), 0.1f);
            }
        } else {
            float length = Float.MAX_VALUE;
            for (Joint child : joint.getChildren()) {
                float len = joint.getModelTransform().getTranslation().subtract(child.getModelTransform().getTranslation()).length();
                if (len < length) {
                    length = len;
                }
            }
            jointsLength.put(armature.getJointIndex(joint), length);
            for (Joint child : joint.getChildren()) {
                computeLength(child, jointsLength, armature);
            }
        }
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        bones.updateGeometry();
        if (interJointWires != null) {
            interJointWires.updateGeometry();
        }
    }

    ColorRGBA selectedColor = ColorRGBA.Orange;
    ColorRGBA baseColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);

    protected Joint select(Geometry g) {
        Node oldNode = bones.getSelectedNode();
        Joint b = bones.select(g);
        if (b == null) {
            return null;
        }
        if (oldNode != null) {
            markSelected(oldNode, false);
        }
        markSelected(bones.getSelectedNode(), true);
        return b;
    }

    /**
     * @return the armature wires
     */
    public ArmatureBone getBoneShapes() {
        return bones;
    }

    /**
     * @return the dotted line between bones (can be null)
     */
    public ArmatureInterJointsWire getInterJointWires() {
        return interJointWires;
    }

    protected void markSelected(Node n, boolean selected) {
        ColorRGBA c = baseColor;
        if (selected) {
            c = selectedColor;
        }
        for (Spatial spatial : n.getChildren()) {
            if (spatial instanceof Geometry) {
                Geometry geom = (Geometry) spatial;

                Geometry batch = (Geometry) getChild(getName() + "-batch0");
                VertexBuffer vb = batch.getMesh().getBuffer(VertexBuffer.Type.Color);
                FloatBuffer color = (FloatBuffer) vb.getData();
                //  System.err.println(getName() + "." + geom.getName() + " index " + getGeometryStartIndex(geom) * 4 + "/" + color.limit());

                color.position(getGeometryStartIndex(geom) * 4);

                for (int i = 0; i < geom.getVertexCount(); i++) {
                    color.put(c.r).put(c.g).put(c.b).put(c.a);
                }
                color.rewind();
                vb.updateData(color);
            }
        }
    }
}