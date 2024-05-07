/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class TextureDef2D implements ResourceDef<Texture2D> {

    private int width, height, samples;
    private Image.Format format;
    
    public TextureDef2D(int width, int height, Image.Format format) {
        this(width, height, 1, format);
    }
    public TextureDef2D(int width, int height, int samples, Image.Format format) {
        this.width = width;
        this.height = height;
        this.samples = samples;
        this.format = format;
    }

    @Override
    public Texture2D createResource() {
        if (samples != 1) {
            return new Texture2D(width, height, samples, format);
        } else {
            return new Texture2D(width, height, format);
        }
    }
    @Override
    public Texture2D applyResource(Object resource) {
        if (!(resource instanceof Texture2D)) {
            return null;
        }
        Texture2D tex = (Texture2D)resource;
        Image img = tex.getImage();
        if (img.getWidth() == width && img.getHeight() == height
                && img.getFormat() == format && img.getMultiSamples() == samples) {
            return tex;
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
