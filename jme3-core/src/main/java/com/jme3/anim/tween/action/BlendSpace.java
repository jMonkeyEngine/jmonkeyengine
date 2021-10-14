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
 * An interface used to adjust & control the blending weight value
 * #{@link BlendSpace#getWeight()} during application runtime based on a value #{@link BlendSpace#setValue(float)}.
 *
 * The blend weight is the delta value used for interpolating the target transforms extracted from actions (parsed from #{@link BlendAction} constructor).
 * The most proper blendWeight values subset this interval [0, 1].
 *
 * To use this interface, you have to implement it and return the desired #{@link BlendSpace#getWeight()} used for interpolation, assuming that
 * you have settled the firstActiveAction and the secondActiveAction using #{@link BlendAction#setFirstActiveIndex(int)},
 * #{@link BlendAction#setSecondActiveIndex(int)}.
 *
 * To be able to add other different values used to adjust the blendWeight accordingly, extend this interface, add the new values,
 * then create your own implementation class.
 *
 * Example showing the usage : #{@link LinearBlendSpace}.
 *
 * <br/>
 * <b>Created by Nehon</b>
 */
public interface BlendSpace {

    /**
     * Sets the target blendAction instance that we would like to adjust & control its actions' weights.
     * @param action the blendAction instance.
     */
    void setBlendAction(BlendAction action);

    /**
     * Used for passing the desired weight (delta value) to the selected #{@link BlendAction} instance,
     * this delta value would be used for interpolating a collection of actions' transformation matrices.
     * @return the blendingWeight value.
     */
    float getWeight();

    /**
     * The value to be used for adjusting the blending weight value returned from #{@link BlendSpace#getWeight()}.
     * @param value the value in floats.
     */
    void setValue(float value);
}
