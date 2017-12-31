package com.jme3.anim.tween.action;

import com.jme3.anim.tween.Tween;

public class LinearBlendSpace implements BlendSpace {

    private BlendAction action;
    private float value;
    private float maxValue;
    private float step;

    public LinearBlendSpace(float maxValue) {
        this.maxValue = maxValue;

    }

    @Override
    public void setBlendAction(BlendAction action) {
        this.action = action;
        Tween[] tweens = action.getTweens();
        step = maxValue / (float) (tweens.length - 1);
    }

    @Override
    public float getWeight() {
        Tween[] tweens = action.getTweens();
        float lowStep = 0, highStep = 0;
        int lowIndex = 0, highIndex = 0;
        for (int i = 0; i < tweens.length && highStep < value; i++) {
            lowStep = highStep;
            lowIndex = i;
            highStep += step;
        }
        highIndex = lowIndex + 1;

        action.setFirstActiveTween(tweens[lowIndex]);
        action.setSecondActiveTween(tweens[highIndex]);

        if (highStep == lowStep) {
            return 0;
        }

        return (value - lowStep) / (highStep - lowStep);
    }

    @Override
    public void setValue(float value) {
        this.value = value;
    }
}
