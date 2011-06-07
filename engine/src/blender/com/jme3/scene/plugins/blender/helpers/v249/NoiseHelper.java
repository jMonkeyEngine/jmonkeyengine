/*
 *
 * $Id: noise.c 14611 2008-04-29 08:24:33Z campbellbarton $
 *
 * ***** BEGIN GPL LICENSE BLOCK *****
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The Original Code is Copyright (C) 2001-2002 by NaN Holding BV.
 * All rights reserved.
 *
 * The Original Code is: all of this file.
 *
 * Contributor(s): none yet.
 *
 * ***** END GPL LICENSE BLOCK *****
 *
 */

package com.jme3.scene.plugins.blender.helpers.v249;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.helpers.v249.TextureHelper.CBData;
import com.jme3.scene.plugins.blender.helpers.v249.TextureHelper.ColorBand;
import com.jme3.scene.plugins.blender.helpers.v249.TextureHelper.TexResult;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;

/**
 * Methods of this class are copied from blender 2.49 source code and modified so that they can be used in java. They
 * are mostly NOT documented because they are not documented in blender's source code. If I find a proper description or
 * discover what they actually do and what parameters mean - I shall describe such methods :) If anyone have some hint
 * what these methods are doing please rite the proper javadoc documentation. These methods should be used to create
 * generated textures.
 * @author Marcin Roguski (Kaelthas)
 */
public class NoiseHelper extends AbstractBlenderHelper {
	private static final Logger	LOGGER				= Logger.getLogger(NoiseHelper.class.getName());

	/* return value */
	protected static final int	TEX_INT				= 0;
	protected static final int	TEX_RGB				= 1;
	protected static final int	TEX_NOR				= 2;

	/* noisetype */
	protected static final int	TEX_NOISESOFT		= 0;
	protected static final int	TEX_NOISEPERL		= 1;

	/* tex->stype in texture.c - cloud types */
	protected static final int	TEX_DEFAULT			= 0;
	protected static final int	TEX_COLOR			= 1;

	/* flag */
	protected static final int	TEX_COLORBAND		= 1;
	protected static final int	TEX_FLIPBLEND		= 2;
	protected static final int	TEX_NEGALPHA		= 4;
	protected static final int	TEX_CHECKER_ODD		= 8;
	protected static final int	TEX_CHECKER_EVEN	= 16;
	protected static final int	TEX_PRV_ALPHA		= 32;
	protected static final int	TEX_PRV_NOR			= 64;
	protected static final int	TEX_REPEAT_XMIR		= 128;
	protected static final int	TEX_REPEAT_YMIR		= 256;
	protected static final int	TEX_FLAG_MASK		= TEX_COLORBAND | TEX_FLIPBLEND | TEX_NEGALPHA | TEX_CHECKER_ODD | TEX_CHECKER_EVEN | TEX_PRV_ALPHA | TEX_PRV_NOR | TEX_REPEAT_XMIR | TEX_REPEAT_YMIR;

	/* tex->noisebasis2 in texture.c - wood waveforms */
	protected static final int	TEX_SIN				= 0;
	protected static final int	TEX_SAW				= 1;
	protected static final int	TEX_TRI				= 2;

	/* tex->stype in texture.c - marble types */
	protected static final int	TEX_SOFT			= 0;
	protected static final int	TEX_SHARP			= 1;
	protected static final int	TEX_SHARPER			= 2;

	/* tex->stype in texture.c - wood types */
	protected static final int	TEX_BAND			= 0;
	protected static final int	TEX_RING			= 1;
	protected static final int	TEX_BANDNOISE		= 2;
	protected static final int	TEX_RINGNOISE		= 3;

	/* tex->stype in texture.c - blend types */
	protected static final int	TEX_LIN				= 0;
	protected static final int	TEX_QUAD			= 1;
	protected static final int	TEX_EASE			= 2;
	protected static final int	TEX_DIAG			= 3;
	protected static final int	TEX_SPHERE			= 4;
	protected static final int	TEX_HALO			= 5;
	protected static final int	TEX_RAD				= 6;

	/* tex->stype in texture.c - stucci types */
	protected static final int	TEX_PLASTIC			= 0;
	protected static final int	TEX_WALLIN			= 1;
	protected static final int	TEX_WALLOUT			= 2;

	/* musgrave stype */
	protected static final int	TEX_MFRACTAL		= 0;
	protected static final int	TEX_RIDGEDMF		= 1;
	protected static final int	TEX_HYBRIDMF		= 2;
	protected static final int	TEX_FBM				= 3;
	protected static final int	TEX_HTERRAIN		= 4;

	/* keyblock->type */
	protected static final int	KEY_LINEAR			= 0;
	protected static final int	KEY_CARDINAL		= 1;
	protected static final int	KEY_BSPLINE			= 2;

	/* CONSTANTS (read from file) */
	protected static float[]	hashpntf;
	protected static short[]	hash;
	protected static float[]	hashvectf;
	protected static short[]	p;
	protected static float[][]	g;

	/**
	 * Constructor. Stores the blender version number and loads the constants needed for computations.
	 * @param blenderVersion
	 *        the number of blender version
	 */
	public NoiseHelper(String blenderVersion) {
		super(blenderVersion);
		this.loadConstants();
	}

	/**
	 * This method loads the constants needed for computations. They are exactly like the ones the blender uses. Each
	 * deriving class should override this method and load its own constraints. Be carefult with overriding though, if
	 * an exception will be thrown the class will not be instantiated.
	 */
	protected void loadConstants() {
		InputStream is = NoiseHelper.class.getResourceAsStream("noiseconstants.dat");
		try {
			ObjectInputStream ois = new ObjectInputStream(is);
			hashpntf = (float[])ois.readObject();
			hash = (short[])ois.readObject();
			hashvectf = (float[])ois.readObject();
			p = (short[])ois.readObject();
			g = (float[][])ois.readObject();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch(ClassNotFoundException e) {
			assert false : "Constants' classes should be arrays of primitive types, so they are ALWAYS known!";
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch(IOException e) {
					LOGGER.log(Level.WARNING, e.getLocalizedMessage());
				}
			}
		}
	}

