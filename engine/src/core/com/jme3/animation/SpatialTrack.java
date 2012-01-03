package com.jme3.animation;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.Arrays;

/**
 * This class represents the track for spatial animation.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SpatialTrack implements Track {
    
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
    public void setTime(float time, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
        Spatial spatial = control.getSpatial();
        
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
            tempV.interpolate(tempV2, blend);
            tempS.interpolate(tempS2, blend);
        }
        
        if (translations != null)
            spatial.setLocalTranslation(tempV);
        if (rotations != null)
            spatial.setLocalRotation(tempQ);
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
    public float getLength() {
            return times == null ? 0 : times[times.length - 1] - times[0];
    }

    /**
     * This method creates a clone of the current object.
     * @return a clone of the current object
     */
    @Override
    public SpatialTrack clone() {
        int tablesLength = times.length;

        float[] timesCopy = this.times.clone();
        Vector3f[] translationsCopy = this.getTranslations() == null ? null : Arrays.copyOf(this.getTranslations(), tablesLength);
        Quaternion[] rotationsCopy = this.getRotations() == null ? null : Arrays.copyOf(this.getRotations(), tablesLength);
        Vector3f[] scalesCopy = this.getScales() == null ? null : Arrays.copyOf(this.getScales(), tablesLength);

        //need to use the constructor here because of the final fields used in this class
        return new SpatialTrack(timesCopy, translationsCopy, rotationsCopy, scalesCopy);
    }
	
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(translations, "translations", null);
        oc.write(rotations, "rotations", null);
        oc.write(times, "times", null);
        oc.write(scales, "scales", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        translations = (CompactVector3Array) ic.readSavable("translations", null);
        rotations = (CompactQuaternionArray) ic.readSavable("rotations", null);
        times = ic.readFloatArray("times", null);
        scales = (CompactVector3Array) ic.readSavable("scales", null);
    }
}
