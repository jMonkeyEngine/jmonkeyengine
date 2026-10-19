package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicFloatArray;
import com.jme3.vulkan.scene.Scene;

public class Roughness extends Property<Float> {

    public Roughness(Scene scene) {
        super(scene, () -> new DynamicFloatArray(scene.getAvailableNodeSlots()));
    }

}
