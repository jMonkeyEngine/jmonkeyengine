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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.io.IOException;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * The class that displays either heads of the bones if no length data is supplied or both heads and tails otherwise.
 */
public class SkeletonPoints extends Mesh {
    /** The skeleton to be displayed. */
    private Skeleton            skeleton;
    /** The map between the bone index and its length. */
    private Map<Integer, Float> boneLengths;

    /**
     * Creates a points with no length data. The points will only show the bone's heads.
     * @param skeleton
     *            the skeleton that will be shown
     */
    public SkeletonPoints(Skeleton skeleton) {
        this(skeleton, null);
    }

    /**
     * Creates a points with bone lengths data. If the data is supplied then the points will show both head and tail of each bone.
     * @param skeleton
     *            the skeleton that will be shown
     * @param boneLengths
     *            a map between the bone's index and the bone's length
     */
    public SkeletonPoints(Skeleton skeleton, Map<Integer, Float> boneLengths) {
        this.skeleton = skeleton;
        this.setMode(Mode.Points);
        int pointsCount = skeleton.getBoneCount();

        if (boneLengths != null) {
            this.boneLengths = boneLengths;
            pointsCount *= 2;
        }

        VertexBuffer pb = new VertexBuffer(Type.Position);
        FloatBuffer fpb = BufferUtils.createFloatBuffer(pointsCount * 3);
        pb.setupData(Usage.Stream, 3, Format.Float, fpb);
        this.setBuffer(pb);

        this.updateCounts();

    }

    /**
     * For serialization only. Do not use.
     */
    protected SkeletonPoints() {
    }

    /**
     * The method updates the geometry according to the positions of the bones.
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
     * De-serializes from the specified importer, for example when loading from
     * a J3O file.
     *
     * @param importer the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        skeleton = (Skeleton) capsule.readSavable("skeleton", null);

        int[] blKeys = capsule.readIntArray("blKeys", null);
        float[] blValues = capsule.readFloatArray("blValues", null);
        if (blKeys == null) {
            boneLengths = null;
        } else {
            assert blValues.length == blKeys.length;
            int numLengths = blKeys.length;
            boneLengths = new HashMap<>(numLengths);
            for (int i = 0; i < numLengths; ++i) {
                boneLengths.put(blKeys[i], blValues[i]);
            }
        }
    }

    /**
     * Serializes to the specified exporter, for example when saving to a J3O
     * file. The current instance is unaffected.
     *
     * @param exporter the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(skeleton, "skeleton", null);

        if (boneLengths != null) {
            int numLengths = boneLengths.size();
            int[] blKeys = new int[numLengths];
            float[] blValues = new float[numLengths];
            int i = 0;
            for (Map.Entry<Integer, Float> entry : boneLengths.entrySet()) {
                blKeys[i] = entry.getKey();
                blValues[i] = entry.getValue();
                ++i;
            }
            capsule.write(blKeys, "blKeys", null);
            capsule.write(blValues, "blValues", null);
        }
    }
}
