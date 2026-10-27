package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.graphics.StencilTest;
import com.jme3.vulkan.scene.Scene;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * {@link StencilTest} state.
 */
public class Stencil extends Property<StencilTest> {

    public Stencil(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
