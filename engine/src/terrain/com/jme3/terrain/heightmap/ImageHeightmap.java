/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

import com.jme3.texture.Image;

/**
 * A heightmap that is built off an image.
 * If you want to be able to supply different Image types to 
 * ImageBaseHeightMapGrid, you need to implement this interface,
 * and have that class extend Abstract heightmap.
 * 
 * @author bowens
 * @deprecated
 */
public interface ImageHeightmap {
    
    /**
     * Set the image to use for this heightmap
     */
    //public void setImage(Image image);
    
    /**
     * The BufferedImage.TYPE_ that is supported
     * by this ImageHeightmap
     */
    //public int getSupportedImageType();
}
