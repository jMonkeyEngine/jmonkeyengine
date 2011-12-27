/*
 * Copyright (c) 2009-2011 jMonkeyEngine
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
package com.jme3.animation;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

/**
 * A convenience class to easily setup a spatial keyframed animation
 * you can add some keyFrames for a given time or a given keyFrameIndex, for translation rotation and scale.
 * The animationHelper will then generate an appropriate SpatialAnimation by interpolating values between the keyFrames.
 * <br><br>
 * Usage is : <br>
 * - Create the AnimationHelper<br>
 * - add some keyFrames<br>
 * - call the buildAnimation() method that will retruna new Animation<br>
 * - add the generated Animation to any existing AnimationControl<br>
 * <br><br>
 * Note that the first keyFrame (index 0) is defaulted with the identy transforms.
 * If you want to change that you have to replace this keyFrame with any transform you want.
 * 
 * @author Nehon
 */
public class AnimationFactory {

    /**
     * step for splitting rotation that have a n ange above PI/2
     */
    private final static float EULER_STEP = FastMath.QUARTER_PI * 3;

    /**
     * enum to determine the type of interpolation
     */
    private enum Type {

        Translation, Rotation, Scale;
    }

    /**
     * Inner Rotation type class to kep track on a rotation Euler angle
     */
    protected class Rotation {

        /**
         * The rotation Quaternion
         */
        Quaternion rotation = new Quaternion();
        /**
         * This rotation expressed in Euler angles
         */
        Vector3f eulerAngles = new Vector3f();
        /**
         * the index of the parent key frame is this keyFrame is a splitted rotation
         */
        int masterKeyFrame = -1;

        public Rotation() {
            rotation.loadIdentity();
        }

        void set(Quaternion rot) {
            rotation.set(rot);
            float[] a = new float[3];
            rotation.toAngles(a);
            eulerAngles.set(a[0], a[1], a[2]);
        }

        void set(float x, float y, float z) {
            float[] a = {x, y, z};
            rotation.fromAngles(a);
            eulerAngles.set(x, y, z);
        }
    }
    /**
     * Name of the animation
     */
    protected String name;
    /**
     * frames per seconds
     */
    protected int fps;
    /**
     * Animation duration in seconds
     */
    protected float duration;
    /**
     * total number of frames
     */
    protected int totalFrames;
    /**
     * time per frame
     */
    protected float tpf;
    /**
     * Time array for this animation
     */
    protected float[] times;
    /**
     * Translation array for this animation
     */
    protected Vector3f[] translations;
    /**
     * rotation array for this animation
     */
    protected Quaternion[] rotations;
    /**
     * scales array for this animation
     */
    protected Vector3f[] scales;
    /**
     * The map of keyFrames to compute the animation. The key is the index of the frame
     */
    protected Vector3f[] keyFramesTranslation;
    protected Vector3f[] keyFramesScale;
    protected Rotation[] keyFramesRotation;

    /**
     * Creates and AnimationHelper
     * @param duration the desired duration for the resulting animation
     * @param name the name of the resulting animation
     */
    public AnimationFactory(float duration, String name) {
        this(duration, name, 30);
    }

    /**
     * Creates and AnimationHelper
     * @param duration the desired duration for the resulting animation
     * @param name the name of the resulting animation
     * @param fps the number of frames per second for this animation (default is 30)
     */
    public AnimationFactory(float duration, String name, int fps) {
        this.name = name;
        this.duration = duration;
        this.fps = fps;
        totalFrames = (int) (fps * duration) + 1;
        tpf = 1 / (float) fps;
        times = new float[totalFrames];
        translations = new Vector3f[totalFrames];
        rotations = new Quaternion[totalFrames];
        scales = new Vector3f[totalFrames];
        keyFramesTranslation = new Vector3f[totalFrames];
        keyFramesTranslation[0] = new Vector3f();
        keyFramesScale = new Vector3f[totalFrames];
        keyFramesScale[0] = new Vector3f(1, 1, 1);
        keyFramesRotation = new Rotation[totalFrames];
        keyFramesRotation[0] = new Rotation();

    }

    /**
     * Adds a key frame for the given Transform at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param transform the transforms to use for this keyFrame
     */
    public void addTimeTransform(float time, Transform transform) {
        addKeyFrameTransform((int) (time / tpf), transform);
    }

    /**
     * Adds a key frame for the given Transform at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param transform the transforms to use for this keyFrame
     */
    public void addKeyFrameTransform(int keyFrameIndex, Transform transform) {
        addKeyFrameTranslation(keyFrameIndex, transform.getTranslation());
        addKeyFrameScale(keyFrameIndex, transform.getScale());
        addKeyFrameRotation(keyFrameIndex, transform.getRotation());
    }

    /**
     * Adds a key frame for the given translation at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param translation the translation to use for this keyFrame
     */
    public void addTimeTranslation(float time, Vector3f translation) {
        addKeyFrameTranslation((int) (time / tpf), translation);
    }

