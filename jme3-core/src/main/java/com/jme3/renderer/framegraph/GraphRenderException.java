/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * Raised when a fatal error occurs during FrameGraph processes.
 * 
 * @author codex
 */
public class GraphRenderException extends RuntimeException {
    
    public GraphRenderException(String message) {
        super(message);
    }
    
}
