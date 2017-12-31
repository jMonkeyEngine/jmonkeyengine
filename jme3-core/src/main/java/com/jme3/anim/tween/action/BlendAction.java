package com.jme3.anim.tween.action;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;

public class BlendAction extends Action {


    private Tween firstActiveTween;
    private Tween secondActiveTween;
    private BlendSpace blendSpace;
    private float blendWeight;

    public BlendAction(BlendSpace blendSpace, Tween... tweens) {
        super(tweens);
        this.blendSpace = blendSpace;
        blendSpace.setBlendAction(this);

        for (Tween tween : tweens) {
            if (tween.getLength() > length) {
                length = tween.getLength();
            }
        }

        //Blending effect maybe unexpected when blended animation don't have the same length
        //Stretching any tween that doesn't have the same length.
        for (int i = 0; i < tweens.length; i++) {
            if (tweens[i].getLength() != length) {
                tweens[i] = Tweens.stretch(length, tweens[i]);
            }
        }

    }

    @Override
    public float getWeightForTween(Tween tween) {
        blendWeight = blendSpace.getWeight();
        if (tween == firstActiveTween) {
            return 1f;
        }
        return weight * blendWeight;
    }

    @Override
    public boolean doInterpolate(double t) {
        if (firstActiveTween == null) {
            blendSpace.getWeight();
        }

        boolean running = this.firstActiveTween.interpolate(t);
        this.secondActiveTween.interpolate(t);

        if (!running) {
            return false;
        }

        return true;
    }

    @Override
    public void reset() {

    }

    protected Tween[] getTweens() {
        return tweens;
    }

    public BlendSpace getBlendSpace() {
        return blendSpace;
    }

    protected void setFirstActiveTween(Tween firstActiveTween) {
        this.firstActiveTween = firstActiveTween;
    }

    protected void setSecondActiveTween(Tween secondActiveTween) {
        this.secondActiveTween = secondActiveTween;
    }
}
