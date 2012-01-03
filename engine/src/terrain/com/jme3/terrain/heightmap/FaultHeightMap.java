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
package com.jme3.terrain.heightmap;

import com.jme3.math.FastMath;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates an heightmap based on the fault algorithm. Each iteration, a random line
 * crossing the map is generated. On one side height values are raised, on the other side
 * lowered.
 * @author cghislai
 */
public class FaultHeightMap extends AbstractHeightMap {

    private static final Logger logger = Logger.getLogger(FaultHeightMap.class.getName());
    /**
     * Values on one side are lowered, on the other side increased,
     * creating a step at the fault line
     */
    public static final int FAULTTYPE_STEP = 0;
    /**
     * Values on one side are lowered, then increase lineary while crossing
     * the fault line to the other side. The fault line will be a inclined
     * plane
     */
    public static final int FAULTTYPE_LINEAR = 1;
    /**
     * Values are lowered on one side, increased on the other, creating a
     * cosine curve on the fault line
     */
    public static final int FAULTTYPE_COSINE = 2;
    /**
     * Value are lowered on both side, but increased on the fault line
     * creating a smooth ridge on the fault line.
     */
    public static final int FAULTTYPE_SINE = 3;
    /**
     * A linear fault is created
     */
    public static final int FAULTSHAPE_LINE = 10;
    /**
     * A circular fault is created.
     */
    public static final int FAULTSHAPE_CIRCLE = 11;
    private long seed; // A seed to feed the random
    private int iterations; // iterations to perform
    private float minFaultHeight; // the height modification applied
    private float maxFaultHeight; // the height modification applied
    private float minRange; // The range for linear and trigo faults
    private float maxRange; // The range for linear and trigo faults
    private float minRadius; // radii for circular fault
    private float maxRadius;
    private int faultType; // The type of fault
    private int faultShape; // The type of fault

    /**
     * Constructor creates the fault. For faulttype other than STEP, a range can
     * be provided. For faultshape circle, min and max radii can be provided.
     * Don't forget to reload the map if you have set parameters after the constructor
     * call.
     * @param size The size of the heightmap
     * @param iterations Iterations to perform
     * @param faultType Type of fault
     * @param faultShape Shape of the fault -line or circle
     * @param minFaultHeight Height modified on each side
     * @param maxFaultHeight Height modified on each side
     * @param seed A seed to feed the Random generator
     * @see setFaultRange, setMinRadius, setMaxRadius
     */
    public FaultHeightMap(int size, int iterations, int faultType, int faultShape, float minFaultHeight, float maxFaultHeight, long seed) throws Exception {
        if (size < 0 || iterations < 0) {
            throw new Exception("Size and iterations must be greater than 0!");
        }
        this.size = size;
        this.iterations = iterations;
        this.faultType = faultType;
        this.faultShape = faultShape;
        this.minFaultHeight = minFaultHeight;
        this.maxFaultHeight = maxFaultHeight;
        this.seed = seed;
        this.minRange = minFaultHeight;
        this.maxRange = maxFaultHeight;
        this.minRadius = size / 10;
        this.maxRadius = size / 4;
        load();
    }

    /**
     * Create an heightmap with linear step faults.
     * @param size size of heightmap
     * @param iterations number of iterations
     * @param minFaultHeight Height modified on each side
     * @param maxFaultHeight Height modified on each side
     */
    public FaultHeightMap(int size, int iterations, float minFaultHeight, float maxFaultHeight) throws Exception {
        this(size, iterations, FAULTTYPE_STEP, FAULTSHAPE_LINE, minFaultHeight, maxFaultHeight, new Random().nextLong());
    }

    @Override
    public boolean load() {
        // clean up data if needed.
        if (null != heightData) {
            unloadHeightMap();
        }
        heightData = new float[size * size];
        float[][] tempBuffer = new float[size][size];
        Random random = new Random(seed);

        for (int i = 0; i < iterations; i++) {
            addFault(tempBuffer, random);
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                setHeightAtPoint(tempBuffer[i][j], i, j);
            }
        }

        normalizeTerrain(NORMALIZE_RANGE);

