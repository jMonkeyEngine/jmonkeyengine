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

import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * Base implementation of the Tween interface that provides
 * default implementations of the getLength() and interpolate()
 * methods that provide common tween clamping and bounds checking.
 * Subclasses need only override the doInterpolate() method and
 * the rest is handled for them.
 *
 * @author Paul Speed
 */
public abstract class AbstractTween implements JmeCloneable, Tween {

    private double length;

    protected AbstractTween(double length) {
        this.length = length;
    }

    @Override
    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    /**
     * Default implementation clamps the time value, converts
     * it to 0 to 1.0 based on getLength(), and calls doInterpolate().
     */
    @Override
    public boolean interpolate(double t) {
        if (t < 0) {
            return true;
        }

        // Scale t to be between 0 and 1 for our length
        if (length == 0) {
            t = 1;
        } else {
            t = t / length;
        }

        boolean done = false;
        if (t >= 1.0) {
            t = 1.0;
            done = true;
        }
        doInterpolate(t);
        return !done;
    }

    protected abstract void doInterpolate(double t);

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new tween (not null)
     */
    @Override
    public AbstractTween jmeClone() {
        try {
            AbstractTween clone = (AbstractTween) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned tween into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this tween (not null)
     * @param original the tween from which this tween was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
    }
}
