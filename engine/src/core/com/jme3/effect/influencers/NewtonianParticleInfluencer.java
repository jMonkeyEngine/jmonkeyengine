package com.jme3.effect.influencers;

import com.jme3.effect.Particle;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
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
        result.startVelocity = startVelocity;
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
