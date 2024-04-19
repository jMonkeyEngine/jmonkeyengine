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
    private final String matName;
    private final Material material;
    private final VarType type;
    private boolean debug = false;
    private T value;
    
    public MatRenderParam(String name, Material material, VarType type) {
        this(name, material, name, type);
    }
    public MatRenderParam(String name, Material material, String matName, VarType type) {
        this.name = name;
        this.matName = matName;
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
        if (debug) {
            System.out.println("assign to material: "+this.value);
        }
        material.setParam(matName, type, this.value);
    }
    @Override
    public T produce() {
        return value;
    }
    @Override
    public void erase() {
        value = null;
    }
    
    public void enableDebug() {
        debug = true;
    }
    
}
