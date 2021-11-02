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
package com.jme3.anim;

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A convenience class to smoothly animate a Spatial using translation,
 * rotation, and scaling.
 *
 * Add keyframes for translation, rotation, and scaling. Invoking
 * {@link #buildAnimation(com.jme3.anim.util.HasLocalTransform)} will then
 * generate an AnimClip that interpolates among the keyframes.
 *
 * By default, the first keyframe (index=0) has an identity Transform. You can
 * override this by replacing the first keyframe with different Transform.
 *
 * For a loop animation, make sure the final transform matches the starting one.
 * Because of a heuristic used by
 * {@link com.jme3.math.Quaternion#slerp(com.jme3.math.Quaternion, com.jme3.math.Quaternion, float)},
 * it's possible for
 * {@link #buildAnimation(com.jme3.anim.util.HasLocalTransform)} to negate the
 * final rotation. To prevent an unwanted rotation at the end of the loop, you
 * may need to add intermediate rotation keyframes.
 *
 * Inspired by Nehon's {@link com.jme3.animation.AnimationFactory}.
 */
public class AnimFactory {

    /**
     * clip duration (in seconds)
     */
    final private float duration;
    /**
     * frame/sample rate for the clip (in frames per second)
     */
    final private float fps;
    /**
     * rotations that define the clip
     */
    final private Map<Float, Quaternion> rotations = new TreeMap<>();
    /**
     * scales that define the clip
     */
    final private Map<Float, Vector3f> scales = new TreeMap<>();
    /**
     * translations that define the clip
     */
    final private Map<Float, Vector3f> translations = new TreeMap<>();
    /**
     * name for the resulting clip
     */
    final private String name;

    /**
     * Instantiate an AnimFactory with an identity transform at t=0.
     *
     * @param duration the duration for the clip (in seconds, &gt;0)
     * @param name the name for the resulting clip
     * @param fps the frame rate for the clip (in frames per second, &gt;0)
     */
    public AnimFactory(float duration, String name, float fps) {
        if (!(duration > 0f)) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (!(fps > 0f)) {
            throw new IllegalArgumentException("FPS must be positive");
        }

        this.name = name;
        this.duration = duration;
        this.fps = fps;
        /*
         * Add the initial Transform.
         */
        Transform transform = new Transform();
        translations.put(0f, transform.getTranslation());
        rotations.put(0f, transform.getRotation());
        scales.put(0f, transform.getScale());
    }

    /**
     * Add a keyframe for the specified rotation at the specified index.
     *
     * @param keyFrameIndex the keyframe in which full rotation should be
     * achieved (&ge;0)
     * @param rotation the local rotation to apply to the target (not null,
     * non-zero norm, unaffected)
     */
    public void addKeyFrameRotation(int keyFrameIndex, Quaternion rotation) {
        float animationTime = keyFrameIndex / fps;
        addTimeRotation(animationTime, rotation);
    }

    /**
     * Add a keyframe for the specified scaling at the specified index.
     *
     * @param keyFrameIndex the keyframe in which full scaling should be
     * achieved (&ge;0)
     * @param scale the local scaling to apply to the target (not null,
     * unaffected)
     */
    public void addKeyFrameScale(int keyFrameIndex, Vector3f scale) {
        float animationTime = keyFrameIndex / fps;
        addTimeScale(animationTime, scale);
    }

    /**
     * Add a keyframe for the specified Transform at the specified index.
     *
     * @param keyFrameIndex the keyframe in which the full Transform should be
     * achieved (&ge;0)
     * @param transform the local Transform to apply to the target (not null,
     * unaffected)
     */
    public void addKeyFrameTransform(int keyFrameIndex, Transform transform) {
        float time = keyFrameIndex / fps;
        addTimeTransform(time, transform);
    }

    /**
     * Add a keyframe for the specified translation at the specified index.
     *
     * @param keyFrameIndex the keyframe in which full translation should be
     * achieved (&ge;0)
     * @param offset the local translation to apply to the target (not null,
     * unaffected)
     */
    public void addKeyFrameTranslation(int keyFrameIndex, Vector3f offset) {
        float time = keyFrameIndex / fps;
        addTimeTranslation(time, offset);
    }

    /**
     * Add a keyframe for the specified rotation at the specified time.
     *
     * @param time the animation time when full rotation should be achieved
     * (&ge;0, &le;duration)
     * @param rotation the local rotation to apply to the target (not null,
     * non-zero norm, unaffected)
     */
    public void addTimeRotation(float time, Quaternion rotation) {
        if (!(time >= 0f && time <= duration)) {
            throw new IllegalArgumentException("animation time out of range");
        }
        float norm = rotation.norm();
        if (norm == 0f) {
            throw new IllegalArgumentException("rotation cannot have norm=0");
        }

        float normalizingFactor = 1f / FastMath.sqrt(norm);
        Quaternion normalized = rotation.mult(normalizingFactor);
        rotations.put(time, normalized);
    }

    /**
     * Add a keyframe for the specified rotation at the specified time, based on
     * Tait-Bryan angles. Note that this is NOT equivalent to
     * {@link com.jme3.animation.AnimationFactory#addTimeRotationAngles(float, float, float, float)}.
     *
     * @param time the animation time when full rotation should be achieved
     * (&ge;0, &le;duration)
     * @param xAngle the X angle (in radians)
     * @param yAngle the Y angle (in radians)
     * @param zAngle the Z angle (in radians)
     */
    public void addTimeRotation(float time, float xAngle, float yAngle,
            float zAngle) {
        if (!(time >= 0f && time <= duration)) {
            throw new IllegalArgumentException("animation time out of range");
        }

        Quaternion quat = new Quaternion().fromAngles(xAngle, yAngle, zAngle);
        rotations.put(time, quat);
    }

    /**
     * Add a keyframe for the specified scale at the specified time.
     *
     * @param time the animation time when full scaling should be achieved
     * (&ge;0, &le;duration)
     * @param scale the local scaling to apply to the target (not null,
     * unaffected)
     */
    public void addTimeScale(float time, Vector3f scale) {
        if (!(time >= 0f && time <= duration)) {
            throw new IllegalArgumentException("animation time out of range");
        }

        Vector3f clone = scale.clone();
        scales.put(time, clone);
    }

    /**
     * Add a keyframe for the specified Transform at the specified time.
     *
     * @param time the animation time when the full Transform should be achieved
     * (&ge;0, &le;duration)
     * @param transform the local Transform to apply to the target (not null,
     * unaffected)
     */
    public void addTimeTransform(float time, Transform transform) {
        if (!(time >= 0f && time <= duration)) {
            throw new IllegalArgumentException("animation time out of range");
        }

        Vector3f translation = transform.getTranslation(null);
        translations.put(time, translation);
        rotations.put(time, transform.getRotation(null));
        scales.put(time, transform.getScale(null));
    }

    /**
     * Add a keyframe for the specified translation at the specified time.
     *
     * @param time the animation time when the full translation should be
     * achieved (&ge;0, &le;duration)
     * @param offset the local translation to apply to the target (not null,
     * unaffected)
     */
    public void addTimeTranslation(float time, Vector3f offset) {
        if (!(time >= 0f && time <= duration)) {
            throw new IllegalArgumentException("animation time out of range");
        }

        Vector3f clone = offset.clone();
        translations.put(time, clone);
    }

    /**
     * Create an AnimClip based on the keyframes added to this factory.
     *
     * @param target the target for this clip (which is typically a Spatial)
     * @return a new clip
     */
    public AnimClip buildAnimation(HasLocalTransform target) {
        Set<Float> times = new TreeSet<>();
        for (int frameI = 0;; ++frameI) {
            float time = frameI / fps;
            if (time > duration) {
                break;
            }
            times.add(time);
        }
        times.addAll(rotations.keySet());
        times.addAll(scales.keySet());
        times.addAll(translations.keySet());

        int numFrames = times.size();
        float[] timeArray = new float[numFrames];
        Vector3f[] translateArray = new Vector3f[numFrames];
        Quaternion[] rotArray = new Quaternion[numFrames];
        Vector3f[] scaleArray = new Vector3f[numFrames];

        int iFrame = 0;
        for (float time : times) {
            timeArray[iFrame] = time;
            translateArray[iFrame] = interpolateTranslation(time);
            rotArray[iFrame] = interpolateRotation(time);
            scaleArray[iFrame] = interpolateScale(time);

            ++iFrame;
        }

        AnimTrack[] tracks = new AnimTrack[1];
        tracks[0] = new TransformTrack(target, timeArray, translateArray,
                rotArray, scaleArray);
        AnimClip result = new AnimClip(name);
        result.setTracks(tracks);

        return result;
    }

    /**
     * Interpolate successive rotation keyframes for the specified time.
     *
     * @param keyFrameTime the animation time (in seconds, &ge;0)
     * @return a new instance
     */
    private Quaternion interpolateRotation(float keyFrameTime) {
        assert keyFrameTime >= 0f && keyFrameTime <= duration;

        float prev = 0f;
        float next = duration;
        for (float key : rotations.keySet()) {
            if (key <= keyFrameTime && key > prev) {
                prev = key;
            }
            if (key >= keyFrameTime && key < next) {
                next = key;
            }
        }
        assert prev <= next;
        Quaternion prevRotation = rotations.get(prev);

        Quaternion result = new Quaternion();
        if (prev == next || !rotations.containsKey(next)) {
            result.set(prevRotation);

        } else { // interpolate
            float fraction = (keyFrameTime - prev) / (next - prev);
            assert fraction >= 0f && fraction <= 1f;
            Quaternion nextRotation = rotations.get(next);
            result.slerp(prevRotation, nextRotation, fraction);
            /*
             * XXX slerp() sometimes negates nextRotation,
             * but usually that's okay because nextRotation and its negative
             * both represent the same rotation.
             */
        }

        return result;
    }

    /**
     * Interpolate successive scale keyframes for the specified time.
     *
     * @param keyFrameTime the animation time (in seconds, &ge;0)
     * @return a new instance
     */
    private Vector3f interpolateScale(float keyFrameTime) {
        assert keyFrameTime >= 0f && keyFrameTime <= duration;

        float prev = 0f;
        float next = duration;
        for (float key : scales.keySet()) {
            if (key <= keyFrameTime && key > prev) {
                prev = key;
            }
            if (key >= keyFrameTime && key < next) {
                next = key;
            }
        }
        assert prev <= next;
        Vector3f prevScale = scales.get(prev);

        Vector3f result = new Vector3f();
        if (prev == next || !scales.containsKey(next)) {
            result.set(prevScale);

        } else { // interpolate
            float fraction = (keyFrameTime - prev) / (next - prev);
            assert fraction >= 0f && fraction <= 1f;
            Vector3f nextScale = scales.get(next);
            result.interpolateLocal(prevScale, nextScale, fraction);
        }

        return result;
    }

    /**
     * Interpolate successive translation keyframes for the specified time.
     *
     * @param keyFrameTime the animation time (in seconds, &ge;0)
     * @return a new instance
     */
    private Vector3f interpolateTranslation(float keyFrameTime) {
        float prev = 0f;
        float next = duration;
        for (float key : translations.keySet()) {
            if (key <= keyFrameTime && key > prev) {
                prev = key;
            }
            if (key >= keyFrameTime && key < next) {
                next = key;
            }
        }
        assert prev <= next;
        Vector3f prevTranslation = translations.get(prev);

        Vector3f result = new Vector3f();
        if (prev == next || !translations.containsKey(next)) {
            result.set(prevTranslation);

        } else { // interpolate
            float fraction = (keyFrameTime - prev) / (next - prev);
            assert fraction >= 0f && fraction <= 1f;
            Vector3f nextTranslation = translations.get(next);
            result.interpolateLocal(prevTranslation, nextTranslation, fraction);
        }

        return result;
    }
}
