/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.animation;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * Contains a list of transforms and times for each keyframe.
 */
public final class BoneTrack implements Savable {

    /**
     * Bone index in the skeleton which this track effects.
     */
    private int targetBoneIndex;
    /**
     * Transforms and times for track.
     */
    private CompactVector3Array translations;
    private CompactQuaternionArray rotations;
    private CompactVector3Array scales;
    private float[] times;
    // temp vectors for interpolation
    private transient final Vector3f tempV = new Vector3f();
    private transient final Quaternion tempQ = new Quaternion();
    private transient final Vector3f tempS = new Vector3f();
    private transient final Vector3f tempV2 = new Vector3f();
    private transient final Quaternion tempQ2 = new Quaternion();
    private transient final Vector3f tempS2 = new Vector3f();

    /**
     * Serialization-only. Do not use.
     */
    public BoneTrack() {
    }

    public BoneTrack(int targetBoneIndex, float[] times, Vector3f[] translations, Quaternion[] rotations) {
        this.targetBoneIndex = targetBoneIndex;
        this.setKeyframes(times, translations, rotations);
    }

    public BoneTrack(int targetBoneIndex, float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
        this(targetBoneIndex, times, translations, rotations);
        this.setKeyframes(times, translations, rotations);
    }

    public BoneTrack(int targetBoneIndex) {
        this.targetBoneIndex = targetBoneIndex;
    }

    public int getTargetBoneIndex() {
        return targetBoneIndex;
    }

    public Quaternion[] getRotations() {
        return rotations.toObjectArray();
    }

    public Vector3f[] getScales() {
        return scales == null ? null : scales.toObjectArray();
    }

    public float[] getTimes() {
        return times;
    }

    public Vector3f[] getTranslations() {
        return translations.toObjectArray();
    }

    public void setKeyframes(float[] times, Vector3f[] translations, Quaternion[] rotations) {
        if (times.length == 0) {
            throw new RuntimeException("BoneTrack with no keyframes!");
        }

        assert times.length == translations.length && times.length == rotations.length;

        this.times = times;
        this.translations = new CompactVector3Array();
        this.translations.add(translations);
        this.translations.freeze();
        this.rotations = new CompactQuaternionArray();
        this.rotations.add(rotations);
        this.rotations.freeze();
    }

    public void setKeyframes(float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
        this.setKeyframes(times, translations, rotations);
        assert times.length == scales.length;
        if (scales != null) {
            this.scales = new CompactVector3Array();
            this.scales.add(scales);
            this.scales.freeze();
        }
    }

    /**
     * Modify the bone which this track modifies in the skeleton to contain
     * the correct animation transforms for a given time.
     * The transforms can be interpolated in some method from the keyframes.
     */
    public void setTime(float time, Skeleton skeleton, float weight) {
        Bone target = skeleton.getBone(targetBoneIndex);

        int lastFrame = times.length - 1;
        if (time < 0 || lastFrame == 0) {
            rotations.get(0, tempQ);
            translations.get(0, tempV);
            if (scales != null) {
                scales.get(0, tempS);
            }
        } else if (time >= times[lastFrame]) {
            rotations.get(lastFrame, tempQ);
            translations.get(lastFrame, tempV);
            if (scales != null) {
                scales.get(lastFrame, tempS);
            }
        } else {
            int startFrame = 0;
            int endFrame = 1;
            // use lastFrame so we never overflow the array
            int i;
            for (i = 0; i < lastFrame && times[i] < time; i++) {
                startFrame = i;
                endFrame = i + 1;
            }

            float blend = (time - times[startFrame])
                    / (times[endFrame] - times[startFrame]);

            rotations.get(startFrame, tempQ);
            translations.get(startFrame, tempV);
            if (scales != null) {
                scales.get(startFrame, tempS);
            }
            rotations.get(endFrame, tempQ2);
            translations.get(endFrame, tempV2);
            if (scales != null) {
                scales.get(endFrame, tempS2);
            }
            tempQ.slerp(tempQ2, blend);
            tempV.interpolate(tempV2, blend);
            tempS.interpolate(tempS2, blend);
        }

        if (weight != 1f) {
//            tempQ.slerp(Quaternion.IDENTITY, 1f - weight);
//            tempV.multLocal(weight);
            target.blendAnimTransforms(tempV, tempQ, scales != null ? tempS : null, weight);
//            target.setAnimTransforms(tempV, tempQ);
        } else {
            target.setAnimTransforms(tempV, tempQ, scales != null ? tempS : null);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(targetBoneIndex, "boneIndex", 0);
        oc.write(translations, "translations", null);
        oc.write(rotations, "rotations", null);
        oc.write(times, "times", null);
        oc.write(scales, "scales", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        targetBoneIndex = ic.readInt("boneIndex", 0);

        translations = (CompactVector3Array) ic.readSavable("translations", null);

        rotations = (CompactQuaternionArray) ic.readSavable("rotations", null);
        times = ic.readFloatArray("times", null);
        scales = (CompactVector3Array) ic.readSavable("scales", null);


        //Backward compatibility for old j3o files generated before revision 6807
        if (translations == null) {
            Savable[] sav = ic.readSavableArray("translations", null);
            if (sav != null) {
                translations = new CompactVector3Array();
                Vector3f[] transCopy = new Vector3f[sav.length];
                System.arraycopy(sav, 0, transCopy, 0, sav.length);
                translations.add(transCopy);
                translations.freeze();
            }
        }
        if (rotations == null) {
            Savable[] sav = ic.readSavableArray("rotations", null);
            if (sav != null) {
                rotations = new CompactQuaternionArray();
                Quaternion[] rotCopy = new Quaternion[sav.length];
                System.arraycopy(sav, 0, rotCopy, 0, sav.length);
                rotations.add(rotCopy);
                rotations.freeze();
            }
        }


    }
}
