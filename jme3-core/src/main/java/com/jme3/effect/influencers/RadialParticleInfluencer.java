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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;

/**
 * an influencer to make blasts expanding on the ground. can be used for various other things
 * @author Nehon
 */
public class RadialParticleInfluencer extends DefaultParticleInfluencer {

    private float radialVelocity = 0f;
    private Vector3f origin = new Vector3f(0, 0, 0);
    private boolean horizontal = false;

    /**
     * This method applies the variation to the particle with already set velocity.
     * @param particle
     *        the particle to be affected
     */
    @Override
    protected void applyVelocityVariation(Particle particle) {
        particle.velocity.set(initialVelocity);
        temp.set(particle.position).subtractLocal(origin).normalizeLocal().multLocal(radialVelocity);
        if (horizontal) {
            temp.y = 0;
        }
        particle.velocity.addLocal(temp);

        temp.set(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
        temp.multLocal(2f);
        temp.subtractLocal(1f, 1f, 1f);
        temp.multLocal(initialVelocity.length());
        particle.velocity.interpolateLocal(temp, velocityVariation);
    }

    /**
     * the origin used for computing the radial velocity direction
     * @return the origin
     */
    public Vector3f getOrigin() {
        return origin;
    }

    /**
     * the origin used for computing the radial velocity direction
     * @param origin
     */
    public void setOrigin(Vector3f origin) {
        this.origin = origin;
    }

    /**
     * the radial velocity
     * @return radialVelocity
     */
    public float getRadialVelocity() {
        return radialVelocity;
    }

    /**
     * the radial velocity
     * @param radialVelocity
     */
    public void setRadialVelocity(float radialVelocity) {
        this.radialVelocity = radialVelocity;
    }

    /**
     * nullify y component of particle velocity to make the effect expand only on x and z axis
     * @return
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * nullify y component of particle velocity to make the effect expand only on x and z axis
     * @param horizontal
     */
    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        super.cloneFields(cloner, original);

        // Change in behavior: the old origin was not cloned -pspeed
        this.origin = cloner.clone(origin);
    }


    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(radialVelocity, "radialVelocity", 0f);
        oc.write(origin, "origin", new Vector3f());
        oc.write(horizontal, "horizontal", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        radialVelocity = ic.readFloat("radialVelocity", 0f);
        origin = (Vector3f) ic.readSavable("origin", new Vector3f());
        horizontal = ic.readBoolean("horizontal", false);
    }
}
