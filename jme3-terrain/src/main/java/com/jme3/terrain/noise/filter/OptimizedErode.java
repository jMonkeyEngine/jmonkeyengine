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

public class OptimizedErode extends AbstractFilter {

	private float talus;
	private int radius;

	public OptimizedErode setRadius(int radius) {
		this.radius = radius;
		return this;
	}

	public int getRadius() {
		return this.radius;
	}

	public OptimizedErode setTalus(float talus) {
		this.talus = talus;
		return this;
	}

	public float getTalus() {
		return this.talus;
	}

	@Override
	public int getMargin(int size, int margin) {
		return super.getMargin(size, margin) + this.radius;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int size) {
		float[] tmp = buffer.array();
		float[] retval = new float[tmp.length];

		for (int y = this.radius + 1; y < size - this.radius; y++) {
			for (int x = this.radius + 1; x < size - this.radius; x++) {
				int idx = y * size + x;
				float h = tmp[idx];

				float horizAvg = 0;
				int horizCount = 0;
				float vertAvg = 0;
				int vertCount = 0;

				boolean horizT = false;
				boolean vertT = false;

				for (int i = 0; i >= -this.radius; i--) {
					int idxV = (y + i) * size + x;
					int idxVL = (y + i - 1) * size + x;
					int idxH = y * size + x + i;
					int idxHL = y * size + x + i - 1;
					float hV = tmp[idxV];
					float hH = tmp[idxH];

					if (Math.abs(h - hV) > this.talus && Math.abs(h - tmp[idxVL]) > this.talus || vertT) {
						vertT = true;
					} else {
						if (Math.abs(h - hV) <= this.talus) {
							vertAvg += hV;
							vertCount++;
						}
					}

					if (Math.abs(h - hH) > this.talus && Math.abs(h - tmp[idxHL]) > this.talus || horizT) {
						horizT = true;
					} else {
						if (Math.abs(h - hH) <= this.talus) {
							horizAvg += hH;
							horizCount++;
						}
					}
				}

				retval[idx] = 0.5f * (vertAvg / (vertCount > 0 ? vertCount : 1) + horizAvg / (horizCount > 0 ? horizCount : 1));
			}
		}
		return FloatBuffer.wrap(retval);
	}

}
