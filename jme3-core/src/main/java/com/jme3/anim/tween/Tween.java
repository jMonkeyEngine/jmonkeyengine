/*
 * Copyright (c) 2015-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
package com.jme3.anim.tween;

/**
 * Represents some action that interpolates across input between 0
 * and some length value.  (For example, movement, rotation, fading.)
 * It's also possible to have zero length 'instant' tweens.
 *
 * @author Paul Speed
 */
public interface Tween extends Cloneable {

    /**
     * Returns the length of the tween.  If 't' represents time in
     * seconds then this is the notional time in seconds that the tween
     * will run.  Note: all the caveats are because tweens may be
     * externally scaled in such a way that 't' no longer represents
     * actual time.
     *
     * @return the duration (in de-scaled seconds)
     */
    public double getLength();

    /**
     * Sets the implementation specific interpolation to the
     * specified 'tween' value as a value in the range from 0 to
     * getLength().  If the value is greater or equal to getLength()
     * then it is internally clamped and the method returns false.
     * If 't' is still in the tween's range then this method returns
     * true.
     *
     * @param t animation time (in de-scaled seconds)
     * @return true if t&gt;length(), otherwise false
     */
    public boolean interpolate(double t);

}

