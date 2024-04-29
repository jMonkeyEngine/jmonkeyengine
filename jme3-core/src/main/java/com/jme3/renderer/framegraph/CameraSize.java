/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class CameraSize {
    
    private int width, height;
    
    public CameraSize() {}
    
    public boolean needsUpdate(Camera cam) {
        return width != cam.getWidth() || height != cam.getHeight();
    }
    public boolean needsUpdate(CameraSize camSize) {
        return width != camSize.width || height != camSize.height;
    }
    public boolean update(Camera cam) {
        boolean changed = needsUpdate(cam);
        width = cam.getWidth();
        height = cam.getHeight();
        return changed;
    }
    public boolean update(CameraSize camSize) {
        boolean changed = needsUpdate(camSize);
        width = camSize.width;
        height = camSize.height;
        return changed;
    }
    
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    
}
