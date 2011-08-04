package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper.ColorBand;
import com.jme3.scene.plugins.blender.textures.TextureHelper.TexResult;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

/**
 * This class generates the 'noise' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorNoise extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator the noise generator
	 */
	public TextureGeneratorNoise(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, DataRepository dataRepository) {
		float div = 3.0f;
		int val, ran, loop;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();
		TexResult texres = new TexResult();
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			for (int j = -halfH; j < halfH; ++j) {
				ran = FastMath.rand.nextInt();// BLI_rand();
				val = ran & 3;

				loop = noisedepth;
				while (loop-- != 0) {
					ran = ran >> 2;
					val *= ran & 3;
					div *= 3.0f;
				}
				texres.tin = val;// / div;
				if (colorBand != null) {
					noiseGenerator.doColorband(colorBand, texres, dataRepository);
					noiseGenerator.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseGenerator.brightnesAndContrast(texres, contrast, brightness);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}
}
