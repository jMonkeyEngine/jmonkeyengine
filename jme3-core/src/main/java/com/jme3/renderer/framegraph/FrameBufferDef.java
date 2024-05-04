/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;

/**
 *
 * @author codex
 */
public class FrameBufferDef extends ResourceDef<FrameBuffer> {

    private int width, height, samples;
    
    public FrameBufferDef(int width, int height, int samples) {
        super(FrameBuffer.class);
        this.width = width;
        this.height = height;
        this.samples = samples;
    }

    @Override
    public FrameBuffer create() {
        return new FrameBuffer(width, height, samples);
    }
    @Override
    public boolean applyRecycled(FrameBuffer r) {
        return r.getWidth() == width && r.getHeight() == height && r.getSamples() == samples;
    }
    @Override
    public void destroy(FrameBuffer r) {
        r.dispose();
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

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getSamples() {
        return samples;
    }
    
}
