package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.scene.Scene;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * {@link ColorBlendAttachment} state.
 */
public class Blend extends Property<ColorBlendAttachment> {

    public Blend(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
