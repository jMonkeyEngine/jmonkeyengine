/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.parameters;

import com.jme3.texture.FrameBuffer.FrameBufferTextureTarget;
import com.jme3.texture.Texture;

/**
 *
 * @author codex
 */
public class TextureTargetParam implements RenderParameter<Texture> {
    
    private final String name;
    private FrameBufferTextureTarget target;
    
    public TextureTargetParam(String name, FrameBufferTextureTarget target) {
        this.name = name;
        this.target = target;
    }
    
    @Override
    public Texture produce() {
        if (target != null) {
            return target.getTexture();
        } else {
            return null;
        }
    }
    
    @Override
    public String getParameterName() {
        return name;
    }
    
    @Override
    public void accept(Texture value) {}
    
    @Override
    public void erase() {
        target = null;
    }
    
    public void setTextureTarget(FrameBufferTextureTarget target) {
        this.target = target;
    }
    
    public FrameBufferTextureTarget getTextureTarget() {
        return target;
    }
    
}
