package com.jme3.anim;

import com.jme3.export.Savable;
import com.jme3.util.clone.JmeCloneable;

/**
 * Interface to derive animation data from a track.
 *
 * @param <T> the type of data that's being animated, such as Transform
 */
public interface AnimTrack<T> extends Savable, JmeCloneable {

    /**
     * Determine the track value for the specified time.
     *
     * @param time the track time (in seconds)
     * @param store storage for the value (not null, modified)
     */
    public void getDataAtTime(double time, T store);

    /**
     * Determine the duration of the track.
     *
     * @return the duration (in seconds, &ge;0)
     */
    public double getLength();
}
