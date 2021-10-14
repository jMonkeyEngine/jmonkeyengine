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

/**
 * A Concrete implementation for the interface #{@link BlendSpace}, used to adjust and control the
 * blendWeight value of the blendAction based on the ratio between the space length & the step used per action,
 * this spaceRatio can be adjusted (increased/decreased) from the #{@link LinearBlendSpace#value}.
 * <br/>
 * <b>Created by Nehon.</b>
 */
public class LinearBlendSpace implements BlendSpace {

    private BlendAction action;
    private float value;
    final private float maxValue;
    final private float minValue;
    private float step;

    /**
     * Creates a linear space to calculate the blendWeight, which correlates with the spaceRatio
     * @param minValue the estimated start value of the linear space.
     * @param maxValue the estimated end value of the linear space.
     */
    public LinearBlendSpace(float minValue, float maxValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    @Override
    public void setBlendAction(BlendAction action) {
        this.action = action;
        //calculate the requested step per action, based on the spaceLength & the number of steps among actions.
        final float estimatedSpaceLength = maxValue - minValue;
        final int numberOfSteps = action.getActions().length - 1;
        step = estimatedSpaceLength / numberOfSteps;
    }

    @Override
    public float getWeight() {
        //start stepping from the start value of the linearSpace.
        float lowStep = minValue, highStep = minValue;
        int lowIndex = 0;
        //iterate over the actions and increment the respective step.
        for (int i = 0; i < action.getActions().length && highStep < value; i++, highStep += step) {
            lowStep = highStep;
            lowIndex = i;
        }
        final int highIndex = lowIndex + 1;

        //set the active actions used by the blendAction instance.
        action.setFirstActiveIndex(lowIndex);
        action.setSecondActiveIndex(highIndex);

        //terminate with blendSpace zero if the linear space slider didn't move
        if (highStep == lowStep) {
            return 0;
        }
        //calculate the final space length from the variable parsed & the pre-last step moved.
        final float spaceLength = value - lowStep;
        //divide the linear space onto the step length to find the spaceRatio.
        return spaceLength / step;
    }

    /**
     * The value used to control the spaceRatio which changes out the blendWeight.
     * @param value the value in floats.
     */
    @Override
    public void setValue(float value) {
        this.value = value;
    }
}
