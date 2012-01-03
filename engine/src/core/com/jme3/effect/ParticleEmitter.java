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
package com.jme3.effect;

import com.jme3.bounding.BoundingBox;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.influencers.DefaultParticleInfluencer;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * <code>ParticleEmitter</code> is a special kind of geometry which simulates
 * a particle system.
 * <p>
 * Particle emitters can be used to simulate various kinds of phenomena,
 * such as fire, smoke, explosions and much more.
 * <p>
 * Particle emitters have many properties which are used to control the 
 * simulation. The interpretation of these properties depends on the 
 * {@link ParticleInfluencer} that has been assigned to the emitter via
 * {@link ParticleEmitter#setParticleInfluencer(com.jme3.effect.influencers.ParticleInfluencer) }.
 * By default the implementation {@link DefaultParticleInfluencer} is used.
 * 
 * @author Kirill Vainer
 */
public class ParticleEmitter extends Geometry {

    private boolean enabled = true;
    private static final EmitterShape DEFAULT_SHAPE = new EmitterPointShape(Vector3f.ZERO);
    private static final ParticleInfluencer DEFAULT_INFLUENCER = new DefaultParticleInfluencer();
    private ParticleEmitterControl control;
    private EmitterShape shape = DEFAULT_SHAPE;
    private ParticleMesh particleMesh;
    private ParticleInfluencer particleInfluencer = DEFAULT_INFLUENCER;
    private ParticleMesh.Type meshType;
    private Particle[] particles;
    private int firstUnUsed;
    private int lastUsed;
//    private int next = 0;
//    private ArrayList<Integer> unusedIndices = new ArrayList<Integer>();
    private boolean randomAngle;
    private boolean selectRandomImage;
    private boolean facingVelocity;
    private float particlesPerSec = 20;
    private float timeDifference = 0;
    private float lowLife = 3f;
    private float highLife = 7f;
    private Vector3f gravity = new Vector3f(0.0f, 0.1f, 0.0f);
    private float rotateSpeed;
    private Vector3f faceNormal = new Vector3f(Vector3f.NAN);
    private int imagesX = 1;
    private int imagesY = 1;
   
    private ColorRGBA startColor = new ColorRGBA(0.4f, 0.4f, 0.4f, 0.5f);
    private ColorRGBA endColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 0.0f);
    private float startSize = 0.2f;
    private float endSize = 2f;
    private boolean worldSpace = true;
    //variable that helps with computations
    private transient Vector3f temp = new Vector3f();

    public static class ParticleEmitterControl implements Control {

        ParticleEmitter parentEmitter;

        public ParticleEmitterControl() {
        }

        public ParticleEmitterControl(ParticleEmitter parentEmitter) {
            this.parentEmitter = parentEmitter;
        }

        public Control cloneForSpatial(Spatial spatial) {
            return this; // WARNING: Sets wrong control on spatial. Will be
            // fixed automatically by ParticleEmitter.clone() method.
        }

        public void setSpatial(Spatial spatial) {
        }

        public void setEnabled(boolean enabled) {
            parentEmitter.setEnabled(enabled);
        }

        public boolean isEnabled() {
            return parentEmitter.isEnabled();
        }

        public void update(float tpf) {
            parentEmitter.updateFromControl(tpf);
        }

        public void render(RenderManager rm, ViewPort vp) {
            parentEmitter.renderFromControl(rm, vp);
        }

        public void write(JmeExporter ex) throws IOException {
        }

        public void read(JmeImporter im) throws IOException {
        }
    }

    @Override
    public ParticleEmitter clone() {
        return clone(true);
    }

    @Override
    public ParticleEmitter clone(boolean cloneMaterial) {
        ParticleEmitter clone = (ParticleEmitter) super.clone(cloneMaterial);
        clone.shape = shape.deepClone();

        // Reinitialize particle list
        clone.setNumParticles(particles.length);

        clone.faceNormal = faceNormal.clone();
        clone.startColor = startColor.clone();
        clone.endColor = endColor.clone();
        clone.particleInfluencer = particleInfluencer.clone();

        // remove wrong control
        clone.controls.remove(control);

        // put correct control
        clone.controls.add(new ParticleEmitterControl(clone));

        // Reinitialize particle mesh
        switch (meshType) {
            case Point:
                clone.particleMesh = new ParticlePointMesh();
                clone.setMesh(clone.particleMesh);
                break;
            case Triangle:
                clone.particleMesh = new ParticleTriMesh();
                clone.setMesh(clone.particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: " + meshType);
        }
        clone.particleMesh.initParticleData(clone, clone.particles.length);
        clone.particleMesh.setImagesXY(clone.imagesX, clone.imagesY);

        return clone;
    }

    public ParticleEmitter(String name, Type type, int numParticles) {
        super(name);

        // ignore world transform, unless user sets inLocalSpace
        this.setIgnoreTransform(true);

        // particles neither receive nor cast shadows
        this.setShadowMode(ShadowMode.Off);

        // particles are usually transparent
        this.setQueueBucket(Bucket.Transparent);

        meshType = type;

        // Must create clone of shape/influencer so that a reference to a static is 
        // not maintained
        shape = shape.deepClone();
        particleInfluencer = particleInfluencer.clone();

        control = new ParticleEmitterControl(this);
        controls.add(control);

        switch (meshType) {
            case Point:
                particleMesh = new ParticlePointMesh();
                this.setMesh(particleMesh);
                break;
            case Triangle:
                particleMesh = new ParticleTriMesh();
                this.setMesh(particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: " + meshType);
        }
        this.setNumParticles(numParticles);
//        particleMesh.initParticleData(this, particles.length);
    }

    /**
     * For serialization only. Do not use.
     */
    public ParticleEmitter() {
        super();
    }

    public void setShape(EmitterShape shape) {
        this.shape = shape;
    }

    public EmitterShape getShape() {
        return shape;
    }

    /**
     * Set the {@link ParticleInfluencer} to influence this particle emitter.
     * 
     * @param particleInfluencer the {@link ParticleInfluencer} to influence 
     * this particle emitter.
     * 
     * @see ParticleInfluencer
     */
    public void setParticleInfluencer(ParticleInfluencer particleInfluencer) {
        this.particleInfluencer = particleInfluencer;
    }

    /**
     * Returns the {@link ParticleInfluencer} that influences this 
     * particle emitter.
     * 
     * @return the {@link ParticleInfluencer} that influences this 
     * particle emitter.
     * 
     * @see ParticleInfluencer
     */
    public ParticleInfluencer getParticleInfluencer() {
        return particleInfluencer;
    }

    /**
     * Returns the mesh type used by the particle emitter.
     * 
     * 
     * @return the mesh type used by the particle emitter.
     * 
     * @see #setMeshType(com.jme3.effect.ParticleMesh.Type)
     * @see ParticleEmitter#ParticleEmitter(java.lang.String, com.jme3.effect.ParticleMesh.Type, int) 
     */
    public ParticleMesh.Type getMeshType() {
        return meshType;
    }

    /**
     * Sets the type of mesh used by the particle emitter.
     * @param meshType The mesh type to use
     */
    public void setMeshType(ParticleMesh.Type meshType) {
        this.meshType = meshType;
        switch (meshType) {
            case Point:
                particleMesh = new ParticlePointMesh();
                this.setMesh(particleMesh);
                break;
            case Triangle:
                particleMesh = new ParticleTriMesh();
                this.setMesh(particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: " + meshType);
        }
        this.setNumParticles(particles.length);
    }

    /**
     * Returns true if particles should spawn in world space. 
     * 
     * @return true if particles should spawn in world space. 
     * 
     * @see ParticleEmitter#setInWorldSpace(boolean) 
     */
    public boolean isInWorldSpace() {
        return worldSpace;
    }

    /**
     * Set to true if particles should spawn in world space. 
     * 
     * <p>If set to true and the particle emitter is moved in the scene,
     * then particles that have already spawned won't be effected by this
     * motion. If set to false, the particles will emit in local space
     * and when the emitter is moved, so are all the particles that
     * were emitted previously.
     * 
     * @param worldSpace true if particles should spawn in world space. 
     */
    public void setInWorldSpace(boolean worldSpace) {
        this.setIgnoreTransform(worldSpace);
        this.worldSpace = worldSpace;
    }

    /**
     * Returns the number of visible particles (spawned but not dead).
     * 
     * @return the number of visible particles
     */
    public int getNumVisibleParticles() {
//        return unusedIndices.size() + next;
        return lastUsed + 1;
    }

    /**
     * Set the maximum amount of particles that
     * can exist at the same time with this emitter.
     * Calling this method many times is not recommended.
     * 
     * @param numParticles the maximum amount of particles that
     * can exist at the same time with this emitter.
     */
    public final void setNumParticles(int numParticles) {
        particles = new Particle[numParticles];
        for (int i = 0; i < numParticles; i++) {
            particles[i] = new Particle();
        }
        //We have to reinit the mesh's buffers with the new size
        particleMesh.initParticleData(this, particles.length);
        particleMesh.setImagesXY(this.imagesX, this.imagesY);
        firstUnUsed = 0;
        lastUsed = -1;
    }

    public int getMaxNumParticles() {
        return particles.length;
    }

    /**
     * Returns a list of all particles (shouldn't be used in most cases).
     * 
     * <p>
     * This includes both existing and non-existing particles.
     * The size of the array is set to the <code>numParticles</code> value
     * specified in the constructor or {@link ParticleEmitter#setNumParticles(int) }
     * method. 
     * 
     * @return a list of all particles.
     */
    public Particle[] getParticles() {
        return particles;
    }

    /**
     * Get the normal which particles are facing. 
     * 
     * @return the normal which particles are facing. 
     * 
     * @see ParticleEmitter#setFaceNormal(com.jme3.math.Vector3f) 
     */
    public Vector3f getFaceNormal() {
        if (Vector3f.isValidVector(faceNormal)) {
            return faceNormal;
        } else {
            return null;
        }
    }

    /**
     * Sets the normal which particles are facing. 
     * 
     * <p>By default, particles
     * will face the camera, but for some effects (e.g shockwave) it may
     * be necessary to face a specific direction instead. To restore
     * normal functionality, provide <code>null</code> as the argument for
     * <code>faceNormal</code>.
     *
     * @param faceNormal The normals particles should face, or <code>null</code>
     * if particles should face the camera.
     */
    public void setFaceNormal(Vector3f faceNormal) {
        if (faceNormal == null || !Vector3f.isValidVector(faceNormal)) {
            this.faceNormal.set(Vector3f.NAN);
        } else {
            this.faceNormal = faceNormal;
        }
    }

    /**
     * Returns the rotation speed in radians/sec for particles.
     * 
     * @return the rotation speed in radians/sec for particles.
     * 
     * @see ParticleEmitter#setRotateSpeed(float) 
     */
    public float getRotateSpeed() {
        return rotateSpeed;
    }

    /**
     * Set the rotation speed in radians/sec for particles
     * spawned after the invocation of this method.
     * 
     * @param rotateSpeed the rotation speed in radians/sec for particles
     * spawned after the invocation of this method.
     */
    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    /**
     * Returns true if every particle spawned
     * should have a random facing angle. 
     * 
     * @return true if every particle spawned
     * should have a random facing angle. 
     * 
     * @see ParticleEmitter#setRandomAngle(boolean) 
     */
    public boolean isRandomAngle() {
        return randomAngle;
    }

    /**
     * Set to true if every particle spawned
     * should have a random facing angle. 
     * 
     * @param randomAngle if every particle spawned
     * should have a random facing angle.
     */
    public void setRandomAngle(boolean randomAngle) {
        this.randomAngle = randomAngle;
    }

    /**
     * Returns true if every particle spawned should get a random
     * image.
     * 
     * @return True if every particle spawned should get a random
     * image.
     * 
     * @see ParticleEmitter#setSelectRandomImage(boolean) 
     */
    public boolean isSelectRandomImage() {
        return selectRandomImage;
    }

    /**
     * Set to true if every particle spawned
     * should get a random image from a pool of images constructed from
     * the texture, with X by Y possible images.
     * 
     * <p>By default, X and Y are equal
     * to 1, thus allowing only 1 possible image to be selected, but if the
     * particle is configured with multiple images by using {@link ParticleEmitter#setImagesX(int) }
     * and {#link ParticleEmitter#setImagesY(int) } methods, then multiple images
     * can be selected. Setting to false will cause each particle to have an animation
     * of images displayed, starting at image 1, and going until image X*Y when
     * the particle reaches its end of life.
     * 
     * @param selectRandomImage True if every particle spawned should get a random
     * image.
     */
    public void setSelectRandomImage(boolean selectRandomImage) {
        this.selectRandomImage = selectRandomImage;
    }

    /**
     * Check if particles spawned should face their velocity.
     * 
     * @return True if particles spawned should face their velocity.
     * 
     * @see ParticleEmitter#setFacingVelocity(boolean) 
     */
    public boolean isFacingVelocity() {
        return facingVelocity;
    }

    /**
     * Set to true if particles spawned should face
     * their velocity (or direction to which they are moving towards).
     * 
     * <p>This is typically used for e.g spark effects.
     * 
     * @param followVelocity True if particles spawned should face their velocity.
     * 
     */
    public void setFacingVelocity(boolean followVelocity) {
        this.facingVelocity = followVelocity;
    }

    /**
     * Get the end color of the particles spawned.
     * 
     * @return the end color of the particles spawned.
     * 
     * @see ParticleEmitter#setEndColor(com.jme3.math.ColorRGBA) 
     */
    public ColorRGBA getEndColor() {
        return endColor;
    }

    /**
     * Set the end color of the particles spawned.
     * 
     * <p>The
     * particle color at any time is determined by blending the start color
     * and end color based on the particle's current time of life relative
     * to its end of life.
     * 
     * @param endColor the end color of the particles spawned.
     */
    public void setEndColor(ColorRGBA endColor) {
        this.endColor.set(endColor);
    }

    /**
     * Get the end size of the particles spawned.
     * 
     * @return the end size of the particles spawned.
     * 
     * @see ParticleEmitter#setEndSize(float) 
     */
    public float getEndSize() {
        return endSize;
    }

    /**
     * Set the end size of the particles spawned.
     * 
     * <p>The
     * particle size at any time is determined by blending the start size
     * and end size based on the particle's current time of life relative
     * to its end of life.
     * 
     * @param endSize the end size of the particles spawned.
     */
    public void setEndSize(float endSize) {
        this.endSize = endSize;
    }

    /**
     * Get the gravity vector.
     * 
     * @return the gravity vector.
     * 
     * @see ParticleEmitter#setGravity(com.jme3.math.Vector3f) 
     */
    public Vector3f getGravity() {
        return gravity;
    }

    /**
     * This method sets the gravity vector.
     * 
     * @param gravity the gravity vector
     */
    public void setGravity(Vector3f gravity) {
        this.gravity.set(gravity);
    }

    /**
     * Sets the gravity vector.
     * 
     * @param x the x component of the gravity vector
     * @param y the y component of the gravity vector
     * @param z the z component of the gravity vector
     */
    public void setGravity(float x, float y, float z) {
        this.gravity.x = x;
        this.gravity.y = y;
        this.gravity.z = z;
    }

    /**
     * Get the high value of life.
     * 
     * @return the high value of life.
     * 
     * @see ParticleEmitter#setHighLife(float) 
     */
    public float getHighLife() {
        return highLife;
    }

    /**
     * Set the high value of life.
     * 
     * <p>The particle's lifetime/expiration
     * is determined by randomly selecting a time between low life and high life.
     * 
     * @param highLife the high value of life.
     */
    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    /**
     * Get the number of images along the X axis (width).
     * 
     * @return the number of images along the X axis (width).
     * 
     * @see ParticleEmitter#setImagesX(int) 
     */
    public int getImagesX() {
        return imagesX;
    }

    /**
     * Set the number of images along the X axis (width).
     * 
     * <p>To determine
     * how multiple particle images are selected and used, see the
     * {@link ParticleEmitter#setSelectRandomImage(boolean) } method.
     * 
     * @param imagesX the number of images along the X axis (width).
     */
    public void setImagesX(int imagesX) {
        this.imagesX = imagesX;
        particleMesh.setImagesXY(this.imagesX, this.imagesY);
    }

    /**
     * Get the number of images along the Y axis (height).
     * 
     * @return the number of images along the Y axis (height).
     * 
     * @see ParticleEmitter#setImagesY(int) 
     */
    public int getImagesY() {
        return imagesY;
    }

    /**
     * Set the number of images along the Y axis (height).
     * 
     * <p>To determine how multiple particle images are selected and used, see the
     * {@link ParticleEmitter#setSelectRandomImage(boolean) } method.
     * 
     * @param imagesY the number of images along the Y axis (height).
     */
    public void setImagesY(int imagesY) {
        this.imagesY = imagesY;
        particleMesh.setImagesXY(this.imagesX, this.imagesY);
    }

    /**
     * Get the low value of life.
     * 
     * @return the low value of life.
     * 
     * @see ParticleEmitter#setLowLife(float) 
     */
    public float getLowLife() {
        return lowLife;
    }

    /**
     * Set the low value of life.
     * 
     * <p>The particle's lifetime/expiration
     * is determined by randomly selecting a time between low life and high life.
     * 
     * @param lowLife the low value of life.
     */
    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }

    /**
     * Get the number of particles to spawn per
     * second.
     * 
     * @return the number of particles to spawn per
     * second.
     * 
     * @see ParticleEmitter#setParticlesPerSec(float) 
     */
    public float getParticlesPerSec() {
        return particlesPerSec;
    }

    /**
     * Set the number of particles to spawn per
     * second.
     * 
     * @param particlesPerSec the number of particles to spawn per
     * second.
     */
    public void setParticlesPerSec(float particlesPerSec) {
        this.particlesPerSec = particlesPerSec;
    }

    /**
     * Get the start color of the particles spawned.
     * 
     * @return the start color of the particles spawned.
     * 
     * @see ParticleEmitter#setStartColor(com.jme3.math.ColorRGBA) 
     */
    public ColorRGBA getStartColor() {
        return startColor;
    }

    /**
     * Set the start color of the particles spawned.
     * 
     * <p>The particle color at any time is determined by blending the start color
     * and end color based on the particle's current time of life relative
     * to its end of life.
     * 
     * @param startColor the start color of the particles spawned
     */
    public void setStartColor(ColorRGBA startColor) {
        this.startColor.set(startColor);
    }

    /**
     * Get the start color of the particles spawned.
     * 
     * @return the start color of the particles spawned.
     * 
     * @see ParticleEmitter#setStartSize(float) 
     */
    public float getStartSize() {
        return startSize;
    }

    /**
     * Set the start size of the particles spawned.
     * 
     * <p>The particle size at any time is determined by blending the start size
     * and end size based on the particle's current time of life relative
     * to its end of life.
     * 
     * @param startSize the start size of the particles spawned.
     */
    public void setStartSize(float startSize) {
        this.startSize = startSize;
    }

    /**
     * @deprecated Use ParticleEmitter.getParticleInfluencer().getInitialVelocity() instead.
     */
    @Deprecated
    public Vector3f getInitialVelocity() {
        return particleInfluencer.getInitialVelocity();
    }

    /**
     * @param initialVelocity Set the initial velocity a particle is spawned with,
     * the initial velocity given in the parameter will be varied according
     * to the velocity variation set in {@link ParticleEmitter#setVelocityVariation(float) }.
     * A particle will move toward its velocity unless it is effected by the
     * gravity.
     *
     * @deprecated
     * This method is deprecated. 
     * Use ParticleEmitter.getParticleInfluencer().setInitialVelocity(initialVelocity); instead.
     *
     * @see ParticleEmitter#setVelocityVariation(float) 
     * @see ParticleEmitter#setGravity(float)
     */
    @Deprecated
    public void setInitialVelocity(Vector3f initialVelocity) {
        this.particleInfluencer.setInitialVelocity(initialVelocity);
    }

    /**
     * @deprecated
     * This method is deprecated. 
     * Use ParticleEmitter.getParticleInfluencer().getVelocityVariation(); instead.
     * @return the initial velocity variation factor
     */
    @Deprecated
    public float getVelocityVariation() {
        return particleInfluencer.getVelocityVariation();
    }

    /**
     * @param variation Set the variation by which the initial velocity
     * of the particle is determined. <code>variation</code> should be a value
     * from 0 to 1, where 0 means particles are to spawn with exactly
     * the velocity given in {@link ParticleEmitter#setStartVel(com.jme3.math.Vector3f) },
     * and 1 means particles are to spawn with a completely random velocity.
     * 
     * @deprecated
     * This method is deprecated. 
     * Use ParticleEmitter.getParticleInfluencer().setVelocityVariation(variation); instead.
     */
    @Deprecated
    public void setVelocityVariation(float variation) {
        this.particleInfluencer.setVelocityVariation(variation);
    }

    private Particle emitParticle(Vector3f min, Vector3f max) {
        int idx = lastUsed + 1;
        if (idx >= particles.length) {
            return null;
        }

        Particle p = particles[idx];
        if (selectRandomImage) {
            p.imageIndex = FastMath.nextRandomInt(0, imagesY - 1) * imagesX + FastMath.nextRandomInt(0, imagesX - 1);
        }

        p.startlife = lowLife + FastMath.nextRandomFloat() * (highLife - lowLife);
        p.life = p.startlife;
        p.color.set(startColor);
        p.size = startSize;
        //shape.getRandomPoint(p.position);
        particleInfluencer.influenceParticle(p, shape);
        if (worldSpace) {
            worldTransform.transformVector(p.position, p.position);
            worldTransform.getRotation().mult(p.velocity, p.velocity);
            // TODO: Make scale relevant somehow??
        }
        if (randomAngle) {
            p.angle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        }
        if (rotateSpeed != 0) {
            p.rotateSpeed = rotateSpeed * (0.2f + (FastMath.nextRandomFloat() * 2f - 1f) * .8f);
        }

        temp.set(p.position).addLocal(p.size, p.size, p.size);
        max.maxLocal(temp);
        temp.set(p.position).subtractLocal(p.size, p.size, p.size);
        min.minLocal(temp);

        ++lastUsed;
        firstUnUsed = idx + 1;
        return p;
    }

    /**
     * Instantly emits all the particles possible to be emitted. Any particles
     * which are currently inactive will be spawned immediately.
     */
    public void emitAllParticles() {
        // Force world transform to update
        this.getWorldTransform();

        TempVars vars = TempVars.get();

        BoundingBox bbox = (BoundingBox) this.getMesh().getBound();

        Vector3f min = vars.vect1;
        Vector3f max = vars.vect2;

        bbox.getMin(min);
        bbox.getMax(max);

        if (!Vector3f.isValidVector(min)) {
            min.set(Vector3f.POSITIVE_INFINITY);
        }
        if (!Vector3f.isValidVector(max)) {
            max.set(Vector3f.NEGATIVE_INFINITY);
        }

        while (emitParticle(min, max) != null);

        bbox.setMinMax(min, max);
        this.setBoundRefresh();

        vars.release();
    }

    /**
     * Instantly kills all active particles, after this method is called, all
     * particles will be dead and no longer visible.
     */
    public void killAllParticles() {
        for (int i = 0; i < particles.length; ++i) {
            if (particles[i].life > 0) {
                this.freeParticle(i);
            }
        }
    }
    
    /**
     * Kills the particle at the given index.
     * 
     * @param index The index of the particle to kill
     * @see #getParticles() 
     */
    public void killParticle(int index){
        freeParticle(index);
    }

    private void freeParticle(int idx) {
        Particle p = particles[idx];
        p.life = 0;
        p.size = 0f;
        p.color.set(0, 0, 0, 0);
        p.imageIndex = 0;
        p.angle = 0;
        p.rotateSpeed = 0;

        if (idx == lastUsed) {
            while (lastUsed >= 0 && particles[lastUsed].life == 0) {
                lastUsed--;
            }
        }
        if (idx < firstUnUsed) {
            firstUnUsed = idx;
        }
    }

    private void swap(int idx1, int idx2) {
        Particle p1 = particles[idx1];
        particles[idx1] = particles[idx2];
        particles[idx2] = p1;
    }

    private void updateParticle(Particle p, float tpf, Vector3f min, Vector3f max){
        // applying gravity
        p.velocity.x -= gravity.x * tpf;
        p.velocity.y -= gravity.y * tpf;
        p.velocity.z -= gravity.z * tpf;
        temp.set(p.velocity).multLocal(tpf);
        p.position.addLocal(temp);

        // affecting color, size and angle
        float b = (p.startlife - p.life) / p.startlife;
        p.color.interpolate(startColor, endColor, b);
        p.size = FastMath.interpolateLinear(b, startSize, endSize);
        p.angle += p.rotateSpeed * tpf;

        // Computing bounding volume
        temp.set(p.position).addLocal(p.size, p.size, p.size);
        max.maxLocal(temp);
        temp.set(p.position).subtractLocal(p.size, p.size, p.size);
        min.minLocal(temp);

        if (!selectRandomImage) {
            p.imageIndex = (int) (b * imagesX * imagesY);
        }
    }
    
    private void updateParticleState(float tpf) {
        // Force world transform to update
        this.getWorldTransform();

        TempVars vars = TempVars.get();

        Vector3f min = vars.vect1.set(Vector3f.POSITIVE_INFINITY);
        Vector3f max = vars.vect2.set(Vector3f.NEGATIVE_INFINITY);

        for (int i = 0; i < particles.length; ++i) {
            Particle p = particles[i];
            if (p.life == 0) { // particle is dead
//                assert i <= firstUnUsed;
                continue;
            }

            p.life -= tpf;
            if (p.life <= 0) {
                this.freeParticle(i);
                continue;
            }

            updateParticle(p, tpf, min, max);

            if (firstUnUsed < i) {
                this.swap(firstUnUsed, i);
                if (i == lastUsed) {
                    lastUsed = firstUnUsed;
                }
                firstUnUsed++;
            }
        }
        
        // Spawns particles within the tpf timeslot with proper age
        float interval = 1f / particlesPerSec;
        tpf += timeDifference;
        while (tpf > interval){
            tpf -= interval;
            Particle p = emitParticle(min, max);
            if (p != null){
                p.life -= tpf;
                if (p.life <= 0){
                    freeParticle(lastUsed);
                }else{
                    updateParticle(p, tpf, min, max);
                }
            }
        }
        timeDifference = tpf;

        BoundingBox bbox = (BoundingBox) this.getMesh().getBound();
        bbox.setMinMax(min, max);
        this.setBoundRefresh();

        vars.release();
    }

    /**
     * Set to enable or disable the particle emitter
     * 
     * <p>When a particle is
     * disabled, it will be "frozen in time" and not update.
     * 
     * @param enabled True to enable the particle emitter
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if a particle emitter is enabled for update.
     * 
     * @return True if a particle emitter is enabled for update.
     * 
     * @see ParticleEmitter#setEnabled(boolean) 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Callback from Control.update(), do not use.
     * @param tpf 
     */
    public void updateFromControl(float tpf) {
        if (enabled) {
            this.updateParticleState(tpf);
        }
    }

    /**
     * Callback from Control.render(), do not use.
     * 
     * @param rm
     * @param vp 
     */
    private void renderFromControl(RenderManager rm, ViewPort vp) {
        Camera cam = vp.getCamera();

        if (meshType == ParticleMesh.Type.Point) {
            float C = cam.getProjectionMatrix().m00;
            C *= cam.getWidth() * 0.5f;

            // send attenuation params
            this.getMaterial().setFloat("Quadratic", C);
        }

        Matrix3f inverseRotation = Matrix3f.IDENTITY;
        TempVars vars = null;
        if (!worldSpace) {
            vars = TempVars.get();

            inverseRotation = this.getWorldRotation().toRotationMatrix(vars.tempMat3).invertLocal();
        }
        particleMesh.updateParticleData(particles, cam, inverseRotation);
        if (!worldSpace) {
            vars.release();
        }
    }

    public void preload(RenderManager rm, ViewPort vp) {
        this.updateParticleState(0);
        particleMesh.updateParticleData(particles, vp.getCamera(), Matrix3f.IDENTITY);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(shape, "shape", DEFAULT_SHAPE);
        oc.write(meshType, "meshType", ParticleMesh.Type.Triangle);
        oc.write(enabled, "enabled", true);
        oc.write(particles.length, "numParticles", 0);
        oc.write(particlesPerSec, "particlesPerSec", 0);
        oc.write(lowLife, "lowLife", 0);
        oc.write(highLife, "highLife", 0);
        oc.write(gravity, "gravity", null);
        oc.write(imagesX, "imagesX", 1);
        oc.write(imagesY, "imagesY", 1);

        oc.write(startColor, "startColor", null);
        oc.write(endColor, "endColor", null);
        oc.write(startSize, "startSize", 0);
        oc.write(endSize, "endSize", 0);
        oc.write(worldSpace, "worldSpace", false);
        oc.write(facingVelocity, "facingVelocity", false);
        oc.write(faceNormal, "faceNormal", new Vector3f(Vector3f.NAN));
        oc.write(selectRandomImage, "selectRandomImage", false);
        oc.write(randomAngle, "randomAngle", false);
        oc.write(rotateSpeed, "rotateSpeed", 0);

        oc.write(particleInfluencer, "influencer", DEFAULT_INFLUENCER);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        shape = (EmitterShape) ic.readSavable("shape", DEFAULT_SHAPE);

        if (shape == DEFAULT_SHAPE) {
            // Prevent reference to static
            shape = shape.deepClone();
        }

        meshType = ic.readEnum("meshType", ParticleMesh.Type.class, ParticleMesh.Type.Triangle);
        int numParticles = ic.readInt("numParticles", 0);


        enabled = ic.readBoolean("enabled", true);
        particlesPerSec = ic.readFloat("particlesPerSec", 0);
        lowLife = ic.readFloat("lowLife", 0);
        highLife = ic.readFloat("highLife", 0);
        gravity = (Vector3f) ic.readSavable("gravity", null);
        imagesX = ic.readInt("imagesX", 1);
        imagesY = ic.readInt("imagesY", 1);

        startColor = (ColorRGBA) ic.readSavable("startColor", null);
        endColor = (ColorRGBA) ic.readSavable("endColor", null);
        startSize = ic.readFloat("startSize", 0);
        endSize = ic.readFloat("endSize", 0);
        worldSpace = ic.readBoolean("worldSpace", false);
        this.setIgnoreTransform(worldSpace);
        facingVelocity = ic.readBoolean("facingVelocity", false);
        faceNormal = (Vector3f)ic.readSavable("faceNormal", new Vector3f(Vector3f.NAN));
        selectRandomImage = ic.readBoolean("selectRandomImage", false);
        randomAngle = ic.readBoolean("randomAngle", false);
        rotateSpeed = ic.readFloat("rotateSpeed", 0);

        switch (meshType) {
            case Point:
                particleMesh = new ParticlePointMesh();
                this.setMesh(particleMesh);
                break;
            case Triangle:
                particleMesh = new ParticleTriMesh();
                this.setMesh(particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: " + meshType);
        }
        this.setNumParticles(numParticles);
//        particleMesh.initParticleData(this, particles.length);
//        particleMesh.setImagesXY(imagesX, imagesY);

        particleInfluencer = (ParticleInfluencer) ic.readSavable("influencer", DEFAULT_INFLUENCER);
        if (particleInfluencer == DEFAULT_INFLUENCER) {
            particleInfluencer = particleInfluencer.clone();
        }

        if (im.getFormatVersion() == 0) {
            // compatibility before the control inside particle emitter
            // was changed:
            // find it in the controls and take it out, then add the proper one in
            for (int i = 0; i < controls.size(); i++) {
                Object obj = controls.get(i);
                if (obj instanceof ParticleEmitter) {
                    controls.remove(i);
                    // now add the proper one in
                    controls.add(new ParticleEmitterControl(this));
                    break;
                }
            }

            // compatability before gravity was not a vector but a float
            if (gravity == null) {
                gravity = new Vector3f();
                gravity.y = ic.readFloat("gravity", 0);
            }
        } else {
            // since the parentEmitter is not loaded, it must be 
            // loaded separately
            control = getControl(ParticleEmitterControl.class);
            control.parentEmitter = this;

        }
    }
}
