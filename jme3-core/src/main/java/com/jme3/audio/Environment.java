/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.audio;

import com.jme3.math.FastMath;

/**
 * Represents an audio environment, primarily used to define reverb effects.
 * This class provides parameters that correspond to the properties controllable
 * through the OpenAL EFX (Environmental Effects Extension) library.
 * By adjusting these parameters, developers can simulate various acoustic spaces
 * like rooms, caves, and concert halls, adding depth and realism to the audio experience.
 *
 * @author Kirill
 */
public class Environment {

    /** High-frequency air absorption gain (0.0f to 1.0f). */
    private float airAbsorbGainHf = 0.99426f;
    /** Factor controlling room effect rolloff with distance. */
    private float roomRolloffFactor = 0;
    /** Overall decay time of the reverberation (in seconds). */
    private float decayTime = 1.49f;
    /** Ratio of high-frequency decay time to overall decay time (0.0f to 1.0f). */
    private float decayHFRatio = 0.54f;
    /** Density of the medium affecting reverb smoothness (0.0f to 1.0f). */
    private float density = 1.0f;
    /** Diffusion of reflections affecting echo distinctness (0.0f to 1.0f). */
    private float diffusion = 0.3f;
    /** Overall gain of the environment effect (linear scale). */
    private float gain = 0.316f;
    /** High-frequency gain of the environment effect (linear scale). */
    private float gainHf = 0.022f;
    /** Delay time for late reverberation relative to early reflections (in seconds). */
    private float lateReverbDelay = 0.088f;
    /** Gain of the late reverberation (linear scale). */
    private float lateReverbGain = 0.768f;
    /** Delay time for the initial reflections (in seconds). */
    private float reflectDelay = 0.162f;
    /** Gain of the initial reflections (linear scale). */
    private float reflectGain = 0.052f;
    /** Flag limiting high-frequency decay by the overall decay time. */
    private boolean decayHfLimit = true;

    public static final Environment Garage = new Environment(
            1, 1, 1, 1, .9f, .5f, .751f, .0039f, .661f, .0137f);
    public static final Environment Dungeon = new Environment(
            .75f, 1, 1, .75f, 1.6f, 1, 0.95f, 0.0026f, 0.93f, 0.0103f);
    public static final Environment Cavern = new Environment(
            .5f, 1, 1, .5f, 2.25f, 1, .908f, .0103f, .93f, .041f);
    public static final Environment AcousticLab = new Environment(
            .5f, 1, 1, 1, .28f, 1, .87f, .002f, .81f, .008f);
    public static final Environment Closet = new Environment(
            1, 1, 1, 1, .15f, 1, .6f, .0025f, .5f, .0006f);

    /**
     * Utility method to convert an EAX decibel value to an amplitude factor.
     * EAX often expresses gain and attenuation in decibels scaled by 1000.
     * This method performs the reverse of that conversion to obtain a linear
     * amplitude value suitable for OpenAL.
     *
     * @param eaxDb The EAX decibel value (scaled by 1000).
     * @return The corresponding amplitude factor.
     */
    private static float eaxDbToAmp(float eaxDb) {
        float dB = eaxDb / 2000f;
        return FastMath.pow(10f, dB);
    }

    /**
     * Constructs a new, default {@code Environment}. The default values are
     * typically chosen to represent a neutral or common acoustic space.
     */
    public Environment() {
    }

    /**
     * Creates a new {@code Environment} as a copy of the provided {@code Environment}.
     *
     * @param source The {@code Environment} to copy the settings from.
     */
    public Environment(Environment source) {
        this.airAbsorbGainHf = source.airAbsorbGainHf;
        this.roomRolloffFactor = source.roomRolloffFactor;
        this.decayTime = source.decayTime;
        this.decayHFRatio = source.decayHFRatio;
        this.density = source.density;
        this.diffusion = source.diffusion;
        this.gain = source.gain;
        this.gainHf = source.gainHf;
        this.lateReverbDelay = source.lateReverbDelay;
        this.lateReverbGain = source.lateReverbGain;
        this.reflectDelay = source.reflectDelay;
        this.reflectGain = source.reflectGain;
        this.decayHfLimit = source.decayHfLimit;
    }

