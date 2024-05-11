/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 * @param <T>
 */
public class TextureDef <T extends Texture> extends AbstractResourceDef<T> {

    private int width, height, depth, samples;
    private Image.Format format;
    
    public TextureDef(int width, int height, Image.Format format) {
        this(width, height, 0, 1, format);
    }
    public TextureDef(int width, int height, int depth, int samples, Image.Format format) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.samples = samples;
        this.format = format;
    }

    @Override
    public Texture createResource() {
        if (samples != 1) {
            return new Texture2D(width, height, samples, format);
        } else {
            return new Texture2D(width, height, format);
        }
    }
    @Override
    public Texture applyResource(Object resource) {
        Image img;
        if (resource instanceof Texture) {
            img = ((Texture)resource).getImage();
        } else if (resource instanceof Image) {
            img = (Image)resource;
        } else {
            return null;
        }
        if (img.getWidth() == width && img.getHeight() == height && (img.getDepth() == depth || depth <= 0)
                && img.getFormat() == format && img.getMultiSamples() == samples) {
            if (depth <= 0) {
                return new Texture2D(img);
            } else {
                return new Texture3D(img);
            }
        }
        return null;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public void setSamples(int samples) {
        this.samples = samples;
    }
    public void setFormat(Image.Format format) {
        this.format = format;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getSamples() {
        return samples;
    }
    public Image.Format getFormat() {
        return format;
    }
    
}