	protected static Map<Integer, AbstractNoiseFunc>	noiseFunctions		= new HashMap<Integer, AbstractNoiseFunc>();
	static {
		// orgBlenderNoise (*Was BLI_hnoise(), removed noisesize, so other functions can call it without scaling.*)
		noiseFunctions.put(Integer.valueOf(0), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				return this.orgBlenderNoise(x, y, z);
			}

			@Override
			public float executeS(float x, float y, float z) {
				return 2.0f * this.orgBlenderNoise(x, y, z) - 1.0f;
			}
		});
		// orgPerlinNoise (*For use with BLI_gNoise/gTurbulence, returns signed noise.*)
		noiseFunctions.put(Integer.valueOf(1), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				return 0.5f + 0.5f * this.noise3Perlin(new float[] {x, y, z});
			}

			@Override
			public float executeS(float x, float y, float z) {
				return this.noise3Perlin(new float[] {x, y, z});
			}
		});
		// newPerlin (* for use with BLI_gNoise()/BLI_gTurbulence(), returns unsigned improved perlin noise *)
		noiseFunctions.put(Integer.valueOf(2), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				return 0.5f + 0.5f * this.newPerlin(x, y, z);
			}

			@Override
			public float executeS(float x, float y, float z) {
				return this.execute(x, y, z);
			}
		});
		// voronoi_F1
		noiseFunctions.put(Integer.valueOf(3), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return da[0];
			}

			@Override
			public float executeS(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return 2.0f * da[0] - 1.0f;
			}
		});
		// voronoi_F2
		noiseFunctions.put(Integer.valueOf(4), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return da[1];
			}

			@Override
			public float executeS(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return 2.0f * da[1] - 1.0f;
			}
		});
		// voronoi_F3
		noiseFunctions.put(Integer.valueOf(5), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return da[2];
			}

			@Override
			public float executeS(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return 2.0f * da[2] - 1.0f;
			}
		});
		// voronoi_F4
		noiseFunctions.put(Integer.valueOf(6), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return da[3];
			}

			@Override
			public float executeS(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return 2.0f * da[3] - 1.0f;
			}
		});
		// voronoi_F1F2
		noiseFunctions.put(Integer.valueOf(7), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return da[1] - da[0];
			}

			@Override
			public float executeS(float x, float y, float z) {
				float[] da = new float[4], pa = new float[12];
				AbstractNoiseFunc.voronoi(x, y, z, da, pa, 1, 0);
				return 2.0f * (da[1] - da[0]) - 1.0f;
			}
		});
		// voronoi_Cr
		noiseFunctions.put(Integer.valueOf(8), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				float t = 10 * noiseFunctions.get(Integer.valueOf(7)).execute(x, y, z);// voronoi_F1F2
				return t > 1.0f ? 1.0f : t;
			}

			@Override
			public float executeS(float x, float y, float z) {
				float t = 10.0f * noiseFunctions.get(Integer.valueOf(7)).execute(x, y, z);// voronoi_F1F2
				return t > 1.0f ? 1.0f : 2.0f * t - 1.0f;
			}
		});
		// cellNoise
		noiseFunctions.put(Integer.valueOf(14), new AbstractNoiseFunc() {
			@Override
			public float execute(float x, float y, float z) {
				int xi = (int)Math.floor(x);
				int yi = (int)Math.floor(y);
				int zi = (int)Math.floor(z);
				long n = xi + yi * 1301 + zi * 314159;
				n ^= n << 13;
				return (n * (n * n * 15731 + 789221) + 1376312589) / 4294967296.0f;
			}

			@Override
			public float executeS(float x, float y, float z) {
				return 2.0f * this.execute(x, y, z) - 1.0f;
			}
		});
	}

	/** Distance metrics for voronoi. e parameter only used in Minkovsky. */
	protected static Map<Integer, IDistanceFunc>		distanceFunctions	= new HashMap<Integer, NoiseHelper.IDistanceFunc>();
	static {
		// real distance
		distanceFunctions.put(Integer.valueOf(0), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				return (float)Math.sqrt(x * x + y * y + z * z);
			}
		});
		// distance squared
		distanceFunctions.put(Integer.valueOf(1), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				return x * x + y * y + z * z;
			}
		});
		// manhattan/taxicab/cityblock distance
		distanceFunctions.put(Integer.valueOf(2), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				return FastMath.abs(x) + FastMath.abs(y) + FastMath.abs(z);
			}
		});
		// Chebychev
		distanceFunctions.put(Integer.valueOf(3), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				x = FastMath.abs(x);
				y = FastMath.abs(y);
				z = FastMath.abs(z);
				float t = x > y ? x : y;
				return z > t ? z : t;
			}
		});
		// minkovsky preset exponent 0.5 (MinkovskyH)
		distanceFunctions.put(Integer.valueOf(4), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				float d = (float)(Math.sqrt(FastMath.abs(x)) + Math.sqrt(FastMath.abs(y)) + Math.sqrt(FastMath.abs(z)));
				return d * d;
			}
		});
		// minkovsky preset exponent 4 (Minkovsky4)
		distanceFunctions.put(Integer.valueOf(5), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				x *= x;
				y *= y;
				z *= z;
				return (float)Math.sqrt(Math.sqrt(x * x + y * y + z * z));
			}
		});
		// Minkovsky, general case, slow, maybe too slow to be useful
		distanceFunctions.put(Integer.valueOf(6), new IDistanceFunc() {
			@Override
			public float execute(float x, float y, float z, float e) {
				return (float)Math.pow(Math.pow(FastMath.abs(x), e) + Math.pow(FastMath.abs(y), e) + Math.pow(FastMath.abs(z), e), 1.0f / e);
			}
		});
	}

	protected static Map<Integer, IMusgraveFunction>	musgraveFunctions	= new HashMap<Integer, NoiseHelper.IMusgraveFunction>();
	static {
		musgraveFunctions.put(Integer.valueOf(TEX_MFRACTAL), new IMusgraveFunction() {
			@Override
			public float execute(Structure tex, float x, float y, float z) {
				float mg_H = ((Number)tex.getFieldValue("mg_H")).floatValue();
				float mg_lacunarity = ((Number)tex.getFieldValue("mg_lacunarity")).floatValue();
				float mg_octaves = ((Number)tex.getFieldValue("mg_octaves")).floatValue();
				int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();

				float rmd, value = 1.0f, pwr = 1.0f, pwHL = (float)Math.pow(mg_lacunarity, -mg_H);
				AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
				if(abstractNoiseFunc == null) {
					abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
				}

				for(int i = 0; i < (int)mg_octaves; ++i) {
					value *= pwr * abstractNoiseFunc.executeS(x, y, z) + 1.0f;
					pwr *= pwHL;
					x *= mg_lacunarity;
					y *= mg_lacunarity;
					z *= mg_lacunarity;
				}
				rmd = (float)(mg_octaves - Math.floor(mg_octaves));
				if(rmd != 0.0f) {
					value *= rmd * abstractNoiseFunc.executeS(x, y, z) * pwr + 1.0f;
				}
				return value;
			}
		});
		musgraveFunctions.put(Integer.valueOf(TEX_RIDGEDMF), new IMusgraveFunction() {
			@Override
			public float execute(Structure tex, float x, float y, float z) {
				float mg_H = ((Number)tex.getFieldValue("mg_H")).floatValue();
				float mg_lacunarity = ((Number)tex.getFieldValue("mg_lacunarity")).floatValue();
				float mg_octaves = ((Number)tex.getFieldValue("mg_octaves")).floatValue();
				float mg_offset = ((Number)tex.getFieldValue("mg_offset")).floatValue();
				int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
				float mg_gain = ((Number)tex.getFieldValue("mg_gain")).floatValue();
				float result, signal, weight;
				float pwHL = (float)Math.pow(mg_lacunarity, -mg_H);
				float pwr = pwHL; /* starts with i=1 instead of 0 */

				AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
				if(abstractNoiseFunc == null) {
					abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
				}

				signal = mg_offset - FastMath.abs(abstractNoiseFunc.executeS(x, y, z));
				signal *= signal;
				result = signal;
				weight = 1.0f;

				for(int i = 1; i < (int)mg_octaves; ++i) {
					x *= mg_lacunarity;
					y *= mg_lacunarity;
					z *= mg_lacunarity;
					weight = signal * mg_gain;
					if(weight > 1.0f) {
						weight = 1.0f;
					} else if(weight < 0.0) {
						weight = 0.0f;
					}
					signal = mg_offset - FastMath.abs(abstractNoiseFunc.executeS(x, y, z));
					signal *= signal;
					signal *= weight;
					result += signal * pwr;
					pwr *= pwHL;
				}
				return result;
			}
		});
		musgraveFunctions.put(Integer.valueOf(TEX_HYBRIDMF), new IMusgraveFunction() {
			@Override
			public float execute(Structure tex, float x, float y, float z) {
				float mg_H = ((Number)tex.getFieldValue("mg_H")).floatValue();
				float mg_lacunarity = ((Number)tex.getFieldValue("mg_lacunarity")).floatValue();
				float mg_octaves = ((Number)tex.getFieldValue("mg_octaves")).floatValue();
				float mg_offset = ((Number)tex.getFieldValue("mg_offset")).floatValue();
				int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
				float mg_gain = ((Number)tex.getFieldValue("mg_gain")).floatValue();
				float result, signal, weight, rmd;
				float pwHL = (float)Math.pow(mg_lacunarity, -mg_H);
				float pwr = pwHL; /* starts with i=1 instead of 0 */
				AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
				if(abstractNoiseFunc == null) {
					abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
				}

				result = abstractNoiseFunc.executeS(x, y, z) + mg_offset;
				weight = mg_gain * result;
				x *= mg_lacunarity;
				y *= mg_lacunarity;
				z *= mg_lacunarity;

				for(int i = 1; weight > 0.001f && i < (int)mg_octaves; ++i) {
					if(weight > 1.0f) {
						weight = 1.0f;
					}
					signal = (abstractNoiseFunc.executeS(x, y, z) + mg_offset) * pwr;
					pwr *= pwHL;
					result += weight * signal;
					weight *= mg_gain * signal;
					x *= mg_lacunarity;
					y *= mg_lacunarity;
					z *= mg_lacunarity;
				}

				rmd = mg_octaves - (float)Math.floor(mg_octaves);
				if(rmd != 0.0f) {
					result += rmd * (abstractNoiseFunc.executeS(x, y, z) + mg_offset) * pwr;
				}
				return result;
			}
		});
		musgraveFunctions.put(Integer.valueOf(TEX_FBM), new IMusgraveFunction() {
			@Override
			public float execute(Structure tex, float x, float y, float z) {
				float mg_H = ((Number)tex.getFieldValue("mg_H")).floatValue();
				float mg_lacunarity = ((Number)tex.getFieldValue("mg_lacunarity")).floatValue();
				float mg_octaves = ((Number)tex.getFieldValue("mg_octaves")).floatValue();
				int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
				float rmd, value = 0.0f, pwr = 1.0f, pwHL = (float)Math.pow(mg_lacunarity, -mg_H);

				AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
				if(abstractNoiseFunc == null) {
					abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
				}

				for(int i = 0; i < (int)mg_octaves; ++i) {
					value += abstractNoiseFunc.executeS(x, y, z) * pwr;
					pwr *= pwHL;
					x *= mg_lacunarity;
					y *= mg_lacunarity;
					z *= mg_lacunarity;
				}

				rmd = (float)(mg_octaves - Math.floor(mg_octaves));
				if(rmd != 0.f) {
					value += rmd * abstractNoiseFunc.executeS(x, y, z) * pwr;
				}
				return value;
			}
		});
		musgraveFunctions.put(Integer.valueOf(TEX_HTERRAIN), new IMusgraveFunction() {
			@Override
			public float execute(Structure tex, float x, float y, float z) {
				float mg_H = ((Number)tex.getFieldValue("mg_H")).floatValue();
				float mg_lacunarity = ((Number)tex.getFieldValue("mg_lacunarity")).floatValue();
				float mg_octaves = ((Number)tex.getFieldValue("mg_octaves")).floatValue();
				int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
				float mg_offset = ((Number)tex.getFieldValue("mg_offset")).floatValue();
				float value, increment, rmd;
				float pwHL = (float)Math.pow(mg_lacunarity, -mg_H);
				float pwr = pwHL; /* starts with i=1 instead of 0 */
				AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
				if(abstractNoiseFunc == null) {
					abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
				}

				/* first unscaled octave of function; later octaves are scaled */
				value = mg_offset + abstractNoiseFunc.executeS(x, y, z);
				x *= mg_lacunarity;
				y *= mg_lacunarity;
				z *= mg_lacunarity;

				for(int i = 1; i < (int)mg_octaves; ++i) {
					increment = (abstractNoiseFunc.executeS(x, y, z) + mg_offset) * pwr * value;
					value += increment;
					pwr *= pwHL;
					x *= mg_lacunarity;
					y *= mg_lacunarity;
					z *= mg_lacunarity;
				}

				rmd = mg_octaves - (float)Math.floor(mg_octaves);
				if(rmd != 0.0) {
					increment = (abstractNoiseFunc.executeS(x, y, z) + mg_offset) * pwr * value;
					value += rmd * increment;
				}
				return value;
			}
		});
	}

	/**
	 * THE FOLLOWING METHODS HELP IN COMPUTATION OF THE TEXTURES.
	 */
	protected void brightnesAndContrast(TexResult texres, float contrast, float brightness) {
		texres.tin = (texres.tin - 0.5f) * contrast + brightness - 0.5f;
		if(texres.tin < 0.0f) {
			texres.tin = 0.0f;
		} else if(texres.tin > 1.0f) {
			texres.tin = 1.0f;
		}
	}

	protected void brightnesAndContrastRGB(Structure tex, TexResult texres) {
		float contrast = ((Number)tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number)tex.getFieldValue("bright")).floatValue();
		float rfac = ((Number)tex.getFieldValue("rfac")).floatValue();
		float gfac = ((Number)tex.getFieldValue("gfac")).floatValue();
		float bfac = ((Number)tex.getFieldValue("bfac")).floatValue();

		texres.tr = rfac * ((texres.tr - 0.5f) * contrast + bright - 0.5f);
		if(texres.tr < 0.0f) {
			texres.tr = 0.0f;
		}
		texres.tg = gfac * ((texres.tg - 0.5f) * contrast + bright - 0.5f);
		if(texres.tg < 0.0f) {
			texres.tg = 0.0f;
		}
		texres.tb = bfac * ((texres.tb - 0.5f) * contrast + bright - 0.5f);
		if(texres.tb < 0.0f) {
			texres.tb = 0.0f;
		}
	}

	/* this allows colorbanded textures to control normals as well */
	public void texNormalDerivate(ColorBand colorBand, TexResult texres, DataRepository dataRepository) {
		if(texres.nor != null) {
			TexResult fakeTexresult;
			try {
				fakeTexresult = (TexResult)texres.clone();
			} catch(CloneNotSupportedException e) {
				throw new IllegalStateException("Texture result class MUST support cloning!", e);
			}

			float fac0 = fakeTexresult.tr + fakeTexresult.tg + fakeTexresult.tb;
			fakeTexresult.tin = texres.nor[0];
			this.doColorband(colorBand, fakeTexresult, dataRepository);

			float fac1 = fakeTexresult.tr + fakeTexresult.tg + fakeTexresult.tb;
			fakeTexresult.tin = texres.nor[1];
			this.doColorband(colorBand, fakeTexresult, dataRepository);

			float fac2 = fakeTexresult.tr + fakeTexresult.tg + fakeTexresult.tb;
			fakeTexresult.tin = texres.nor[2];
			this.doColorband(colorBand, fakeTexresult, dataRepository);

			float fac3 = fakeTexresult.tr + fakeTexresult.tg + fakeTexresult.tb;

			texres.nor[0] = 0.3333f * (fac0 - fac1);
			texres.nor[1] = 0.3333f * (fac0 - fac2);
			texres.nor[2] = 0.3333f * (fac0 - fac3);

			texres.nor[0] = texres.tin - texres.nor[0];
			texres.nor[1] = texres.tin - texres.nor[1];
			texres.nor[2] = texres.tin - texres.nor[2];
		}
	}

	/**
	 * This method calculates the colorband for the texture.
	 * @param colorBand
	 *        the colorband data
	 * @param texres
	 *        the texture pixel result
	 * @param dataRepository
	 *        the data repository
	 * @return <b>true</b> if calculation suceedess and <b>false</b> otherwise
	 */
	public boolean doColorband(ColorBand colorBand, TexResult texres, DataRepository dataRepository) {
		CBData cbd1, cbd2, cbd0, cbd3;
		int i1 = 0, i2 = 0, a;
		float fac, mfac;
		float[] t = new float[4];

		if(colorBand == null || colorBand.tot == 0) {
			return true;
		}

		cbd1 = colorBand.data[0];
		if(colorBand.tot == 1) {
			texres.tr = cbd1.r;
			texres.tg = cbd1.g;
			texres.tb = cbd1.b;
			texres.ta = cbd1.a;
		} else {
			if(texres.tin <= cbd1.pos && colorBand.ipotype < 2) {
				texres.tr = cbd1.r;
				texres.tg = cbd1.g;
				texres.tb = cbd1.b;
				texres.ta = cbd1.a;
			} else {
				/* we're looking for first pos > in */
				for(a = 0; a < colorBand.tot; ++a, ++i1) {
					cbd1 = colorBand.data[i1];
					if(cbd1.pos > texres.tin) {
						break;
					}
				}

				if(a == colorBand.tot) {
					cbd2 = colorBand.data[i1 - 1];
					try {
						cbd1 = (CBData)cbd2.clone();
					} catch(CloneNotSupportedException e) {
						throw new IllegalStateException("Clone not supported for " + CBData.class.getName() + " class! Fix that!");
					}
					cbd1.pos = 1.0f;
				} else if(a == 0) {
					try {
						cbd2 = (CBData)cbd1.clone();
					} catch(CloneNotSupportedException e) {
						throw new IllegalStateException("Clone not supported for " + CBData.class.getName() + " class! Fix that!");
					}
					cbd2.pos = 0.0f;
				} else {
					cbd2 = colorBand.data[i1 - 1];
				}

				if(texres.tin >= cbd1.pos && colorBand.ipotype < 2) {
					texres.tr = cbd1.r;
					texres.tg = cbd1.g;
					texres.tb = cbd1.b;
					texres.ta = cbd1.a;
				} else {

					if(cbd2.pos != cbd1.pos) {
						fac = (texres.tin - cbd1.pos) / (cbd2.pos - cbd1.pos);
					} else {
						fac = 0.0f;
					}

					if(colorBand.ipotype == 4) {
						/* constant */
						texres.tr = cbd2.r;
						texres.tg = cbd2.g;
						texres.tb = cbd2.b;
						texres.ta = cbd2.a;
						return true;
					}

					if(colorBand.ipotype >= 2) {
						/* ipo from right to left: 3 2 1 0 */

						if(a >= colorBand.tot - 1) {
							cbd0 = cbd1;
						} else {
							cbd0 = colorBand.data[i1 + 1];
						}
						if(a < 2) {
							cbd3 = cbd2;
						} else {
							cbd3 = colorBand.data[i2 - 1];
						}

						fac = FastMath.clamp(fac, 0.0f, 1.0f);

						if(colorBand.ipotype == 3) {
							this.setFourIpo(fac, t, KEY_CARDINAL);
						} else {
							this.setFourIpo(fac, t, KEY_BSPLINE);
						}

						texres.tr = t[3] * cbd3.r + t[2] * cbd2.r + t[1] * cbd1.r + t[0] * cbd0.r;
						texres.tg = t[3] * cbd3.g + t[2] * cbd2.g + t[1] * cbd1.g + t[0] * cbd0.g;
						texres.tb = t[3] * cbd3.b + t[2] * cbd2.b + t[1] * cbd1.b + t[0] * cbd0.b;
						texres.ta = t[3] * cbd3.a + t[2] * cbd2.a + t[1] * cbd1.a + t[0] * cbd0.a;
						texres.tr = FastMath.clamp(texres.tr, 0.0f, 1.0f);
						texres.tg = FastMath.clamp(texres.tg, 0.0f, 1.0f);
						texres.tb = FastMath.clamp(texres.tb, 0.0f, 1.0f);
						texres.ta = FastMath.clamp(texres.ta, 0.0f, 1.0f);
					} else {

						if(colorBand.ipotype == 1) { /* EASE */
							mfac = fac * fac;
							fac = 3.0f * mfac - 2.0f * mfac * fac;
						}
						mfac = 1.0f - fac;

						texres.tr = mfac * cbd1.r + fac * cbd2.r;
						texres.tg = mfac * cbd1.g + fac * cbd2.g;
						texres.tb = mfac * cbd1.b + fac * cbd2.b;
						texres.ta = mfac * cbd1.a + fac * cbd2.a;
					}
				}
			}
		}
		return true;
	}

	protected void setFourIpo(float d, float[] data, int type) {
		if(type == KEY_LINEAR) {
			data[0] = 0.0f;
			data[1] = 1.0f - d;
			data[2] = d;
			data[3] = 0.0f;
		} else {
			float d2 = d * d;
			float d3 = d2 * d;
			if(type == KEY_CARDINAL) {
				float fc = 0.71f;
				data[0] = -fc * d3 + 2.0f * fc * d2 - fc * d;
				data[1] = (2.0f - fc) * d3 + (fc - 3.0f) * d2 + 1.0f;
				data[2] = (fc - 2.0f) * d3 + (3.0f - 2.0f * fc) * d2 + fc * d;
				data[3] = fc * d3 - fc * d2;
			} else if(type == KEY_BSPLINE) {
				data[0] = -0.16666666f * d3 + 0.5f * d2 - 0.5f * d + 0.16666666f;
				data[1] = 0.5f * d3 - d2 + 0.6666666f;
				data[2] = -0.5f * d3 + 0.5f * d2 + 0.5f * d + 0.16666666f;
				data[3] = 0.16666666f * d3;
			}
		}
	}

	interface IWaveForm {
		float execute(float x);
	}

	protected static IWaveForm[]	waveformFunctions	= new IWaveForm[3];
	static {
		waveformFunctions[0] = new IWaveForm() {// tex_sin
			@Override
			public float execute(float x) {
				return 0.5f + 0.5f * (float)Math.sin(x);
			}
		};
		waveformFunctions[1] = new IWaveForm() {// tex_saw
			@Override
			public float execute(float x) {
				int n = (int)(x / FastMath.TWO_PI);
				x -= n * FastMath.TWO_PI;
				if(x < 0.0f) {
					x += FastMath.TWO_PI;
				}
				return x / FastMath.TWO_PI;
			}
		};
		waveformFunctions[2] = new IWaveForm() {// tex_tri
			@Override
			public float execute(float x) {
				return 1.0f - 2.0f * FastMath.abs((float)Math.floor(x * 1.0f / FastMath.TWO_PI + 0.5f) - x * 1.0f / FastMath.TWO_PI);
			}
		};
	}

	/* computes basic wood intensity value at x,y,z */
	public float woodInt(Structure tex, float x, float y, float z, DataRepository dataRepository) {
		int noisebasis2 = ((Number)tex.getFieldValue("noisebasis2")).intValue();
		int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
		int stype = ((Number)tex.getFieldValue("stype")).intValue();
		float noisesize = ((Number)tex.getFieldValue("noisesize")).floatValue();
		float turbul = ((Number)tex.getFieldValue("turbul")).floatValue();
		int noiseType = ((Number)tex.getFieldValue("noisetype")).intValue();
		float wi = 0;
		int waveform = noisebasis2; /* wave form: TEX_SIN=0, TEX_SAW=1, TEX_TRI=2 */
		int wt = stype; /* wood type: TEX_BAND=0, TEX_RING=1, TEX_BANDNOISE=2, TEX_RINGNOISE=3 */

		if(waveform > TEX_TRI || waveform < TEX_SIN) {
			waveform = 0; /* check to be sure noisebasis2 is initialized ahead of time */
		}

		if(wt == TEX_BAND) {
			wi = waveformFunctions[waveform].execute((x + y + z) * 10.0f);
		} else if(wt == TEX_RING) {
			wi = waveformFunctions[waveform].execute((float)Math.sqrt(x * x + y * y + z * z) * 20.0f);
		} else if(wt == TEX_BANDNOISE) {
			wi = turbul * this.bliGNoise(noisesize, x, y, z, noiseType != TEX_NOISESOFT, noisebasis);
			wi = waveformFunctions[waveform].execute((x + y + z) * 10.0f + wi);
		} else if(wt == TEX_RINGNOISE) {
			wi = turbul * this.bliGNoise(noisesize, x, y, z, noiseType != TEX_NOISESOFT, noisebasis);
			wi = waveformFunctions[waveform].execute((float)Math.sqrt(x * x + y * y + z * z) * 20.0f + wi);
		}
		return wi;
	}

	/* computes basic marble intensity at x,y,z */
	public float marbleInt(Structure tex, float x, float y, float z, DataRepository dataRepository) {
		float noisesize = ((Number)tex.getFieldValue("noisesize")).floatValue();
		int noisebasis = ((Number)tex.getFieldValue("noisebasis")).intValue();
		int noisedepth = ((Number)tex.getFieldValue("noisedepth")).intValue();
		int stype = ((Number)tex.getFieldValue("stype")).intValue();/* marble type: TEX_SOFT=0, TEX_SHARP=1,TEX_SHAPER=2 */
		float turbul = ((Number)tex.getFieldValue("turbul")).floatValue();
		int noisetype = ((Number)tex.getFieldValue("noisetype")).intValue();
		int waveform = ((Number)tex.getFieldValue("noisebasis2")).intValue(); /* wave form: TEX_SIN=0, TEX_SAW=1, TEX_TRI=2 */

		if(waveform > TEX_TRI || waveform < TEX_SIN) {
			waveform = 0; /* check to be sure noisebasis2 isn't initialized ahead of time */
		}

		float n = 5.0f * (x + y + z);
		float mi = n + turbul * this.bliGTurbulence(noisesize, x, y, z, noisedepth, noisetype != TEX_NOISESOFT, noisebasis);

		if(stype >= NoiseHelper.TEX_SOFT) { /* TEX_SOFT always true */
			mi = waveformFunctions[waveform].execute(mi);
			if(stype == TEX_SHARP) {
				mi = (float)Math.sqrt(mi);
			} else if(stype == TEX_SHARPER) {
				mi = (float)Math.sqrt(Math.sqrt(mi));
			}
		}
		return mi;
	}

	public void voronoi(float x, float y, float z, float[] da, float[] pa, float me, int dtype) {
		AbstractNoiseFunc.voronoi(x, y, z, da, pa, me, dtype);
	}

	public void cellNoiseV(float x, float y, float z, float[] ca) {
		AbstractNoiseFunc.cellNoiseV(x, y, z, ca);
	}

	/**
	 * THE FOLLOWING METHODS HELP IN NOISE COMPUTATIONS
	 */

	/**
	 * Separated from orgBlenderNoise above, with scaling.
	 * @param noisesize
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private float bliHnoise(float noisesize, float x, float y, float z) {
		if(noisesize == 0.0) {
			return 0.0f;
		}
		x = (1.0f + x) / noisesize;
		y = (1.0f + y) / noisesize;
		z = (1.0f + z) / noisesize;
		return noiseFunctions.get(0).execute(x, y, z);
	}

	/**
	 * @param noisesize
	 * @param x
	 * @param y
	 * @param z
	 * @param nr
	 * @return
	 */
	public float bliTurbulence(float noisesize, float x, float y, float z, int nr) {
		float d = 0.5f, div = 1.0f;

		float s = this.bliHnoise(noisesize, x, y, z);
		while(nr > 0) {
			s += d * this.bliHnoise(noisesize * d, x, y, z);
			div += d;
			d *= 0.5;
			--nr;
		}
		return s / div;
	}

	/**
	 * @param noisesize
	 * @param x
	 * @param y
	 * @param z
	 * @param nr
	 * @return
	 */
	public float bliTurbulence1(float noisesize, float x, float y, float z, int nr) {
		float s, d = 0.5f, div = 1.0f;

		s = FastMath.abs((-1.0f + 2.0f * this.bliHnoise(noisesize, x, y, z)));
		while(nr > 0) {
			s += Math.abs(d * (-1.0f + 2.0f * this.bliHnoise(noisesize * d, x, y, z)));
			div += d;
			d *= 0.5;
			--nr;
		}
		return s / div;
	}

	/**
	 * @param noisesize
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float bliHnoisep(float noisesize, float x, float y, float z) {
		return noiseFunctions.get(Integer.valueOf(0)).noise3Perlin(new float[] {x / noisesize, y / noisesize, z / noisesize});
	}

	/**
	 * @param point
	 * @param lofreq
	 * @param hifreq
	 * @return
	 */
	public float turbulencePerlin(float[] point, float lofreq, float hifreq) {
		float freq, t = 0, p[] = new float[] {point[0] + 123.456f, point[1], point[2]};
		for(freq = lofreq; freq < hifreq; freq *= 2.) {
			t += Math.abs(noiseFunctions.get(Integer.valueOf(0)).noise3Perlin(p)) / freq;
			p[0] *= 2.0f;
			p[1] *= 2.0f;
			p[2] *= 2.0f;
		}
		return t - 0.3f; /* readjust to make mean value = 0.0 */
	}

	/**
	 * @param noisesize
	 * @param x
	 * @param y
	 * @param z
	 * @param nr
	 * @return
	 */
	public float turbulencep(float noisesize, float x, float y, float z, int nr) {
		float[] vec = new float[] {x / noisesize, y / noisesize, z / noisesize};
		++nr;
		return this.turbulencePerlin(vec, 1.0f, (1 << nr));
	}

	/**
	 * Newnoise: generic noise function for use with different noisebases
	 * @param x
	 * @param y
	 * @param z
	 * @param oct
	 * @param isHard
	 * @param noisebasis
	 * @return
	 */
	public float bliGNoise(float noisesize, float x, float y, float z, boolean isHard, int noisebasis) {
		AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
		if(abstractNoiseFunc == null) {
			abstractNoiseFunc = noiseFunctions.get(0);
			noisebasis = 0;
		}
		if(noisebasis == 0) {// add one to make return value same as BLI_hnoise
			x += 1;
			y += 1;
			z += 1;
		}

		if(noisesize != 0.0) {
			noisesize = 1.0f / noisesize;
			x *= noisesize;
			y *= noisesize;
			z *= noisesize;
		}
		if(isHard) {
			return Math.abs(2.0f * abstractNoiseFunc.execute(x, y, z) - 1.0f);
		}
		return abstractNoiseFunc.execute(x, y, z);
	}

	/**
	 * Newnoise: generic turbulence function for use with different noisebasis
	 * @param x
	 * @param y
	 * @param z
	 * @param oct
	 * @param isHard
	 * @param noisebasis
	 * @return
	 */
	public float bliGTurbulence(float noisesize, float x, float y, float z, int oct, boolean isHard, int noisebasis) {
		AbstractNoiseFunc abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noisebasis));
		if(abstractNoiseFunc == null) {
			abstractNoiseFunc = noiseFunctions.get(0);
			noisebasis = 0;
		}
		if(noisebasis == 0) {// add one to make return value same as BLI_hnoise
			x += 1;
			y += 1;
			z += 1;
		}
		float sum = 0, t, amp = 1, fscale = 1;

		if(noisesize != 0.0) {
			noisesize = 1.0f / noisesize;
			x *= noisesize;
			y *= noisesize;
			z *= noisesize;
		}
		for(int i = 0; i <= oct; ++i, amp *= 0.5, fscale *= 2) {
			t = abstractNoiseFunc.execute(fscale * x, fscale * y, fscale * z);
			if(isHard) {
				t = FastMath.abs(2.0f * t - 1.0f);
			}
			sum += t * amp;
		}

		sum *= (float)(1 << oct) / (float)((1 << oct + 1) - 1);
		return sum;
	}

	/**
	 * "Variable Lacunarity Noise" A distorted variety of Perlin noise. This method is used to calculate distorted noise
	 * texture.
	 * @param x
	 * @param y
	 * @param z
	 * @param distortion
	 * @param nbas1
	 * @param nbas2
	 * @return
	 */
	public float mgVLNoise(float x, float y, float z, float distortion, int nbas1, int nbas2) {
		AbstractNoiseFunc abstractNoiseFunc1 = noiseFunctions.get(Integer.valueOf(nbas1));
		if(abstractNoiseFunc1 == null) {
			abstractNoiseFunc1 = noiseFunctions.get(Integer.valueOf(0));
		}
		AbstractNoiseFunc abstractNoiseFunc2 = noiseFunctions.get(Integer.valueOf(nbas2));
		if(abstractNoiseFunc2 == null) {
			abstractNoiseFunc2 = noiseFunctions.get(Integer.valueOf(0));
		}
		// get a random vector and scale the randomization
		float rx = abstractNoiseFunc1.execute(x + 13.5f, y + 13.5f, z + 13.5f) * distortion;
		float ry = abstractNoiseFunc1.execute(x, y, z) * distortion;
		float rz = abstractNoiseFunc1.execute(x - 13.5f, y - 13.5f, z - 13.5f) * distortion;
		return abstractNoiseFunc2.executeS(x + rx, y + ry, z + rz); //distorted-domain noise
	}

	public void mgMFractalOrfBmTex(Structure tex, float[] texvec, ColorBand colorBand, TexResult texres, DataRepository dataRepository) {
		int stype = ((Number)tex.getFieldValue("stype")).intValue();
		float nsOutscale = ((Number)tex.getFieldValue("ns_outscale")).floatValue();
		float nabla = ((Number)tex.getFieldValue("nabla")).floatValue();
		float noisesize = ((Number)tex.getFieldValue("noisesize")).floatValue();
		float contrast = ((Number)tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number)tex.getFieldValue("bright")).floatValue();

		IMusgraveFunction mgravefunc = stype == TEX_MFRACTAL ? musgraveFunctions.get(Integer.valueOf(stype)) : musgraveFunctions.get(Integer.valueOf(TEX_FBM));

		texres.tin = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1], texvec[2]);
		if(texres.nor != null) {
			float offs = nabla / noisesize; // also scaling of texvec
			// calculate bumpnormal
			texres.nor[0] = nsOutscale * mgravefunc.execute(tex, texvec[0] + offs, texvec[1], texvec[2]);
			texres.nor[1] = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1] + offs, texvec[2]);
			texres.nor[2] = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1], texvec[2] + offs);
			this.texNormalDerivate(colorBand, texres, dataRepository);
		}
		this.brightnesAndContrast(texres, contrast, brightness);
	}

	public void mgRidgedOrHybridMFTex(Structure tex, float[] texvec, ColorBand colorBand, TexResult texres, DataRepository dataRepository) {
		int stype = ((Number)tex.getFieldValue("stype")).intValue();
		float nsOutscale = ((Number)tex.getFieldValue("ns_outscale")).floatValue();
		float nabla = ((Number)tex.getFieldValue("nabla")).floatValue();
		float noisesize = ((Number)tex.getFieldValue("noisesize")).floatValue();
		float contrast = ((Number)tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number)tex.getFieldValue("bright")).floatValue();

		IMusgraveFunction mgravefunc = stype == TEX_RIDGEDMF ? musgraveFunctions.get(Integer.valueOf(stype)) : musgraveFunctions.get(Integer.valueOf(TEX_HYBRIDMF));

		texres.tin = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1], texvec[2]);
		if(texres.nor != null) {
			float offs = nabla / noisesize; // also scaling of texvec
			// calculate bumpnormal
			texres.nor[0] = nsOutscale * mgravefunc.execute(tex, texvec[0] + offs, texvec[1], texvec[2]);
			texres.nor[1] = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1] + offs, texvec[2]);
			texres.nor[2] = nsOutscale * mgravefunc.execute(tex, texvec[0], texvec[1], texvec[2] + offs);
			this.texNormalDerivate(colorBand, texres, dataRepository);
		}
		this.brightnesAndContrast(texres, contrast, brightness);
	}

	public void mgHTerrainTex(Structure tex, float[] texvec, ColorBand colorBand, TexResult texres, DataRepository dataRepository) {
		float nsOutscale = ((Number)tex.getFieldValue("ns_outscale")).floatValue();
		float nabla = ((Number)tex.getFieldValue("nabla")).floatValue();
		float noisesize = ((Number)tex.getFieldValue("noisesize")).floatValue();
		float contrast = ((Number)tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number)tex.getFieldValue("bright")).floatValue();

		IMusgraveFunction musgraveFunction = musgraveFunctions.get(Integer.valueOf(TEX_HTERRAIN));
		texres.tin = nsOutscale * musgraveFunction.execute(tex, texvec[0], texvec[1], texvec[2]);
		if(texres.nor != null) {
			float offs = nabla / noisesize; // also scaling of texvec
			// calculate bumpnormal
			texres.nor[0] = nsOutscale * musgraveFunction.execute(tex, texvec[0] + offs, texvec[1], texvec[2]);
			texres.nor[1] = nsOutscale * musgraveFunction.execute(tex, texvec[0], texvec[1] + offs, texvec[2]);
			texres.nor[2] = nsOutscale * musgraveFunction.execute(tex, texvec[0], texvec[1], texvec[2] + offs);
			this.texNormalDerivate(colorBand, texres, dataRepository);
		}
		this.brightnesAndContrast(texres, contrast, brightness);
	}

	/**
	 * This class is abstract to the noise functions computations. It has two methods. One calculates the Signed (with
	 * 'S' at the end) and the other Unsigned value.
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static abstract class AbstractNoiseFunc {
		/**
		 * This method calculates the unsigned value of the noise.
		 * @param x
		 *        the x texture coordinate
		 * @param y
		 *        the y texture coordinate
		 * @param z
		 *        the z texture coordinate
		 * @return value of the noise
		 */
		public abstract float execute(float x, float y, float z);

		/**
		 * This method calculates the signed value of the noise.
		 * @param x
		 *        the x texture coordinate
		 * @param y
		 *        the y texture coordinate
		 * @param z
		 *        the z texture coordinate
		 * @return value of the noise
		 */
		public abstract float executeS(float x, float y, float z);

		/*
		 * Not 'pure' Worley, but the results are virtually the same. Returns distances in da and point coords in pa
		 */
		protected static void voronoi(float x, float y, float z, float[] da, float[] pa, float me, int dtype) {
			float xd, yd, zd, d, p[];

			IDistanceFunc distanceFunc = distanceFunctions.get(Integer.valueOf(dtype));
			if(distanceFunc == null) {
				distanceFunc = distanceFunctions.get(Integer.valueOf(0));
			}

			int xi = (int)FastMath.floor(x);
			int yi = (int)FastMath.floor(y);
			int zi = (int)FastMath.floor(z);
			da[0] = da[1] = da[2] = da[3] = 1e10f;
			for(int xx = xi - 1; xx <= xi + 1; ++xx) {
				for(int yy = yi - 1; yy <= yi + 1; ++yy) {
					for(int zz = zi - 1; zz <= zi + 1; ++zz) {
						p = AbstractNoiseFunc.hashPoint(xx, yy, zz);
						xd = x - (p[0] + xx);
						yd = y - (p[1] + yy);
						zd = z - (p[2] + zz);
						d = distanceFunc.execute(xd, yd, zd, me);
						if(d < da[0]) {
							da[3] = da[2];
							da[2] = da[1];
							da[1] = da[0];
							da[0] = d;
							pa[9] = pa[6];
							pa[10] = pa[7];
							pa[11] = pa[8];
							pa[6] = pa[3];
							pa[7] = pa[4];
							pa[8] = pa[5];
							pa[3] = pa[0];
							pa[4] = pa[1];
							pa[5] = pa[2];
							pa[0] = p[0] + xx;
							pa[1] = p[1] + yy;
							pa[2] = p[2] + zz;
						} else if(d < da[1]) {
							da[3] = da[2];
							da[2] = da[1];
							da[1] = d;
							pa[9] = pa[6];
							pa[10] = pa[7];
							pa[11] = pa[8];
							pa[6] = pa[3];
							pa[7] = pa[4];
							pa[8] = pa[5];
							pa[3] = p[0] + xx;
							pa[4] = p[1] + yy;
							pa[5] = p[2] + zz;
						} else if(d < da[2]) {
							da[3] = da[2];
							da[2] = d;
							pa[9] = pa[6];
							pa[10] = pa[7];
							pa[11] = pa[8];
							pa[6] = p[0] + xx;
							pa[7] = p[1] + yy;
							pa[8] = p[2] + zz;
						} else if(d < da[3]) {
							da[3] = d;
							pa[9] = p[0] + xx;
							pa[10] = p[1] + yy;
							pa[11] = p[2] + zz;
						}
					}
				}
			}
		}

		// #define HASHVEC(x,y,z) hashvectf+3*hash[ (hash[ (hash[(z) & 255]+(y)) & 255]+(x)) & 255]

		/* needed for voronoi */
		// #define HASHPNT(x,y,z) hashpntf+3*hash[ (hash[ (hash[(z) & 255]+(y)) & 255]+(x)) & 255]
		protected static float[] hashPoint(int x, int y, int z) {
			float[] result = new float[3];
			result[0] = hashpntf[3 * hash[hash[hash[z & 255] + y & 255] + x & 255]];
			result[1] = hashpntf[3 * hash[hash[hash[z & 255] + y & 255] + x & 255] + 1];
			result[2] = hashpntf[3 * hash[hash[hash[z & 255] + y & 255] + x & 255] + 2];
			return result;
		}

		// #define setup(i,b0,b1,r0,r1) \
		// t = vec[i] + 10000.; \
		// b0 = ((int)t) & 255; \
		// b1 = (b0+1) & 255; \
		// r0 = t - (int)t; \
		// r1 = r0 - 1.;

		// vec[3]
		public float noise3Perlin(float[] vec) {
			int bx0, bx1, by0, by1, bz0, bz1, b00, b10, b01, b11;
			float rx0, rx1, ry0, ry1, rz0, rz1, sx, sy, sz, a, b, c, d, t, u, v;
			int i, j;

			// setup(0, bx0,bx1, rx0,rx1);
			t = vec[0] + 10000.0f;
			bx0 = (int)t & 255;
			bx1 = bx0 + 1 & 255;
			rx0 = t - (int)t;
			rx1 = rx0 - 1.0f;
			// setup(1, by0,by1, ry0,ry1);
			t = vec[0] + 10000.0f;
			by0 = (int)t & 255;
			by1 = by0 + 1 & 255;
			ry0 = t - (int)t;
			ry1 = ry0 - 1.0f;
			// setup(2, bz0,bz1, rz0,rz1);
			t = vec[0] + 10000.0f;
			bz0 = (int)t & 255;
			bz1 = bz0 + 1 & 255;
			rz0 = t - (int)t;
			rz1 = rz0 - 1.0f;

			i = p[bx0];
			j = p[bx1];

			b00 = p[i + by0];
			b10 = p[j + by0];
			b01 = p[i + by1];
			b11 = p[j + by1];

			/* lerp moved to improved perlin above */

			sx = this.surve(rx0);
			sy = this.surve(ry0);
			sz = this.surve(rz0);

			float[] q = new float[3];
			q = g[b00 + bz0];
			u = this.at(rx0, ry0, rz0, q);
			q = g[b10 + bz0];
			v = this.at(rx1, ry0, rz0, q);
			a = this.lerp(sx, u, v);

			q = g[b01 + bz0];
			u = this.at(rx0, ry1, rz0, q);
			q = g[b11 + bz0];
			v = this.at(rx1, ry1, rz0, q);
			b = this.lerp(sx, u, v);

			c = this.lerp(sy, a, b); /* interpolate in y at lo x */

			q = g[b00 + bz1];
			u = this.at(rx0, ry0, rz1, q);
			q = g[b10 + bz1];
			v = this.at(rx1, ry0, rz1, q);
			a = this.lerp(sx, u, v);

			q = g[b01 + bz1];
			u = this.at(rx0, ry1, rz1, q);
			q = g[b11 + bz1];
			v = this.at(rx1, ry1, rz1, q);
			b = this.lerp(sx, u, v);

			d = this.lerp(sy, a, b); /* interpolate in y at hi x */

			return 1.5f * this.lerp(sz, c, d); /* interpolate in z */
		}

		public float orgBlenderNoise(float x, float y, float z) {
			float cn1, cn2, cn3, cn4, cn5, cn6, i;
			float ox, oy, oz, jx, jy, jz;
			float n = 0.5f;
			int ix, iy, iz, b00, b01, b10, b11, b20, b21;

			ox = x - (ix = (int)Math.floor(x));
			oy = y - (iy = (int)Math.floor(y));
			oz = z - (iz = (int)Math.floor(z));

			jx = ox - 1;
			jy = oy - 1;
			jz = oz - 1;

			cn1 = ox * ox;
			cn2 = oy * oy;
			cn3 = oz * oz;
			cn4 = jx * jx;
			cn5 = jy * jy;
			cn6 = jz * jz;

			cn1 = 1.0f - 3.0f * cn1 + 2.0f * cn1 * ox;
			cn2 = 1.0f - 3.0f * cn2 + 2.0f * cn2 * oy;
			cn3 = 1.0f - 3.0f * cn3 + 2.0f * cn3 * oz;
			cn4 = 1.0f - 3.0f * cn4 - 2.0f * cn4 * jx;
			cn5 = 1.0f - 3.0f * cn5 - 2.0f * cn5 * jy;
			cn6 = 1.0f - 3.0f * cn6 - 2.0f * cn6 * jz;

			b00 = hash[hash[ix & 255] + (iy & 255)];
			b10 = hash[hash[ix + 1 & 255] + (iy & 255)];
			b01 = hash[hash[ix & 255] + (iy + 1 & 255)];
			b11 = hash[hash[ix + 1 & 255] + (iy + 1 & 255)];

			b20 = iz & 255;
			b21 = iz + 1 & 255;

			/* 0 */
			i = cn1 * cn2 * cn3;
			int hIndex = 3 * hash[b20 + b00];
			n += i * (hashvectf[hIndex] * ox + hashvectf[hIndex + 1] * oy + hashvectf[hIndex + 2] * oz);
			/* 1 */
			i = cn1 * cn2 * cn6;
			hIndex = 3 * hash[b21 + b00];
			n += i * (hashvectf[hIndex] * ox + hashvectf[hIndex + 1] * oy + hashvectf[hIndex + 2] * jz);
			/* 2 */
			i = cn1 * cn5 * cn3;
			hIndex = 3 * hash[b20 + b01];
			n += i * (hashvectf[hIndex] * ox + hashvectf[hIndex + 1] * jy + hashvectf[hIndex + 2] * oz);
			/* 3 */
			i = cn1 * cn5 * cn6;
			hIndex = 3 * hash[b21 + b01];
			n += i * (hashvectf[hIndex] * ox + hashvectf[hIndex + 1] * jy + hashvectf[hIndex + 2] * jz);
			/* 4 */
			i = cn4 * cn2 * cn3;
			hIndex = 3 * hash[b20 + b10];
			n += i * (hashvectf[hIndex] * jx + hashvectf[hIndex + 1] * oy + hashvectf[hIndex + 2] * oz);
			/* 5 */
			i = cn4 * cn2 * cn6;
			hIndex = 3 * hash[b21 + b10];
			n += i * (hashvectf[hIndex] * jx + hashvectf[hIndex + 1] * oy + hashvectf[hIndex + 2] * jz);
			/* 6 */
			i = cn4 * cn5 * cn3;
			hIndex = 3 * hash[b20 + b11];
			n += i * (hashvectf[hIndex] * jx + hashvectf[hIndex + 1] * jy + hashvectf[hIndex + 2] * oz);
			/* 7 */
			i = cn4 * cn5 * cn6;
			hIndex = 3 * hash[b21 + b11];
			n += i * (hashvectf[hIndex] * jx + hashvectf[hIndex + 1] * jy + hashvectf[hIndex + 2] * jz);

			if(n < 0.0f) {
				n = 0.0f;
			} else if(n > 1.0f) {
				n = 1.0f;
			}
			return n;
		}

		/* instead of adding another permutation array, just use hash table defined above */
		public float newPerlin(float x, float y, float z) {
			int A, AA, AB, B, BA, BB;
			float u = (float)Math.floor(x), v = (float)Math.floor(y), w = (float)Math.floor(z);
			int X = (int)u & 255, Y = (int)v & 255, Z = (int)w & 255; // FIND UNIT CUBE THAT CONTAINS POINT
			x -= u; // FIND RELATIVE X,Y,Z
			y -= v; // OF POINT IN CUBE.
			z -= w;
			u = this.npfade(x); // COMPUTE FADE CURVES
			v = this.npfade(y); // FOR EACH OF X,Y,Z.
			w = this.npfade(z);
			A = hash[X] + Y;
			AA = hash[A] + Z;
			AB = hash[A + 1] + Z; // HASH COORDINATES OF
			B = hash[X + 1] + Y;
			BA = hash[B] + Z;
			BB = hash[B + 1] + Z; // THE 8 CUBE CORNERS,
			return this.lerp(w, this.lerp(v, this.lerp(u, this.grad(hash[AA], x, y, z), // AND ADD
					this.grad(hash[BA], x - 1, y, z)), // BLENDED
					this.lerp(u, this.grad(hash[AB], x, y - 1, z), // RESULTS
							this.grad(hash[BB], x - 1, y - 1, z))),// FROM 8
					this.lerp(v, this.lerp(u, this.grad(hash[AA + 1], x, y, z - 1), // CORNERS
							this.grad(hash[BA + 1], x - 1, y, z - 1)), // OF CUBE
							this.lerp(u, this.grad(hash[AB + 1], x, y - 1, z - 1), this.grad(hash[BB + 1], x - 1, y - 1, z - 1))));
		}

		/**
		 * Returns a vector/point/color in ca, using point hasharray directly
		 */
		protected static void cellNoiseV(float x, float y, float z, float[] ca) {
			int xi = (int)Math.floor(x);
			int yi = (int)Math.floor(y);
			int zi = (int)Math.floor(z);
			float[] p = AbstractNoiseFunc.hashPoint(xi, yi, zi);
			ca[0] = p[0];
			ca[1] = p[1];
			ca[2] = p[2];
		}

		protected float lerp(float t, float a, float b) {
			return a + t * (b - a);
		}

		protected float npfade(float t) {
			return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
		}

		protected float grad(int hash, float x, float y, float z) {
			int h = hash & 0x0F; // CONVERT LO 4 BITS OF HASH CODE
			float u = h < 8 ? x : y, // INTO 12 GRADIENT DIRECTIONS.
			v = h < 4 ? y : h == 12 || h == 14 ? x : z;
			return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
		}

		/**
		 * Dot product of two vectors.
		 * @param a
		 *        the first vector
		 * @param b
		 *        the second vector
		 * @return the dot product of two vectors
		 */
		protected float dot(float[] a, float[] b) {
			return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
		}

		protected float surve(float t) {
			return t * t * (3.0f - 2.0f * t);
		}

		protected float at(float rx, float ry, float rz, float[] q) {
			return rx * q[0] + ry * q[1] + rz * q[2];
		}
	}

	/**
	 * This interface is used for distance calculation classes. Distance metrics for voronoi. e parameter only used in
	 * Minkovsky.
	 */
	interface IDistanceFunc {
		/**
		 * This method calculates the distance for voronoi algorithms.
		 * @param x
		 *        the x coordinate
		 * @param y
		 *        the y coordinate
		 * @param z
		 *        the z coordinate
		 * @param e
		 *        this parameter used in Monkovsky (no idea what it really is ;)
		 * @return
		 */
		float execute(float x, float y, float z, float e);
	}

	interface IMusgraveFunction {
		float execute(Structure tex, float x, float y, float z);
	}
}
