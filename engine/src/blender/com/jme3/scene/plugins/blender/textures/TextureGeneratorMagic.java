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
 * This class generates the 'magic' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMagic extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator the noise generator
	 */
	public TextureGeneratorMagic(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, DataRepository dataRepository) {
		float x, y, z, turb;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue() / 5.0f;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				turb = turbul;
				texvec[1] = hDelta * j;
				x = (float) Math.sin((texvec[0] + texvec[1]) * 5.0f);// in blender: Math.sin((texvec[0] + texvec[1] + texvec[2]) * 5.0f);
				y = (float) Math.cos((-texvec[0] + texvec[1]) * 5.0f);// in blender: Math.cos((-texvec[0] + texvec[1] - texvec[2]) * 5.0f);
				z = -(float) Math.cos((-texvec[0] - texvec[1]) * 5.0f);// in blender: Math.cos((-texvec[0] - texvec[1] + texvec[2]) * 5.0f);

				if (colorBand != null) {
					texres.tin = 0.3333f * (x + y + z);
					noiseGenerator.doColorband(colorBand, texres, dataRepository);
				} else {
					if (noisedepth > 0) {
						x *= turb;
						y *= turb;
						z *= turb;
						y = -(float) Math.cos(x - y + z) * turb;
						if (noisedepth > 1) {
							x = (float) Math.cos(x - y - z) * turb;
							if (noisedepth > 2) {
								z = (float) Math.sin(-x - y - z) * turb;
								if (noisedepth > 3) {
									x = -(float) Math.cos(-x + y - z) * turb;
									if (noisedepth > 4) {
										y = -(float) Math.sin(-x + y + z) * turb;
										if (noisedepth > 5) {
											y = -(float) Math.cos(-x + y + z) * turb;
											if (noisedepth > 6) {
												x = (float) Math.cos(x + y + z) * turb;
												if (noisedepth > 7) {
													z = (float) Math.sin(x + y - z) * turb;
													if (noisedepth > 8) {
														x = -(float) Math.cos(-x - y + z) * turb;
														if (noisedepth > 9) {
															y = -(float) Math.sin(x - y + z) * turb;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}

					if (turb != 0.0f) {
						turb *= 2.0f;
						x /= turb;
						y /= turb;
						z /= turb;
					}
					texres.tr = 0.5f - x;
					texres.tg = 0.5f - y;
					texres.tb = 0.5f - z;
				}
				noiseGenerator.brightnesAndContrastRGB(tex, texres);
				data.put((byte) (texres.tin * 255));
				data.put((byte) (texres.tb * 255));
				data.put((byte) (texres.tg * 255));
				data.put((byte) (texres.tr * 255));
			}
		}
		return new Texture2D(new Image(Format.ABGR8, width, height, data));
	}
}
