/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.material.logic;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import java.util.EnumSet;

/**
 * 
 * 
 * @author codex
 */
public interface RenderLogic {
    
    /**
     * 
     * @param def 
     */
    public void initTechniqueDef(TechniqueDef def);
    
    /**
     * 
     * @param assetManager
     * @param renderManager
     * @param rendererCaps
     * @param lights
     * @param defines
     * @return 
     */
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager, 
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines);
    
    /**
     * 
     * @param rm
     * @param shader
     * @param geometry
     * @param lights
     * @param lastBindUnits 
     */
    public void render(RenderManager rm, Shader shader, Geometry geometry,
            LightList lights, Material.BindUnits lastBindUnits);
    
}
