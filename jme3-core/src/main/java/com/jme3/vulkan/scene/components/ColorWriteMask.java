package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.ColorComponent;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.util.Flag;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * color write mask.
 */
public class ColorWriteMask extends Property<Flag<ColorComponent>> {

    public ColorWriteMask(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
