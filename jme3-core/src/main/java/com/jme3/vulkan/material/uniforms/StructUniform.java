package com.jme3.vulkan.material.uniforms;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.material.technique.PushConstantRange;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

public class StructUniform <T extends Struct> implements VulkanUniform<T> {

    private final IntEnum<Descriptor> descriptor;
    private final Flag<BufferUsage> usage;
    private final StructLayout layout;
    private final BufferGenerator<VulkanBuffer> generator;
    private VulkanBuffer buffer;
    private T value;

    public StructUniform(IntEnum<Descriptor> descriptor, Flag<BufferUsage> usage, StructLayout layout, T value, BufferGenerator<VulkanBuffer> generator) {
        this.descriptor = descriptor;
        this.usage = usage;
        this.layout = layout;
        this.value = value;
        this.generator = generator;
    }

    @Override
    public DescriptorSetWriter createWriter(SetLayoutBinding binding) {
        if (value == null) {
            return null;
        }
        int size = layout.structSize(value);
        if (buffer == null) {
            buffer = generator.createBuffer(MemorySize.bytes(size), usage, GlVertexBuffer.Usage.Dynamic);
        } else if (buffer.size().getBytes() < size) {
            buffer.resize(buffer.size().setBytes(size));
        }
        try (BufferMapping m = buffer.map()) {
            value.push(layout, m);
        }
        return new Writer(binding, buffer);
    }

    @Override
    public SetLayoutBinding createBinding(int binding, Flag<ShaderStage> scope) {
        return new SetLayoutBinding(descriptor, binding, 1, scope);
    }

    @Override
    public void fillPushConstantsBuffer(PushConstantRange constant, BufferMapping mapping) {
        if (value != null) {
            constant.getSize().position(mapping.getBytes());
            value.push(layout, mapping);
        }
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String getDefineValue() {
        return value == null ? null : Uniform.ENABLED_DEFINE;
    }

    public VulkanBuffer getBuffer() {
        return buffer;
    }

    private static class Writer extends AbstractSetWriter {

        private final long id, offset, bytes;

        private Writer(SetLayoutBinding binding, MappableBuffer data) {
            super(binding, 0, 1);
            this.id = ((GpuBuffer<Long>)data).getGpuObject();
            this.offset = data.size().getOffset();
            this.bytes = data.size().getBytes();
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(id)
                    .offset(offset)
                    .range(bytes));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return id == writer.id
                    && offset == writer.offset
                    && bytes == writer.bytes
                    && Objects.equals(binding, writer.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, id, offset, bytes);
        }

    }

}
