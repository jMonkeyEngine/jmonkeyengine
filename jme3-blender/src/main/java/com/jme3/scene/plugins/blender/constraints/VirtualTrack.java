package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;

import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

/**
 * A virtual track that stores computed frames after constraints are applied.
 * Not all the frames need to be inserted. If there are lacks then the class
 * will fill the gaps.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class VirtualTrack {
    /** The name of the track (for debugging purposes). */
    private String               name;
    /** The last frame for the track. */
    public int                   maxFrame;
    /** The max time for the track. */
    public float                 maxTime;
    /** Translations of the track. */
    public ArrayList<Vector3f>   translations;
    /** Rotations of the track. */
    public ArrayList<Quaternion> rotations;
    /** Scales of the track. */
    public ArrayList<Vector3f>   scales;

    /**
     * Constructs the object storing the maximum frame and time.
     * 
     * @param maxFrame
     *            the last frame for the track
     * @param maxTime
     *            the max time for the track
     */
    public VirtualTrack(String name, int maxFrame, float maxTime) {
        this.name = name;
        this.maxFrame = maxFrame;
        this.maxTime = maxTime;
    }

    /**
     * Sets the transform for the given frame.
     * 
     * @param frameIndex
     *            the frame for which the transform will be set
     * @param transform
     *            the transformation to be set
     */
    public void setTransform(int frameIndex, Transform transform) {
        if (translations == null) {
            translations = this.createList(Vector3f.ZERO, frameIndex);
        }
        this.append(translations, Vector3f.ZERO, frameIndex - translations.size());
        translations.add(transform.getTranslation().clone());

        if (rotations == null) {
            rotations = this.createList(Quaternion.IDENTITY, frameIndex);
        }
        this.append(rotations, Quaternion.IDENTITY, frameIndex - rotations.size());
        rotations.add(transform.getRotation().clone());

        if (scales == null) {
            scales = this.createList(Vector3f.UNIT_XYZ, frameIndex);
        }
        this.append(scales, Vector3f.UNIT_XYZ, frameIndex - scales.size());
        scales.add(transform.getScale().clone());
    }

    /**
     * Returns the track as a bone track.
     * 
     * @param targetBoneIndex
     *            the bone index
     * @return the bone track
     */
    public BoneTrack getAsBoneTrack(int targetBoneIndex) {
        if (translations == null && rotations == null && scales == null) {
            return null;
        }
        return new BoneTrack(targetBoneIndex, this.createTimes(), translations.toArray(new Vector3f[maxFrame]), rotations.toArray(new Quaternion[maxFrame]), scales.toArray(new Vector3f[maxFrame]));
    }

    /**
     * Returns the track as a spatial track.
     * 
     * @return the spatial track
     */
    public SpatialTrack getAsSpatialTrack() {
        if (translations == null && rotations == null && scales == null) {
            return null;
        }
        return new SpatialTrack(this.createTimes(), translations.toArray(new Vector3f[maxFrame]), rotations.toArray(new Quaternion[maxFrame]), scales.toArray(new Vector3f[maxFrame]));
    }

    /**
     * The method creates times for the track based on the given maximum values.
     * 
     * @return the times for the track
     */
    private float[] createTimes() {
        float[] times = new float[maxFrame];
        float dT = maxTime / maxFrame;
        float t = 0;
        for (int i = 0; i < maxFrame; ++i) {
            times[i] = t;
            t += dT;
        }
        return times;
    }

    /**
     * Helper method that creates a list of a given size filled with given
     * elements.
     * 
     * @param element
     *            the element to be put into the list
     * @param count
     *            the list size
     * @return the list
     */
    private <T> ArrayList<T> createList(T element, int count) {
        ArrayList<T> result = new ArrayList<T>(count);
        for (int i = 0; i < count; ++i) {
            result.add(element);
        }
        return result;
    }

    /**
     * Appends the element to the given list.
     * 
     * @param list
     *            the list where the element will be appended
     * @param element
     *            the element to be appended
     * @param count
     *            how many times the element will be appended
     */
    private <T> void append(ArrayList<T> list, T element, int count) {
        for (int i = 0; i < count; ++i) {
            list.add(element);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(2048);
        result.append("TRACK: ").append(name).append('\n');
        if (translations != null && translations.size() > 0) {
            result.append("TRANSLATIONS: ").append(translations.toString()).append('\n');
        }
        if (rotations != null && rotations.size() > 0) {
            result.append("ROTATIONS:    ").append(rotations.toString()).append('\n');
        }
        if (scales != null && scales.size() > 0) {
            result.append("SCALES:       ").append(scales.toString()).append('\n');
        }
        return result.toString();
    }
}