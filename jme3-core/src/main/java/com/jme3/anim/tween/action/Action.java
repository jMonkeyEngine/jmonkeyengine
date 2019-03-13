package com.jme3.anim.tween.action;

import com.jme3.anim.AnimationMask;
import com.jme3.anim.tween.Tween;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

public abstract class Action implements JmeCloneable, Tween {

    protected Action[] actions;
    private double length;
    private double speed = 1;
    private AnimationMask mask;
    private boolean forward = true;
    private boolean loop = true;

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
        if (speed < 0) {
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

    public boolean isForward() {
        return forward;
    }

    protected void setForward(boolean forward) {
        if (this.forward == forward) {
            return;
        }
        this.forward = forward;
        for (Action action : actions) {
            action.setForward(forward);
        }

    }

    /**
     * @return True if the action will keep looping after it is done playing,
     * otherwise, false.
     */
    public boolean isLooping() {
        return loop;
    }

    /**
     * Set the looping mode for the action. The default is true.
     *
     * @param loop True if the action should keep looping after it is done
     * playing.
     */
    public void setLooping(boolean loop) {
        this.loop = loop;
    }

    /**
     * @return The current execution time for this action.
     */
    public abstract double getTime();

    /**
     * @return The remaining time left for this action. If the action is looping
     * then this returns the remaining time for the current loop iteration.
     */
    public double getRemaining() {
        if (getTime() < 0) {
            return getLength();
        }
        return Math.max(0, getLength() - getTime());
    }

    /**
     * @return The remaining time as a scaled value between 0 and 1.0.
     */
    public double getPercentRemaining() {
        return getRemaining() / getLength();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new action (not null)
     */
    @Override
    public Action jmeClone() {
        try {
            Action clone = (Action) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned action into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this action (not null)
     * @param original the action from which this action was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        actions = cloner.clone(actions);
        mask = cloner.clone(mask);
    }
}
