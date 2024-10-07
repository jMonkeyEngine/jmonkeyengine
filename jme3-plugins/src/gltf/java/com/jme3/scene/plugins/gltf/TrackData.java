/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetLoadException;
import com.jme3.math.*;

import java.util.*;

public class TrackData {

    public enum Type {
        Translation,
        Rotation,
        Scale,
        Morph
    }

    Float length;
    float[] times;
    List<TimeData> timeArrays = new ArrayList<>();


    Vector3f[] translations;
    Quaternion[] rotations;
    Vector3f[] scales;
    float[] weights;

    public void update() {

        if (equalTimes(timeArrays)) {
            times = timeArrays.get(0).times;
        } else {
            //Times array are different and contains different sampling times.
            //We have to merge them because JME needs the 3 types of transforms for each keyFrame.

            //extracting keyframes information
            List<KeyFrame> keyFrames = new ArrayList<>();
            TimeData timeData = timeArrays.get(0);
            Type type = timeData.type;
            float lastTime = -1f;
            for (int i = 0; i < timeData.times.length; i++) {
                float time = timeData.times[i];
                //avoid some double keyframes that can have bad effects on interpolation
                if (Float.floatToIntBits(time) == Float.floatToIntBits(lastTime)) {
                    lastTime = time;
                    continue;
                }
                lastTime = time;
                KeyFrame keyFrame = new KeyFrame();
                keyFrame.time = time;
                setKeyFrameTransforms(type, keyFrame, timeData.times);
                keyFrames.add(keyFrame);
            }

            for (int i = 1; i < timeArrays.size(); i++) {
                timeData = timeArrays.get(i);
                type = timeData.type;
                for (float time : timeData.times) {
                    for (int j = 0; j < keyFrames.size(); j++) {
                        KeyFrame kf = keyFrames.get(j);
                        if (Float.floatToIntBits(kf.time) != Float.floatToIntBits(time)) {
                            if (time > kf.time) {
                                if (j < keyFrames.size() - 1) {
                                    // Keep searching for the insertion point.
                                    continue;
                                }
                                kf = new KeyFrame();
                                kf.time = time;
                                // Add kf after the last keyframe in the list.
                                keyFrames.add(kf);
                            } else {
                                kf = new KeyFrame();
                                kf.time = time;
                                keyFrames.add(j, kf);
                                //we inserted a keyframe let's shift the counter.
                                j++;
                            }
                        }
                        setKeyFrameTransforms(type, kf, timeData.times);
                        break;
                    }
                }
            }
            // populating transforms array from the keyframes, interpolating
            times = new float[keyFrames.size()];

            ensureArraysLength();

            TransformIndices translationIndices = new TransformIndices();
            TransformIndices rotationIndices = new TransformIndices();
            TransformIndices scaleIndices = new TransformIndices();

            for (int i = 0; i < keyFrames.size(); i++) {
                KeyFrame kf = keyFrames.get(i);
                //we need Interpolate between keyframes when transforms are sparse.
                times[i] = kf.time;
                if (translations != null) {
                    populateTransform(Type.Translation, i, keyFrames, kf, translationIndices);
                }
                if (rotations != null) {
                    populateTransform(Type.Rotation, i, keyFrames, kf, rotationIndices);
                }
                if (scales != null) {
                    populateTransform(Type.Scale, i, keyFrames, kf, scaleIndices);
                }
            }
        }

        if (times[0] > 0) {
            //Anim doesn't start at 0, JME can't handle that and will interpolate transforms linearly from 0 to the first frame of the anim.
            //we need to add a frame at 0 that copies the first real frame

            float[] newTimes = new float[times.length + 1];
            newTimes[0] = 0f;
            System.arraycopy(times, 0, newTimes, 1, times.length);
            times = newTimes;

            if (translations != null) {
                Vector3f[] newTranslations = new Vector3f[translations.length + 1];
                newTranslations[0] = translations[0];
                System.arraycopy(translations, 0, newTranslations, 1, translations.length);
                translations = newTranslations;
            }
            if (rotations != null) {
                Quaternion[] newRotations = new Quaternion[rotations.length + 1];
                newRotations[0] = rotations[0];
                System.arraycopy(rotations, 0, newRotations, 1, rotations.length);
                rotations = newRotations;
            }
            if (scales != null) {
                Vector3f[] newScales = new Vector3f[scales.length + 1];
                newScales[0] = scales[0];
                System.arraycopy(scales, 0, newScales, 1, scales.length);
                scales = newScales;
            }
            if (weights != null) {
                int nbMorph = weights.length / (times.length - 1);
                float[] newWeights = new float[weights.length + nbMorph];
                System.arraycopy(weights, 0, newWeights, 0, nbMorph);
                System.arraycopy(weights, 0, newWeights, nbMorph, weights.length);
                weights = newWeights;
            }
        }

        checkTimesConsistency();

        length = times[times.length - 1];
    }

    /**
     * Verify that the
     * {@link #times}, {@link #translations}, {@link #rotations}, and
     * {@link #scales} vectors all have the same length, if present.
     *
     * @throws AssetLoadException if the lengths differ
     */
    public void checkTimesConsistency() {
        if ((translations != null && times.length != translations.length)
                || (rotations != null && times.length != rotations.length)
                || (scales != null && times.length != scales.length)) {
            throw new AssetLoadException("Inconsistent animation sampling ");
        }
    }

    @Deprecated
    public void checkTimesConsistantcy() {
        checkTimesConsistency();
    }

