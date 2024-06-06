/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.light;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;

/**
 *
 * @author codex
 */
public class LightImagePacker {
    
    private final Texture2D[] textures = new Texture2D[3];
    private final ImageRaster[] rasters = new ImageRaster[3];
    private final ColorRGBA tempColor = new ColorRGBA();
    
    public LightImagePacker() {}
    
    public void setTextures(Texture2D tex1, Texture2D tex2, Texture2D tex3) {
        validateSize(tex1, tex2);
        validateSize(tex1, tex3);
        updateTexture(0, tex1);
        updateTexture(1, tex2);
        updateTexture(2, tex3);
    }
    public Texture2D[] getTextures() {
        return textures;
    }
    public Texture2D getTexture(int i) {
        return textures[i];
    }
    
    public void packLightsToTextures(LightList lights) {
        if (lights.size() == 0) {
            return;
        }
        int x = 0, y = 0;
        int w = textures[0].getImage().getWidth();
        int h = textures[0].getImage().getHeight();
        boolean spotlight = false;
        for (Light l : lights) {
            if (l.getType() == Light.Type.Ambient || l.getType() == Light.Type.Probe) {
                continue;
            }
            tempColor.set(l.getColor()).setAlpha(l.getType().getId());
            rasters[0].setPixel(x, y, tempColor);
            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight)l;
                    vectorToColor(dl.getDirection(), tempColor);
                    rasters[1].setPixel(x, y, tempColor);
                    break;
                case Point:
                    PointLight pl = (PointLight)l;
                    vectorToColor(pl.getPosition(), tempColor);
                    tempColor.a = pl.getInvRadius();
                    rasters[1].setPixel(x, y, tempColor);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight)l;
                    vectorToColor(sl.getPosition(), tempColor);
                    tempColor.a = sl.getInvSpotRange();
                    rasters[1].setPixel(x, y, tempColor);
                    // We transform the spot direction in view space here to save 5 varying later in the lighting shader
                    // one vec4 less and a vec4 that becomes a vec3
                    // the downside is that spotAngleCos decoding happens now in the frag shader.
                    vectorToColor(sl.getDirection(), tempColor);
                    tempColor.a = sl.getPackedAngleCos();
                    rasters[2].setPixel(x, y, tempColor);
                    spotlight = true;
                    break;
            }
            if (++x >= w) {
                x = 0;
                if (++y >= h) {
                    break;
                }
            }
        }
        textures[0].getImage().setUpdateNeeded();
        textures[1].getImage().setUpdateNeeded();
        if (spotlight) {
            textures[2].getImage().setUpdateNeeded();
        }
    }
    
    private void validateSamples(Texture2D tex) {
        if (tex.getImage().getMultiSamples() != 1) {
            throw new IllegalArgumentException("Texture cannot be multisampled.");
        }
    }
    private void validateSize(Texture2D base, Texture2D target) {
        Image baseImg = base.getImage();
        Image targetImg = target.getImage();
        if (baseImg.getWidth() != targetImg.getWidth() || baseImg.getHeight() != targetImg.getHeight()) {
            throw new IllegalArgumentException("Texture has incorrect demensions.");
        }
    }
    private void updateTexture(int i, Texture2D tex) {
        validateSamples(tex);
        if (textures[i] != tex) {
            textures[i] = tex;
            rasters[i] = ImageRaster.create(tex.getImage());
        }
    }
    private ColorRGBA vectorToColor(Vector3f vec, ColorRGBA color) {
        if (color == null) {
            color = new ColorRGBA();
        }
        color.r = vec.x;
        color.g = vec.y;
        color.b = vec.z;
        return color;
    }
    
}
