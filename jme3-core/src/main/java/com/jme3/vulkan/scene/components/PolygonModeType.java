package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.PolygonMode;
import com.jme3.vulkan.scene.Scene;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * {@link PolygonMode}. Suffixed {@code Type}, like {@link Scene.MeshType}, since
 * {@link PolygonMode} is already the name of the value this holds.
 */
public class PolygonModeType extends Property<PolygonMode> {

    public PolygonModeType(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
