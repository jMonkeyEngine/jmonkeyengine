/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer;

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;

/**
 * 
 * 
 * @author codex
 */
public interface RenderLogic {
    
    public boolean render(RenderManager rm, Geometry geometry);
    
    /**
     * 
     * @param renderManager
     * @param shader
     * @param geometry
     * @param lights
     * @param lastBindUnits 
     * @return 
     */
    public boolean render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, Material.BindUnits lastBindUnits);
    
}
