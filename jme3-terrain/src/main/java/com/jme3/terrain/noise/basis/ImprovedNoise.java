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

import com.jme3.terrain.noise.ShaderUtils;

// JAVA REFERENCE IMPLEMENTATION OF IMPROVED NOISE - COPYRIGHT 2002 KEN PERLIN.
/**
 * Perlin's default implementation of Improved Perlin Noise
 * designed to work with Noise base
 */
public final class ImprovedNoise extends Noise {

	@Override
	public void init() {

	}

	static public float noise(float x, float y, float z) {
		int X = ShaderUtils.floor(x), // FIND UNIT CUBE THAT
		Y = ShaderUtils.floor(y), // CONTAINS POINT.
		Z = ShaderUtils.floor(z);
		x -= X; // FIND RELATIVE X,Y,Z
		y -= Y; // OF POINT IN CUBE.
		z -= Z;
		X = X & 255;
		Y = Y & 255;
		Z = Z & 255;
		float u = ImprovedNoise.fade(x), // COMPUTE FADE CURVES
		v = ImprovedNoise.fade(y), // FOR EACH OF X,Y,Z.
		w = ImprovedNoise.fade(z);
		int A = ImprovedNoise.p[X] + Y;
		int AA = ImprovedNoise.p[A] + Z;
		int AB = ImprovedNoise.p[A + 1] + Z;
		int B = ImprovedNoise.p[X + 1] + Y;
		int BA = ImprovedNoise.p[B] + Z;
		int BB = ImprovedNoise.p[B + 1] + Z;

		return ImprovedNoise.lerp(
				w,
				ImprovedNoise.lerp(
						v,
						ImprovedNoise.lerp(u, ImprovedNoise.grad3(ImprovedNoise.p[AA], x, y, z),
								ImprovedNoise.grad3(ImprovedNoise.p[BA], x - 1, y, z)), // BLENDED
						ImprovedNoise.lerp(u, ImprovedNoise.grad3(ImprovedNoise.p[AB], x, y - 1, z), // RESULTS
								ImprovedNoise.grad3(ImprovedNoise.p[BB], x - 1, y - 1, z))),// FROM
				ImprovedNoise.lerp(v,
						ImprovedNoise.lerp(u, ImprovedNoise.grad3(ImprovedNoise.p[AA + 1], x, y, z - 1), // CORNERS
								ImprovedNoise.grad3(ImprovedNoise.p[BA + 1], x - 1, y, z - 1)), // OF
						ImprovedNoise.lerp(u, ImprovedNoise.grad3(ImprovedNoise.p[AB + 1], x, y - 1, z - 1),
								ImprovedNoise.grad3(ImprovedNoise.p[BB + 1], x - 1, y - 1, z - 1))));
	}

	static final float fade(final float t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	static final float lerp(final float t, final float a, final float b) {
		return a + t * (b - a);
	}

	static float grad(final int hash, final float x, final float y, final float z) {
		int h = hash & 15; // CONVERT LO 4 BITS OF HASH CODE
		float u = h < 8 ? x : y, // INTO 12 GRADIENT DIRECTIONS.
		v = h < 4 ? y : h == 12 || h == 14 ? x : z;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	static final float grad3(final int hash, final float x, final float y, final float z) {
		int h = hash & 15; // CONVERT LO 4 BITS OF HASH CODE
		return x * ImprovedNoise.GRAD3[h][0] + y * ImprovedNoise.GRAD3[h][1] + z * ImprovedNoise.GRAD3[h][2];
	}

	static final int p[] = new int[512], permutation[] = { 151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36,
			103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11,
			32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158,
			231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80,
			73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217,
			226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183,
			170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
			224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235,
			249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205,
			93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180 };

	private static float[][] GRAD3 = new float[][] { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 }, { -1, 0, 1 },
			{ 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, -1, 1 },
			{ 0, 1, 1 } };

	static {
		for (int i = 0; i < 256; i++) {
			ImprovedNoise.p[256 + i] = ImprovedNoise.p[i] = ImprovedNoise.permutation[i];
		}
	}

	@Override
	public float value(final float x, final float y, final float z) {
		return ImprovedNoise.noise(this.scale * x, this.scale * y, this.scale * z);
	}

}
