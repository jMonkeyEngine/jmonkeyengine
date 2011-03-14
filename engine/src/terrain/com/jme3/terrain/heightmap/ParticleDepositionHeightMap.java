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

import java.util.logging.Logger;

/**
 * <code>ParticleDepositionHeightMap</code> creates a heightmap based on the
 * Particle Deposition algorithm based on Jason Shankel's paper from
 * "Game Programming Gems". A heightmap is created using a Molecular beam
 * epitaxy, or MBE, for depositing thin layers of atoms on a substrate.
 * We drop a sequence of particles and simulate their flow across a surface
 * of previously dropped particles. This creates a few high peaks, for further
 * realism we can define a caldera. Similar to the way volcano's form
 * islands, rock is deposited via lava, when the lava cools, it recedes
 * into the volcano, creating the caldera.
 *
 * @author Mark Powell
 * @version $Id$
 */
public class ParticleDepositionHeightMap extends AbstractHeightMap {

    private static final Logger logger = Logger.getLogger(ParticleDepositionHeightMap.class.getName());
    //Attributes.
    private int jumps;
    private int peakWalk;
    private int minParticles;
    private int maxParticles;
    private float caldera;

    /**
     * Constructor sets the attributes of the Particle Deposition
     * Height Map and then generates the map.
     *
     * @param size the size of the terrain where the area is size x size.
     * @param jumps number of areas to drop particles. Can also think
     *              of it as the number of peaks.
     * @param peakWalk determines how much to agitate the drop point
     *              during a creation of a single peak. The lower the number
     *              the more the drop point will be agitated. 1 will insure
     *              agitation every round.
     * @param minParticles defines the minimum number of particles to
     *              drop during a single jump.
     * @param maxParticles defines the maximum number of particles to
     *              drop during a single jump.
     * @param caldera defines the altitude to invert a peak. This is
     *              represented as a percentage, where 0.0 will not invert
     *              anything, and 1.0 will invert all.
     *
     * @throws JmeException if any value is less than zero, and
     *              if caldera is not between 0 and 1. If minParticles is greater than
     *              max particles as well.
     */
    public ParticleDepositionHeightMap(
            int size,
            int jumps,
            int peakWalk,
            int minParticles,
            int maxParticles,
            float caldera) throws Exception {


        if (size <= 0
                || jumps < 0
                || peakWalk < 0
                || minParticles > maxParticles
                || minParticles < 0
                || maxParticles < 0) {


            throw new Exception(
                    "values must be greater than zero, "
                    + "and minParticles must be greater than maxParticles");
        }
        if (caldera < 0.0f || caldera > 1.0f) {
            throw new Exception(
                    "Caldera level must be " + "between 0 and 1");
        }


        this.size = size;
        this.jumps = jumps;
        this.peakWalk = peakWalk;
        this.minParticles = minParticles;
        this.maxParticles = maxParticles;
        this.caldera = caldera;


        load();
    }

