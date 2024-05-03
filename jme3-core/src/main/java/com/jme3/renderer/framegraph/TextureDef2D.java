/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class TextureDef2D extends ResourceDef<Texture2D> {

    private int width, height, samples;
    private Image.Format format;
    
    public TextureDef2D(int width, int height, Image.Format format) {
        this(width, height, 1, format);
    }
    public TextureDef2D(int width, int height, int samples, Image.Format format) {
        super(Texture2D.class);
        this.width = width;
        this.height = height;
        this.samples = samples;
        this.format = format;
    }

    @Override
    public Texture2D create() {
        if (samples != 1) {
            return new Texture2D(width, height, samples, format);
        } else {
            return new Texture2D(width, height, format);
        }
    }
    @Override
    public boolean applyRecycled(Texture2D resource) {
        Image img = resource.getImage();
        return img.getWidth() == width && img.getHeight() == height
                && img.getFormat() == format && img.getMultiSamples() == samples;
    }
    @Override
    public void destroy(Texture2D tex) {
        tex.getImage().dispose();
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
