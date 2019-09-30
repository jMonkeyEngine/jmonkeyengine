/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.particles.influencers;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * Gravity Module
 *
 * Controls the gravity affecting the particle
 *
 * @author t0neg0d
 * @author Jedic
 */
public class GravityInfluencer extends ParticleInfluencer {
	private VectorValueType gravity = new VectorValueType(new Vector3f(0,9.8f,0));

	private transient Vector3f store = new Vector3f();

	public GravityInfluencer() {

	}

	public GravityInfluencer(Vector3f value) {
		gravity.setValue(value);
	}
	
  @Override
	public void update(ParticleData p, float tpf) {
		if (enabled) {
			gravity.getValue3f(p.percentLife, p.randomValue, store);

			// transform so the gravity applies according to the world
			if (emitter.getParticlesFollowEmitter()) {
				emitter.getWorldTransform().transformVector(store, store);
			}
			p.velocity.x -= store.x * tpf;
			p.velocity.y -= store.y * tpf;
			p.velocity.z -= store.z * tpf;
		}
	}
	
	public void initialize(ParticleData p) {
		
	}

	public void reset(ParticleData p) {
		
	}

	/**
	 * Sets gravity to the provided Vector3f
	 * @param gravity Vector3f representing gravity
	 */
	public void setGravity(VectorValueType gravity) {
		this.gravity = gravity;
	}
  
  public VectorValueType getGravity() {
    return this.gravity;
  }
  
	/**
	 * Sets gravity per axis to the specified values as a constant gravity
	 * @param x Gravity along the x axis
	 * @param y Gravity along the y axis
	 * @param z Gravity along the z axis
	 */
	public void setGravity(float x, float y, float z) {
		this.gravity = new VectorValueType(new Vector3f(x, y, z));
	}
	
  @Override
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(gravity, "gravity", new VectorValueType(new Vector3f(0, 9.8f, 0)));
	}

  @Override
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		gravity = (VectorValueType) ic.readSavable("gravity", new VectorValueType(new Vector3f(0, 9.8f, 0)));
	}
	
	@Override
	public ParticleInfluencer clone() {
		GravityInfluencer clone = (GravityInfluencer) super.clone();
		clone.setGravity(gravity.clone());
		return clone;
	}

	@Override
	public int getPriority() {
		return 500;
	}

}
