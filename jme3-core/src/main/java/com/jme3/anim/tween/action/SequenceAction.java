package com.jme3.anim.tween.action;

import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.tween.Tween;

public class SequenceAction extends Action {

    private int currentIndex = 0;
    private double accumTime;
    private double transitionTime = 0;
    private float mainWeight = 1.0f;
    private double transitionLength = 0.4f;
    private TransitionTween transition = new TransitionTween(transitionLength);


    public SequenceAction(Tween... tweens) {
        super(tweens);
        for (Tween tween : tweens) {
            length += tween.getLength();
        }
    }

    @Override
    public float getWeightForTween(Tween tween) {
        return weight * mainWeight;
    }

    @Override
    public boolean doInterpolate(double t) {
        Tween currentTween = tweens[currentIndex];
        if (transition.getLength() > currentTween.getLength()) {
            transition.setLength(currentTween.getLength());
        }

        transition.interpolate(t - transitionTime);

        boolean running = currentTween.interpolate(t - accumTime);
        if (!running) {
            accumTime += currentTween.getLength();
            currentIndex++;
            transitionTime = accumTime;
            transition.setLength(transitionLength);
        }

        if (t >= length) {
            reset();
            return false;
        }
        return true;
    }

    public void reset() {
        currentIndex = 0;
        accumTime = 0;
        transitionTime = 0;
        mainWeight = 1;
    }

    public void setTransitionLength(double transitionLength) {
        this.transitionLength = transitionLength;
    }

    private class TransitionTween extends AbstractTween {


        public TransitionTween(double length) {
            super(length);
        }

        @Override
        protected void doInterpolate(double t) {
            mainWeight = (float) t;
        }
    }
}
