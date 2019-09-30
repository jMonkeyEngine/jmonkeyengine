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
 * RotationLifetimeInfluencer
 * Handles the rotation over the lifetime of a particle
 *
 * @author t0neg0d
 * @author Jedic
 */
public class RotationLifetimeInfluencer extends ParticleInfluencer {

	private VectorValueType speedOverLifetime = new VectorValueType(new Vector3f());

  private transient Vector3f temp = new Vector3f();

	public void update(ParticleData p, float tpf) {
		if (enabled) {
			speedOverLifetime.getValue3f(p.percentLife, p.randomValue, temp);

			p.angles.x += temp.x*tpf;
			p.angles.y += temp.y*tpf;
			p.angles.z += temp.z*tpf;

		}
	}

	public void initialize(ParticleData p) {
	}

	public void reset(ParticleData p) {
		
	}

	public VectorValueType getSpeedOverLifetime() {
		return speedOverLifetime;
	}

	public void setSpeedOverLifetime(VectorValueType speedOverLifetime) {
		this.speedOverLifetime = speedOverLifetime;
	}


	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(speedOverLifetime, "speedoverlifetime", new VectorValueType(new Vector3f()));
	}

	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		speedOverLifetime = (VectorValueType) ic.readSavable("speedoverlifetime", new VectorValueType(new Vector3f()));
	}
	
	@Override
	public ParticleInfluencer clone() {
		RotationLifetimeInfluencer clone = (RotationLifetimeInfluencer) super.clone();
		clone.speedOverLifetime = speedOverLifetime.clone();
		return clone;
	}
}
