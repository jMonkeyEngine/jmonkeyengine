package com.jme3.anim.tween.action;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.util.Weighted;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;

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

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        tweens = (Tween[]) ic.readSavableArray("tweens", null);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(tweens, "tweens", null);
    }
}
