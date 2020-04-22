/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

public abstract class Action implements JmeCloneable, Tween {

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
