package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;

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
 * This class generates the 'stucci' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorStucci extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator the noise generator
	 */
	public TextureGeneratorStucci(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, DataRepository dataRepository) {
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		int noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noisetype = ((Number) tex.getFieldValue("noisetype")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue();
		boolean isHard = noisetype != NoiseGenerator.TEX_NOISESOFT;
		int stype = ((Number) tex.getFieldValue("stype")).intValue();

		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, b2, ofs;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;// x
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;// y (z is always = 0)
				b2 = noiseGenerator.bliGNoise(noisesize, texvec[0], texvec[1], texvec[2], isHard, noisebasis);

				ofs = turbul / 200.0f;

				if (stype != 0) {
					ofs *= b2 * b2;
				}

				texres.tin = noiseGenerator.bliGNoise(noisesize, texvec[0], texvec[1], texvec[2] + ofs, isHard, noisebasis);// ==nor[2]
				if (colorBand != null) {
					noiseGenerator.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {
						texres.nor[0] = noiseGenerator.bliGNoise(noisesize, texvec[0] + ofs, texvec[1], texvec[2], isHard, noisebasis);
						texres.nor[1] = noiseGenerator.bliGNoise(noisesize, texvec[0], texvec[1] + ofs, texvec[2], isHard, noisebasis);
						texres.nor[2] = texres.tin;
						noiseGenerator.texNormalDerivate(colorBand, texres, dataRepository);

						if (stype == NoiseGenerator.TEX_WALLOUT) {
							texres.nor[0] = -texres.nor[0];
							texres.nor[1] = -texres.nor[1];
							texres.nor[2] = -texres.nor[2];
						}
					}
				}

				if (stype == NoiseGenerator.TEX_WALLOUT) {
					texres.tin = 1.0f - texres.tin;
				}
				if (texres.tin < 0.0f) {
					texres.tin = 0.0f;
				}
				if (colorBand != null) {
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}
}
