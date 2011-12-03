package com.jme3.effect.influencers;

import com.jme3.effect.Particle;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * This emitter influences the particles so that they move all in the same direction.
 * The direction may vary a little if the velocity variation is non zero.
 * This influencer is default for the particle emitter.
 * @author Marcin Roguski (Kaelthas)
 */
public class DefaultParticleInfluencer implements ParticleInfluencer {

    /** Temporary variable used to help with calculations. */
    protected transient Vector3f temp = new Vector3f();
    /** The initial velocity of the particles. */
    protected Vector3f startVelocity = new Vector3f();
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
    	particle.velocity.set(startVelocity);
        temp.set(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
        temp.multLocal(2f);
        temp.subtractLocal(1f, 1f, 1f);
        temp.multLocal(startVelocity.length());
        particle.velocity.interpolate(temp, velocityVariation);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(startVelocity, "startVelocity", Vector3f.ZERO);
        oc.write(velocityVariation, "variation", 0.2f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        startVelocity = (Vector3f) ic.readSavable("startVelocity", Vector3f.ZERO.clone());
        velocityVariation = ic.readFloat("variation", 0.2f);
    }

    @Override
    public ParticleInfluencer clone() {
        try {
            DefaultParticleInfluencer clone = (DefaultParticleInfluencer) super.clone();
            clone.startVelocity = startVelocity.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public void setInitialVelocity(Vector3f initialVelocity) {
        this.startVelocity.set(initialVelocity);
    }

    @Override
    public Vector3f getInitialVelocity() {
        return startVelocity;
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
