package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper.ColorBand;
import com.jme3.scene.plugins.blender.textures.TextureHelper.TexResult;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;

/**
 * This class generates the 'musgrave' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMusgrave extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorMusgrave(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		int stype = ((Number) tex.getFieldValue("stype")).intValue();
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					switch (stype) {
						case NoiseGenerator.TEX_MFRACTAL:
						case NoiseGenerator.TEX_FBM:
							noiseGenerator.mgMFractalOrfBmTex(tex, texvec, colorBand, texres, dataRepository);
							break;
						case NoiseGenerator.TEX_RIDGEDMF:
						case NoiseGenerator.TEX_HYBRIDMF:
							noiseGenerator.mgRidgedOrHybridMFTex(tex, texvec, colorBand, texres, dataRepository);
							break;
						case NoiseGenerator.TEX_HTERRAIN:
							noiseGenerator.mgHTerrainTex(tex, texvec, colorBand, texres, dataRepository);
							break;
						default:
							throw new IllegalStateException("Unknown type of musgrave texture: " + stype);
					}
					if (colorBand != null) {
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
						data.put((byte) (texres.tr * 255.0f));
						data.put((byte) (texres.tg * 255.0f));
						data.put((byte) (texres.tb * 255.0f));
					} else {
						data.put((byte) (texres.tin * 255.0f));
					}
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(data);
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
}
