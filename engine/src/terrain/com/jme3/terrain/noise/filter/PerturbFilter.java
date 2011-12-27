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
package com.jme3.terrain.noise.filter;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.fractal.FractalSum;

public class PerturbFilter extends AbstractFilter {

	private float magnitude;

	@Override
	public int getMargin(int size, int margin) {
		margin = super.getMargin(size, margin);
		return (int) Math.floor(this.magnitude * (margin + size) + margin);
	}

	public void setMagnitude(float magnitude) {
		this.magnitude = magnitude;
	}

	public float getMagnitude() {
		return this.magnitude;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer data, int workSize) {
		float[] arr = data.array();
		int origSize = (int) Math.ceil(workSize / (2 * this.magnitude + 1));
		int offset = (workSize - origSize) / 2;
		Logger.getLogger(PerturbFilter.class.getCanonicalName()).info(
				"Found origSize : " + origSize + " and offset: " + offset + " for workSize : " + workSize + " and magnitude : "
						+ this.magnitude);
		float[] retval = new float[workSize * workSize];
		float[] perturbx = new FractalSum().setOctaves(8).setScale(5f).getBuffer(sx, sy, base, workSize).array();
		float[] perturby = new FractalSum().setOctaves(8).setScale(5f).getBuffer(sx, sy, base + 1, workSize).array();
		for (int y = 0; y < workSize; y++) {
			for (int x = 0; x < workSize; x++) {
				// Perturb our coordinates
				float noisex = perturbx[y * workSize + x];
				float noisey = perturby[y * workSize + x];

				int px = (int) (origSize * noisex * this.magnitude);
				int py = (int) (origSize * noisey * this.magnitude);

				float c00 = arr[this.wrap(y - py, workSize) * workSize + this.wrap(x - px, workSize)];
				float c01 = arr[this.wrap(y - py, workSize) * workSize + this.wrap(x + px, workSize)];
				float c10 = arr[this.wrap(y + py, workSize) * workSize + this.wrap(x - px, workSize)];
				float c11 = arr[this.wrap(y + py, workSize) * workSize + this.wrap(x + px, workSize)];

				float c0 = ShaderUtils.mix(c00, c01, noisex);
				float c1 = ShaderUtils.mix(c10, c11, noisex);
				retval[y * workSize + x] = ShaderUtils.mix(c0, c1, noisey);
			}
		}
		return FloatBuffer.wrap(retval);
	}

	private int wrap(int v, int size) {
		if (v < 0) {
			return v + size - 1;
		} else if (v >= size) {
			return v - size;
		} else {
			return v;
		}
	}
}
