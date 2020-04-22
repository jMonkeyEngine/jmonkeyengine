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

public class LinearBlendSpace implements BlendSpace {

    private BlendAction action;
    private float value;
    private float maxValue;
    private float minValue;
    private float step;

    public LinearBlendSpace(float minValue, float maxValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    @Override
    public void setBlendAction(BlendAction action) {
        this.action = action;
        Action[] actions = action.getActions();
        step = (maxValue - minValue) / (actions.length - 1);
    }

    @Override
    public float getWeight() {
        Action[] actions = action.getActions();
        float lowStep = minValue, highStep = minValue;
        int lowIndex = 0, highIndex = 0;
        for (int i = 0; i < actions.length && highStep < value; i++) {
            lowStep = highStep;
            lowIndex = i;
            highStep += step;
        }
        highIndex = lowIndex + 1;

        action.setFirstActiveIndex(lowIndex);
        action.setSecondActiveIndex(highIndex);

        if (highStep == lowStep) {
            return 0;
        }

        return (value - lowStep) / (highStep - lowStep);
    }

    @Override
    public void setValue(float value) {
        this.value = value;
    }
}
