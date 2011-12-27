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
import com.jme3.terrain.noise.filter.AbstractFilter;
import com.jme3.terrain.noise.modulator.Modulator;

public class FilteredBasis extends AbstractFilter implements Basis {

	private Basis basis;
	private List<Modulator> modulators = new ArrayList<Modulator>();
	private float scale;

	public FilteredBasis() {}

	public FilteredBasis(Basis basis) {
		this.basis = basis;
	}

	public Basis getBasis() {
		return this.basis;
	}

	public void setBasis(Basis basis) {
		this.basis = basis;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer data, int size) {
		return data;
	}

	@Override
	public void init() {
		this.basis.init();
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

	@Override
	public Basis addModulator(Modulator modulator) {
		this.modulators.add(modulator);
		return this;
	}

	@Override
	public float value(float x, float y, float z) {
		throw new UnsupportedOperationException(
				"Method value cannot be called on FilteredBasis and its descendants. Use getBuffer instead!");
	}

	@Override
	public FloatBuffer getBuffer(float sx, float sy, float base, int size) {
		int margin = this.getMargin(size, 0);
		int workSize = size + 2 * margin;
		FloatBuffer retval = this.basis.getBuffer(sx - margin, sy - margin, base, workSize);
		return this.clip(this.doFilter(sx, sy, base, retval, workSize), workSize, size, margin);
	}

	public FloatBuffer clip(FloatBuffer buf, int origSize, int newSize, int offset) {
		FloatBuffer result = FloatBuffer.allocate(newSize * newSize);

		float[] orig = buf.array();
		for (int i = offset; i < offset + newSize; i++) {
			result.put(orig, i * origSize + offset, newSize);
		}

		return result;
	}
}
