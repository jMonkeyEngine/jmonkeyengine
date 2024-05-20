/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.io;

import com.jme3.renderer.ViewPort;

/**
 *
 * @author codex
 * @param <T>
 */
public interface GraphSource <T> {
    
    public T getGraphValue(ViewPort viewPort);
    
}