        logger.log(Level.INFO, "Fault heightmap generated");
        return true;
    }

    protected void addFault(float[][] tempBuffer, Random random) {
        float faultHeight = minFaultHeight + random.nextFloat() * (maxFaultHeight - minFaultHeight);
        float range = minRange + random.nextFloat() * (maxRange - minRange);
        switch (faultShape) {
            case FAULTSHAPE_LINE:
                addLineFault(tempBuffer, random, faultHeight, range);
                break;
            case FAULTSHAPE_CIRCLE:
                addCircleFault(tempBuffer, random, faultHeight, range);
                break;
        }
    }

    protected void addLineFault(float[][] tempBuffer, Random random, float faultHeight, float range) {
        int x1 = random.nextInt(size);
        int x2 = random.nextInt(size);
        int y1 = random.nextInt(size);
        int y2 = random.nextInt(size);


        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float dist = ((x2 - x1) * (j - y1) - (y2 - y1) * (i - x1))
                        / (FastMath.sqrt(FastMath.sqr(x2 - x1) + FastMath.sqr(y2 - y1)));
                tempBuffer[i][j] += calcHeight(dist, random, faultHeight, range);
            }
        }
    }

    protected void addCircleFault(float[][] tempBuffer, Random random, float faultHeight, float range) {
        float radius = random.nextFloat() * (maxRadius - minRadius) + minRadius;
        int intRadius = (int) FastMath.floor(radius);
        // Allox circle center to be out of map if not by more than radius.
        // Unlucky cases will put them in the far corner, with the circle
        // entirely outside heightmap
        int x = random.nextInt(size + 2 * intRadius) - intRadius;
        int y = random.nextInt(size + 2 * intRadius) - intRadius;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float dist;
                if (i != x || j != y) {
                    int dx = i - x;
                    int dy = j - y;
                    float dmag = FastMath.sqrt(FastMath.sqr(dx) + FastMath.sqr(dy));
                    float rx = x + dx / dmag * radius;
                    float ry = y + dy / dmag * radius;
                    dist = FastMath.sign(dmag - radius)
                        * FastMath.sqrt(FastMath.sqr(i - rx) + FastMath.sqr(j - ry));
                } else {
                    dist = 0;
                }
                tempBuffer[i][j] += calcHeight(dist, random, faultHeight, range);
            }
        }
    }

    protected float calcHeight(float dist, Random random, float faultHeight, float range) {
        switch (faultType) {
            case FAULTTYPE_STEP: {
                return FastMath.sign(dist) * faultHeight;
            }
            case FAULTTYPE_LINEAR: {
                if (FastMath.abs(dist) > range) {
                    return FastMath.sign(dist) * faultHeight;
                }
                float f = FastMath.abs(dist) / range;
                return FastMath.sign(dist) * faultHeight * f;
            }
            case FAULTTYPE_SINE: {
                if (FastMath.abs(dist) > range) {
                    return -faultHeight;
                }
                float f = dist / range;
                // We want -1 at f=-1 and f=1; 1 at f=0
                return FastMath.sin((1 + 2 * f) * FastMath.PI / 2) * faultHeight;
            }
            case FAULTTYPE_COSINE: {
                if (FastMath.abs(dist) > range) {
                    return -FastMath.sign(dist) * faultHeight;
                }
                float f = dist / range;
                float val =  FastMath.cos((1 + f) * FastMath.PI / 2) * faultHeight;
                return val;
            }
        }
        //shoudn't go here
        throw new RuntimeException("Code needs update to switch allcases");
    }

    public int getFaultShape() {
        return faultShape;
    }

    public void setFaultShape(int faultShape) {
        this.faultShape = faultShape;
    }

    public int getFaultType() {
        return faultType;
    }

    public void setFaultType(int faultType) {
        this.faultType = faultType;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public float getMaxFaultHeight() {
        return maxFaultHeight;
    }

    public void setMaxFaultHeight(float maxFaultHeight) {
        this.maxFaultHeight = maxFaultHeight;
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
    }

    public float getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    public float getMinFaultHeight() {
        return minFaultHeight;
    }

    public void setMinFaultHeight(float minFaultHeight) {
        this.minFaultHeight = minFaultHeight;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public void setMinRadius(float minRadius) {
        this.minRadius = minRadius;
    }

    public float getMinRange() {
        return minRange;
    }

    public void setMinRange(float minRange) {
        this.minRange = minRange;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
