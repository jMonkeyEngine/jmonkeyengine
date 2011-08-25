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

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AbstractHeightMap</code> provides a base implementation of height
 * data for terrain rendering. The loading of the data is dependent on the
 * subclass. The abstract implementation provides a means to retrieve the height
 * data and to save it.
 *
 * It is the general contract that any subclass provide a means of editing
 * required attributes and calling <code>load</code> again to recreate a
 * heightfield with these new parameters.
 *
 * @author Mark Powell
 * @version $Id: AbstractHeightMap.java 4133 2009-03-19 20:40:11Z blaine.dev $
 */
public abstract class AbstractHeightMap implements HeightMap {

    private static final Logger logger = Logger.getLogger(AbstractHeightMap.class.getName());
    /** Height data information. */
    protected float[] heightData = null;
    /** The size of the height map's width. */
    protected int size = 0;
    /** Allows scaling the Y height of the map. */
    protected float heightScale = 1.0f;
    /** The filter is used to erode the terrain. */
    protected float filter = 0.5f;
    /** The range used to normalize terrain */
    public static float NORMALIZE_RANGE = 255f;

    /**
     * <code>unloadHeightMap</code> clears the data of the height map. This
     * insures it is ready for reloading.
     */
    public void unloadHeightMap() {
        heightData = null;
    }

    /**
     * <code>setHeightScale</code> sets the scale of the height values.
     * Typically, the height is a little too extreme and should be scaled to a
     * smaller value (i.e. 0.25), to produce cleaner slopes.
     *
     * @param scale
     *            the scale to multiply height values by.
     */
    public void setHeightScale(float scale) {
        heightScale = scale;
    }

    /**
     * <code>setHeightAtPoint</code> sets the height value for a given
     * coordinate. It is recommended that the height value be within the 0 - 255
     * range.
     *
     * @param height
     *            the new height for the coordinate.
     * @param x
     *            the x (east/west) coordinate.
     * @param z
     *            the z (north/south) coordinate.
     */
    public void setHeightAtPoint(float height, int x, int z) {
        heightData[x + (z * size)] = height;
    }

    /**
     * <code>setSize</code> sets the size of the terrain where the area is
     * size x size.
     *
     * @param size
     *            the new size of the terrain.
     * @throws Exception 
     *
     * @throws JmeException
     *             if the size is less than or equal to zero.
     */
    public void setSize(int size) throws Exception {
        if (size <= 0) {
            throw new Exception("size must be greater than zero.");
        }

        this.size = size;
    }

    /**
     * <code>setFilter</code> sets the erosion value for the filter. This
     * value must be between 0 and 1, where 0.2 - 0.4 produces arguably the best
     * results.
     *
     * @param filter
     *            the erosion value.
     * @throws Exception 
     * @throws JmeException
     *             if filter is less than 0 or greater than 1.
     */
    public void setMagnificationFilter(float filter) throws Exception {
        if (filter < 0 || filter >= 1) {
            throw new Exception("filter must be between 0 and 1");
        }
        this.filter = filter;
    }

    /**
     * <code>getTrueHeightAtPoint</code> returns the non-scaled value at the
     * point provided.
     *
     * @param x
     *            the x (east/west) coordinate.
     * @param z
     *            the z (north/south) coordinate.
     * @return the value at (x,z).
     */
    public float getTrueHeightAtPoint(int x, int z) {
        //logger.info( heightData[x + (z*size)]);
        return heightData[x + (z * size)];
    }

    /**
     * <code>getScaledHeightAtPoint</code> returns the scaled value at the
     * point provided.
     *
     * @param x
     *            the x (east/west) coordinate.
     * @param z
     *            the z (north/south) coordinate.
     * @return the scaled value at (x, z).
     */
    public float getScaledHeightAtPoint(int x, int z) {
        return ((heightData[x + (z * size)]) * heightScale);
    }

