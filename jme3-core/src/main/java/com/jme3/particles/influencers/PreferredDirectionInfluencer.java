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
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * PreferredDirectionInfluencer
 * Attempts to change the particles direction over time
 *
 * @author t0neg0d
 * @author Jedic
 */
public class PreferredDirectionInfluencer extends ParticleInfluencer {

	private VectorValueType preferredDirection = new VectorValueType(new Vector3f());
	private ValueType weight = new ValueType(0.025f);

	// temp variables
	private Vector3f temp = new Vector3f();

	public void update(ParticleData p, float tpf) {
		
	}

	public void initialize(ParticleData p) {
		if (enabled) {
			float currWeight = weight.getValue(p.percentLife, p.randomValue);
			preferredDirection.getValue3f(p.percentLife, p.randomValue, temp);
			p.velocity.interpolateLocal(temp, currWeight);
		}
	}

	public void reset(ParticleData p) {
		
	}

	public VectorValueType getPreferredDirection() {
		return preferredDirection;
	}

	public void setPreferredDirection(VectorValueType preferredDirection) {
		this.preferredDirection = preferredDirection;
	}

	public ValueType getWeight() {
		return weight;
	}

	public void setWeight(ValueType weight) {
		this.weight = weight;
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(preferredDirection, "preferredDirection", new VectorValueType());
		oc.write(weight, "weight", new ValueType(0.025f));
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		preferredDirection = (VectorValueType) ic.readSavable("preferredDirection", new VectorValueType());
		weight = (ValueType) ic.readSavable("weight", new ValueType(0.025f));
	}
	
	@Override
	public ParticleInfluencer clone() {
		PreferredDirectionInfluencer clone = (PreferredDirectionInfluencer) super.clone();
		clone.preferredDirection = preferredDirection.clone();
		clone.weight = weight.clone();
		return clone;
	}

}
