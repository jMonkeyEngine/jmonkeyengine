/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph.definitions;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * General resource definition for textures.
 * <p>
 * Other textures and objects only apply for reallocation if a contained
 * image meets the parameters of this definition.
 * 
 * @author codex
 * @param <T>
 */
public class TextureDef <T extends Texture> extends AbstractResourceDef<T> implements Consumer<T> {
    
    private final Class<T> type;
    private Function<Image, T> textureBuilder;
    private Function<Object, Image> imageExtractor;
    private int width = 128;
    private int height = 128;
    private int depth = 0;
    private int samples = 1;
    private Image.Format format;
    private ColorSpace colorSpace = ColorSpace.Linear;
    private Texture.MagFilter magFilter = Texture.MagFilter.Bilinear;
    private Texture.MinFilter minFilter = Texture.MinFilter.BilinearNoMipMaps;
    private Texture.ShadowCompareMode shadowCompare = Texture.ShadowCompareMode.Off;
    private Texture.WrapMode wrapS, wrapT, wrapR;
    private boolean formatFlexible = false;
    private boolean colorSpaceFlexible = false;
    
    /**
     * 
     * @param type
     * @param textureBuilder 
     */
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder) {
        this(type, textureBuilder, Image.Format.RGBA8);
    }
    /**
     * 
     * @param type
     * @param textureBuilder
     * @param format 
     */
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder, Image.Format format) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(textureBuilder);
        Objects.requireNonNull(format);
        this.type = type;
        this.textureBuilder = textureBuilder;
        this.format = format;
        if (!Texture2D.class.isAssignableFrom(type)) {
            depth = 1;
        }
    }
    
    @Override
    public T createResource() {
        Image img;
        if (depth > 0) {
            img = new Image(format, width, height, depth, new ArrayList<>(depth), colorSpace);
        } else {
            img = new Image(format, width, height, null, colorSpace);
        }
        return createTexture(img);
    }
    @Override
    public T applyDirectResource(Object resource) {
        if (type.isAssignableFrom(resource.getClass())) {
            T tex = (T)resource;
            if (validateImage(tex.getImage())) {
                setupTexture(tex);
                return tex;
            }
        }
        return null;
    }
    @Override
    public T applyIndirectResource(Object resource) {
        Image img;
        if (imageExtractor != null) {
            if ((img = imageExtractor.apply(resource)) == null) {
                return null;
            }
        } else if (resource instanceof Texture) {
            img = ((Texture)resource).getImage();
        } else if (resource instanceof Image) {
            img = (Image)resource;
        } else {
            return null;
        }
        if (validateImage(img)) {
            return textureBuilder.apply(img);
        }
        return null;
    }
    @Override
    public Consumer<T> getDisposalMethod() {
        return this;
    }
    @Override
    public boolean isDisposeOnRelease() {
        return false;
    }
    @Override
    public void accept(T t) {
        t.getImage().dispose();
    }
    
    protected T createTexture(Image img) {
        T tex = textureBuilder.apply(img);
        tex.getImage().setMultiSamples(samples);
        setupTexture(tex);
        return tex;
    }
    protected void setupTexture(Texture tex) {
        if (magFilter != null) {
            tex.setMagFilter(magFilter);
        }
        if (minFilter != null) {
            tex.setMinFilter(minFilter);
        }
        if (shadowCompare != null) {
            tex.setShadowCompareMode(shadowCompare);
        }
        if (wrapS != null) {
            tex.setWrap(Texture.WrapAxis.S, wrapS);
        }
        if (wrapT != null) {
            tex.setWrap(Texture.WrapAxis.T, wrapT);
        }
        if (wrapR != null && depth > 0) {
            tex.setWrap(Texture.WrapAxis.R, wrapR);
        }
    }
    protected boolean validateImage(Image img) {
        return validateImageSize(img)
            && (samples <= 0 || img.getMultiSamples() == samples)
            && validateImageFormat(img)
            && (colorSpaceFlexible || img.getColorSpace() == colorSpace);
    }
    protected boolean validateImageSize(Image img) {
        return img.getWidth() == width && img.getHeight() == height && (depth == 0 || img.getDepth() == depth);
    }
    protected boolean validateImageFormat(Image img) {
        return img.getFormat() == format || (formatFlexible && img.getFormat().isDepthFormat() == format.isDepthFormat());
    }
    
    /**
     * Sets the function that constructs a texture from an image.
     * 
     * @param textureBuilder 
     */
    public void setTextureBuilder(Function<Image, T> textureBuilder) {
        Objects.requireNonNull(textureBuilder);
        this.textureBuilder = textureBuilder;
    }
    /**
     * Sets the function that extracts an Image from and object.
     * 
     * @param imageExtractor 
     */
    public void setImageExtractor(Function<Object, Image> imageExtractor) {
        this.imageExtractor = imageExtractor;
    }
    /**
     * Sets the texture width.
     * 
     * @param width texture width greater than zero
     */
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than zero.");
        }
        this.width = width;
    }
    /**
     * Sets the texture height.
     * 
     * @param height texture height greater than zero
     */
    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be greater than zero.");
        }
        this.height = height;
    }
    /**
     * Sets the texture depth.
     * <p>
     * Values less than or equal to zero indicate a 2D texture.
     * 
     * @param depth texture depth (or less or equal to than zero)
     */
    public void setDepth(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than zero.");
        }
        this.depth = depth;
    }
    /**
     * Sets the width and height of the texture to the length.
     * 
     * @param length 
     */
    public void setSquare(int length) {
        width = height = length;
    }
    /**
     * Sets the width, height, and depth of the texture to the length.
     * 
     * @param length 
     */
    public void setCube(int length) {
        width = height = depth = length;
    }
    /**
     * Sets the width and height of the texture.
     * 
     * @param width
     * @param height 
     */
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }
    /**
     * Sets the width, height, and depth of the texture.
     * 
     * @param width
     * @param height
     * @param depth 
     */
    public void setSize(int width, int height, int depth) {
        setWidth(width);
        setHeight(height);
        setDepth(depth);
    }
    /**
     * Sets the given texture demensions to contain the specified number of pixels.
     * 
     * @param pixels
     * @param w true to set width
     * @param h true to set height
     * @param d true to set depth
     */
    public void setNumPixels(int pixels, boolean w, boolean h, boolean d) {
        width = height = 1;
        if (depth > 0) depth = 1;
        else depth = 0;
        int n = 0;
        if (w) n++;
        if (h) n++;
        if (d) n++;
        int length;
        switch (n) {
            case 3:  length = (int)Math.ceil(Math.cbrt(pixels)); break;
            case 2:  length = (int)Math.ceil(Math.sqrt(pixels)); break;
            default: length = pixels;
        }
        if (w) width = length;
        if (h) height = length;
        if (d) depth = length;
    }
    /**
     * Sets the number of samples of the texture's image.
     * 
     * @param samples 
     */
    public void setSamples(int samples) {
        if (samples <= 0) {
            throw new IllegalArgumentException("Image samples must be greater than zero.");
        }
        this.samples = samples;
    }
    /**
     * Sets the format of the image.
     * 
     * @param format 
     */
    public void setFormat(Image.Format format) {
        Objects.requireNonNull(format);
        this.format = format;
    }
    /**
     * Sets reallocation so that the target image does not need the exact
     * format of the definition.
     * <p>
     * default=false
     * 
     * @param formatFlexible 
     */
    public void setFormatFlexible(boolean formatFlexible) {
        this.formatFlexible = formatFlexible;
    }
    /**
     * Sets the color space of the texture.
     * 
     * @param colorSpace 
     */
    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }
    /**
     * Sets the magnification filter of the texture.
     * 
     * @param magFilter mag filter, or null to use default
     */
    public void setMagFilter(Texture.MagFilter magFilter) {
        this.magFilter = magFilter;
    }
    /**
     * Sets the minification filter of the texture.
     * 
     * @param minFilter min filter, or null to use default
     */
    public void setMinFilter(Texture.MinFilter minFilter) {
        this.minFilter = minFilter;
    }
    /**
     * Sets reallocation so that the target image does not need the exact
     * format of the definition.
     * 
     * @param colorSpaceFlexible 
     */
    public void setColorSpaceFlexible(boolean colorSpaceFlexible) {
        this.colorSpaceFlexible = colorSpaceFlexible;
    }
    /**
     * Sets the wrap mode on all axis.
     * 
     * @param mode 
     */
    public void setWrap(Texture.WrapMode mode) {
        wrapS = wrapT = wrapR = mode;
    }
    /**
     * Sets the wrap mode on the specified axis.
     * 
     * @param axis
     * @param mode 
     */
    public void setWrap(Texture.WrapAxis axis, Texture.WrapMode mode) {
        switch (axis) {
            case S: wrapS = mode; break;
            case T: wrapT = mode; break;
            case R: wrapR = mode; break;
        }
    }
    
    /**
     * Gets the texture type handled by this definition.
     * 
     * @return 
     */
    public Class<T> getType() {
        return type;
    }
    /**
     * 
     * @return 
     */
    public Function<Image, T> getTextureBuilder() {
        return textureBuilder;
    }
    /**
     * 
     * @return 
     */
    public Function<Object, Image> getImageExtractor() {
        return imageExtractor;
    }
    /**
     * 
     * @return 
     */
    public int getWidth() {
        return width;
    }
    /**
     * 
     * @return 
     */
    public int getHeight() {
        return height;
    }
    /**
     * 
     * @return 
     */
    public int getDepth() {
        return depth;
    }
    /**
     * 
     * @return 
     */
    public int getSamples() {
        return samples;
    }
    /**
     * 
     * @return 
     */
    public Image.Format getFormat() {
        return format;
    }
    /**
     * 
     * @return 
     */
    public boolean isFormatFlexible() {
        return formatFlexible;
    }
    /**
     * 
     * @return 
     */
    public ColorSpace getColorSpace() {
        return colorSpace;
    }
    /**
     * 
     * @return 
     */
    public Texture.MagFilter getMagFilter() {
        return magFilter;
    }
    /**
     * 
     * @return 
     */
    public Texture.MinFilter getMinFilter() {
        return minFilter;
    }
    /**
     * 
     * @return 
     */
    public boolean isColorSpaceFlexible() {
        return colorSpaceFlexible;
    }
    /**
     * Gets the wrap mode on the specified axis.
     * 
     * @param axis
     * @return 
     */
    public Texture.WrapMode getWrap(Texture.WrapAxis axis) {
        switch (axis) {
            case S: return wrapS;
            case T: return wrapT;
            case R: return wrapR;
            default: throw new IllegalArgumentException();
        }
    }
    
}
