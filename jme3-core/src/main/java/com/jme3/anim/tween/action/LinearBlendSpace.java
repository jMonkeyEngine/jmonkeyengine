package com.jme3.anim.tween.action;

public class LinearBlendSpace implements BlendSpace {

    private BlendAction action;
    private float value;
    final private float maxValue;
    final private float minValue;
    private float step;

    public LinearBlendSpace(float minValue, float maxValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    @Override
    public void setBlendAction(BlendAction action) {
        this.action = action;
        Action[] actions = action.getActions();
        step = (maxValue - minValue) / (actions.length - 1);
    }

    @Override
    public float getWeight() {
        Action[] actions = action.getActions();
        float lowStep = minValue, highStep = minValue;
        int lowIndex = 0, highIndex = 0;
        for (int i = 0; i < actions.length && highStep < value; i++) {
            lowStep = highStep;
            lowIndex = i;
            highStep += step;
        }
        highIndex = lowIndex + 1;

        action.setFirstActiveIndex(lowIndex);
        action.setSecondActiveIndex(highIndex);

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
