/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.effect.influencers;

import com.jme3.effect.Particle;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * This emitter influences the particles so that they move all in the same direction.
 * The direction may vary a little if the velocity variation is non zero.
 * This influencer is default for the particle emitter.
 * @author Marcin Roguski (Kaelthas)
 */
public class DefaultParticleInfluencer implements ParticleInfluencer {

    //Version #1 : changed startVelocity to initialvelocity for consistency with accessors
    //and also changed it in serialization
    public static final int SAVABLE_VERSION = 1;
    /** Temporary variable used to help with calculations. */
    protected transient Vector3f temp = new Vector3f();
    /** The initial velocity of the particles. */
    protected Vector3f initialVelocity = new Vector3f();
    /** The velocity's variation of the particles. */
    protected float velocityVariation = 0.2f;

    @Override
    public void influenceParticle(Particle particle, EmitterShape emitterShape) {
        emitterShape.getRandomPoint(particle.position);
        this.applyVelocityVariation(particle);
    }

    /**
     * This method applies the variation to the particle with already set velocity.
     * @param particle
     *        the particle to be affected
     */
    protected void applyVelocityVariation(Particle particle) {
    	particle.velocity.set(initialVelocity);
        temp.set(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
        temp.multLocal(2f);
        temp.subtractLocal(1f, 1f, 1f);
        temp.multLocal(initialVelocity.length());
        particle.velocity.interpolateLocal(temp, velocityVariation);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(initialVelocity, "initialVelocity", Vector3f.ZERO);
        oc.write(velocityVariation, "variation", 0.2f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        // NOTE: In previous versions of jME3, initialVelocity was called startVelocity
        if (ic.getSavableVersion(DefaultParticleInfluencer.class) == 0){
            initialVelocity = (Vector3f) ic.readSavable("startVelocity", Vector3f.ZERO.clone());
        }else{
            initialVelocity = (Vector3f) ic.readSavable("initialVelocity", Vector3f.ZERO.clone());
        }
        velocityVariation = ic.readFloat("variation", 0.2f);
    }

    @Override
    public ParticleInfluencer clone() {
        try {
            DefaultParticleInfluencer clone = (DefaultParticleInfluencer) super.clone();
            clone.initialVelocity = initialVelocity.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        this.initialVelocity = cloner.clone(initialVelocity);

        // Change in behavior: I'm cloning 'for real' the 'temp' field because
        // otherwise it will be shared across all clones.  Note: if this is
        // ok because of how its used then it might as well be static and let
        // everything share it.
        // Note 2: transient fields _are_ cloned just like anything else so
        // thinking it wouldn't get cloned is also not right.
        // -pspeed
        this.temp = cloner.clone(temp);
    }

    @Override
    public void setInitialVelocity(Vector3f initialVelocity) {
        this.initialVelocity.set(initialVelocity);
    }

    @Override
    public Vector3f getInitialVelocity() {
        return initialVelocity;
    }

    @Override
    public void setVelocityVariation(float variation) {
        this.velocityVariation = variation;
    }

    @Override
    public float getVelocityVariation() {
        return velocityVariation;
    }
}
