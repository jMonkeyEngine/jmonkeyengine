package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

public class PBR implements ShaderInterface {

    private final ColorRGBA color = new ColorRGBA();
    private float metallic = 0.5f;
    private float roughness = 0.5f;
    private Texture colorMap, normalMap;

    public ColorRGBA getColor() {
        return color;
    }

    public void getColor(ColorRGBA color) {
        this.color.set(color);
    }

    public float getMetallic() {
        return metallic;
    }

    public void setMetallic(float metallic) {
        this.metallic = metallic;
    }

    public float getRoughness() {
        return roughness;
    }

    public void setRoughness(float roughness) {
        this.roughness = roughness;
    }

    public Texture getColorMap() {
        return colorMap;
    }

    public void setColorMap(Texture colorMap) {
        this.colorMap = colorMap;
    }

    public Texture getNormalMap() {
        return normalMap;
    }

    public void setNormalMap(Texture normalMap) {
        this.normalMap = normalMap;
    }

}
