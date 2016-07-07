/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.environment.util;

import com.jme3.environment.util.EnvMapUtils;
import com.jme3.math.ColorRGBA;
import static com.jme3.math.FastMath.pow;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.MipMapImageRaster;
import com.jme3.util.BufferUtils;

/**
 * Wraps a Cube map and allows to read from or write pixels into it.
 * 
 * It uses the ImageRaster class to tailor the read write operations.
 * 
 * @author Nehon
 */
public class CubeMapWrapper {

    private MipMapImageRaster mipMapRaster;
    private final DefaultImageRaster raster;
    private int[] sizes;
    private final Vector2f uvs = new Vector2f();
    private final Image image;

    /**
     * Creates a CubeMapWrapper for the given cube map
     * Note that the cube map must be initialized, and the mipmaps sizes should 
     * be set if relevant for them to be readable/writable
     * @param cubeMap the cubemap to wrap.
     */
    public CubeMapWrapper(TextureCubeMap cubeMap) {
        image = cubeMap.getImage();
        if (image.hasMipmaps()) {
            int nbMipMaps = image.getMipMapSizes().length;
            sizes = new int[nbMipMaps];
            mipMapRaster = new MipMapImageRaster(image, 0);

            for (int i = 0; i < nbMipMaps; i++) {
                sizes[i] = Math.max(1, image.getWidth() >> i);
            }
        } else {
            sizes = new int[1];
            sizes[0] = image.getWidth();
        }
        raster = new DefaultImageRaster(image, 0,0 , false);
    }

    /**
     * Reads a pixel from the cube map given the coordinate vector
     * @param vector the direction vector to fetch the texel
     * @param store the color in which to store the pixel color read.
     * @return the color of the pixel read.
     */
    public ColorRGBA getPixel(Vector3f vector, ColorRGBA store) {

        if (store == null) {
            store = new ColorRGBA();
        }

        int face = EnvMapUtils.getCubemapFaceTexCoordFromVector(vector, sizes[0], uvs, EnvMapUtils.FixSeamsMethod.Stretch);
        raster.setSlice(face);
        return raster.getPixel((int) uvs.x, (int) uvs.y, store);
    }

    /**
     * 
     * Reads a pixel from the cube map given the coordinate vector
     * @param vector the direction vector to fetch the texel
     * @param mipLevel the mip level to read from
     * @param store the color in which to store the pixel color read.
     * @return the color of the pixel read.
     */
    public ColorRGBA getPixel(Vector3f vector, int mipLevel, ColorRGBA store) {
        if (mipMapRaster == null) {
            throw new IllegalArgumentException("This cube map has no mip maps");
        }
        if (store == null) {
            store = new ColorRGBA();
        }

        int face = EnvMapUtils.getCubemapFaceTexCoordFromVector(vector, sizes[mipLevel], uvs, EnvMapUtils.FixSeamsMethod.Stretch);
        mipMapRaster.setSlice(face);
        mipMapRaster.setMipLevel(mipLevel);
        return mipMapRaster.getPixel((int) uvs.x, (int) uvs.y, store);
    }

    /**
     * Reads a pixel from the cube map given the 2D coordinates and the face to read from
     * @param x the x tex coordinate (from 0 to width)
     * @param y the y tex coordinate (from 0 to height)
     * @param face the face to read from
     * @param store the color where the result is stored.
     * @return the color read.
     */
    public ColorRGBA getPixel(int x, int y, int face, ColorRGBA store) {
        if (store == null) {
            store = new ColorRGBA();
        }
        raster.setSlice(face);
        return raster.getPixel((int) x, (int) y, store);
    }

