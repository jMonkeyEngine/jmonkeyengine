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
package com.jme3.util;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * <code>SkyFactory</code> is used to create jME {@link Spatial}s that can
 * be attached to the scene to display a sky image in the background.
 * 
 * @author Kirill Vainer
 */
public class SkyFactory {

    /**
     * Create a sky with radius=10 using the given cubemap or spheremap texture.
     *
     * For the sky to be visible, its radius must fall between the near and far
     * planes of the camera's frustrum.
     *
     * @param assetManager from which to load materials
     * @param texture to use
     * @param normalScale The normal scale is multiplied by the 3D normal to get
     * a texture coordinate. Use Vector3f.UNIT_XYZ to not apply and
     * transformation to the normal.
     * @param sphereMap determines how the texture is used:<br>
     * <ul>
     * <li>true: The texture is a Texture2D with the pixels arranged for
     * <a href="http://en.wikipedia.org/wiki/Sphere_mapping">sphere
     * mapping</a>.</li>
     * <li>false: The texture is either a TextureCubeMap or Texture2D. If it is
     * a Texture2D then the image is taken from it and is inserted into a
     * TextureCubeMap</li>
     * </ul>
     * @return a new spatial representing the sky, ready to be attached to the
     * scene graph
     */
    public static Spatial createSky(AssetManager assetManager, Texture texture,
            Vector3f normalScale, boolean sphereMap) {
        return createSky(assetManager, texture, normalScale, sphereMap, 10);
    }

