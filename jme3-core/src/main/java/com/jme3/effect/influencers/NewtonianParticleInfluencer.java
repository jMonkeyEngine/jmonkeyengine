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
import com.jme3.math.Matrix3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * This influencer calculates initial velocity with the use of the emitter's shape.
 * @author Marcin Roguski (Kaelthas)
 */
public class NewtonianParticleInfluencer extends DefaultParticleInfluencer {

    /** Normal to emitter's shape factor. */
    protected float normalVelocity;
    /** Emitter's surface tangent factor. */
    protected float surfaceTangentFactor;
    /** Emitters tangent rotation factor. */
    protected float surfaceTangentRotation;

    /**
     * Constructor. Sets velocity variation to 0.0f.
     */
    public NewtonianParticleInfluencer() {
        this.velocityVariation = 0.0f;
    }

    @Override
    public void influenceParticle(Particle particle, EmitterShape emitterShape) {
        emitterShape.getRandomPointAndNormal(particle.position, particle.velocity);
        // influencing the particle's velocity
        if (surfaceTangentFactor == 0.0f) {
            particle.velocity.multLocal(normalVelocity);
        } else {
            // calculating surface tangent (velocity contains the 'normal' value)
            temp.set(particle.velocity.z * surfaceTangentFactor, particle.velocity.y * surfaceTangentFactor, -particle.velocity.x * surfaceTangentFactor);
            if (surfaceTangentRotation != 0.0f) {// rotating the tangent
                Matrix3f m = new Matrix3f();
                m.fromAngleNormalAxis(FastMath.PI * surfaceTangentRotation, particle.velocity);
                temp = m.multLocal(temp);
            }
            // applying normal factor (this must be done first)
            particle.velocity.multLocal(normalVelocity);
            // adding tangent vector
            particle.velocity.addLocal(temp);
        }
        if (velocityVariation != 0.0f) {
            this.applyVelocityVariation(particle);
        }
    }

    /**
     * This method returns the normal velocity factor.
     * @return the normal velocity factor
     */
    public float getNormalVelocity() {
        return normalVelocity;
    }

    /**
     * This method sets the normal velocity factor.
     * @param normalVelocity
     *        the normal velocity factor
     */
    public void setNormalVelocity(float normalVelocity) {
        this.normalVelocity = normalVelocity;
    }

    /**
     * This method sets the surface tangent factor.
     * @param surfaceTangentFactor
     *        the surface tangent factor
     */
    public void setSurfaceTangentFactor(float surfaceTangentFactor) {
        this.surfaceTangentFactor = surfaceTangentFactor;
    }

    /**
     * This method returns the surface tangent factor.
     * @return the surface tangent factor
     */
    public float getSurfaceTangentFactor() {
        return surfaceTangentFactor;
    }

    /**
     * This method sets the surface tangent rotation factor.
     * @param surfaceTangentRotation
     *        the surface tangent rotation factor
     */
    public void setSurfaceTangentRotation(float surfaceTangentRotation) {
        this.surfaceTangentRotation = surfaceTangentRotation;
    }

    /**
     * This method returns the surface tangent rotation factor.
     * @return the surface tangent rotation factor
     */
    public float getSurfaceTangentRotation() {
        return surfaceTangentRotation;
    }

    @Override
    protected void applyVelocityVariation(Particle particle) {
        temp.set(FastMath.nextRandomFloat() * velocityVariation, FastMath.nextRandomFloat() * velocityVariation, FastMath.nextRandomFloat() * velocityVariation);
        particle.velocity.addLocal(temp);
    }

    @Override
    public ParticleInfluencer clone() {
        NewtonianParticleInfluencer result = new NewtonianParticleInfluencer();
        result.normalVelocity = normalVelocity;
        result.initialVelocity = initialVelocity;
        result.velocityVariation = velocityVariation;
        result.surfaceTangentFactor = surfaceTangentFactor;
        result.surfaceTangentRotation = surfaceTangentRotation;
        return result;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(normalVelocity, "normalVelocity", 0.0f);
        oc.write(surfaceTangentFactor, "surfaceTangentFactor", 0.0f);
        oc.write(surfaceTangentRotation, "surfaceTangentRotation", 0.0f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        normalVelocity = ic.readFloat("normalVelocity", 0.0f);
        surfaceTangentFactor = ic.readFloat("surfaceTangentFactor", 0.0f);
        surfaceTangentRotation = ic.readFloat("surfaceTangentRotation", 0.0f);
    }
}
