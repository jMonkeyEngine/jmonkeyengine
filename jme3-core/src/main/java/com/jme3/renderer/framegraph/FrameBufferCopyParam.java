/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;

/**
 *
 * @author codex
 */
public class FrameBufferCopyParam implements RenderParameter<FrameBuffer> {
    
    private final String name;
    private Renderer renderer;
    private FrameBuffer target, source;
    private final boolean copyColor, copyDepth;
    private boolean ready = true;
    
    public FrameBufferCopyParam(String name, Renderer renderer, FrameBuffer target, boolean copyColor, boolean copyDepth) {
        this.name = name;
        this.renderer = renderer;
        this.target = target;
        this.copyColor = copyColor;
        this.copyDepth = copyDepth;
    }
    
    @Override
    public String getParameterName() {
        return name;
    }
    @Override
    public void accept(FrameBuffer value) {
        if (source != value || ready) {
            source = value;
            if ((source != null || target != null) && renderer != null) {
                renderer.copyFrameBuffer(source, target, copyColor, copyDepth);
                ready = false;
            }
        }
    }
    @Override
    public FrameBuffer produce() {
        return target;
    }
    
    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }
    public void setTarget(FrameBuffer target) {
        this.target = target;
    }
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public FrameBuffer getSource() {
        return source;
    }
    public boolean isCopyColor() {
        return copyColor;
    }
    public boolean isCopyDepth() {
        return copyDepth;
    }
    
}
