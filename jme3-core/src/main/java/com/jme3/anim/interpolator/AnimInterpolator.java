package com.jme3.anim.interpolator;

import static com.jme3.anim.interpolator.FrameInterpolator.TrackDataReader;
import static com.jme3.anim.interpolator.FrameInterpolator.TrackTimeReader;

/**
 * Created by nehon on 15/04/17.
 */
public abstract class AnimInterpolator<T> {

    public abstract T interpolate(float t, int currentIndex, TrackDataReader<T> data, TrackTimeReader times, T store);

}