    /**
     * Adds a key frame for the given translation at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param translation the translation to use for this keyFrame
     */
    public void addKeyFrameTranslation(int keyFrameIndex, Vector3f translation) {
        Vector3f t = getTranslationForFrame(keyFrameIndex);
        t.set(translation);
    }

    /**
     * Adds a key frame for the given rotation at the given time<br>
     * This can't be used if the interpolated angle is higher than PI (180°)<br>
     * Use {@link addTimeRotationAngles(float time, float x, float y, float z)}  instead that uses Euler angles rotations.<br>     * 
     * @param time the time at which the keyFrame must be inserted
     * @param rotation the rotation Quaternion to use for this keyFrame
     * @see #addTimeRotationAngles(float time, float x, float y, float z) 
     */
    public void addTimeRotation(float time, Quaternion rotation) {
        addKeyFrameRotation((int) (time / tpf), rotation);
    }

    /**
     * Adds a key frame for the given rotation at the given keyFrame index<br>
     * This can't be used if the interpolated angle is higher than PI (180°)<br>
     * Use {@link addKeyFrameRotationAngles(int keyFrameIndex, float x, float y, float z)} instead that uses Euler angles rotations.
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param rotation the rotation Quaternion to use for this keyFrame
     * @see #addKeyFrameRotationAngles(int keyFrameIndex, float x, float y, float z) 
     */
    public void addKeyFrameRotation(int keyFrameIndex, Quaternion rotation) {
        Rotation r = getRotationForFrame(keyFrameIndex);
        r.set(rotation);
    }

    /**
     * Adds a key frame for the given rotation at the given time.<br>
     * Rotation is expressed by Euler angles values in radians.<br>
     * Note that the generated rotation will be stored as a quaternion and interpolated using a spherical linear interpolation (slerp)<br>
     * Hence, this method may create intermediate keyFrames if the interpolation angle is higher than PI to ensure continuity in animation<br>
     * 
     * @param time the time at which the keyFrame must be inserted
     * @param x the rotation around the x axis (aka yaw) in radians
     * @param y the rotation around the y axis (aka roll) in radians
     * @param z the rotation around the z axis (aka pitch) in radians
     */
    public void addTimeRotationAngles(float time, float x, float y, float z) {
        addKeyFrameRotationAngles((int) (time / tpf), x, y, z);
    }

    /**
     * Adds a key frame for the given rotation at the given key frame index.<br>
     * Rotation is expressed by Euler angles values in radians.<br>
     * Note that the generated rotation will be stored as a quaternion and interpolated using a spherical linear interpolation (slerp)<br>
     * Hence, this method may create intermediate keyFrames if the interpolation angle is higher than PI to ensure continuity in animation<br>
     * 
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param x the rotation around the x axis (aka yaw) in radians
     * @param y the rotation around the y axis (aka roll) in radians
     * @param z the rotation around the z axis (aka pitch) in radians
     */
    public void addKeyFrameRotationAngles(int keyFrameIndex, float x, float y, float z) {
        Rotation r = getRotationForFrame(keyFrameIndex);
        r.set(x, y, z);

        // if the delta of euler angles is higher than PI, we create intermediate keyframes
        // since we are using quaternions and slerp for rotation interpolation, we cannot interpolate over an angle higher than PI
        int prev = getPreviousKeyFrame(keyFrameIndex, keyFramesRotation);
        //previous rotation keyframe
        Rotation prevRot = keyFramesRotation[prev];
        //the maximum delta angle (x,y or z)
        float delta = Math.max(Math.abs(x - prevRot.eulerAngles.x), Math.abs(y - prevRot.eulerAngles.y));
        delta = Math.max(delta, Math.abs(z - prevRot.eulerAngles.z));
        //if delta > PI we have to create intermediates key frames
        if (delta >= FastMath.PI) {
            //frames delta
            int dF = keyFrameIndex - prev;
            //angle per frame for x,y ,z
            float dXAngle = (x - prevRot.eulerAngles.x) / (float) dF;
            float dYAngle = (y - prevRot.eulerAngles.y) / (float) dF;
            float dZAngle = (z - prevRot.eulerAngles.z) / (float) dF;

            // the keyFrame step
            int keyStep = (int) (((float) (dF)) / delta * (float) EULER_STEP);
            // the current keyFrame
            int cursor = prev + keyStep;
            while (cursor < keyFrameIndex) {
                //for each step we create a new rotation by interpolating the angles
                Rotation dr = getRotationForFrame(cursor);
                dr.masterKeyFrame = keyFrameIndex;
                dr.set(prevRot.eulerAngles.x + cursor * dXAngle, prevRot.eulerAngles.y + cursor * dYAngle, prevRot.eulerAngles.z + cursor * dZAngle);
                cursor += keyStep;
            }

        }

    }

    /**
     * Adds a key frame for the given scale at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param scale the scale to use for this keyFrame
     */
    public void addTimeScale(float time, Vector3f scale) {
        addKeyFrameScale((int) (time / tpf), scale);
    }

    /**
     * Adds a key frame for the given scale at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param scale the scale to use for this keyFrame
     */
    public void addKeyFrameScale(int keyFrameIndex, Vector3f scale) {
        Vector3f s = getScaleForFrame(keyFrameIndex);
        s.set(scale);
    }

