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

import com.jme3.animation.Bone;

import java.util.Map;

import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The class that creates a mesh to display how bones behave. If it is supplied
 * with the bones' lengths it will show exactly how the bones look like on the
 * scene. If not then only connections between each bone heads will be shown.
 */
public class SkeletonDebugger extends BatchNode {

    /**
     * The lines of the bones or the wires between their heads.
     */
    private SkeletonBone bones;

    private Skeleton skeleton;
    /**
     * The dotted lines between a bone's tail and the had of its children. Not
     * available if the length data was not provided.
     */
    private SkeletonInterBoneWire interBoneWires;
    private List<Bone> selectedBones = new ArrayList<Bone>();

    public SkeletonDebugger() {
    }

    /**
     * Creates a debugger with no length data. The wires will be a connection
     * between the bones' heads only. The points will show the bones' heads only
     * and no dotted line of inter bones connection will be visible.
     *
     * @param name     the name of the debugger's node
     * @param skeleton the skeleton that will be shown
     */
    public SkeletonDebugger(String name, Skeleton skeleton, boolean guessBonesOrientation) {
        super(name);
        this.skeleton = skeleton;
        skeleton.reset();
        skeleton.updateWorldVectors();
        Map<Integer, Float> boneLengths = new HashMap<Integer, Float>();

        for (Bone bone : skeleton.getRoots()) {
            computeLength(bone, boneLengths, skeleton);
        }

        bones = new SkeletonBone(skeleton, boneLengths, guessBonesOrientation);

        this.attachChild(bones);

        interBoneWires = new SkeletonInterBoneWire(skeleton, boneLengths, guessBonesOrientation);
        Geometry g = new Geometry(name + "_interwires", interBoneWires);
        g.setBatchHint(BatchHint.Never);
        this.attachChild(g);
    }

    protected void initialize(AssetManager assetManager) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.05f, 1.0f));//new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f)   
        setMaterial(mat);
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setBoolean("VertexColor", true);
        bones.setMaterial(mat2);
        batch();

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

    public Skeleton getSkeleton() {
        return skeleton;
    }


    private void computeLength(Bone b, Map<Integer, Float> boneLengths, Skeleton skeleton) {
        if (b.getChildren().isEmpty()) {
            if (b.getParent() != null) {
                boneLengths.put(skeleton.getBoneIndex(b), boneLengths.get(skeleton.getBoneIndex(b.getParent())) * 0.75f);
            } else {
                boneLengths.put(skeleton.getBoneIndex(b), 0.1f);
            }
        } else {
            float length = Float.MAX_VALUE;
            for (Bone bone : b.getChildren()) {
                float len = b.getModelSpacePosition().subtract(bone.getModelSpacePosition()).length();
                if (len < length) {
                    length = len;
                }
            }
            boneLengths.put(skeleton.getBoneIndex(b), length);
            for (Bone bone : b.getChildren()) {
                computeLength(bone, boneLengths, skeleton);
            }
        }
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        bones.updateGeometry();
        if (interBoneWires != null) {
            interBoneWires.updateGeometry();
        }
    }

    ColorRGBA selectedColor = ColorRGBA.Orange;
    ColorRGBA baseColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);

    protected Bone select(Geometry g) {
        Node oldNode = bones.getSelectedNode();
        Bone b = bones.select(g);
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
     * @return the skeleton wires
     */
    public SkeletonBone getBoneShapes() {
        return bones;
    }

    /**
     * @return the dotted line between bones (can be null)
     */
    public SkeletonInterBoneWire getInterBoneWires() {
        return interBoneWires;
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