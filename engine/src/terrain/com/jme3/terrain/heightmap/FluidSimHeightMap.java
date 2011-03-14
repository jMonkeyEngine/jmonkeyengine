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
package com.jme3.terrain.heightmap;

import java.util.Random;
import java.util.logging.Logger;

/**
 * <code>FluidSimHeightMap</code> generates a height map based using some
 * sort of fluid simulation. The heightmap is treated as a highly viscous and
 * rubbery fluid enabling to fine tune the generated heightmap using a number
 * of parameters.
 *
 * @author Frederik Boelthoff
 * @see <a href="http://www.gamedev.net/reference/articles/article2001.asp">Terrain Generation Using Fluid Simulation</a>
 * @version $Id$
 *
 */
public class FluidSimHeightMap extends AbstractHeightMap {

    private static final Logger logger = Logger.getLogger(FluidSimHeightMap.class.getName());
    private float waveSpeed = 100.0f;  // speed at which the waves travel
    private float timeStep = 0.033f;  // constant time-step between each iteration
    private float nodeDistance = 10.0f;   // distance between each node of the surface
    private float viscosity = 100.0f; // viscosity of the fluid
    private int iterations;    // number of iterations
    private float minInitialHeight = -500; // min initial height
    private float maxInitialHeight = 500; // max initial height
    private long seed; // the seed for the random number generator
    float coefA, coefB, coefC; // pre-computed coefficients in the fluid equation

    /**
     * Constructor sets the attributes of the hill system and generates the
     * height map. It gets passed a number of tweakable parameters which
     * fine-tune the outcome.
     *
     * @param size
     *            size the size of the terrain to be generated
     * @param iterations
     *            the number of iterations to do
     * @param minInitialHeight
     *                        the minimum initial height of a terrain value
     * @param maxInitialHeight
     *                        the maximum initial height of a terrain value
     * @param viscosity
     *                        the viscosity of the fluid
     * @param waveSpeed
     *                        the speed at which the waves travel
     * @param timestep
     *                        the constant time-step between each iteration
     * @param nodeDistance
     *                        the distance between each node of the heightmap
     * @param seed
     *            the seed to generate the same heightmap again
     * @throws JmeException
     *             if size of the terrain is not greater that zero, or number of
     *             iterations is not greater that zero, or the minimum initial height
     *             is greater than the maximum (or the other way around)
     */
    public FluidSimHeightMap(int size, int iterations, float minInitialHeight, float maxInitialHeight, float viscosity, float waveSpeed, float timestep, float nodeDistance, long seed) throws Exception {
        if (size <= 0 || iterations <= 0 || minInitialHeight >= maxInitialHeight) {
            throw new Exception(
                    "Either size of the terrain is not greater that zero, "
                    + "or number of iterations is not greater that zero, "
                    + "or minimum height greater or equal as the maximum, "
                    + "or maximum height smaller or equal as the minimum.");
        }

        this.size = size;
        this.seed = seed;
        this.iterations = iterations;
        this.minInitialHeight = minInitialHeight;
        this.maxInitialHeight = maxInitialHeight;
        this.viscosity = viscosity;
        this.waveSpeed = waveSpeed;
        this.timeStep = timestep;
        this.nodeDistance = nodeDistance;

        load();
    }

    /**
     * Constructor sets the attributes of the hill system and generates the
     * height map.
     *
     * @param size
     *            size the size of the terrain to be generated
     * @param iterations
     *            the number of iterations to do
     * @throws JmeException
     *             if size of the terrain is not greater that zero, or number of
     *             iterations is not greater that zero
     */
    public FluidSimHeightMap(int size, int iterations) throws Exception {
        if (size <= 0 || iterations <= 0) {
            throw new Exception(
                    "Either size of the terrain is not greater that zero, "
                    + "or number of iterations is not greater that zero");
        }

        this.size = size;
        this.iterations = iterations;

        load();
    }