    /**
     * <code>getInterpolatedHeight</code> returns the height of a point that
     * does not fall directly on the height posts.
     *
     * @param x
     *            the x coordinate of the point.
     * @param z
     *            the y coordinate of the point.
     * @return the interpolated height at this point.
     */
    public float getInterpolatedHeight(float x, float z) {
        float low, highX, highZ;
        float intX, intZ;
        float interpolation;

        low = getScaledHeightAtPoint((int) x, (int) z);

        if (x + 1 >= size) {
            return low;
        }

        highX = getScaledHeightAtPoint((int) x + 1, (int) z);

        interpolation = x - (int) x;
        intX = ((highX - low) * interpolation) + low;

        if (z + 1 >= size) {
            return low;
        }

        highZ = getScaledHeightAtPoint((int) x, (int) z + 1);

        interpolation = z - (int) z;
        intZ = ((highZ - low) * interpolation) + low;

        return ((intX + intZ) / 2);
    }

    /**
     * <code>getHeightMap</code> returns the entire grid of height data.
     *
     * @return the grid of height data.
     */
    public float[] getHeightMap() {
        return heightData;
    }

    /**
     * Build a new array of height data with the scaled values.
     * @return
     */
    public float[] getScaledHeightMap() {
        float[] hm = new float[heightData.length];
        for (int i=0; i<heightData.length; i++) {
            hm[i] = heightScale * heightData[i];
        }
        return hm;
    }

    /**
     * <code>getSize</code> returns the size of one side the height map. Where
     * the area of the height map is size x size.
     *
     * @return the size of a single side.
     */
    public int getSize() {
        return size;
    }

