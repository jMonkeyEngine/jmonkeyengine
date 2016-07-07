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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import static com.jme3.math.FastMath.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

/**
 *
 * This class holds several utility method unseful for Physically Based
 * Rendering. It alloaws to compute useful pre filtered maps from an env map.
 *
 * @author Nehon
 */
public class EnvMapUtils {

    public final static int NUM_SH_COEFFICIENT = 9;
    // See Peter-Pike Sloan paper for these coefficients
    //http://www.ppsloan.org/publications/StupidSH36.pdf
    public static float[] shBandFactor = {1.0f,
        2.0f / 3.0f, 2.0f / 3.0f, 2.0f / 3.0f,
        1.0f / 4.0f, 1.0f / 4.0f, 1.0f / 4.0f, 1.0f / 4.0f, 1.0f / 4.0f};

    public static enum FixSeamsMethod {

        /**
         * wrap texture coordinates
         */
        Wrap,
        /**
         * stretch texture coordinates
         */
        Stretch,
        /**
         * No seams fix
         */
        None;
    }

    /**
     * Creates a cube map from 6 images
     *
     * @param leftImg the west side image, also called negative x (negX) or left
     * image
     * @param rightImg the east side image, also called positive x (posX) or
     * right image
     * @param downImg the bottom side image, also called negative y (negY) or
     * down image
     * @param upImg the up side image, also called positive y (posY) or up image
     * @param backImg the south side image, also called positive z (posZ) or
     * back image
     * @param frontImg the north side image, also called negative z (negZ) or
     * front image
     * @param format the format of the image
     * @return a cube map
     */
    public static TextureCubeMap makeCubeMap(Image rightImg, Image leftImg, Image upImg, Image downImg, Image backImg, Image frontImg, Image.Format format) {
        Image cubeImage = new Image(format, leftImg.getWidth(), leftImg.getHeight(), null, ColorSpace.Linear);

        cubeImage.addData(rightImg.getData(0));
        cubeImage.addData(leftImg.getData(0));

        cubeImage.addData(upImg.getData(0));
        cubeImage.addData(downImg.getData(0));

        cubeImage.addData(backImg.getData(0));
        cubeImage.addData(frontImg.getData(0));

        if (leftImg.getEfficentData() != null) {
            // also consilidate efficient data
            ArrayList<Object> efficientData = new ArrayList<Object>(6);
            efficientData.add(rightImg.getEfficentData());
            efficientData.add(leftImg.getEfficentData());
            efficientData.add(upImg.getEfficentData());
            efficientData.add(downImg.getEfficentData());
            efficientData.add(backImg.getEfficentData());
            efficientData.add(frontImg.getEfficentData());
            cubeImage.setEfficentData(efficientData);
        }

        TextureCubeMap cubeMap = new TextureCubeMap(cubeImage);
        cubeMap.setAnisotropicFilter(0);
        cubeMap.setMagFilter(Texture.MagFilter.Bilinear);
        cubeMap.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        cubeMap.setWrap(Texture.WrapMode.EdgeClamp);

        return cubeMap;
    }
  
    /**
     * Make a duplicate of this cube Map. That means that it's another instant
     * od TextureCubeMap, but the underlying buffers are duplicates of the
     * original ones. see {@link ByteBuffer#duplicate()}
     *
     * Use this if you need to read from the map from multiple threads, it
     * should garanty the thread safety. Note that if you want to write to the
     * cube map you have to make sure that the different thread do not write to
     * the same area of the buffer. The position, limit and mark are not an
     * issue.
     *
     * @param sourceMap
     * @return
     */
    public static TextureCubeMap duplicateCubeMap(TextureCubeMap sourceMap) {
        Image srcImg = sourceMap.getImage();
        Image cubeImage = new Image(srcImg.getFormat(), srcImg.getWidth(), srcImg.getHeight(), null, srcImg.getColorSpace());

        for (ByteBuffer d : srcImg.getData()) {
            cubeImage.addData(d.duplicate());
        }

        if (srcImg.getEfficentData() != null) {
            // also consilidate efficient data
            ArrayList<Object> efficientData = new ArrayList<Object>(6);
            efficientData.add(srcImg.getEfficentData());
            cubeImage.setEfficentData(efficientData);
        }

        TextureCubeMap cubeMap = new TextureCubeMap(cubeImage);
        cubeMap.setAnisotropicFilter(sourceMap.getAnisotropicFilter());
        cubeMap.setMagFilter(sourceMap.getMagFilter());
        cubeMap.setMinFilter(sourceMap.getMinFilter());
        cubeMap.setWrap(sourceMap.getWrap(Texture.WrapAxis.S));

        return cubeMap;
    }

