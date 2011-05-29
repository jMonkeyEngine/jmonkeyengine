package com.jme3.util.android;

import java.util.ArrayList;
import android.graphics.Bitmap;

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


/**
 * <code>AndroidSkyFactory</code> creates a sky box spatial
 * @author larynx, derived from SkyFactory and adapted for android
 *
 */
public class AndroidSkyFactory 
{
    private static final Sphere sphereMesh = new Sphere(10, 10, 101f, false, true);

    public static Spatial createSky(AssetManager assetManager, Texture texture, Vector3f normalScale, boolean sphereMap)
    {
        Geometry sky = new Geometry("Sky", sphereMesh);
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);

        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");           
        skyMat.setVector3("NormalScale", normalScale);
        if (sphereMap)
        {
            skyMat.setBoolean("SphereMap", sphereMap);
        }
        else if (!(texture instanceof TextureCubeMap))
        {
            // make sure its a cubemap
            Image img = texture.getImage();
            texture = new TextureCubeMap();
            texture.setImage(img);
        }
        skyMat.setTexture("Texture", texture);
        sky.setMaterial(skyMat);
        
        return sky;
    }

    private static void checkImage(Image image)
    {
        if (image.getWidth() != image.getHeight())
            throw new IllegalArgumentException("Image width and height must be the same");

        if (image.getMultiSamples() != 1)
            throw new IllegalArgumentException("Multisample textures not allowed");
    }

    private static void checkImagesForCubeMap(Image ... images)
    {
        if (images.length == 1) return;

        Format fmt = images[0].getFormat();
        int width = images[0].getWidth();
        int height = images[0].getHeight();

        checkImage(images[0]);

        for (int i = 1; i < images.length; i++)
        {
            Image image = images[i];
            checkImage(images[i]);
            if (image.getFormat() != fmt) throw new IllegalArgumentException("Images must have same format");
            if (image.getWidth() != width) throw new IllegalArgumentException("Images must have same width");
            if (image.getHeight() != height) throw new IllegalArgumentException("Images must have same height");
        }
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south, 
                                    Texture up, Texture down, Vector3f normalScale)
    {
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

        ArrayList<Bitmap> arrayList = new ArrayList<Bitmap>(6);
        
        arrayList.add((Bitmap)westImg.getEfficentData());
        arrayList.add((Bitmap)eastImg.getEfficentData());
        
        arrayList.add((Bitmap)downImg.getEfficentData());
        arrayList.add((Bitmap)upImg.getEfficentData());

        arrayList.add((Bitmap)southImg.getEfficentData());
        arrayList.add((Bitmap)northImg.getEfficentData());

        cubeImage.setEfficentData(arrayList);

        TextureCubeMap cubeMap = new TextureCubeMap(cubeImage);        
        cubeMap.setAnisotropicFilter(0);
        cubeMap.setMagFilter(Texture.MagFilter.Bilinear);
        cubeMap.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        cubeMap.setWrap(Texture.WrapMode.EdgeClamp);
        
        
        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");            
        skyMat.setTexture("Texture", cubeMap);            
        skyMat.setVector3("NormalScale", normalScale);
        sky.setMaterial(skyMat);

        return sky;
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south, 
                                    Texture up, Texture down)
    {
        return createSky(assetManager, west, east, north, south, up, down, Vector3f.UNIT_XYZ);
    }

    public static Spatial createSky(AssetManager assetManager, Texture texture, boolean sphereMap)
    {
        return createSky(assetManager, texture, Vector3f.UNIT_XYZ, sphereMap);
    }

    public static Spatial createSky(AssetManager assetManager, String textureName, boolean sphereMap)
    {
        TextureKey key = new TextureKey(textureName, true);
        key.setGenerateMips(true);
        key.setAsCube(!sphereMap);
        Texture tex = assetManager.loadTexture(key);
        return createSky(assetManager, tex, sphereMap);
    }
}