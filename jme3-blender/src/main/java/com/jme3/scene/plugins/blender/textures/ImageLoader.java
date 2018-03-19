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
package com.jme3.scene.plugins.blender.textures;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.texture.plugins.HDRLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An image loader class. It uses three loaders (AWTLoader, TGALoader and DDSLoader) in an attempt to load the image from the given
 * input stream.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ImageLoader extends AWTLoader {
    private static final Logger LOGGER    = Logger.getLogger(ImageLoader.class.getName());
    private static final Logger hdrLogger = Logger.getLogger(HDRLoader.class.getName()); // Used to silence HDR Errors
    
    /**
     * List of Blender-Supported Texture Extensions (we have to guess them, so
     * the AssetLoader can find them. Not good, but better than nothing.
     * Source: https://docs.blender.org/manual/en/dev/data_system/files/media/image_formats.html
     */
    private static final String[] extensions = new String[]
        { /* Windows Bitmap */".bmp",
          /* Iris */ ".sgi", ".rgb", ".bw",
          /* PNG */ ".png",
          /* JPEG */ ".jpg", ".jpeg",
          /* JPEG 2000 */ ".jp2", ".j2c",
          /* Targa */".tga",
          /* Cineon & DPX */".cin", ".dpx",
          /* OpenEXR */ ".exr",
          /* Radiance HDR */ ".hdr",
          /* TIFF */ ".tif", ".tiff",
          /* DDS (Direct X) */ ".dds" };

    /**
     * This method loads a image which is packed into the blender file.
     * It makes use of all the registered AssetLoaders
     * 
     * @param inputStream
     *            blender input stream
     * @param startPosition
     *            position in the stream where the image data starts
     * @param flipY
     *            if the image should be flipped (does not work with DirectX image)
     * @return loaded image or null if it could not be loaded
     * @deprecated This method has only been left in for API compability.
     * Use loadTexture instead
     */
    public Image loadImage(AssetManager assetManager, BlenderInputStream inputStream, int startPosition, boolean flipY) {
        Texture tex = loadTexture(assetManager, inputStream, startPosition, flipY);
        
        if (tex == null) {
            return null;
        } else {
            return tex.getImage();
        }
    }
    
    /**
     * This method loads a texture which is packed into the blender file.
     * It makes use of all the registered AssetLoaders
     * 
     * @param inputStream
     *            blender input stream
     * @param startPosition
     *            position in the stream where the image data starts
     * @param flipY
     *            if the image should be flipped (does not work with DirectX image)
     * @return loaded texture or null if it could not be loaded
     */
    public Texture loadTexture(AssetManager assetManager, BlenderInputStream inputStream, int startPosition, boolean flipY) {
        inputStream.setPosition(startPosition);
        TextureKey tKey;
        Texture result = null;
        
        hdrLogger.setLevel(Level.SEVERE); // When we bruteforce try HDR on a non hdr file, it prints unreadable chars
        
        for (String ext: extensions) {
            tKey = new TextureKey("dummy" + ext, flipY);
            try {
                result = assetManager.loadAssetFromStream(tKey, inputStream);
            } catch (Exception e) {
                continue;
            }
            
            if (result != null) {
                break; // Could locate a possible asset
            }
        }
        
        if (result == null) {
            LOGGER.warning("Texture could not be loaded by any of the available loaders!\n"
                    + "Since the file has been packed into the blender file, there is no"
                    + "way for us to tell you which texture it was.");
        }
        
        return result;
    }
}
