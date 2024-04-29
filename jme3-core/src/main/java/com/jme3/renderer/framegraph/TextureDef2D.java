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

    private final int width;
    private final int height;
    private final Image.Format format;
    
    public TextureDef2D(int width, int height, Image.Format format) {
        super(Texture2D.class);
        this.width = width;
        this.height = height;
        this.format = format;
    }

    @Override
    public Texture2D create() {
        return new Texture2D(width, height, format);
    }

    @Override
    public boolean acceptReallocationOf(Texture2D resource) {
        Image img = resource.getImage();
        return img.getWidth() == width && img.getHeight() == height && img.getFormat() == format;
    }
    
}
