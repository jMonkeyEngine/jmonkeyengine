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
package com.jme3.scene.debug;

import java.nio.FloatBuffer;
import java.util.Map;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;

/**
 * A class that displays a dotted line between a bone tail and its childrens' heads.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SkeletonInterBoneWire extends Mesh {
    private static final int    POINT_AMOUNT = 10;
    /** The amount of connections between bones. */
    private int                 connectionsAmount;
    /** The skeleton that will be showed. */
    private Skeleton            skeleton;
    /** The map between the bone index and its length. */
    private Map<Integer, Float> boneLengths;

    /**
     * Creates buffers for points. Each line has POINT_AMOUNT of points.
     * @param skeleton
     *            the skeleton that will be showed
     * @param boneLengths
     *            the lengths of the bones
     */
    public SkeletonInterBoneWire(Skeleton skeleton, Map<Integer, Float> boneLengths) {
        this.skeleton = skeleton;

        for (Bone bone : skeleton.getRoots()) {
            this.countConnections(bone);
        }

        this.setMode(Mode.Points);
        this.setPointSize(1);
        this.boneLengths = boneLengths;

        VertexBuffer pb = new VertexBuffer(Type.Position);
        FloatBuffer fpb = BufferUtils.createFloatBuffer(POINT_AMOUNT * connectionsAmount * 3);
        pb.setupData(Usage.Stream, 3, Format.Float, fpb);
        this.setBuffer(pb);

        this.updateCounts();
    }

    /**
     * The method updates the geometry according to the poitions of the bones.
     */
    public void updateGeometry() {
        VertexBuffer vb = this.getBuffer(Type.Position);
        FloatBuffer posBuf = this.getFloatBuffer(Type.Position);
        posBuf.clear();
        for (int i = 0; i < skeleton.getBoneCount(); ++i) {
            Bone bone = skeleton.getBone(i);
            Vector3f parentTail = bone.getModelSpacePosition().add(bone.getModelSpaceRotation().mult(Vector3f.UNIT_Y.mult(boneLengths.get(i))));

            for (Bone child : bone.getChildren()) {
                Vector3f childHead = child.getModelSpacePosition();
                Vector3f v = childHead.subtract(parentTail);
                float pointDelta = v.length() / POINT_AMOUNT;
                v.normalizeLocal().multLocal(pointDelta);
                Vector3f pointPosition = parentTail.clone();
                for (int j = 0; j < POINT_AMOUNT; ++j) {
                    posBuf.put(pointPosition.getX()).put(pointPosition.getY()).put(pointPosition.getZ());
                    pointPosition.addLocal(v);
                }
            }
        }
        posBuf.flip();
        vb.updateData(posBuf);

        this.updateBound();
    }

    /**
     * Th method couns the connections between bones.
     * @param bone
     *            the bone where counting starts
     */
    private void countConnections(Bone bone) {
        for (Bone child : bone.getChildren()) {
            ++connectionsAmount;
            this.countConnections(child);
        }
    }
}
