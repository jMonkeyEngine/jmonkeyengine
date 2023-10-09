/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.scene.plugins.gltf;

/**
 * Adapter for converting GLTF emissive strength to JME emissive intensity.
 * 
 * @author codex
 */
public class PBREmissiveStrengthMaterialAdapter extends PBRMaterialAdapter {
    
    public PBREmissiveStrengthMaterialAdapter() {
        super();
        addParamMapping("emissiveStrength", "EmissiveIntensity");
    }
    
}
