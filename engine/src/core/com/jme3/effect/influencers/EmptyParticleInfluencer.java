package com.jme3.effect.influencers;

import com.jme3.effect.Particle;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * This influencer does not influence particle at all.
 * It makes particles not to move.
 * @author Marcin Roguski (Kaelthas)
 */
public class EmptyParticleInfluencer implements ParticleInfluencer {

    @Override
    public void write(JmeExporter ex) throws IOException {
    }

    @Override
    public void read(JmeImporter im) throws IOException {
    }

    @Override
    public void influenceParticle(Particle particle, EmitterShape emitterShape) {
    }

    @Override
    public void setInitialVelocity(Vector3f initialVelocity) {
    }

    @Override
    public Vector3f getInitialVelocity() {
        return null;
    }

    @Override
    public void setVelocityVariation(float variation) {
    }

    @Override
    public float getVelocityVariation() {
        return 0;
    }

    @Override
    public ParticleInfluencer clone() {
        try {
            return (ParticleInfluencer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
