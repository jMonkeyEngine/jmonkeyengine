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
package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureGeneratorMusgrave.MusgraveData;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This generator is responsible for creating various noises used to create
 * generated textures loaded from blender.
 * It derives from AbstractBlenderHelper but is not stored in blender context.
 * It is only used by TextureHelper.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class NoiseGenerator extends AbstractBlenderHelper {
    private static final Logger LOGGER = Logger.getLogger(NoiseGenerator.class.getName());
    
    // flag
    protected static final int TEX_COLORBAND = 1;
    protected static final int TEX_FLIPBLEND = 2;
    protected static final int TEX_NEGALPHA = 4;
    protected static final int TEX_CHECKER_ODD = 8;
    protected static final int TEX_CHECKER_EVEN = 16;
    protected static final int TEX_PRV_ALPHA = 32;
    protected static final int TEX_PRV_NOR = 64;
    protected static final int TEX_REPEAT_XMIR = 128;
    protected static final int TEX_REPEAT_YMIR = 256;
    protected static final int TEX_FLAG_MASK = TEX_COLORBAND | TEX_FLIPBLEND | TEX_NEGALPHA | TEX_CHECKER_ODD | TEX_CHECKER_EVEN | TEX_PRV_ALPHA | TEX_PRV_NOR | TEX_REPEAT_XMIR | TEX_REPEAT_YMIR;

    // tex->stype
    protected static final int TEX_PLASTIC = 0;
    protected static final int TEX_WALLIN = 1;
    protected static final int TEX_WALLOUT = 2;

    // musgrave stype
    protected static final int TEX_MFRACTAL = 0;
    protected static final int TEX_RIDGEDMF = 1;
    protected static final int TEX_HYBRIDMF = 2;
    protected static final int TEX_FBM = 3;
    protected static final int TEX_HTERRAIN = 4;

    // keyblock->type
    protected static final int KEY_LINEAR = 0;
    protected static final int KEY_CARDINAL = 1;
    protected static final int KEY_BSPLINE = 2;

    // CONSTANTS (read from file)
    protected static float[] hashpntf;
    protected static short[] hash;
    protected static float[] hashvectf;
    protected static short[] p;
    protected static float[][] g;

    /**
     * Constructor. Stores the blender version number and loads the constants needed for computations.
     * @param blenderVersion
     *        the number of blender version
     */
    public NoiseGenerator(String blenderVersion) {
        super(blenderVersion, false);
        this.loadConstants();
    }

    /**
     * This method loads the constants needed for computations. They are exactly like the ones the blender uses. Each
     * deriving class should override this method and load its own constraints. Be carefult with overriding though, if
     * an exception will be thrown the class will not be instantiated.
     */
    protected void loadConstants() {
        InputStream is = NoiseGenerator.class.getResourceAsStream("noiseconstants.dat");
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            hashpntf = (float[]) ois.readObject();
            hash = (short[]) ois.readObject();
            hashvectf = (float[]) ois.readObject();
            p = (short[]) ois.readObject();
            g = (float[][]) ois.readObject();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } catch (ClassNotFoundException e) {
            assert false : "Constants' classes should be arrays of primitive types, so they are ALWAYS known!";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage());
                }
            }
        }
    }
    
    protected static Map<Integer, NoiseFunction> noiseFunctions = new HashMap<Integer, NoiseFunction>();
    static {
        noiseFunctions.put(Integer.valueOf(0), new NoiseFunction() {
        	// originalBlenderNoise
            @Override
            public float execute(float x, float y, float z) {
                return NoiseFunctions.originalBlenderNoise(x, y, z);
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                return 2.0f * NoiseFunctions.originalBlenderNoise(x, y, z) - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(1), new NoiseFunction() {
        	// orgPerlinNoise
            @Override
            public float execute(float x, float y, float z) {
                return 0.5f + 0.5f * NoiseFunctions.noise3Perlin(x, y, z);
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                return NoiseFunctions.noise3Perlin(x, y, z);
            }
        });
        noiseFunctions.put(Integer.valueOf(2), new NoiseFunction() {
        	// newPerlin
            @Override
            public float execute(float x, float y, float z) {
                return 0.5f + 0.5f * NoiseFunctions.newPerlin(x, y, z);
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                return this.execute(x, y, z);
            }
        });
        noiseFunctions.put(Integer.valueOf(3), new NoiseFunction() {
        	// voronoi_F1
            @Override
            public float execute(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return da[0];
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return 2.0f * da[0] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(4), new NoiseFunction() {
        	// voronoi_F2
            @Override
            public float execute(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return da[1];
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return 2.0f * da[1] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(5), new NoiseFunction() {
        	// voronoi_F3
            @Override
            public float execute(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return da[2];
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return 2.0f * da[2] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(6), new NoiseFunction() {
        	// voronoi_F4
            @Override
            public float execute(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return da[3];
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return 2.0f * da[3] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(7), new NoiseFunction() {
        	// voronoi_F1F2
            @Override
            public float execute(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return da[1] - da[0];
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float[] da = new float[4], pa = new float[12];
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, 0);
                return 2.0f * (da[1] - da[0]) - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(8), new NoiseFunction() {
        	// voronoi_Cr
            @Override
            public float execute(float x, float y, float z) {
                float t = 10 * noiseFunctions.get(Integer.valueOf(7)).execute(x, y, z);// voronoi_F1F2
                return t > 1.0f ? 1.0f : t;
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                float t = 10.0f * noiseFunctions.get(Integer.valueOf(7)).execute(x, y, z);// voronoi_F1F2
                return t > 1.0f ? 1.0f : 2.0f * t - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(14), new NoiseFunction() {
        	// cellNoise
            @Override
            public float execute(float x, float y, float z) {
                int xi = (int) Math.floor(x);
                int yi = (int) Math.floor(y);
                int zi = (int) Math.floor(z);
                long n = xi + yi * 1301 + zi * 314159;
                n ^= n << 13;
                return (n * (n * n * 15731 + 789221) + 1376312589) / 4294967296.0f;
            }

            @Override
            public float executeSigned(float x, float y, float z) {
                return 2.0f * this.execute(x, y, z) - 1.0f;
            }
        });
    }
    /** Distance metrics for voronoi. e parameter only used in Minkovsky. */
    protected static Map<Integer, DistanceFunction> distanceFunctions = new HashMap<Integer, NoiseGenerator.DistanceFunction>();

    static {
        distanceFunctions.put(Integer.valueOf(0), new DistanceFunction() {
        	// real distance
            @Override
            public float execute(float x, float y, float z, float e) {
                return (float) Math.sqrt(x * x + y * y + z * z);
            }
        });
        distanceFunctions.put(Integer.valueOf(1), new DistanceFunction() {
        	// distance squared
            @Override
            public float execute(float x, float y, float z, float e) {
                return x * x + y * y + z * z;
            }
        });
        distanceFunctions.put(Integer.valueOf(2), new DistanceFunction() {
        	// manhattan/taxicab/cityblock distance
            @Override
            public float execute(float x, float y, float z, float e) {
                return FastMath.abs(x) + FastMath.abs(y) + FastMath.abs(z);
            }
        });
        distanceFunctions.put(Integer.valueOf(3), new DistanceFunction() {
        	// Chebychev
            @Override
            public float execute(float x, float y, float z, float e) {
                x = FastMath.abs(x);
                y = FastMath.abs(y);
                z = FastMath.abs(z);
                float t = x > y ? x : y;
                return z > t ? z : t;
            }
        });
        distanceFunctions.put(Integer.valueOf(4), new DistanceFunction() {
        	// Minkovsky, preset exponent 0.5 (MinkovskyH)
            @Override
            public float execute(float x, float y, float z, float e) {
                float d = (float) (Math.sqrt(FastMath.abs(x)) + Math.sqrt(FastMath.abs(y)) + Math.sqrt(FastMath.abs(z)));
                return d * d;
            }
        });
        distanceFunctions.put(Integer.valueOf(5), new DistanceFunction() {
        	// Minkovsky, preset exponent 0.25 (Minkovsky4)
            @Override
            public float execute(float x, float y, float z, float e) {
                x *= x;
                y *= y;
                z *= z;
                return (float) Math.sqrt(Math.sqrt(x * x + y * y + z * z));
            }
        });
        distanceFunctions.put(Integer.valueOf(6), new DistanceFunction() {
        	// Minkovsky, general case
            @Override
            public float execute(float x, float y, float z, float e) {
                return (float) Math.pow(Math.pow(FastMath.abs(x), e) + Math.pow(FastMath.abs(y), e) + Math.pow(FastMath.abs(z), e), 1.0f / e);
            }
        });
    }
    
    protected static Map<Integer, MusgraveFunction> musgraveFunctions = new HashMap<Integer, NoiseGenerator.MusgraveFunction>();
    static {
        musgraveFunctions.put(Integer.valueOf(TEX_MFRACTAL), new MusgraveFunction() {

            @Override
            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float rmd, value = 1.0f, pwr = 1.0f, pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(musgraveData.noisebasis));
                if (abstractNoiseFunc == null) {
                    abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
                }

                for (int i = 0; i < (int) musgraveData.octaves; ++i) {
                    value *= pwr * abstractNoiseFunc.executeSigned(x, y, z) + 1.0f;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }
                rmd = (float) (musgraveData.octaves - Math.floor(musgraveData.octaves));
                if (rmd != 0.0f) {
                    value *= rmd * abstractNoiseFunc.executeSigned(x, y, z) * pwr + 1.0f;
                }
                return value;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_RIDGEDMF), new MusgraveFunction() {

            @Override
            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float result, signal, weight;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;

                NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(musgraveData.noisebasis));
                if (abstractNoiseFunc == null) {
                    abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
                }

                signal = musgraveData.offset - FastMath.abs(abstractNoiseFunc.executeSigned(x, y, z));
                signal *= signal;
                result = signal;
                weight = 1.0f;

                for (int i = 1; i < (int) musgraveData.octaves; ++i) {
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                    weight = signal * musgraveData.gain;
                    if (weight > 1.0f) {
                        weight = 1.0f;
                    } else if (weight < 0.0) {
                        weight = 0.0f;
                    }
                    signal = musgraveData.offset - FastMath.abs(abstractNoiseFunc.executeSigned(x, y, z));
                    signal *= signal;
                    signal *= weight;
                    result += signal * pwr;
                    pwr *= pwHL;
                }
                return result;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_HYBRIDMF), new MusgraveFunction() {

            @Override
            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float result, signal, weight, rmd;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;
                NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(musgraveData.noisebasis));
                if (abstractNoiseFunc == null) {
                    abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
                }

                result = abstractNoiseFunc.executeSigned(x, y, z) + musgraveData.offset;
                weight = musgraveData.gain * result;
                x *= musgraveData.lacunarity;
                y *= musgraveData.lacunarity;
                z *= musgraveData.lacunarity;

                for (int i = 1; weight > 0.001f && i < (int) musgraveData.octaves; ++i) {
                    if (weight > 1.0f) {
                        weight = 1.0f;
                    }
                    signal = (abstractNoiseFunc.executeSigned(x, y, z) + musgraveData.offset) * pwr;
                    pwr *= pwHL;
                    result += weight * signal;
                    weight *= musgraveData.gain * signal;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = musgraveData.octaves - (float) Math.floor(musgraveData.octaves);
                if (rmd != 0.0f) {
                    result += rmd * (abstractNoiseFunc.executeSigned(x, y, z) + musgraveData.offset) * pwr;
                }
                return result;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_FBM), new MusgraveFunction() {

            @Override
            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float rmd, value = 0.0f, pwr = 1.0f, pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);

                NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(musgraveData.noisebasis));
                if (abstractNoiseFunc == null) {
                    abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
                }

                for (int i = 0; i < (int) musgraveData.octaves; ++i) {
                    value += abstractNoiseFunc.executeSigned(x, y, z) * pwr;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = (float) (musgraveData.octaves - Math.floor(musgraveData.octaves));
                if (rmd != 0.f) {
                    value += rmd * abstractNoiseFunc.executeSigned(x, y, z) * pwr;
                }
                return value;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_HTERRAIN), new MusgraveFunction() {

            @Override
            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float value, increment, rmd;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;
                NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(musgraveData.noisebasis));
                if (abstractNoiseFunc == null) {
                    abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(0));
                }

                value = musgraveData.offset + abstractNoiseFunc.executeSigned(x, y, z);
                x *= musgraveData.lacunarity;
                y *= musgraveData.lacunarity;
                z *= musgraveData.lacunarity;

                for (int i = 1; i < (int) musgraveData.octaves; ++i) {
                    increment = (abstractNoiseFunc.executeSigned(x, y, z) + musgraveData.offset) * pwr * value;
                    value += increment;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = musgraveData.octaves - (float) Math.floor(musgraveData.octaves);
                if (rmd != 0.0) {
                    increment = (abstractNoiseFunc.executeSigned(x, y, z) + musgraveData.offset) * pwr * value;
                    value += rmd * increment;
                }
                return value;
            }
        });
    }
    
    public static class NoiseFunctions {
    	public static float noise(float x, float y, float z, float noiseSize, int noiseDepth, int noiseBasis, boolean isHard) {
    		NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noiseBasis));
    		if (abstractNoiseFunc == null) {
	            abstractNoiseFunc = noiseFunctions.get(0);
	            noiseBasis = 0;
	        }
    		
    		if (noiseBasis == 0) {
    			++x;
	            ++y;
	            ++z;
	        }

	        if (noiseSize != 0.0) {
	            noiseSize = 1.0f / noiseSize;
	            x *= noiseSize;
	            y *= noiseSize;
	            z *= noiseSize;
	        }
	        float result = abstractNoiseFunc.execute(x, y, z);
	        return isHard ? Math.abs(2.0f * result - 1.0f) : result;
    	}
    	
    	public static float turbulence(float x, float y, float z, float noiseSize, int noiseDepth, int noiseBasis, boolean isHard) {
    		NoiseFunction abstractNoiseFunc = noiseFunctions.get(Integer.valueOf(noiseBasis));
    		if (abstractNoiseFunc == null) {
	            abstractNoiseFunc = noiseFunctions.get(0);
	            noiseBasis = 0;
	        }
    		
    		if (noiseBasis == 0) {
	            ++x;
	            ++y;
	            ++z;
	        }
	        if (noiseSize != 0.0) {
	            noiseSize = 1.0f / noiseSize;
	            x *= noiseSize;
	            y *= noiseSize;
	            z *= noiseSize;
	        }
	        
	        float sum = 0, t, amp = 1, fscale = 1;
	        for (int i = 0; i <= noiseDepth; ++i, amp *= 0.5, fscale *= 2) {
	            t = abstractNoiseFunc.execute(fscale * x, fscale * y, fscale * z);
	            if (isHard) {
	                t = FastMath.abs(2.0f * t - 1.0f);
	            }
	            sum += t * amp;
	        }

	        sum *= (float) (1 << noiseDepth) / (float) ((1 << noiseDepth + 1) - 1);
	        return sum;
    	}
    	
    	/**
         * Not 'pure' Worley, but the results are virtually the same. Returns distances in da and point coords in pa
         */
        public static void voronoi(float x, float y, float z, float[] da, float[] pa, float distanceExponent, int distanceType) {
            float xd, yd, zd, d, p[] = new float[3];

            DistanceFunction distanceFunc = distanceFunctions.get(Integer.valueOf(distanceType));
            if (distanceFunc == null) {
                distanceFunc = distanceFunctions.get(Integer.valueOf(0));
            }

            int xi = (int) FastMath.floor(x);
            int yi = (int) FastMath.floor(y);
            int zi = (int) FastMath.floor(z);
            da[0] = da[1] = da[2] = da[3] = 1e10f;
            for (int i = xi - 1; i <= xi + 1; ++i) {
                for (int j = yi - 1; j <= yi + 1; ++j) {
                    for (int k = zi - 1; k <= zi + 1; ++k) {
                        NoiseMath.hash(i, j, k, p);
                        xd = x - (p[0] + i);
                        yd = y - (p[1] + j);
                        zd = z - (p[2] + k);
                        d = distanceFunc.execute(xd, yd, zd, distanceExponent);
                        if (d < da[0]) {
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
                            pa[0] = p[0] + i;
                            pa[1] = p[1] + j;
                            pa[2] = p[2] + k;
                        } else if (d < da[1]) {
                            da[3] = da[2];
                            da[2] = da[1];
                            da[1] = d;
                            pa[9] = pa[6];
                            pa[10] = pa[7];
                            pa[11] = pa[8];
                            pa[6] = pa[3];
                            pa[7] = pa[4];
                            pa[8] = pa[5];
                            pa[3] = p[0] + i;
                            pa[4] = p[1] + j;
                            pa[5] = p[2] + k;
                        } else if (d < da[2]) {
                            da[3] = da[2];
                            da[2] = d;
                            pa[9] = pa[6];
                            pa[10] = pa[7];
                            pa[11] = pa[8];
                            pa[6] = p[0] + i;
                            pa[7] = p[1] + j;
                            pa[8] = p[2] + k;
                        } else if (d < da[3]) {
                            da[3] = d;
                            pa[9] = p[0] + i;
                            pa[10] = p[1] + j;
                            pa[11] = p[2] + k;
                        }
                    }
                }
            }
        }
        
        // instead of adding another permutation array, just use hash table defined above
        public static float newPerlin(float x, float y, float z) {
            int A, AA, AB, B, BA, BB;
            float floorX = (float) Math.floor(x), floorY = (float) Math.floor(y), floorZ = (float) Math.floor(z);
            int intX = (int) floorX & 0xFF, intY = (int) floorY & 0xFF, intZ = (int) floorZ & 0xFF;
            x -= floorX;
            y -= floorY;
            z -= floorZ;
            //computing fading curves
            floorX = NoiseMath.npfade(x);
            floorY = NoiseMath.npfade(y);
            floorZ = NoiseMath.npfade(z);
            A = hash[intX] + intY;
            AA = hash[A] + intZ;
            AB = hash[A + 1] + intZ;
            B = hash[intX + 1] + intY;
            BA = hash[B] + intZ;
            BB = hash[B + 1] + intZ;
            return  NoiseMath.lerp(floorZ, NoiseMath.lerp(floorY, NoiseMath.lerp(floorX, NoiseMath.grad(hash[AA], x, y, z),
            		NoiseMath.grad(hash[BA], x - 1, y, z)),
            		NoiseMath.lerp(floorX, NoiseMath.grad(hash[AB], x, y - 1, z),
            		NoiseMath.grad(hash[BB], x - 1, y - 1, z))),
            		NoiseMath.lerp(floorY, NoiseMath.lerp(floorX, NoiseMath.grad(hash[AA + 1], x, y, z - 1),
            		NoiseMath.grad(hash[BA + 1], x - 1, y, z - 1)),
            		NoiseMath.lerp(floorX, NoiseMath.grad(hash[AB + 1], x, y - 1, z - 1), 
            		NoiseMath.grad(hash[BB + 1], x - 1, y - 1, z - 1))));
        }

        public static float noise3Perlin(float x, float y, float z) {
            float t = x + 10000.0f;
            int bx0 = (int) t & 0xFF;
            int bx1 = bx0 + 1 & 0xFF;
            float rx0 = t - (int) t;
            float rx1 = rx0 - 1.0f;
            
            t = y + 10000.0f;
            int by0 = (int) t & 0xFF;
            int by1 = by0 + 1 & 0xFF;
            float ry0 = t - (int) t;
            float ry1 = ry0 - 1.0f;
            
            t = z + 10000.0f;
            int bz0 = (int) t & 0xFF;
            int bz1 = bz0 + 1 & 0xFF;
            float rz0 = t - (int) t;
            float rz1 = rz0 - 1.0f;

            int i = p[bx0];
            int j = p[bx1];

            int b00 = p[i + by0];
            int b10 = p[j + by0];
            int b01 = p[i + by1];
            int b11 = p[j + by1];

            float sx = NoiseMath.surve(rx0);
            float sy = NoiseMath.surve(ry0);
            float sz = NoiseMath.surve(rz0);

            float[] q = g[b00 + bz0];
            float u = NoiseMath.at(rx0, ry0, rz0, q);
            q = g[b10 + bz0];
            float v = NoiseMath.at(rx1, ry0, rz0, q);
            float a = NoiseMath.lerp(sx, u, v);

            q = g[b01 + bz0];
            u = NoiseMath.at(rx0, ry1, rz0, q);
            q = g[b11 + bz0];
            v = NoiseMath.at(rx1, ry1, rz0, q);
            float b = NoiseMath.lerp(sx, u, v);

            float c = NoiseMath.lerp(sy, a, b);

            q = g[b00 + bz1];
            u = NoiseMath.at(rx0, ry0, rz1, q);
            q = g[b10 + bz1];
            v = NoiseMath.at(rx1, ry0, rz1, q);
            a = NoiseMath.lerp(sx, u, v);

            q = g[b01 + bz1];
            u = NoiseMath.at(rx0, ry1, rz1, q);
            q = g[b11 + bz1];
            v = NoiseMath.at(rx1, ry1, rz1, q);
            b = NoiseMath.lerp(sx, u, v);

            float d = NoiseMath.lerp(sy, a, b);
            return 1.5f * NoiseMath.lerp(sz, c, d);
        }

        public static float originalBlenderNoise(float x, float y, float z) {
            float n = 0.5f;

            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            
            float ox = x - ix;
            float oy = y - iy;
            float oz = z - iz;

            float jx = ox - 1;
            float jy = oy - 1;
            float jz = oz - 1;

            float cn1 = ox * ox;
            float cn2 = oy * oy;
            float cn3 = oz * oz;
            float cn4 = jx * jx;
            float cn5 = jy * jy;
            float cn6 = jz * jz;

            cn1 = 1.0f - 3.0f * cn1 + 2.0f * cn1 * ox;
            cn2 = 1.0f - 3.0f * cn2 + 2.0f * cn2 * oy;
            cn3 = 1.0f - 3.0f * cn3 + 2.0f * cn3 * oz;
            cn4 = 1.0f - 3.0f * cn4 - 2.0f * cn4 * jx;
            cn5 = 1.0f - 3.0f * cn5 - 2.0f * cn5 * jy;
            cn6 = 1.0f - 3.0f * cn6 - 2.0f * cn6 * jz;
            float[] cn = new float[] {cn1 * cn2 * cn3, cn1 * cn2 * cn6, cn1 * cn5 * cn3, cn1 * cn5 * cn6,
					  cn4 * cn2 * cn3, cn4 * cn2 * cn6, cn4 * cn5 * cn3, cn4 * cn5 * cn6,};
            
            int b00 = hash[hash[ix & 0xFF] + (iy & 0xFF)];
            int b01 = hash[hash[ix & 0xFF] + (iy + 1 & 0xFF)];
            int b10 = hash[hash[ix + 1 & 0xFF] + (iy & 0xFF)];
            int b11 = hash[hash[ix + 1 & 0xFF] + (iy + 1 & 0xFF)];
            int[] b1 = new int[] {b00, b00, b01, b01, b10, b10, b11, b11};
            
            int[] b2 = new int[] {iz & 0xFF, iz + 1 & 0xFF};
            
            float[] xFactor = new float[] {ox, ox, ox, ox, jx, jx, jx, jx};
            float[] yFactor = new float[] {oy, oy, jy, jy, oy, oy, jy, jy};
            float[] zFactor = new float[] {oz, jz, oz, jz, oz, jz, oz, jz};
            
            for(int i=0;i<8;++i) {
            	int hIndex = 3 * hash[b1[i] + b2[i%2]];
            	n += cn[i] * (hashvectf[hIndex] * xFactor[i] + hashvectf[hIndex + 1] * yFactor[i] + hashvectf[hIndex + 2] * zFactor[i]);
            }

            if (n < 0.0f) {
                n = 0.0f;
            } else if (n > 1.0f) {
                n = 1.0f;
            }
            return n;
        }
    }

    /**
     * This class is abstract to the noise functions computations. It has two methods. One calculates the Signed (with
     * 'S' at the end) and the other Unsigned value.
     * @author Marcin Roguski (Kaelthas)
     */
    interface NoiseFunction {

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
        float execute(float x, float y, float z);

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
        float executeSigned(float x, float y, float z);
    }
    
    public static class NoiseMath {
    	public static float lerp(float t, float a, float b) {
            return a + t * (b - a);
        }

    	public static float npfade(float t) {
            return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
        }

    	public static float grad(int hash, float x, float y, float z) {
            int h = hash & 0x0F;
            float u = h < 8 ? x : y, v = h < 4 ? y : h == 12 || h == 14 ? x : z;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

    	public static float surve(float t) {
            return t * t * (3.0f - 2.0f * t);
        }

    	public static float at(float x, float y, float z, float[] q) {
            return x * q[0] + y * q[1] + z * q[2];
        }
    	
    	public static void hash(int x, int y, int z, float[] result) {
            result[0] = hashpntf[3 * hash[hash[hash[z & 0xFF] + y & 0xFF] + x & 0xFF]];
            result[1] = hashpntf[3 * hash[hash[hash[z & 0xFF] + y & 0xFF] + x & 0xFF] + 1];
            result[2] = hashpntf[3 * hash[hash[hash[z & 0xFF] + y & 0xFF] + x & 0xFF] + 2];
        }
    }
    
    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
    	return true;
    }

    /**
     * This interface is used for distance calculation classes. Distance metrics for voronoi. e parameter only used in
     * Minkovsky.
     */
    interface DistanceFunction {

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

    interface MusgraveFunction {

        float execute(MusgraveData musgraveData, float x, float y, float z);
    }
}
