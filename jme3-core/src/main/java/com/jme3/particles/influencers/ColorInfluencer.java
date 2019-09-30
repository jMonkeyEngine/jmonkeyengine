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
import com.jme3.math.ColorRGBA;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.ColorValueType;
import com.jme3.particles.valuetypes.Gradient;

import java.io.IOException;

/**
 * Color Module
 *
 * The color module allows you to change the particle's color over time
 *
 * @author t0neg0d
 * @author Jedic
 */
public class ColorInfluencer extends ParticleInfluencer {

	private ColorValueType colorOverTime = new ColorValueType(ColorRGBA.White.clone());

	public ColorInfluencer() {
	}
	
  @Override
	public void update(ParticleData p, float tpf) {
		if (enabled) {
			colorOverTime.getValueColor(p.percentLife, p.randomValue, p.color);

			// multiply by start color
			p.color.r *= p.startColor.r;
			p.color.g *= p.startColor.g;
			p.color.b *= p.startColor.b;
			p.color.a *= p.startColor.a;
		}
	}

	@Override
	public void initialize(ParticleData p) {
		colorOverTime.getValueColor(0.0f, p.randomValue, p.color);


		// multiply by start color
		p.color.r *= p.startColor.r;
		p.color.g *= p.startColor.g;
		p.color.b *= p.startColor.b;
		p.color.a *= p.startColor.a;
	}

	@Override
	public void reset(ParticleData p) {
		p.color.set(0.0f, 0.0f, 0.0f, 0.0f);
	}

	/**
	 * This is a convenience method to quickly set a start and end color for a particle over time
	 * @param start - the start color of the particle
	 * @param end - the end color of the particle
	 */
	public void setStartEndColor(ColorRGBA start, ColorRGBA end) {
		colorOverTime.setGradient(new Gradient().addGradPoint(start, 0.0f)
				                                 .addGradPoint(end, 1.0f));
	}

	public ColorValueType getColorOverTime() {
		return colorOverTime;
	}

	public void setColorOverTime(ColorValueType colorOverTime) {
		this.colorOverTime = colorOverTime;
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(colorOverTime, "colorvalue", colorOverTime);
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		colorOverTime = (ColorValueType) ic.readSavable("colorvalue", new ColorValueType(ColorRGBA.Red.clone()));
	}

	@Override
	public ParticleInfluencer clone() {
		ColorInfluencer clone = (ColorInfluencer) super.clone();
		clone.colorOverTime = colorOverTime.clone();
		return clone;
	}

}
