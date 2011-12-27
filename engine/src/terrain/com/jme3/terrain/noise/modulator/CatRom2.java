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
package com.jme3.terrain.noise.modulator;

import java.util.HashMap;
import java.util.Map;

import com.jme3.terrain.noise.ShaderUtils;

public class CatRom2 implements Modulator {

	private int sampleRate = 100;

	private final float[] table;

	private static Map<Integer, CatRom2> instances = new HashMap<Integer, CatRom2>();

	public CatRom2(final int sampleRate) {
		this.sampleRate = sampleRate;
		this.table = new float[4 * sampleRate + 1];
		for (int i = 0; i < 4 * sampleRate + 1; i++) {
			float x = i / (float) sampleRate;
			x = (float) Math.sqrt(x);
			if (x < 1) {
				this.table[i] = 0.5f * (2 + x * x * (-5 + x * 3));
			} else {
				this.table[i] = 0.5f * (4 + x * (-8 + x * (5 - x)));
			}
		}
	}

	public static CatRom2 getInstance(final int sampleRate) {
		if (!CatRom2.instances.containsKey(sampleRate)) {
			CatRom2.instances.put(sampleRate, new CatRom2(sampleRate));
		}
		return CatRom2.instances.get(sampleRate);
	}

	@Override
	public float value(final float... in) {
		if (in[0] >= 4) {
			return 0;
		}
		in[0] = in[0] * this.sampleRate + 0.5f;
		int i = ShaderUtils.floor(in[0]);
		if (i >= 4 * this.sampleRate + 1) {
			return 0;
		}
		return this.table[i];
	}
}
