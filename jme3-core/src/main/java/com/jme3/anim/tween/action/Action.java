/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
import com.jme3.anim.tween.Tween;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * Wraps an array of Tween actions into an action object.
 * 
 * <p>
 * Notes :
 * <li> The sequence of tweens is determined by {@link com.jme3.anim.tween.Tweens} utility class and the {@link BaseAction} interpolates that sequence. </li>
 * <li> This implementation mimics the {@link com.jme3.anim.tween.AbstractTween}, but it delegates the interpolation method {@link Tween#interpolate(double)}
 * to the {@link BlendableAction} class. </li>
 * </p>
 * 
 * Created by Nehon.
 *
 * @see BlendableAction
 * @see BaseAction
 */
public abstract class Action implements JmeCloneable, Tween {
    
    /**
     * A sequence of actions which wraps given tween actions.
     */
    protected Action[] actions;
    private double length;
    private double speed = 1;
    private AnimationMask mask;
    private boolean forward = true;
    
    /**
     * Instantiates an action object that wraps a tween actions array by extracting their actions to the collection {@link Action#actions}.
     * <p>
     * Notes :
     * <li> If intentions are to wrap some tween actions, then subclasses have to call this constructor, examples : {@link BlendableAction} and {@link BlendAction}. </li>
     * <li> If intentions are to make an implementation of {@link Action} that shouldn't wrap tweens of actions, then subclasses shouldn't call this
     * constructor, examples : {@link ClipAction} and {@link BaseAction}. </li>
     * </p> 
     *
     * @param tweens the tween actions to be wrapped.
     */    
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
    
    /**
     * Retrieves the length (the duration) of the current action.
     *
     * @return the length of the action in seconds.
     */
    @Override
    public double getLength() {
        return length;
    }

    /**
     * Alters the length (duration) of this Action. This can be used to extend
     * or truncate an Action.
     *
     * @param length the desired length (in unscaled seconds, default=0)
     */
    public void setLength(double length) {
        this.length = length;
    }

    /**
     * Retrieves the speedup factor applied by the layer for this action.
     *
     * @see Action#setSpeed(double) for detailed documentation
     * @return the speed of frames.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Alters the speedup factor applied by the layer running this action.
     * <p>
     * Notes:
     * <li> This factor controls the animation direction, if the speed is a positive value then the animation will run forward and vice versa. </li>
     * <li> The speed factor gets applied, inside the {@link com.jme3.anim.AnimLayer}, on each interpolation step by this formula : time += tpf * action.getSpeed() * composer.globalSpeed. </li>
     * <li> Default speed is 1.0, it plays the animation clips at their normal speed. </li>
     * <li> Setting the speed factor to Zero will stop the animation, while setting it to a negative number will play the animation in a backward fashion. </li>
     * </p>
     * 
     * @param speed the speed of frames.
     */
    public void setSpeed(double speed) {
        this.speed = speed;
        if (speed < 0) {
            setForward(false);
        } else {
            setForward(true);
        }
    }

    /**
     * Retrieves the animation mask for this action.
     * The animation mask controls which part of the model would be animated. A model part can be
     * registered using a {@link com.jme3.anim.Joint}.
     *
     * @return the animation mask instance.
     */
    public AnimationMask getMask() {
        return mask;
    }

    /**
     * Adjusts the animation mask for this action.
     * The animation mask controls which part of the model would be animated. A model part can be
     * registered using a {@link com.jme3.anim.Joint}.
     *
     * @param mask the animation mask instance.
     */
    public void setMask(AnimationMask mask) {
        this.mask = mask;
    }

    /**
     * Creates a shallow clone for the JME cloner.
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
    
    /**
     * Tests the value of the forward flag.
     *
     * @return true if the animation action is running forward, false otherwise.
     */
    protected boolean isForward() {
        return forward;
    }

    /**
     * Adjusts the forward flag which controls the animation action directionality.
     *
     * @param forward true to run the animation forward, false otherwise.
     * @see Action#setSpeed(double) to change the directionality of the tween actions, negative numbers play the animation in a backward fashion
     */
    protected void setForward(boolean forward) {
        if (this.forward == forward) {
            return;
        }
        this.forward = forward;
        for (Action action : actions) {
            action.setForward(forward);
        }
    }
}