    /*
     * Generates a heightmap using fluid simulation and the attributes set by
     * the constructor or the setters.
     */
    public boolean load() {
        // Clean up data if needed.
        if (null != heightData) {
            unloadHeightMap();
        }

        heightData = new float[size * size];
        float[][] tempBuffer = new float[2][size * size];
        Random random = new Random(seed);

        // pre-compute the coefficients in the fluid equation
        coefA = (4 - (8 * waveSpeed * waveSpeed * timeStep * timeStep) / (nodeDistance * nodeDistance)) / (viscosity * timeStep + 2);
        coefB = (viscosity * timeStep - 2) / (viscosity * timeStep + 2);
        coefC = ((2 * waveSpeed * waveSpeed * timeStep * timeStep) / (nodeDistance * nodeDistance)) / (viscosity * timeStep + 2);

        // initialize the heightmaps to random values except for the edges
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                tempBuffer[0][j + i * size] = tempBuffer[1][j + i * size] = randomRange(random, minInitialHeight, maxInitialHeight);
            }
        }

        int curBuf = 0;
        int ind;

        float[] oldBuffer;
        float[] newBuffer;

        // Iterate over the heightmap, applying the fluid simulation equation.
        // Although it requires knowledge of the two previous timesteps, it only
        // accesses one pixel of the k-1 timestep, so using a simple trick we only
        // need to store the heightmap twice, not three times, and we can avoid
        // copying data every iteration.
        for (int i = 0; i < iterations; i++) {
            oldBuffer = tempBuffer[1 - curBuf];
            newBuffer = tempBuffer[curBuf];

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    ind = x + y * size;
                    float neighborsValue = 0;
                    int neighbors = 0;

                    if (x > 0) {
                        neighborsValue += newBuffer[ind - 1];
                        neighbors++;
                    }
                    if (x < size - 1) {
                        neighborsValue += newBuffer[ind + 1];
                        neighbors++;
                    }
                    if (y > 0) {
                        neighborsValue += newBuffer[ind - size];
                        neighbors++;
                    }
                    if (y < size - 1) {
                        neighborsValue += newBuffer[ind + size];
                        neighbors++;
                    }
                    if (neighbors != 4) {
                        neighborsValue *= 4 / neighbors;
                    }
                    oldBuffer[ind] = coefA * newBuffer[ind] + coefB
                            * oldBuffer[ind] + coefC * (neighborsValue);
                }
            }

            curBuf = 1 - curBuf;
        }

        // put the normalized heightmap into the range [0...255] and into the heightmap
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                heightData[x + y * size] = (float) (tempBuffer[curBuf][x + y * size]);
            }
        }
        normalizeTerrain(NORMALIZE_RANGE);

        logger.info("Created Heightmap using fluid simulation");

        return true;
    }

    private float randomRange(Random random, float min, float max) {
        return (random.nextFloat() * (max - min)) + min;
    }

    /**
     * Sets the number of times the fluid simulation should be iterated over
     * the heightmap. The more often this is, the less features (hills, etc)
     * the terrain will have, and the smoother it will be.
     *
     * @param iterations
     *            the number of iterations to do
     * @throws JmeException
     *             if iterations if not greater than zero
     */
    public void setIterations(int iterations) throws Exception {
        if (iterations <= 0) {
            throw new Exception(
                    "Number of iterations is not greater than zero");
        }
        this.iterations = iterations;
    }

    /**
     * Sets the maximum initial height of the terrain.
     *
     * @param maxInitialHeight
     *                        the maximum initial height
     * @see #setMinInitialHeight(int)
     */
    public void setMaxInitialHeight(float maxInitialHeight) {
        this.maxInitialHeight = maxInitialHeight;
    }

    /**
     * Sets the minimum initial height of the terrain.
     *
     * @param minInitialHeight
     *                        the minimum initial height
     * @see #setMaxInitialHeight(int)
     */
    public void setMinInitialHeight(float minInitialHeight) {
        this.minInitialHeight = minInitialHeight;
    }

    /**
     * Sets the distance between each node of the heightmap.
     *
     * @param nodeDistance
     *                       the distance between each node
     */
    public void setNodeDistance(float nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    /**
     * Sets the time-speed between each iteration of the fluid
     * simulation algortithm.
     *
     * @param timeStep
     *                       the time-step between each iteration
     */
    public void setTimeStep(float timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * Sets the viscosity of the simulated fuid.
     *
     * @param viscosity
     *                      the viscosity of the fluid
     */
    public void setViscosity(float viscosity) {
        this.viscosity = viscosity;
    }

    /**
     * Sets the speed at which the waves trave.
     *
     * @param waveSpeed
     *                      the speed at which the waves travel
     */
    public void setWaveSpeed(float waveSpeed) {
        this.waveSpeed = waveSpeed;
    }
}
