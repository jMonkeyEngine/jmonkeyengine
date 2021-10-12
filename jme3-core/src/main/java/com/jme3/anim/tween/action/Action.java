/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.anim.tween.action;

import com.jme3.anim.AnimationMask;
import com.jme3.anim.Armature;
import com.jme3.anim.tween.Tween;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * <h2>Base implementation interface(1st level impl/prototype) for the interface #{@link Tween} specialized to the new animation system.
 * The Action class encloses an array of {@link Tween} and converts them into jme animation actions if they actually aren't.</h2>
 * <h3>NB : this interface mimics the {@link com.jme3.anim.tween.AbstractTween}, but it delegates the interpolation method #{@link Tween#interpolate(double)}
 * to the #{@link BlendableAction} class.</h3>
 * Subclasses need to make a constructor call passing in the Tween Actions to enclose them & override #{@link Action#interpolate(double)}.<br/>
 * <u>Example of operation(Uses Abstract Factory Pattern Method(interpolate/doInterpolate)) :</u><br/>
 * <br/>
 * #Check out #{@link BlendableAction} class.<br/>
 * <code>
 *  public abstract class AbstractAction extends Action {<br/>
 *      private double length;
 *      private volatile float interpolationDelta = 0f;
 *      public ActionImpl(Tween... tweenActions){<br/>
 *          super(tweenActions);<br/>
 *      }<br/>
 *      <code>@</code>Override<br/>
 *      public boolean interpolate(double t){<br/>
 *          //animation is out or in a reverse order.<br/>
 *          if(t < 0){<br/>
 *              return true;<br/>
 *          }<br/>
 *          //find where is the interpolation slider for transition.<br/>
 *          //interpolationDelta would be -plugged into the interpolateTransforms(transformI, transformII, delta)- as a shared value.<br/>
 *          interpolationDelta = t / length;<br/>
 *          doInterpolate(t);<br/>
 *          //interpolate as long as the interpolation slider haven't reached the full strength.<br/>
 *          return interpolationDelta < 1.0d;<br/>
 *      }<br/>
 *       <code>@</code>Override<br/>
 *      public double getLength(){<br/>
 *           return length;<br/>
 *      }<br/>
 *       <code>@</code>Override<br/>
 *      public void cloneFields(Cloner cloner, Object original) {<br/>
 *         //deep clone fields
 *      }<br/>
 *      <code>@</code>Override<br/>
 *      public Action jmeClone() {<br/>
 *          //shallow clone<br/>
 *         try {<br/>
 *             return (Action) super.clone();<br/>
 *         } catch (CloneNotSupportedException exception) {<br/>
 *             throw new RuntimeException(exception);<br/>
 *         }<br/>
 *     }<br/>
 *      //override this factory method to do interpolation<br/>
 *      protected abstract void doInterpolate(double t);<br/>
 *  }<br/>
 * </code>
 * <b>Inspired by Nehon.</b>
 */
public abstract class Action implements JmeCloneable, Tween {

    protected Action[] actions;
    private double length;
    //the default speed is 1, which results in a normal tpf.
    private double speed = 1;
    private AnimationMask mask;
    private boolean forward = true;

    /**
     * Instantiate an action object which encloses a couple of tween animation actions.
     * @param tweens the tween actions to enclose.
     */
    protected Action(Tween... tweens) {
        this.actions = new Action[tweens.length];
        for (int i = 0; i < tweens.length; i++) {
            Tween tween = tweens[i];
            //enclose the tween in a baseAction & collect its children actions, if the passed tween isn't really an animation action - cast otherwise - then save the tween action in the actions[].
            if (tween instanceof Action) {
                this.actions[i] = (Action) tween;
            } else {
                this.actions[i] = new BaseAction(tween);
            }
        }
    }

    /**
     * Gets the length aka the duration of the current whole action that encloses Tweens.
     * @return the length of the action.
     */
    @Override
    public double getLength() {
        return length;
    }

    /**
     * Alter the length (duration) of this Action.  This can be used to extend
     * or truncate an Action.
     * @param length the desired length (in unscaled seconds, default=0)
     */
    public void setLength(double length) {
        this.length = length;
    }

    /**
     * Gets the speed of the frames of this action which gets applied for the Layer enclosing this action.
     * Gets applied each advance() by this formula : time += tpf * currentAction.getSpeed() * ac.globalSpeed.
     * @return the speed of frames.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Sets the speed of the frames of this action which gets applied for the Layer enclosing this action.
     * Gets applied each advance() by this formula : time += tpf * currentAction.getSpeed() * ac.globalSpeed.
     * @param speed the speed of frames.
     */
    public void setSpeed(double speed) {
        this.speed = speed;
        setForward(speed < 0);
    }

    /**
     * Gets the animation mask for this action.
     * the animation mask holds data(subsets of an object) about which part of the animation would be played.
     * @return the animation mask instance.
     */
    public AnimationMask getMask() {
        return mask;
    }

    /**
     * Sets the animation mask for this action.
     * the animation mask holds the info about which part(subsets of an object) of the animation to play, like joints in #{@link com.jme3.anim.ArmatureMask#addFromJoint(Armature, String)}
     * @param mask the animation mask instance.
     */
    public void setMask(AnimationMask mask) {
        this.mask = mask;
    }

    /**
     * Checks for the forward boolean flag.
     * @return true if the animation action meant to be seek in a forward manner, false otherwise.
     */
    protected boolean isForward() {
        return forward;
    }

    /**
     * The forward flag, controls the animation action directionality.
     * <br/>
     * <b>forward = true : ensures that animation would seek in a forward manner.</b>
     * <b>forward = false : ensures that animation would seek in a backward manner.</b>
     * @param forward true if the animation is meant to be seek forward, false otherwise.
     */
    protected void setForward(boolean forward) {
        if(this.forward == forward){
            return;
        }
        this.forward = forward;
        for (Action action : actions) {
            action.setForward(forward);
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new action (not null)
     */
    @Override
    public Action jmeClone() {
        try {
            return (Action) super.clone();
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
