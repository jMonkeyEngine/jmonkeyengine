/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxMaterial extends FbxObject<Material> {
    
    private static final Logger logger = Logger.getLogger(FbxMaterial.class.getName());
    
    private String shadingModel; // TODO: do we care about this? lambert just has no specular?
    private final FbxMaterialProperties properties = new FbxMaterialProperties();
    
    public FbxMaterial(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        if(!getSubclassName().equals("")) {
            return;
        }
        
        FbxElement shadingModelEl = element.getChildById("ShadingModel");
        if (shadingModelEl != null) { 
            shadingModel = (String) shadingModelEl.properties.get(0);
            if (!shadingModel.equals("")) {
                if (!shadingModel.equalsIgnoreCase("phong") &&
                    !shadingModel.equalsIgnoreCase("lambert")) {
                    logger.log(Level.WARNING, "FBX material uses unknown shading model: {0}. "
                                            + "Material may display incorrectly.", shadingModel);  
                }
            }
        }
        
        for (FbxElement child : element.getFbxProperties()) {
            properties.setPropertyFromElement(child);
        }
    }
    
    @Override
    public void connectObject(FbxObject object) {
        unsupportedConnectObject(object);
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        if (!(object instanceof FbxTexture)) {
            unsupportedConnectObjectProperty(object, property);
        }
        
        properties.setPropertyTexture(property, (FbxTexture) object);
    }
    
    private static void multRGB(ColorRGBA color, float factor) {
        color.r *= factor;
        color.g *= factor;
        color.b *= factor;
    }
    
    @Override
    protected Material toJmeObject() {
        ColorRGBA ambient  = null;
        ColorRGBA diffuse  = null;
        ColorRGBA specular = null;
        ColorRGBA transp   = null;
        ColorRGBA emissive = null;
        float shininess    = 1f;
        boolean separateTexCoord = false;
        
        Texture diffuseMap  = null;
        Texture specularMap = null;
        Texture normalMap   = null;
        Texture transpMap   = null;
        Texture emitMap     = null;
        Texture aoMap       = null;
        
        FbxTexture fbxDiffuseMap = null;
        
        Object diffuseColor = properties.getProperty("DiffuseColor");
        if (diffuseColor != null) {
            if (diffuseColor instanceof ColorRGBA) {
                diffuse = ((ColorRGBA) diffuseColor).clone();
            } else if (diffuseColor instanceof FbxTexture) {
                FbxTexture tex = (FbxTexture) diffuseColor;
                fbxDiffuseMap = tex;
                diffuseMap = tex.getJmeObject();
                diffuseMap.getImage().setColorSpace(ColorSpace.sRGB);
            }
        }
        
        Object diffuseFactor = properties.getProperty("DiffuseFactor");
        if (diffuseFactor != null && diffuseFactor instanceof Float) {
            float factor = (Float)diffuseFactor;
            if (diffuse != null) {
                multRGB(diffuse, factor);
            } else {
                diffuse = new ColorRGBA(factor, factor, factor, 1f);
            }
        }
        
        Object specularColor = properties.getProperty("SpecularColor");
        if (specularColor != null) {
            if (specularColor instanceof ColorRGBA) {
                specular = ((ColorRGBA) specularColor).clone();
            } else if (specularColor instanceof FbxTexture) {
                FbxTexture tex = (FbxTexture) specularColor;
                specularMap = tex.getJmeObject();
                specularMap.getImage().setColorSpace(ColorSpace.sRGB);
            }
        }
        
        Object specularFactor = properties.getProperty("SpecularFactor");
        if (specularFactor != null && specularFactor instanceof Float) {
            float factor = (Float)specularFactor;
            if (specular != null) {
                multRGB(specular, factor);
            } else {
                specular = new ColorRGBA(factor, factor, factor, 1f);
            }
        }
        
        Object transparentColor = properties.getProperty("TransparentColor");
        if (transparentColor != null) {
            if (transparentColor instanceof ColorRGBA) {
                transp = ((ColorRGBA) transparentColor).clone();
            } else if (transparentColor instanceof FbxTexture) {
                FbxTexture tex = (FbxTexture) transparentColor;
                transpMap = tex.getJmeObject();
                transpMap.getImage().setColorSpace(ColorSpace.sRGB);
            }
        }
        
        Object transparencyFactor = properties.getProperty("TransparencyFactor");
        if (transparencyFactor != null && transparencyFactor instanceof Float) {
            float factor = (Float)transparencyFactor;
            if (transp != null) {
                transp.a *= factor;
            } else {
                transp = new ColorRGBA(1f, 1f, 1f, factor);
            }
        }
        
        Object emissiveColor = properties.getProperty("EmissiveColor");
        if (emissiveColor != null) {
            if (emissiveColor instanceof ColorRGBA) {
                emissive = ((ColorRGBA)emissiveColor).clone();
            } else if (emissiveColor instanceof FbxTexture) {
                FbxTexture tex = (FbxTexture) emissiveColor;
                emitMap = tex.getJmeObject();
                emitMap.getImage().setColorSpace(ColorSpace.sRGB);
            }
        }
        
        Object emissiveFactor = properties.getProperty("EmissiveFactor");
        if (emissiveFactor != null && emissiveFactor instanceof Float) {
            float factor = (Float)emissiveFactor;
            if (emissive != null) { 
                multRGB(emissive, factor);
            } else {
                emissive = new ColorRGBA(factor, factor, factor, 1f);
            }
        }
        
        Object ambientColor = properties.getProperty("AmbientColor");
        if (ambientColor != null && ambientColor instanceof ColorRGBA) {
            ambient = ((ColorRGBA)ambientColor).clone();
        }
        
        Object ambientFactor = properties.getProperty("AmbientFactor");
        if (ambientFactor != null && ambientFactor instanceof Float) { 
            float factor = (Float)ambientFactor;
            if (ambient != null) {
                multRGB(ambient, factor);
            } else {
                ambient = new ColorRGBA(factor, factor, factor, 1f);
            }
        }
        
        Object shininessFactor = properties.getProperty("Shininess");
        if (shininessFactor != null) {
            if (shininessFactor instanceof Float) {
                shininess = (Float) shininessFactor;
            } else if (shininessFactor instanceof FbxTexture) {
                // TODO: support shininess textures
            }
        }
        
        Object bumpNormal = properties.getProperty("NormalMap");
        if (bumpNormal != null) {
            if (bumpNormal instanceof FbxTexture) {
                // TODO: check all meshes that use this material have tangents
                //       otherwise shading errors occur
                FbxTexture tex = (FbxTexture) bumpNormal;
                normalMap = tex.getJmeObject();
                normalMap.getImage().setColorSpace(ColorSpace.Linear);
            }
        }
        
        Object aoColor = properties.getProperty("DiffuseColor2");
        if (aoColor != null) {
            if (aoColor instanceof FbxTexture) {
                FbxTexture tex = (FbxTexture) aoColor;
                if (tex.getUvSet() != null && fbxDiffuseMap != null) {
                    if (!tex.getUvSet().equals(fbxDiffuseMap.getUvSet())) {
                        separateTexCoord = true;
                    }
                }
                aoMap = tex.getJmeObject();
                aoMap.getImage().setColorSpace(ColorSpace.sRGB);
            }
        }
        
        // TODO: how to disable transparency from diffuse map?? Need "UseAlpha" again..

        assert ambient == null  || ambient.a  == 1f;
        assert diffuse == null  || diffuse.a  == 1f;
        assert specular == null || specular.a == 1f;
        assert emissive == null || emissive.a == 1f;
        assert transp == null   || (transp.r == 1f && transp.g == 1f && transp.b == 1f);
        
        // If shininess is less than 1.0, the lighting shader won't be able
        // to handle it. Must disable specularity in that case.
        if (shininess < 1f) {
            shininess = 1f;
            specular = ColorRGBA.Black;
        }
        
        // Guess whether we should enable alpha blending.
        // FBX does not specify this explicitly.
        boolean useAlphaBlend = false;
        
        if (diffuseMap != null && diffuseMap == transpMap) {
            // jME3 already uses alpha from diffuseMap
            // (if alpha blend is enabled)
            useAlphaBlend = true;
            transpMap = null; 
        } else if (diffuseMap != null && transpMap != null && diffuseMap != transpMap) {
            // TODO: potential bug here. Alpha from diffuse may 
            // leak unintentionally. 
            useAlphaBlend = true;
        } else if (transpMap != null) {
            // We have alpha map but no diffuse map, OK.
            useAlphaBlend = true;
        }
        
        if (transp != null && transp.a != 1f) {
            // Consolidate transp into diffuse
            // (jME3 doesn't use a separate alpha color)
            
            // TODO: potential bug here. Alpha from diffuse may 
            // leak unintentionally. 
            useAlphaBlend = true;
            if (diffuse != null) {
                diffuse.a = transp.a;
            } else {
                diffuse = transp;
            }
        }
        
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setName(name);
        
        // TODO: load this from FBX material.
        mat.setReceivesShadows(true);
        
        if (useAlphaBlend) {
            // No clue whether this is a transparent or translucent model, so guess.
            mat.setTransparent(true);
            mat.setFloat("AlphaDiscardThreshold", 0.01f);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        }
        
        mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        
        // Set colors.
        if (ambient != null || diffuse != null || specular != null) {
            // If either of those is set, we have to set them all.
            // NOTE: default specular is black, unless it is set explicitly.
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Ambient",  /*ambient  != null ? ambient  :*/ ColorRGBA.White);
            mat.setColor("Diffuse",  diffuse  != null ? diffuse  : ColorRGBA.White);
            mat.setColor("Specular", specular != null ? specular : ColorRGBA.Black);
        }
        
        if (emissive != null) { 
            mat.setColor("GlowColor", emissive);
        }
        
        // Set shininess.
        if (shininess > 1f) {
            // Convert shininess from 
            // Phong (FBX shading model) to Blinn (jME3 shading model).
            float blinnShininess = (shininess * 5.1f) + 1f;
            mat.setFloat("Shininess", blinnShininess);
        }
        
        // Set textures.
        if (diffuseMap != null) {
            mat.setTexture("DiffuseMap", diffuseMap);
        }
        if (specularMap != null) {
            mat.setTexture("SpecularMap", specularMap);
        }
        if (normalMap != null) {
            mat.setTexture("NormalMap", normalMap);
        }
        if (transpMap != null) {
//            mat.setTexture("AlphaMap", transpMap);
        }
        if (emitMap != null) {
            mat.setTexture("GlowMap", emitMap);
        }
        if (aoMap != null) {
            mat.setTexture("LightMap", aoMap);
            if (separateTexCoord) {
                mat.setBoolean("SeparateTexCoord", true);
            }
        }
        
        return mat;
    }
}
