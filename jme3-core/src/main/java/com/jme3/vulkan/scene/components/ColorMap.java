package com.jme3.vulkan.scene.components;

import com.jme3.texture.Texture;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.scene.Scene;

public class ColorMap extends Property<Texture> {

    public ColorMap(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
