/**
 * Copyright (c) 2011, Novyon Events
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Anthyon
 */
package com.jme3.terrain.noise.basis;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.terrain.noise.Basis;
import com.jme3.terrain.noise.modulator.Modulator;
import com.jme3.terrain.noise.modulator.NoiseModulator;

/**
 * Utility base class for Noise implementations
 * 
 * @author Anthyon
 * 
 */
public abstract class Noise implements Basis {

	protected List<Modulator> modulators = new ArrayList<Modulator>();

	protected float scale = 1.0f;

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public FloatBuffer getBuffer(float sx, float sy, float base, int size) {
		FloatBuffer retval = FloatBuffer.allocate(size * size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				retval.put(this.modulate((sx + x) / size, (sy + y) / size, base));
			}
		}
		return retval;
	}

	public float modulate(float x, float y, float z) {
		float retval = this.value(x, y, z);
		for (Modulator m : this.modulators) {
			if (m instanceof NoiseModulator) {
				retval = m.value(retval);
			}
		}
		return retval;
	}

	@Override
	public Basis addModulator(Modulator modulator) {
		this.modulators.add(modulator);
		return this;
	}

	@Override
	public Basis setScale(float scale) {
		this.scale = scale;
		return this;
	}

	@Override
	public float getScale() {
		return this.scale;
	}
}
