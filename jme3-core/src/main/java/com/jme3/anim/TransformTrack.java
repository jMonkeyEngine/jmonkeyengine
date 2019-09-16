/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.anim;

import com.jme3.anim.interpolator.FrameInterpolator;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.CompactQuaternionArray;
import com.jme3.animation.CompactVector3Array;
import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * Contains a list of transforms and times for each keyframe.
 *
 * @author Rémy Bouquet
 */
public class TransformTrack implements AnimTrack<Transform> {

    private double length;
    private HasLocalTransform target;

    /**
     * Transforms and times for track.
     */
    private CompactVector3Array translations;
    private CompactQuaternionArray rotations;
    private CompactVector3Array scales;
    private FrameInterpolator interpolator = FrameInterpolator.DEFAULT;
    private float[] times;

    /**
     * Serialization-only. Do not use.
     */
    public TransformTrack() {
    }

    /**
     * Creates a transform track for the given bone index
     *
     * @param times        a float array with the time of each frame
     * @param translations the translation of the bone for each frame
     * @param rotations    the rotation of the bone for each frame
     * @param scales       the scale of the bone for each frame
     */
    public TransformTrack(HasLocalTransform target, float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
        this.target = target;
        this.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * return the array of rotations of this track
     *
     * @return an array
     */
    public Quaternion[] getRotations() {
        return rotations.toObjectArray();
    }

    /**
     * returns the array of scales for this track
     *
     * @return an array or null
     */
    public Vector3f[] getScales() {
        return scales == null ? null : scales.toObjectArray();
    }

    /**
     * returns the arrays of time for this track
     *
     * @return the pre-existing array
     */
    public float[] getTimes() {
        return times;
    }

    /**
     * returns the array of translations of this track
     *
     * @return an array
     */
    public Vector3f[] getTranslations() {
        return translations.toObjectArray();
    }


    /**
     * Sets the keyframes times for this Joint track
     *
     * @param times the keyframes times
     */
    public void setTimes(float[] times) {
        if (times.length == 0) {
            throw new RuntimeException("TransformTrack with no keyframes!");
        }
        this.times = times;
        length = times[times.length - 1] - times[0];
    }

    /**
     * Set the translations for this joint track
     *
     * @param translations the translation of the bone for each frame
     */
    public void setKeyframesTranslation(Vector3f[] translations) {
        if (times == null) {
            throw new RuntimeException("TransformTrack doesn't have any time for key frames, please call setTimes first");
        }
        if (translations.length == 0) {
            throw new RuntimeException("TransformTrack with no translation keyframes!");
        }
        this.translations = new CompactVector3Array();
        this.translations.add(translations);
        this.translations.freeze();

        assert times != null && times.length == translations.length;
    }

    /**
     * Set the scales for this joint track
     *
     * @param scales the scales of the bone for each frame
     */
    public void setKeyframesScale(Vector3f[] scales) {
        if (times == null) {
            throw new RuntimeException("TransformTrack doesn't have any time for key frames, please call setTimes first");
        }
        if (scales.length == 0) {
            throw new RuntimeException("TransformTrack with no scale keyframes!");
        }
        this.scales = new CompactVector3Array();
        this.scales.add(scales);
        this.scales.freeze();

        assert times != null && times.length == scales.length;
    }

    /**
     * Set the rotations for this joint track
     *
     * @param rotations the rotations of the bone for each frame
     */
    public void setKeyframesRotation(Quaternion[] rotations) {
        if (times == null) {
            throw new RuntimeException("TransformTrack doesn't have any time for key frames, please call setTimes first");
        }
        if (rotations.length == 0) {
            throw new RuntimeException("TransformTrack with no rotation keyframes!");
        }
        this.rotations = new CompactQuaternionArray();
        this.rotations.add(rotations);
        this.rotations.freeze();

        assert times != null && times.length == rotations.length;
    }


    /**
     * Set the translations, rotations and scales for this bone track
     *
     * @param times        a float array with the time of each frame
     * @param translations the translation of the bone for each frame
     * @param rotations    the rotation of the bone for each frame
     * @param scales       the scale of the bone for each frame
     */
    public void setKeyframes(float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
        setTimes(times);
        if (translations != null) {
            setKeyframesTranslation(translations);
        }
        if (rotations != null) {
            setKeyframesRotation(rotations);
        }
        if (scales != null) {
            setKeyframesScale(scales);
        }
    }

    public double getLength() {
        return length;
    }

    public void getDataAtTime(double t, Transform transform) {
        float time = (float) t;

        int lastFrame = times.length - 1;
        if (time < 0 || lastFrame == 0) {
            if (translations != null) {
                translations.get(0, transform.getTranslation());
            }
            if (rotations != null) {
                rotations.get(0, transform.getRotation());
            }
            if (scales != null) {
                scales.get(0, transform.getScale());
            }
            return;
        }

        int startFrame = 0;
        int endFrame = 1;
        float blend = 0;
        if (time >= times[lastFrame]) {
            // extrapolate beyond the final frame of the animation
            startFrame = lastFrame;

            float inferredInterval = times[lastFrame] - times[lastFrame - 1];
            if (inferredInterval > 0f) {
                blend = (time - times[startFrame]) / inferredInterval;
            }

        } else {
            // use lastFrame so we never overflow the array
            int i;
            for (i = 0; i < lastFrame && times[i] < time; i++) {
                startFrame = i;
                endFrame = i + 1;
            }
            blend = (time - times[startFrame])
                    / (times[endFrame] - times[startFrame]);
        }

        Transform interpolated = interpolator.interpolate(blend, startFrame, translations, rotations, scales, times);

        if (translations != null) {
            transform.setTranslation(interpolated.getTranslation());
        }
        if (rotations != null) {
            transform.setRotation(interpolated.getRotation());
        }
        if (scales != null) {
            transform.setScale(interpolated.getScale());
        }
    }

    public void setFrameInterpolator(FrameInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    public HasLocalTransform getTarget() {
        return target;
    }

    public void setTarget(HasLocalTransform target) {
        this.target = target;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(translations, "translations", null);
        oc.write(rotations, "rotations", null);
        oc.write(times, "times", null);
        oc.write(scales, "scales", null);
        oc.write(target, "target", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        translations = (CompactVector3Array) ic.readSavable("translations", null);
        rotations = (CompactQuaternionArray) ic.readSavable("rotations", null);
        times = ic.readFloatArray("times", null);
        scales = (CompactVector3Array) ic.readSavable("scales", null);
        target = (HasLocalTransform) ic.readSavable("target", null);
        setTimes(times);
    }

    @Override
    public TransformTrack jmeClone() {
        try {
            TransformTrack clone = (TransformTrack) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.target = cloner.clone(target);
    }
}
