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

package com.jme3.effect;

import com.jme3.bounding.BoundingBox;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.influencers.DefaultParticleInfluencer;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
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

public class ParticleEmitter extends Geometry implements Control {
    private static final EmitterShape DEFAULT_SHAPE = new EmitterPointShape(Vector3f.ZERO);
    private static final ParticleInfluencer DEFAULT_INFLUENCER = new DefaultParticleInfluencer();
    
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
    private float emitCarry;
    private float lowLife  = 3f;
    private float highLife = 7f;
    private Vector3f gravity = new Vector3f(0.0f, 0.1f, 0.0f);
    private float rotateSpeed;
    private Vector3f faceNormal = new Vector3f(Vector3f.NAN);

    private int imagesX = 1;
    private int imagesY = 1;

    private boolean enabled = true;
    private ColorRGBA startColor = new ColorRGBA(0.4f,0.4f,0.4f,0.5f);
    private ColorRGBA endColor = new ColorRGBA(0.1f,0.1f,0.1f,0.0f);
    private float startSize = 0.2f;
    private float endSize = 2f;
    private boolean worldSpace = true;

    //variable that helps with computations
    private transient Vector3f temp = new Vector3f();
    
    @Override
    public ParticleEmitter clone(){
        ParticleEmitter clone = (ParticleEmitter) super.clone();
        clone.shape = shape.deepClone();
        clone.setNumParticles(particles.length);
        clone.faceNormal = faceNormal.clone();
        clone.startColor = startColor.clone();
        clone.endColor = endColor.clone();
        clone.particleInfluencer = particleInfluencer.clone();
        clone.controls.add(clone);
        return clone;
    }

