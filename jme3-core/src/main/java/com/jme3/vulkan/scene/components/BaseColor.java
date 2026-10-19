package com.jme3.vulkan.scene.components;

import com.jme3.math.ColorRGBA;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.scene.Scene;

public class BaseColor extends Property<ColorRGBA> {

    public BaseColor(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
