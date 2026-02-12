package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.AsyncMappingBuffer;

import java.lang.ref.WeakReference;

public class VertexBuffer {

    private final VertexBinding binding;
    private final AsyncMappingBuffer<MappableBuffer> data;
    private final GlVertexBuffer.Usage usage;
    private WeakReference<VertexBuffer> weakRef;

    public VertexBuffer(VertexBinding binding, int elements, GlVertexBuffer.Usage usage) {
        this.binding = binding;
        this.data = new AsyncMappingBuffer<>(binding.createBuffer(elements, usage));
        this.usage = usage;
    }

    public VertexBinding getBinding() {
        return binding;
    }

    public AsyncMappingBuffer<MappableBuffer> getData() {
        return data;
    }

    public GlVertexBuffer.Usage getUsage() {
        return usage;
    }

    public WeakReference<VertexBuffer> getWeakRef() {
        if (weakRef == null) {
            weakRef = new WeakReference<>(this);
        }
        return weakRef;
    }

}