    public ParticleEmitter(String name, Type type, int numParticles){
        super(name);

        // ignore world transform, unless user sets inLocalSpace
        this.setIgnoreTransform(true);

        // particles neither receive nor cast shadows
        this.setShadowMode(ShadowMode.Off);

        // particles are usually transparent
        this.setQueueBucket(Bucket.Transparent);

        meshType = type;

        this.setNumParticles(numParticles);

        controls.add(this);

        switch (meshType){
            case Point:
                particleMesh = new ParticlePointMesh();
                this.setMesh(particleMesh);
                break;
            case Triangle:
                particleMesh = new ParticleTriMesh();
                this.setMesh(particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: "+meshType);
        }
        particleMesh.initParticleData(this, particles.length);
    }

    public ParticleEmitter(){
        super();
    }

    @Override
	public Control cloneForSpatial(Spatial spatial){
        return (Control) spatial;
    }

    public void setShape(EmitterShape shape) {
        this.shape = shape;
    }

    public EmitterShape getShape(){
        return shape;
    }
    
    public void setParticleInfluencer(ParticleInfluencer particleInfluencer) {
		this.particleInfluencer = particleInfluencer;
	}
    
    public ParticleInfluencer getParticleInfluencer() {
		return particleInfluencer;
	}

    public ParticleMesh.Type getMeshType() {
		return meshType;
	}
    
    public boolean isInWorldSpace() {
        return worldSpace;
    }

    public void setInWorldSpace(boolean worldSpace) {
        this.setIgnoreTransform(worldSpace);
        this.worldSpace = worldSpace;
    }

    public int getNumVisibleParticles(){
//        return unusedIndices.size() + next;
        return lastUsed + 1;
    }

    /**
     * @param numParticles The maximum amount of particles that
     * can exist at the same time with this emitter.
     * Calling this method many times is not recommended.
     */
    public final void setNumParticles(int numParticles){
        particles = new Particle[numParticles];
        for (int i = 0; i < numParticles; i++){
            particles[i] = new Particle();
        }
        firstUnUsed = 0;
        lastUsed = -1;
    }

    /**
     * @return A list of all particles (shouldn't be used in most cases).
     * This includes both existing and non-existing particles.
     * The size of the array is set to the <code>numParticles</code> value
     * specified in the constructor or {@link ParticleEmitter#setNumParticles(int) }
     * method. 
     */
    public Particle[] getParticles(){
        return particles;
    }

    public Vector3f getFaceNormal() {
        if (Vector3f.isValidVector(faceNormal)) {
			return faceNormal;
		} else {
			return null;
		}
    }

    /**
     * Sets the normal which particles are facing. By default, particles
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

    public float getRotateSpeed() {
        return rotateSpeed;
    }

    /**
     * @param rotateSpeed Set the rotation speed in radians/sec for particles
     * spawned after the invocation of this method.
     */
    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public boolean isRandomAngle() {
        return randomAngle;
    }

    /**
     * @param randomAngle Set to <code>true</code> if every particle spawned
     * should have a random facing angle. 
     */
    public void setRandomAngle(boolean randomAngle) {
        this.randomAngle = randomAngle;
    }

    public boolean isSelectRandomImage() {
        return selectRandomImage;
    }

    /**
     * @param selectRandomImage Set to true if every particle spawned
     * should get a random image from a pool of images constructed from
     * the texture, with X by Y possible images. By default, X and Y are equal
     * to 1, thus allowing only 1 possible image to be selected, but if the
     * particle is configured with multiple images by using {@link ParticleEmitter#setImagesX(int) }
     * and {#link ParticleEmitter#setImagesY(int) } methods, then multiple images
     * can be selected. Setting to false will cause each particle to have an animation
     * of images displayed, starting at image 1, and going until image X*Y when
     * the particle reaches its end of life.
     */
    public void setSelectRandomImage(boolean selectRandomImage) {
        this.selectRandomImage = selectRandomImage;
    }

    public boolean isFacingVelocity() {
        return facingVelocity;
    }

    /**
     * @param followVelocity Set to true if particles spawned should face
     * their velocity (or direction to which they are moving towards).
     * This is typically used for e.g spark effects.
     */
    public void setFacingVelocity(boolean followVelocity) {
        this.facingVelocity = followVelocity;
    }

    public ColorRGBA getEndColor() {
        return endColor;
    }

    /**
     * @param endColor Set the end color of the particles spawned. The
     * particle color at any time is determined by blending the start color
     * and end color based on the particle's current time of life relative
     * to its end of life.
     */
    public void setEndColor(ColorRGBA endColor) {
        this.endColor.set(endColor);
    }

    public float getEndSize() {
        return endSize;
    }

    /**
     * @param endSize Set the end size of the particles spawned.The
     * particle size at any time is determined by blending the start size
     * and end size based on the particle's current time of life relative
     * to its end of life.
     */
    public void setEndSize(float endSize) {
        this.endSize = endSize;
    }

	/**
	 * This method sets the gravity value of Y axis.
	 * By default the Y axis is the only one to have gravity value non zero.
	 * @param gravity
	 *        Set the gravity of Y axis, in units/sec/sec, of particles
	 *        spawned.
	 */
	@Deprecated
	public void setGravity(float gravity) {
		this.gravity.y = gravity;
	}

	/**
	 * This method returns the gravity vector.
	 * @return the gravity vector
	 */
	public Vector3f getGravity() {
		return gravity;
	}

	/**
	 * This method sets the gravity vector.
	 * @param gravity
	 *        the gravity vector
	 */
	public void setGravity(Vector3f gravity) {
		this.gravity.set(gravity);
	}

	/**
	 * This method sets the gravity vector.
	 * @param gravity
	 *        the gravity vector
	 */
	public void setGravity(float[] gravity) {
		this.setGravity(gravity[0], gravity[1], gravity[2]);
	}

	/**
	 * This method sets the gravity vector.
	 * @param gravity
	 *        the gravity vector
	 */
	public void setGravity(float x, float y, float z) {
		this.gravity.x = x;
		this.gravity.y = y;
		this.gravity.z = z;
	}

    public float getHighLife() {
        return highLife;
    }

    /**
     * @param highLife Set the high value of life. The particle's lifetime/expiration
     * is determined by randomly selecting a time between low life and high life.
     */
    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    public int getImagesX() {
        return imagesX;
    }

    /**
     * @param imagesX Set the number of images along the X axis (width). To determine
     * how multiple particle images are selected and used, see the
     * {@link ParticleEmitter#setSelectRandomImage(boolean) } method.
     */
    public void setImagesX(int imagesX) {
        this.imagesX = imagesX;
        particleMesh.setImagesXY(this.imagesX, this.imagesY);
    }

    public int getImagesY() {
        return imagesY;
    }

    /**
     * @param imagesY Set the number of images along the Y axis (height). To determine
     * how multiple particle images are selected and used, see the
     * {@link ParticleEmitter#setSelectRandomImage(boolean) } method.
     */
    public void setImagesY(int imagesY) {
        this.imagesY = imagesY;
        particleMesh.setImagesXY(this.imagesX, this.imagesY);
    }

    public float getLowLife() {
        return lowLife;
    }

    /**
     * @param lowLife Set the low value of life. The particle's lifetime/expiration
     * is determined by randomly selecting a time between low life and high life.
     */
    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }

