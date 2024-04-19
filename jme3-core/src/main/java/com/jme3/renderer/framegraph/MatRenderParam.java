/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.material.Material;
import com.jme3.shader.VarType;

/**
 *
 * @author codex
 * @param <T>
 */
public class MatRenderParam <T> implements RenderParameter<T> {
    
    private final String name;
    private final Material material;
    private final VarType type;
    private T value;

    public MatRenderParam(String name, Material material, VarType type) {
        this.name = name;
        this.material = material;
        this.type = type;
    }

    @Override
    public String getParameterName() {
        return name;
    }
    @Override
    public void accept(T value) {
        this.value = value;
        material.setParam(name, type, this.value);
    }
    @Override
    public T produce() {
        return value;
    }
    @Override
    public void erase() {
        value = null;
    }
    
}
