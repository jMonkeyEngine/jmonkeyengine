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

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A concrete implementation used to blend between 2 actions #{@link BlendAction#firstActiveIndex}, #{@link BlendAction#secondActiveIndex} & apply them on their targets,
 * blending between 2 actions is done by interpolating the set of transforms serially starting from the firstAction transforms, moving on to the second one transforms,
 * using the blendWeight as a delta value, then interpolating the resulted value with the current target local transforms,
 * and finalizing by applying the final collected result of interpolation to the target as a local transform matrix.
 *
 * If the blendWeight is :
 * - less than 1, then interpolate the 1st action & the second one & apply the results.
 * - equal to zero, then interpolate the 1st action only & apply the results.
 * - equal to one, then interpolate only the second action with blendWeight = 1.
 *
 * <b>Created by Nehon.</b>
 */
public class BlendAction extends BlendableAction {

    private int firstActiveIndex;
    private int secondActiveIndex;
    final private BlendSpace blendSpace;
    private double[] timeFactor;
    final private Map<HasLocalTransform, Transform> targetMap = new HashMap<>();

    /**
     * Creates an action that utilizes a blend space to blend between 2 selected actions from the collection of actions provided.
     * @param blendSpace the blendSpace, used for calculating the blendWeight
     * @param actions the collection of actions, used for selecting 2 actions to blend between them.
     */
    public BlendAction(BlendSpace blendSpace, BlendableAction... actions) {
        super(actions);
        this.blendSpace = blendSpace;
        blendSpace.setBlendAction(this);

        //prepare for interpolating actions by gathering targets & timeFactors based on the actionLength & the global length.
        gatherTargetsFromActions(actions);
        gatherTimeFactorPerAction(actions);
    }

    /**
     * Used for gathering the timeFactor or the timeDelta value for the inscribed actions.
     * The timeFactor : is used to scale the tpf for the current running inscribed action.
     * Each action should have its own timeFactor used for interpolation.
     * @param actions the actions, used for fetching the timeFactor values.
     */
    private void gatherTimeFactorPerAction(final BlendableAction... actions){
        timeFactor = new double[actions.length];
        for (int i = 0; i < actions.length; i++) {
            //get the action duration.
            final double actionLength = actions[i].getLength();
            if (actionLength > 0 && getLength() > 0) {
                //get the timeFactor(timeScale) of the inscribed action with respect to its parent Action.
                this.timeFactor[i] = actionLength / getLength();
            }else{
                //assign a default value if durations are negative
                this.timeFactor[i] = 1;
            }
        }
    }

    /**
     * Used for gathering the targets & prepare them from the parsed anim actions args.
     * @param actions the actions used for gathering targets.
     */
    private void gatherTargetsFromActions(final BlendableAction... actions){
        for (final BlendableAction action : actions) {
            //stretch the blend action length(duration) to the max possible length to accommodate the actions.
            if (action.getLength() > getLength()) {
                setLength(action.getLength());
            }
            //get the target objects to apply the tracks extracted from the actions on them.
            //NB: this line calls getTargets(); implementations on : ClipAction Class & Your Custom Actions(if exist) to fetch targets.
            final Collection<HasLocalTransform> targets = action.getTargets();
            //iterate over them
            for (final HasLocalTransform target : targets) {
                //get their current local transform
                Transform t = targetMap.get(target);
                //fill the empty map with empty transforms ready to collect interpolations from targets transforms.
                if (t == null) {
                    t = new Transform();
                    targetMap.put(target, t);
                }
            }
        }
    }

    /**
     * The factory method, overridden to manage the interpolation.
     * @param t the current time of frames.
     */
    @Override
    public void doInterpolate(final double t) {
        final float blendWeight = blendSpace.getWeight();
        final BlendableAction firstActiveAction = (BlendableAction) actions[firstActiveIndex];
        final BlendableAction secondActiveAction = (BlendableAction) actions[secondActiveIndex];
        //activate the collection delegation -> will skip the transitionWeight value calculation.
        activateTransformationDelegation(firstActiveAction, secondActiveAction);
        //only interpolate the first action if the weight is below 1.
        if (blendWeight < 1f) {
            interpolateAction(firstActiveAction, 1f, t * timeFactor[firstActiveIndex]);
        }
        //protect the second action from extrapolation & negative values.
        interpolateAction(secondActiveAction, Math.min(Math.max(blendWeight, 0f), 1f), t * timeFactor[secondActiveIndex]);
        //release the resources.
        resetTransformationDelegation(firstActiveAction, secondActiveAction);
    }

    /**
     * Resets the delegation & release the resources.
     * @param firstAction the firstActiveAction to release.
     * @param secondAction the secondActiveAction to release.
     */
    private void resetTransformationDelegation(final BlendableAction firstAction, final BlendableAction secondAction){
        firstAction.setCollectTransformDelegate(null);
        secondAction.setCollectTransformDelegate(null);
    }

