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
import java.nio.ShortBuffer;
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
 * The class that displays either wires between the bones' heads if no length data is supplied and
 * full bones' shapes otherwise.
 */
public class SkeletonWire extends Mesh {
    /** The number of bones' connections. Used in non-length mode. */
    private int                 numConnections;
    /** The skeleton to be displayed. */
    private Skeleton            skeleton;
    /** The map between the bone index and its length. */
    private Map<Integer, Float> boneLengths;

    /**
     * Creates a wire with no length data. The wires will be a connection between the bones' heads only.
     * @param skeleton
     *            the skeleton that will be shown
     */
    public SkeletonWire(Skeleton skeleton) {
        this(skeleton, null);
    }

    /**
     * Creates a wire with bone lengths data. If the data is supplied then the wires will show each full bone (from head to tail).
     * @param skeleton
     *            the skeleton that will be shown
     * @param boneLengths
     *            a map between the bone's index and the bone's length
     */
    public SkeletonWire(Skeleton skeleton, Map<Integer, Float> boneLengths) {
        this.skeleton = skeleton;

        for (Bone bone : skeleton.getRoots()) {
            this.countConnections(bone);
        }

        this.setMode(Mode.Lines);
        int lineVerticesCount = skeleton.getBoneCount();
        if (boneLengths != null) {
            this.boneLengths = boneLengths;
            lineVerticesCount *= 2;
        }

        VertexBuffer pb = new VertexBuffer(Type.Position);
        FloatBuffer fpb = BufferUtils.createFloatBuffer(lineVerticesCount * 3);
        pb.setupData(Usage.Stream, 3, Format.Float, fpb);
        this.setBuffer(pb);

        VertexBuffer ib = new VertexBuffer(Type.Index);
        ShortBuffer sib = BufferUtils.createShortBuffer(boneLengths != null ? lineVerticesCount : numConnections * 2);
        ib.setupData(Usage.Static, 2, Format.UnsignedShort, sib);
        this.setBuffer(ib);

        if (boneLengths != null) {
            for (int i = 0; i < lineVerticesCount; ++i) {
                sib.put((short) i);
            }
        } else {
            for (Bone bone : skeleton.getRoots()) {
                this.writeConnections(sib, bone);
            }
        }
        sib.flip();

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
            Vector3f head = bone.getModelSpacePosition();

            posBuf.put(head.getX()).put(head.getY()).put(head.getZ());
            if (boneLengths != null) {
                Vector3f tail = head.add(bone.getModelSpaceRotation().mult(Vector3f.UNIT_Y.mult(boneLengths.get(i))));
                posBuf.put(tail.getX()).put(tail.getY()).put(tail.getZ());
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
            numConnections++;
            this.countConnections(child);
        }
    }

    /**
     * The method writes the indexes for the connection vertices. Used in non-length mode.
     * @param indexBuf
     *            the index buffer
     * @param bone
     *            the bone
     */
    private void writeConnections(ShortBuffer indexBuf, Bone bone) {
        for (Bone child : bone.getChildren()) {
            // write myself
            indexBuf.put((short) skeleton.getBoneIndex(bone));
            // write the child
            indexBuf.put((short) skeleton.getBoneIndex(child));

            this.writeConnections(indexBuf, child);
        }
    }
}
