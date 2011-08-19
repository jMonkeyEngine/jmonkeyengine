package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.math.FastMath;
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
 * This class generates the 'blend' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public final class TextureGeneratorBlend extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorBlend(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		int stype = ((Number) tex.getFieldValue("stype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth, x, y, t;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					if ((flag & NoiseGenerator.TEX_FLIPBLEND) != 0) {
						x = texvec[1];
						y = texvec[0];
					} else {
						x = texvec[0];
						y = texvec[1];
					}

					if (stype == NoiseGenerator.TEX_LIN) { /* lin */
						texres.tin = (1.0f + x) / 2.0f;
					} else if (stype == NoiseGenerator.TEX_QUAD) { /* quad */
						texres.tin = (1.0f + x) / 2.0f;
						if (texres.tin < 0.0f) {
							texres.tin = 0.0f;
						} else {
							texres.tin *= texres.tin;
						}
					} else if (stype == NoiseGenerator.TEX_EASE) { /* ease */
						texres.tin = (1.0f + x) / 2.0f;
						if (texres.tin <= 0.0f) {
							texres.tin = 0.0f;
						} else if (texres.tin >= 1.0f) {
							texres.tin = 1.0f;
						} else {
							t = texres.tin * texres.tin;
							texres.tin = 3.0f * t - 2.0f * t * texres.tin;
						}
					} else if (stype == NoiseGenerator.TEX_DIAG) { /* diag */
						texres.tin = (2.0f + x + y) / 4.0f;
					} else if (stype == NoiseGenerator.TEX_RAD) { /* radial */
						texres.tin = (float) Math.atan2(y, x) / FastMath.TWO_PI + 0.5f;
					} else { /* sphere TEX_SPHERE */
						texres.tin = 1.0f - (float) Math.sqrt(x * x + y * y + texvec[2] * texvec[2]);
						if (texres.tin < 0.0f) {
							texres.tin = 0.0f;
						}
						if (stype == NoiseGenerator.TEX_HALO) {
							texres.tin *= texres.tin;
						} /* halo */
					}
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
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(data);
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
}
