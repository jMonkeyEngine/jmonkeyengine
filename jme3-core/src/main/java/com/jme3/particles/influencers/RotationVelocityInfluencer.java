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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * RotationVelocityInfluencer
 * Handles the rotation of a particle based on the current velocity
 *
 * @author Jedic
 */
public class RotationVelocityInfluencer extends ParticleInfluencer {

	private Vector2f velocityRange = new Vector2f(0, 1.0f);
	private VectorValueType speedOverVelocity = new VectorValueType(new Vector3f());

  private transient Vector3f temp = new Vector3f();

	public void update(ParticleData p, float tpf) {
		if (enabled) {

			float velocity = p.velocity.length();
			if (velocity < velocityRange.x) velocity = velocityRange.x;
			if (velocity > velocityRange.y) velocity = velocityRange.y;

			velocity /= velocityRange.y;

			speedOverVelocity.getValue3f(velocity, p.randomValue, temp);


			p.angles.x += temp.x*tpf;
			p.angles.y += temp.y*tpf;
			p.angles.z += temp.z*tpf;

		}
	}

	public void initialize(ParticleData p) {
	}

	public void reset(ParticleData p) {
		
	}

	public VectorValueType getSpeedOverVelocity() {
		return speedOverVelocity;
	}

	public void setSpeedOverVelocity(VectorValueType speedOverVelocity) {
		this.speedOverVelocity = speedOverVelocity;
	}

	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(velocityRange, "velocityrange", new Vector2f(0, 1));
		oc.write(speedOverVelocity, "speedovervelocity", new VectorValueType(new Vector3f()));
	}

	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		velocityRange = (Vector2f) ic.readSavable("velocityrange", new Vector2f(0, 1));
		speedOverVelocity = (VectorValueType) ic.readSavable("speedovervelocity", new VectorValueType(new Vector3f()));
	}
	
	@Override
	public ParticleInfluencer clone() {
		RotationVelocityInfluencer clone = (RotationVelocityInfluencer) super.clone();
		clone.speedOverVelocity = speedOverVelocity.clone();
		clone.velocityRange = velocityRange.clone();
		return clone;
	}
}