    /**
     * Creates a new {@code Environment} with the specified parameters. These parameters
     * directly influence the properties of the reverb effect as managed by OpenAL EFX.
     *
     * @param density      The density of the medium.
     * @param diffusion    The diffusion of the reflections.
     * @param gain         Overall gain applied to the environment effect.
     * @param gainHf       High-frequency gain applied to the environment effect.
     * @param decayTime    The overall decay time of the reflected sound.
     * @param decayHf      Ratio of high-frequency decay time to the overall decay time.
     * @param reflectGain  Gain applied to the initial reflections.
     * @param reflectDelay Delay time for the initial reflections.
     * @param lateGain     Gain applied to the late reverberation.
     * @param lateDelay    Delay time for the late reverberation.
     */
    public Environment(float density, float diffusion, float gain, float gainHf,
                       float decayTime, float decayHf, float reflectGain, float reflectDelay,
                       float lateGain, float lateDelay) {
        this.decayTime = decayTime;
        this.decayHFRatio = decayHf;
        this.density = density;
        this.diffusion = diffusion;
        this.gain = gain;
        this.gainHf = gainHf;
        this.lateReverbDelay = lateDelay;
        this.lateReverbGain = lateGain;
        this.reflectDelay = reflectDelay;
        this.reflectGain = reflectGain;
    }

    /**
     * Creates a new {@code Environment} by interpreting an array of 28 float values
     * as an EAX preset. This constructor attempts to map the EAX preset values to
     * the corresponding OpenAL EFX parameters. Note that not all EAX parameters
     * have a direct equivalent in standard OpenAL EFX, so some values might be
     * approximated or ignored.
     *
     * @param e An array of 28 float values representing an EAX preset.
     * @throws IllegalArgumentException If the provided array does not have a length of 28.
     */
    public Environment(float[] e) {
        if (e.length != 28)
            throw new IllegalArgumentException("Not an EAX preset");

        // skip env id
        // e[0]
        // skip room size
        // e[1]

//        density = 0;
        diffusion = e[2];
        gain = eaxDbToAmp(e[3]); // convert
        gainHf = eaxDbToAmp(e[4]) / eaxDbToAmp(e[5]); // convert
        decayTime = e[6];
        decayHFRatio = e[7] / e[8];
        reflectGain = eaxDbToAmp(e[9]); // convert
        reflectDelay = e[10];

        // skip 3 pan values
        // e[11] e[12] e[13]

        lateReverbGain = eaxDbToAmp(e[14]); // convert
        lateReverbDelay = e[15];

        // skip 3 pan values
        // e[16] e[17] e[18]

        // skip echo time, echo damping, mod time, mod damping
        // e[19] e[20] e[21] e[22]

        airAbsorbGainHf = eaxDbToAmp(e[23]);

        // skip HF Reference and LF Reference
        // e[24] e[25]

        roomRolloffFactor = e[26];

        // skip flags
        // e[27]
    }

    public float getAirAbsorbGainHf() {
        return airAbsorbGainHf;
    }

    public void setAirAbsorbGainHf(float airAbsorbGainHf) {
        this.airAbsorbGainHf = airAbsorbGainHf;
    }

    public float getDecayHFRatio() {
        return decayHFRatio;
    }

    public void setDecayHFRatio(float decayHFRatio) {
        this.decayHFRatio = decayHFRatio;
    }

    public boolean isDecayHfLimit() {
        return decayHfLimit;
    }

    public void setDecayHfLimit(boolean decayHfLimit) {
        this.decayHfLimit = decayHfLimit;
    }

    public float getDecayTime() {
        return decayTime;
    }

