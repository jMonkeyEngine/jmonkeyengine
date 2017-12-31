package com.jme3.anim.tween.action;

import com.jme3.anim.tween.action.BlendAction;

public interface BlendSpace {

    public void setBlendAction(BlendAction action);

    public float getWeight();

    public void setValue(float value);
}
