package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.util.struct.*;
import com.jme3.vulkan.buffer.BufferCopy;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;

public class PBR implements ShadingInterface, Disposable {

    public static class Data extends Struct<StructField> {

        public final ObjectArrayField<ColorRGBA> color;
        public final FloatArrayField metallic;
        public final FloatArrayField roughness;

        public Data(int length) {
            color = addField(new ObjectArrayField<>(length, ColorRGBA.class, ColorRGBA[]::new));
            metallic = addField(new FloatArrayField(length));
            roughness = addField(new FloatArrayField(length));
            bind(StructLayout.std140);
        }

    }

    private static final SlottedBuffer<Data> data = new SlottedBuffer<>();

    public static EngineBuffer getData() {
        return data;
    }

    private final DisposableReference ref;
    private final int dataIndex;
    private Texture colorMap, normalMap;

    public PBR(CommandBuffer cmd, MemoryAllocator alloc) {
        dataIndex = data.acquireSlot((n, p) -> {
            Data d = new Data(n);
            d.bind(alloc.createDynamicBuffer(d.capacity(), EngineBuffer.Role.Uniform));
            if (p != null) {
                cmd.cmdCopy(p, d, new BufferCopy().add(p, d), OpLocation.PreferHost);
            }
            return d;
        });
        ref = DisposableManager.reference(this);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> data.releaseSlot(dataIndex);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public void setColor(ColorRGBA color) {
        data.getBuffer().color.set(dataIndex, color);
    }

    public void setMetallic(float metallic) {
        data.getBuffer().metallic.set(dataIndex, metallic);
    }

    public void setRoughness(float roughness) {
        data.getBuffer().roughness.set(dataIndex, roughness);
    }

    public void setColorMap(Texture colorMap) {
        this.colorMap = colorMap;
    }

    public void setNormalMap(Texture normalMap) {
        this.normalMap = normalMap;
    }

}
