/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.parameters;

/**
 *
 * @author codex
 * @param <T>
 */
public class ValueRenderParam <T> implements RenderParameter<T> {
    
    private final String name;
    private T value;
    private T defaultValue;
    
    public ValueRenderParam(String name) {
        this(name, null, null);
    }
    public ValueRenderParam(String name, T value) {
        this(name, value, null);
    }
    public ValueRenderParam(String name, T value, T defaultValue) {
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
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
        return (value != null ? value : defaultValue);
    }
    
    public T produceRaw() {
        return value;
    }
    
    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
    
}
