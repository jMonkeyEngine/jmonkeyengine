/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.io;

import com.jme3.renderer.ViewPort;

/**
 * Provides values to a framegraph from game logic.
 * 
 * @author codex
 * @param <T>
 */
public interface GraphSource <T> {
    
    /**
     * Gets the value provided to the framegraph.
     * 
     * @param viewPort viewport currently being rendered
     * @return value (may be null in some circumstances)
     */
    public T getGraphValue(ViewPort viewPort);
    
}
