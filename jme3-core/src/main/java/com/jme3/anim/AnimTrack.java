package com.jme3.anim;

import com.jme3.export.Savable;
import com.jme3.util.clone.JmeCloneable;

public interface AnimTrack<T> extends Savable, JmeCloneable {

    public void getDataAtTime(double time, T store);
    public double getLength();


}
