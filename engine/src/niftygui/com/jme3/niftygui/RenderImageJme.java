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

package com.jme3.niftygui;

import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import de.lessvoid.nifty.spi.render.RenderImage;

public class RenderImageJme implements RenderImage {

    private Texture2D texture;
    private Image image;
    private int width;
    private int height;

    public RenderImageJme(String filename, boolean linear, NiftyJmeDisplay display){
        TextureKey key = new TextureKey(filename, true);

        key.setAnisotropy(0);
        key.setAsCube(false);
        key.setGenerateMips(false);
        
        texture = (Texture2D) display.getAssetManager().loadTexture(key);
        texture.setMagFilter(linear ? MagFilter.Bilinear : MagFilter.Nearest);
        texture.setMinFilter(linear ? MinFilter.BilinearNoMipMaps : MinFilter.NearestNoMipMaps);
        image = texture.getImage();

        width = image.getWidth();
        height = image.getHeight();
    }

    public RenderImageJme(Texture2D texture){
        if (texture.getImage() == null)
            throw new IllegalArgumentException("texture.getImage() cannot be null");
        
        this.texture = texture;
        this.image = texture.getImage();
        width = image.getWidth();
        height = image.getHeight();
    }

    public Texture2D getTexture(){
        return texture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void dispose() {
    }
}