    /**
     * Computes the vector coordinates, for the given x,y texture coordinates
     * and the given cube map face.
     *
     * Also computes the solid angle for those coordinates and returns it.
     *
     * To know what the solid angle is please read this.
     * http://www.codinglabs.net/article_physically_based_rendering.aspx
     *
     *
     * Original solid angle calculation code is from Ignacio Castaño. This
     * formula is from Manne Öhrström's thesis. It takes two coordiantes in the
     * range [-1, 1] that define a portion of a cube face and return the area of
     * the projection of that portion on the surface of the sphere.
     *
     * @param x texture coordinate from 0 to 1 in the given cube map face
     * @param y texture coordinate from 0 to 1 in the given cube map face
     * @param mapSize the size of the cube map
     * @param face the face id of the cube map
     * @param store the vector3f where the vector will be stored. don't provide
     * null for this param
     * @return the solid angle for the give parameters
     */
    static float getSolidAngleAndVector(int x, int y, int mapSize, int face, Vector3f store, FixSeamsMethod fixSeamsMethod) {

        if (store == null) {
            throw new IllegalArgumentException("the store parameter ust not be null");
        }

        /* transform from [0..res - 1] to [- (1 - 1 / res) .. (1 - 1 / res)]
         (+ 0.5f is for texel center addressing) */
        float u = (2.0f * ((float) x + 0.5f) / (float) mapSize) - 1.0f;
        float v = (2.0f * ((float) y + 0.5f) / (float) mapSize) - 1.0f;

        getVectorFromCubemapFaceTexCoord(x, y, mapSize, face, store, fixSeamsMethod);

        /* Solid angle weight approximation :
         * U and V are the -1..1 texture coordinate on the current face.
         * Get projected area for this texel */
        float x0, y0, x1, y1;
        float invRes = 1.0f / (float) mapSize;
        x0 = u - invRes;
        y0 = v - invRes;
        x1 = u + invRes;
        y1 = v + invRes;

        return areaElement(x0, y0) - areaElement(x0, y1) - areaElement(x1, y0) + areaElement(x1, y1);
    }

    /**
     * used to compute the solid angle
     *
     * @param x tex coordinates
     * @param y tex coordinates
     * @return
     */
    private static float areaElement(float x, float y) {
        return (float) Math.atan2(x * y, sqrt(x * x + y * y + 1));
    }

    /**
     *
     * Computes the 3 component vector coordinates for the given face and coords
     *
     * @param x the x texture coordinate
     * @param y the y texture coordinate
     * @param mapSize the size of a face of the cube map
     * @param face the face to consider
     * @param store a vector3f where the resulting vector will be stored
     * @param fixSeamsMethod the method to fix the seams
     * @return
     */
    public static Vector3f getVectorFromCubemapFaceTexCoord(int x, int y, int mapSize, int face, Vector3f store, FixSeamsMethod fixSeamsMethod) {
        if (store == null) {
            store = new Vector3f();
        }

        float u;
        float v;

        if (fixSeamsMethod == FixSeamsMethod.Stretch) {
            /* Code from Nvtt : http://code.google.com/p/nvidia-texture-tools/source/browse/trunk/src/nvtt/CubeSurface.cpp		
             * transform from [0..res - 1] to [-1 .. 1], match up edges exactly. */
            u = (2.0f * (float) x / ((float) mapSize - 1.0f)) - 1.0f;
            v = (2.0f * (float) y / ((float) mapSize - 1.0f)) - 1.0f;
        } else {
            //Done if any other fix method or no fix method is set
            /* transform from [0..res - 1] to [- (1 - 1 / res) .. (1 - 1 / res)]
             * (+ 0.5f is for texel center addressing) */
            u = (2.0f * ((float) x + 0.5f) / (float) (mapSize)) - 1.0f;
            v = (2.0f * ((float) y + 0.5f) / (float) (mapSize)) - 1.0f;
        }

        if (fixSeamsMethod == FixSeamsMethod.Wrap) {
            // Warp texel centers in the proximity of the edges.
            float a = pow((float) mapSize, 2.0f) / pow(((float) mapSize - 1f), 3.0f);
            u = a * pow(u, 3f) + u;
            v = a * pow(v, 3f) + v;
        }

        //compute vector depending on the face
        // Code from Nvtt : http://code.google.com/p/nvidia-texture-tools/source/browse/trunk/src/nvtt/CubeSurface.cpp	
        switch (face) {
            case 0:
                store.set(1f, -v, -u);
                break;
            case 1:
                store.set(-1f, -v, u);
                break;
            case 2:
                store.set(u, 1f, v);
                break;
            case 3:
                store.set(u, -1f, -v);
                break;
            case 4:
                store.set(u, -v, 1f);
                break;
            case 5:
                store.set(-u, -v, -1.0f);
                break;
        }

        return store.normalizeLocal();
    }

