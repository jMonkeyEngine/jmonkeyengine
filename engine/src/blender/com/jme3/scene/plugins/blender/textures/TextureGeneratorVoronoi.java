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
 * This class generates the 'voronoi' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorVoronoi extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorVoronoi(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		float vn_w1 = ((Number) tex.getFieldValue("vn_w1")).floatValue();
		float vn_w2 = ((Number) tex.getFieldValue("vn_w2")).floatValue();
		float vn_w3 = ((Number) tex.getFieldValue("vn_w3")).floatValue();
		float vn_w4 = ((Number) tex.getFieldValue("vn_w4")).floatValue();
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float ns_outscale = ((Number) tex.getFieldValue("ns_outscale")).floatValue();
		float vn_mexp = ((Number) tex.getFieldValue("vn_mexp")).floatValue();
		int vn_distm = ((Number) tex.getFieldValue("vn_distm")).intValue();
		int vn_coltype = ((Number) tex.getFieldValue("vn_coltype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();

		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = vn_coltype != 0 || colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = vn_coltype != 0 || colorBand != null ? 3 : 1;

		float[] da = new float[4], pa = new float[12]; /* distance and point coordinate arrays of 4 nearest neighbours */
		float[] ca = vn_coltype != 0 ? new float[3] : null; // cell color
		float aw1 = FastMath.abs(vn_w1);
		float aw2 = FastMath.abs(vn_w2);
		float aw3 = FastMath.abs(vn_w3);
		float aw4 = FastMath.abs(vn_w4);
		float sc = aw1 + aw2 + aw3 + aw4;
		if (sc != 0.f) {
			sc = ns_outscale / sc;
		}

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					noiseGenerator.voronoi(texvec[0], texvec[1], texvec[2], da, pa, vn_mexp, vn_distm);
					texres.tin = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
					if (vn_coltype != 0) {
						noiseGenerator.cellNoiseV(pa[0], pa[1], pa[2], ca);
						texres.tr = aw1 * ca[0];
						texres.tg = aw1 * ca[1];
						texres.tb = aw1 * ca[2];
						noiseGenerator.cellNoiseV(pa[3], pa[4], pa[5], ca);
						texres.tr += aw2 * ca[0];
						texres.tg += aw2 * ca[1];
						texres.tb += aw2 * ca[2];
						noiseGenerator.cellNoiseV(pa[6], pa[7], pa[8], ca);
						texres.tr += aw3 * ca[0];
						texres.tg += aw3 * ca[1];
						texres.tb += aw3 * ca[2];
						noiseGenerator.cellNoiseV(pa[9], pa[10], pa[11], ca);
						texres.tr += aw4 * ca[0];
						texres.tg += aw4 * ca[1];
						texres.tb += aw4 * ca[2];
						if (vn_coltype >= 2) {
							float t1 = (da[1] - da[0]) * 10.0f;
							if (t1 > 1) {
								t1 = 1.0f;
							}
							if (vn_coltype == 3) {
								t1 *= texres.tin;
							} else {
								t1 *= sc;
							}
							texres.tr *= t1;
							texres.tg *= t1;
							texres.tb *= t1;
						} else {
							texres.tr *= sc;
							texres.tg *= sc;
							texres.tb *= sc;
						}
					}
					if (colorBand != null) {
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
						if (texres.nor != null) {
							float offs = nabla / noisesize; // also scaling of texvec
							// calculate bumpnormal
							noiseGenerator.voronoi(texvec[0] + offs, texvec[1], texvec[2], da, pa, vn_mexp, vn_distm);
							texres.nor[0] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
							noiseGenerator.voronoi(texvec[0], texvec[1] + offs, texvec[2], da, pa, vn_mexp, vn_distm);
							texres.nor[1] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
							noiseGenerator.voronoi(texvec[0], texvec[1], texvec[2] + offs, da, pa, vn_mexp, vn_distm);
							texres.nor[2] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
							noiseGenerator.texNormalDerivate(colorBand, texres, dataRepository);
						}
					}

					if (vn_coltype != 0 || colorBand != null) {
						noiseGenerator.brightnesAndContrastRGB(tex, texres);
						data.put((byte) (texres.tr * 255.0f));// tin or tr??
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
