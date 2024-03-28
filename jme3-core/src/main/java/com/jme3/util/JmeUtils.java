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
package com.jme3.util;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.renderer.Limits;
import com.jme3.shadow.AbstractShadowFilter;
import com.jme3.shadow.AbstractShadowRenderer;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.shadow.SpotLightShadowRenderer;

/**
 * Various utilities for JME projects.
 * 
 * @author codex
 */
public class JmeUtils {
    
    /**
     * Creates a shadow renderer for the light.
     * <p>
     * Directional shadows will be created with 2 splits.
     * 
     * @param assetManager
     * @param light
     * @param resolution
     * @return shadow renderer
     * @see #createShadowRenderer(com.jme3.asset.AssetManager, com.jme3.light.Light, int, int)
     */
    public static AbstractShadowRenderer createShadowRenderer(AssetManager assetManager, Light light, int resolution) {
        return createShadowRenderer(assetManager, light, resolution, 2);
    }
    
    /**
     * Creates a shadow renderer for the light.
     * 
     * @param assetManager
     * @param light light to cast shadows with. Must be a directional, point, or spot light.
     * @param resolution resolution of the shadow map
     * @param splits number of splits for directional shadows (unused for point and spot shadows)
     * @return shadow renderer
     * @throws IllegalArgumentException if light is not a directional, point, or spot light
     */
    public static AbstractShadowRenderer createShadowRenderer(AssetManager assetManager, Light light, int resolution, int splits) {
        if (light instanceof DirectionalLight) {
            DirectionalLightShadowRenderer r = new DirectionalLightShadowRenderer(assetManager, resolution, splits);
            r.setLight((DirectionalLight)light);
            return r;
        } else if (light instanceof PointLight) {
            PointLightShadowRenderer r = new PointLightShadowRenderer(assetManager, resolution);
            r.setLight((PointLight)light);
            return r;
        } else if (light instanceof SpotLight) {
            SpotLightShadowRenderer r = new SpotLightShadowRenderer(assetManager, resolution);
            r.setLight((SpotLight)light);
            return r;
        } else {
            throw new IllegalArgumentException("Light must be a DirectionalLight, PointLight, or SpotLight to cast shadows.");
        }
    }
    
    /**
     * Creates a shadow filter for the light.
     * <p>
     * Directional shadows will be created with 2 splits.
     * 
     * @param assetManager
     * @param light
     * @param resolution
     * @return shadow filter
     * @see #createShadowFilter(com.jme3.asset.AssetManager, com.jme3.light.Light, int, int)
     */
    public static AbstractShadowFilter createShadowFilter(AssetManager assetManager, Light light, int resolution) {
        return createShadowFilter(assetManager, light, resolution, 2);
    }
    
    /**
     * Creates a shadow filter for the light.
     * 
     * @param assetManager
     * @param light light to cast shadows with. Must be a directional, point, or spot light.
     * @param resolution resolution of the shadow map
     * @param splits number of splits for directional shadows (unused for point and spot shadows)
     * @return shadow filter
     * @throws IllegalArgumentException if light is not a directional, point, or spot light
     */
    public static AbstractShadowFilter createShadowFilter(AssetManager assetManager, Light light, int resolution, int splits) {
        if (light instanceof DirectionalLight) {
            DirectionalLightShadowFilter f = new DirectionalLightShadowFilter(assetManager, resolution, splits);
            f.setLight((DirectionalLight)light);
            return f;
        } else if (light instanceof PointLight) {
            PointLightShadowFilter f = new PointLightShadowFilter(assetManager, resolution);
            f.setLight((PointLight)light);
            return f;
        } else if (light instanceof SpotLight) {
            SpotLightShadowFilter f = new SpotLightShadowFilter(assetManager, resolution);
            f.setLight((SpotLight)light);
            return f;
        } else {
            throw new IllegalArgumentException("Light must be a DirectionalLight, PointLight, or SpotLight to cast shadows.");
        }
    }
    
    /**
     * Loads the light probe from a j3o scene.
     * <p>
     * The main scene node must contain a light probe in its local light list
     * at index=0.
     * 
     * @param assetManager
     * @param assetPath
     * @return loaded light probe
     */
    public static LightProbe loadLightProbe(AssetManager assetManager, String assetPath) {
        return (LightProbe)assetManager.loadModel(assetPath).getLocalLightList().get(0);
    }
    
    /**
     * Sets the default anisotropy level.
     * <p>
     * Anisotropy sharpens textures viewed at oblique angles. This feature is only
     * supported by desktop platforms, not mobile devices.
     * <p>
     * If the given level is higher than graphics drivers allow, the level will
     * be capped at the limit.
     * <p>
     * <strong>JME wiki page:</strong> https://wiki.jmonkeyengine.org/docs/3.4/core/texture/anisotropic_filtering.html
     * 
     * @param app
     * @param level unclamped default anisotropy level
     * @return default anisotropy level after clamping
     */
    public static int setDefaultAnisotropyLevel(Application app, int level) {
        level = Math.min(level, app.getRenderer().getLimits().get(Limits.TextureAnisotropy));
        app.getRenderer().setDefaultAnisotropicFilter(level);
        return level;
    }
    
}