    /**
     *
     * Computes the texture coortinates and the face of the cube map from the
     * given vector
     *
     * @param texelVect the vector to fetch texelt from the cube map
     * @param fixSeamsMethod the method to fix the seams
     * @param mapSize the size of one face of the cube map
     * @param store a Vector2f where the texture coordinates will be stored
     * @return the face from which to fetch the texel
     */
    public static int getCubemapFaceTexCoordFromVector(Vector3f texelVect, int mapSize, Vector2f store, FixSeamsMethod fixSeamsMethod) {

        float u = 0, v = 0, bias = 0;
        int face;
        float absX = abs(texelVect.x);
        float absY = abs(texelVect.y);
        float absZ = abs(texelVect.z);
        float max = Math.max(Math.max(absX, absY), absZ);
        if (max == absX) {
            face = texelVect.x > 0 ? 0 : 1;
        } else if (max == absY) {
            face = texelVect.y > 0 ? 2 : 3;
        } else {
            face = texelVect.z > 0 ? 4 : 5;
        }

        //compute vector depending on the face
        // Code from Nvtt : http://code.google.com/p/nvidia-texture-tools/source/browse/trunk/src/nvtt/CubeSurface.cpp	
        switch (face) {
            case 0:
                //store.set(1f, -v, -u, 0);
                bias = 1f / texelVect.x;
                u = -texelVect.z;
                v = -texelVect.y;
                break;
            case 1:
                // store.set(-1f, -v, u, 0);
                bias = -1f / texelVect.x;
                u = texelVect.z;
                v = -texelVect.y;
                break;
            case 2:
                //store.set(u, 1f, v, 0);
                bias = 1f / texelVect.y;
                u = texelVect.x;
                v = texelVect.z;
                break;
            case 3:
                //store.set(u, -1f, -v, 0);
                bias = -1f / texelVect.y;
                u = texelVect.x;
                v = -texelVect.z;
                break;
            case 4:
                //store.set(u, -v, 1f, 0);
                bias = 1f / texelVect.z;
                u = texelVect.x;
                v = -texelVect.y;
                break;
            case 5:
                //store.set(-u, -v, -1.0f, 0);
                bias = -1f / texelVect.z;
                u = -texelVect.x;
                v = -texelVect.y;
                break;
        }
        u *= bias;
        v *= bias;

        if (fixSeamsMethod == FixSeamsMethod.Stretch) {
            /* Code from Nvtt : http://code.google.com/p/nvidia-texture-tools/source/browse/trunk/src/nvtt/CubeSurface.cpp		
             * transform from [0..res - 1] to [-1 .. 1], match up edges exactly. */
            u = Math.round((u + 1.0f) * ((float) mapSize - 1.0f) * 0.5f);
            v = Math.round((v + 1.0f) * ((float) mapSize - 1.0f) * 0.5f);
        } else {
            //Done if any other fix method or no fix method is set
            /* transform from [0..res - 1] to [- (1 - 1 / res) .. (1 - 1 / res)]
             * (+ 0.5f is for texel center addressing) */
            u = Math.round((u + 1.0f) * ((float) mapSize) * 0.5f - 0.5f);
            v = Math.round((v + 1.0f) * ((float) mapSize) * 0.5f - 0.5f);

        }

        store.set(u, v);
        return face;
    }

