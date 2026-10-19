package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicFloatArray;
import com.jme3.vulkan.scene.Scene;

public class Metallic extends Property<Float> {

    public Metallic(Scene scene) {
        super(scene, () -> new DynamicFloatArray(scene.getAvailableNodeSlots()));
    }

}
