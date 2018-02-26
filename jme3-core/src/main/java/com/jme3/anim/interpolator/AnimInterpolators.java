package com.jme3.anim.interpolator;

import com.jme3.math.*;

import static com.jme3.anim.interpolator.FrameInterpolator.TrackDataReader;
import static com.jme3.anim.interpolator.FrameInterpolator.TrackTimeReader;

/**
 * Created by nehon on 15/04/17.
 */
public class AnimInterpolators {

    //Rotation interpolators

    public static final AnimInterpolator<Quaternion> NLerp = new AnimInterpolator<Quaternion>() {
        private Quaternion next = new Quaternion();

        @Override
        public Quaternion interpolate(float t, int currentIndex, TrackDataReader<Quaternion> data, TrackTimeReader times, Quaternion store) {
            data.getEntryClamp(currentIndex, store);
            data.getEntryClamp(currentIndex + 1, next);
            store.nlerp(next, t);
            return store;
        }
    };

    public static final AnimInterpolator<Quaternion> SLerp = new AnimInterpolator<Quaternion>() {
        private Quaternion next = new Quaternion();

        @Override
        public Quaternion interpolate(float t, int currentIndex, TrackDataReader<Quaternion> data, TrackTimeReader times, Quaternion store) {
            data.getEntryClamp(currentIndex, store);
            data.getEntryClamp(currentIndex + 1, next);
            //MathUtils.slerpNoInvert(store, next, t, store);
            MathUtils.slerp(store, next, t, store);
            return store;
        }
    };

    public static final AnimInterpolator<Quaternion> SQuad = new AnimInterpolator<Quaternion>() {
        private Quaternion a = new Quaternion();
        private Quaternion b = new Quaternion();

        private Quaternion q0 = new Quaternion();
        private Quaternion q1 = new Quaternion();
        private Quaternion q2 = new Quaternion();
        private Quaternion q3 = new Quaternion();

        @Override
        public Quaternion interpolate(float t, int currentIndex, TrackDataReader<Quaternion> data, TrackTimeReader times, Quaternion store) {
            data.getEntryModSkip(currentIndex - 1, q0);
            data.getEntryModSkip(currentIndex, q1);
            data.getEntryModSkip(currentIndex + 1, q2);
            data.getEntryModSkip(currentIndex + 2, q3);
            MathUtils.squad(q0, q1, q2, q3, a, b, t, store);
            return store;
        }
    };

    //Position / Scale interpolators
    public static final AnimInterpolator<Vector3f> LinearVec3f = new AnimInterpolator<Vector3f>() {
        private Vector3f next = new Vector3f();

        @Override
        public Vector3f interpolate(float t, int currentIndex, TrackDataReader<Vector3f> data, TrackTimeReader times, Vector3f store) {
            data.getEntryClamp(currentIndex, store);
            data.getEntryClamp(currentIndex + 1, next);
            store.interpolateLocal(next, t);
            return store;
        }
    };
    /**
     * CatmullRom interpolation
     */
    public static final CatmullRomInterpolator CatmullRom = new CatmullRomInterpolator();

    public static class CatmullRomInterpolator extends AnimInterpolator<Vector3f> {
        private Vector3f p0 = new Vector3f();
        private Vector3f p1 = new Vector3f();
        private Vector3f p2 = new Vector3f();
        private Vector3f p3 = new Vector3f();
        private float tension = 0.7f;

        public CatmullRomInterpolator(float tension) {
            this.tension = tension;
        }

        public CatmullRomInterpolator() {
        }

        @Override
        public Vector3f interpolate(float t, int currentIndex, TrackDataReader<Vector3f> data, TrackTimeReader times, Vector3f store) {
            data.getEntryModSkip(currentIndex - 1, p0);
            data.getEntryModSkip(currentIndex, p1);
            data.getEntryModSkip(currentIndex + 1, p2);
            data.getEntryModSkip(currentIndex + 2, p3);

            FastMath.interpolateCatmullRom(t, tension, p0, p1, p2, p3, store);
            return store;
        }
    }

    //Time Interpolators

    public static class TimeInterpolator extends AnimInterpolator<Float> {
        private EaseFunction ease;

        public TimeInterpolator(EaseFunction ease) {
            this.ease = ease;
        }

        @Override
        public Float interpolate(float t, int currentIndex, TrackDataReader<Float> data, TrackTimeReader times, Float store) {
            return ease.apply(t);
        }
    }

    //in
    public static final TimeInterpolator easeInQuad = new TimeInterpolator(Easing.inQuad);
    public static final TimeInterpolator easeInCubic = new TimeInterpolator(Easing.inCubic);
    public static final TimeInterpolator easeInQuart = new TimeInterpolator(Easing.inQuart);
    public static final TimeInterpolator easeInQuint = new TimeInterpolator(Easing.inQuint);
    public static final TimeInterpolator easeInBounce = new TimeInterpolator(Easing.inBounce);
    public static final TimeInterpolator easeInElastic = new TimeInterpolator(Easing.inElastic);

    //out
    public static final TimeInterpolator easeOutQuad = new TimeInterpolator(Easing.outQuad);
    public static final TimeInterpolator easeOutCubic = new TimeInterpolator(Easing.outCubic);
    public static final TimeInterpolator easeOutQuart = new TimeInterpolator(Easing.outQuart);
    public static final TimeInterpolator easeOutQuint = new TimeInterpolator(Easing.outQuint);
    public static final TimeInterpolator easeOutBounce = new TimeInterpolator(Easing.outBounce);
    public static final TimeInterpolator easeOutElastic = new TimeInterpolator(Easing.outElastic);

    //inout
    public static final TimeInterpolator easeInOutQuad = new TimeInterpolator(Easing.inOutQuad);
    public static final TimeInterpolator easeInOutCubic = new TimeInterpolator(Easing.inOutCubic);
    public static final TimeInterpolator easeInOutQuart = new TimeInterpolator(Easing.inOutQuart);
    public static final TimeInterpolator easeInOutQuint = new TimeInterpolator(Easing.inOutQuint);
    public static final TimeInterpolator easeInOutBounce = new TimeInterpolator(Easing.inOutBounce);
    public static final TimeInterpolator easeInOutElastic = new TimeInterpolator(Easing.inOutElastic);

    //extra
    public static final TimeInterpolator smoothStep = new TimeInterpolator(Easing.smoothStep);
    public static final TimeInterpolator smootherStep = new TimeInterpolator(Easing.smootherStep);

    public static final TimeInterpolator constant = new TimeInterpolator(Easing.constant);


}
