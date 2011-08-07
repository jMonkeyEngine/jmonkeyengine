package com.jme3.animation;

import com.jme3.export.Savable;

public interface Animation extends Savable, Cloneable {
    
    public String getName();
    
    public float getLength();
    
    public void setTime(float time, float blendAmount, AnimControl control, AnimChannel channel);
    
    public Animation clone();
}