    /*
    public static void main(String... argv) {

//        for (int givenFace = 0; givenFace < 6; givenFace++) {
//
//            //int givenFace = 1;
//            for (int x = 0; x < 128; x++) {
//                for (int y = 0; y < 128; y++) {
//                    Vector3f v = EnvMapUtils.getVectorFromCubemapFaceTexCoord(x, y, 128, givenFace, null, FixSeamsMethod.None);
//                    Vector2f uvs = new Vector2f();
//                    int face = EnvMapUtils.getCubemapFaceTexCoordFromVector(v, 128, uvs, FixSeamsMethod.None);
//
//                    if ((int) uvs.x != x || (int) uvs.y != y) {
//                        System.err.println("error " + uvs + " should be " + x + "," + y + " vect was " + v);
//                    }
//                    if (givenFace != face) {
//                        System.err.println("error face: " + face + " should be " + givenFace);
//                    }
//                }
//            }
//        }
//        System.err.println("done ");
        int total = 0;
        for (int i = 0; i < 6; i++) {
            int size = (int) pow(2, 7 - i);
            int samples = EnvMapUtils.getSampleFromMip(i, 6);
            int iterations = (samples * size * size);
            total += iterations;
            float roughness = EnvMapUtils.getRoughnessFromMip(i, 6);
            System.err.println("roughness " + i + " : " + roughness + " , map : " + size + " , samples : " + samples + " , iterations : " + iterations);
            System.err.println("reverse " + EnvMapUtils.getMipFromRoughness(roughness, 6));

        }
        System.err.println("total " + total);
        System.err.println(128 * 128 * 1024);
        System.err.println("test " + EnvMapUtils.getMipFromRoughness(0.9999f, 6));
        System.err.println("nb mip = " + (Math.log(128) / Math.log(2) - 1));

    }*/

    public static int getSampleFromMip(int mipLevel, int miptot) {        
        return mipLevel==0?1:Math.min(1 << (miptot - 1 + (mipLevel) * 2 ), 8192);
    }

    public static float getRoughnessFromMip(int miplevel, int miptot) {
        float mipScale = 1.0f;
        float mipOffset = -0.3f;

        return pow(2, (miplevel - (miptot - 1) + mipOffset) / mipScale);
    }

    public static float getMipFromRoughness(float roughness, int miptot) {
        float mipScale = 1.0f;
        float Lod = (float) (Math.log(roughness) / Math.log(2)) * mipScale + miptot - 1.0f;       

        return (float) Math.max(0.0, Lod);
    }

    /**
     * same as
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap, com.jme3.utils.EnvMapUtils.FixSeamsMethod)}
     * the fix method used is {@link FixSeamsMethod#Wrap}
     *
     * @param cubeMap the environment cube map to compute SH for
     * @return an array of 9 vector3f representing thos coefficients for each
     * r,g,b channnel
     */
    public static Vector3f[] getSphericalHarmonicsCoefficents(TextureCubeMap cubeMap) {
        return getSphericalHarmonicsCoefficents(cubeMap, FixSeamsMethod.Wrap);
    }

