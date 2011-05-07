package com.jme3.util;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
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

public class SkyFactory {

    private static final Sphere sphereMesh = new Sphere(10, 10, 10, false, true);

    public static Spatial createSky(AssetManager assetManager, Texture texture, Vector3f normalScale, boolean sphereMap){
        Geometry sky = new Geometry("Sky", sphereMesh);
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);

        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");
        
        skyMat.setVector3("NormalScale", normalScale);
        if (sphereMap){
            skyMat.setBoolean("SphereMap", sphereMap);
        }else if (!(texture instanceof TextureCubeMap)){
            // make sure its a cubemap
            Image img = texture.getImage();
            texture = new TextureCubeMap();
            texture.setImage(img);
        }
        skyMat.setTexture("Texture", texture);
        sky.setMaterial(skyMat);
        
        return sky;
    }

    private static void checkImage(Image image){
//        if (image.getDepth() != 1)
//            throw new IllegalArgumentException("3D/Array images not allowed");

        if (image.getWidth() != image.getHeight())
            throw new IllegalArgumentException("Image width and height must be the same");

        if (image.getMultiSamples() != 1)
            throw new IllegalArgumentException("Multisample textures not allowed");
    }

    private static void checkImagesForCubeMap(Image ... images){
        if (images.length == 1) return;

        Format fmt = images[0].getFormat();
        int width = images[0].getWidth();
        int size = images[0].getData(0).capacity();

        checkImage(images[0]);

        for (int i = 1; i < images.length; i++){
            Image image = images[i];
            checkImage(images[i]);
            if (image.getFormat() != fmt) throw new IllegalArgumentException("Images must have same format");
            if (image.getWidth() != width) throw new IllegalArgumentException("Images must have same resolution");
            if (image.getData(0).capacity() != size) throw new IllegalArgumentException("Images must have same size");
        }
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south, Texture up, Texture down, Vector3f normalScale){
        Geometry sky = new Geometry("Sky", sphereMesh);
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);

        Image westImg  = west.getImage();
        Image eastImg  = east.getImage();
        Image northImg = north.getImage();
        Image southImg = south.getImage();
        Image upImg    = up.getImage();
        Image downImg  = down.getImage();

        checkImagesForCubeMap(westImg, eastImg, northImg, southImg, upImg, downImg);

        Image cubeImage = new Image(westImg.getFormat(), westImg.getWidth(), westImg.getHeight(), null);
        
        cubeImage.addData(westImg.getData(0));
        cubeImage.addData(eastImg.getData(0));
        
        cubeImage.addData(downImg.getData(0));
        cubeImage.addData(upImg.getData(0));

        cubeImage.addData(southImg.getData(0));
        cubeImage.addData(northImg.getData(0));

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

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south, Texture up, Texture down){
        return createSky(assetManager, west, east, north, south, up, down, Vector3f.UNIT_XYZ);
    }

    public static Spatial createSky(AssetManager assetManager, Texture texture, boolean sphereMap){
        return createSky(assetManager, texture, Vector3f.UNIT_XYZ, sphereMap);
    }

    public static Spatial createSky(AssetManager assetManager, String textureName, boolean sphereMap){
        TextureKey key = new TextureKey(textureName, true);
        key.setGenerateMips(true);
        key.setAsCube(!sphereMap);
        Texture tex = assetManager.loadTexture(key);
        return createSky(assetManager, tex, sphereMap);
    }

    

}
