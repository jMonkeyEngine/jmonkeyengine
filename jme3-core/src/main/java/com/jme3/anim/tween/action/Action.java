package com.jme3.anim.tween.action;

import com.jme3.anim.AnimationMask;
import com.jme3.anim.tween.Tween;

public abstract class Action implements Tween {

    protected Action[] actions;
    private double length;
    private double speed = 1;
    private AnimationMask mask;
    private boolean forward = true;

    protected Action(Tween... tweens) {
        this.actions = new Action[tweens.length];
        for (int i = 0; i < tweens.length; i++) {
            Tween tween = tweens[i];
            if (tween instanceof Action) {
                this.actions[i] = (Action) tween;
            } else {
                this.actions[i] = new BaseAction(tween);
            }
        }
    }

    @Override
    public double getLength() {
        return length;
    }

    protected void setLength(double length) {
        this.length = length;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        if( speed < 0){
            setForward(false);
        } else {
            setForward(true);
        }
    }

    public AnimationMask getMask() {
        return mask;
    }

    public void setMask(AnimationMask mask) {
        this.mask = mask;
    }

    protected boolean isForward() {
        return forward;
    }

    protected void setForward(boolean forward) {
        if(this.forward == forward){
            return;
        }
        this.forward = forward;
        for (Action action : actions) {
            action.setForward(forward);
        }

    }
}
