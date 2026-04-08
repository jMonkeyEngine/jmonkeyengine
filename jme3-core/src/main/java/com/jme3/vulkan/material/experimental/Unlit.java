package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.descriptors.CachedDescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.material.structs.UnshadedParams;
import com.jme3.vulkan.pipeline.PipelineLayout;

import java.util.IdentityHashMap;
import java.util.Map;

public class Unlit implements ShaderInterface {

    private final Map<DescriptorSetLayout, CachedDescriptorSet> sets = new IdentityHashMap<>();
    private final UnshadedParams params = new UnshadedParams();

    {
        params.bind(StructLayout.std140);
    }

    public void update(PipelineLayout pipeline, DescriptorPool pool, ParameterPool parameters) {
        pipeline.allocate(pool, sets);
        pipeline.write(sets, "ColorMap", parameters.get("ColorMap"));
        try (StructMapping<UnshadedParams> m = parameters.mapBuffer("Params", params)) {
            params.color.compareAndSet(parameters.get("Color"));
        }
    }

    @Override
    public void close() {

    }

    protected UnshadedParams getUnshaded() {
        return getStruct("Unshaded", UnshadedParams::new, StructLayout.std140);
    }

    public void setBaseColor(ColorRGBA color) {
        getUnshaded().color.set(color);
    }

    public void setVertexColor(boolean vertexColor) {
        getUnshaded().vertexColor.set(true);
    }

    public void setColorMap(Texture texture) {
        parameters.put("ColorMap", texture);
    }

    public ColorRGBA getBaseColor() {
        return getUnshaded().color.get();
    }

    public boolean isVertexColor() {
        return getUnshaded().vertexColor.get();
    }

    public Texture getColorMap() {
        return (Texture)parameters.get("ColorMap");
    }

}
