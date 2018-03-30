package com.jme3.anim.tween.action;

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlendAction extends BlendableAction {

    private int firstActiveIndex;
    private int secondActiveIndex;
    private BlendSpace blendSpace;
    private float blendWeight;
    private double[] timeFactor;
    private Map<HasLocalTransform, Transform> targetMap = new HashMap<>();

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
    }

    public void doInterpolate(double t) {
        blendWeight = blendSpace.getWeight();
        BlendableAction firstActiveAction = (BlendableAction) actions[firstActiveIndex];
        BlendableAction secondActiveAction = (BlendableAction) actions[secondActiveIndex];
        firstActiveAction.setCollectTransformDelegate(this);
        secondActiveAction.setCollectTransformDelegate(this);

        //only interpolate the first action if the weight if below 1.
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
