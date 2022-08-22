/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlendAction extends BlendableAction {

    private int firstActiveIndex;
    private int secondActiveIndex;
    final private BlendSpace blendSpace;
    private float blendWeight;
    final private Tween[] scaledActions;
    final private Map<HasLocalTransform, Transform> targetMap = new HashMap<>();

    public BlendAction(BlendSpace blendSpace, BlendableAction... actions) {
        this(blendSpace, false, actions);
    }

    /**
     *
     * @param blendSpace The blend space used for calculating blend weight
     * @param smartStretch If smart stretch it is false, actions that do not have the same length will
     *                     stretch to the max length, if it is true, they will be looped when possible
     *                     before stretching. Gives the best result when max transition weight is set
     *                     to something lower than 1 (e.g. 0.5).
     * @param actions The actions to blend
     */
    public BlendAction(BlendSpace blendSpace, boolean smartStretch, BlendableAction... actions) {
        super(actions);
        this.scaledActions = new Tween[actions.length];
        this.blendSpace = blendSpace;
        blendSpace.setBlendAction(this);

        for (BlendableAction action : actions) {
            if (action.getLength() > getLength()) {
                setLength(action.getLength());
            }
            Collection<HasLocalTransform> targets = action.getTargets();
            for (HasLocalTransform target : targets) {
                Transform t = targetMap.get(target);
                if (t == null) {
                    t = new Transform();
                    targetMap.put(target, t);
                }
            }
        }

        //Blending effect maybe unexpected when blended animation don't have the same length
        //Stretching any action that doesn't have the same length.
        for (int i = 0; i < this.actions.length; i++) {
            double actionLength = this.actions[i].getLength();
            if (actionLength != getLength()) {
                if (actionLength > 0 && getLength() > 0) {
                    Tween action = this.actions[i];
                    if (smartStretch) {
                        // Check if we can loop it before stretching
                        int count = (int) (Math.round(getLength() / actionLength));
                        if (count > 1) {
                            action = new Loop(action, count);
                        }
                    }
                    this.scaledActions[i] = Tweens.stretch(getLength(), action);
                } else {
                    // If action length is 0, don't do anything
                    this.scaledActions[i] = this.actions[i];
                }
            } else {
                // No need to stretch if action length equals to max length
                this.scaledActions[i] = this.actions[i];
            }
        }
    }

    @Override
    public void doInterpolate(double t) {
        blendWeight = blendSpace.getWeight();
        BlendableAction firstActiveAction = (BlendableAction) actions[firstActiveIndex];
        BlendableAction secondActiveAction = (BlendableAction) actions[secondActiveIndex];
        firstActiveAction.setCollectTransformDelegate(this);
        secondActiveAction.setCollectTransformDelegate(this);

        // Only interpolate the first action if the weight is below 1.
        if (blendWeight < 1f) {
            firstActiveAction.setWeight(1f);
            scaledActions[firstActiveIndex].interpolate(t);
            if (blendWeight == 0) {
                for (HasLocalTransform target : targetMap.keySet()) {
                    collect(target, targetMap.get(target));
                }
            }
        }

        //Second action should be interpolated
        secondActiveAction.setWeight(blendWeight);
        scaledActions[secondActiveIndex].interpolate(t);

        firstActiveAction.setCollectTransformDelegate(null);
        secondActiveAction.setCollectTransformDelegate(null);

    }

    protected Action[] getActions() {
        return actions;
    }

    public BlendSpace getBlendSpace() {
        return blendSpace;
    }

    protected void setFirstActiveIndex(int index) {
        this.firstActiveIndex = index;
    }

    protected void setSecondActiveIndex(int index) {
        this.secondActiveIndex = index;
    }

    @Override
    public Collection<HasLocalTransform> getTargets() {
        return targetMap.keySet();
    }

    @Override
    public void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source) {

        Transform tr = targetMap.get(target);
        if (weight == 1) {
            tr.set(t);
        } else if (weight > 0) {
            tr.interpolateTransforms(tr, t, weight);
        }

        if (source == actions[secondActiveIndex]) {
            collect(target, tr);
        }
    }

    private void collect(HasLocalTransform target, Transform tr) {
        if (collectTransformDelegate != null) {
            collectTransformDelegate.collectTransform(target, tr, this.getWeight(), this);
        } else {
            if (getTransitionWeight() == 1) {
                target.setLocalTransform(tr);
            } else {
                Transform trans = target.getLocalTransform();
                trans.interpolateTransforms(trans, tr, getTransitionWeight());
                target.setLocalTransform(trans);
            }
        }
    }

    private static class Loop implements Tween {

        private final Tween delegate;
        private final double length;

        public Loop(Tween delegate, int count) {
            this.delegate = delegate;
            this.length = count * delegate.getLength();
        }

        @Override
        public double getLength() {
            return length;
        }

        @Override
        public boolean interpolate(double t) {
            if (t < 0) {
                return true;
            }

            delegate.interpolate(t % delegate.getLength());
            return t < length;
        }
    }
}
