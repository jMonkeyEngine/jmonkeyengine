/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.textures.generating;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.textures.generating.TextureGeneratorMusgrave.MusgraveData;

/**
 * This generator is responsible for creating various noises used to create
 * generated textures loaded from blender.
 * It is only used by TextureHelper.
 * Take note that these functions are not thread safe.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class NoiseGenerator {
    private static final Logger LOGGER       = Logger.getLogger(NoiseGenerator.class.getName());

    // tex->stype
    protected static final int  TEX_PLASTIC  = 0;
    protected static final int  TEX_WALLIN   = 1;
    protected static final int  TEX_WALLOUT  = 2;

    // musgrave stype
    protected static final int  TEX_MFRACTAL = 0;
    protected static final int  TEX_RIDGEDMF = 1;
    protected static final int  TEX_HYBRIDMF = 2;
    protected static final int  TEX_FBM      = 3;
    protected static final int  TEX_HTERRAIN = 4;

    // keyblock->type
    protected static final int  KEY_LINEAR   = 0;
    protected static final int  KEY_CARDINAL = 1;
    protected static final int  KEY_BSPLINE  = 2;

    // CONSTANTS (read from file)
    protected static float[]    hashpntf;
    protected static short[]    hash;
    protected static float[]    hashvectf;
    protected static short[]    p;
    protected static float[][]  g;

    /**
     * Constructor. Loads the constants needed for computations. They are exactly like the ones the blender uses. Each
     * deriving class should override this method and load its own constraints.
     */
    public NoiseGenerator() {
        LOGGER.fine("Loading noise constants.");
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

    protected static Map<Integer, NoiseFunction>    noiseFunctions    = new HashMap<Integer, NoiseFunction>();
    static {
        noiseFunctions.put(Integer.valueOf(0), new NoiseFunction() {
            // originalBlenderNoise
            public float execute(float x, float y, float z) {
                return NoiseFunctions.originalBlenderNoise(x, y, z);
            }

            public float executeSigned(float x, float y, float z) {
                return 2.0f * NoiseFunctions.originalBlenderNoise(x, y, z) - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(1), new NoiseFunction() {
            // orgPerlinNoise
            public float execute(float x, float y, float z) {
                return 0.5f + 0.5f * NoiseFunctions.noise3Perlin(x, y, z);
            }

            public float executeSigned(float x, float y, float z) {
                return NoiseFunctions.noise3Perlin(x, y, z);
            }
        });
        noiseFunctions.put(Integer.valueOf(2), new NoiseFunction() {
            // newPerlin
            public float execute(float x, float y, float z) {
                return 0.5f + 0.5f * NoiseFunctions.newPerlin(x, y, z);
            }

            public float executeSigned(float x, float y, float z) {
                return this.execute(x, y, z);
            }
        });
        noiseFunctions.put(Integer.valueOf(3), new NoiseFunction() {
            private final float[] da = new float[4];
            private final float[] pa = new float[12];

            // voronoi_F1
            public float execute(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return da[0];
            }

            public float executeSigned(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return 2.0f * da[0] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(4), new NoiseFunction() {
            private final float[] da = new float[4];
            private final float[] pa = new float[12];

            // voronoi_F2
            public float execute(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return da[1];
            }

            public float executeSigned(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return 2.0f * da[1] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(5), new NoiseFunction() {
            private final float[] da = new float[4];
            private final float[] pa = new float[12];

            // voronoi_F3
            public float execute(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return da[2];
            }

            public float executeSigned(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return 2.0f * da[2] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(6), new NoiseFunction() {
            private final float[] da = new float[4];
            private final float[] pa = new float[12];

            // voronoi_F4
            public float execute(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return da[3];
            }

            public float executeSigned(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return 2.0f * da[3] - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(7), new NoiseFunction() {
            private final float[] da = new float[4];
            private final float[] pa = new float[12];

            // voronoi_F1F2
            public float execute(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return da[1] - da[0];
            }

            public float executeSigned(float x, float y, float z) {
                NoiseFunctions.voronoi(x, y, z, da, pa, 1, NATURAL_DISTANCE_FUNCTION);
                return 2.0f * (da[1] - da[0]) - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(8), new NoiseFunction() {
            private final NoiseFunction voronoiF1F2NoiseFunction = noiseFunctions.get(Integer.valueOf(7));

            // voronoi_Cr
            public float execute(float x, float y, float z) {
                float t = 10 * voronoiF1F2NoiseFunction.execute(x, y, z);
                return t > 1.0f ? 1.0f : t;
            }

            public float executeSigned(float x, float y, float z) {
                float t = 10.0f * voronoiF1F2NoiseFunction.execute(x, y, z);
                return t > 1.0f ? 1.0f : 2.0f * t - 1.0f;
            }
        });
        noiseFunctions.put(Integer.valueOf(14), new NoiseFunction() {
            // cellNoise
            public float execute(float x, float y, float z) {
                int xi = (int) FastMath.floor(x);
                int yi = (int) FastMath.floor(y);
                int zi = (int) FastMath.floor(z);
                long n = xi + yi * 1301 + zi * 314159;
                n ^= n << 13;
                return (n * (n * n * 15731 + 789221) + 1376312589) / 4294967296.0f;
            }

            public float executeSigned(float x, float y, float z) {
                return 2.0f * this.execute(x, y, z) - 1.0f;
            }
        });
    }
    /** Distance metrics for voronoi. e parameter only used in Minkovsky. */
    protected static Map<Integer, DistanceFunction> distanceFunctions = new HashMap<Integer, NoiseGenerator.DistanceFunction>();
    private static DistanceFunction                 NATURAL_DISTANCE_FUNCTION;                                                   // often used in noise functions, se I store it here to avoid fetching it every time
    static {
        distanceFunctions.put(Integer.valueOf(0), new DistanceFunction() {
            // real distance
            public float execute(float x, float y, float z, float e) {
                return (float) Math.sqrt(x * x + y * y + z * z);
            }
        });
        distanceFunctions.put(Integer.valueOf(1), new DistanceFunction() {
            // distance squared
            public float execute(float x, float y, float z, float e) {
                return x * x + y * y + z * z;
            }
        });
        distanceFunctions.put(Integer.valueOf(2), new DistanceFunction() {
            // manhattan/taxicab/cityblock distance
            public float execute(float x, float y, float z, float e) {
                return FastMath.abs(x) + FastMath.abs(y) + FastMath.abs(z);
            }
        });
        distanceFunctions.put(Integer.valueOf(3), new DistanceFunction() {
            // Chebychev
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
            public float execute(float x, float y, float z, float e) {
                float d = (float) (Math.sqrt(FastMath.abs(x)) + Math.sqrt(FastMath.abs(y)) + Math.sqrt(FastMath.abs(z)));
                return d * d;
            }
        });
        distanceFunctions.put(Integer.valueOf(5), new DistanceFunction() {
            // Minkovsky, preset exponent 0.25 (Minkovsky4)
            public float execute(float x, float y, float z, float e) {
                x *= x;
                y *= y;
                z *= z;
                return (float) Math.sqrt(Math.sqrt(x * x + y * y + z * z));
            }
        });
        distanceFunctions.put(Integer.valueOf(6), new DistanceFunction() {
            // Minkovsky, general case
            public float execute(float x, float y, float z, float e) {
                return (float) Math.pow(Math.pow(FastMath.abs(x), e) + Math.pow(FastMath.abs(y), e) + Math.pow(FastMath.abs(z), e), 1.0f / e);
            }
        });
        NATURAL_DISTANCE_FUNCTION = distanceFunctions.get(Integer.valueOf(0));
    }

    protected static Map<Integer, MusgraveFunction> musgraveFunctions = new HashMap<Integer, NoiseGenerator.MusgraveFunction>();
    static {
        musgraveFunctions.put(Integer.valueOf(TEX_MFRACTAL), new MusgraveFunction() {

            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float rmd, value = 1.0f, pwr = 1.0f, pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);

                for (int i = 0; i < (int) musgraveData.octaves; ++i) {
                    value *= pwr * musgraveData.noiseFunction.executeSigned(x, y, z) + 1.0f;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }
                rmd = musgraveData.octaves - FastMath.floor(musgraveData.octaves);
                if (rmd != 0.0f) {
                    value *= rmd * musgraveData.noiseFunction.executeSigned(x, y, z) * pwr + 1.0f;
                }
                return value;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_RIDGEDMF), new MusgraveFunction() {

            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float result, signal, weight;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;

                signal = musgraveData.offset - FastMath.abs(musgraveData.noiseFunction.executeSigned(x, y, z));
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
                    signal = musgraveData.offset - FastMath.abs(musgraveData.noiseFunction.executeSigned(x, y, z));
                    signal *= signal;
                    signal *= weight;
                    result += signal * pwr;
                    pwr *= pwHL;
                }
                return result;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_HYBRIDMF), new MusgraveFunction() {

            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float result, signal, weight, rmd;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;

                result = musgraveData.noiseFunction.executeSigned(x, y, z) + musgraveData.offset;
                weight = musgraveData.gain * result;
                x *= musgraveData.lacunarity;
                y *= musgraveData.lacunarity;
                z *= musgraveData.lacunarity;

                for (int i = 1; weight > 0.001f && i < (int) musgraveData.octaves; ++i) {
                    if (weight > 1.0f) {
                        weight = 1.0f;
                    }
                    signal = (musgraveData.noiseFunction.executeSigned(x, y, z) + musgraveData.offset) * pwr;
                    pwr *= pwHL;
                    result += weight * signal;
                    weight *= musgraveData.gain * signal;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = musgraveData.octaves - FastMath.floor(musgraveData.octaves);
                if (rmd != 0.0f) {
                    result += rmd * (musgraveData.noiseFunction.executeSigned(x, y, z) + musgraveData.offset) * pwr;
                }
                return result;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_FBM), new MusgraveFunction() {

            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float rmd, value = 0.0f, pwr = 1.0f, pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);

                for (int i = 0; i < (int) musgraveData.octaves; ++i) {
                    value += musgraveData.noiseFunction.executeSigned(x, y, z) * pwr;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = musgraveData.octaves - FastMath.floor(musgraveData.octaves);
                if (rmd != 0.f) {
                    value += rmd * musgraveData.noiseFunction.executeSigned(x, y, z) * pwr;
                }
                return value;
            }
        });
        musgraveFunctions.put(Integer.valueOf(TEX_HTERRAIN), new MusgraveFunction() {

            public float execute(MusgraveData musgraveData, float x, float y, float z) {
                float value, increment, rmd;
                float pwHL = (float) Math.pow(musgraveData.lacunarity, -musgraveData.h);
                float pwr = pwHL;

                value = musgraveData.offset + musgraveData.noiseFunction.executeSigned(x, y, z);
                x *= musgraveData.lacunarity;
                y *= musgraveData.lacunarity;
                z *= musgraveData.lacunarity;

                for (int i = 1; i < (int) musgraveData.octaves; ++i) {
                    increment = (musgraveData.noiseFunction.executeSigned(x, y, z) + musgraveData.offset) * pwr * value;
                    value += increment;
                    pwr *= pwHL;
                    x *= musgraveData.lacunarity;
                    y *= musgraveData.lacunarity;
                    z *= musgraveData.lacunarity;
                }

                rmd = musgraveData.octaves - FastMath.floor(musgraveData.octaves);
                if (rmd != 0.0) {
                    increment = (musgraveData.noiseFunction.executeSigned(x, y, z) + musgraveData.offset) * pwr * value;
                    value += rmd * increment;
                }
                return value;
            }
        });
    }

    public static class NoiseFunctions {
        public static float noise(float x, float y, float z, float noiseSize, int noiseDepth, NoiseFunction noiseFunction, boolean isHard) {
            if (noiseSize != 0.0) {
                noiseSize = 1.0f / noiseSize;
                x *= noiseSize;
                y *= noiseSize;
                z *= noiseSize;
            }
            float result = noiseFunction.execute(x, y, z);
            return isHard ? FastMath.abs(2.0f * result - 1.0f) : result;
        }

        public static float turbulence(float x, float y, float z, float noiseSize, int noiseDepth, NoiseFunction noiseFunction, boolean isHard) {
            if (noiseSize != 0.0) {
                noiseSize = 1.0f / noiseSize;
                x *= noiseSize;
                y *= noiseSize;
                z *= noiseSize;
            }

            float sum = 0, t, amp = 1, fscale = 1;
            for (int i = 0; i <= noiseDepth; ++i, amp *= 0.5f, fscale *= 2f) {
                t = noiseFunction.execute(fscale * x, fscale * y, fscale * z);
                if (isHard) {
                    t = FastMath.abs(2.0f * t - 1.0f);
                }
                sum += t * amp;
            }

            sum *= (float) (1 << noiseDepth) / (float) ((1 << noiseDepth + 1) - 1);
            return sum;
        }

        private static final float[] voronoiP = new float[3];

        public static void voronoi(float x, float y, float z, float[] da, float[] pa, float distanceExponent, DistanceFunction distanceFunction) {
            float xd, yd, zd, d;

            int xi = (int) FastMath.floor(x);
            int yi = (int) FastMath.floor(y);
            int zi = (int) FastMath.floor(z);
            da[0] = da[1] = da[2] = da[3] = Float.MAX_VALUE;// 1e10f;
            for (int i = xi - 1; i <= xi + 1; ++i) {
                for (int j = yi - 1; j <= yi + 1; ++j) {
                    for (int k = zi - 1; k <= zi + 1; ++k) {
                        NoiseMath.hash(i, j, k, voronoiP);
                        xd = x - (voronoiP[0] + i);
                        yd = y - (voronoiP[1] + j);
                        zd = z - (voronoiP[2] + k);
                        d = distanceFunction.execute(xd, yd, zd, distanceExponent);
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
                            pa[0] = voronoiP[0] + i;
                            pa[1] = voronoiP[1] + j;
                            pa[2] = voronoiP[2] + k;
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
                            pa[3] = voronoiP[0] + i;
                            pa[4] = voronoiP[1] + j;
                            pa[5] = voronoiP[2] + k;
                        } else if (d < da[2]) {
                            da[3] = da[2];
                            da[2] = d;
                            pa[9] = pa[6];
                            pa[10] = pa[7];
                            pa[11] = pa[8];
                            pa[6] = voronoiP[0] + i;
                            pa[7] = voronoiP[1] + j;
                            pa[8] = voronoiP[2] + k;
                        } else if (d < da[3]) {
                            da[3] = d;
                            pa[9] = voronoiP[0] + i;
                            pa[10] = voronoiP[1] + j;
                            pa[11] = voronoiP[2] + k;
                        }
                    }
                }
            }
        }

        // instead of adding another permutation array, just use hash table defined above
        public static float newPerlin(float x, float y, float z) {
            int A, AA, AB, B, BA, BB;
            float floorX = FastMath.floor(x), floorY = FastMath.floor(y), floorZ = FastMath.floor(z);
            int intX = (int) floorX & 0xFF, intY = (int) floorY & 0xFF, intZ = (int) floorZ & 0xFF;
            x -= floorX;
            y -= floorY;
            z -= floorZ;
            // computing fading curves
            floorX = NoiseMath.npfade(x);
            floorY = NoiseMath.npfade(y);
            floorZ = NoiseMath.npfade(z);
            A = hash[intX] + intY;
            AA = hash[A] + intZ;
            AB = hash[A + 1] + intZ;
            B = hash[intX + 1] + intY;
            BA = hash[B] + intZ;
            BB = hash[B + 1] + intZ;
            return NoiseMath.lerp(floorZ, NoiseMath.lerp(floorY, NoiseMath.lerp(floorX, NoiseMath.grad(hash[AA], x, y, z), NoiseMath.grad(hash[BA], x - 1, y, z)), NoiseMath.lerp(floorX, NoiseMath.grad(hash[AB], x, y - 1, z), NoiseMath.grad(hash[BB], x - 1, y - 1, z))),
                    NoiseMath.lerp(floorY, NoiseMath.lerp(floorX, NoiseMath.grad(hash[AA + 1], x, y, z - 1), NoiseMath.grad(hash[BA + 1], x - 1, y, z - 1)), NoiseMath.lerp(floorX, NoiseMath.grad(hash[AB + 1], x, y - 1, z - 1), NoiseMath.grad(hash[BB + 1], x - 1, y - 1, z - 1))));
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

        private static final float[] cn      = new float[8];
        private static final int[]   b1      = new int[8];
        private static final int[]   b2      = new int[2];
        private static final float[] xFactor = new float[8];
        private static final float[] yFactor = new float[8];
        private static final float[] zFactor = new float[8];

        public static float originalBlenderNoise(float x, float y, float z) {
            float n = 0.5f;

            int ix = (int) FastMath.floor(x);
            int iy = (int) FastMath.floor(y);
            int iz = (int) FastMath.floor(z);

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

            cn[0] = cn1 * cn2 * cn3;
            cn[1] = cn1 * cn2 * cn6;
            cn[2] = cn1 * cn5 * cn3;
            cn[3] = cn1 * cn5 * cn6;
            cn[4] = cn4 * cn2 * cn3;
            cn[5] = cn4 * cn2 * cn6;
            cn[6] = cn4 * cn5 * cn3;
            cn[7] = cn4 * cn5 * cn6;

            b1[0] = b1[1] = hash[hash[ix & 0xFF] + (iy & 0xFF)];
            b1[2] = b1[3] = hash[hash[ix & 0xFF] + (iy + 1 & 0xFF)];
            b1[4] = b1[5] = hash[hash[ix + 1 & 0xFF] + (iy & 0xFF)];
            b1[6] = b1[7] = hash[hash[ix + 1 & 0xFF] + (iy + 1 & 0xFF)];

            b2[0] = iz & 0xFF;
            b2[1] = iz + 1 & 0xFF;

            xFactor[0] = xFactor[1] = xFactor[2] = xFactor[3] = ox;
            xFactor[4] = xFactor[5] = xFactor[6] = xFactor[7] = jx;
            yFactor[0] = yFactor[1] = yFactor[4] = yFactor[5] = oy;
            yFactor[2] = yFactor[3] = yFactor[6] = yFactor[7] = jy;
            zFactor[0] = zFactor[2] = zFactor[4] = zFactor[6] = oz;
            zFactor[1] = zFactor[3] = zFactor[5] = zFactor[7] = jz;

            for (int i = 0; i < cn.length; ++i) {
                int hIndex = 3 * hash[b1[i] + b2[i % 2]];
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
         *            the x texture coordinate
         * @param y
         *            the y texture coordinate
         * @param z
         *            the z texture coordinate
         * @return value of the noise
         */
        float execute(float x, float y, float z);

        /**
         * This method calculates the signed value of the noise.
         * @param x
         *            the x texture coordinate
         * @param y
         *            the y texture coordinate
         * @param z
         *            the z texture coordinate
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

    /**
     * This interface is used for distance calculation classes. Distance metrics for voronoi. e parameter only used in
     * Minkovsky.
     */
    interface DistanceFunction {

        /**
         * This method calculates the distance for voronoi algorithms.
         * @param x
         *            the x coordinate
         * @param y
         *            the y coordinate
         * @param z
         *            the z coordinate
         * @param e
         *            this parameter used in Monkovsky (no idea what it really is ;)
         * @return
         */
        float execute(float x, float y, float z, float e);
    }

    interface MusgraveFunction {

        float execute(MusgraveData musgraveData, float x, float y, float z);
    }
}
