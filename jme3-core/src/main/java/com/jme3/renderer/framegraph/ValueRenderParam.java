/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public class ValueRenderParam <T> implements RenderParameter<T> {
    
    private final String name;
    private T value;
    
    public ValueRenderParam(String name) {
        this(name, null);
    }
    public ValueRenderParam(String name, T value) {
        this.name = name;
        this.value = value;
    }
    
    @Override
    public String getParameterName() {
        return name;
    }
    @Override
    public void accept(T value) {
        this.value = value;
    }
    @Override
    public T produce() {
        return value;
    }
    
}