    private void populateTransform(Type type, int index, List<KeyFrame> keyFrames, KeyFrame currentKeyFrame, TransformIndices transformIndices) {
        Object transform = getTransform(type, currentKeyFrame);
        if (transform != null) {
            getArray(type)[index] = transform;
            transformIndices.last = index;
        } else {
            transformIndices.next = findNext(keyFrames, type, index);
            if (transformIndices.next == -1) {
                //no next let's use prev value.
                if (transformIndices.last == -1) {
                    //last Transform Index = -1 it means there are no transforms. nothing more to do
                    return;
                }
                KeyFrame lastKeyFrame = keyFrames.get(transformIndices.last);
                getArray(type)[index] = getTransform(type, lastKeyFrame);
                return;
            }
            KeyFrame nextKeyFrame = keyFrames.get(transformIndices.next);
            if (transformIndices.last == -1) {
                //no previous transforms let's use the new one.
                translations[index] = nextKeyFrame.translation;
            } else {
                //interpolation between the previous transform and the next one.
                KeyFrame lastKeyFrame = keyFrames.get(transformIndices.last);
                float ratio = currentKeyFrame.time / (nextKeyFrame.time - lastKeyFrame.time);
                interpolate(type, ratio, lastKeyFrame, nextKeyFrame, index);
            }

        }
    }

    private int findNext(List<KeyFrame> keyFrames, Type type, int fromIndex) {
        for (int i = fromIndex + 1; i < keyFrames.size(); i++) {
            KeyFrame kf = keyFrames.get(i);
            switch (type) {
                case Translation:
                    if (kf.translation != null) {
                        return i;
                    }
                    break;
                case Rotation:
                    if (kf.rotation != null) {
                        return i;
                    }
                    break;
                case Scale:
                    if (kf.scale != null) {
                        return i;
                    }
                    break;
            }
        }
        return -1;
    }

    public int getNbKeyFrames() {
        if (times != null) {
            return times.length;
        }
        return 0;
    }

    private void interpolate(Type type, float ratio, KeyFrame lastKeyFrame, KeyFrame nextKeyFrame, int currentIndex) {
        //TODO here we should interpolate differently according to the interpolation given in the gltf file.
        switch (type) {
            case Translation:
                translations[currentIndex] = FastMath.interpolateLinear(ratio, lastKeyFrame.translation, nextKeyFrame.translation);
                break;
            case Rotation:
                Quaternion rot = new Quaternion().set(lastKeyFrame.rotation);
                rot.nlerp(nextKeyFrame.rotation, ratio);
                rotations[currentIndex] = rot;
                break;
            case Scale:
                scales[currentIndex] = FastMath.interpolateLinear(ratio, lastKeyFrame.scale, nextKeyFrame.scale);
                break;
        }
    }

    private Object[] getArray(Type type) {
        switch (type) {
            case Translation:
                return translations;
            case Rotation:
                return rotations;
            case Scale:
                return scales;
            default:
                return translations;
        }
    }

    private Object getTransform(Type type, KeyFrame kf) {
        switch (type) {
            case Translation:
                return kf.translation;
            case Rotation:
                return kf.rotation;
            case Scale:
                return kf.scale;
            default:
                return kf.translation;
        }
    }

    private void ensureArraysLength() {
        if (translations != null && translations.length != times.length) {
            translations = new Vector3f[times.length];
        }
        if (rotations != null && rotations.length != times.length) {
            rotations = new Quaternion[times.length];
        }
        if (scales != null && scales.length != times.length) {
            scales = new Vector3f[times.length];
        }
    }


    //JME assumes there are translation and rotation track every time, so we create them with identity transforms if they don't exist
    //TODO change this behavior in BoneTrack.
    public void ensureTranslationRotations(Transform localTransforms) {
        if (translations == null) {
            translations = new Vector3f[times.length];
            for (int i = 0; i < translations.length; i++) {
                translations[i] = localTransforms.getTranslation();
            }
        }
        if (rotations == null) {
            rotations = new Quaternion[times.length];
            for (int i = 0; i < rotations.length; i++) {
                rotations[i] = localTransforms.getRotation();
            }
        }
        if (scales == null) {
            scales = new Vector3f[times.length];
            for (int i = 0; i < scales.length; i++) {
                scales[i] = localTransforms.getScale();
            }
        }
    }

    private void setKeyFrameTransforms(Type type, KeyFrame keyFrame, float[] transformTimes) {
        int index = 0;
        while (Float.floatToIntBits(transformTimes[index]) != Float.floatToIntBits(keyFrame.time)) {
            index++;
        }
        switch (type) {
            case Translation:
                keyFrame.translation = translations[index];
                break;
            case Rotation:
                keyFrame.rotation = rotations[index];
                break;
            case Scale:
                keyFrame.scale = scales[index];
                break;
        }
    }

    private boolean equalTimes(List<TimeData> timeData) {
        if (timeData.size() == 1) {
            return true;
        }
        float[] times0 = timeData.get(0).times;
        for (int i = 1; i < timeData.size(); i++) {
            float[] timesI = timeData.get(i).times;
            if (!Arrays.equals(times0, timesI)) {
                return false;
            }
        }
        return true;
    }

    static class TimeData {

        float[] times;
        Type type;

        public TimeData(float[] times, Type type) {
            this.times = times;
            this.type = type;
        }
    }

    private class TransformIndices {
        int last = -1;
        int next = -1;
    }

    private class KeyFrame {
        float time;
        Vector3f translation;
        Quaternion rotation;
        Vector3f scale;
    }

}