    /**
     * <code>load</code> generates the heightfield using the Particle Deposition
     * algorithm. <code>load</code> uses the latest attributes, so a call
     * to <code>load</code> is recommended if attributes have changed using
     * the set methods.
     */
    public boolean load() {
        int x, y;
        int calderaX, calderaY;
        int sx, sy;
        int tx, ty;
        int m;
        float calderaStartPoint;
        float cutoff;
        int dx[] = {0, 1, 0, size - 1, 1, 1, size - 1, size - 1};
        int dy[] = {1, 0, size - 1, 0, size - 1, 1, size - 1, 1};
        float[][] tempBuffer = new float[size][size];
        //map 0 unmarked, unvisited, 1 marked, unvisited, 2 marked visited.
        int[][] calderaMap = new int[size][size];
        boolean done;


        int minx, maxx;
        int miny, maxy;


        if (null != heightData) {
            unloadHeightMap();
        }


        heightData = new float[size * size];


        //create peaks.
        for (int i = 0; i < jumps; i++) {


            //pick a random point.
            x = (int) (Math.rint(Math.random() * (size - 1)));
            y = (int) (Math.rint(Math.random() * (size - 1)));


            //set the caldera point.
            calderaX = x;
            calderaY = y;


            int numberParticles =
                    (int) (Math.rint(
                    (Math.random() * (maxParticles - minParticles))
                    + minParticles));
            //drop particles.
            for (int j = 0; j < numberParticles; j++) {
                //check to see if we should aggitate the drop point.
                if (peakWalk != 0 && j % peakWalk == 0) {
                    m = (int) (Math.rint(Math.random() * 7));
                    x = (x + dx[m] + size) % size;
                    y = (y + dy[m] + size) % size;
                }


                //add the particle to the piont.
                tempBuffer[x][y] += 1;


                sx = x;
                sy = y;
                done = false;


                //cause the particle to "slide" down the slope and settle at
                //a low point.
                while (!done) {
                    done = true;


                    //check neighbors to see if we are higher.
                    m = (int) (Math.rint((Math.random() * 8)));
                    for (int jj = 0; jj < 8; jj++) {
                        tx = (sx + dx[(jj + m) % 8]) % (size);
                        ty = (sy + dy[(jj + m) % 8]) % (size);


                        //move to the neighbor.
                        if (tempBuffer[tx][ty] + 1.0f < tempBuffer[sx][sy]) {
                            tempBuffer[tx][ty] += 1.0f;
                            tempBuffer[sx][sy] -= 1.0f;
                            sx = tx;
                            sy = ty;
                            done = false;
                            break;
                        }
                    }
                }


                //This point is higher than the current caldera point,
                //so move the caldera here.
                if (tempBuffer[sx][sy] > tempBuffer[calderaX][calderaY]) {
                    calderaX = sx;
                    calderaY = sy;
                }
            }


            //apply the caldera.
            calderaStartPoint = tempBuffer[calderaX][calderaY];
            cutoff = calderaStartPoint * (1.0f - caldera);
            minx = calderaX;
            maxx = calderaX;
            miny = calderaY;
            maxy = calderaY;


            calderaMap[calderaX][calderaY] = 1;


            done = false;
            while (!done) {
                done = true;
                sx = minx;
                sy = miny;
                tx = maxx;
                ty = maxy;


                for (x = sx; x <= tx; x++) {
                    for (y = sy; y <= ty; y++) {


                        calderaX = (x + size) % size;
                        calderaY = (y + size) % size;


                        if (calderaMap[calderaX][calderaY] == 1) {
                            calderaMap[calderaX][calderaY] = 2;


                            if (tempBuffer[calderaX][calderaY] > cutoff
                                    && tempBuffer[calderaX][calderaY]
                                    <= calderaStartPoint) {


                                done = false;
                                tempBuffer[calderaX][calderaY] =
                                        2 * cutoff - tempBuffer[calderaX][calderaY];


                                //check the left and right neighbors
                                calderaX = (calderaX + 1) % size;
                                if (calderaMap[calderaX][calderaY] == 0) {
                                    if (x + 1 > maxx) {
                                        maxx = x + 1;
                                    }
                                    calderaMap[calderaX][calderaY] = 1;
                                }


                                calderaX = (calderaX + size - 2) % size;
                                if (calderaMap[calderaX][calderaY] == 0) {
                                    if (x - 1 < minx) {
                                        minx = x - 1;
                                    }
                                    calderaMap[calderaX][calderaY] = 1;
                                }


                                //check the upper and lower neighbors.
                                calderaX = (x + size) % size;
                                calderaY = (calderaY + 1) % size;
                                if (calderaMap[calderaX][calderaY] == 0) {
                                    if (y + 1 > maxy) {
                                        maxy = y + 1;
                                    }
                                    calderaMap[calderaX][calderaY] = 1;
                                }
                                calderaY = (calderaY + size - 2) % size;
                                if (calderaMap[calderaX][calderaY] == 0) {
                                    if (y - 1 < miny) {
                                        miny = y - 1;
                                    }
                                    calderaMap[calderaX][calderaY] = 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        //transfer the new terrain into the height map.
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                setHeightAtPoint((float) tempBuffer[i][j], j, i);
            }
        }
        erodeTerrain();
        normalizeTerrain(NORMALIZE_RANGE);

        logger.info("Created heightmap using Particle Deposition");


        return false;
    }

    /**
     * <code>setJumps</code> sets the number of jumps or peaks that will
     * be created during the next call to <code>load</code>.
     * @param jumps the number of jumps to use for next load.
     * @throws JmeException if jumps is less than zero.
     */
    public void setJumps(int jumps) throws Exception {
        if (jumps < 0) {
            throw new Exception("jumps must be positive");
        }
        this.jumps = jumps;
    }

    /**
     * <code>setPeakWalk</code> sets how often the jump point will be
     * aggitated. The lower the peakWalk, the more often the point will
     * be aggitated.
     *
     * @param peakWalk the amount to aggitate the jump point.
     * @throws JmeException if peakWalk is negative or zero.
     */
    public void setPeakWalk(int peakWalk) throws Exception {
        if (peakWalk <= 0) {
            throw new Exception(
                    "peakWalk must be greater than " + "zero");
        }
        this.peakWalk = peakWalk;
    }

    /**
     * <code>setCaldera</code> sets the level at which a peak will be
     * inverted.
     *
     * @param caldera the level at which a peak will be inverted. This must be
     *              between 0 and 1, as it is represented as a percentage.
     * @throws JmeException if caldera is not between 0 and 1.
     */
    public void setCaldera(float caldera) throws Exception {
        if (caldera < 0.0f || caldera > 1.0f) {
            throw new Exception(
                    "Caldera level must be " + "between 0 and 1");
        }
        this.caldera = caldera;
    }

    /**
     * <code>setMaxParticles</code> sets the maximum number of particles
     * for a single jump.
     * @param maxParticles the maximum number of particles for a single jump.
     * @throws JmeException if maxParticles is negative or less than
     *              the current number of minParticles.
     */
    public void setMaxParticles(int maxParticles) {
        this.maxParticles = maxParticles;
    }

    /**
     * <code>setMinParticles</code> sets the minimum number of particles
     * for a single jump.
     * @param minParticles the minimum number of particles for a single jump.
     * @throws JmeException if minParticles are greater than
     *              the current maxParticles;
     */
    public void setMinParticles(int minParticles) throws Exception {
        if (minParticles > maxParticles) {
            throw new Exception(
                    "minParticles must be less " + "than the current maxParticles");
        }
        this.minParticles = minParticles;
    }
}
