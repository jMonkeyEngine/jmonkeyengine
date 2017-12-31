package com.jme3.anim.tween.action;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.util.Weighted;
import com.jme3.export.*;

import java.io.IOException;

public abstract class Action implements Tween, Weighted {

    protected Tween[] tweens;
    protected float weight = 1;
    protected double length;
    protected Action parentAction;

    protected Action(Tween... tweens) {
        this.tweens = tweens;
        for (Tween tween : tweens) {
            if (tween instanceof Weighted) {
                ((Weighted) tween).setParentAction(this);
            }
        }
    }

    @Override
    public double getLength() {
        return length;
    }

    @Override
    public boolean interpolate(double t) {
        if (parentAction != null) {
            weight = parentAction.getWeightForTween(this);
        }

        return doInterpolate(t);
    }

    public abstract float getWeightForTween(Tween tween);

    public abstract boolean doInterpolate(double t);

    public abstract void reset();

    @Override
    public void setParentAction(Action parentAction) {
        this.parentAction = parentAction;
    }
}
