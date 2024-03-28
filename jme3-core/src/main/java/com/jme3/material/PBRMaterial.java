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
import com.jme3.texture.Texture;

/**
 * Material using PBRLighting.j3md.
 * 
 * @author codex
 */
public class PBRMaterial extends Material {
    
    /**
     * Creates material with PBRLighting.j3md.
     * 
     * @param assetManager 
     */
    public PBRMaterial(AssetManager assetManager) {
        super(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
    }
    
    /****************
     *   TEXTURES   *
     ****************/
    
    /**
     * Sets the base color map.
     * <p>
     * Also known as albedo or diffuse. This defines the base color used to shade
     * the geometry.
     * <p>
     * default=null
     * 
     * @param baseColorMap 
     */
    public void setBaseColorMap(Texture baseColorMap) {
        setTexture("BaseColorMap", baseColorMap);
    }
    
    /**
     * Sets the metallic map.
     * <p>
     * Defines how metallic the geometry will appear when shaded.
     * <p>
     * default=null
     * 
     * @param metallicMap 
     */
    public void setMetallicMap(Texture metallicMap) {
        setTexture("MetallicMap", metallicMap);
    }
    
    /**
     * Sets the roughness map.
     * <p>
     * Defines how able the geometry can reflect light. Low values cleanly
     * reflect light directed at them, while high values greatly diffuse the light.
     * <p>
     * default=null
     * 
     * @param roughnessMap 
     */
    public void setRoughnessMap(Texture roughnessMap) {
        setTexture("RoughnessMap", roughnessMap);
    }
    
    /**
     * Sets the metallic-roughness map.
     * <p>
     * Defines both the metallic and roughness properties.
     * <p>
     * The channels are assigned as follows:
     * <ul>
     *  <li>red = ambient occlusion (if {@link #packAOInMetallicRoughnessMap(boolean)} is set to true.
     *  <li>green = roughness
     *  <li>blue = metallic
     * </ul>
     * Alpha channel is not assigned.
     * <p>
     * default=null
     * 
     * @param metallicRoughnessMap 
     * @see #setMetallicMap(com.jme3.texture.Texture)
     * @see #setRoughnessMap(com.jme3.texture.Texture) 
     */
    public void setMetallicRoughnessMap(Texture metallicRoughnessMap) {
        setTexture("MetallicRoughnessMap", metallicRoughnessMap);
    }
    
    /**
     * Sets the emissive map.
     * <p>
     * Defines where the geometry "emits light". Emission areas are not affected
     * by light sources (or the lack thereof).
     * <p>
     * default=null
     * 
     * @param emissiveMap 
     */
    public void setEmissiveMap(Texture emissiveMap) {
        setTexture("EmissiveMap", emissiveMap);
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
     * @param specularMap 
     */
    public void setSpecularMap(Texture specularMap) {
        setTexture("SpecularMap", specularMap);
    }
    
    /**
     * Sets the glossiness map.
     * <p>
     * default=null
     * 
     * @param glossMap 
     */
    public void setGlossinessMap(Texture glossMap) {
        setTexture("GlossinessMap", glossMap);
    }
    
    /**
     * Sets the specular-glossiness map.
     * <p>
     * Defines both the specular and glossiness properties.
     * <p>
     * The channels are assigned as follows:
     * <ul>
     *  <li>red, green, and blue = specular
     *  <li>alpha = glossiness
     * </ul>
     * <p>
     * default=null
     * 
     * @param specGlossMap 
     */
    public void setSpecularGlossinessMap(Texture specGlossMap) {
        setTexture("SpecularGlossinessMap", specGlossMap);
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
     * Sets the light map.
     * <p>
     * Defines where the geometry is exposed to light. This is mainly used with
     * baked illumination for static scenes.
     * <p>
     * default=null
     * 
     * @param lightMap 
     */
    public void setLightMap(Texture lightMap) {
        setTexture("LightMap", lightMap);
    }
    
    /**************
     *   COLORS   *
     **************/
    
    /**
     * Sets the base color.
     * <p>
     * Defines the base color of the geometry before shading.
     * <p>
     * default=(1.0, 1.0, 1.0, 1.0)
     * 
     * @param baseColor 
     */
    public void setBaseColor(ColorRGBA baseColor) {
        setColor("BaseColor", baseColor);
    }
    
    /**
     * Sets the emissive color.
     * <p>
     * Defines the color of "light" the entire geometry emits. If this parameter is
     * not null, the geometry is unaffected by light sources (or the lack thereof).
     * <p>
     * default=null
     * 
     * @param emissiveColor 
     */
    public void setEmissiveColor(ColorRGBA emissiveColor) {
        setColor("Emissive", emissiveColor);
    }
    
    /**
     * Sets the specular color.
     * <p>
     * Defines the color where light takes exactly one bounce off the geometry
     * to reach the camera.
     * <p>
     * default=(1.0, 1.0, 1.0, 1.0)
     * 
     * @param specColor 
     */
    public void setSpecularColor(ColorRGBA specColor) {
        setColor("Specular", specColor);
    }
    
    /**************
     *   FLOATS   *
     **************/
    
    /**
     * Sets the alpha discard threshold.
     * <p>
     * Pixels with an alpha value below this threshold will be discarded.
     * <p>
     * default=null
     * 
     * @param alphaDiscard 
     */
    public void setAlphaDiscardThreshold(Float alphaDiscard) {
        setFloat("AlphaDiscardThreshold", alphaDiscard);
    }
    
    /**
     * Sets the metallic value.
     * <p>
     * Defines how metallic the geometry will look when shaded.
     * <p>
     * default=1.0
     * 
     * @param metallic 
     */
    public void setMetallic(Float metallic) {
        setFloat("Metallic", metallic);
    }
    
    /**
     * Sets the roughness value.
     * <p>
     * Defines how able the geometry can reflect light. Low values cleanly
     * reflect light directed at them, while high values greatly diffuse the light.
     * <p>
     * default=1.0
     * 
     * @param roughness 
     */
    public void setRoughness(Float roughness) {
        setFloat("Roughness", roughness);
    }
    
    /**
     * Sets the emissive power value.
     * <p>
     * Defines the power of "light" emitted from the geometry. This often puts colors
     * resulting into HDR, which is important for HDR-dependent filters such as ToneMapFilter.
     * <p>
     * default=3.0
     * 
     * @param emissivePower 
     */
    public void setEmissivePower(Float emissivePower) {
        setFloat("EmissivePower", emissivePower);
    }
    
    /**
     * Sets the emissive intensity value.
     * <p>
     * Defines the intensity of "light" emitted from the geometry. This often puts resulting
     * colors into HDR, which is important for HDR-dependent filters such as ToneMapFilter.
     * <p>
     * default=2.0
     * 
     * @param emissiveIntensity 
     */
    public void setEmissiveIntensity(Float emissiveIntensity) {
        setFloat("EmissiveIntensity", emissiveIntensity);
    }
    
    /**
     * Sets the glossiness value.
     * <p>
     * default=1.0
     * 
     * @param gloss 
     */
    public void setGlossiness(Float gloss) {
        setFloat("Glossiness", gloss);
    }
    
    /**
     * Sets the specular anti-aliasing screen-space variance.
     * <p>
     * default=null
     * 
     * @param sigma 
     */
    public void setSpecAntiAliasingVariance(Float sigma) {
        setFloat("SpecularAASigma", sigma);
    }
    
    /**
     * Sets the specular anti-aliasing clamping threshold.
     * <p>
     * default=null
     * 
     * @param kappa 
     */
    public void setSpecAntiAliasingClamp(Float kappa) {
        setFloat("SpecularAAKappa", kappa);
    }
    
    /**
     * Sets the parallax height.
     * <p>
     * default=0.05
     * 
     * @param parallaxHeight 
     */
    public void setParallaxHeight(Float parallaxHeight) {
        setFloat("ParallaxHeight", parallaxHeight);
    }
    
    /**
     * Sets the ambient occlusion strength.
     * <p>
     * Zero results in no occlusion; one results in full occlusion.
     * <p>
     * default=null
     * 
     * @param aoStrength 
     */
    public void setAmbientOcclusionStrength(Float aoStrength) {
        setFloat("AoStrength", aoStrength);
    }
    
    /****************
     *   BOOLEANS   *
     ****************/
    
    /**
     * Enables using the specular-glossiness map over the individual maps.
     * <p>
     * default=false
     * 
     * @param specGloss 
     */
    public void useSpecGlossPipeline(Boolean specGloss) {
        setBoolean("UseSpecGloss", specGloss);
    }
    
    /**
     * Enables specular anti-aliasing.
     * <p>
     * default=true
     * 
     * @param specAA 
     */
    public void useSpecularAA(Boolean specAA) {
        setBoolean("UseSpecularAA", specAA);
    }
    
    /**
     * If true, parallax values will be read from the alpha channel of the normal map,
     * if it exists, instead of the dedicated parallax map.
     * <p>
     * default=false
     * 
     * @param packNormParallax 
     */
    public void usePackedNormalParallax(Boolean packNormParallax) {
        setBoolean("PackedNormalParallax", packNormParallax);
    }
    
    /**
     * Enables using steep parallax as opposed to classic parallax.
     * <p>
     * Steep parallax produces better results, but is slower than classic parallax.
     * <p>
     * default=false
     * 
     * @param steepParallax 
     */
    public void useSteepParallax(Boolean steepParallax) {
        setBoolean("SteepParallax", steepParallax);
    }
    
    /**
     * Enables horizon fading.
     * <p>
     * default=false
     * 
     * @param horizonFade 
     */
    public void useHorizonFade(Boolean horizonFade) {
        setBoolean("HorizonFade", horizonFade);
    }
    
    /**
     * Enables use of a seperate texture coordinate (TexCoord2 buffer) for the light map.
     * <p>
     * default=false
     * 
     * @param seperateTexCoord 
     */
    public void useSeperateTexCoord(Boolean seperateTexCoord) {
        setBoolean("SeperateTexCoord", seperateTexCoord);
    }
    
    /**
     * If true, ambient occlusion will be read from the light map.
     * <p>
     * default=false
     * 
     * @param lightAsAO 
     */
    public void useLightMapForAmbientOcclusion(Boolean lightAsAO) {
        setBoolean("LightMapAsAOMap", lightAsAO);
    }
    
    /**
     * If true, ambient occlusion will be read from the red channel of the
     * metallic-roughness map.
     * <p>
     * default=false
     * 
     * @param packAOInMR 
     */
    public void packAOInMetallicRoughnessMap(Boolean packAOInMR) {
        setBoolean("AOPackedInMRMap", packAOInMR);
    }
    
    /**
     * Enables instancing.
     * <p>
     * default=false
     * 
     * @param instancing 
     */
    public void useInstancing(Boolean instancing) {
        setBoolean("UseInstancing", instancing);
    }
    
    /**
     * Enables vertex colors (Color buffer).
     * 
     * @param vertColors 
     */
    public void useVertexColors(Boolean vertColors) {
        setBoolean("UseVertexColors", vertColors);
    }
    
    /*************
     *   OTHER   *
     *************/
    
    /**
     * Sets the normal map convention used to calculate normals from the normal map.
     * <p>
     * default={@link NormalType#OpenGL}
     * 
     * @param normalType 
     */
    public void setNormalMapType(NormalType normalType) {
        normalType.apply(this);
    }
    
}
