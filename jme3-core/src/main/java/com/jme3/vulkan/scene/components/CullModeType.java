package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.pipeline.CullMode;
import com.jme3.vulkan.scene.Scene;

/**
 * Per-node override of a {@link com.jme3.vulkan.material.experimental.PipelineKey}'s
 * {@link CullMode}. Suffixed {@code Type}, like {@link Scene.MeshType}, since
 * {@link CullMode} is already the name of the value this holds.
 */
public class CullModeType extends Property<CullMode> {

    public CullModeType(Scene scene) {
        super(scene, () -> new DynamicObjectArray<>(scene.getAvailableNodeSlots()));
    }

}