    /**
     * returns the translation for a given frame index
     * creates the translation if it doesn't exists
     * @param keyFrameIndex index
     * @return the translation
     */
    private Vector3f getTranslationForFrame(int keyFrameIndex) {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw new ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and " + totalFrames + " (received " + keyFrameIndex + ")");
        }
        Vector3f v = keyFramesTranslation[keyFrameIndex];
        if (v == null) {
            v = new Vector3f();
            keyFramesTranslation[keyFrameIndex] = v;
        }
        return v;
    }

    /**
     * returns the scale for a given frame index
     * creates the scale if it doesn't exists
     * @param keyFrameIndex index
     * @return the scale
     */
    private Vector3f getScaleForFrame(int keyFrameIndex) {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw new ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and " + totalFrames + " (received " + keyFrameIndex + ")");
        }
        Vector3f v = keyFramesScale[keyFrameIndex];
        if (v == null) {
            v = new Vector3f();
            keyFramesScale[keyFrameIndex] = v;
        }
        return v;
    }

    /**
     * returns the rotation for a given frame index
     * creates the rotation if it doesn't exists
     * @param keyFrameIndex index
     * @return the rotation
     */
    private Rotation getRotationForFrame(int keyFrameIndex) {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw new ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and " + totalFrames + " (received " + keyFrameIndex + ")");
        }
        Rotation v = keyFramesRotation[keyFrameIndex];
        if (v == null) {
            v = new Rotation();
            keyFramesRotation[keyFrameIndex] = v;
        }
        return v;
    }

    /**
     * Creates an Animation based on the keyFrames previously added to the helper.
     * @return the generated animation 
     */
    public Animation buildAnimation() {
        interpolateTime();
        interpolate(keyFramesTranslation, Type.Translation);
        interpolate(keyFramesRotation, Type.Rotation);
        interpolate(keyFramesScale, Type.Scale);

        SpatialTrack spatialTrack = new SpatialTrack(times, translations, rotations, scales);

        //creating the animation
        Animation spatialAnimation = new Animation(name, duration);
        spatialAnimation.setTracks(new SpatialTrack[]{spatialTrack});

        return spatialAnimation;
    }

    /**
     * interpolates time values
     */
    private void interpolateTime() {
        for (int i = 0; i < totalFrames; i++) {
            times[i] = i * tpf;
        }
    }

    /**
     * Interpolates over the key frames for the given keyFrame array and the given type of transform
     * @param keyFrames the keyFrames array
     * @param type the type of transforms
     */
    private void interpolate(Object[] keyFrames, Type type) {
        int i = 0;
        while (i < totalFrames) {
            //fetching the next keyFrame index transform in the array
            int key = getNextKeyFrame(i, keyFrames);
            if (key != -1) {
                //computing the frame span to interpolate over
                int span = key - i;
                //interating over the frames
                for (int j = i; j <= key; j++) {
                    // computing interpolation value
                    float val = (float) (j - i) / (float) span;
                    //interpolationg depending on the transform type
                    switch (type) {
                        case Translation:
                            translations[j] = FastMath.interpolateLinear(val, (Vector3f) keyFrames[i], (Vector3f) keyFrames[key]);
                            break;
                        case Rotation:
                            Quaternion rot = new Quaternion();
                            rotations[j] = rot.slerp(((Rotation) keyFrames[i]).rotation, ((Rotation) keyFrames[key]).rotation, val);
                            break;
                        case Scale:
                            scales[j] = FastMath.interpolateLinear(val, (Vector3f) keyFrames[i], (Vector3f) keyFrames[key]);
                            break;
                    }
                }
                //jumping to the next keyFrame
                i = key;
            } else {
                //No more key frame, filling the array witht he last transform computed.
                for (int j = i; j < totalFrames; j++) {

                    switch (type) {
                        case Translation:
                            translations[j] = ((Vector3f) keyFrames[i]).clone();
                            break;
                        case Rotation:
                            rotations[j] = ((Quaternion) ((Rotation) keyFrames[i]).rotation).clone();
                            break;
                        case Scale:
                            scales[j] = ((Vector3f) keyFrames[i]).clone();
                            break;
                    }
                }
                //we're done
                i = totalFrames;
            }
        }
    }

    /**
     * Get the index of the next keyFrame that as a transform
     * @param index the start index
     * @param keyFrames the keyFrames array
     * @return the index of the next keyFrame
     */
    private int getNextKeyFrame(int index, Object[] keyFrames) {
        for (int i = index + 1; i < totalFrames; i++) {
            if (keyFrames[i] != null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the index of the previous keyFrame that as a transform
     * @param index the start index
     * @param keyFrames the keyFrames array
     * @return the index of the previous keyFrame
     */
    private int getPreviousKeyFrame(int index, Object[] keyFrames) {
        for (int i = index - 1; i >= 0; i--) {
            if (keyFrames[i] != null) {
                return i;
            }
        }
        return -1;
    }
}
