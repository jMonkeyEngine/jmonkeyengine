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
public abstract class ResourceDef <T> {
    
    private final Class<T> resType;
    private int timeout = -1;

    public ResourceDef(Class<T> resType) {
        this.resType = resType;
    }
    
    public abstract T create();
    public abstract boolean applyRecycled(T resource);
    public void destroy(T resource) {}
    
    public void setRecycleTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public boolean isOfResourceType(Class type) {
        return resType != null && resType.isAssignableFrom(type);
    }
    public boolean isAcceptsRecycled() {
        return resType != null;
    }
    public boolean isRecycleable() {
        return true;
    }
    public int getRecycleTimeout() {
        return timeout;
    }
    
}
