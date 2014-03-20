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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helper class containing useful functions explained in the book:
 * Texturing & Modeling - A Procedural Approach
 * 
 * @author Anthyon
 * 
 */
public class ShaderUtils {

	public static final float[] i2c(final int color) {
		return new float[] { (color & 0x00ff0000) / 256f, (color & 0x0000ff00) / 256f, (color & 0x000000ff) / 256f,
				(color & 0xff000000) / 256f };
	}

	public static final int c2i(final float[] color) {
		return (color.length == 4 ? (int) (color[3] * 256) : 0xff000000) | ((int) (color[0] * 256) << 16) | ((int) (color[1] * 256) << 8)
				| (int) (color[2] * 256);
	}

	public static final float mix(final float a, final float b, final float f) {
		return (1 - f) * a + f * b;
	}

	public static final Color mix(final Color a, final Color b, final float f) {
		return new Color((int) ShaderUtils.clamp(ShaderUtils.mix(a.getRed(), b.getRed(), f), 0, 255), (int) ShaderUtils.clamp(
				ShaderUtils.mix(a.getGreen(), b.getGreen(), f), 0, 255), (int) ShaderUtils.clamp(
				ShaderUtils.mix(a.getBlue(), b.getBlue(), f), 0, 255));
	}

	public static final int mix(final int a, final int b, final float f) {
		return (int) ((1 - f) * a + f * b);
	}

	public static final float[] mix(final float[] c1, final float[] c2, final float f) {
		return new float[] { ShaderUtils.mix(c1[0], c2[0], f), ShaderUtils.mix(c1[1], c2[1], f), ShaderUtils.mix(c1[2], c2[2], f) };
	}

	public static final float step(final float a, final float x) {
		return x < a ? 0 : 1;
	}

	public static final float boxstep(final float a, final float b, final float x) {
		return ShaderUtils.clamp((x - a) / (b - a), 0, 1);
	}

	public static final float pulse(final float a, final float b, final float x) {
		return ShaderUtils.step(a, x) - ShaderUtils.step(b, x);
	}

	public static final float clamp(final float x, final float a, final float b) {
		return x < a ? a : x > b ? b : x;
	}

	public static final float min(final float a, final float b) {
		return a < b ? a : b;
	}

	public static final float max(final float a, final float b) {
		return a > b ? a : b;
	}

	public static final float abs(final float x) {
		return x < 0 ? -x : x;
	}

	public static final float smoothstep(final float a, final float b, final float x) {
		if (x < a) {
			return 0;
		} else if (x > b) {
			return 1;
		}
		float xx = (x - a) / (b - a);
		return xx * xx * (3 - 2 * xx);
	}

	public static final float mod(final float a, final float b) {
		int n = (int) (a / b);
		float aa = a - n * b;
		if (aa < 0) {
			aa += b;
		}
		return aa;
	}

	public static final int floor(final float x) {
		return x > 0 ? (int) x : (int) x - 1;
	}

	public static final float ceil(final float x) {
		return (int) x + (x > 0 && x != (int) x ? 1 : 0);
	}

	public static final float spline(float x, final float[] knot) {
		float CR00 = -0.5f;
		float CR01 = 1.5f;
		float CR02 = -1.5f;
		float CR03 = 0.5f;
		float CR10 = 1.0f;
		float CR11 = -2.5f;
		float CR12 = 2.0f;
		float CR13 = -0.5f;
		float CR20 = -0.5f;
		float CR21 = 0.0f;
		float CR22 = 0.5f;
		float CR23 = 0.0f;
		float CR30 = 0.0f;
		float CR31 = 1.0f;
		float CR32 = 0.0f;
		float CR33 = 0.0f;

		int span;
		int nspans = knot.length - 3;
		float c0, c1, c2, c3; /* coefficients of the cubic. */
		if (nspans < 1) {/* illegal */
			throw new RuntimeException("Spline has too few knots.");
		}
		/* Find the appropriate 4-point span of the spline. */
		x = ShaderUtils.clamp(x, 0, 1) * nspans;
		span = (int) x;
		if (span >= knot.length - 3) {
			span = knot.length - 3;
		}
		x -= span;
		/* Evaluate the span cubic at x using Horner’s rule. */
		c3 = CR00 * knot[span + 0] + CR01 * knot[span + 1] + CR02 * knot[span + 2] + CR03 * knot[span + 3];
		c2 = CR10 * knot[span + 0] + CR11 * knot[span + 1] + CR12 * knot[span + 2] + CR13 * knot[span + 3];
		c1 = CR20 * knot[span + 0] + CR21 * knot[span + 1] + CR22 * knot[span + 2] + CR23 * knot[span + 3];
		c0 = CR30 * knot[span + 0] + CR31 * knot[span + 1] + CR32 * knot[span + 2] + CR33 * knot[span + 3];
		return ((c3 * x + c2) * x + c1) * x + c0;
	}

