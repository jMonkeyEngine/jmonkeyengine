/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.terrain.heightmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import org.novyon.noise.Basis;

import com.jme3.math.Vector3f;
import com.jme3.terrain.MapUtils;

public class FractalHeightMapGrid implements HeightMapGrid {

	public class FloatBufferHeightMap extends AbstractHeightMap {

		private final FloatBuffer buffer;

		public FloatBufferHeightMap(FloatBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public boolean load() {
			this.heightData = this.buffer.array();
			return true;
		}

	}

	private int size;
	private final Basis base;
	private final String cacheDir;
	private final float heightScale;

	public FractalHeightMapGrid(Basis base, String cacheDir, float heightScale) {
		this.base = base;
		this.cacheDir = cacheDir;
		this.heightScale = heightScale;
	}

	@Override
	public HeightMap getHeightMapAt(Vector3f location) {
		AbstractHeightMap heightmap = null;
		if (this.cacheDir != null && new File(this.cacheDir, "terrain_" + (int) location.x + "_" + (int) location.z + ".png").exists()) {
			try {
				BufferedImage im = null;
				im = ImageIO.read(new File(this.cacheDir, "terrain_" + (int) location.x + "_" + (int) location.z + ".png"));
				heightmap = new Grayscale16BitHeightMap(im);
				heightmap.setHeightScale(heightScale);
			} catch (IOException e) {}
		} else {
			FloatBuffer buffer = this.base.getBuffer(location.x * (this.size - 1), location.z * (this.size - 1), 0, this.size);
			if (this.cacheDir != null) {
				MapUtils.saveImage(MapUtils.toGrayscale16Image(buffer, this.size), new File(this.cacheDir, "terrain_" + (int) location.x
						+ "_" + (int) location.z + ".png"));
			}
			float[] arr = buffer.array();
			for (int i = 0; i < arr.length; i++) {
				arr[i] = arr[i] * this.heightScale;
			}
			heightmap = new FloatBufferHeightMap(buffer);
		}
		heightmap.load();
		return heightmap;
	}

	@Override
	public void setSize(int size) {
		this.size = size;
	}

}