    /**
     * Activates the delegation to run this factory method #{@link BlendAction#collectTransform(HasLocalTransform, Transform, float, BlendableAction)}
     * from the firstAction & the secondAction #{@link ClipAction#doInterpolate(double)} method for interpolation.
     * @param firstAction the firstActiveAction.
     * @param secondAction the secondActiveAction.
     */
    private void activateTransformationDelegation(final BlendableAction firstAction, final BlendableAction secondAction){
        firstAction.setCollectTransformDelegate(this);
        secondAction.setCollectTransformDelegate(this);
    }

    /**
     * Interpolates the actions according to a blendingWeight(delta value).
     * @param action the action to interpolate.
     * @param blendWeight the delta factor of interpolation.
     * @param scaledTime the scaledTime of frames, used to get the requested transformations at this time frames before interpolating, check #{@link ClipAction#doInterpolate(double)} line#79 & #{@link com.jme3.anim.TransformTrack#getDataAtTime(double, Transform)}.
     */
    private void interpolateAction(final BlendableAction action, final float blendWeight, final double scaledTime){
        //set the delta value(synonyms: scaleFactor, deltaFactor, nlerpFactor) of interpolation
        action.setWeight(blendWeight);
        //call the factory method doInterpolate() accordingly as long as this action stays in range.
        action.interpolate(scaledTime);
    }

    /**
     * Gets the involved actions.
     * @return an array of involved actions.
     */
    protected Action[] getActions() {
        return actions;
    }

    /**
     * Gets the blendSpace instance used for calculating the blendingWeight.
     * @return the blendSpace instance.
     */
    public BlendSpace getBlendSpace() {
        return blendSpace;
    }

    /**
     * Used to adjust the index of the firstAction of the blending animation.
     * @param index the desired index.
     */
    public void setFirstActiveIndex(final int index) {
        this.firstActiveIndex = index;
    }
    /**
     * Used to adjust the index of the secondAction of the blending animation.
     * @param index the desired index.
     */
    public void setSecondActiveIndex(final int index) {
        this.secondActiveIndex = index;
    }

    /**
     * The factory method overridden to return the gathered targets from {@link BlendAction#gatherTargetsFromActions(BlendableAction...)}.
     * @return the gathered targets, to apply interpolation on them.
     */
    @Override
    public Collection<HasLocalTransform> getTargets() {
        return targetMap.keySet();
    }

    /**
     * A factory method used subsequently with each update for interpolating transforms serially
     * & collect them, which can be used later as tr2 again with the object current local transform.
     * NB : this only run when the TransformDelegation is activated parsing in this instance as a source.
     *
     * --Internal use only--Don't call it manually from outside the API.
     *
     * @param target the target involved for interpolation.
     * @param t the transform to be used for interpolation.
     * @param weight the scale factor(delta) used for interpolation.
     * @param source the source of the method call.
     */
    @Override
    public void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source) {
        //shallow copy into a reference var, at the first action, this field var is empty,
        //by time we collect our interpolated transforms within it, preparing to be used as the blending interpolation value tr2.
        final Transform tr = targetMap.get(target);
        //serially interpolate transforms using the reference, only if a positive rational number between 0 & 1.
        tr.interpolateTransforms(tr, t, Math.max(0, weight));

        //if the delegation source is the 2nd action clip, as the interpolation reaches the 2nd action,
        //then, collect the interpolated values & finalize by interpolating with the current transform and applying the values.
        //if the weight is zero, then skip the second action & collect the data.
        if (source == actions[secondActiveIndex] || blendSpace.getWeight() == 0) {
            collect(target, tr);
        }
    }

    /**
     * Collects data based on the transform delegation criterion flag,
     * if the delegation is activated then collection & interpolation is delegated to the parsed source,
     * using the weight from that source as the delta val of the interpolation, otherwise the interpolation uses the default transitionWeight from #{@link BlendableAction#getTransitionWeight()}.
     * @param target the target to collect data transform from, used for interpolation with the other transform.
     * @param tr the transformation matrix used for interpolation with the current target transform.
     */
    private void collect(final HasLocalTransform target, final Transform tr) {
        if (collectTransformDelegate != null) {
            collectTransformDelegate.collectTransform(target, tr, this.getWeight(), this);
        } else {
            //using the default transition weight, if no delegation.
            final Transform trans = target.getLocalTransform();
            //interpolating again between the current transformation & the interpolated values(from the 1st & second clips).
            trans.interpolateTransforms(trans, tr, getTransitionWeight());
            //apply the values.
            target.setLocalTransform(trans);
        }
    }

}