    /**
     * <code>save</code> will save the heightmap data into a new RAW file
     * denoted by the supplied filename.
     *
     * @param filename
     *            the file name to save the current data as.
     * @return true if the save was successful, false otherwise.
     * @throws Exception 
     *
     * @throws JmeException
     *             if filename is null.
     */
    public boolean save(String filename) throws Exception {
        if (null == filename) {
            throw new Exception("Filename must not be null");
        }
        //open the streams and send the height data to the file.
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            DataOutputStream dos = new DataOutputStream(fos);
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    dos.write((int) heightData[j + (i * size)]);
                }
            }

            fos.close();
            dos.close();
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Error opening file {0}", filename);
            return false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error writing to file {0}", filename);
            return false;
        }

        logger.log(Level.INFO, "Saved terrain to {0}", filename);
        return true;
    }

    /**
     * <code>normalizeTerrain</code> takes the current terrain data and
     * converts it to values between 0 and <code>value</code>.
     *
     * @param value
     *            the value to normalize to.
     */
    public void normalizeTerrain(float value) {
        float currentMin, currentMax;
        float height;

        currentMin = heightData[0];
        currentMax = heightData[0];

        //find the min/max values of the height fTemptemptempBuffer
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (heightData[i + j * size] > currentMax) {
                    currentMax = heightData[i + j * size];
                } else if (heightData[i + j * size] < currentMin) {
                    currentMin = heightData[i + j * size];
                }
            }
        }

        //find the range of the altitude
        if (currentMax <= currentMin) {
            return;
        }

        height = currentMax - currentMin;

        //scale the values to a range of 0-255
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                heightData[i + j * size] = ((heightData[i + j * size] - currentMin) / height) * value;
            }
        }
    }

    /**
     * Find the minimum and maximum height values.
     * @return a float array with two value: min height, max height
     */
    public float[] findMinMaxHeights() {
        float[] minmax = new float[2];

        float currentMin, currentMax;
        currentMin = heightData[0];
        currentMax = heightData[0];

        for (int i = 0; i < heightData.length; i++) {
            if (heightData[i] > currentMax) {
                currentMax = heightData[i];
            } else if (heightData[i] < currentMin) {
                currentMin = heightData[i];
            }
        }
        minmax[0] = currentMin;
        minmax[1] = currentMax;
        return minmax;
    }

    /**
     * <code>erodeTerrain</code> is a convenience method that applies the FIR
     * filter to a given height map. This simulates water errosion.
     *
     * @see setFilter
     */
    public void erodeTerrain() {
        //erode left to right
        float v;

        for (int i = 0; i < size; i++) {
            v = heightData[i];
            for (int j = 1; j < size; j++) {
                heightData[i + j * size] = filter * v + (1 - filter) * heightData[i + j * size];
                v = heightData[i + j * size];
            }
        }

        //erode right to left
        for (int i = size - 1; i >= 0; i--) {
            v = heightData[i];
            for (int j = 0; j < size; j++) {
                heightData[i + j * size] = filter * v + (1 - filter) * heightData[i + j * size];
                v = heightData[i + j * size];
                //erodeBand(tempBuffer[size * i + size - 1], -1);
            }
        }

        //erode top to bottom
        for (int i = 0; i < size; i++) {
            v = heightData[i];
            for (int j = 0; j < size; j++) {
                heightData[i + j * size] = filter * v + (1 - filter) * heightData[i + j * size];
                v = heightData[i + j * size];
            }
        }

        //erode from bottom to top
        for (int i = size - 1; i >= 0; i--) {
            v = heightData[i];
            for (int j = 0; j < size; j++) {
                heightData[i + j * size] = filter * v + (1 - filter) * heightData[i + j * size];
                v = heightData[i + j * size];
            }
        }
    }

    /**
     * Flattens out the valleys. The flatten algorithm makes the valleys more
     * prominent while keeping the hills mostly intact. This effect is based on
     * what happens when values below one are squared. The terrain will be
     * normalized between 0 and 1 for this function to work.
     *
     * @param flattening
     *            the power of flattening applied, 1 means none
     */
    public void flatten(byte flattening) {
        // If flattening is one we can skip the calculations
        // since it wouldn't change anything. (e.g. 2 power 1 = 2)
        if (flattening <= 1) {
            return;
        }

        float[] minmax = findMinMaxHeights();

        normalizeTerrain(1f);

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float flat = 1.0f;
                float original = heightData[x + y * size];

                // Flatten as many times as desired;
                for (int i = 0; i < flattening; i++) {
                    flat *= original;
                }
                heightData[x + y * size] = flat;
            }
        }

        // re-normalize back to its oraginal height range
        float height = minmax[1] - minmax[0];
        normalizeTerrain(height);
    }

    /**
     * Smooth the terrain. For each node, its 8 neighbors heights
     * are averaged and will participate in the  node new height
     * by a factor <code>np</code> between 0 and 1
     * 
     * You must first load() the heightmap data before this will have any effect.
     * 
     * @param np
     *          The factor to what extend the neighbors average has an influence.
     *          Value of 0 will ignore neighbors (no smoothing)
     *          Value of 1 will ignore the node old height.
     */
    public void smooth(float np) {
        smooth(np, 1);
    }
    
    /**
     * Smooth the terrain. For each node, its X(determined by radius) neighbors heights
     * are averaged and will participate in the  node new height
     * by a factor <code>np</code> between 0 and 1
     *
     * You must first load() the heightmap data before this will have any effect.
     * 
     * @param np
     *          The factor to what extend the neighbors average has an influence.
     *          Value of 0 will ignore neighbors (no smoothing)
     *          Value of 1 will ignore the node old height.
     */
    public void smooth(float np, int radius) {
        if (np < 0 || np > 1) {
            return;
        }
        if (radius == 0)
            radius = 1;
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int neighNumber = 0;
                float neighAverage = 0;
                for (int rx = -radius; rx <= radius; rx++) {
                    for (int ry = -radius; ry <= radius; ry++) {
                        if (x+rx < 0 || x+rx >= size) {
                        continue;
                        }
                        if (y+ry < 0 || y+ry >= size) {
                            continue;
                        }
                        neighNumber++;
                        neighAverage += heightData[(x+rx) + (y+ry) * size];
                    }
                }

                neighAverage /= neighNumber;
                float cp = 1 - np;
                heightData[x + y * size] = neighAverage * np + heightData[x + y * size] * cp;
            }
        }
    }
}
