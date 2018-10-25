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
package com.jme3.anim;

import com.jme3.anim.interpolator.FrameInterpolator;
import com.jme3.animation.*;
import com.jme3.export.*;
import com.jme3.scene.Geometry;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 * Contains a list of weights and times for each keyframe.
 *
 * @author Rémy Bouquet
 */
public class MorphTrack implements AnimTrack<float[]> {

    private double length;
    private Geometry target;

    /**
     * Weights and times for track.
     */
    private float[] weights;
    private FrameInterpolator interpolator = FrameInterpolator.DEFAULT;
    private float[] times;
    private int nbMorphTargets;

    /**
     * Serialization-only. Do not use.
     */
    public MorphTrack() {
    }

    /**
     * Creates a morph track with the given Geometry as a target
     *
     * @param times        a float array with the time of each frame
     * @param weights       the morphs for each frames
     */
    public MorphTrack(Geometry target, float[] times, float[] weights, int nbMorphTargets) {
        this.target = target;
        this.nbMorphTargets = nbMorphTargets;
        this.setKeyframes(times, weights);
    }

    /**
     * return the array of weights of this track
     *
     * @return
     */
    public float[] getWeights() {
        return weights;
    }

    /**
     * returns the arrays of time for this track
     *
     * @return
     */
    public float[] getTimes() {
        return times;
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
     * Set the weight for this morph track
     *
     * @param times        a float array with the time of each frame
     * @param weights      the weights of the morphs for each frame

     */
    public void setKeyframes(float[] times, float[] weights) {
        setTimes(times);
        if (weights != null) {
            if (times == null) {
                throw new RuntimeException("MorphTrack doesn't have any time for key frames, please call setTimes first");
            }

            this.weights = weights;

            assert times != null && times.length == weights.length;
        }
    }

    @Override
    public double getLength() {
        return length;
    }

    @Override
    public void getDataAtTime(double t, float[] store) {
        float time = (float) t;

        int lastFrame = times.length - 1;
        if (time < 0 || lastFrame == 0) {
            if (weights != null) {
                System.arraycopy(weights,0,store,0, nbMorphTargets);
            }
            return;
        }

        int startFrame = 0;
        int endFrame = 1;
        float blend = 0;
        if (time >= times[lastFrame]) {
            startFrame = lastFrame;

            time = time - times[startFrame] + times[startFrame - 1];
            blend = (time - times[startFrame - 1])
                    / (times[startFrame] - times[startFrame - 1]);

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

        interpolator.interpolateWeights(blend, startFrame, weights, nbMorphTargets, store);
    }

    public void setFrameInterpolator(FrameInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    public Geometry getTarget() {
        return target;
    }

    public void setTarget(Geometry target) {
        this.target = target;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(weights, "weights", null);
        oc.write(times, "times", null);
        oc.write(target, "target", null);
        oc.write(nbMorphTargets, "nbMorphTargets", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        weights = ic.readFloatArray("weights", null);
        times = ic.readFloatArray("times", null);
        target = (Geometry) ic.readSavable("target", null);
        nbMorphTargets = ic.readInt("nbMorphTargets", 0);
        setTimes(times);
    }

    @Override
    public Object jmeClone() {
        try {
            MorphTrack clone = (MorphTrack) super.clone();
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
