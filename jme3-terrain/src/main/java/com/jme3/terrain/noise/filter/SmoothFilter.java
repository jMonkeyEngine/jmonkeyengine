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

public class SmoothFilter extends AbstractFilter {

	private int radius;
	private float effect;

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getRadius() {
		return this.radius;
	}

	public void setEffect(float effect) {
		this.effect = effect;
	}

	public float getEffect() {
		return this.effect;
	}

	@Override
	public int getMargin(int size, int margin) {
		return super.getMargin(size, margin) + this.radius;
	}

	@Override
	public FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int size) {
		float[] data = buffer.array();
		float[] retval = new float[data.length];

		for (int y = this.radius; y < size - this.radius; y++) {
			for (int x = this.radius; x < size - this.radius; x++) {
				int idx = y * size + x;
				float n = 0;
				for (int i = -this.radius; i < this.radius + 1; i++) {
					for (int j = -this.radius; j < this.radius + 1; j++) {
						n += data[(y + i) * size + x + j];
					}
				}
				retval[idx] = this.effect * n / (4 * this.radius * (this.radius + 1) + 1) + (1 - this.effect) * data[idx];
			}
		}

		return FloatBuffer.wrap(retval);
	}
}
