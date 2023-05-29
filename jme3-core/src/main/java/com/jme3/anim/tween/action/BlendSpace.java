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

/**
 * Controls the blending between 2 successive actions in a {@link BlendAction} by adjusting blending weight value
 * {@link BlendSpace#getWeight()} during application runtime based on an arbitrary value {@link BlendSpace#setValue(float)}.
 * 
 * <p>
 * Notes:
 * <li> Blending is the action of mixing between 2 successive animation {@link BlendableAction}s by interpolating their transforms and 
 * then applying the result on the assigned {@link HasLocalTransform} object, the {@link BlendSpace} provides this blending action with a blend weight value. </li>
 * <li> The blend weight is the value for the interpolation for the target transforms. </li>
 * <li> The blend weight value should lie in this interval [0, 1]. </li>
 * </p>
 * 
 * <p>
 * Different blending weight case scenarios managed by {@link BlendAction} internally:
 * <li> In case of (0 < Blending weight < 1), the blending is executed each update among 2 actions, the first action will use 
 * a blend value of 1 and the second action will use the blend space weight as a value for the interpolation. </li>
 * <li> In case of (Blending weight = 0), the blending hasn't started yet, only the first action will be interpolated at (weight = 1). </li>
 * <li> In case of (Blending weight = 1), the blending is finished and only the second action will continue to run at (weight = 1). </li>
 * <li> Negative values and values greater than 1 aren't allowed and will result in an IllegalArgumentException. </li>
 * <li> Find more at {@link BlendAction#doInterpolate(double)} and {@link BlendAction#collectTransform(HasLocalTransform, Transform, float, BlendableAction)}. </li>
 * </p>
 * 
 * Created by Nehon.
 * @see LinearBlendSpace an example of blendspace implementation
 */
public interface BlendSpace {

    /**
     * Adjusts the target blend action instance that will utilize the blend weight value provided by this blend-space implementation.
     * 
     * @param action the blend action instance that will utilize this blend-space.
     */
    public void setBlendAction(BlendAction action);

    /**
     * Provides the blend weight value to the assigned {@link BlendAction} instance,
     * this value will be used for interpolating a collection of actions' transformations (keyframes).
     * 
     * @return the blending weight value.
     * @see LinearBlendSpace#getWeight()
     */
    public float getWeight();

    /**
     * An arbitrary value used for adjusting the blending weight value.
     * 
     * @param value the value in floats.
     * @see LinearBlendSpace#setValue(float)
     */
    public void setValue(float value);
}
