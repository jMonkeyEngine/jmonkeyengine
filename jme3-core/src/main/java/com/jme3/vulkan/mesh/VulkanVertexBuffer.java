package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.AdaptiveBuffer;
import com.jme3.vulkan.buffers.generate.BufferGenerator;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

public class VulkanVertexBuffer implements VertexBuffer {

    private final VertexBinding binding;
    private final AdaptiveBuffer buffer;
    private int mappers = 0;
    private PointerBuffer memory;

    public VulkanVertexBuffer(VertexBinding binding, BufferGenerator<?> generator, int vertices) {
        this.binding = binding;
        this.buffer = new AdaptiveBuffer(MemorySize.bytes(vertices * binding.getStride()), BufferUsage.Vertex, generator);
    }

    @Override
    public PointerBuffer map() {
        if (mappers++ == 0) {
            memory = buffer.map();
        }
        return memory;
    }

    @Override
    public void unmap() {
        if (--mappers <= 0) {
            memory = null;
            buffer.unmap();
        }
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public GpuBuffer getBuffer() {
        return buffer;
    }

    @Override
    public void setNumVertices(int vertices) {
        buffer.resize(vertices * binding.getStride() / buffer.size().getBytesPerElement());
    }

    @Override
    public void setAccessFrequency(AccessRate access) {
        buffer.setAccessFrequency(access);
    }

    @Override
    public long getOffset() {
        return 0;
    }

    @Override
    public boolean isInstanceBuffer() {
        return binding.getRate().is(InputRate.Instance);
    }

}
