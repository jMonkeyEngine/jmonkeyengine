package com.jme3.anim.tween.action;

import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;

import java.util.Collection;

public abstract class BlendableAction extends Action {

    protected BlendableAction collectTransformDelegate;
    private float transitionWeight = 1.0f;
    private double transitionLength = 0.4f;
    private float weight = 1f;
    private TransitionTween transition = new TransitionTween(transitionLength);

    public BlendableAction(Tween... tweens) {
        super(tweens);
    }

    public void setCollectTransformDelegate(BlendableAction delegate) {
        this.collectTransformDelegate = delegate;
    }

    @Override
    public boolean interpolate(double t) {
        // Sanity check the inputs
        if (t < 0) {
            return true;
        }

        if (collectTransformDelegate == null) {
            if (transition.getLength() > getLength()) {
                transition.setLength(getLength());
            }
            if(isForward()) {
                transition.interpolate(t);
            } else {
                float v = Math.max((float)(getLength() - t), 0f);
                transition.interpolate(v);
            }
        } else {
            transitionWeight = 1f;
        }

        if (weight == 0) {
            //weight is 0 let's not interpolate
            return t < getLength();
        }

        doInterpolate(t);

        return t < getLength();
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    protected abstract void doInterpolate(double t);

    public abstract Collection<HasLocalTransform> getTargets();

    public abstract void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source);

    public double getTransitionLength() {
        return transitionLength;
    }

    public void setTransitionLength(double transitionLength) {
        this.transitionLength = transitionLength;
    }

    protected float getTransitionWeight() {
        return transitionWeight;
    }

    private class TransitionTween extends AbstractTween {


        public TransitionTween(double length) {
            super(length);
        }

        @Override
        protected void doInterpolate(double t) {
            transitionWeight = (float) t;
        }
    }

}
