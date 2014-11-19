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
package com.jme3.texture;

import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a Texture array
 * warning, this feature is only supported on opengl 3.0 version.
 * To check if a hardware supports TextureArray check : 
 * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
 * @author phate666
 */
public class TextureArray extends Texture {

    private WrapMode wrapS = WrapMode.EdgeClamp;
    private WrapMode wrapT = WrapMode.EdgeClamp;

    /**
     * Construct a TextureArray
     * warning, this feature is only supported on opengl 3.0 version.
     * To check if a hardware supports TextureArray check : 
     * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
     */
    public TextureArray() {
        super();
    }

    /**
     * Construct a TextureArray from the given list of images.
     * To check if a hardware supports TextureArray check : 
     * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
     * @param images 
     */
    public TextureArray(List<Image> images) {
        super();
        
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        Format format = images.get(0).getFormat();
        ColorSpace colorSpace = images.get(0).getColorSpace();
        int[] mipMapSizes = images.get(0).getMipMapSizes();
        Image arrayImage = new Image(format, width, height, null, colorSpace);
        arrayImage.setMipMapSizes(mipMapSizes);
        
        for (Image img : images) {
            if (img.getHeight() != height || img.getWidth() != width) {
                throw new IllegalArgumentException("Images in texture array must have same dimensions");
            }
            if (img.getFormat() != format) {
                throw new IllegalArgumentException("Images in texture array must have same format");
            }
            if (!Arrays.equals(mipMapSizes, img.getMipMapSizes())) {
                throw new IllegalArgumentException("Images in texture array must have same mipmap sizes");
            }
            
            arrayImage.addData(img.getData(0));
        }
        
        setImage(arrayImage);
    }

    @Override
    public Texture createSimpleClone() {
        TextureArray clone = new TextureArray();
        createSimpleClone(clone);
        return clone;
    }

    @Override
    public Texture createSimpleClone(Texture rVal) {
        rVal.setWrap(WrapAxis.S, wrapS);
        rVal.setWrap(WrapAxis.T, wrapT);
        return super.createSimpleClone(rVal);
    }

    @Override
    public Type getType() {
        return Type.TwoDimensionalArray;
    }

    @Override
    public WrapMode getWrap(WrapAxis axis) {
        switch (axis) {
            case S:
                return wrapS;
            case T:
                return wrapT;
            default:
                throw new IllegalArgumentException("invalid WrapAxis: " + axis);
        }
    }

    @Override
    public void setWrap(WrapAxis axis, WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        } else if (axis == null) {
            throw new IllegalArgumentException("axis can not be null.");
        }
        switch (axis) {
            case S:
                this.wrapS = mode;
                break;
            case T:
                this.wrapT = mode;
                break;
            default:
                throw new IllegalArgumentException("Not applicable for 2D textures");
        }
    }

    @Override
    public void setWrap(WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        this.wrapS = mode;
        this.wrapT = mode;
    }
}