     /**
     * Reads a pixel from the cube map given the 2D coordinates and the face and 
     * the mip level to read from
     * @param x the x tex coordinate (from 0 to width)
     * @param y the y tex coordinate (from 0 to height)
     * @param face the face to read from
     * @param mipLevel the miplevel to read from
     * @param store the color where the result is stored.
     * @return the color read.
     */
    public ColorRGBA getPixel(int x, int y, int face, int mipLevel, ColorRGBA store) {
        if (mipMapRaster == null) {
            throw new IllegalArgumentException("This cube map has no mip maps");
        }
        if (store == null) {
            store = new ColorRGBA();
        }
        mipMapRaster.setSlice(face);
        mipMapRaster.setMipLevel(mipLevel);
        return mipMapRaster.getPixel((int) x, (int) y, store);
    }

    /**
     * writes a pixel given the coordinates vector and the color.
     * @param vector the cooredinates where to write the pixel
     * @param color the color to write
     */
    public void setPixel(Vector3f vector, ColorRGBA color) {

        int face = EnvMapUtils.getCubemapFaceTexCoordFromVector(vector, sizes[0], uvs, EnvMapUtils.FixSeamsMethod.Stretch);
        raster.setSlice(face);
        raster.setPixel((int) uvs.x, (int) uvs.y, color);
    }
    /**
     * writes a pixel given the coordinates vector, the mip level and the color.
     * @param vector the cooredinates where to write the pixel
     * @param mipLevel the miplevel to write to
     * @param color the color to write
     */
    public void setPixel(Vector3f vector, int mipLevel, ColorRGBA color) {
        if (mipMapRaster == null) {
            throw new IllegalArgumentException("This cube map has no mip maps");
        }
        int face = EnvMapUtils.getCubemapFaceTexCoordFromVector(vector, sizes[mipLevel], uvs, EnvMapUtils.FixSeamsMethod.Stretch);
        mipMapRaster.setSlice(face);
        mipMapRaster.setMipLevel(mipLevel);
        mipMapRaster.setPixel((int) uvs.x, (int) uvs.y, color);
    }

    /**
     * Writes a pixel given the 2D cordinates and the color
     * @param x the x tex coord (from 0 to width)
     * @param y the y tex coord (from 0 to height)
     * @param face the face to write to
     * @param color the color to write
     */
    public void setPixel(int x, int y, int face, ColorRGBA color) {
        raster.setSlice(face);
        raster.setPixel((int) x, (int) y, color);
    }

    /**
     * Writes a pixel given the 2D cordinates, the mip level and the color
     * @param x the x tex coord (from 0 to width)
     * @param y the y tex coord (from 0 to height)
     * @param face the face to write to
     * @param mipLevel the mip level to write to
     * @param color the color to write
     */
    public void setPixel(int x, int y, int face, int mipLevel, ColorRGBA color) {
        if (mipMapRaster == null) {
            throw new IllegalArgumentException("This cube map has no mip maps");
        }

        mipMapRaster.setSlice(face);
        mipMapRaster.setMipLevel(mipLevel);
        mipMapRaster.setPixel((int) x, (int) y, color);
    }

    /**
     * Inits the mip maps of a cube map witht he given number of mip maps
     * @param nbMipMaps the number of mip maps to initialize
     */
    public void initMipMaps(int nbMipMaps) {
        int maxMipMap = (int) (Math.log(image.getWidth()) / Math.log(2) + 1);
        if (nbMipMaps > maxMipMap) {
            throw new IllegalArgumentException("Max mip map number for a " + image.getWidth() + "x" + image.getHeight() + " cube map is " + maxMipMap);
        }

        sizes = new int[nbMipMaps];

        int totalSize = 0;
        for (int i = 0; i < nbMipMaps; i++) {
            int size = (int) pow(2, maxMipMap - 1 - i);
            sizes[i] = size * size * image.getFormat().getBitsPerPixel() / 8;
            totalSize += sizes[i];
        }
        
        image.setMipMapSizes(sizes);        
        image.getData().clear();
        for (int i = 0; i < 6; i++) {
            image.addData(BufferUtils.createByteBuffer(totalSize));
        }
        mipMapRaster = new MipMapImageRaster(image, 0);        
    }
}
