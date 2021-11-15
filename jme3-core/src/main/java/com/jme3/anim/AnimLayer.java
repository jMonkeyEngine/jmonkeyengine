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
package com.jme3.anim;

import com.jme3.anim.tween.action.Action;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * A portion of an AnimComposer that can run (at most) one Action at a time.
 *
 * <p>A composer with multiple layers can run multiple actions simultaneously.
 * For instance, one layer could run a "wave" action on the model's upper body
 * while another ran a "walk" action on the model's lower body.
 *
 * <p>Each layer has a name, which is stored in the composer, not in the layer
 * itself.  A layer cannot be shared between multiple composers.
 *
 * <p>Animation time may advance at a different rate from real time, based on
 * speedup factors set in the composer and the current Action.
 */
public class AnimLayer implements JmeCloneable {
    /**
     * The Action currently running on this layer, or null if none.
     */
    private Action currentAction;
    /**
     * The composer that owns this layer. Were it not for cloning, this field
     * would be final.
     */
    private AnimComposer composer;
    /**
     * Limits the portion of the model animated by this layer. If null, this
     * layer can animate the entire model.
     */
    private final AnimationMask mask;
    /**
     * The current animation time, in scaled seconds. Always non-negative.
     */
    private double time;
    /**
     * The software object (such as an AnimEvent) that currently controls this
     * layer, or null if unknown.
     */
    private Object manager;

    /**
     * Instantiates a layer without a manager or a current Action, owned by the
     * specified composer.
     *
     * @param composer the owner (not null, alias created)
     * @param mask the AnimationMask (alias created) or null to allow this layer
     *     to animate the entire model
     */
    AnimLayer(AnimComposer composer, AnimationMask mask) {
        assert composer != null;
        this.composer = composer;

        this.mask = mask;
    }

    /**
     * Returns the Action that's currently running.
     *
     * @return the pre-existing instance, or null if none
     */
    public Action getCurrentAction() {
        return currentAction;
    }

    /**
     * Returns the current manager.
     *
     * @return the pre-existing object (such as an AnimEvent) or null for
     *     unknown
     */
    public Object getManager() {
        return manager;
    }

    /**
     * Returns the animation mask.
     *
     * @return the pre-existing instance, or null if this layer can animate the
     *     entire model
     */
    public AnimationMask getMask() {
        return mask;
    }

    /**
     * Returns the animation time, in scaled seconds.
     *
     * @return the current animation time (not negative)
     */
    public double getTime() {
        return time;
    }

    /**
     * Runs the specified Action, starting from time = 0. This cancels any
     * Action previously running on this layer.
     *
     * @param actionToRun the Action to run (alias created) or null for no
     *     action
     */
    public void setCurrentAction(Action actionToRun) {
        this.time = 0.0;
        this.currentAction = actionToRun;
    }

    /**
     * Assigns the specified manager. This cancels any manager previously
     * assigned.
     *
     * @param desiredManager the desired manager (such as an AnimEvent, alias
     *     created) or null for unknown manager
     */
    public void setManager(Object desiredManager) {
        this.manager = desiredManager;
    }

    /**
     * Changes the animation time, wrapping the specified time to fit the
     * current Action. An Action be running.
     *
     * @param desiredAnimationTime the desired time (in scaled seconds)
     */
    public void setTime(double desiredAnimationTime) {
        double length = currentAction.getLength();
        if (desiredAnimationTime >= 0.0) {
            time = desiredAnimationTime % length;
        } else {
            time = (desiredAnimationTime % length) + length;
        }
    }

    /**
     * Updates the animation time and the current Action during a
     * controlUpdate().
     *
     * @param realSeconds the amount to advance the current Action, in real
     *     seconds
     */
    void update(float realSeconds) {
        if (currentAction == null) {
            return;
        }

        double speedup = currentAction.getSpeed() * composer.getGlobalSpeed();
        double scaledSeconds = speedup * realSeconds;
        time += scaledSeconds;

        // wrap negative times to the [0, length] range:
        if (time < 0.0) {
            double length = currentAction.getLength();
            time = (time % length + length) % length;
        }

        // update the current Action, filtered by this layer's mask:
        currentAction.setMask(mask);
        boolean running = currentAction.interpolate(time);
        currentAction.setMask(null);

        if (!running) { // went past the end of the current Action
            time = 0.0;
        }
    }

    /**
     * Converts this shallow-cloned layer into a deep-cloned one, using the
     * specified Cloner and original to resolve copied fields.
     *
     * <p>The clone's current Action gets nulled out. Its manager and mask get
     * aliased to the original's manager and mask.
     *
     * @param cloner the Cloner that's cloning this layer (not null)
     * @param original the instance from which this layer was shallow-cloned
     *     (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        composer = cloner.clone(composer);
        currentAction = null;
    }

    @Override
    public Object jmeClone() {
        try {
            AnimLayer clone = (AnimLayer) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError();
        }
    }
}
