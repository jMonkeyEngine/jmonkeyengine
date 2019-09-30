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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * Impulse Module
 *
 * Modifies the existing velocity over time by interpolating between it and the linear / orbital values.
 *
 * @author t0neg0d
 * @author Jedic
 */
public class ImpulseInfluencer extends ParticleInfluencer {
	private float chance = .2f;
	private float magnitude = 0.5f;
	private float strength = 1;

	private VectorValueType linear = new VectorValueType(new Vector3f(0, 2, 0));
	private VectorValueType orbital = new VectorValueType(new Vector3f(0, 0, 0));


	private transient Vector3f tempOne = new Vector3f();
	private transient Vector3f tempTwo = new Vector3f();
	
	public void update(ParticleData p, float tpf) {
		if (enabled) {
			if (FastMath.rand.nextFloat() > 1-(chance+tpf)) {

				orbital.getValue3f(p.percentLife, p.randomValue, tempOne);
				linear.getValue3f(p.percentLife, p.randomValue, tempTwo);

				tempTwo.y += tempOne.x * Math.cos(8 * Math.PI * p.percentLife);
				tempTwo.z += tempOne.x * Math.sin(8 * Math.PI * p.percentLife);

				// Y
				tempTwo.x += tempOne.y * Math.cos(8 * Math.PI * p.percentLife);
				tempTwo.z += tempOne.y * Math.sin(8 * Math.PI * p.percentLife);

				// Z
				tempTwo.x += tempOne.z * Math.cos(8 * Math.PI * p.percentLife);
				tempTwo.y += tempOne.z * Math.sin(8 * Math.PI * p.percentLife);
				tempTwo.multLocal(strength);

				p.velocity.interpolateLocal(tempTwo, magnitude);
			}
		}
	}
	
	public void initialize(ParticleData p) {
		
	}
	
	public void reset(ParticleData p) {
		
	}

	/**
	 * The linear velocity we are pushing the particle towards
	 * @return
	 */
	public VectorValueType getLinear() {
		return linear;
	}

	public void setLinear(VectorValueType linear) {
		this.linear = linear;
	}

	/**
	 * The orbital velocity we are pushing the particle towards
	 * @return
	 */
	public VectorValueType getOrbital() {
		return orbital;
	}

	public void setOrbital(VectorValueType orbital) {
		this.orbital = orbital;
	}

	/**
	 * Sets the chance the influencer has of successfully affecting the particle's velocity vector
	 * @param chance float
	 */
	public void setChance(float chance) { this.chance = chance; }
	
	/**
	 * Returns the chance the influencer has of successfully affecting the particle's velocity vector
	 * @return float
	 */
	public float getChance() { return chance; }

	/**
	 * Sets the magnitued at which the impulse will effect the particle's velocity vector
	 * @param magnitude float
	 */
	public void setMagnitude(float magnitude) { this.magnitude = magnitude; }
	/**
	 * Returns  the magnitued at which the impulse will effect the particle's velocity vector
	 * @return float
	 */
	public float getMagnitude() { return magnitude; }
	/**
	 * Sets the strength of the full impulse
	 * @param strength float
	 */
	public void setStrength(float strength) { this.strength = strength; }
	/**
	 * Returns the strength of the full impulse
	 * @return float
	 */
	public float getStrength() { return strength; }
	
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(enabled, "enabled", true);
		oc.write(chance, "chance", 0.02f);
		oc.write(magnitude, "magnitude", 0.2f);
		oc.write(strength, "strength", 3f);
		oc.write(linear, "linear", new VectorValueType(new Vector3f()));
		oc.write(orbital, "orbital", new VectorValueType(new Vector3f()));
	}
	
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		enabled = ic.readBoolean("enabled", true);
		chance = ic.readFloat("chance", 0.02f);
		magnitude = ic.readFloat("magnitude", 0.2f);
		strength = ic.readFloat("strength", 3f);
		linear = (VectorValueType) ic.readSavable("linear", new VectorValueType(new Vector3f()));
		orbital = (VectorValueType) ic.readSavable("orbital", new VectorValueType(new Vector3f()));
  }
	
	@Override
	public ParticleInfluencer clone() {
		ImpulseInfluencer clone = (ImpulseInfluencer) super.clone();
		clone.setChance(chance);
		clone.setMagnitude(magnitude);
		clone.setStrength(strength);
		clone.linear = linear.clone();
		clone.orbital = orbital.clone();
		return clone;
	}

}
