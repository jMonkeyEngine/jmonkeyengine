/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
 * Audio environment, for reverb effects.
 * @author Kirill
 */
public class Environment {

    private float airAbsorbGainHf   = 0.99426f;
    private float roomRolloffFactor = 0;

    private float decayTime         = 1.49f;
    private float decayHFRatio      = 0.54f;

    private float density           = 1.0f;
    private float diffusion         = 0.3f;

    private float gain              = 0.316f;
    private float gainHf            = 0.022f;

    private float lateReverbDelay   = 0.088f;
    private float lateReverbGain    = 0.768f;

    private float reflectDelay      = 0.162f;
    private float reflectGain       = 0.052f;

    private boolean decayHfLimit    = true;

    public static final Environment Garage, Dungeon, Cavern, AcousticLab, Closet;

    static {
        Garage = new Environment(1, 1, 1, 1, .9f, .5f, .751f, .0039f, .661f, .0137f);
        Dungeon = new Environment(.75f, 1, 1, .75f, 1.6f, 1, 0.95f, 0.0026f, 0.93f, 0.0103f);
        Cavern = new Environment(.5f, 1, 1, .5f, 2.25f, 1, .908f, .0103f, .93f, .041f);
        AcousticLab = new Environment(.5f, 1, 1, 1, .28f, 1, .87f, .002f, .81f, .008f);
        Closet = new Environment(1, 1, 1, 1, .15f, 1, .6f, .0025f, .5f, .0006f);
    }

    private static final float eaxDbToAmp(float eaxDb){
        float dB = eaxDb / 2000f;
        return FastMath.pow(10f, dB);
    }

    public Environment(){
    }

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

    public Environment(float density, float diffusion, float gain, float gainHf,
                       float decayTime, float decayHf, float reflGain,
                       float reflDelay, float lateGain, float lateDelay){
        this.decayTime = decayTime;
        this.decayHFRatio = decayHf;
        this.density = density;
        this.diffusion = diffusion;
        this.gain = gain;
        this.gainHf = gainHf;
        this.lateReverbDelay = lateDelay;
        this.lateReverbGain = lateGain;
        this.reflectDelay = reflDelay;
        this.reflectGain = reflGain;
    }

    public Environment(float[] e){
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
}
