package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.graphics.DepthTest;
import com.jme3.vulkan.scene.Scene;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * {@link DepthTest} state (test/write enabled, compare op, bias enabled). Suffixed
 * {@code Type}, like {@link Scene.MeshType}, since {@link DepthTest} is already the name of
 * the value this holds.
 */
public class DepthTestType extends Property<DepthTest> {

    public DepthTestType(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
