package com.jme3.math;

/**
 * Expose several Easing function from Robert Penner
 * Created by Nehon on 26/03/2017.
 */
public class Easing {


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

    public static EaseFunction inQuad = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value;
        }
    };

    public static EaseFunction inCubic = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value * value;
        }
    };

    public static EaseFunction inQuart = new EaseFunction() {
        @Override
        public float apply(float value) {
            return value * value * value * value;
        }
    };

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
     * An Ease function composed of 2 sb function for custom in and out easing
     */
    public static class InOut implements EaseFunction {

        private EaseFunction in;
        private EaseFunction out;

        public InOut(EaseFunction in, EaseFunction out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public float apply(float value) {
            if (value < 0.5) {
                value = value * 2;
                return inQuad.apply(value) / 2;
            } else {
                value = (value - 0.5f) * 2;
                return outQuad.apply(value) / 2 + 0.5f;
            }
        }
    }

    private static class Invert implements EaseFunction {

        private EaseFunction func;

        public Invert(EaseFunction func) {
            this.func = func;
        }

        @Override
        public float apply(float value) {
            return 1f - func.apply(1f - value);
        }
    }


}
