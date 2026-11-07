package com.jme3.anim.tween.action;

import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

public class LinearBlendSpace implements BlendSpace, JmeCloneable {

    private BlendAction action;
    private float value;
    final private float maxValue;
    final private float minValue;
    private float step;

    public LinearBlendSpace(float minValue, float maxValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new blend space (not null)
     */
    @Override
    public LinearBlendSpace jmeClone() {
        try {
            return (LinearBlendSpace) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Resolve the owning action through the same cloner as this blend space.
     *
     * @param cloner the cloner that's cloning this blend space (not null)
     * @param original the blend space from which this one was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        action = cloner.clone(action);
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