    /**
     * Returns the Spherical Harmonics coefficients for this cube map.
     *
     * The method used is the one from this article :
     * http://graphics.stanford.edu/papers/envmap/envmap.pdf
     *
     * Also good resources on spherical harmonics
     * http://dickyjim.wordpress.com/2013/09/04/spherical-harmonics-for-beginners/
     *
     * @param cubeMap the environment cube map to compute SH for
     * @param fixSeamsMethod method to fix seams when computing the SH
     * coefficients
     * @return an array of 9 vector3f representing thos coefficients for each
     * r,g,b channnel
     */
    public static Vector3f[] getSphericalHarmonicsCoefficents(TextureCubeMap cubeMap, FixSeamsMethod fixSeamsMethod) {

        Vector3f[] shCoef = new Vector3f[NUM_SH_COEFFICIENT];

        float[] shDir = new float[9];
        float weightAccum = 0.0f;
        float weight;

        if (cubeMap.getImage().getData(0) == null) {
            throw new IllegalStateException("The cube map must contain Efficient data, if you rendered the cube map on the GPU plase use renderer.readFrameBuffer, to create a CPU image");
        }

        int width = cubeMap.getImage().getWidth();
        int height = cubeMap.getImage().getHeight();

        Vector3f texelVect = new Vector3f();
        ColorRGBA color = new ColorRGBA();

        CubeMapWrapper envMapReader = new CubeMapWrapper(cubeMap);
        for (int face = 0; face < 6; face++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    weight = getSolidAngleAndVector(x, y, width, face, texelVect, fixSeamsMethod);

                    evalShBasis(texelVect, shDir);

                    envMapReader.getPixel(x, y, face, color);

                    for (int i = 0; i < NUM_SH_COEFFICIENT; i++) {

                        if (shCoef[i] == null) {
                            shCoef[i] = new Vector3f();
                        }

                        shCoef[i].setX(shCoef[i].x + color.r * shDir[i] * weight);
                        shCoef[i].setY(shCoef[i].y + color.g * shDir[i] * weight);
                        shCoef[i].setZ(shCoef[i].z + color.b * shDir[i] * weight);
                    }

                    weightAccum += weight;
                }
            }
        }

        /* Normalization - The sum of solid angle should be equal to the solid angle of the sphere (4 PI), so
         * normalize in order our weightAccum exactly match 4 PI. */
        for (int i = 0; i < NUM_SH_COEFFICIENT; ++i) {
            shCoef[i].multLocal(4.0f * PI / weightAccum);
        }
        return shCoef;
    }

    /**
     * Computes SH coefficient for a given textel dir The method used is the one
     * from this article : http://graphics.stanford.edu/papers/envmap/envmap.pdf
     *
     * @param texelVect
     * @param shDir
     */
    public static void evalShBasis(Vector3f texelVect, float[] shDir) {

        float xV = texelVect.x;
        float yV = texelVect.y;
        float zV = texelVect.z;

        float pi = PI;
        float sqrtPi = sqrt(pi);
        float sqrt3Pi = sqrt(3f / pi);
        float sqrt5Pi = sqrt(5f / pi);
        float sqrt15Pi = sqrt(15f / pi);

        float x2 = xV * xV;
        float y2 = yV * yV;
        float z2 = zV * zV;

        shDir[0] = (1f / (2f * sqrtPi));
        shDir[1] = -(sqrt3Pi * yV) / 2f;
        shDir[2] = (sqrt3Pi * zV) / 2f;
        shDir[3] = -(sqrt3Pi * xV) / 2f;
        shDir[4] = (sqrt15Pi * xV * yV) / 2f;
        shDir[5] = -(sqrt15Pi * yV * zV) / 2f;
        shDir[6] = (sqrt5Pi * (-1f + 3f * z2)) / 4f;
        shDir[7] = -(sqrt15Pi * xV * zV) / 2f;
        shDir[8] = sqrt15Pi * (x2 - y2) / 4f;
        
//        shDir[0]  = (1f/(2.f*sqrtPi));
//
//	shDir[1]  = -(sqrt(3f/pi)*yV)/2.f;
//	shDir[2]  = (sqrt(3/pi)*zV)/2.f;
//	shDir[3]  = -(sqrt(3/pi)*xV)/2.f;
//
//	shDir[4]  = (sqrt(15f/pi)*xV*yV)/2.f;
//	shDir[5]  = -(sqrt(15f/pi)*yV*zV)/2.f;
//	shDir[6]  = (sqrt(5f/pi)*(-1 + 3f*z2))/4.f;
//	shDir[7]  = -(sqrt(15f/pi)*xV*zV)/2.f;
//	shDir[8]  = sqrt(15f/pi)*(x2 - y2)/4.f;


    }

    /**
     * {@link EnvMapUtils#generateIrradianceMap(com.jme3.math.Vector3f[], com.jme3.texture.TextureCubeMap, int, com.jme3.utils.EnvMapUtils.FixSeamsMethod)
     * }
     *
     * @param shCoeffs the spherical harmonics coefficients to use
     * @param targetMapSize the size of the target map
     * @return the irradiance map.
     */
    public static TextureCubeMap generateIrradianceMap(Vector3f[] shCoeffs, int targetMapSize) {
        return generateIrradianceMap(shCoeffs, targetMapSize, FixSeamsMethod.Wrap, null);
    }

    /**
     * Generates the Irradiance map (used for image based difuse lighting) from
     * Spherical Harmonics coefficients previously computed with
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap)}
     * Note that the output cube map is in RGBA8 format.
     *
     * @param shCoeffs the SH coeffs
     * @param targetMapSize the size of the irradiance map to generate
     * @param fixSeamsMethod the method to fix seams
     * @param store
     * @return The irradiance cube map for the given coefficients
     */
    public static TextureCubeMap generateIrradianceMap(Vector3f[] shCoeffs, int targetMapSize, FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        TextureCubeMap irrCubeMap = store;
        if (irrCubeMap == null) {
            irrCubeMap = new TextureCubeMap(targetMapSize, targetMapSize, Image.Format.RGB16F);
            irrCubeMap.setMagFilter(Texture.MagFilter.Bilinear);
            irrCubeMap.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            irrCubeMap.getImage().setColorSpace(ColorSpace.Linear);
        }

        for (int i = 0; i < 6; i++) {
            ByteBuffer buf = BufferUtils.createByteBuffer(targetMapSize * targetMapSize * irrCubeMap.getImage().getFormat().getBitsPerPixel()/8);
            irrCubeMap.getImage().setData(i, buf);
        }

        Vector3f texelVect = new Vector3f();
        ColorRGBA color = new ColorRGBA(ColorRGBA.Black);
        float[] shDir = new float[9];
        CubeMapWrapper envMapWriter = new CubeMapWrapper(irrCubeMap);
        for (int face = 0; face < 6; face++) {

            for (int y = 0; y < targetMapSize; y++) {
                for (int x = 0; x < targetMapSize; x++) {
                    getVectorFromCubemapFaceTexCoord(x, y, targetMapSize, face, texelVect, fixSeamsMethod);
                    evalShBasis(texelVect, shDir);
                    color.set(0, 0, 0, 0);
                    for (int i = 0; i < NUM_SH_COEFFICIENT; i++) {
                        color.set(color.r + shCoeffs[i].x * shDir[i] * shBandFactor[i],
                                color.g + shCoeffs[i].y * shDir[i] * shBandFactor[i],
                                color.b + shCoeffs[i].z * shDir[i] * shBandFactor[i],
                                1.0f);
                    }
                    
                    //clamping the color because very low value close to zero produce artifacts
                    color.r = Math.max(0.0001f, color.r);
                    color.g = Math.max(0.0001f, color.g);
                    color.b = Math.max(0.0001f, color.b);
                    envMapWriter.setPixel(x, y, face, color);
                }
            }
        }
        return irrCubeMap;
    }

    /**
     * Generates the prefiltered env map (used for image based specular
     * lighting) With the GGX/Shlick brdf
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap)}
     * Note that the output cube map is in RGBA8 format.
     *
     * @param sourceEnvMap
     * @param targetMapSize the size of the irradiance map to generate
     * @param store
     * @param fixSeamsMethod the method to fix seams
     * @return The irradiance cube map for the given coefficients
     */
    public static TextureCubeMap generatePrefilteredEnvMap(TextureCubeMap sourceEnvMap, int targetMapSize, FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        TextureCubeMap pem = store;
        if (pem == null) {
            pem = new TextureCubeMap(targetMapSize, targetMapSize, Image.Format.RGB16F);
            pem.setMagFilter(Texture.MagFilter.Bilinear);
            pem.setMinFilter(Texture.MinFilter.Trilinear);
            pem.getImage().setColorSpace(ColorSpace.Linear);
        }

        int nbMipMap = (int) (Math.log(targetMapSize) / Math.log(2) - 1);

        CubeMapWrapper sourceWrapper = new CubeMapWrapper(sourceEnvMap);
        CubeMapWrapper targetWrapper = new CubeMapWrapper(pem);
        targetWrapper.initMipMaps(nbMipMap);

        Vector3f texelVect = new Vector3f();
        Vector3f color = new Vector3f();
        ColorRGBA outColor = new ColorRGBA();
        for (int mipLevel = 0; mipLevel < nbMipMap; mipLevel++) {
            System.err.println("mip level " + mipLevel);
            float roughness = getRoughnessFromMip(mipLevel, nbMipMap);
            int nbSamples = getSampleFromMip(mipLevel, nbMipMap);
            int targetMipMapSize = (int) pow(2, nbMipMap + 1 - mipLevel);
            for (int face = 0; face < 6; face++) {
                System.err.println("face " + face);
                for (int y = 0; y < targetMipMapSize; y++) {
                    for (int x = 0; x < targetMipMapSize; x++) {
                        color.set(0, 0, 0);
                        getVectorFromCubemapFaceTexCoord(x, y, targetMipMapSize, face, texelVect, FixSeamsMethod.Wrap);
                        prefilterEnvMapTexel(sourceWrapper, roughness, texelVect, nbSamples, color);
                        outColor.set(color.x, color.y, color.z, 1.0f);
                        // System.err.println("coords " + x + "," + y);
                        targetWrapper.setPixel(x, y, face, mipLevel, outColor);
                    }
                }
            }
        }
        return pem;
    }

    public static Vector4f getHammersleyPoint(int i, final int nbrSample, Vector4f store) {
        if (store == null) {
            store = new Vector4f();
        }
        float phi;
        long ui = i;
        store.setX((float) i / (float) nbrSample);

        /* From http://holger.dammertz.org/stuff/notes_HammersleyOnHemisphere.html
         * Radical Inverse : Van der Corput */
        ui = (ui << 16) | (ui >> 16);
        ui = ((ui & 0x55555555) << 1) | ((ui & 0xAAAAAAAA) >>> 1);
        ui = ((ui & 0x33333333) << 2) | ((ui & 0xCCCCCCCC) >>> 2);
        ui = ((ui & 0x0F0F0F0F) << 4) | ((ui & 0xF0F0F0F0) >>> 4);
        ui = ((ui & 0x00FF00FF) << 8) | ((ui & 0xFF00FF00) >>> 8);

        ui = ui & 0xffffffff;
        store.setY(2.3283064365386963e-10f * (float) (ui)); /* 0x100000000 */

        phi = 2.0f * PI * store.y;
        store.setZ(cos(phi));
        store.setW(sin(phi));

        return store;
    }

    private static Vector3f prefilterEnvMapTexel(CubeMapWrapper envMapReader, float roughness, Vector3f N, int numSamples, Vector3f store) {        

        Vector3f prefilteredColor = store;
        float totalWeight = 0.0f;

        TempVars vars = TempVars.get();
        Vector4f Xi = vars.vect4f1;
        Vector3f H = vars.vect1;
        Vector3f tmp = vars.vect2;
        ColorRGBA c = vars.color;
        // a = roughness² and a2 = a²
        float a2 = roughness * roughness;
        a2 *= a2;
        a2 *= 10;
        for (int i = 0; i < numSamples; i++) {
            Xi = getHammersleyPoint(i, numSamples, Xi);            
            H = importanceSampleGGX(Xi, a2, N, H, vars);

            H.normalizeLocal();
            tmp.set(H);
            float NoH = N.dot(tmp);

            Vector3f L = tmp.multLocal(NoH * 2).subtractLocal(N);
            float NoL = clamp(N.dot(L), 0.0f, 1.0f);
            if (NoL > 0) {
                envMapReader.getPixel(L, c);
                prefilteredColor.setX(prefilteredColor.x + c.r * NoL);
                prefilteredColor.setY(prefilteredColor.y + c.g * NoL);
                prefilteredColor.setZ(prefilteredColor.z + c.b * NoL);

                totalWeight += NoL;
            }
        }
        vars.release();
        return prefilteredColor.divideLocal(totalWeight);
    }

    public static Vector3f importanceSampleGGX(Vector4f xi, float a2, Vector3f normal, Vector3f store, TempVars vars) {
        if (store == null) {
            store = new Vector3f();
        }

        float cosTheta = sqrt((1f - xi.x) / (1f + (a2 - 1f) * xi.x));
        float sinTheta = sqrt(1f - cosTheta * cosTheta);

        float sinThetaCosPhi = sinTheta * xi.z;//xi.z is cos(phi)
        float sinThetaSinPhi = sinTheta * xi.w;//xi.w is sin(phi)

        Vector3f upVector = Vector3f.UNIT_X;

        if (abs(normal.z) < 0.999) {
            upVector = Vector3f.UNIT_Y;
        }

        Vector3f tangentX = vars.vect3.set(upVector).crossLocal(normal).normalizeLocal();
        Vector3f tangentY = vars.vect4.set(normal).crossLocal(tangentX);

        // Tangent to world space
        tangentX.multLocal(sinThetaCosPhi);
        tangentY.multLocal(sinThetaSinPhi);
        vars.vect5.set(normal).multLocal(cosTheta);

        // Tangent to world space
        store.set(tangentX).addLocal(tangentY).addLocal(vars.vect5);

        return store;
    }

    /**
     * Creates a debug Node of the given cube map to attach to the gui node
     *
     * the cube map is layered this way :
     * <pre>
     *         _____
     *        |     |
     *        | +Y  |
     *   _____|_____|_____ _____
     *  |     |     |     |     |
     *  | -X  | +Z  | +X  | -Z  |
     *  |_____|_____|_____|_____|
     *        |     |
     *        | -Y  |
     *        |_____|
     *
     *</pre>
     *
     * @param cubeMap the cube map
     * @param assetManager the asset Manager
     * @return
     */
    public static Node getCubeMapCrossDebugView(TextureCubeMap cubeMap, AssetManager assetManager) {
        Node n = new Node("CubeMapDebug" + cubeMap.getName());
        int size = cubeMap.getImage().getWidth();
        Picture[] pics = new Picture[6];

        float ratio = 128f / (float) size;

        for (int i = 0; i < 6; i++) {
            pics[i] = new Picture("bla");
            Texture2D tex = new Texture2D(new Image(cubeMap.getImage().getFormat(), size, size, cubeMap.getImage().getData(i), cubeMap.getImage().getColorSpace()));

            pics[i].setTexture(assetManager, tex, true);
            pics[i].setWidth(size);
            pics[i].setHeight(size);
            n.attachChild(pics[i]);
        }

        pics[0].setLocalTranslation(size, size * 2, 1);
        pics[0].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
        pics[1].setLocalTranslation(size * 3, size * 2, 1);
        pics[1].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
        pics[2].setLocalTranslation(size * 2, size * 3, 1);
        pics[2].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
        pics[3].setLocalTranslation(size * 2, size, 1);
        pics[3].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
        pics[4].setLocalTranslation(size * 2, size * 2, 1);
        pics[4].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
        pics[5].setLocalTranslation(size * 4, size * 2, 1);
        pics[5].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));

        Quad q = new Quad(size * 4, size * 3);
        Geometry g = new Geometry("bg", q);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Black);
        g.setMaterial(mat);
        g.setLocalTranslation(0, 0, 0);

        n.attachChild(g);
        n.setLocalScale(ratio);
        return n;
    }

    public static Node getCubeMapCrossDebugViewWithMipMaps(TextureCubeMap cubeMap, AssetManager assetManager) {
        Node n = new Node("CubeMapDebug" + cubeMap.getName());
        int size = cubeMap.getImage().getWidth();
        int nbMips = cubeMap.getImage().getMipMapSizes().length;
        Picture[] pics = new Picture[6*nbMips];

        float ratio = 1f;// 128f / (float) size;

        int offset = 0;
        int guiOffset = 0;
        for (int mipLevel = 0; mipLevel < nbMips; mipLevel++) {
            size = Math.max(1, cubeMap.getImage().getWidth() >> mipLevel);
            int dataSize = cubeMap.getImage().getMipMapSizes()[mipLevel];
            byte[] dataArray = new byte[dataSize];
            for (int i = 0; i < 6; i++) {
                
                ByteBuffer bb = cubeMap.getImage().getData(i);
              
                bb.rewind();
                bb.position(offset);
                bb.get(dataArray, 0, dataSize);
                ByteBuffer data = BufferUtils.createByteBuffer(dataArray);

                pics[i] = new Picture("bla");
                Texture2D tex = new Texture2D(new Image(cubeMap.getImage().getFormat(), size, size, data, cubeMap.getImage().getColorSpace()));

                pics[i].setTexture(assetManager, tex, true);
                pics[i].setWidth(size);
                pics[i].setHeight(size);
                n.attachChild(pics[i]);
            }
            pics[0].setLocalTranslation(guiOffset + size, guiOffset + size * 2, 1);
            pics[0].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            pics[1].setLocalTranslation(guiOffset + size * 3, guiOffset + size * 2, 1);
            pics[1].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            pics[2].setLocalTranslation(guiOffset + size * 2, guiOffset + size * 3, 1);
            pics[2].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            pics[3].setLocalTranslation(guiOffset + size * 2, guiOffset + size, 1);
            pics[3].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            pics[4].setLocalTranslation(guiOffset + size * 2, guiOffset + size * 2, 1);
            pics[4].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            pics[5].setLocalTranslation(guiOffset + size * 4, guiOffset + size * 2, 1);
            pics[5].setLocalRotation(new Quaternion().fromAngleAxis(PI, Vector3f.UNIT_Z));
            
            guiOffset+=size *2+1;
            offset += dataSize;
            
        }

        Quad q = new Quad(cubeMap.getImage().getWidth() * 4 + nbMips, guiOffset + size);
        Geometry g = new Geometry("bg", q);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Black);
        g.setMaterial(mat);
        g.setLocalTranslation(0, 0, 0);

        n.attachChild(g);
        n.setLocalScale(ratio);
        return n;
    }
    
     /**
     * initialize the Irradiancemap
     * @param size the size of the map
     * @param imageFormat the format of the image
     * @return the initialized Irradiance map
     */
    public static TextureCubeMap createIrradianceMap(int size, Image.Format imageFormat) {

        TextureCubeMap irrMap = new TextureCubeMap(size, size, imageFormat);
        irrMap.setMagFilter(Texture.MagFilter.Bilinear);
        irrMap.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        irrMap.getImage().setColorSpace(ColorSpace.Linear);
        return irrMap;
    }

    /**
     * initialize the pem map
     * @param size the size of the map
     * @param imageFormat the format of the image
     * @return the initialized prefiltered env map
     */
    public static TextureCubeMap createPrefilteredEnvMap(int size, Image.Format imageFormat) {

        TextureCubeMap pem = new TextureCubeMap(size, size, imageFormat);
        pem.setMagFilter(Texture.MagFilter.Bilinear);
        pem.setMinFilter(Texture.MinFilter.Trilinear);
        pem.getImage().setColorSpace(ColorSpace.Linear);
        int nbMipMap = (int) (Math.log(size) / Math.log(2) - 1);
        CubeMapWrapper targetWrapper = new CubeMapWrapper(pem);
        targetWrapper.initMipMaps(nbMipMap);
        return pem;
    }
}
