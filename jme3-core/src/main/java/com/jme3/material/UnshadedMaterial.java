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
 *
 * @author codex
 */
public class UnshadedMaterial extends Material {

    public UnshadedMaterial(AssetManager assetManager) {
        super(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    }
    
    public void setColorMap(Texture colorMap) {
        setTexture("ColorMap", colorMap);
    }
    public void setLightMap(Texture lightMap) {
        setTexture("LightMap", lightMap);
    }
    public void setGlowMap(Texture glowMap) {
        setTexture("GlowMap", glowMap);
    }
    
    public void setColor(ColorRGBA color) {
        setColor("Color", color);
    }
    public void setGlowColor(ColorRGBA glow) {
        setColor("GlowColor", glow);
    }
    
    public void setPointSize(float pointSize) {
        setFloat("PointSize", pointSize);
    }
    public void setDesaturationValue(float value) {
        setFloat("DesaturationValue", value);
    }
    
    public void useVertexColors(boolean vertColors) {
        setBoolean("VertexColor", vertColors);
    }
    public void useSeperateTexCoord(boolean seperateTexCoord) {
        setBoolean("SeperateTexCoord", seperateTexCoord);
    }
    public void useInstancing(boolean instancing) {
        setBoolean("UseInstancing", instancing);
    }
    
}
