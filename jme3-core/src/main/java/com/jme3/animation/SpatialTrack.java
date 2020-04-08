/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * This class represents the track for spatial animation.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
@Deprecated
public class SpatialTrack implements JmeCloneable, Track {

    /** 
     * Translations of the track. 
     */
    private CompactVector3Array translations;
    
    /** 
     * Rotations of the track. 
     */
    private CompactQuaternionArray rotations;
    
    /**
     * Scales of the track. 
     */
    private CompactVector3Array scales;

    /**
     * The spatial to which this track applies.
     * Note that this is optional, if no spatial is defined, the AnimControl's Spatial will be used.
     */
    private Spatial trackSpatial;

    /** 
     * The times of the animations frames. 
     */
    private float[] times;

    public SpatialTrack() {
    }

    /**
     * Creates a spatial track for the given track data.
     * 
     * @param times
     *            a float array with the time of each frame
     * @param translations
     *            the translation of the bone for each frame
     * @param rotations
     *            the rotation of the bone for each frame
     * @param scales
     *            the scale of the bone for each frame
     */
    public SpatialTrack(float[] times, Vector3f[] translations,
                        Quaternion[] rotations, Vector3f[] scales) {
        setKeyframes(times, translations, rotations, scales);
    }

    /**
     * 
     * Modify the spatial which this track modifies.
     * 
     * @param time
     *            the current time of the animation
     */
    @Override
    public void setTime(float time, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
        Spatial spatial = trackSpatial;
        if (spatial == null) {
            spatial = control.getSpatial();
        }

        Vector3f tempV = vars.vect1;
        Vector3f tempS = vars.vect2;
        Quaternion tempQ = vars.quat1;
        Vector3f tempV2 = vars.vect3;
        Vector3f tempS2 = vars.vect4;
        Quaternion tempQ2 = vars.quat2;
        
        int lastFrame = times.length - 1;
        if (time < 0 || lastFrame == 0) {
            if (rotations != null)
                rotations.get(0, tempQ);
            if (translations != null)
                translations.get(0, tempV);
            if (scales != null) {
                scales.get(0, tempS);
            }
        } else if (time >= times[lastFrame]) {
            if (rotations != null)
                rotations.get(lastFrame, tempQ);
            if (translations != null)
                translations.get(lastFrame, tempV);
            if (scales != null) {
                scales.get(lastFrame, tempS);
            }
        } else {
            int startFrame = 0;
            int endFrame = 1;
            // use lastFrame so we never overflow the array
            for (int i = 0; i < lastFrame && times[i] < time; ++i) {
                startFrame = i;
                endFrame = i + 1;
            }

            float blend = (time - times[startFrame]) / (times[endFrame] - times[startFrame]);

            if (rotations != null)
                rotations.get(startFrame, tempQ);
            if (translations != null)
                translations.get(startFrame, tempV);
            if (scales != null) {
                scales.get(startFrame, tempS);
            }
            if (rotations != null)
                rotations.get(endFrame, tempQ2);
            if (translations != null)
                translations.get(endFrame, tempV2);
            if (scales != null) {
                scales.get(endFrame, tempS2);
            }
            tempQ.nlerp(tempQ2, blend);
            tempV.interpolateLocal(tempV2, blend);
            tempS.interpolateLocal(tempS2, blend);
        }

        if (translations != null) {
            spatial.setLocalTranslation(tempV);
        }
        if (rotations != null) {
            spatial.setLocalRotation(tempQ);
        }
        if (scales != null) {
            spatial.setLocalScale(tempS);
        }
    }

    /**
     * Set the translations, rotations and scales for this track.
     * 
     * @param times
     *            a float array with the time of each frame
     * @param translations
     *            the translation of the bone for each frame
     * @param rotations
     *            the rotation of the bone for each frame
     * @param scales
     *            the scale of the bone for each frame
     */
    public void setKeyframes(float[] times, Vector3f[] translations,
                             Quaternion[] rotations, Vector3f[] scales) {
        if (times.length == 0) {
            throw new RuntimeException("BoneTrack with no keyframes!");
        }

        this.times = times;
        if (translations != null) {
            assert times.length == translations.length;
            this.translations = new CompactVector3Array();
            this.translations.add(translations);
            this.translations.freeze();
        }
        if (rotations != null) {
            assert times.length == rotations.length;
            this.rotations = new CompactQuaternionArray();
            this.rotations.add(rotations);
            this.rotations.freeze();
        }
        if (scales != null) {
            assert times.length == scales.length;
            this.scales = new CompactVector3Array();
            this.scales.add(scales);
            this.scales.freeze();
        }
    }

    /**
     * @return the array of rotations of this track
     */
    public Quaternion[] getRotations() {
            return rotations == null ? null : rotations.toObjectArray();
    }

    /**
     * @return the array of scales for this track
     */
    public Vector3f[] getScales() {
            return scales == null ? null : scales.toObjectArray();
    }

    /**
     * @return the arrays of time for this track
     */
    public float[] getTimes() {
            return times;
    }

    /**
     * @return the array of translations of this track
     */
    public Vector3f[] getTranslations() {
            return translations == null ? null : translations.toObjectArray();
    }

    /**
     * @return the length of the track
     */
    @Override
    public float getLength() {
            return times == null ? 0 : times[times.length - 1] - times[0];
    }

    /**
     * Create a clone with the same track spatial.
     *
     * @return a new track
     */
    @Override
    public SpatialTrack clone() {
        Cloner cloner = new Cloner();
        cloner.setClonedValue(trackSpatial, trackSpatial);
        return cloner.clone(this);
    }

    @Override
    public float[] getKeyFrameTimes() {
        return times;
    }

    public void setTrackSpatial(Spatial trackSpatial) {
        this.trackSpatial = trackSpatial;
    }

    public Spatial getTrackSpatial() {
        return trackSpatial;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new track
     */
    @Override
    public SpatialTrack jmeClone() {
        try {
            return (SpatialTrack) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException("Can't clone track", exception);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned track into a deep-cloned one, using the specified cloner
     * to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the track from which this track was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        translations = cloner.clone(translations);
        rotations = cloner.clone(rotations);
        scales = cloner.clone(scales);
        trackSpatial = cloner.clone(trackSpatial);
        times = cloner.clone(times);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(translations, "translations", null);
        oc.write(rotations, "rotations", null);
        oc.write(times, "times", null);
        oc.write(scales, "scales", null);
        oc.write(trackSpatial, "trackSpatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        translations = (CompactVector3Array) ic.readSavable("translations", null);
        rotations = (CompactQuaternionArray) ic.readSavable("rotations", null);
        times = ic.readFloatArray("times", null);
        scales = (CompactVector3Array) ic.readSavable("scales", null);
        trackSpatial = (Spatial) ic.readSavable("trackSpatial", null);
    }
}
