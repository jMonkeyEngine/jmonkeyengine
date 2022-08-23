package com.jme3.anim.tween.action;

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlendAction extends BlendableAction {

    private int firstActiveIndex;
    private int secondActiveIndex;
    final private BlendSpace blendSpace;
    private float blendWeight;
    final private double[] timeFactor;
    private double[] speedFactors;
    final private Map<HasLocalTransform, Transform> targetMap = new HashMap<>();

    public BlendAction(BlendSpace blendSpace, BlendableAction... actions) {
        super(actions);
        timeFactor = new double[actions.length];
        this.blendSpace = blendSpace;
        blendSpace.setBlendAction(this);

        for (BlendableAction action : actions) {
            if (action.getLength() > getLength()) {
                setLength(action.getLength());
            }
            Collection<HasLocalTransform> targets = action.getTargets();
            for (HasLocalTransform target : targets) {
                Transform t = targetMap.get(target);
                if (t == null) {
                    t = new Transform();
                    targetMap.put(target, t);
                }
            }
        }

        //Blending effect maybe unexpected when blended animation don't have the same length
        //Stretching any action that doesn't have the same length.
        for (int i = 0; i < this.actions.length; i++) {
            this.timeFactor[i] = 1;
            if (this.actions[i].getLength() != getLength()) {
                double actionLength = this.actions[i].getLength();
                if (actionLength > 0 && getLength() > 0) {
                    this.timeFactor[i] = this.actions[i].getLength() / getLength();
                }
            }
        }

        // Generate speed factors used to resolve slow motion effect on stretched
        // actions by dynamically calculating speed.
        speedFactors = calculateSpeedFactors(this);
    }

    @Override
    public void doInterpolate(double t) {
        blendWeight = blendSpace.getWeight();
        BlendableAction firstActiveAction = (BlendableAction) actions[firstActiveIndex];
        BlendableAction secondActiveAction = (BlendableAction) actions[secondActiveIndex];
        firstActiveAction.setCollectTransformDelegate(this);
        secondActiveAction.setCollectTransformDelegate(this);

        // Only interpolate the first action if the weight is below 1.
        if (blendWeight < 1f) {
            firstActiveAction.setWeight(1f);
            firstActiveAction.interpolate(t * timeFactor[firstActiveIndex]);
            if (blendWeight == 0) {
                for (HasLocalTransform target : targetMap.keySet()) {
                    collect(target, targetMap.get(target));
                }
            }
        }

        //Second action should be interpolated
        secondActiveAction.setWeight(blendWeight);
        secondActiveAction.interpolate(t * timeFactor[secondActiveIndex]);

        firstActiveAction.setCollectTransformDelegate(null);
        secondActiveAction.setCollectTransformDelegate(null);

    }

    protected Action[] getActions() {
        return actions;
    }

    public BlendSpace getBlendSpace() {
        return blendSpace;
    }

    @Override
    public double getSpeed() {
        if (speedFactors != null) {
            return super.getSpeed() * FastMath.interpolateLinear(blendWeight,
                    (float) speedFactors[firstActiveIndex],
                    (float) speedFactors[secondActiveIndex]);
        }

        return super.getSpeed();
    }

    /**
     * @return The speed factor or null if there is none
     */
    public double[] getSpeedFactors() {
        return speedFactors;
    }

    /**
     * Used to resolve the slow motion side effect caused by stretching actions that
     * doesn't have the same length.
     *
     * @param speedFactors The speed factors for each child action. BlendAction will
     *                     interpolate factor for current frame based on blend weight
     *                     and will multiply it to speed.
     */
    public void setSpeedFactors(double... speedFactors) {
        if (speedFactors.length != actions.length) {
            throw new IllegalArgumentException("Array length must be " + actions.length);
        }

        this.speedFactors = speedFactors;
    }

    public void clearSpeedFactors() {
        this.speedFactors = null;
    }

    /**
     * BlendAction will stretch it's child actions if they don't have the same length.
     * This might cause stretched animations to run slowly. This method generates the factor
     * based on how much actions are stretched. Multiplying this factor to base speed will
     * nullify the slow animation side effect caused by stretching. BlendAction will use the
     * blend weight taken from BlendSpace to interpolate the speed factor for current frame.
     *
     * @param blendAction The blend action used to generate speed factors for it
     * @return new array
     */
    public static double[] calculateSpeedFactors(BlendAction blendAction) {
        Action[] actions = blendAction.getActions();
        return Arrays.stream(actions)
                .mapToDouble(action -> blendAction.getLength() / action.getLength())
                .toArray();
    }

    protected void setFirstActiveIndex(int index) {
        this.firstActiveIndex = index;
    }

    protected void setSecondActiveIndex(int index) {
        this.secondActiveIndex = index;
    }

    @Override
    public Collection<HasLocalTransform> getTargets() {
        return targetMap.keySet();
    }

    @Override
    public void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source) {

        Transform tr = targetMap.get(target);
        if (weight == 1) {
            tr.set(t);
        } else if (weight > 0) {
            tr.interpolateTransforms(tr, t, weight);
        }

        if (source == actions[secondActiveIndex]) {
            collect(target, tr);
        }
    }

    private void collect(HasLocalTransform target, Transform tr) {
        if (collectTransformDelegate != null) {
            collectTransformDelegate.collectTransform(target, tr, this.getWeight(), this);
        } else {
            if (getTransitionWeight() == 1) {
                target.setLocalTransform(tr);
            } else {
                Transform trans = target.getLocalTransform();
                trans.interpolateTransforms(trans, tr, getTransitionWeight());
                target.setLocalTransform(trans);
            }
        }
    }

}