	public static final float[] spline(final float x, final float[][] knots) {
		float[] retval = new float[knots.length];
		for (int i = 0; i < knots.length; i++) {
			retval[i] = ShaderUtils.spline(x, knots[i]);
		}
		return retval;
	}

	public static final float gammaCorrection(final float gamma, final float x) {
		return (float) Math.pow(x, 1 / gamma);
	}

	public static final float bias(final float b, final float x) {
		return (float) Math.pow(x, Math.log(b) / Math.log(0.5));
	}

	public static final float gain(final float g, final float x) {
		return x < 0.5 ? ShaderUtils.bias(1 - g, 2 * x) / 2 : 1 - ShaderUtils.bias(1 - g, 2 - 2 * x) / 2;
	}

	public static final float sinValue(final float s, final float minFreq, final float maxFreq, final float swidth) {
		float value = 0;
		float cutoff = ShaderUtils.clamp(0.5f / swidth, 0, maxFreq);
		float f;
		for (f = minFreq; f < 0.5 * cutoff; f *= 2) {
			value += Math.sin(2 * Math.PI * f * s) / f;
		}
		float fade = ShaderUtils.clamp(2 * (cutoff - f) / cutoff, 0, 1);
		value += fade * Math.sin(2 * Math.PI * f * s) / f;
		return value;
	}

	public static final float length(final float x, final float y, final float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public static final float[] rotate(final float[] v, final float[][] m) {
		float x = v[0] * m[0][0] + v[1] * m[0][1] + v[2] * m[0][2];
		float y = v[0] * m[1][0] + v[1] * m[1][1] + v[2] * m[1][2];
		float z = v[0] * m[2][0] + v[1] * m[2][1] + v[2] * m[2][2];
		return new float[] { x, y, z };
	}

	public static final float[][] calcRotationMatrix(final float ax, final float ay, final float az) {
		float[][] retval = new float[3][3];
		float cax = (float) Math.cos(ax);
		float sax = (float) Math.sin(ax);
		float cay = (float) Math.cos(ay);
		float say = (float) Math.sin(ay);
		float caz = (float) Math.cos(az);
		float saz = (float) Math.sin(az);

		retval[0][0] = cay * caz;
		retval[0][1] = -cay * saz;
		retval[0][2] = say;
		retval[1][0] = sax * say * caz + cax * saz;
		retval[1][1] = -sax * say * saz + cax * caz;
		retval[1][2] = -sax * cay;
		retval[2][0] = -cax * say * caz + sax * saz;
		retval[2][1] = cax * say * saz + sax * caz;
		retval[2][2] = cax * cay;

		return retval;
	}

	public static final float[] normalize(final float[] v) {
		float l = ShaderUtils.length(v);
		float[] r = new float[v.length];
		int i = 0;
		for (float vv : v) {
			r[i++] = vv / l;
		}
		return r;
	}

	public static final float length(final float[] v) {
		float s = 0;
		for (float vv : v) {
			s += vv * vv;
		}
		return (float) Math.sqrt(s);
	}

	public static final ByteBuffer getImageDataFromImage(BufferedImage bufferedImage) {
		WritableRaster wr;
		DataBuffer db;

		BufferedImage bi = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.drawImage(bufferedImage, null, null);
		bufferedImage = bi;
		wr = bi.getRaster();
		db = wr.getDataBuffer();

		DataBufferInt dbi = (DataBufferInt) db;
		int[] data = dbi.getData();

		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.asIntBuffer().put(data);
		byteBuffer.flip();

		return byteBuffer;
	}

	public static float frac(float f) {
		return f - ShaderUtils.floor(f);
	}

	public static float[] floor(float[] fs) {
		float[] retval = new float[fs.length];
		for (int i = 0; i < fs.length; i++) {
			retval[i] = ShaderUtils.floor(fs[i]);
		}
		return retval;
	}
}
