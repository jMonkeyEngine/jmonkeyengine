/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxMaterialProperties {

    private static final Logger logger = Logger.getLogger(FbxMaterialProperties.class.getName());
    
    private static final Map<String, FBXMaterialProperty> propertyMetaMap = new HashMap<String, FBXMaterialProperty>();
    
    private final Map<String, Object> propertyValueMap = new HashMap<>();
    
    private static enum Type {
        Color,
        Alpha,
        Factor,
        Texture2DOrColor,
        Texture2DOrAlpha,
        Texture2DOrFactor,
        Texture2D,
        TextureCubeMap,
        Ignore;
    }
    
    private static class FBXMaterialProperty {
        private final String name;
        private final Type type;

        public FBXMaterialProperty(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }

    private static void defineProp(String name, Type type) {
        propertyMetaMap.put(name, new FBXMaterialProperty(name, type));
    }
    
    private static void defineAlias(String alias, String name) {
        propertyMetaMap.put(alias, propertyMetaMap.get(name));
    }
    
    static {
        // Lighting->Ambient
        // TODO: Add support for AmbientMap??
        defineProp("AmbientColor", Type.Color);
        defineProp("AmbientFactor", Type.Factor);
        defineAlias("Ambient", "AmbientColor");
        
        // Lighting->DiffuseMap/Diffuse
        defineProp("DiffuseColor", Type.Texture2DOrColor);
        defineProp("DiffuseFactor", Type.Factor);
        defineAlias("Diffuse", "DiffuseColor");
        
        // Lighting->SpecularMap/Specular
        defineProp("SpecularColor", Type.Texture2DOrColor);
        defineProp("SpecularFactor", Type.Factor);
        defineAlias("Specular", "SpecularColor");
        
        // Lighting->AlphaMap/Diffuse
        defineProp("TransparentColor", Type.Texture2DOrAlpha);
        
        // Lighting->Diffuse
        defineProp("TransparencyFactor", Type.Alpha);
        defineAlias("Opacity", "TransparencyFactor");
        
        // Lighting->GlowMap/GlowColor
        defineProp("EmissiveColor", Type.Texture2DOrColor);
        defineProp("EmissiveFactor", Type.Factor);
        defineAlias("Emissive", "EmissiveColor");
        
        // Lighting->Shininess
        defineProp("Shininess", Type.Factor);
        defineAlias("ShininessExponent", "Shininess");
        
        // Lighting->NormalMap
        defineProp("NormalMap", Type.Texture2D);
        defineAlias("Normal", "NormalMap");
        
        // Lighting->EnvMap
        defineProp("ReflectionColor", Type.Texture2DOrColor);
        
        // Lighting->FresnelParams
        defineProp("Reflectivity", Type.Factor);
        defineAlias("ReflectionFactor", "Reflectivity");
        
        // ShadingModel is no longer specified under Properties element.
        defineProp("ShadingModel", Type.Ignore);
        
        // MultiLayer materials aren't supported.
        defineProp("MultiLayer", Type.Ignore); 
        
        // Not sure what this is. NormalMap again??
        defineProp("Bump", Type.Texture2DOrColor);
        
        defineProp("BumpFactor", Type.Factor);
        defineProp("DisplacementColor", Type.Color);
        defineProp("DisplacementFactor", Type.Factor);
    }
    
    public void setPropertyTexture(String name, FbxTexture texture) {
        FBXMaterialProperty prop = propertyMetaMap.get(name);
        
        if (prop == null) {
            logger.log(Level.WARNING, "Unknown FBX material property '{0}'", name);
            return; 
        }

        if (propertyValueMap.get(name) instanceof FbxTexture) {
            // Multiple / layered textures
            // Just write into 2nd slot for now (maybe will use for lightmaps).
            name = name + "2";
        }
        
        propertyValueMap.put(name, texture);
    }
    
    public void setPropertyFromElement(FbxElement propertyElement) {
        String name = (String) propertyElement.properties.get(0);
        FBXMaterialProperty prop = propertyMetaMap.get(name);
        
        if (prop == null) {
            logger.log(Level.WARNING, "Unknown FBX material property ''{0}''", name);
            return; 
        }
        
        // It is either a color, alpha, or factor. 
        // Textures can only be set via setPropertyTexture.
        
        // If it is an alias, get the real name of the property.
        String realName = prop.name;
        
        switch (prop.type) {
            case Alpha:
            case Factor:
            case Texture2DOrFactor:
            case Texture2DOrAlpha:
                double value = (Double) propertyElement.properties.get(4);
                propertyValueMap.put(realName, (float)value);
                break;
            case Color:
            case Texture2DOrColor:
                double x = (Double) propertyElement.properties.get(4);
                double y = (Double) propertyElement.properties.get(5);
                double z = (Double) propertyElement.properties.get(6);
                ColorRGBA color = new ColorRGBA((float)x, (float)y, (float)z, 1f);
                propertyValueMap.put(realName, color);
                break;
            default:
                logger.log(Level.WARNING, "FBX material property ''{0}'' requires a texture.", name);
                break;
        }
    }
    
    public Object getProperty(String name) { 
        return propertyValueMap.get(name);
    }
}
