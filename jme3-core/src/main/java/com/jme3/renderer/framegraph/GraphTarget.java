/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.ViewPort;

/**
 * Receives values from a FrameGraph.
 * 
 * @author codex
 * @param <T>
 */
public interface GraphTarget <T> {
    
    public void setGraphValue(ViewPort viewPort, T value);
    
}