    /**
     * Create a sky using the given cubemap or spheremap texture.
     *
     * @param assetManager from which to load materials
     * @param texture to use
     * @param normalScale The normal scale is multiplied by the 3D normal to get
     * a texture coordinate. Use Vector3f.UNIT_XYZ to not apply and
     * transformation to the normal.
     * @param sphereMap determines how the texture is used:<br>
     * <ul>
     * <li>true: The texture is a Texture2D with the pixels arranged for
     * <a href="http://en.wikipedia.org/wiki/Sphere_mapping">sphere
     * mapping</a>.</li>
     * <li>false: The texture is either a TextureCubeMap or Texture2D. If it is
     * a Texture2D then the image is taken from it and is inserted into a
     * TextureCubeMap</li>
     * </ul>
     * @param sphereRadius the sky sphere's radius: for the sky to be visible,
     * its radius must fall between the near and far planes of the camera's
     * frustrum
     * @return a new spatial representing the sky, ready to be attached to the
     * scene graph
     */
    public static Spatial createSky(AssetManager assetManager, Texture texture,
            Vector3f normalScale, boolean sphereMap, int sphereRadius) {
        if (texture == null) {
            throw new IllegalArgumentException("texture cannot be null");
        }
        final Sphere sphereMesh = new Sphere(10, 10, sphereRadius, false, true);

        Geometry sky = new Geometry("Sky", sphereMesh);
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        sky.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));

        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");

        skyMat.setVector3("NormalScale", normalScale);
        if (sphereMap) {
            skyMat.setBoolean("SphereMap", sphereMap);
        } else if (!(texture instanceof TextureCubeMap)) {
            // make sure its a cubemap
            Image img = texture.getImage();
            texture = new TextureCubeMap();
            texture.setImage(img);
        }
        skyMat.setTexture("Texture", texture);
        sky.setMaterial(skyMat);

        return sky;
    }

    private static void checkImage(Image image) {
//        if (image.getDepth() != 1)
//            throw new IllegalArgumentException("3D/Array images not allowed");

        if (image.getWidth() != image.getHeight()) {
            throw new IllegalArgumentException("Image width and height must be the same");
        }

        if (image.getMultiSamples() != 1) {
            throw new IllegalArgumentException("Multisample textures not allowed");
        }
    }

    private static void checkImagesForCubeMap(Image... images) {
        if (images.length == 1) {
            return;
        }

        Format fmt = images[0].getFormat();
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        
        ByteBuffer data = images[0].getData(0);
        int size = data != null ? data.capacity() : 0;

        checkImage(images[0]);

        for (int i = 1; i < images.length; i++) {
            Image image = images[i];
            checkImage(images[i]);
            if (image.getFormat() != fmt) {
                throw new IllegalArgumentException("Images must have same format");
            }
            if (image.getWidth() != width || image.getHeight() != height)  {
                throw new IllegalArgumentException("Images must have same resolution");
            }
            ByteBuffer data2 = image.getData(0);
            if (data2 != null){
                if (data2.capacity() != size) {
                    throw new IllegalArgumentException("Images must have same size");
                }
            }
        }
    }

    /**
     * Create a cube-mapped sky with radius=10 using six textures.
     *
     * For the sky to be visible, its radius must fall between the near and far
     * planes of the camera's frustrum.
     *
     * @param assetManager from which to load materials
     * @param west texture for the western face of the cube
     * @param east texture for the eastern face of the cube
     * @param north texture for the northern face of the cube
     * @param south texture for the southern face of the cube
     * @param up texture for the top face of the cube
     * @param down texture for the bottom face of the cube
     * @param normalScale The normal scale is multiplied by the 3D normal to get
     * a texture coordinate. Use Vector3f.UNIT_XYZ to not apply and
     * transformation to the normal.
     * @return a new spatial representing the sky, ready to be attached to the
     * scene graph
     */
    public static Spatial createSky(AssetManager assetManager, Texture west,
            Texture east, Texture north, Texture south, Texture up,
            Texture down, Vector3f normalScale) {
        return createSky(assetManager, west, east, north, south, up, down,
                normalScale, 10);
    }

    /**
     * Create a cube-mapped sky using six textures.
     *
     * @param assetManager from which to load materials
     * @param west texture for the western face of the cube
     * @param east texture for the eastern face of the cube
     * @param north texture for the northern face of the cube
     * @param south texture for the southern face of the cube
     * @param up texture for the top face of the cube
     * @param down texture for the bottom face of the cube
     * @param normalScale The normal scale is multiplied by the 3D normal to get
     * a texture coordinate. Use Vector3f.UNIT_XYZ to not apply and
     * transformation to the normal.
     * @param sphereRadius the sky sphere's radius: for the sky to be visible,
     * its radius must fall between the near and far planes of the camera's
     * frustrum
     * @return a new spatial representing the sky, ready to be attached to the
     * scene graph
     */
    public static Spatial createSky(AssetManager assetManager, Texture west,
            Texture east, Texture north, Texture south, Texture up,
            Texture down, Vector3f normalScale, float sphereRadius) {
        final Sphere sphereMesh = new Sphere(10, 10, sphereRadius, false, true);
        Geometry sky = new Geometry("Sky", sphereMesh);
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        sky.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));

        Image westImg = west.getImage();
        Image eastImg = east.getImage();
        Image northImg = north.getImage();
        Image southImg = south.getImage();
        Image upImg = up.getImage();
        Image downImg = down.getImage();

        checkImagesForCubeMap(westImg, eastImg, northImg, southImg, upImg, downImg);

        Image cubeImage = new Image(westImg.getFormat(), westImg.getWidth(), westImg.getHeight(), null);

        cubeImage.addData(westImg.getData(0));
        cubeImage.addData(eastImg.getData(0));

        cubeImage.addData(downImg.getData(0));
        cubeImage.addData(upImg.getData(0));

        cubeImage.addData(southImg.getData(0));
        cubeImage.addData(northImg.getData(0));
        
        if (westImg.getEfficentData() != null){
            // also consilidate efficient data
            ArrayList<Object> efficientData = new ArrayList<Object>(6);
            efficientData.add(westImg.getEfficentData());
            efficientData.add(eastImg.getEfficentData());
            efficientData.add(downImg.getEfficentData());
            efficientData.add(upImg.getEfficentData());
            efficientData.add(southImg.getEfficentData());
            efficientData.add(northImg.getEfficentData());
            cubeImage.setEfficentData(efficientData);
        }

        TextureCubeMap cubeMap = new TextureCubeMap(cubeImage);
        cubeMap.setAnisotropicFilter(0);
        cubeMap.setMagFilter(Texture.MagFilter.Bilinear);
        cubeMap.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        cubeMap.setWrap(Texture.WrapMode.EdgeClamp);

        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");
        skyMat.setTexture("Texture", cubeMap);
        skyMat.setVector3("NormalScale", normalScale);
        sky.setMaterial(skyMat);

        return sky;
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south, Texture up, Texture down) {
        return createSky(assetManager, west, east, north, south, up, down, Vector3f.UNIT_XYZ);
    }

    public static Spatial createSky(AssetManager assetManager, Texture texture, boolean sphereMap) {
        return createSky(assetManager, texture, Vector3f.UNIT_XYZ, sphereMap);
    }

    public static Spatial createSky(AssetManager assetManager, String textureName, boolean sphereMap) {
        TextureKey key = new TextureKey(textureName, true);
        key.setGenerateMips(true);
        key.setAsCube(!sphereMap);
        Texture tex = assetManager.loadTexture(key);
        return createSky(assetManager, tex, sphereMap);
    }
}
