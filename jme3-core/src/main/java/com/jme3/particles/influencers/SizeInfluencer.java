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
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.Curve;
import com.jme3.particles.valuetypes.ValueType;

import java.io.IOException;

/**
 * Size Module
 * The size module controls the particle size over time
 *
 * @author t0neg0d
 * @author Jedic
 */
public class SizeInfluencer extends ParticleInfluencer {

	private ValueType sizeOverTime = new ValueType(1.0f);

	@Override
	public void update(ParticleData p, float tpf) {
		if (enabled) {
			p.size = p.startSize * sizeOverTime.getValue(p.percentLife, p.randomValue);
		}
	}

	@Override
	public void initialize(ParticleData p) {
		p.size = p.startSize * sizeOverTime.getValue(0, p.randomValue);
	}

	@Override
	public void reset(ParticleData p){
    p.size = p.startSize * sizeOverTime.getValue(0, p.randomValue);
	}

	public ValueType getSizeOverTime() {
		return sizeOverTime;
	}

	public void setSizeOverTime(ValueType sizeOverTime) {
		this.sizeOverTime = sizeOverTime;
	}
	
	@Override
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(sizeOverTime, "sizeovertime", new ValueType(1.0f));
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		sizeOverTime = (ValueType)ic.readSavable("sizeovertime", new ValueType(1.0f));
	}
	
	@Override
	public ParticleInfluencer clone() {
		SizeInfluencer clone = (SizeInfluencer) super.clone();
		clone.sizeOverTime = sizeOverTime.clone();
		return clone;
	}
}
