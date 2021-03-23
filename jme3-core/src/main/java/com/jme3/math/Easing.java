/*
 * Copyright (c) 2017-2021 jMonkeyEngine
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
package com.jme3.math;

/**
 * Expose several Easing function from Robert Penner
 * Created by Nehon on 26/03/2017.
 */
public class Easing {
    /**
     * a function that always returns 0
     */
    public static EaseFunction constant = new EaseFunction() {
        @Override
        public float apply(float value) {
            return 0;
        }
    };
    /**
     * In
     */
    public static EaseFunction linear = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value;
        }
    };

    /**
     * a function that returns the square of its input
     */
    public static EaseFunction inQuad = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value;
        }
    };

    /**
     * a function that returns the cube of its input
     */
    public static EaseFunction inCubic = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value * value;
        }
    };

    /**
     * a function that returns the 4th power of its input
     */
    public static EaseFunction inQuart = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value * value * value;
        }
    };

    /**
     * a function that returns the 5th power of its input
     */
    public static EaseFunction inQuint = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value * value * value * value;
        }
    };

    /**
     * Out Elastic and bounce
     */
    public static EaseFunction outElastic = new EaseFunction() {
        @Override
        public float apply(float value) {
            return FastMath.pow(2f, -10f * value) * FastMath.sin((value - 0.3f / 4f) * (2f * FastMath.PI) / 0.3f) + 1f;
        }
    };

    /**
     * a function that starts quickly, then bounces several times
     */
    public static EaseFunction outBounce = new EaseFunction() {
        @Override
        public float apply(float value) {
            if (value < (1f / 2.75f)) {
                return (7.5625f * value * value);
            } else if (value < (2f / 2.75f)) {
                return (7.5625f * (value -= (1.5f / 2.75f)) * value + 0.75f);
            } else if (value < (2.5 / 2.75)) {
                return (7.5625f * (value -= (2.25f / 2.75f)) * value + 0.9375f);
            } else {
                return (7.5625f * (value -= (2.625f / 2.75f)) * value + 0.984375f);
            }
        }
    };

    /**
     * In Elastic and bounce
     */
    public static EaseFunction inElastic = new Invert(outElastic);
    /**
     * a function containing a series of increasing bounces
     */
    public static EaseFunction inBounce = new Invert(outBounce);

    /**
     * Out
     */
    public static EaseFunction outQuad = new Invert(inQuad);
    public static EaseFunction outCubic = new Invert(inCubic);
    public static EaseFunction outQuart = new Invert(inQuart);
    public static EaseFunction outQuint = new Invert(inQuint);

    /**
     * inOut
     */
    public static EaseFunction inOutQuad = new InOut(inQuad, outQuad);
    public static EaseFunction inOutCubic = new InOut(inCubic, outCubic);
    public static EaseFunction inOutQuart = new InOut(inQuart, outQuart);
    public static EaseFunction inOutQuint = new InOut(inQuint, outQuint);
    public static EaseFunction inOutElastic = new InOut(inElastic, outElastic);
    public static EaseFunction inOutBounce = new InOut(inBounce, outBounce);

    /**
     * Extra functions
     */
    public static EaseFunction smoothStep = new EaseFunction() {
        @Override
        public float apply(float t) {
            return t * t * (3f - 2f * t);
        }
    };

    public static EaseFunction smootherStep = new EaseFunction() {
        @Override
        public float apply(float t) {
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }
    };

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Easing() {
    }

    /**
     * An Ease function composed of 2 sb function for custom in and out easing
     */
    public static class InOut implements EaseFunction {

        final private EaseFunction in;
        final private EaseFunction out;

        /**
         * Instantiate a function that blends 2 pre-existing functions.
         *
         * @param in the function to use at value=0
         * @param out the function to use at value=1
         */
        public InOut(EaseFunction in, EaseFunction out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public float apply(float value) {
            if (value < 0.5) {
                value = value * 2;
                return in.apply(value) / 2;
            } else {
                value = (value - 0.5f) * 2;
                return out.apply(value) / 2 + 0.5f;
            }
        }
    }

    private static class Invert implements EaseFunction {

        final private EaseFunction func;

        public Invert(EaseFunction func) {
            this.func = func;
        }

        @Override
        public float apply(float value) {
            return 1f - func.apply(1f - value);
        }
    }
}
