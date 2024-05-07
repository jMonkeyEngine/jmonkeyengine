/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import java.util.function.Consumer;

/**
 *
 * @author codex
 * @param <T>
 */
public interface ResourceDef <T> {
    
    public T createResource();
    
    public T applyResource(Object resource);
    
    public default Consumer<T> getDisposalMethod() {
        return null;
    }
    
}
