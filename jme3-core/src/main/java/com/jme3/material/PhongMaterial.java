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
 * Material using Lighting.j3md.
 * 
 * @author codex
 */
public class PhongMaterial extends Material {
    
    /**
     * Creates material with Lighting.j3md.
     * 
     * @param assetManager 
     */
    public PhongMaterial(AssetManager assetManager) {
        super(assetManager, Materials.LIGHTING);
    }
    
    /**
     * Sets the diffuse map.
     * <p>
     * Also known as albedo or base color. This defines the basic color of the geometry.
     * <p>
     * default=null
     * 
     * @param diffuseMap 
     */
    public void setDiffuseMap(Texture diffuseMap) {
        setTexture("DiffuseMap", diffuseMap);
    }
    
    /**
     * Sets the normal map.
     * <p>
     * Simulates fake bumpiness on the geometry by altering normals per-pixel.
     * <p>
     * default=null
     * 
     * @param normalMap 
     */
    public void setNormalMap(Texture normalMap) {
        setTexture("NormalMap", normalMap);
    }
    
    /**
     * Sets the normal map and normal map type.
     * 
     * @param normalMap
     * @param normalType 
     * @see #setNormalMap(com.jme3.texture.Texture)
     * @see #setNormalMapType(com.jme3.material.NormalType)
     */
    public void setNormalMap(Texture normalMap, NormalType normalType) {
        setNormalMap(normalMap);
        setNormalMapType(normalType);
    }
    
    /**
     * Sets the specular map.
     * <p>
     * Specular defines the color where light takes exactly one bounce off the geometry
     * to reach the camera.
     * <p>
     * default=null
     * 
     * @param specMap 
     */
    public void setSpecularMap(Texture specMap) {
        setTexture("SpecularMap", specMap);
    }
    
    /**
     * Sets the parallax map.
     * <p>
     * Parallax attempts to simulate deeper crevices in the geometry than normal maps
     * by altering texture coordinates and normals per-pixel.
     * <p>
     * default=null
     * 
     * @param parallaxMap 
     */
    public void setParallaxMap(Texture parallaxMap) {
        setTexture("ParallaxMap", parallaxMap);
    }
    
    /**
     * Sets the alpha map.
     * <p>
     * Alters the alpha output by multiplying the alpha by this map's red channel.
     * <p>
     * default=null
     * 
     * @param alphaMap 
     */
    public void setAlphaMap(Texture alphaMap) {
        setTexture("AlphaMap", alphaMap);
    }
    
    /**
     * Sets the color ramp texture.
     * <p>
     * The diffuse and specular colors in the shader are multiplied by this texture.
     * This is useful for producing toon-style effects.
     * <p>
     * default=null
     * 
     * @param colorRamp 
     */
    public void setColorRamp(Texture colorRamp) {
        setTexture("ColorRamp", colorRamp);
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
     * Sets the environment map used to simulate reflections.
     * <p>
     * default=null
     * 
     * @param envMap 
     */
    public void setEnvironmentMap(TextureCubeMap envMap) {
        setParam("EnvMap", VarType.TextureCubeMap, envMap);
    }
    
    /**
     * Sets the ambient light color.
     * <p>
     * Faces not under direct light will be influenced by this color.
     * <p>
     * default=null
     * 
     * @param ambient 
     */
    public void setAmbientColor(ColorRGBA ambient) {
        setColor("Ambient", ambient);
    }
    
    /**
     * Sets the diffuse color.
     * <p>
     * Defines the base color used to shade the geometry.
     * <p>
     * default=null
     * 
     * @param diffuse 
     */
    public void setDiffuseColor(ColorRGBA diffuse) {
        setColor("Diffuse", diffuse);
    }
    
    /**
     * Sets the specular color.
     * <p>
     * Defines the color where light takes exactly one bounce off the geometry
     * to reach the camera.
     * <p>
     * default=null
     * 
     * @param specular 
     */
    public void setSpecularColor(ColorRGBA specular) {
        setColor("Specular", specular);
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
     * Sets the alpha discard threshold.
     * <p>
     * Pixels with alpha below the threshold will be discarded.
     * <p>
     * default=null
     * 
     * @param threshold 
     */
    public void setAlphaDiscardThreshold(Float threshold) {
        setFloat("AlphaDiscardThreshold", threshold);
    }
    
    /**
     * Sets the shininess, or how easily the geometry reflects light.
     * <p>
     * default=1.0
     * 
     * @param shininess 
     */
    public void setShininess(Float shininess) {
        setFloat("Shininess", shininess);
    }
    
    /**
     * Sets the height of the parallax effect.
     * <p>
     * default=0.05
     * 
     * @param parallaxHeight 
     */
    public void setParallaxHeight(Float parallaxHeight) {
        setFloat("ParallaxHeight", parallaxHeight);
    }
    
    /**
     * Enables lighting by vertex.
     * <p>
     * default=false
     * 
     * @param vertLighting 
     */
    public void useVertexLighting(boolean vertLighting) {
        setBoolean("VertexLighting", vertLighting);
    }
    
    /**
     * If true, {@link ColorRGBA} parameters are favored over map parameters,
     * and vise versa for false.
     * <p>
     * In order to use {@link #setDiffuseColor(com.jme3.math.ColorRGBA)},
     * {@link #setSpecularColor(com.jme3.math.ColorRGBA)}, etc., this value must
     * be true. Otherwise maps will be used.
     * <p>
     * default=false
     * 
     * @param matColors 
     */
    public void useMaterialColors(boolean matColors) {
        setBoolean("UseMaterialColors", matColors);
    }
    
    /**
     * Enables coloring by vertex attributes (Color buffer).
     * <p>
     * default=false
     * 
     * @param vertColors 
     */
    public void useVertexColors(boolean vertColors) {
        setBoolean("UseVertexColors", vertColors);
    }
    
    /**
     * Enables steep parallax algorithm.
     * <p>
     * Produces better results at the cost of performance.
     * <p>
     * default=false
     * 
     * @param steepParallax 
     */
    public void useSteepParallax(boolean steepParallax) {
        setBoolean("SteepParallax", steepParallax);
    }
    
    /**
     * Sets the environment map to be treated as a sphere.
     * <p>
     * default=false
     * 
     * @param sphere 
     */
    public void useEnvironmentMapAsSphere(boolean sphere) {
        setBoolean("EnvMapAsSphereMap", sphere);
    }
    
    /**
     * Enables instancing.
     * <p>
     * default=false
     * 
     * @param instancing 
     */
    public void useInstancing(boolean instancing) {
        setBoolean("UseInstancing", instancing);
    }
    
    /**
     * Sets the fresnel parameters.
     * <p>
     * The arguments are combined into a {@link Vector3f}.
     * <p>
     * default=null (Vector3f)
     * 
     * @param bias
     * @param scale
     * @param power
     * @return combined Vector3f
     */
    public Vector3f setFresnel(float bias, float scale, float power) {
        Vector3f params = new Vector3f(bias, scale, power);
        setVector3("FresnelParams", params);
        return params;
    }
    
    /**
     * Sets the fresnel parameters
     * <p>
     * default=null
     * 
     * @param params 
     */
    public void setFresnel(Vector3f params) {
        setVector3("FresnelParams", params);
    }
    
    /**
     * Sets the normal map type.
     * <p>
     * default={@link NormalType#OpenGL}
     * 
     * @param normalType 
     */
    public void setNormalMapType(NormalType normalType) {
        normalType.apply(this);
    }
    
}
