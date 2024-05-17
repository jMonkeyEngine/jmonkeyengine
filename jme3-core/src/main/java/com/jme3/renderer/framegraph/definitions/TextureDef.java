/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import java.util.Objects;
import java.util.function.Function;

/**
 *
 * @author codex
 * @param <T>
 */
public class TextureDef <T extends Texture> extends AbstractResourceDef<T> {

    private final Class<T> type;
    private Function<Image, T> textureBuilder;
    private Function<Object, Image> imageExtractor;
    private int width = 128;
    private int height = 128;
    private int depth = 0;
    private int samples = 1;
    private Image.Format format;
    private ColorSpace colorSpace = ColorSpace.Linear;
    private Texture.MagFilter magFilter = Texture.MagFilter.Nearest;
    private Texture.MinFilter minFilter = Texture.MinFilter.NearestNearestMipMap;
    private Texture.ShadowCompareMode shadowCompare;
    private Texture.WrapMode wrapS, wrapT, wrapR;
    private boolean formatFlexible = false;
    private boolean colorSpaceFlexible = false;
    
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder) {
        this(type, textureBuilder, Image.Format.RGBA8);
    }
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder, Image.Format format) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(textureBuilder);
        Objects.requireNonNull(format);
        this.type = type;
        this.textureBuilder = textureBuilder;
        this.format = format;
    }
    
    @Override
    public T createResource() {
        return createTexture(new Image(format, width, height, depth, null, colorSpace));
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
    
    private T createTexture(Image img) {
        T tex = textureBuilder.apply(img);
        tex.getImage().setMultiSamples(samples);
        setupTexture(tex);
        return tex;
    }
    private void setupTexture(Texture tex) {
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
        if (wrapR != null) {
            tex.setWrap(Texture.WrapAxis.R, wrapR);
        }
    }
    private boolean validateImage(Image img) {
        return img.getWidth() == width && img.getHeight() == height && (depth == 0 || img.getDepth() == depth)
            && (samples <= 0 || img.getMultiSamples() == samples)
            && (img.getFormat() == format || (formatFlexible && img.getFormat().isDepthFormat() == format.isDepthFormat()))
            && (colorSpaceFlexible || img.getColorSpace() == colorSpace);
    }

    public void setTextureBuilder(Function<Image, T> textureBuilder) {
        Objects.requireNonNull(textureBuilder);
        this.textureBuilder = textureBuilder;
    }
    public void setImageExtractor(Function<Object, Image> imageExtractor) {
        this.imageExtractor = imageExtractor;
    }
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than zero.");
        }
        this.width = width;
    }
    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be greater than zero.");
        }
        this.height = height;
    }
    public void setDepth(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than zero.");
        }
        this.depth = depth;
    }
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }
    public void setSize(int width, int height, int depth) {
        setWidth(width);
        setHeight(height);
        setDepth(depth);
    }
    public void setSamples(int samples) {
        if (samples <= 0) {
            throw new IllegalArgumentException("Image samples must be greater than zero.");
        }
        this.samples = samples;
    }
    public void setFormat(Image.Format format) {
        Objects.requireNonNull(format);
        this.format = format;
    }
    public void setFormatFlexible(boolean formatFlexible) {
        this.formatFlexible = formatFlexible;
    }
    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }
    public void setMagFilter(Texture.MagFilter magFilter) {
        this.magFilter = magFilter;
    }
    public void setMinFilter(Texture.MinFilter minFilter) {
        this.minFilter = minFilter;
    }
    public void setColorSpaceFlexible(boolean colorSpaceFlexible) {
        this.colorSpaceFlexible = colorSpaceFlexible;
    }
    public void setWrap(Texture.WrapMode mode) {
        wrapS = wrapT = wrapR = mode;
    }
    public void setWrap(Texture.WrapAxis axis, Texture.WrapMode mode) {
        switch (axis) {
            case S: wrapS = mode; break;
            case T: wrapT = mode; break;
            case R: wrapR = mode; break;
        }
    }

    public Class<T> getType() {
        return type;
    }
    public Function<Image, T> getTextureBuilder() {
        return textureBuilder;
    }
    public Function<Object, Image> getImageExtractor() {
        return imageExtractor;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getDepth() {
        return depth;
    }
    public int getSamples() {
        return samples;
    }
    public Image.Format getFormat() {
        return format;
    }
    public boolean isFormatFlexible() {
        return formatFlexible;
    }
    public ColorSpace getColorSpace() {
        return colorSpace;
    }
    public Texture.MagFilter getMagFilter() {
        return magFilter;
    }
    public Texture.MinFilter getMinFilter() {
        return minFilter;
    }
    public boolean isColorSpaceFlexible() {
        return colorSpaceFlexible;
    }
    public Texture.WrapMode getWrap(Texture.WrapAxis axis) {
        switch (axis) {
            case S: return wrapS;
            case T: return wrapT;
            case R: return wrapR;
            default: throw new IllegalArgumentException();
        }
    }
    
}
