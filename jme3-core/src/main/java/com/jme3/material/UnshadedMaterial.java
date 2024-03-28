/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

/**
 * Material using Unshaded.j3md.
 * 
 * @author codex
 */
public class UnshadedMaterial extends Material {
    
    /**
     * Creates a material with Unshaded.j3md.
     * 
     * @param assetManager 
     */
    public UnshadedMaterial(AssetManager assetManager) {
        super(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    }
    
    /**
     * Sets the color map.
     * <p>
     * Defines the base color of the geometry.
     * <p>
     * default=null
     * 
     * @param colorMap 
     */
    public void setColorMap(Texture colorMap) {
        setTexture("ColorMap", colorMap);
    }
    
    /**
     * Sets the light map.
     * <p>
     * Defines which areas of the geometry are under light. Used mainly for baked
     * illumination on static scenes.
     * <p>
     * default=null
     * 
     * @param lightMap 
     */
    public void setLightMap(Texture lightMap) {
        setTexture("LightMap", lightMap);
    }
    
    /**
     * Sets the glow map.
     * <p>
     * Defines which sections of the geometry glow with BloomFilter.
     * <p>
     * default=null
     * 
     * @param glowMap 
     */
    public void setGlowMap(Texture glowMap) {
        setTexture("GlowMap", glowMap);
    }
    
    /**
     * Sets the main color of the geometry.
     * 
     * @param color 
     */
    public void setColor(ColorRGBA color) {
        setColor("Color", color);
    }
    
    /**
     * Sets the color the whole geometry will glow with BloomFilter.
     * <p>
     * default=null
     * 
     * @param glow 
     */
    public void setGlowColor(ColorRGBA glow) {
        setColor("GlowColor", glow);
    }
    
    /**
     * Sets the size each individual vertex is render as with
     * {@link com.jme3.scene.Mesh.Mode#Points}.
     * 
     * @param pointSize 
     */
    public void setPointSize(float pointSize) {
        setFloat("PointSize", pointSize);
    }
    
    /**
     * Sets the color desaturation value.
     * 
     * @param value 
     */
    public void setDesaturationValue(float value) {
        setFloat("DesaturationValue", value);
    }
    
    /**
     * Enables coloring by vertex attributes (Color mesh buffer).
     * 
     * @param vertColors 
     */
    public void useVertexColors(boolean vertColors) {
        setBoolean("VertexColor", vertColors);
    }
    
    /**
     * If true, a seperate set of texture coordinates (TexCoord2 mesh buffer)
     * will be used for the light map.
     * 
     * @param seperateTexCoord 
     */
    public void useSeperateTexCoord(boolean seperateTexCoord) {
        setBoolean("SeperateTexCoord", seperateTexCoord);
    }
    
    /**
     * Enables instancing.
     * 
     * @param instancing 
     */
    public void useInstancing(boolean instancing) {
        setBoolean("UseInstancing", instancing);
    }
    
}
