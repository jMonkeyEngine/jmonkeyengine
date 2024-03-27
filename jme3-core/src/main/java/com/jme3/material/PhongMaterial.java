/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;

/**
 *
 * @author codex
 */
public class PhongMaterial extends Material {

    public PhongMaterial(AssetManager assetManager) {
        super(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    }
    
    public void setDiffuseMap(Texture diffuseMap) {
        setTexture("DiffuseMap", diffuseMap);
    }
    public void setNormalMap(Texture normalMap) {
        setTexture("NormalMap", normalMap);
    }
    public void setNormalMap(Texture normalMap, NormalType normalType) {
        setNormalMap(normalMap);
        setNormalMapType(normalType);
    }
    public void setSpecularMap(Texture specMap) {
        setTexture("SpecularMap", specMap);
    }
    public void setParallaxMap(Texture parallaxMap) {
        setTexture("ParallaxMap", parallaxMap);
    }
    public void setAlphaMap(Texture alphaMap) {
        setTexture("AlphaMap", alphaMap);
    }
    public void setColorRamp(Texture colorRamp) {
        setTexture("ColorRamp", colorRamp);
    }
    public void setGlowMap(Texture glowMap) {
        setTexture("GlowMap", glowMap);
    }
    public void setLightMap(Texture lightMap) {
        setTexture("LightMap", lightMap);
    }
    public void setEnvironmentMap(TextureCubeMap envMap) {
        setParam("EnvMap", VarType.TextureCubeMap, envMap);
    }
    
    public void setAmbientColor(ColorRGBA ambient) {
        setColor("Ambient", ambient);
    }
    public void setDiffuseColor(ColorRGBA diffuse) {
        setColor("Diffuse", diffuse);
    }
    public void setSpecularColor(ColorRGBA specular) {
        setColor("Specular", specular);
    }
    public void setGlowColor(ColorRGBA glow) {
        setColor("GlowColor", glow);
    }
    
    public void setAlphaDiscardThreshold(float threshold) {
        setFloat("AlphaDiscardThreshold", threshold);
    }
    public void setShininess(float shininess) {
        setFloat("Shininess", shininess);
    }
    public void setParallaxHeight(float parallaxHeight) {
        setFloat("ParallaxHeight", parallaxHeight);
    }
    
    public void useVertexLighting(boolean vertLighting) {
        setBoolean("VertexLighting", vertLighting);
    }
    public void useMaterialColors(boolean matColors) {
        setBoolean("UseMaterialColors", matColors);
    }
    public void useVertexColors(boolean vertColors) {
        setBoolean("UseVertexColors", vertColors);
    }
    public void useSteepParallax(boolean steepParallax) {
        setBoolean("SteepParallax", steepParallax);
    }
    public void useEnvironmentMapAsSphere(boolean sphere) {
        setBoolean("EnvMapAsSphereMap", sphere);
    }
    public void useInstancing(boolean instancing) {
        setBoolean("UseInstancing", instancing);
    }
    
    public Vector3f setFresnel(float bias, float scale, float power) {
        Vector3f params = new Vector3f(bias, scale, power);
        setVector3("FresnelParams", params);
        return params;
    }
    public void setFresnel(Vector3f params) {
        setVector3("FresnelParams", params);
    }
    public void setNormalMapType(NormalType normalType) {
        normalType.apply(this);
    }
    
}