    public float getParticlesPerSec() {
        return particlesPerSec;
    }

    /**
     * @param particlesPerSec Set the number of particles to spawn per
     * second.
     */
    public void setParticlesPerSec(float particlesPerSec) {
        this.particlesPerSec = particlesPerSec;
    }

    public ColorRGBA getStartColor() {
        return startColor;
    }

    /**
     * @param startColor Set the start color of the particles spawned. The
     * particle color at any time is determined by blending the start color
     * and end color based on the particle's current time of life relative
     * to its end of life.
     */
    public void setStartColor(ColorRGBA startColor) {
        this.startColor.set(startColor);
    }

    public float getStartSize() {
        return startSize;
    }

    /**
     * @param startSize Set the start size of the particles spawned.The
     * particle size at any time is determined by blending the start size
     * and end size based on the particle's current time of life relative
     * to its end of life.
     */
    public void setStartSize(float startSize) {
        this.startSize = startSize;
    }

    /**
     * This method is deprecated.
     * Use ParticleEmitter.getParticleInfluencer().getInitialVelocity() instead.
     * @return the initial velocity for particles
     */
    @Deprecated
    public Vector3f getInitialVelocity(){
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
    public void setInitialVelocity(Vector3f initialVelocity){
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

//    private int newIndex(){
//        liveParticles ++;
//        return unusedIndices.remove(0);
//        if (unusedIndices.size() > 0){
//            liveParticles++;
//            return unusedIndices.remove(0);
//        }else if (next < particles.length){
//            liveParticles++;
//            return next++;
//        }else{
//            return -1;
//        }
//    }

//    private void freeIndex(int index){
//        liveParticles--;
//        if (index == next-1)
//            next--;
//        else
//        assert !unusedIndices.contains(index);
//        unusedIndices.add(index);
//    }

    private boolean emitParticle(Vector3f min, Vector3f max){
//        int idx = newIndex();
//        if (idx == -1)
//            return false;
        int idx = lastUsed + 1;
        if (idx >= particles.length) {
            return false;
        }

        Particle p = particles[idx];
        if (selectRandomImage) {
			p.imageIndex = FastMath.nextRandomInt(0, imagesY-1) * imagesX + FastMath.nextRandomInt(0, imagesX-1);
		}

        p.startlife = lowLife + FastMath.nextRandomFloat() * (highLife - lowLife);
        p.life = p.startlife;
        p.color.set(startColor);
        p.size = startSize;
        //shape.getRandomPoint(p.position);
        particleInfluencer.influenceParticle(p, shape);
        if (worldSpace){
            p.position.addLocal(worldTransform.getTranslation());
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
        return true;
    }

    /**
     * Instantly emits all the particles possible to be emitted. Any particles
     * which are currently inactive will be spawned immediately.
     */
    public void emitAllParticles(){
        // Force world transform to update
        this.getWorldTransform();

        TempVars vars = TempVars.get();
        assert vars.lock();
        
        BoundingBox bbox = (BoundingBox) this.getMesh().getBound();

        Vector3f min = vars.vect1;
        Vector3f max = vars.vect2;

        bbox.getMin(min);
        bbox.getMax(max);

        if (!Vector3f.isValidVector(min)){
            min.set(Vector3f.POSITIVE_INFINITY);
        }
        if (!Vector3f.isValidVector(max)){
            max.set(Vector3f.NEGATIVE_INFINITY);
        }
        
        while (this.emitParticle(min, max)) {
			;
		}

        bbox.setMinMax(min, max);
        this.setBoundRefresh();

        assert vars.unlock();
    }

    /**
     * Instantly kills all active particles, after this method is called, all
     * particles will be dead and no longer visible.
     */
    public void killAllParticles(){
        for (int i = 0; i < particles.length; ++i){
            if (particles[i].life > 0) {
				this.freeParticle(i);
			}
        }
    }

    private void freeParticle(int idx){
        Particle p = particles[idx];
        p.life = 0;
        p.size = 0f;
        p.color.set(0,0,0,0);
        p.imageIndex = 0;
        p.angle = 0;
        p.rotateSpeed = 0;

//        freeIndex(idx);

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

    private void updateParticleState(float tpf){
        // Force world transform to update
        this.getWorldTransform();

        TempVars vars = TempVars.get();
        assert vars.lock();

        Vector3f min = vars.vect1.set(Vector3f.POSITIVE_INFINITY);
        Vector3f max = vars.vect2.set(Vector3f.NEGATIVE_INFINITY);

        for (int i = 0; i < particles.length; ++i){
            Particle p = particles[i];
            if (p.life == 0){ // particle is dead
//                assert i <= firstUnUsed;
                continue;
            }

            p.life -= tpf;
            if (p.life <= 0){
                this.freeParticle(i);
                continue;
            }

            // position += velocity * tpf
            p.distToCam = -1;
            
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
            
            if (firstUnUsed < i) {
                this.swap(firstUnUsed, i);
                if (i == lastUsed) {
                    lastUsed = firstUnUsed;
                }
                firstUnUsed++;
            }
        }

        float particlesToEmitF = particlesPerSec * tpf;
        int particlesToEmit = (int) particlesToEmitF;
        emitCarry += particlesToEmitF - particlesToEmit;

        while (emitCarry > 1f){
            ++particlesToEmit;
            emitCarry -= 1f;
        }

        for (int i = 0; i < particlesToEmit; ++i){
            this.emitParticle(min, max);
        }

        BoundingBox bbox = (BoundingBox) this.getMesh().getBound();
        bbox.setMinMax(min, max);
        this.setBoundRefresh();

        assert vars.unlock();
    }

    /**
     * Do not use.
     */
    @Override
	public void setSpatial(Spatial spatial) {
    }

    /**
     * @param enabled Set to enable or disable a particle. When a particle is
     * disabled, it will be "frozen in time" and not update.
     */
    @Override
	public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
	public boolean isEnabled() {
        return enabled;
    }

    @Override
	public void update(float tpf) {
        if (enabled) {
        	this.updateParticleState(tpf);
		}
    }

    @Override
	public void render(RenderManager rm, ViewPort vp) {
        Camera cam = vp.getCamera();

        if (meshType == ParticleMesh.Type.Point){
            float C = cam.getProjectionMatrix().m00;
            C *= cam.getWidth() * 0.5f;

            // send attenuation params
            this.getMaterial().setFloat("Quadratic", C);
        }

        Matrix3f inverseRotation = Matrix3f.IDENTITY;
        if (!worldSpace){
            TempVars vars = TempVars.get();
            assert vars.lock();
            inverseRotation = this.getWorldRotation().toRotationMatrix(vars.tempMat3).invertLocal();
        }
        particleMesh.updateParticleData(particles, cam, inverseRotation);
        if (!worldSpace){
            assert TempVars.get().unlock();
        }
    }

    public void preload(RenderManager rm, ViewPort vp){
        this.updateParticleState(0);
        particleMesh.updateParticleData(particles, vp.getCamera(), Matrix3f.IDENTITY);
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
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
        oc.write(selectRandomImage, "selectRandomImage", false);
        oc.write(randomAngle, "randomAngle", false);
        oc.write(rotateSpeed, "rotateSpeed", 0);
        
        oc.write(particleInfluencer, "influencer", DEFAULT_INFLUENCER);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        shape = (EmitterShape) ic.readSavable("shape", DEFAULT_SHAPE);
        meshType = ic.readEnum("meshType", ParticleMesh.Type.class, ParticleMesh.Type.Triangle);
        int numParticles = ic.readInt("numParticles", 0);
        this.setNumParticles(numParticles);

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
        facingVelocity = ic.readBoolean("facingVelocity", false);
        selectRandomImage = ic.readBoolean("selectRandomImage", false);
        randomAngle = ic.readBoolean("randomAngle", false);
        rotateSpeed = ic.readFloat("rotateSpeed", 0);
        
        switch (meshType){
            case Point:
                particleMesh = new ParticlePointMesh();
                this.setMesh(particleMesh);
                break;
            case Triangle:
                particleMesh = new ParticleTriMesh();
                this.setMesh(particleMesh);
                break;
            default:
                throw new IllegalStateException("Unrecognized particle type: "+meshType);
        }
        particleMesh.initParticleData(this, particles.length);
        
        particleInfluencer = (ParticleInfluencer) ic.readSavable("influencer", DEFAULT_INFLUENCER);
    }

}
