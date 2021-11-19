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

import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;
import com.jme3.util.clone.Cloner;
import java.util.Collection;

/**
 * An implementation of the Action interface, used to manage the interpolation code
 * for its subclasses based on the action length (duration) and time per frames in seconds.
 *
 * Example of its implementation :
 * <li> {@link ClipAction} and {@link BlendAction}. </li>
 *
 * To implement this interface, override the methods :
 * <li>{@link BlendableAction#doInterpolate(double)} and place your interpolation management code.</li>
 * <li>{@link BlendableAction#collectTransform(HasLocalTransform, Transform, float, BlendableAction)} and place your interpolation code based on the #{@link BlendableAction#weight}.</li>
 * <li>{@link BlendableAction#getTargets()} and place the code to get target objects subjected to interpolation.</li>
 *
 * <b>Created by Nehon.</b>
 */
public abstract class BlendableAction extends Action {

    protected BlendableAction collectTransformDelegate;
    private float transitionWeight = 1.0f;
    private double transitionLength = 0.4f;
    private float weight = 1f;
    private TransitionTween transition = new TransitionTween(transitionLength);

    /**
     * Callback to the constructor of the super class to add some actions to {@link Action#actions}.
     * @param tweens actions in raw tween {@link AbstractTween} and its descendants( ex : {@link com.jme3.anim.tween.Tweens#parallel(Tween...)},
     *               or an array of {@link Action} and its descendants(ex : {@link ClipAction} and {@link BaseAction}.
     * @see Action#Action(Tween...)
     */
    public BlendableAction(Tween... tweens) {
        super(tweens);
    }

    //******************The Abstract Methods used by this abstract pattern****************************
    /**
     * Override this method to manage the interpolation code inside.
     * Called by {@link BlendableAction#interpolate(double)}, after clamping time(t) to a ratio between (0)
     * and (1) based on the action length.
     * @param t the current time of frames.
     */
    protected abstract void doInterpolate(double t);

    /**
     * Override this method to be able to collect the target objects for this action that we would apply the interpolation on them.
     * @return the current involved targets.
     */
    public abstract Collection<HasLocalTransform> getTargets();

    /**
     * Override this method to be able to interpolate collected tracks based on the custom weight {@link BlendableAction#weight} from a delegated source.
     * This method is called indirectly by delegating from another BlendableActions sources using {@link BlendableAction#setCollectTransformDelegate(BlendableAction)}.
     * @param target the target involved for interpolation.
     * @param t the transform to be used in interpolation.
     * @param weight the scale factor(delta) used for interpolation.
     * @param source the source of the method call(Source of delegation).
     */
    public abstract void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source);

    //******************End of Abstract Methods****************************

    /**
     * Delegates the interpolation to a specific entity of type {@link BlendableAction} based on its weight attribute.
     * @param delegate the instance, of type {@link BlendableAction}, used for delegation.
     */
    public void setCollectTransformDelegate(BlendableAction delegate) {
        this.collectTransformDelegate = delegate;
    }

    @Override
    public boolean interpolate(double t) {
        // Sanity check the inputs
        if (t < 0) {
            return true;
        }
        //if the interpolation isn't delegated to a subclass instance, then calculate the transitionWeight from here.
        if (collectTransformDelegate == null) {
            //clamp the transition length to the length of the global action, if its out of range.
            if (transition.getLength() > getLength()) {
                transition.setLength(getLength());
            }
            //calculate the transitionWeight time factor per transition of values from 0 to 1.0d
            if(isForward()) {
                transition.interpolate(t);
            } else {
                //otherwise, subtract the current time of frames from the whole duration(step back) then recalculate the transitionWeight.
                float v = Math.max((float)(getLength() - t), 0f);
                transition.interpolate(v);
            }
        } else {
            //otherwise, set the transitionWeight to max(1).
            transitionWeight = 1f;
        }

        if (weight == 0) {
            //weight is 0 let's not interpolate
            return t < getLength();
        }
        //callback for the abstract method doInterpolate()
        // -- example : ClipAction class (Single Action) and BlendAction class (blending between 2 actions).
        doInterpolate(t);
        //return true and interpolate again as long as the frame time hasn't exceeded the action length in fps.
        return t < getLength();
    }

    /**
     * Gets the action weight, which acts as a scale factor for the action transformations.
     * Default value is 1.0f.
     * @return the action weight.
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Sets the weight of the blendable action.
     *
     * The Action Weight : scales the actions transformations before interpolating between them,
     * i.e : how much we want this action to run in a ratio from 0 to 1.
     * <b> Notes : </b>
     * <li> Valid animation weight is between (0) and (1) </li>
     * <li> Weight attribute is used for interpolation only if the delegation flag is activated, otherwise the default transitionWeight is utilized. </li>
     * Default value = 1.0f.
     * @param weight the weight/scaleFactor of the action.
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Gets the interpolation transition duration.
     * Default value is 0.4f.
     * @return the transition length.
     */
    public double getTransitionLength() {
        return transitionLength;
    }

    /**
     * Sets the interpolation transition duration used to calculate the transitionWeight.
     * Default value is 0.4f.
     * @param transitionLength the value of the transition length to settle.
     */
    public void setTransitionLength(double transitionLength) {
        this.transitionLength = transitionLength;
        this.transition.setLength(transitionLength);
    }
    /**
     * Gets the default transitionWeight calculated by the default tween along (0 - {@link BlendableAction#getTransitionLength()})
     * The transition weight is used as the default (undelegated) delta value of interpolation between subsequent transformations.
     * Default value is 1.0f.
     * @return the interpolation transition weight.
     */
    protected float getTransitionWeight() {
        return transitionWeight;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new action (not null)
     */
    @Override
    public BlendableAction jmeClone() {
        try {
            BlendableAction blendableAction = (BlendableAction) super.clone();
            return blendableAction;
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
        super.cloneFields(cloner, original);
        collectTransformDelegate = cloner.clone(collectTransformDelegate);
        transition = cloner.clone(transition);
    }

    /**
     * A default implementation of a Tween used to qualify the value of {@link BlendableAction#transitionWeight}
     * based on the formula (t = t / length), where transitionWeight is a scale factor between (0 and 1) representing a time step towards the transitionLength (maxTime).
     */
    private class TransitionTween extends AbstractTween {

        public TransitionTween(double length) {
            super(length);
        }

        @Override
        protected void doInterpolate(double t) {
            //evaluating the default transition time through the (t / length)
            //which is a part of the current action length; where (t) is the time per frame, an increasing value.
            transitionWeight = (float) t;
        }
    }

}
