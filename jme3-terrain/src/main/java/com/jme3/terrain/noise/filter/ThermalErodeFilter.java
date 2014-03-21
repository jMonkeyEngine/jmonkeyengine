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

public class ThermalErodeFilter extends AbstractFilter {

	private float talus;
	private float c;

	public ThermalErodeFilter setC(float c) {
		this.c = c;
		return this;
	}

	public ThermalErodeFilter setTalus(float talus) {
		this.talus = talus;
		return this;
	}

	@Override
	public int getMargin(int size, int margin) {
		return super.getMargin(size, margin) + 1;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int workSize) {
		float[] ga = buffer.array();
		float[] sa = new float[workSize * workSize];

		int[] idxrel = { -workSize - 1, -workSize + 1, workSize - 1, workSize + 1 };

		for (int y = 0; y < workSize; y++) {
			for (int x = 0; x < workSize; x++) {
				int idx = y * workSize + x;
				ga[idx] += sa[idx];
				sa[idx] = 0;

				float[] deltas = new float[idxrel.length];
				float deltaMax = this.talus;
				float deltaTotal = 0;

				for (int j = 0; j < idxrel.length; j++) {
					if (idx + idxrel[j] > 0 && idx + idxrel[j] < ga.length) {
						float dj = ga[idx] - ga[idx + idxrel[j]];
						if (dj > this.talus) {
							deltas[j] = dj;
							deltaTotal += dj;
							if (dj > deltaMax) {
								deltaMax = dj;
							}
						}
					}
				}

				for (int j = 0; j < idxrel.length; j++) {
					if (deltas[j] != 0) {
						float d = this.c * (deltaMax - this.talus) * deltas[j] / deltaTotal;
						if (d > ga[idx] + sa[idx]) {
							d = ga[idx] + sa[idx];
						}
						sa[idx] -= d;
						sa[idx + idxrel[j]] += d;
					}
					deltas[j] = 0;
				}
			}
		}

		return buffer;
	}

}
