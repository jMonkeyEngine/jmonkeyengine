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

import com.jme3.terrain.noise.Basis;

public class HydraulicErodeFilter extends AbstractFilter {

	private Basis waterMap;
	private Basis sedimentMap;
	private float Kr;
	private float Ks;
	private float Ke;
	private float Kc;
	private float T;

	public void setKc(float kc) {
		this.Kc = kc;
	}

	public void setKe(float ke) {
		this.Ke = ke;
	}

	public void setKr(float kr) {
		this.Kr = kr;
	}

	public void setKs(float ks) {
		this.Ks = ks;
	}

	public void setSedimentMap(Basis sedimentMap) {
		this.sedimentMap = sedimentMap;
	}

	public void setT(float t) {
		this.T = t;
	}

	public void setWaterMap(Basis waterMap) {
		this.waterMap = waterMap;
	}

	@Override
	public int getMargin(int size, int margin) {
		return super.getMargin(size, margin) + 1;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int workSize) {
		float[] ga = buffer.array();
		// float[] wa = this.waterMap.getBuffer(sx, sy, base, workSize).array();
		// float[] sa = this.sedimentMap.getBuffer(sx, sy, base,
		// workSize).array();
		float[] wt = new float[workSize * workSize];
		float[] st = new float[workSize * workSize];

		int[] idxrel = { -workSize - 1, -workSize + 1, workSize - 1, workSize + 1 };

		// step 1. water arrives and step 2. captures material
		for (int y = 0; y < workSize; y++) {
			for (int x = 0; x < workSize; x++) {
				int idx = y * workSize + x;
				float wtemp = this.Kr; // * wa[idx];
				float stemp = this.Ks; // * sa[idx];
				if (wtemp > 0) {
					wt[idx] += wtemp;
					if (stemp > 0) {
						ga[idx] -= stemp * wt[idx];
						st[idx] += stemp * wt[idx];
					}
				}

				// step 3. water is transported to it's neighbours
				float a = ga[idx] + wt[idx];
				// float[] aj = new float[idxrel.length];
				float amax = 0;
				int amaxidx = -1;
				float ac = 0;
				float dtotal = 0;

				for (int j = 0; j < idxrel.length; j++) {
					if (idx + idxrel[j] > 0 && idx + idxrel[j] < workSize) {
						float at = ga[idx + idxrel[j]] + wt[idx + idxrel[j]];
						if (a - at > a - amax) {
							dtotal += at;
							amax = at;
							amaxidx = j;
							ac++;
						}
					}
				}

				float aa = (dtotal + a) / (ac + 1);
				// for (int j = 0; j < idxrel.length; j++) {
				// if (idx + idxrel[j] > 0 && idx + idxrel[j] < workSize && a -
				// aj[j] > 0) {
				if (amaxidx > -1) {
					float dwj = Math.min(wt[idx], a - aa) * (a - amax) / dtotal;
					float dsj = st[idx] * dwj / wt[idx];
					wt[idx] -= dwj;
					st[idx] -= dsj;
					wt[idx + idxrel[amaxidx]] += dwj;
					st[idx + idxrel[amaxidx]] += dsj;
				}
				// }

				// step 4. water evaporates and deposits material
				wt[idx] = wt[idx] * (1 - this.Ke);
				if (wt[idx] < this.T) {
					wt[idx] = 0;
				}
				float smax = this.Kc * wt[idx];
				if (st[idx] > smax) {
					ga[idx] += st[idx] - smax;
					st[idx] -= st[idx] - smax;
				}
			}
		}

		return buffer;
	}

}
