package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.vulkan.util.pointer.Pointer;
import com.jme3.vulkan.util.pointer.PushPointer;

public class PBR implements ShadingInterface {

    private final Pointer<ColorRGBA> color = new PushPointer<>(new ColorRGBA(0, 0, 0, 0));
    private final Pointer<Float> metallic = new PushPointer<>(0.5f);
    private final Pointer<Float> roughness = new PushPointer<>(0.5f);
    private final Pointer<Boolean> fogEnabled = new PushPointer<>(false);
    private final Pointer<ColorRGBA> fogColor = new PushPointer<>(new ColorRGBA(0, 0, 0, 0));

    private Texture colorMap;
    private Texture normalMap;

    public Pointer<ColorRGBA> getColor() {
        return color;
    }

    public Pointer<Float> getMetallic() {
        return metallic;
    }

    public Pointer<Float> getRoughness() {
        return roughness;
    }

    public Pointer<Boolean> getFogEnabled() {
        return fogEnabled;
    }

    public Pointer<ColorRGBA> getFogColor() {
        return fogColor;
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
