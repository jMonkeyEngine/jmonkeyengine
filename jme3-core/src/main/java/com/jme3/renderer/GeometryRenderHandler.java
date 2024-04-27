/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer;

import com.jme3.scene.Geometry;

/**
 *
 * @author codex
 */
public interface GeometryRenderHandler {
    
    public boolean renderGeometry(RenderManager rm, Geometry geom);
    
}
