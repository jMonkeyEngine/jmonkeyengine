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
