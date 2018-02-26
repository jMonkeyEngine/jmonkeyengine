package com.jme3.anim.interpolator;

import com.jme3.animation.*;
import com.jme3.math.*;

/**
 * Created by nehon on 15/04/17.
 */
public class FrameInterpolator {

    public static final FrameInterpolator DEFAULT = new FrameInterpolator();

    private AnimInterpolator<Float> timeInterpolator;
    private AnimInterpolator<Vector3f> translationInterpolator = AnimInterpolators.LinearVec3f;
    private AnimInterpolator<Quaternion> rotationInterpolator = AnimInterpolators.NLerp;
    private AnimInterpolator<Vector3f> scaleInterpolator = AnimInterpolators.LinearVec3f;

    private TrackDataReader<Vector3f> translationReader = new TrackDataReader<>();
    private TrackDataReader<Quaternion> rotationReader = new TrackDataReader<>();
    private TrackDataReader<Vector3f> scaleReader = new TrackDataReader<>();
    private TrackTimeReader timesReader = new TrackTimeReader();


    private Transform transforms = new Transform();

    public Transform interpolate(float t, int currentIndex, CompactVector3Array translations, CompactQuaternionArray rotations, CompactVector3Array scales, float[] times){
        timesReader.setData(times);
        if( timeInterpolator != null){
            t = timeInterpolator.interpolate(t,currentIndex, null, timesReader, null );
        }
        if(translations != null) {
            translationReader.setData(translations);
            translationInterpolator.interpolate(t, currentIndex, translationReader, timesReader, transforms.getTranslation());
        }
        if(rotations != null) {
            rotationReader.setData(rotations);
            rotationInterpolator.interpolate(t, currentIndex, rotationReader, timesReader, transforms.getRotation());
        }
        if(scales != null){
            scaleReader.setData(scales);
            scaleInterpolator.interpolate(t, currentIndex, scaleReader, timesReader, transforms.getScale());
        }
        return transforms;
    }

    public void interpolateWeights(float t, int currentIndex, float[] weights, int nbMorphTargets, float[] store) {
        int start = currentIndex * nbMorphTargets;
        for (int i = 0; i < nbMorphTargets; i++) {
            int current = start + i;
            int next = current + nbMorphTargets;
            if (next >= weights.length) {
                next = current;
            }

            float val =  FastMath.interpolateLinear(t, weights[current], weights[next]);
            store[i] = val;
        }
    }

    public void setTimeInterpolator(AnimInterpolator<Float> timeInterpolator) {
        this.timeInterpolator = timeInterpolator;
    }

    public void setTranslationInterpolator(AnimInterpolator<Vector3f> translationInterpolator) {
        this.translationInterpolator = translationInterpolator;
    }

    public void setRotationInterpolator(AnimInterpolator<Quaternion> rotationInterpolator) {
        this.rotationInterpolator = rotationInterpolator;
    }

    public void setScaleInterpolator(AnimInterpolator<Vector3f> scaleInterpolator) {
        this.scaleInterpolator = scaleInterpolator;
    }


    public static class TrackTimeReader {
        private float[] data;

        protected void setData(float[] data) {
            this.data = data;
        }

        public float getEntry(int index) {
            return data[mod(index, data.length)];
        }

        public int getLength() {
            return data.length;
        }
    }

    public static class TrackDataReader<T> {

        private CompactArray<T> data;

        protected void setData(CompactArray<T> data) {
            this.data = data;
        }

        public T getEntryMod(int index, T store) {
            return data.get(mod(index, data.getTotalObjectSize()), store);
        }

        public T getEntryClamp(int index, T store) {
            index = (int) FastMath.clamp(index, 0, data.getTotalObjectSize() - 1);
            return data.get(index, store);
        }

        public T getEntryModSkip(int index, T store) {
            int total = data.getTotalObjectSize();
            if (index == -1) {
                index--;
            } else if (index >= total) {
                index++;
            }

            index = mod(index, total);


            return data.get(index, store);
        }
    }

    /**
     * Euclidean modulo (cycle on 0,n instead of -n,0; 0,n)
     *
     * @param val
     * @param n
     * @return
     */
    private static int mod(int val, int n) {
        return ((val % n) + n) % n;
    }

}
