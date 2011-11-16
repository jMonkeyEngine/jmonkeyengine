/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap.grid;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.terrain.MapUtils;
import com.jme3.terrain.geomipmap.TerrainGridTileLoader;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.Grayscale16BitHeightMap;
import com.jme3.terrain.heightmap.HeightMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import javax.imageio.ImageIO;
import org.novyon.noise.Basis;

/**
 *
 * @author Anthyon, normenhansen
 */
public class FractalTileLoader implements TerrainGridTileLoader{
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

	private int patchSize;
	private int quadSize;
	private final Basis base;
	private final String cacheDir;
	private final float heightScale;

	public FractalTileLoader(Basis base, String cacheDir, float heightScale) {
		this.base = base;
		this.cacheDir = cacheDir;
		this.heightScale = heightScale;
	}

	private HeightMap getHeightMapAt(Vector3f location) {
		AbstractHeightMap heightmap = null;
		if (this.cacheDir != null && new File(this.cacheDir, "terrain_" + (int) location.x + "_" + (int) location.z + ".png").exists()) {
			try {
				BufferedImage im = null;
				im = ImageIO.read(new File(this.cacheDir, "terrain_" + (int) location.x + "_" + (int) location.z + ".png"));
				heightmap = new Grayscale16BitHeightMap(im);
				heightmap.setHeightScale(heightScale);
			} catch (IOException e) {}
		} else {
			FloatBuffer buffer = this.base.getBuffer(location.x * (this.quadSize - 1), location.z * (this.quadSize - 1), 0, this.quadSize);
			if (this.cacheDir != null) {
				MapUtils.saveImage(MapUtils.toGrayscale16Image(buffer, this.quadSize), new File(this.cacheDir, "terrain_" + (int) location.x
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

    public TerrainQuad getTerrainQuadAt(Vector3f location) {
        HeightMap heightMapAt = getHeightMapAt(location);
        TerrainQuad q = new TerrainQuad("Quad" + location, patchSize, quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap());
        return q;
    }

    public void setPatchSize(int patchSize) {
        this.patchSize = patchSize;
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }

    public void write(JmeExporter ex) throws IOException {
        //TODO: serialization
    }

    public void read(JmeImporter im) throws IOException {
        //TODO: serialization
    }    
}
