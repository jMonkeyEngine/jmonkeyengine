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
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * PreferredDestinationInfluencer
 * This influencer attempts to move a particle towards a given destination
 *
 * @author t0neg0d
 * @author Jedic
 */
public class PreferredDestinationInfluencer extends ParticleInfluencer {

	private VectorValueType preferredDestination = new VectorValueType();
	private ValueType weight = new ValueType(.085f);
	private ValueType chance = new ValueType(.0985f);

	private boolean worldPosition = true;

	// temp variables
	private Vector3f temp = new Vector3f();

	public void update(ParticleData p, float tpf) {
		if (enabled) {
			float currChance = chance.getValue(p.percentLife, p.randomValue);
			if (FastMath.rand.nextFloat() < currChance) {
				float currWeight = weight.getValue(p.percentLife, p.randomValue);
				preferredDestination.getValue3f(p.percentLife, p.randomValue, temp);
				temp.subtractLocal(p.position);

				p.velocity.interpolateLocal(temp, currWeight);
			}
		}
	}

	public void initialize(ParticleData p) {
		
	}

	public void reset(ParticleData p) {
		
	}

	public VectorValueType getPreferredDestination() {
		return preferredDestination;
	}

	public void setPreferredDestination(VectorValueType preferredDestination) {
		this.preferredDestination = preferredDestination;
	}

	public ValueType getWeight() {
		return weight;
	}

	public void setWeight(ValueType weight) {
		this.weight = weight;
	}

	public ValueType getChance() {
		return chance;
	}

	public void setChance(ValueType chance) {
		this.chance = chance;
	}

	public boolean isWorldPosition() {
		return worldPosition;
	}

	public void setWorldPosition(boolean worldPosition) {
		this.worldPosition = worldPosition;
	}

	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(enabled, "enabled", true);
		oc.write(preferredDestination, "preferredDestination", new VectorValueType());
		oc.write(chance, "chance", new ValueType(.085f));
		oc.write(weight, "weight", new ValueType(.0985f));
	}

	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		preferredDestination = (VectorValueType) ic.readSavable("preferredDestination", new VectorValueType());
		chance = (ValueType) ic.readSavable("chance", new ValueType(.085f));
		weight = (ValueType) ic.readSavable("weight", new ValueType(.0985f));
	}
	
	@Override
	public ParticleInfluencer clone() {
		PreferredDestinationInfluencer clone = (PreferredDestinationInfluencer) super.clone();
		clone.preferredDestination = preferredDestination.clone();
		clone.chance = chance.clone();
		clone.weight = weight.clone();
		return clone;
	}

}
