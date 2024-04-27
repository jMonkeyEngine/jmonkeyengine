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
public class ResourceDef <T> {
    
    private Class<T> resType;
    
    public T create() {
        return null;
    }
    
    public <R> T repurpose(ResourceDef<R> def, R resource) {
        if (resType.isAssignableFrom(resource.getClass())) {
            return (T)resource;
        }
        return null;
    }
    
}
