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
package com.jme3.terrain.noise;

/**
 * Helper class for working with colors and gradients
 * 
 * @author Anthyon
 * 
 */
public class Color {

	private final float[] rgba = new float[4];

	public Color() {}

	public Color(final int r, final int g, final int b) {
		this(r, g, b, 255);
	}

	public Color(final int r, final int g, final int b, final int a) {
		this.rgba[0] = (r & 255) / 256f;
		this.rgba[1] = (g & 255) / 256f;
		this.rgba[2] = (b & 255) / 256f;
		this.rgba[3] = (a & 255) / 256f;
	}

	public Color(final float r, final float g, final float b) {
		this(r, g, b, 1);
	}

	public Color(final float r, final float g, final float b, final float a) {
		this.rgba[0] = ShaderUtils.clamp(r, 0, 1);
		this.rgba[1] = ShaderUtils.clamp(g, 0, 1);
		this.rgba[2] = ShaderUtils.clamp(b, 0, 1);
		this.rgba[3] = ShaderUtils.clamp(a, 0, 1);
	}

	public Color(final int h, final float s, final float b) {
		this(h, s, b, 1);
	}

	public Color(final int h, final float s, final float b, final float a) {
		this.rgba[3] = a;
		if (s == 0) {
			// achromatic ( grey )
			this.rgba[0] = b;
			this.rgba[1] = b;
			this.rgba[2] = b;
			return;
		}

		float hh = h / 60.0f;
		int i = ShaderUtils.floor(hh);
		float f = hh - i;
		float p = b * (1 - s);
		float q = b * (1 - s * f);
		float t = b * (1 - s * (1 - f));

		if (i == 0) {
			this.rgba[0] = b;
			this.rgba[1] = t;
			this.rgba[2] = p;
		} else if (i == 1) {
			this.rgba[0] = q;
			this.rgba[1] = b;
			this.rgba[2] = p;
		} else if (i == 2) {
			this.rgba[0] = p;
			this.rgba[1] = b;
			this.rgba[2] = t;
		} else if (i == 3) {
			this.rgba[0] = p;
			this.rgba[1] = q;
			this.rgba[2] = b;
		} else if (i == 4) {
			this.rgba[0] = t;
			this.rgba[1] = p;
			this.rgba[2] = b;
		} else {
			this.rgba[0] = b;
			this.rgba[1] = p;
			this.rgba[2] = q;
		}
	}

	public int toInteger() {
		return 0x00000000 | (int) (this.rgba[3] * 256) << 24 | (int) (this.rgba[0] * 256) << 16 | (int) (this.rgba[1] * 256) << 8
				| (int) (this.rgba[2] * 256);
	}

	public String toWeb() {
		return Integer.toHexString(this.toInteger());
	}

	public Color toGrayscale() {
		float v = (this.rgba[0] + this.rgba[1] + this.rgba[2]) / 3f;
		return new Color(v, v, v, this.rgba[3]);
	}

	public Color toSepia() {
		float r = ShaderUtils.clamp(this.rgba[0] * 0.393f + this.rgba[1] * 0.769f + this.rgba[2] * 0.189f, 0, 1);
		float g = ShaderUtils.clamp(this.rgba[0] * 0.349f + this.rgba[1] * 0.686f + this.rgba[2] * 0.168f, 0, 1);
		float b = ShaderUtils.clamp(this.rgba[0] * 0.272f + this.rgba[1] * 0.534f + this.rgba[2] * 0.131f, 0, 1);
		return new Color(r, g, b, this.rgba[3]);
	}
}
