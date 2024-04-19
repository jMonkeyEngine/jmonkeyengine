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
public class FrameBufferParam extends ValueRenderParam<FrameBuffer> {
    
    private boolean copyColor, copyDepth, copyStencil;

    public FrameBufferParam(String name, boolean copyColor, boolean copyDepth, boolean copyStencil) {
        super(name);
        this.copyColor = copyColor;
        this.copyDepth = copyDepth;
        this.copyStencil = copyStencil;
    }
    public FrameBufferParam(String name, FrameBuffer value, boolean copyColor, boolean copyDepth, boolean copyStencil) {
        super(name, value);
        this.copyColor = copyColor;
        this.copyDepth = copyDepth;
        this.copyStencil = copyStencil;
    }

    public boolean isCopyColor() {
        return copyColor;
    }
    public boolean isCopyDepth() {
        return copyDepth;
    }
    public boolean isCopyStencil() {
        return copyStencil;
    }
    
}
