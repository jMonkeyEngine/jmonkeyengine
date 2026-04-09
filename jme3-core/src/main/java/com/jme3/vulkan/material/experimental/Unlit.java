package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.material.structs.UnshadedParams;
import com.jme3.vulkan.util.Flag;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class Unlit {

    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<MappableBuffer, StructMapping> mappings = new IdentityHashMap<>();
    private final MappableBuffer visualBuffer = JmePlatform.allocateStandardBuffer(1, BufferUsage.Uniform, UpdateHint.Dynamic);
    private final UnshadedParams visuals = new UnshadedParams();

    {
        visuals.bind(StructLayout.std140);
        visualBuffer.resize(visuals.getSize());
    }

    protected <T extends Struct> T map(MappableBuffer buffer, T struct) {
        StructMapping<T> map = mappings.get(buffer);
        if (map == null) {
            mappings.put(buffer, map = buffer.mapStruct(struct));
        }
        return map.get();
    }

    public void flush() {
        for (StructMapping m : mappings.values()) {
            m.close();
        }
        mappings.clear();
    }

    public void setColor(ColorRGBA color) {
        map(visualBuffer, visuals).color.set(color);
    }

    public void setColorMap(Texture texture) {
        parameters.put("ColorMap", texture);
    }

}
