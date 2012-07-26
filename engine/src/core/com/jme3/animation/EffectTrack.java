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
package com.jme3.animation;

import com.jme3.effect.ParticleEmitter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * EffectTrack is a track to add to an existing animation, to emmit particles during animations
 * for example : exhausts, dust raised by foot steps, shock waves, lightnings etc...
 * 
 * usage is 
 * <pre>
 * AnimControl control model.getControl(AnimControl.class);
 * EffectTrack track = new EffectTrack(existingEmmitter, control.getAnim("TheAnim").getLength());
 * control.getAnim("TheAnim").addTrack(track);
 * </pre>
 * 
 * if the emitter has emmits 0 particles per seconds emmitAllPArticles will be called on it at time 0 + startOffset.
 * if it he it has more it will start emmit normally at time 0 + startOffset.
 * 
 *
 * @author Nehon
 */
public class EffectTrack implements Track {

    private ParticleEmitter emitter;
    private float startOffset = 0;
    private float particlesPerSeconds = 0;
    private float length = 0;
    private boolean emitted = false;
    private boolean initialized = false;

    //Anim listener that stops the Emmitter when the animation is finished or changed.
    private class OnEndListener implements AnimEventListener {

        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
            stop();
        }

        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
            stop();
        }
    }
    

    /**
     * default constructor only for serialization
     */
    public EffectTrack() {
    }

    
    /**
     * Creates and EffectTrack
     * @param emitter the emmitter of the track
     * @param length the length of the track (usually the length of the animation you want to add the track to)
     */
    public EffectTrack(ParticleEmitter emitter, float length) {
        this.emitter = emitter;
        //saving particles per second value
        this.particlesPerSeconds = emitter.getParticlesPerSec();
        //setting the emmitter to not emmit.
        this.emitter.setParticlesPerSec(0);
        this.length = length;

    }

    /**
     * Creates and EffectTrack
     * @param emitter the emmitter of the track
     * @param length the length of the track (usually the length of the animation you want to add the track to)
     * @param startOffset the time in second when the emitter will be triggerd after the animation starts (default is 0)
     */
    public EffectTrack(ParticleEmitter emitter, float length, float startOffset) {
        this(emitter, length);
        this.startOffset = startOffset;
    }

    /**
     * Internal use only
     * @see Track#setTime(float, float, com.jme3.animation.AnimControl, com.jme3.animation.AnimChannel, com.jme3.util.TempVars) 
     */
    public void setTime(float time, float weight, AnimControl control, AnimChannel channel, TempVars vars) {

        //first time adding the Animation listener to stop the track at the end of the animation
        if (!initialized) {
            control.addListener(new OnEndListener());
            initialized = true;
        }
        //checking fo time to trigger the effect
        if (!emitted && time >= startOffset) {
            emitted = true;
            //if the emitter has 0 particles per seconds emmit all particles in one shot
            if (particlesPerSeconds == 0) {
                emitter.emitAllParticles();
            } else {
                //else reset its former particlePerSec value to let it emmit.
                emitter.setParticlesPerSec(particlesPerSeconds);
            }
        }
    }
    
    //stops the emmiter to emit.
    private void stop() {
        emitter.setParticlesPerSec(0);
        emitted = false;
    }

    /**
     * Retruns the length of the track
     * @return length of the track
     */
    public float getLength() {
        return length;
    }

    /**
     * Clone this track
     * @return 
     */
    @Override
    public Track clone() {
        return new EffectTrack(emitter, length, startOffset);

    }    

    /**
     * Internal use only serialization
     * @param ex exporter
     * @throws IOException exception
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(emitter, "emitter", null);
        out.write(length, "length", 0);

        out.write(startOffset, "startOffset", 0);
    }

    /**
     * Internal use only serialization
     * @param im importer
     * @throws IOException Exception
     */   
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        emitter = (ParticleEmitter) in.readSavable("emitter", null);
        this.particlesPerSeconds = emitter.getParticlesPerSec();
        emitter.setParticlesPerSec(0);
        length = in.readFloat("length", length);
        startOffset = in.readFloat("startOffset", 0);
    }
}