    public void setDecayTime(float decayTime) {
        this.decayTime = decayTime;
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getDiffusion() {
        return diffusion;
    }

    public void setDiffusion(float diffusion) {
        this.diffusion = diffusion;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getGainHf() {
        return gainHf;
    }

    public void setGainHf(float gainHf) {
        this.gainHf = gainHf;
    }

    public float getLateReverbDelay() {
        return lateReverbDelay;
    }

    public void setLateReverbDelay(float lateReverbDelay) {
        this.lateReverbDelay = lateReverbDelay;
    }

    public float getLateReverbGain() {
        return lateReverbGain;
    }

    public void setLateReverbGain(float lateReverbGain) {
        this.lateReverbGain = lateReverbGain;
    }

    public float getReflectDelay() {
        return reflectDelay;
    }

    public void setReflectDelay(float reflectDelay) {
        this.reflectDelay = reflectDelay;
    }

    public float getReflectGain() {
        return reflectGain;
    }

    public void setReflectGain(float reflectGain) {
        this.reflectGain = reflectGain;
    }

    public float getRoomRolloffFactor() {
        return roomRolloffFactor;
    }

    public void setRoomRolloffFactor(float roomRolloffFactor) {
        this.roomRolloffFactor = roomRolloffFactor;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Environment))
            return false;

        if (obj == this)
            return true;

        Environment other = (Environment) obj;
        float epsilon = 1e-6f;

        float[] thisFloats = {
                this.airAbsorbGainHf,
                this.decayHFRatio,
                this.decayTime,
                this.density,
                this.diffusion,
                this.gain,
                this.gainHf,
                this.lateReverbDelay,
                this.lateReverbGain,
                this.reflectDelay,
                this.reflectGain,
                this.roomRolloffFactor
        };

        float[] otherFloats = {
                other.airAbsorbGainHf,
                other.decayHFRatio,
                other.decayTime,
                other.density,
                other.diffusion,
                other.gain,
                other.gainHf,
                other.lateReverbDelay,
                other.lateReverbGain,
                other.reflectDelay,
                other.reflectGain,
                other.roomRolloffFactor
        };

        for (int i = 0; i < thisFloats.length; i++) {
            if (Math.abs(thisFloats[i] - otherFloats[i]) >= epsilon) {
                return false;
            }
        }

        return this.decayHfLimit == other.decayHfLimit;
    }

    @Override
    public int hashCode() {
        int result = (airAbsorbGainHf != +0.0f ? Float.floatToIntBits(airAbsorbGainHf) : 0);
        result = 31 * result + (roomRolloffFactor != +0.0f ? Float.floatToIntBits(roomRolloffFactor) : 0);
        result = 31 * result + (decayTime != +0.0f ? Float.floatToIntBits(decayTime) : 0);
        result = 31 * result + (decayHFRatio != +0.0f ? Float.floatToIntBits(decayHFRatio) : 0);
        result = 31 * result + (density != +0.0f ? Float.floatToIntBits(density) : 0);
        result = 31 * result + (diffusion != +0.0f ? Float.floatToIntBits(diffusion) : 0);
        result = 31 * result + (gain != +0.0f ? Float.floatToIntBits(gain) : 0);
        result = 31 * result + (gainHf != +0.0f ? Float.floatToIntBits(gainHf) : 0);
        result = 31 * result + (lateReverbDelay != +0.0f ? Float.floatToIntBits(lateReverbDelay) : 0);
        result = 31 * result + (lateReverbGain != +0.0f ? Float.floatToIntBits(lateReverbGain) : 0);
        result = 31 * result + (reflectDelay != +0.0f ? Float.floatToIntBits(reflectDelay) : 0);
        result = 31 * result + (reflectGain != +0.0f ? Float.floatToIntBits(reflectGain) : 0);
        result = 31 * result + (decayHfLimit ? 1 : 0);
        return result;
    }
}
