/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
import com.jme3.export.*;
import com.jme3.scene.Geometry;
import com.jme3.util.clone.Cloner;

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
    /**
     * The interpolator to use, or null to always use the default interpolator
     * of the current thread.
     */
    private FrameInterpolator interpolator = null;
    private float[] times;
    private int nbMorphTargets;

    /**
     * Serialization-only. Do not use.
     */
    protected MorphTrack() {
    }

    /**
     * Creates a morph track with the given Geometry as a target
     *
     * @param target   the desired target (alias created)
     * @param times    a float array with the time of each frame (alias created
     *                 -- do not modify after passing it to this constructor)
     * @param weights  the morphs for each frames (alias created -- do not
     *                 modify after passing it to this constructor)
     * @param nbMorphTargets
     *                 the desired number of morph targets
     */
    public MorphTrack(Geometry target, float[] times, float[] weights, int nbMorphTargets) {
        this.target = target;
        this.nbMorphTargets = nbMorphTargets;
        this.setKeyframes(times, weights);
    }

    /**
     * return the array of weights of this track
     *
     * @return the pre-existing array -- do not modify
     */
    public float[] getWeights() {
        return weights;
    }

    /**
     * Set the weights for this morph track. Note that the number of weights
     * must equal the number of frames times the number of morph targets.
     *
     * @param weights  the weights of the morphs for each frame (alias created
     *                 -- do not modify after passing it to this setter)
     *
     * @throws IllegalStateException if this track does not have times set
     * @throws IllegalArgumentException if weights is an empty array or if
     *         the number of weights violates the frame count constraint
     */
    public void setKeyframesWeight(float[] weights) {
        if (times == null) {
            throw new IllegalStateException("MorphTrack doesn't have any time for key frames, please call setTimes first");
        }
        if (weights.length == 0) {
            throw new IllegalArgumentException("MorphTrack with no weight keyframes!");
        }
        if (times.length * nbMorphTargets != weights.length) {
            throw new IllegalArgumentException("weights.length must equal nbMorphTargets * times.length");
        }

        this.weights = weights;
    }

    /**
     * returns the arrays of time for this track
     *
     * @return the pre-existing array -- do not modify
     */
    public float[] getTimes() {
        return times;
    }

    /**
     * Sets the keyframes times for this Joint track
     *
     * @param times  the keyframes times (alias created -- do not modify after
     *               passing it to this setter)
     * @throws IllegalArgumentException if times is empty
     */
    public void setTimes(float[] times) {
        if (times.length == 0) {
            throw new IllegalArgumentException("TransformTrack with no keyframes!");
        }
        this.times = times;
        length = times[times.length - 1] - times[0];
    }


    /**
     * Sets the times and weights for this morph track. Note that the number of weights
     * must equal the number of frames times the number of morph targets.
     *
     * @param times    a float array with the time of each frame (alias created
     *                 -- do not modify after passing it to this setter)
     * @param weights  the weights of the morphs for each frame (alias created
     *                 -- do not modify after passing it to this setter)
     */
    public void setKeyframes(float[] times, float[] weights) {
        if (times != null) {
            setTimes(times);
        }
        if (weights != null) {
            setKeyframesWeight(weights);
        }
    }

    /**
     * @return the number of morph targets
     */
    public int getNbMorphTargets() {
        return nbMorphTargets;
    }

    /**
     * Sets the number of morph targets and the corresponding weights.
     * Note that the number of weights must equal the number of frames times the number of morph targets.
     *
     * @param weights        the weights for each frame (alias created
     *                       -- do not modify after passing it to this setter)
     * @param nbMorphTargets the desired number of morph targets
     * @throws IllegalArgumentException if the number of weights and the new
     *         number of morph targets violate the frame count constraint
     */
    public void setNbMorphTargets(float[] weights, int nbMorphTargets) {
        if (times.length * nbMorphTargets != weights.length) {
            throw new IllegalArgumentException("weights.length must equal nbMorphTargets * times.length");
        }
        this.nbMorphTargets = nbMorphTargets;
        setKeyframesWeight(weights);
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

        FrameInterpolator fi = (interpolator == null)
                ? FrameInterpolator.getThreadDefault() : interpolator;
        fi.interpolateWeights(blend, startFrame, weights, nbMorphTargets, store);
    }

    /**
     * Replace the FrameInterpolator.
     *
     * @param interpolator the interpolator to use (alias created)
     */
    public void setFrameInterpolator(FrameInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     * Access the target geometry.
     *
     * @return the pre-existing instance
     */
    public Geometry getTarget() {
        return target;
    }

    /**
     * Replace the target geometry.
     *
     * @param target the Geometry to use as the target (alias created)
     */
    public void setTarget(Geometry target) {
        this.target = target;
    }

    /**
     * Serialize this track to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param ex the exporter to write to (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(weights, "weights", null);
        oc.write(times, "times", null);
        oc.write(target, "target", null);
        oc.write(nbMorphTargets, "nbMorphTargets", 0);
    }

    /**
     * De-serialize this track from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to use (not null)
     * @throws IOException from the importer
     */
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
        // Note: interpolator, times, and weights are not cloned
    }

}
